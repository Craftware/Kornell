package kornell.server.jdbc.repository

import java.sql.ResultSet
import java.util.UUID

import kornell.core.entity.Person
import kornell.core.entity.role.RoleType
import kornell.core.error.exception.{EntityNotFoundException, UnauthorizedAccessException}
import kornell.server.authentication.ThreadLocalAuthenticator
import kornell.server.jdbc.SQL._
import kornell.server.util.SHA256
import org.mindrot.BCrypt

import scala.util.Try

object AuthRepo {

  def apply() = new AuthRepo

  def lookup(institutionUUID: String, userkey: String): Option[UsrValue] =
    authByUsername(institutionUUID, userkey)
      .orElse(authByCPF(institutionUUID, userkey))
      .orElse(authByEmail(institutionUUID, userkey))

  implicit def toUsrValue(r: ResultSet): UsrValue =
    (r.getString("password"), r.getString("personUUID"), r.getBoolean("forcePasswordUpdate"))

  type UsrKey = (String, String)
  type UsrValue = (UsrPassword, PersonUUID, PasswordResetRequired) //1st String is bcrypt(sha256(password)), 2nd
  type UsrPassword = String
  type PersonUUID = String
  type PasswordResetRequired = Boolean

  def authByEmail(institutionUUID: String, email: String): Option[UsrValue] =
    sql"""
   select pwd.password as password, pwd.personUUID, p.forcePasswordUpdate
   from Password pwd
   join Person p on p.uuid = pwd.personUUID
   where p.email=${email}
     and p.institutionUUID=${institutionUUID}
    """.first[UsrValue](toUsrValue)

  def authByCPF(institutionUUID: String, cpf: String): Option[UsrValue] =
    sql"""
   select pwd.password as password, pwd.personUUID, p.forcePasswordUpdate
   from Password pwd
   join Person p on p.uuid = pwd.personUUID
   where p.cpf=${cpf}
     and p.institutionUUID=${institutionUUID}
    """.first[UsrValue](toUsrValue)

  def authByUsername(institutionUUID: String, username: String): Option[UsrValue] =
    sql"""
    select pwd.password, pwd.personUUID, p.forcePasswordUpdate
    from Password pwd
  join Person p on p.uuid = pwd.personUUID
  where pwd.username=${username}
  and pwd.institutionUUID=${institutionUUID}
    """.first[UsrValue](toUsrValue)

  def usernameOf(personUUID: String): Option[String] = {
    val username = sql"""
      select username from Password where personUUID = $personUUID
    """.first[String] { rs => rs.getString("username") }
    username
  }

}

class AuthRepo() {

  type AuthValue = (String, Boolean)

  def authenticate(institutionUUID: String, userkey: String, password: String): Option[AuthValue] = Try {
    val usrValue = AuthRepo.lookup(institutionUUID, userkey).get
    if (BCrypt.checkpw(SHA256(password), usrValue._1)) {
      Option((usrValue._2, usrValue._3))
    } else {
      None
    }
  }.getOrElse(None)

  def withPerson[T](fun: Person => T): T = {
    val authenticatedUUID = ThreadLocalAuthenticator.getAuthenticatedPersonUUID
    authenticatedUUID match {
      case Some(personUUID) => {
        val person = PersonRepo(personUUID).first
        person match {
          case Some(one) => fun(one)
          case None => throw new EntityNotFoundException("personNotFound")
        }
      }
      case None => throw new UnauthorizedAccessException("authenticationFailed")
    }
  }

  def getPersonByPasswordChangeUUID(passwordChangeUUID: String): Option[Person] =
    sql"""
      select p.* from Person p
      join Password pwd on pwd.personUUID = p.uuid
      where pwd.requestPasswordChangeUUID = $passwordChangeUUID
    """.first[Person]

  def getUsernameByPersonUUID(personUUID: String): Option[String] =
    sql"""
      select pwd.username from Password pwd
      where pwd.personUUID = $personUUID
    """.first[String]

  def getPersonByUsernameAndPasswordUpdateFlag(username: String): Option[Person] =
    sql"""
      select p.* from Person p
      join Password pwd on pwd.personUUID = p.uuid
      where pwd.username = ${username} and p.forcePasswordUpdate = true
    """.first[Person].orElse(
      sql"""
        select p.* from Person p
        where p.email = ${username} and p.forcePasswordUpdate = true
      """.first[Person]
    )

  def hasPassword(institutionUUID: String, username: String): Boolean =
    sql"""
      select pwd.username from Password pwd
      where pwd.username = $username
      and pwd.institutionUUID = $institutionUUID
      and pwd.password is not null
    """.first[String].isDefined

  def updatePassword(personUUID: String, plainPassword: String, disableForceUpdatePassword: Boolean): Unit = {
    sql"""
      update Password set password=${BCrypt.hashpw(SHA256(plainPassword), BCrypt.gensalt())}, requestPasswordChangeUUID=null where personUUID=${personUUID}
    """.executeUpdate
    if (disableForceUpdatePassword) {
      sql"""
         update Person set forcePasswordUpdate=false where uuid=${personUUID}
      """.executeUpdate
    }
  }

  def setPlainPassword(institutionUUID: String, personUUID: String, username: String, plainPassword: String, forcePasswordUpdate: Boolean, requestPasswordChangeUUID: String = null): Unit = {
    val pwd = Option(plainPassword) match {
      case Some(_) => BCrypt.hashpw(SHA256(plainPassword), BCrypt.gensalt())
      case None => null
    }
    sql"""
      insert into Password (uuid,personUUID,username,password,requestPasswordChangeUUID,institutionUUID)
      values (${UUID.randomUUID.toString},$personUUID,$username,$pwd,$requestPasswordChangeUUID,$institutionUUID)
      on duplicate key update
      username = $username, password = $pwd, requestPasswordChangeUUID = $requestPasswordChangeUUID
    """.executeUpdate

    if (forcePasswordUpdate) {
      sql"""
        update Person set forcePasswordUpdate = true
        where uuid = $personUUID
      """.executeUpdate
    }
  }

  def updateRequestPasswordChangeUUID(personUUID: String, requestPasswordChangeUUID: String): Unit =
    sql"""
        update Password set requestPasswordChangeUUID = $requestPasswordChangeUUID
        where personUUID = $personUUID
      """.executeUpdate

  def grantPlatformAdmin(personUUID: String, institutionUUID: String): Unit = {
    sql"""
        insert into Role (uuid, personUUID, role, institutionUUID, courseClassUUID)
        values (${UUID.randomUUID.toString}, ${personUUID},
        ${RoleType.platformAdmin.toString},
        ${institutionUUID},
        ${null})
        """.executeUpdate
  }

  def grantInstitutionAdmin(personUUID: String, institutionUUID: String): Unit =
    sql"""
        insert into Role (uuid, personUUID, role, institutionUUID, courseClassUUID)
        values (${UUID.randomUUID.toString},
        ${personUUID},
        ${RoleType.institutionAdmin.toString},
        ${institutionUUID},
        ${null} )
        """.executeUpdate
}
