package kornell.server.jdbc.repository

import com.google.web.bindery.autobean.shared.AutoBeanCodex
import com.google.web.bindery.autobean.shared.AutoBeanUtils
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource
import kornell.core.entity.AuditedEntityType
import kornell.core.entity.CourseClassState
import kornell.core.entity.EnrollmentState
import kornell.core.error.exception.EntityConflictException
import kornell.core.event.ActomEntered
import kornell.core.event.AttendanceSheetSigned
import kornell.core.event.CourseClassStateChanged
import kornell.core.event.EnrollmentStateChanged
import kornell.core.event.EnrollmentTransferred
import kornell.core.event.EventFactory
import kornell.core.util.UUID
import kornell.server.authentication.ThreadLocalAuthenticator
import kornell.server.ep.EnrollmentSEP
import kornell.server.jdbc.SQL.SQLHelper
import kornell.server.util.EmailService
import kornell.server.util.ServerTime
import kornell.server.util.Settings
import com.google.web.bindery.autobean.shared.AutoBean
import kornell.core.to.EntityChangedEventsTO
import kornell.core.event.EnrollmentTransferred
import kornell.core.event.AttendanceSheetSigned
import kornell.core.event.EnrollmentStateChanged
import kornell.core.event.EventFactory
import kornell.core.entity.CourseClassState
import kornell.core.entity.EnrollmentState
import kornell.core.event.ActomEntered
import kornell.core.event.CourseClassStateChanged
import kornell.core.to.EntityChangedEventsTO
import kornell.core.error.exception.EntityConflictException
import kornell.core.entity.AuditedEntityType
import kornell.server.repository.TOs._
import kornell.server.jdbc.SQL._
import scala.collection.JavaConverters._
import kornell.core.event.EntityChanged

object EventsRepo {
  val events = AutoBeanFactorySource.create(classOf[EventFactory])

  def newEnrollmentStateChanged = events.newEnrollmentStateChanged.as

  def logActomEntered(event: ActomEntered) = {
    sql"""
    insert into ActomEntered(uuid,eventFiredAt,enrollmentUUID,actomKey) 
    values(${event.getUUID},
  		   ${event.getEventFiredAt},
           ${event.getEnrollmentUUID},
		   ${event.getActomKey}); 
	""".executeUpdate

    EnrollmentSEP.onProgress(event.getEnrollmentUUID)
    EnrollmentSEP.onAssessment(event.getEnrollmentUUID)
  }

  def logAttendanceSheetSigned(event: AttendanceSheetSigned) = {
    val todayStart = ServerTime.todayStart
    val todayEnd = ServerTime.todayEnd
    // don't log more than once a day
    val attendanceSheetSignedUUID = sql"""
			  select uuid from AttendanceSheetSigned
			  where personUUID=${event.getPersonUUID()}
			  and institutionUUID=${event.getInstitutionUUID()}
			  and eventFiredAt between ${todayStart} and ${todayEnd}
		  """.first
    if (!attendanceSheetSignedUUID.isDefined)
      sql"""
		    insert into AttendanceSheetSigned(uuid,eventFiredAt,institutionUUID,personUUID)
		    values(${event.getUUID}, ${event.getEventFiredAt}, ${event.getInstitutionUUID}, ${event.getPersonUUID});
			""".executeUpdate

  }

  def logEnrollmentStateChanged(uuid: String, eventFiredAt: String, fromPersonUUID: String,
    enrollmentUUID: String, fromState: EnrollmentState, toState: EnrollmentState, sendEmail: Boolean) = {

    sql"""insert into EnrollmentStateChanged(uuid,eventFiredAt,person_uuid,enrollment_uuid,fromState,toState)
	    values(${uuid},
			   ${eventFiredAt},
         ${fromPersonUUID},
         ${enrollmentUUID},
         ${fromState.toString},
			   ${toState.toString});
		""".executeUpdate

    sql"""update Enrollment set state = ${toState.toString} where uuid = ${enrollmentUUID};
		""".executeUpdate

    if (EnrollmentState.enrolled.equals(toState) && sendEmail) {
      val enrollment = EnrollmentRepo(enrollmentUUID).get
      val person = PersonRepo(enrollment.getPersonUUID).get
      if (person.getEmail != null && !"true".equals(Settings.get("TEST_MODE").orNull)) {
        if (enrollment.getCourseClassUUID != null) {
          val courseClass = CourseClassesRepo(enrollment.getCourseClassUUID).get
		  val course = CoursesRepo.byCourseClassUUID(courseClass.getUUID).get
          val institution = InstitutionsRepo.byUUID(courseClass.getInstitutionUUID).get
          EmailService.sendEmailEnrolled(person, institution, course, enrollment)
        } else {
          val courseVersion = CourseVersionRepo(enrollment.getCourseVersionUUID).get
          val course = CourseRepo(courseVersion.getCourseUUID).get
          val institution = InstitutionsRepo.byUUID(course.getInstitutionUUID).get
          EmailService.sendEmailEnrolled(person, institution, course, enrollment)
        }
      }
    }
  }

