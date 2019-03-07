package kornell.server.jdbc

import java.sql.ResultSet
import java.util.logging.Logger

import kornell.core.entity._
import kornell.core.entity.role.{RoleCategory, RoleType}
import kornell.core.event.EntityChanged
import kornell.core.to._
import kornell.server.repository.Entities._
import kornell.server.repository.{Entities, TOs}
import kornell.server.repository.TOs._

/**
 * Classes in this package are Data Access Objects for JDBC Databases
 *
 * This is the naming convention for methods in this package:
 *
 * first() => Return Option[T], when the result may not exist
 * get() => Returns T, presuming the result exists
 * find() => Return Collection[T], as the result of a query
 */
package object repository {

  val logger: Logger = Logger.getLogger("kornell.server.jdbc.repository")

  //TODO: Move converters to their repos
  implicit def toInstitution(rs: ResultSet): Institution =
    newInstitution(rs.getString("uuid"),
      rs.getString("name"),
      rs.getString("fullName"),
      rs.getString("terms"),
      rs.getString("baseURL"),
      rs.getBoolean("demandsPersonContactDetails"),
      rs.getBoolean("validatePersonContactDetails"),
      rs.getBoolean("allowRegistration"),
      rs.getBoolean("allowRegistrationByUsername"),
      rs.getDate("activatedAt"),
      rs.getString("skin"),
      BillingType.valueOf(rs.getString("billingType")),
      InstitutionType.valueOf(rs.getString("institutionType")),
      rs.getString("dashboardVersionUUID"),
      rs.getBoolean("useEmailWhitelist"),
      rs.getString("timeZone"),
      rs.getString("institutionSupportEmail"),
      rs.getBoolean("advancedMode"),
      rs.getBoolean("notifyInstitutionAdmins"),
      rs.getString("allowedLanguages"),
      rs.getBoolean("disabled"),
      rs.getBoolean("enforceSequentialProgress"),
      rs.getBoolean("showPlatformPanel"))

  implicit def toContentRepository(rs: ResultSet): ContentRepository =
    newContentRepository(rs.getString("uuid"),
      RepositoryType.valueOf(rs.getString("repositoryType")),
      rs.getString("accessKeyId"),
      rs.getString("secretAccessKey"),
      rs.getString("bucketName"),
      rs.getString("prefix"),
      rs.getString("region"),
      rs.getString("institutionUUID"),
      rs.getString("path"),
      rs.getString("accountName"),
      rs.getString("accountKey"),
      rs.getString("container")
    )

  implicit def toCourseClass(r: ResultSet): CourseClass =
    newCourseClass(r.getString("uuid"), r.getString("name"),
      r.getString("courseVersionUUID"), r.getString("institutionUUID"),
      r.getBigDecimal("requiredScore"), r.getBoolean("publicClass"),
      r.getBoolean("overrideEnrollments"),
      r.getBoolean("invisible"), r.getInt("maxEnrollments"),
      r.getDate("createdAt"), r.getString("createdBy"),
      EntityState.valueOf(r.getString("state")),
      RegistrationType.valueOf(r.getString("registrationType")),
      r.getString("institutionRegistrationPrefixUUID"), r.getBoolean("courseClassChatEnabled"),
      r.getBoolean("chatDockEnabled"), r.getBoolean("allowBatchCancellation"),
      r.getBoolean("tutorChatEnabled"), r.getBoolean("approveEnrollmentsAutomatically"),
      r.getDate("startDate"), r.getString("ecommerceIdentifier"),
      r.getString("thumbUrl"), r.getBoolean("sandbox"),
      r.getInt("enrollmentExpiryDays"))

  implicit def toCourse(rs: ResultSet): Course = newCourse(
    rs.getString("uuid"),
    rs.getString("code"),
    rs.getString("name"),
    rs.getString("description"),
    rs.getString("infoJson"),
    EntityState.valueOf(rs.getString("state")),
    rs.getString("institutionUUID"),
    rs.getBoolean("childCourse"),
    rs.getString("thumbUrl"),
    ContentSpec.valueOf(rs.getString("contentSpec")))

  implicit def toCourseTO(rs: ResultSet): CourseTO = newCourseTO(
    toCourse(rs))

  implicit def toCourseVersion(rs: ResultSet): CourseVersion = newCourseVersion(
    rs.getString("uuid"),
    rs.getString("name"),
    rs.getString("courseUUID"),
    rs.getDate("versionCreatedAt"),
    rs.getString("distributionPrefix"),
    EntityState.valueOf(rs.getString("state")),
    rs.getBoolean("disabled"),
    rs.getString("parentVersionUUID"),
    rs.getInt("instanceCount"),
    rs.getString("classroomJson"),
    rs.getString("classroomJsonPublished"),
    rs.getString("label"),
    rs.getString("thumbUrl"))

  implicit def toCourseClassTO(rs: ResultSet): CourseClassTO = {
    val course = newCourse(
      rs.getString("courseUUID"),
      rs.getString("code"),
      rs.getString("name"),
      rs.getString("description"),
      rs.getString("infoJson"),
      EntityState.valueOf(rs.getString("state")),
      rs.getString("institutionUUID"),
      rs.getBoolean("childCourse"),
      rs.getString("courseThumbUrl"),
      ContentSpec.valueOf(rs.getString("contentSpec")))

    val version = newCourseVersion(
      rs.getString("courseVersionUUID"),
      rs.getString("courseVersionName"),
      rs.getString("courseUUID"),
      rs.getDate("versionCreatedAt"),
      rs.getString("distributionPrefix"),
      EntityState.valueOf(rs.getString("courseVersionState")),
      rs.getBoolean("disabled"),
      rs.getString("parentVersionUUID"),
      rs.getInt("instanceCount"),
      rs.getString("classroomJson"),
      rs.getString("classroomJsonPublished"),
      rs.getString("label"),
      rs.getString("courseVersionThumbUrl"))

    val clazz = newCourseClass(
      rs.getString("courseClassUUID"),
      rs.getString("courseClassName"),
      rs.getString("courseVersionUUID"),
      rs.getString("institutionUUID"),
      rs.getBigDecimal("requiredScore"),
      rs.getBoolean("publicClass"),
      rs.getBoolean("overrideEnrollments"),
      rs.getBoolean("invisible"),
      rs.getInt("maxEnrollments"),
      rs.getDate("createdAt"),
      rs.getString("createdBy"),
      EntityState.valueOf(rs.getString("courseClassState")),
      RegistrationType.valueOf(rs.getString("registrationType")),
      rs.getString("institutionRegistrationPrefixUUID"),
      rs.getBoolean("courseClassChatEnabled"),
      rs.getBoolean("chatDockEnabled"),
      rs.getBoolean("allowBatchCancellation"),
      rs.getBoolean("tutorChatEnabled"),
      rs.getBoolean("approveEnrollmentsAutomatically"),
      null,
      rs.getString("ecommerceIdentifier"),
      rs.getString("courseClassThumbUrl"),
      rs.getBoolean("sandbox"),
      rs.getInt("enrollmentExpiryDays"))

    TOs.newCourseClassTO(course, version, clazz, rs.getString("institutionRegistrationPrefixName"))
  }

  implicit def toCourseVersionTO(rs: ResultSet): CourseVersionTO = {
    val courseVersion = newCourseVersion(
      rs.getString("courseVersionUUID"),
      rs.getString("courseVersionName"),
      rs.getString("courseUUID"),
      rs.getDate("versionCreatedAt"),
      rs.getString("distributionPrefix"),
      EntityState.valueOf(rs.getString("courseVersionState")),
      rs.getBoolean("courseVersionDisabled"),
      rs.getString("parentVersionUUID"),
      rs.getInt("instanceCount"),
      rs.getString("classroomJson"),
      rs.getString("classroomJsonPublished"),
      rs.getString("label"),
      rs.getString("courseVersionThumbUrl"))

    val course = newCourse(
      rs.getString("courseUUID"),
      rs.getString("courseCode"),
      rs.getString("courseName"),
      rs.getString("courseDescription"),
      rs.getString("infoJson"),
      EntityState.valueOf(rs.getString("courseState")),
      rs.getString("institutionUUID"),
      rs.getBoolean("childCourse"),
      rs.getString("courseThumbUrl"),
      ContentSpec.valueOf(rs.getString("contentSpec")))

    TOs.newCourseVersionTO(course, courseVersion)
  }

  implicit def toEnrollment(rs: ResultSet): Enrollment = {
    newEnrollment(
      rs.getString("uuid"),
      rs.getTimestamp("enrolledOn"),
      rs.getString("courseClassUUID"),
      rs.getString("personUUID"),
      rs.getInt("progress"),
      rs.getString("notes"),
      EnrollmentState.valueOf(rs.getString("state")),
      rs.getTimestamp("lastProgressUpdate"),
      Option(rs.getString("assessment"))
        .map(Assessment.valueOf)
        .orNull,
      rs.getTimestamp("lastAssessmentUpdate"),
      rs.getBigDecimal("assessmentScore"),
      rs.getTimestamp("certifiedAt"),
      rs.getString("courseVersionUUID"),
      rs.getString("parentEnrollmentUUID"),
      rs.getDate("startDate"),
      rs.getDate("endDate"),
      rs.getBigDecimal("preAssessmentScore"),
      rs.getBigDecimal("postAssessmentScore"),
      EnrollmentSource.valueOf(rs.getString("enrollmentSource")))
  }

  implicit def toEnrollmentTO(rs: ResultSet): EnrollmentTO = {
    val enrollment = newEnrollment(
      rs.getString("uuid"),
      rs.getDate("enrolledOn"),
      rs.getString("courseClassUUID"),
      rs.getString("personUUID"),
      rs.getInt("progress"),
      rs.getString("notes"),
      EnrollmentState.valueOf(rs.getString("state")),
      rs.getTimestamp("lastProgressUpdate"),
      Option(rs.getString("assessment"))
        .map(Assessment.valueOf)
        .orNull,
      rs.getTimestamp("lastAssessmentUpdate"),
      rs.getBigDecimal("assessmentScore"),
      rs.getTimestamp("certifiedAt"),
      null, null, null,
      rs.getDate("endDate"))

    TOs.newEnrollmentTO(enrollment, rs.getString("personUUID"), rs.getString("fullName"), rs.getString("username"))
  }

  implicit def toPerson(rs: ResultSet): Person = newPerson(
    rs.getString("uuid"),
    rs.getString("fullName"),
    rs.getString("lastPlaceVisited"),
    rs.getString("email"),
    rs.getString("company"),
    rs.getString("title"),
    rs.getString("sex"),
    rs.getDate("birthDate"),
    rs.getString("confirmation"),
    rs.getString("telephone"),
    rs.getString("country"),
    rs.getString("state"),
    rs.getString("city"),
    rs.getString("addressLine1"),
    rs.getString("addressLine2"),
    rs.getString("postalCode"),
    rs.getString("cpf"),
    rs.getString("institutionUUID"),
    rs.getTimestamp("termsAcceptedOn"),
    RegistrationType.valueOf(rs.getString("registrationType")),
    rs.getString("institutionRegistrationPrefixUUID"),
    rs.getBoolean("receiveEmailCommunication"),
    rs.getBoolean("forcePasswordUpdate"))

  implicit def toPersonTO(rs: ResultSet): PersonTO = newPersonTO(toPerson(rs),
    rs.getString("username"))

  implicit def toRole(rs: java.sql.ResultSet): kornell.core.entity.role.Role = {
    val roleType = RoleType.valueOf(rs.getString("role"))
    val role = roleType match {
      case RoleType.user => Entities.newUserRole
      case RoleType.platformAdmin => Entities.newRoleAsPlatformAdmin(rs.getString("personUUID"), rs.getString("institutionUUID"))
      case RoleType.institutionAdmin => Entities.newInstitutionAdminRole(rs.getString("personUUID"), rs.getString("institutionUUID"))
      case RoleType.courseClassAdmin => Entities.newCourseClassAdminRole(rs.getString("personUUID"), rs.getString("courseClassUUID"))
      case RoleType.tutor => Entities.newTutorRole(rs.getString("personUUID"), rs.getString("courseClassUUID"))
      case RoleType.courseClassObserver => Entities.newCourseClassObserverRole(rs.getString("personUUID"), rs.getString("courseClassUUID"))
      case RoleType.controlPanelAdmin => Entities.newControlPanelAdminRole(rs.getString("personUUID"))
      case RoleType.publisher => Entities.newPublisherRole(rs.getString("personUUID"), rs.getString("institutionUUID"))
      case RoleType.institutionCourseClassesAdmin => Entities.newInstitutionCourseClassesAdminRole(rs.getString("personUUID"), rs.getString("institutionUUID"))
      case RoleType.institutionCourseClassesObserver => Entities.newInstitutionCourseClassesAdminRole(rs.getString("personUUID"), rs.getString("institutionUUID"))
    }
    role
  }

  implicit def toRoleTO(rs: java.sql.ResultSet, bindMode: String): RoleTO = {
    val role = toRole(rs)
    TOs.newRoleTO(role, {
      if (role != null && RoleCategory.BIND_WITH_PERSON.equals(bindMode))
        PeopleRepo.getByUUID(role.getPersonUUID).get
      else
        null
    }, rs.getString("username"))
  }

  implicit def toInstitutionRegistrationPrefix(rs: ResultSet): InstitutionRegistrationPrefix =
    newInstitutionRegistrationPrefix(rs.getString("uuid"),
      rs.getString("name"),
      rs.getString("institutionUUID"),
      rs.getBoolean("showEmailOnProfile"),
      rs.getBoolean("showCPFOnProfile"),
      rs.getBoolean("showContactInformationOnProfile"))

  implicit def toUnreadChatThreadTO(rs: ResultSet): UnreadChatThreadTO = newUnreadChatThreadTO(
    rs.getString("unreadMessages"),
    rs.getString("chatThreadUUID"),
    rs.getString("threadType"),
    rs.getString("creatorName"),
    rs.getString("entityUUID"),
    rs.getString("entityName"))

  implicit def toChatThreadMessageTO(rs: ResultSet): ChatThreadMessageTO = newChatThreadMessageTO(
    rs.getString("senderFullName"),
    if (rs.getString("senderRole") == null)
      RoleType.user
    else
      RoleType.valueOf(rs.getString("senderRole")),
    rs.getTimestamp("sentAt"),
    rs.getString("message"))

  implicit def toChatThreadParticipant(rs: ResultSet): ChatThreadParticipant = newChatThreadParticipant(
    rs.getString("uuid"),
    rs.getString("chatThreadUUID"),
    rs.getString("personUUID"),
    rs.getTimestamp("lastReadAt"),
    rs.getBoolean("active"),
    rs.getTimestamp("lastJoinDate"),
    rs.getInt("unreadCount"))

  implicit def toChatThread(rs: ResultSet): ChatThread = newChatThread(
    rs.getString("uuid"),
    rs.getTimestamp("createdAt"),
    rs.getString("institutionUUID"),
    rs.getString("courseClassUUID"),
    rs.getString("personUUID"),
    rs.getString("threadType"),
    rs.getBoolean("active"),
    rs.getTimestamp("lastSentAt"))

  implicit def toTokenTO(rs: ResultSet): TokenTO = newTokenTO(
    rs.getString("token"),
    rs.getTimestamp("expiry"),
    rs.getString("personUUID"),
    AuthClientType.valueOf(rs.getString("clientType")))

  implicit def toSimplePersonTO(rs: ResultSet): SimplePersonTO = newSimplePersonTO(
    rs.getString("uuid"),
    rs.getString("fullName"),
    rs.getString("username"))

  implicit def toDashboardLeaderboardItemTO(rs: ResultSet): DashboardLeaderboardItemTO = newDashboardLeaderboardItemTO(
    rs.getString("uuid"),
    rs.getString("fullName"),
    rs.getString("attribute"))

  implicit def toEntityChanged(rs: ResultSet): EntityChanged = newEntityChanged(
    rs.getString("uuid"),
    rs.getTimestamp("eventFiredAt"),
    rs.getString("institutionUUID"),
    rs.getString("personUUID"),
    AuditedEntityType.valueOf(rs.getString("entityType")),
    rs.getString("entityUUID"),
    rs.getString("fromValue"),
    rs.getString("toValue"),
    rs.getString("entityName"),
    rs.getString("fromPersonName"),
    rs.getString("fromUsername"))

  implicit def toCourseDetailsHint(rs: ResultSet): CourseDetailsHint = newCourseDetailsHint(
    rs.getString("uuid"),
    rs.getString("title"),
    rs.getString("text"),
    CourseDetailsEntityType.valueOf(rs.getString("entityType")),
    rs.getString("entityUUID"),
    rs.getInt("index"),
    rs.getString("fontAwesomeClassName"))

  implicit def toCourseDetailsLibrary(rs: ResultSet): CourseDetailsLibrary = newCourseDetailsLibrary(
    rs.getString("uuid"),
    rs.getString("title"),
    rs.getString("description"),
    CourseDetailsEntityType.valueOf(rs.getString("entityType")),
    rs.getString("entityUUID"),
    rs.getInt("index"),
    rs.getInt("size"),
    rs.getString("path"),
    rs.getTimestamp("uploadDate"),
    rs.getString("fontAwesomeClassName"))

  implicit def toCourseDetailsSection(rs: ResultSet): CourseDetailsSection = newCourseDetailsSection(
    rs.getString("uuid"),
    rs.getString("title"),
    rs.getString("text"),
    CourseDetailsEntityType.valueOf(rs.getString("entityType")),
    rs.getString("entityUUID"),
    rs.getInt("index"))

  implicit def toCertificateDetails(rs: ResultSet): CertificateDetails = newCertificateDetails(
    rs.getString("uuid"),
    rs.getString("bgImage"),
    CertificateType.valueOf(rs.getString("certificateType")),
    CourseDetailsEntityType.valueOf(rs.getString("entityType")),
    rs.getString("entityUUID"))

  implicit def toPostbackConfig(rs: ResultSet): PostbackConfig = newPostbackConfig(
    rs.getString("uuid"),
    rs.getString("institutionUUID"),
    PostbackType.valueOf(rs.getString("postbackType")),
    rs.getString("contents"))

  implicit def toEmailTemplate(rs: ResultSet): EmailTemplate = newEmailTemplate(
    rs.getString("uuid"),
    EmailTemplateType.valueOf(rs.getString("templateType")),
    rs.getString("locale"),
    rs.getString("title"),
    rs.getString("template"))

  implicit def toTrack(rs: ResultSet): Track = newTrack(
    rs.getString("uuid"),
    rs.getString("institutionUUID"),
    rs.getString("name"))

  implicit def toTrackEnrollment(rs: ResultSet): TrackEnrollment = newTrackEnrollment(
    rs.getString("uuid"),
    rs.getString("personUUID"),
    rs.getString("trackUUID"))

  implicit def toTrackItem(rs: ResultSet): TrackItem = newTrackItem(
    rs.getString("uuid"),
    rs.getString("courseVersionUUID"),
    rs.getString("trackUUID"),
    rs.getString("parentUUID"),
    rs.getInt("order"),
    rs.getBoolean("havingPreRequirements"),
    rs.getTimestamp("startDate"))
}
