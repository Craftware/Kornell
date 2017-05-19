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
import kornell.core.entity.EnrollmentState
import javax.servlet.http.HttpServletResponse

object ReportInstitutionBillingGenerator {
  
  def generateInstitutionBillingReport(institutionUUID: String, periodStart: String, periodEnd: String, resp: HttpServletResponse) = {
    val institution = InstitutionRepo(institutionUUID).get
    resp.addHeader("Content-disposition", "attachment; filename=" + institution.getName + " - " + periodStart + ".xls")
    resp.setContentType("application/vnd.ms-excel")    
    
    val parameters: HashMap[String, Object] = new HashMap()
    parameters.put("institutionName", institution.getName)
    parameters.put("periodStart", periodStart)
    parameters.put("periodEnd", periodEnd)
	    
    institution.getBillingType match {
      case BillingType.monthly => generateInstitutionBillingMonthlyReport(institution.getUUID, periodStart, periodEnd, parameters)
      case BillingType.enrollment => generateInstitutionBillingEnrollmentReport(institution.getUUID, periodStart, periodEnd, parameters)
    }
  }

  private def generateInstitutionBillingMonthlyReport(institutionUUID: String, periodStart: String, periodEnd: String, parameters: HashMap[String,Object]): Array[Byte] = {

	  implicit def toInstitutionBillingMonthlyReportTO(rs: ResultSet): InstitutionBillingMonthlyReportTO =
	    TOs.newInstitutionBillingMonthlyReportTO(
	      rs.getString("personUUID"),
	      rs.getString("fullName"),
	      rs.getString("username"))
	      
    val institutionBillingReportTO = sql"""
    			SELECT
					p.uuid AS 'personUUID', 
					p.fullName AS 'fullName',
					pw.username AS 'username'
				FROM AttendanceSheetSigned att
				JOIN Person p ON p.uuid = att.personUUID
				JOIN Password pw ON pw.personUUID = p.uuid
				WHERE att.eventFiredAt >= ${periodStart + "-01 00:00:00"} AND att.eventFiredAt < ${periodEnd + "-01 00:00:00"}
				AND (email IS null OR email NOT LIKE '%craftware.com.br%')
				AND att.institutionUUID = ${institutionUUID} 
				AND (SELECT count(uuid) FROM Enrollment where personUUID = p.uuid and DATE_FORMAT(enrolledOn, '%Y-%m-%d')< ${periodEnd}) > 0
				GROUP BY att.personUUID
				ORDER BY LOWER(p.fullName)
	    """.map[InstitutionBillingMonthlyReportTO](toInstitutionBillingMonthlyReportTO)
		  
    val cl = Thread.currentThread.getContextClassLoader
    val jasperStream = cl.getResourceAsStream("reports/institutionBillingXLS_monthly.jasper")
    getReportBytesFromStream(institutionBillingReportTO, parameters, jasperStream, "xls")
  }

  private def generateInstitutionBillingEnrollmentReport(institutionUUID: String, periodStart: String, periodEnd: String, parameters: HashMap[String,Object]): Array[Byte] = {

	  implicit def toInstitutionBillingEnrollmentReportTO(rs: ResultSet): InstitutionBillingEnrollmentReportTO =
	    TOs.newInstitutionBillingEnrollmentReportTO(
	      rs.getString("enrollmentUUID"),
	      rs.getString("courseName"),
	      rs.getString("courseVersionName"),
	      rs.getString("courseClassName"),
	      rs.getString("fullName"),
	      rs.getString("username"),
	      rs.getTimestamp("firstEventFiredAt"))
	      
    val institutionBillingReportTO = sql"""
    		SELECT 
				e.uuid AS 'enrollmentUUID', 
				c.name AS 'courseName',
				cv.name AS 'courseVersionName',
				cc.name AS 'courseClassName',
				p.fullName, 
				pw.username,
    			ae.firstEventFiredAt
			FROM Enrollment e
			JOIN CourseClass cc ON cc.uuid = e.courseClassUUID
			JOIN CourseVersion cv ON cv.uuid = cc.courseVersionUUID
			JOIN Course c ON c.uuid = cv.courseUUID
			JOIN Person p ON p.uuid = e.personUUID
			JOIN Password pw on pw.personUUID = p.uuid
			LEFT JOIN (
					SELECT enrollmentUUID, MIN(eventFiredAt) AS firstEventFiredAt FROM ActomEntered GROUP BY enrollmentUUID
				) AS ae ON ae.enrollmentUUID = e.uuid
			WHERE cc.institutionUUID = ${institutionUUID}
			AND (
					(e.lastBilledAt IS NULL
					AND ae.firstEventFiredAt >= ${periodStart + "-01 00:00:00"}
					AND ae.firstEventFiredAt < ${periodEnd + "-01 00:00:00"})
				OR (e.lastBilledAt IS NOT NULL
					AND e.lastBilledAt >= ${periodStart + "-01 00:00:00"}
					AND e.lastBilledAt < ${periodEnd + "-01 00:00:00"})
				) 
			AND (email IS null OR email NOT LIKE '%craftware.com.br%')
			ORDER BY LOWER(c.name),LOWER(cv.name),LOWER(cc.name), LOWER(p.fullName)
	    """.map[InstitutionBillingEnrollmentReportTO](toInstitutionBillingEnrollmentReportTO)
		  
    val cl = Thread.currentThread.getContextClassLoader
    val jasperStream = cl.getResourceAsStream("reports/institutionBillingXLS_enrollment.jasper")
    getReportBytesFromStream(institutionBillingReportTO, parameters, jasperStream, "xls")
  }
 
}