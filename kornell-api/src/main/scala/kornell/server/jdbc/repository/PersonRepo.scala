package kornell.server.jdbc.repository

import kornell.core.entity.Person
import kornell.core.entity.role.{RoleCategory, RoleType}
import kornell.core.error.exception.EntityConflictException
import kornell.core.util.StringUtils._
import kornell.server.jdbc.PreparedStmt
import kornell.server.jdbc.SQL._

class PersonRepo(val uuid: String) {

  def get: Person = first.get

  def first: Option[Person] = PeopleRepo.getByUUID(uuid)

  def update(person: Person): PersonRepo = {
    sql"""
      update Person set fullName = ${person.getFullName},
      email = ${person.getEmail}, company = ${person.getCompany}, title = ${person.getTitle},
      sex = ${person.getSex}, birthDate = ${person.getBirthDate}, confirmation = ${person.getConfirmation},
      telephone = ${person.getTelephone}, country = ${person.getCountry}, state = ${person.getState}, city = ${person.getCity},
      addressLine1 = ${person.getAddressLine1}, addressLine2 = ${person.getAddressLine2}, postalCode = ${person.getPostalCode},
      cpf = ${person.getCPF}, registrationType = ${person.getRegistrationType.toString}, institutionRegistrationPrefixUUID = ${person.getInstitutionRegistrationPrefixUUID},
      receiveEmailCommunication = ${person.isReceiveEmailCommunication} where uuid = $uuid
      """.executeUpdate
    PeopleRepo.updateCaches(person)
    PersonRepo.this
  }

  def setPassword(password: String, forcePasswordUpdate: Boolean = false): PersonRepo = {
    val username = AuthRepo().getUsernameByPersonUUID(uuid).getOrElse(get.getEmail)
    AuthRepo().setPlainPassword(get.getInstitutionUUID, uuid, username, password, forcePasswordUpdate)
    PersonRepo.this
  }

  def updatePassword(personUUID: String, plainPassword: String, disableForceUpdatePassword: Boolean = false): PersonRepo = {
    AuthRepo().updatePassword(personUUID, plainPassword, disableForceUpdatePassword)
    PersonRepo.this
  }

  def hasPassword(institutionUUID: String): Boolean = {
    val sql = s"select count(*) from Person p join Password pw on pw.personUUID = p.uuid where p.uuid = '${uuid}' and p.institutionUUID = '${institutionUUID}' and pw.password is not null"
    val pstmt = new PreparedStmt(sql, List())
    val result = pstmt.get[Boolean]
    result
  }

  //TODO: Better security against SQLInjection?
  //TODO: Better dynamic queries
  //TODO: Teste BOTH args case!!
  def isRegistered(institutionUUID: String, cpf: String, email: String): Boolean = {
    var sql = s"select count(*) from Person p left join Password pw on pw.personUUID = p.uuid where p.uuid != '${uuid}' and p.institutionUUID = '${institutionUUID}' and pw.password is not null "
    if (isSome(cpf)) {
      sql = sql + s"and (p.cpf = '${digitsOf(cpf)}' or pw.username = '${digitsOf(cpf)}')"
    }
    if (isSome(email)) {
      sql = sql + s"and (p.email = '${email}' or pw.username = '${email}')"
    }
    if (sql.contains("--")) throw new EntityConflictException("invalidValue")
    val pstmt = new PreparedStmt(sql, List())
    val result = pstmt.get[Boolean]
    result
  }

  def hasPowerOver(targetPersonUUID: String): Boolean = {
    val actorRoles = new RolesRepo().getUserRoles(uuid, RoleCategory.BIND_DEFAULT).getRoleTOs
    val targetPerson = PersonRepo(targetPersonUUID).get
    val targetUsername = AuthRepo().getUsernameByPersonUUID(targetPersonUUID)

    //if there's no username yet, any admin can have power
    targetUsername.isEmpty ||
      {
        val targetRoles = new RolesRepo().getUserRoles(targetPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs

        //people have power over themselves
        (uuid == targetPersonUUID) ||
          {
            //platformAdmin has power over everyone
            RoleCategory.isPlatformAdmin(actorRoles, targetPerson.getInstitutionUUID)
          } || {
            //institutionAdmin doesn't have power over platformAdmins, other institutionAdmins or people from other institutions
            !RoleCategory.isPlatformAdmin(targetRoles, targetPerson.getInstitutionUUID) &&
              !RoleCategory.hasRole(targetRoles, RoleType.institutionAdmin) &&
              RoleCategory.isInstitutionAdmin(actorRoles, targetPerson.getInstitutionUUID)
          } || {
            //courseClassAdmin doesn't have power over platformAdmins, institutionAdmins, other courseClassAdmins or non enrolled users
            val enrollmentTOs = EnrollmentsRepo.byPerson(targetPersonUUID)
            !RoleCategory.isPlatformAdmin(targetRoles, targetPerson.getInstitutionUUID) &&
              !RoleCategory.hasRole(targetRoles, RoleType.institutionAdmin) &&
              !RoleCategory.hasRole(targetRoles, RoleType.institutionCourseClassesAdmin) &&
              !RoleCategory.hasRole(targetRoles, RoleType.courseClassAdmin) && {
                enrollmentTOs exists {
                  to => RoleCategory.isCourseClassAdmin(actorRoles, to.getCourseClassUUID)
                }
              }
          }
      }
  }

  def getUsername: String = sql"""select username from Password where personUUID=${uuid}""".first[String].orNull

  def acceptTerms(p: Person): Unit = {
    sql"""update Person
         set termsAcceptedOn = now()
         where uuid=${uuid}
           """.executeUpdate
    PeopleRepo.invalidateCache(p)
  }

  def actomsVisitedBy(enrollmentUUID: String): List[String] = sql"""
    select actomKey, eventFiredAt from ActomEntered ae
    join Enrollment e on ae.enrollmentUUID=e.uuid
    where e.uuid = ${enrollmentUUID}
    and e.personUUID = ${uuid}
    order by eventFiredAt
    """.map[String]({ rs => rs.getString("actomKey") })

}

object PersonRepo {
  def apply(uuid: String) = new PersonRepo(uuid)
}
