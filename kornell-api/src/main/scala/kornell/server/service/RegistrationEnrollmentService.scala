package kornell.server.service

import kornell.core.entity._
import kornell.core.entity.role.RoleCategory
import kornell.core.error.exception.EntityConflictException
import kornell.core.to.{EnrollmentRequestTO, EnrollmentRequestsTO, RegistrationRequestTO, UserInfoTO}
import kornell.core.util.{StringUtils, UUID}
import kornell.server.api.ActomResource
import kornell.server.jdbc.repository.{CourseClassRepo, CourseVersionRepo, EnrollmentsRepo, EventsRepo, InstitutionRepo, InstitutionsRepo, PeopleRepo, PersonRepo, RolesRepo}
import kornell.server.repository.TOs._
import kornell.server.util.EmailService
import java.util.logging.Logger


import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object RegistrationEnrollmentService {

  val logger: Logger = Logger.getLogger(classOf[RegistrationEnrollmentService].getName)

  def deanRequestEnrollments(enrollmentRequests: EnrollmentRequestsTO, dean: Person): Unit = {
    val courseClassUUID = enrollmentRequests.getEnrollmentRequests.get(0).getCourseClassUUID
    val courseClass = CourseClassRepo(courseClassUUID).get
    val institution = InstitutionRepo(courseClass.getInstitutionUUID).get
    val currentEnrollmentCount = EnrollmentsRepo.byCourseClass(courseClassUUID).getCount
    if ((currentEnrollmentCount + enrollmentRequests.getEnrollmentRequests.size) > courseClass.getMaxEnrollments) {
      throw new EntityConflictException("tooManyEnrollments")
    }
    enrollmentRequests.getEnrollmentRequests.asScala.foreach(e => deanRequestEnrollment(e, dean, EnrollmentSource.WEBSITE))
    //send email for over 50 enrollments on DEFAULT institutions, but 3 on DASHBOARD institutions
    //@TODO refactor this after improvements on child enrollment generation with hundreds of scorm attributes
    if (enrollmentRequests.getEnrollmentRequests.size > 50 ||
      (enrollmentRequests.getEnrollmentRequests.size > 3 && InstitutionType.DASHBOARD.equals(institution.getInstitutionType)))
      EmailService.sendEmailBatchEnrollment(dean, InstitutionsRepo.getByUUID(dean.getInstitutionUUID).get, CourseClassRepo(enrollmentRequests.getEnrollmentRequests.get(0).getCourseClassUUID).get)
  }

  def isInvalidRequestEnrollment(enrollmentRequest: EnrollmentRequestTO, deanUUID: String): Boolean = {
    val roles = new RolesRepo().getUserRoles(deanUUID, RoleCategory.BIND_DEFAULT).getRoleTOs
    !(RoleCategory.isPlatformAdmin(roles, enrollmentRequest.getInstitutionUUID) ||
      RoleCategory.isInstitutionAdmin(roles, enrollmentRequest.getInstitutionUUID) ||
      RoleCategory.isCourseClassAdmin(roles, enrollmentRequest.getCourseClassUUID))
  }

  def postbackRequestEnrollment(req: EnrollmentRequestTO, payload: String): Unit = {
    deanRequestEnrollment(req, null, EnrollmentSource.POSTBACK, payload)
  }

  private def deanRequestEnrollment(req: EnrollmentRequestTO, dean: Person, enrollmentSource: EnrollmentSource, notes: String = null): Unit = {
    req.setEnrollmentSource(enrollmentSource)
    req.setUsername(req.getUsername.trim)
    PeopleRepo.get(req.getInstitutionUUID, req.getUsername) match {
      case Some(one) => deanEnrollExistingPerson(one, req, dean, notes)
      case None => deanEnrollNewPerson(req, dean, notes)
    }
  }

  private def deanEnrollNewPerson(enrollmentRequest: EnrollmentRequestTO, dean: Person, notes: String = null): Unit = {
    if (!enrollmentRequest.isCancelEnrollment) {
      val person = enrollmentRequest.getRegistrationType match {
        case RegistrationType.email => PeopleRepo.createPerson(enrollmentRequest.getInstitutionUUID, enrollmentRequest.getUsername, enrollmentRequest.getFullName)
        case RegistrationType.cpf => PeopleRepo.createPerson(enrollmentRequest.getInstitutionUUID, enrollmentRequest.getEmail, enrollmentRequest.getFullName, enrollmentRequest.getUsername)
        case RegistrationType.username => PeopleRepo.createPersonUsername(enrollmentRequest.getInstitutionUUID, enrollmentRequest.getUsername, enrollmentRequest.getFullName, enrollmentRequest.getInstitutionRegistrationPrefixUUID)
      }
      val personRepo = PersonRepo(person.getUUID)
      if (!enrollmentRequest.getRegistrationType.equals(RegistrationType.email)) {
        personRepo.setPassword(enrollmentRequest.getPassword, true)
      }
      val enrollment = createEnrollment(personRepo.get.getUUID, enrollmentRequest.getCourseClassUUID, null, EnrollmentState.enrolled, dean, null, enrollmentRequest.getEnrollmentSource, notes)
      if (enrollmentRequest.getCourseVersionUUID != null) {
        createChildEnrollments(enrollment, enrollmentRequest.getCourseVersionUUID, person.getUUID, dean)
      }
    }
  }

  private def deanEnrollExistingPerson(person: Person, enrollmentRequest: EnrollmentRequestTO, dean: Person, notes: String = null): Unit = {
    logger.fine(person.getFullName + " " + RegistrationEnrollmentService)
    val personRepo = PersonRepo(person.getUUID)
    if (enrollmentRequest.getCourseVersionUUID == null) {
      EnrollmentsRepo.byCourseClassAndPerson(enrollmentRequest.getCourseClassUUID, person.getUUID, getDeleted = true) match {
        case Some(enrollment) => deanUpdateExistingEnrollment(person, enrollment, enrollmentRequest.getInstitutionUUID, dean, enrollmentRequest.isCancelEnrollment)
        case None => createEnrollment(person.getUUID, enrollmentRequest.getCourseClassUUID, null, EnrollmentState.enrolled, dean, null, enrollmentRequest.getEnrollmentSource, notes)
      }
    } else {
      val courseVersion = CourseVersionRepo(enrollmentRequest.getCourseVersionUUID).get
      if (courseVersion.getParentVersionUUID != null) {
        throw new EntityConflictException("cannotEnrollOnChildVersion")
      }
      EnrollmentsRepo.byCourseClassAndPerson(enrollmentRequest.getCourseClassUUID, person.getUUID, getDeleted = true) match {
        case Some(enrollment) => deanUpdateExistingEnrollment(person, enrollment, enrollmentRequest.getInstitutionUUID, dean, enrollmentRequest.isCancelEnrollment)
        case None => {
          val enrollment = createEnrollment(person.getUUID, enrollmentRequest.getCourseClassUUID, null, EnrollmentState.enrolled, dean, null, enrollmentRequest.getEnrollmentSource, null)
          createChildEnrollments(enrollment, enrollmentRequest.getCourseVersionUUID, person.getUUID, dean)
        }
      }
    }
    //if there's no fullName set, get it from the enrollment request
    if (StringUtils.isNone(person.getFullName)) {
      person.setFullName(enrollmentRequest.getFullName)
      personRepo.update(person)
    }
  }

  val SEP = ":"

  type EnrollmentUUID = String
  type ActomKey = String
  type Props = Map[String, String]
  type ActomId = (EnrollmentUUID, ActomKey)

  //TODO: This method is generating ~1000 lines for enrollment in CVS course, consider using references instead of copies
  private def createChildEnrollments(enrollment: Enrollment, courseVersionUUID: String, personUUID: String, dean: Person): Unit = {
    val enrollmentMap = collection.mutable.Map[String, String]()
    val enrolls = new ListBuffer[String]()
    var moduleCounter = 0
    val parentEnrollmentUUID = enrollment.getUUID
    enrollmentMap("knl.dashboard.enrollmentUUID") = parentEnrollmentUUID
    enrolls += parentEnrollmentUUID

    CourseVersionRepo(courseVersionUUID).getChildren.foreach(cv => {
      for (i <- 0 until cv.getInstanceCount) {
        val childEnrollment = createEnrollment(personUUID, null, cv.getUUID, EnrollmentState.enrolled, dean, parentEnrollmentUUID, enrollment.getEnrollmentSource, null)
        val childUUID = childEnrollment.getUUID
        enrollmentMap(s"knl.module.${moduleCounter}.name") = cv.getLabel + SEP + i
        enrollmentMap(s"knl.module.${moduleCounter}.index") = s"$i"
        enrollmentMap(s"knl.module.${moduleCounter}.label") = cv.getLabel
        enrollmentMap(s"knl.module.${moduleCounter}.enrollmentUUID") = childUUID
        enrolls += childUUID
        moduleCounter += 1
      }
    })
    enrollmentMap("knl.module._count") = moduleCounter.toString
    val enrollmentsJMap = enrollmentMap.asJava
    for (uuid <- enrolls) {
      //TODO: Support MultiSCO
      val actomResource = new ActomResource(uuid, "index.html")
      actomResource.putValues(enrollmentsJMap)
    }
  }

  private def deanUpdateExistingEnrollment(person: Person, enrollment: Enrollment, institutionUUID: String, dean: Person, cancelEnrollment: Boolean): Unit = {
    val enrollerUUID = if (dean == null) null else dean.getUUID
    if (cancelEnrollment && !EnrollmentState.cancelled.equals(enrollment.getState))
      EventsRepo.logEnrollmentStateChanged(UUID.random, enrollerUUID, enrollment.getUUID, enrollment.getState, EnrollmentState.cancelled, enrollment.getCourseVersionUUID == null, null)
    else if (!cancelEnrollment && (EnrollmentState.cancelled.equals(enrollment.getState)
      || EnrollmentState.deleted.equals(enrollment.getState)
      || EnrollmentState.requested.equals(enrollment.getState)
      || EnrollmentState.denied.equals(enrollment.getState))) {
      EventsRepo.logEnrollmentStateChanged(UUID.random, enrollerUUID, enrollment.getUUID, enrollment.getState, EnrollmentState.enrolled, enrollment.getCourseVersionUUID == null, null)
    }
  }

  def userRequestRegistration(regReq: RegistrationRequestTO): UserInfoTO = {
    val email = regReq.getEmail
    val username = regReq.getUsername
    val cpf = regReq.getCPF

    PeopleRepo.get(regReq.getInstitutionUUID, username, cpf, email) match {
      case Some(one) => userUpdateExistingPerson(regReq, one)
      case None => userCreateNewPerson(regReq)
    }
  }

  private def userCreateNewPerson(regReq: RegistrationRequestTO): UserInfoTO = {
    val person = PeopleRepo.createPerson(regReq.getInstitutionUUID, regReq.getEmail, regReq.getFullName, regReq.getCPF)

    val user = newUserInfoTO
    val username = usernameOf(regReq)
    user.setPerson(person)
    user.setUsername(username)
    PersonRepo(person.getUUID).setPassword(regReq.getPassword)
    user
  }

  def usernameOf(regReq: RegistrationRequestTO): String = StringUtils.opt(regReq.getUsername)
    .orElse(regReq.getCPF)
    .orElse(regReq.getEmail)
    .getOrNull

  private def userUpdateExistingPerson(regReq: RegistrationRequestTO, personOld: Person): UserInfoTO = {
    val personRepo = PersonRepo(personOld.getUUID)

    //update the user's info
    val person = personRepo.get
    person.setFullName(regReq.getFullName)
    if (regReq.getEmail != null)
      person.setEmail(regReq.getEmail)
    if (regReq.getCPF != null)
      person.setCPF(regReq.getCPF)
    person.setRegistrationType(regReq.getRegistrationType)
    personRepo.update(person)

    val username = usernameOf(regReq)

    val user = newUserInfoTO
    user.setPerson(person)
    user.setUsername(username)

    personRepo.setPassword(regReq.getPassword)

    user
  }

  private def createEnrollment(personUUID: String, courseClassUUID: String, courseVersionUUID: String, enrollmentState: EnrollmentState, enroller: Person, parentEnrollmentUUID: String = null, enrollmentSource: EnrollmentSource = null, notes: String = null): Enrollment = {
    val enrollerUUID = if (enroller == null) null else enroller.getUUID

    val enrollment = EnrollmentsRepo.create(
      courseClassUUID = courseClassUUID,
      personUUID = personUUID,
      enrollmentState = EnrollmentState.notEnrolled,
      courseVersionUUID = courseVersionUUID,
      parentEnrollmentUUID = parentEnrollmentUUID,
      enrollmentSource = enrollmentSource)
    EventsRepo.logEnrollmentStateChanged(
      UUID.random, enrollerUUID,
      enrollment.getUUID, enrollment.getState, enrollmentState, courseVersionUUID == null, notes)
    enrollment
  }
}
