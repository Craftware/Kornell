package kornell.server.api

import java.net.URL
import java.util.UUID

import javax.servlet.http.HttpServletRequest
import javax.ws.rs._
import javax.ws.rs.core.{Context, Response, SecurityContext}
import kornell.core.entity.role.RoleCategory
import kornell.core.entity.{AuditedEntityType, Person, RegistrationType}
import kornell.core.error.exception.{EntityNotFoundException, UnauthorizedAccessException}
import kornell.core.to.{RegistrationRequestTO, UserHelloTO, UserInfoTO}
import kornell.core.util.StringUtils
import kornell.server.jdbc.repository.{AuthRepo, CourseClassesRepo, EventsRepo, InstitutionRepo, InstitutionsRepo, PeopleRepo, PersonRepo, RolesRepo, TokenRepo}
import kornell.server.repository.TOs
import kornell.server.repository.TOs.{newUserHelloTO, newUserInfoTO}
import kornell.server.service.RegistrationEnrollmentService
import kornell.server.util.Conditional.toConditional
import kornell.server.util.Settings._
import kornell.server.util.{AccessDeniedErr, EmailService}

import scala.collection.JavaConverters.asScalaBufferConverter

//TODO Person/People Resource
@Path("user")
class UserResource(private val authRepo: AuthRepo) {
  def this() = this(AuthRepo())

  val buildNumber: String = BUILD_NUM.getOpt.orElse("development_build").get

  @GET
  @Path("login")
  @Produces(Array(UserHelloTO.TYPE))
  def get(@Context req: HttpServletRequest,
    @QueryParam("name") name: String,
    @QueryParam("hostName") hostName: String): UserHelloTO = {
    val userHello = newUserHelloTO

    var institution = {
      if (name != null) InstitutionsRepo.getByName(name)
      else if (hostName != null) InstitutionsRepo.getByHostName(hostName)
      else None
    }
    if (institution.isEmpty && req.getHeader("Referer") != null) {
      val refererUrl = new URL(req.getHeader("Referer"))
      institution = InstitutionsRepo.getByHostName(refererUrl.getHost)
    }

    if (institution.isEmpty) {
      if (StringUtils.isSome(name) || StringUtils.isSome(hostName)) {
        throw new EntityNotFoundException("unknownInstitution")
      }
    } else {
      userHello.setInstitution(institution.get)
    }

    val auth = req.getHeader("X-KNL-TOKEN")

    val token = TokenRepo().checkToken(auth)
    if (token.isDefined) {
      val person = PersonRepo(token.get.getPersonUUID).first.orNull
      userHello.setUserInfoTO(getUser(person).orNull)
      userHello.setCourseClassesTO(CourseClassesRepo.byPersonAndInstitution(person.getUUID, person.getInstitutionUUID))

      if (institution.isDefined && person.getInstitutionUUID != institution.get.getUUID) {
        throw new UnauthorizedAccessException("personDoesNotBelongToInstitution")
      }
    }

    userHello.setBuildNumber(buildNumber)

    userHello
  }

  def getUser(person: Person): Option[UserInfoTO] = {
    val user = newUserInfoTO
    val username = PersonRepo(person.getUUID).getUsername
    user.setUsername(username)
    user.setPerson(person)
    user.setLastPlaceVisited(person.getLastPlaceVisited)
    val roleTOs = new RolesRepo().getUserRoles(person.getUUID, RoleCategory.BIND_DEFAULT)
    user.setRoles(roleTOs.getRoleTOs)
    if (RegistrationType.username.equals(person.getRegistrationType)) {
      user.setInstitutionRegistrationPrefix(InstitutionRepo(person.getInstitutionUUID).getInstitutionRegistrationPrefixes.getInstitutionRegistrationPrefixes
        .asScala.filter(irp => irp.getUUID.equals(person.getInstitutionRegistrationPrefixUUID)).head)
    }

    Option(user)
  }

