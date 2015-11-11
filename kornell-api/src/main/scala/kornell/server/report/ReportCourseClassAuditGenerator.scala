package kornell.server.report

import java.io.InputStream
import java.sql.ResultSet
import java.util.HashMap
import scala.collection.JavaConverters.seqAsJavaListConverter
import kornell.core.to.report.InstitutionBillingEnrollmentReportTO
import kornell.server.jdbc.SQL.SQLHelper
import kornell.server.repository.TOs
import kornell.server.jdbc.repository.InstitutionRepo
import kornell.core.to.report.InstitutionBillingMonthlyReportTO
import kornell.core.entity.BillingType
import kornell.core.entity.Institution
import kornell.core.entity.CourseClass
import kornell.core.to.report.CourseClassAuditTO
import kornell.server.jdbc.repository.InstitutionsRepo
import kornell.core.entity.EnrollmentState
import kornell.server.util.ServerTime
import java.util.logging.Logger

object ReportCourseClassAuditGenerator {
  val log = Logger.getLogger(getClass.getName)
  
  def generateCourseClassAuditReport(courseClass: CourseClass, tsOffset: String): Array[Byte] = {
    val parameters: HashMap[String, Object] = new HashMap()
    parameters.put("courseClassName", courseClass.getName)
    parameters.put("institutionBaseURL", InstitutionRepo(courseClass.getInstitutionUUID).get.getBaseURL)
	    
    generateCourseClassAuditReport(courseClass.getUUID, parameters, tsOffset)
  }
 
  private def generateCourseClassAuditReport(courseClassUUID: String, parameters: HashMap[String,Object], tsOffset: String): Array[Byte] = {

	  implicit def toCourseClassAuditTO(rs: ResultSet): CourseClassAuditTO =
	    TOs.newCourseClassAuditTO(
	    	rs.getString("eventFiredAt"),
			rs.getString("eventType"),
			rs.getString("adminFullName"),
			rs.getString("adminUsername"),
			rs.getString("participantFullName"),
			rs.getString("participantUsername"),
			rs.getString("fromCourseClassName"),
			rs.getString("toCourseClassName"), 
			rs.getString("fromState"),
			rs.getString("toState"),
			rs.getString("adminUUID"),
			rs.getString("participantUUID"),
			rs.getString("enrollmentUUID"),
			rs.getString("fromCourseClassUUID"),
			rs.getString("toCourseClassUUID"))
	  
    val courseClassAuditTO = sql"""
	    select 
			esc.eventFiredAt as eventFiredAt,
		    'stateChanged' as eventType,
		    admin.fullName as adminFullName,
		    adminPwd.username as adminUsername,
		    participant.fullname as participantFullName,
		    participantPwd.username as participantUsername,
		    cc.name as fromCourseClassName,
		    '-' as toCourseClassName,
			case    
				when esc.fromState = ${EnrollmentState.notEnrolled.toString} then '-'  
				when esc.fromState = ${EnrollmentState.cancelled.toString} then 'Cancelada'  
				when esc.fromState = ${EnrollmentState.requested.toString} then 'Requisitada'  
				when esc.fromState = ${EnrollmentState.denied.toString} then 'Negada'  
				when esc.fromState = ${EnrollmentState.enrolled.toString} then 'Matriculado'  
				when esc.fromState = ${EnrollmentState.deleted.toString} then 'Excluída'  
				else '?'   
			end as fromState,
			case    
				when esc.toState = ${EnrollmentState.notEnrolled.toString} then '-'  
				when esc.toState = ${EnrollmentState.cancelled.toString} then 'Cancelada'  
				when esc.toState = ${EnrollmentState.requested.toString} then 'Requisitada'  
				when esc.toState = ${EnrollmentState.denied.toString} then 'Negada'  
				when esc.toState = ${EnrollmentState.enrolled.toString} then 'Matriculado'  
				when esc.toState = ${EnrollmentState.deleted.toString} then 'Excluída'  
				else '?'   
			end as toState,
		    admin.uuid as adminUUID,
		    participant.uuid as participantUUID,
		    e.uuid as enrollmentUUID,
		    cc.uuid as fromCourseClassUUID,
		    '-' as toCourseClassUUID
		from EnrollmentStateChanged esc
		join Person admin on admin.uuid = esc.person_uuid
		join Password adminPwd on adminPwd.person_uuid = admin.uuid
		join Enrollment e on e.uuid = esc.enrollment_uuid
		join CourseClass cc on cc.uuid = e.class_uuid and cc.uuid = ${courseClassUUID}
		join Person participant on participant.uuid = e.person_uuid
		join Password participantPwd on participantPwd.person_uuid = participant.uuid
		union
		select 
			et.eventFiredAt as eventFiredAt,
		    'transferred' as eventType,
		    admin.fullName as adminFullName,
		    adminPwd.username as adminUsername,
		    participant.fullname as participantFullName,
		    participantPwd.username as participantUsername,
		    ccFrom.name as fromCourseClassName,
		    ccTo.name as toCourseClassName,
		    '-' as fromState,
		    '-' as toState,
		    admin.uuid as adminUUID,
		    participant.uuid as participantUUID,
		    e.uuid as enrollmentUUID,
		    ccFrom.uuid as fromCourseClassUUID,
		    ccTo.uuid as toCourseClassUUID
		from EnrollmentTransferred et
		join Person admin on admin.uuid = et.personUUID
		join Password adminPwd on adminPwd.person_uuid = admin.uuid
		join Enrollment e on e.uuid = et.enrollmentUUID
		join CourseClass ccFrom on ccFrom.uuid = et.fromCourseClassUUID and ccFrom.uuid = ${courseClassUUID}
		join CourseClass ccTo on ccTo.uuid = et.toCourseClassUUID
		join Person participant on participant.uuid = e.person_uuid
		join Password participantPwd on participantPwd.person_uuid = participant.uuid
		order by eventFiredAt desc
	    """.map[CourseClassAuditTO](toCourseClassAuditTO)
		  
	
    val cl = Thread.currentThread.getContextClassLoader
    val jasperStream = cl.getResourceAsStream("reports/courseClassAuditXLS.jasper")
    ReportGenerator.getReportBytesFromStream(courseClassAuditTO.map(fixDates(_, tsOffset)), parameters, jasperStream, "xls")
  }
  
  private def fixDates(to: CourseClassAuditTO, tsOffset: String) = {
	to.setEventFiredAt(ServerTime.adjustTimezoneOffset(to.getEventFiredAt, tsOffset.toInt))
    to
  }
  
 
}