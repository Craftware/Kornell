package kornell.server.report

import java.sql.ResultSet
import java.util

import javax.servlet.http.HttpServletResponse
import kornell.core.entity.BillingType
import kornell.core.to.report.{InstitutionBillingEnrollmentReportTO, InstitutionBillingMonthlyReportTO}
import kornell.server.jdbc.SQL.SQLHelper
import kornell.server.jdbc.repository.InstitutionRepo
import kornell.server.repository.TOs

object ReportInstitutionBillingGenerator {

  def generateInstitutionBillingReport(institutionUUID: String, periodStart: String, periodEnd: String, resp: HttpServletResponse): Array[Byte] = {
    val institution = InstitutionRepo(institutionUUID).get
    resp.addHeader("Content-disposition", "attachment; filename=" + institution.getName + " - " + periodStart + ".xls")
    resp.setContentType("application/vnd.ms-excel")

    val parameters: util.HashMap[String, Object] = new util.HashMap()
    parameters.put("institutionName", institution.getName)
    parameters.put("periodStart", periodStart)
    parameters.put("periodEnd", periodEnd)

    institution.getBillingType match {
      case BillingType.monthly => generateInstitutionBillingMonthlyReport(institution.getUUID, periodStart, periodEnd, parameters)
      case BillingType.enrollment => generateInstitutionBillingEnrollmentReport(institution.getUUID, periodStart, periodEnd, parameters)
    }
  }

  private def generateInstitutionBillingMonthlyReport(institutionUUID: String, periodStart: String, periodEnd: String, parameters: util.HashMap[String, Object]): Array[Byte] = {

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
        AND (email IS null OR email NOT LIKE '%craftware.com%' OR email NOT LIKE '%eduvem.com%')
        AND att.institutionUUID = ${institutionUUID}
        AND (SELECT count(uuid) FROM Enrollment where personUUID = p.uuid and DATE_FORMAT(enrolledOn, '%Y-%m-%d')< ${periodEnd}) > 0
        GROUP BY p.uuid, pw.username
        ORDER BY LOWER(p.fullName)
      """.map[InstitutionBillingMonthlyReportTO](toInstitutionBillingMonthlyReportTO)

    getReportBytesFromJrxml(institutionBillingReportTO, parameters, "institutionBillingXLS_monthly", "xls")
  }

  private def generateInstitutionBillingEnrollmentReport(institutionUUID: String, periodStart: String, periodEnd: String, parameters: util.HashMap[String, Object]): Array[Byte] = {

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
      AND cc.sandbox = 0
      AND (
          (e.lastBilledAt IS NULL
          AND ae.firstEventFiredAt >= ${periodStart + "-01 00:00:00"}
          AND ae.firstEventFiredAt < ${periodEnd + "-01 00:00:00"})
        OR (e.lastBilledAt IS NOT NULL
          AND e.lastBilledAt >= ${periodStart + "-01 00:00:00"}
          AND e.lastBilledAt < ${periodEnd + "-01 00:00:00"})
        )
      AND (email IS null OR email NOT LIKE '%craftware.com%' OR email NOT LIKE '%eduvem.com%')
      AND cc.sandbox = 0
      ORDER BY c.name, cv.name, cc.name, p.fullName
      """.map[InstitutionBillingEnrollmentReportTO](toInstitutionBillingEnrollmentReportTO)

    getReportBytesFromJrxml(institutionBillingReportTO, parameters, "institutionBillingXLS_enrollment", "xls")
  }
}