  @GET
  @Path("get/{personUUID}")
  @Produces(Array(UserInfoTO.TYPE))
  def getByPersonUUID(implicit @Context sc: SecurityContext,
    @PathParam("personUUID") personUUID: String): Option[UserInfoTO] = {
      val user = newUserInfoTO
      val person = PersonRepo(personUUID).first.get
      if (person != null) {
        user.setPerson(person)
        user.setUsername(PersonRepo(person.getUUID).getUsername)
        if (RegistrationType.username.equals(person.getRegistrationType)) {
          user.setInstitutionRegistrationPrefix(InstitutionRepo(person.getInstitutionUUID).getInstitutionRegistrationPrefixes.getInstitutionRegistrationPrefixes
            .asScala.filter(irp => irp.getUUID.equals(person.getInstitutionRegistrationPrefixUUID)).head)
        }
        Option(user)
      } else {
        throw new EntityNotFoundException("personNotFound")
      }
    }.requiring(PersonRepo(getAuthenticatedPersonUUID).hasPowerOver(personUUID), AccessDeniedErr()).get

  @GET
  @Path("check/{institutionUUID}/{username}")
  @Produces(Array(UserInfoTO.TYPE))
  def checkUsernameAndEmail(@PathParam("username") username: String,
    @PathParam("institutionUUID") institutionUUID: String): Option[UserInfoTO] = {
    val user = newUserInfoTO
    //verify if there's a password set for this email
    if (authRepo.hasPassword(institutionUUID, username))
      user.setUsername(username)
    Option(user)
  }

  @GET
  @Path("requestPasswordChange/{email}/{institutionName}")
  def requestPasswordChange(@PathParam("email") email: String,
    @PathParam("institutionName") institutionName: String): Response = {
    val institution = InstitutionsRepo.getByName(institutionName)
    val person = PeopleRepo.getByEmail(institution.get.getUUID, email)
    if (person.isDefined && institution.isDefined) {
      val requestPasswordChangeUUID = UUID.randomUUID.toString

      AuthRepo().getUsernameByPersonUUID(person.get.getUUID) match {
        case Some(_) => authRepo.updateRequestPasswordChangeUUID(person.get.getUUID, requestPasswordChangeUUID)
        case None => authRepo.setPlainPassword(institution.get.getUUID, person.get.getUUID, person.get.getEmail, null, false, requestPasswordChangeUUID)
      }

      EmailService.sendEmailRequestPasswordChange(person.get, institution.get, requestPasswordChangeUUID)
    } else {
      throw new EntityNotFoundException("personOrInstitutionNotFound")
    }
    Response.noContent.build
  }

  @PUT
  @Path("resetPassword/{passwordChangeUUID}")
  @Produces(Array(UserInfoTO.TYPE))
  def resetPassword(@PathParam("passwordChangeUUID") passwordChangeUUID: String, password: String): Option[UserInfoTO] = {
    val person = authRepo.getPersonByPasswordChangeUUID(passwordChangeUUID)
    if (person.isDefined) {
      PersonRepo(person.get.getUUID).setPassword(password, false)

      //log entity change
      EventsRepo.logEntityChange(person.get.getInstitutionUUID, AuditedEntityType.password, person.get.getUUID, null, null, person.get.getUUID)

      val user = newUserInfoTO
      user.setUsername(person.get.getEmail)
      Option(user)
    } else {
      throw new UnauthorizedAccessException("passwordChangeFailed")
    }
  }

  @PUT
  @Path("changePassword/{targetPersonUUID}/")
  def changePassword(implicit @Context sc: SecurityContext,
    @PathParam("targetPersonUUID") targetPersonUUID: String, password: String): Response = {
    authRepo.withPerson { p =>
      if (!PersonRepo(p.getUUID).hasPowerOver(targetPersonUUID))
        throw new UnauthorizedAccessException("passwordChangeDenied")
      else {
        val targetPersonRepo = PersonRepo(targetPersonUUID)
        targetPersonRepo.setPassword(password, p.getUUID != targetPersonUUID)

        //log entity change
        EventsRepo.logEntityChange(targetPersonRepo.get.getInstitutionUUID, AuditedEntityType.password, targetPersonUUID, null, null)
      }
    }
    Response.noContent.build
  }

