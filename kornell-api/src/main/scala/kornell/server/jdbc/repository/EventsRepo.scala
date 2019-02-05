package kornell.server.jdbc.repository

import com.google.web.bindery.autobean.shared.{AutoBean, AutoBeanCodex, AutoBeanUtils}
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource
import kornell.core.entity.{AuditedEntityType, EnrollmentState, EntityState}
import kornell.core.error.exception.EntityConflictException
import kornell.core.event._
import kornell.core.to.EntityChangedEventsTO
import kornell.server.authentication.ThreadLocalAuthenticator
import kornell.server.jdbc.SQL.{SQLHelper, _}
import kornell.server.repository.TOs._
import kornell.server.util.{EmailService, Settings}
import org.joda.time.DateTime
import java.util.UUID

import scala.collection.JavaConverters._

object EventsRepo {

  val events: EventFactory = AutoBeanFactorySource.create(classOf[EventFactory])

  def newEnrollmentStateChanged: EnrollmentStateChanged = events.newEnrollmentStateChanged.as

  def logActomEntered(event: ActomEntered): Unit = {
    sql"""
    insert into ActomEntered(uuid,eventFiredAt,enrollmentUUID,actomKey)
    values(${UUID.randomUUID.toString},
         now(),
           ${event.getEnrollmentUUID},
       ${event.getActomKey})
  """.executeUpdate

    EnrollmentRepo(event.getEnrollmentUUID).updateProgressAndAssessment()
  }

  def logAttendanceSheetSigned(event: AttendanceSheetSigned): Unit = {
    val todayStart = DateTime.now.withTimeAtStartOfDay.toDate
    val todayEnd = DateTime.now.plusDays(1).withTimeAtStartOfDay.minusMillis(1).toDate
    // don't log more than once a day
    val attendanceSheetSignedUUID = sql"""
        select uuid from AttendanceSheetSigned
        where personUUID=${event.getPersonUUID}
        and institutionUUID=${event.getInstitutionUUID}
        and eventFiredAt between ${todayStart} and ${todayEnd}
      """.first
    if (attendanceSheetSignedUUID.isEmpty)
      sql"""
        insert into AttendanceSheetSigned(uuid,eventFiredAt,institutionUUID,personUUID)
        values(${UUID.randomUUID.toString}, now(), ${event.getInstitutionUUID}, ${event.getPersonUUID});
      """.executeUpdate

  }

  def logEnrollmentStateChanged(fromPersonUUID: String,
    enrollmentUUID: String, fromState: EnrollmentState, toState: EnrollmentState, sendEmail: Boolean, notes: String): Unit = {

    sql"""insert into EnrollmentStateChanged(uuid,eventFiredAt,personUUID,enrollmentUUID,fromState,toState,notes)
      values(${UUID.randomUUID.toString},
         now(),
         ${fromPersonUUID},
         ${enrollmentUUID},
         ${fromState.toString},
         ${toState.toString},
         ${notes});
    """.executeUpdate

    sql"""update Enrollment set state = ${toState.toString} where uuid = ${enrollmentUUID};
    """.executeUpdate

    EnrollmentsRepo.invalidateCache(enrollmentUUID)

    if (EnrollmentState.enrolled.equals(toState) && sendEmail) {
      val enrollment = EnrollmentRepo(enrollmentUUID).get
      val person = PersonRepo(enrollment.getPersonUUID).get
      val testMode = Settings.TEST_MODE.getOpt.orNull
      val notTestMode = !"true".equals(testMode)
      if (person.getEmail != null && notTestMode) {
        val courseClass = CourseClassesRepo(enrollment.getCourseClassUUID).get
        val course = CoursesRepo.byCourseClassUUID(courseClass.getUUID).get
        val institution = InstitutionsRepo.getByUUID(courseClass.getInstitutionUUID).get
        EmailService.sendEmailEnrolled(person, institution, course, enrollment, courseClass)
      }
    }
  }

  def logEnrollmentStateChanged(event: EnrollmentStateChanged): Unit =
    logEnrollmentStateChanged(event.getFromPersonUUID,
      event.getEnrollmentUUID, event.getFromState, event.getToState, true, null)

  def logCourseClassStateChanged(fromPersonUUID: String,
    courseClassUUID: String, fromState: EntityState, toState: EntityState): Unit = {

    sql"""insert into CourseClassStateChanged(uuid,eventFiredAt,personUUID,courseClassUUID,fromState,toState)
      values(${UUID.randomUUID.toString},
     now(),
         ${fromPersonUUID},
         ${courseClassUUID},
         ${fromState.toString},
     ${toState.toString})
    """.executeUpdate

    sql"""update CourseClass set state = ${toState.toString} where uuid = ${courseClassUUID}
    """.executeUpdate

  }

  def logCourseClassStateChanged(event: CourseClassStateChanged): Unit =
    logCourseClassStateChanged(event.getFromPersonUUID,
      event.getCourseClassUUID, event.getFromState, event.getToState)

  def logEnrollmentTransferred(event: EnrollmentTransferred): Unit = {
    if (EnrollmentRepo(event.getEnrollmentUUID).checkExistingEnrollment(event.getToCourseClassUUID)) {
      throw new EntityConflictException("userAlreadyEnrolledInClass")
    }
    sql"""insert into EnrollmentTransferred (uuid, personUUID, enrollmentUUID, fromCourseClassUUID, toCourseClassUUID, eventFiredAt)
        values (${UUID.randomUUID.toString},
        ${event.getFromPersonUUID},
        ${event.getEnrollmentUUID},
        ${event.getFromCourseClassUUID},
        ${event.getToCourseClassUUID},
        now())""".executeUpdate

    EnrollmentRepo(event.getEnrollmentUUID).transfer(event.getFromCourseClassUUID, event.getToCourseClassUUID)
  }