  def logEnrollmentStateChanged(event: EnrollmentStateChanged): Unit =
    logEnrollmentStateChanged(event.getUUID, event.getEventFiredAt, event.getFromPersonUUID,
      event.getEnrollmentUUID, event.getFromState, event.getToState, true)

  def logCourseClassStateChanged(uuid: String, eventFiredAt: String, fromPersonUUID: String,
    courseClassUUID: String, fromState: CourseClassState, toState: CourseClassState) = {

    sql"""insert into CourseClassStateChanged(uuid,eventFiredAt,personUUID,courseClassUUID,fromState,toState)
	    values(${uuid},
		 ${eventFiredAt},
         ${fromPersonUUID},
         ${courseClassUUID},
         ${fromState.toString},
		 ${toState.toString});
		""".executeUpdate

    sql"""update CourseClass set state = ${toState.toString} where uuid = ${courseClassUUID};
		""".executeUpdate
		
  }

  def logCourseClassStateChanged(event: CourseClassStateChanged): Unit =
    logCourseClassStateChanged(event.getUUID, event.getEventFiredAt, event.getFromPersonUUID,
      event.getCourseClassUUID, event.getFromState, event.getToState)

  def logEnrollmentTransferred(event: EnrollmentTransferred): Unit = {
    if (EnrollmentRepo(event.getEnrollmentUUID).checkExistingEnrollment(event.getToCourseClassUUID)) {
      throw new EntityConflictException("userAlreadyEnrolledInClass") 
    }
    sql"""insert into EnrollmentTransferred (uuid, personUUID, enrollmentUUID, fromCourseClassUUID, toCourseClassUUID, eventFiredAt) 
        values (${UUID.random},
        ${event.getFromPersonUUID},
        ${event.getEnrollmentUUID},
        ${event.getFromCourseClassUUID},
        ${event.getToCourseClassUUID},
        now());""".executeUpdate
        
        EnrollmentRepo(event.getEnrollmentUUID).transfer(event.getFromCourseClassUUID, event.getToCourseClassUUID)
  }

  def logEntityChange(institutionUUID: String, auditedEntityType: AuditedEntityType, entityUUID: String, fromBean: Any, toBean: Any) = {
	val toAB = AutoBeanUtils.getAutoBean(toBean)
    val toValue: String = AutoBeanCodex.encode(toAB).getPayload.toString
    var fromAB: AutoBean[Any] = null  
    var fromValue: String = null
    if(fromBean != null){
    	fromAB = AutoBeanUtils.getAutoBean(fromBean)
    	fromValue = AutoBeanCodex.encode(fromAB).getPayload.toString
    }
	val logChange = fromBean == null || {
	  val diffMap = AutoBeanUtils.diff(fromAB, toAB)
	  diffMap.size() > 0 && fromValue != toValue
	}
    if(logChange){
	    sql"""insert into EntityChanged(uuid, personUUID, institutionUUID, entityType, entityUUID, fromValue, toValue, eventFiredAt)
		    values(${UUID.random},
	         ${ThreadLocalAuthenticator.getAuthenticatedPersonUUID.get},
	         ${institutionUUID},
	         ${auditedEntityType.toString},
	         ${entityUUID},
	         ${fromValue},
	         ${toValue},
			 now());
			""".executeUpdate
    }
  }
  
  def getEntityChangedEvents(institutionUUID: String, entityType: AuditedEntityType, pageSize: Int, pageNumber: Int): EntityChangedEventsTO = {
    val resultOffset = (pageNumber.max(1) - 1) * pageSize
    
    val entityChangedEventsTO = newEntityChangedEventsTO(sql"""
	  	select ec.*, p.fullName as fromPersonName, pwd.username as fromUsername, 'FIX-ME' as entityName
		from EntityChanged ec
			join Person p on p.uuid = ec.personUUID
			join Password pwd on p.uuid = pwd.person_uuid
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
	      	AuditedEntityType.password => PersonRepo(entityChanged.getEntityUUID).first.get.getFullName
	      case AuditedEntityType.institution | 
	      	AuditedEntityType.institutionAdmin | 
	      	AuditedEntityType.institutionHostName | 
	      	AuditedEntityType.institutionEmailWhitelist => InstitutionRepo(entityChanged.getEntityUUID).first.get.getName
	      case AuditedEntityType.course => CourseRepo(entityChanged.getEntityUUID).first.get.getTitle
	      case AuditedEntityType.courseVersion => CourseVersionRepo(entityChanged.getEntityUUID).first.get.getName
	      case AuditedEntityType.courseClass | 
	      	AuditedEntityType.courseClassAdmin | 
	      	AuditedEntityType.courseClassObserver |
	      	AuditedEntityType.courseClassTutor =>  CourseClassRepo(entityChanged.getEntityUUID).first.get.getName
	      case _ => "FIX-ME"
      }
   } 
   
   entityChangedEventsTO.getEntitiesChanged.asScala.foreach(ec => ec.setEntityName(getEntityName(ec)))

   entityChangedEventsTO
  }

}