  //Used when user has the forcePasswordUpdate flag on his account
  @PUT
  @Path("updatePassword/{username}")
  @Produces(Array(UserInfoTO.TYPE))
  def updatePassword(@PathParam("username") username: String, password: String): Option[UserInfoTO] = {
    val person = authRepo.getPersonByUsernameAndPasswordUpdateFlag(username)
    if (person.isDefined) {
      PersonRepo(person.get.getUUID).updatePassword(person.get.getUUID, password, true)

      //log entity change
      EventsRepo.logEntityChange(person.get.getInstitutionUUID, AuditedEntityType.password, person.get.getUUID, null, null, person.get.getUUID)

      val user = newUserInfoTO
      user.setUsername(person.get.getEmail)
      Option(user)
    } else {
      throw new UnauthorizedAccessException("passwordChangeFailed")
    }
  }

  @GET
  @Path("hasPowerOver/{targetPersonUUID}")
  @Produces(Array("application/boolean"))
  def changePassword(implicit @Context sc: SecurityContext,
    @PathParam("targetPersonUUID") targetPersonUUID: String): Boolean = {
    authRepo.withPerson { p =>
      PersonRepo(p.getUUID).hasPowerOver(targetPersonUUID)
    }
  }

  @PUT
  @Path("registrationRequest")
  @Consumes(Array(RegistrationRequestTO.TYPE))
  @Produces(Array(UserInfoTO.TYPE))
  def createUser(regReq: RegistrationRequestTO): UserInfoTO = RegistrationEnrollmentService.userRequestRegistration(regReq)

  @PUT
  @Path("{personUUID}")
  @Consumes(Array(UserInfoTO.TYPE))
  @Produces(Array(UserInfoTO.TYPE))
  def update(implicit @Context sc: SecurityContext,
    userInfo: UserInfoTO,
    @PathParam("personUUID") personUUID: String): UserInfoTO = authRepo.withPerson { p =>
    if (userInfo != null) {
      if (!PersonRepo(p.getUUID).hasPowerOver(personUUID)) {
        throw new UnauthorizedAccessException("passwordChangeDenied")
      } else {
        val from = PersonRepo(personUUID).first.get

        PersonRepo(personUUID).update(userInfo.getPerson)

        //log entity change
        EventsRepo.logEntityChange(p.getInstitutionUUID, AuditedEntityType.person, personUUID, from, userInfo.getPerson)
        val roleTOs = new RolesRepo().getUserRoles(personUUID, RoleCategory.BIND_DEFAULT)
        userInfo.setRoles(roleTOs.getRoleTOs)
        userInfo
      }
    } else {
      throw new EntityNotFoundException("missingPayload")
    }
  }

  @PUT
  @Path("acceptTerms")
  @Consumes(Array("text/plain"))
  @Produces(Array(UserInfoTO.TYPE))
  def acceptTerms(): Option[UserInfoTO] = AuthRepo().withPerson { p =>
    val from = PersonRepo(p.getUUID).first.get

    PersonRepo(p.getUUID).acceptTerms(from)

    val to = PersonRepo(p.getUUID).first.get

    //log entity change
    EventsRepo.logEntityChange(p.getInstitutionUUID, AuditedEntityType.person, p.getUUID, from, to)

    getUser(p)
  }

  def createUser(institutionUUID: String, fullName: String, email: String, cpf: String, username: String, password: String): String = {
    val regreq = TOs.newRegistrationRequestTO(institutionUUID, fullName, email, password, cpf, username, RegistrationType.email)
    createUser(regreq).getPerson.getUUID
  }
}

object UserResource {
  def apply(authRepo: AuthRepo): UserResource = new UserResource(AuthRepo())
  def apply(): UserResource = apply(AuthRepo())
}