  def logEntityChange(institutionUUID: String, auditedEntityType: AuditedEntityType, entityUUID: String, fromBean: Any, toBean: Any): Unit = {
    logEntityChange(institutionUUID, auditedEntityType, entityUUID, fromBean, toBean, null)
  }

  def logEntityChange(institutionUUID: String, auditedEntityType: AuditedEntityType, entityUUID: String, fromBean: Any, toBean: Any, personUUID: String): Unit = {
    var fromAB: AutoBean[Any] = null
    var fromValue: String = null
    if (fromBean != null) {
      fromAB = AutoBeanUtils.getAutoBean(fromBean)
      fromValue = AutoBeanCodex.encode(fromAB).getPayload
    }
    var toAB: AutoBean[Any] = null
    var toValue: String = null
    if (toBean != null) {
      toAB = AutoBeanUtils.getAutoBean(toBean)
      toValue = AutoBeanCodex.encode(toAB).getPayload
    }
    val logChange = fromBean == null || toBean == null || {
      val diffMap = AutoBeanUtils.diff(fromAB, toAB)
      diffMap.size() > 0 && fromValue != toValue
    }
    if (logChange) {
      sql"""insert into EntityChanged(uuid, personUUID, institutionUUID, entityType, entityUUID, fromValue, toValue, eventFiredAt)
        values(${UUID.randomUUID.toString},
           ${ThreadLocalAuthenticator.getAuthenticatedPersonUUID.getOrElse(personUUID)},
           ${institutionUUID},
           ${auditedEntityType.toString},
           ${entityUUID},
           ${fromValue},
           ${toValue},
       now())
      """.executeUpdate
    }
  }

  def getEntityChangedEvents(institutionUUID: String, entityType: AuditedEntityType, pageSize: Int, pageNumber: Int): EntityChangedEventsTO = {
    val resultOffset = (pageNumber.max(1) - 1) * pageSize

    val entityChangedEventsTO = newEntityChangedEventsTO(sql"""
      select ec.*, p.fullName as fromPersonName, pwd.username as fromUsername, 'FIX-ME' as entityName
    from EntityChanged ec
      join Person p on p.uuid = ec.personUUID
      join Password pwd on p.uuid = pwd.personUUID
    where ec.institutionUUID = ${institutionUUID}
        and ec.entityType = ${entityType.toString}
    order by eventFiredAt desc limit ${resultOffset}, ${pageSize}
    """.map[EntityChanged](toEntityChanged))

    entityChangedEventsTO.setPageSize(pageSize)
    entityChangedEventsTO.setPageNumber(pageNumber.max(1))
    entityChangedEventsTO.setCount({
      sql"""select count(ec.uuid)
    from EntityChanged ec
    where ec.institutionUUID = ${institutionUUID}
        and ec.entityType = ${entityType.toString}"""
        .first[String].get.toInt
    })
    entityChangedEventsTO.setSearchCount(entityChangedEventsTO.getCount)

    def getEntityName(entityChanged: EntityChanged): String = {
      entityChanged.getEntityType match {
        case AuditedEntityType.person |
          AuditedEntityType.password => {
          val person = PersonRepo(entityChanged.getEntityUUID).first
          if (person.isDefined) {
            person.get.getFullName
          } else "DELETED_ENTITY"
        }
        case AuditedEntityType.institution |
          AuditedEntityType.institutionAdmin |
          AuditedEntityType.institutionHostName |
          AuditedEntityType.institutionEmailWhitelist => InstitutionsRepo.getByUUID(entityChanged.getEntityUUID).get.getName
        case AuditedEntityType.course => {
          val course = CourseRepo(entityChanged.getEntityUUID).first
          if (course.isDefined) {
            course.get.getName
          } else "DELETED_ENTITY"
        }
        case AuditedEntityType.courseVersion => {
          val courseVersion = CourseVersionRepo(entityChanged.getEntityUUID).first
          if (courseVersion.isDefined) {
            courseVersion.get.getName
          } else "DELETED_ENTITY"
        }
        case AuditedEntityType.courseClass |
          AuditedEntityType.courseClassAdmin |
          AuditedEntityType.courseClassObserver |
          AuditedEntityType.courseClassTutor => {
          val courseClass = CourseClassRepo(entityChanged.getEntityUUID).first
          if (courseClass.isDefined) {
            courseClass.get.getName
          } else "DELETED_ENTITY"
        }
        case _ => "FIX-ME"
      }
    }

    entityChangedEventsTO.getEntitiesChanged.asScala.foreach(ec => ec.setEntityName(getEntityName(ec)))

    entityChangedEventsTO
  }

  def deleteActoms(enrollmentUUID: String): Unit = {
    sql"""
      DELETE from ActomEntered where enrollmentUUID = ${enrollmentUUID}
    """.executeUpdate

    sql"""
      DELETE from ActomEntries where enrollmentUUID = ${enrollmentUUID}
    """.executeUpdate

    sql"""
      DELETE from ActomEntryChangedEvent where enrollmentUUID = ${enrollmentUUID}
    """.executeUpdate
  }
}
