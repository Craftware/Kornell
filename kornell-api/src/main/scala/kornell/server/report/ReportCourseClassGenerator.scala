package kornell.server.report

import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.net.{HttpURLConnection, URL}
import java.sql.ResultSet
import java.text.{Normalizer, SimpleDateFormat}
import java.util
import java.util.Date

import kornell.core.entity.{EnrollmentState, EntityState}
import kornell.core.error.exception.{EntityConflictException, EntityNotFoundException, ServerErrorException}
import kornell.core.to.SimplePeopleTO
import kornell.core.to.report.{CourseClassReportTO, EnrollmentsBreakdownTO}
import kornell.core.util.StringUtils._
import kornell.server.authentication.ThreadLocalAuthenticator
import kornell.server.content.ContentManagers
import kornell.server.jdbc.PreparedStmt
import kornell.server.jdbc.SQL.SQLHelper
import kornell.server.jdbc.repository.{ContentRepositoriesRepo, CourseClassRepo, CourseRepo, InstitutionRepo, PersonRepo}
import kornell.server.repository.TOs
import kornell.server.service.ContentService
import kornell.server.util.DateConverter

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object ReportCourseClassGenerator {

  def newCourseClassReportTO: CourseClassReportTO = new CourseClassReportTO
  def newCourseClassReportTO(fullName: String, username: String, email: String, cpf: String, state: String, progressState: String,
    progress: Int, assessmentScore: BigDecimal, preAssessmentScore: BigDecimal, postAssessmentScore: BigDecimal,
    certifiedAt: Date, enrolledAt: Date, courseName: String, courseVersionName: String, courseClassName: String,
    company: String, title: String, sex: String, birthDate: String, telephone: String, country: String, stateProvince: String,
    city: String, addressLine1: String, addressLine2: String, postalCode: String): CourseClassReportTO = {
    val to = newCourseClassReportTO
    to.setFullName(fullName)
    to.setUsername(username)
    to.setEmail(email)
    to.setCpf(cpf)
    to.setState(state)
    to.setProgressState(progressState)
    to.setProgress(progress)
    to.setAssessmentScore(assessmentScore)
    to.setPreAssessmentScore(preAssessmentScore)
    to.setPostAssessmentScore(postAssessmentScore)
    to.setCertifiedAt(DateConverter.convertDate(certifiedAt))
    to.setEnrolledAt(DateConverter.convertDate(enrolledAt))
    to.setCourseName(courseName)
    to.setCourseVersionName(courseVersionName)
    to.setCourseClassName(courseClassName)
    to.setCompany(company)
    to.setTitle(title)
    to.setSex(sex)
    to.setBirthDate(birthDate)
    to.setTelephone(telephone)
    to.setCountry(country)
    to.setStateProvince(stateProvince)
    to.setCity(city)
    to.setAddressLine1(addressLine1)
    to.setAddressLine2(addressLine2)
    to.setPostalCode(postalCode)
    to
  }

  implicit def toCourseClassReportTO(rs: ResultSet): CourseClassReportTO =
    newCourseClassReportTO(
      rs.getString("fullName"),
      rs.getString("username"),
      rs.getString("email"),
      rs.getString("cpf"),
      rs.getString("state"),
      rs.getString("progressState"),
      rs.getInt("progress"),
      rs.getBigDecimal("assessmentScore"),
      rs.getBigDecimal("preAssessmentScore"),
      rs.getBigDecimal("postAssessmentScore"),
      rs.getTimestamp("certifiedAt"),
      rs.getTimestamp("enrolledAt"),
      rs.getString("courseName"),
      rs.getString("courseVersionName"),
      rs.getString("courseClassName"),
      rs.getString("company"),
      rs.getString("title"),
      rs.getString("sex"),
      rs.getString("birthDate"),
      rs.getString("telephone"),
      rs.getString("country"),
      rs.getString("stateProvince"),
      rs.getString("city"),
      rs.getString("addressLine1"),
      rs.getString("addressLine2"),
      rs.getString("postalCode"))

  type BreakdownData = (String, Integer)
  implicit def breakdownConvertion(rs: ResultSet): BreakdownData = (rs.getString(1), rs.getInt(2))

  def generateCourseClassReport(courseUUID: String, courseClassUUID: String, fileType: String, peopleTO: SimplePeopleTO): String = {
    if (courseUUID != null || courseClassUUID != null) {
      var sql = s"""
        select
          p.fullName,
          if(pw.username is not null, pw.username, p.email) as username,
          p.email,
          p.cpf,
          case
            when e.state = '${EnrollmentState.cancelled.toString}' then 'Cancelada'
            when e.state = '${EnrollmentState.requested.toString}' then 'Requisitada'
            when e.state = '${EnrollmentState.denied.toString}' then 'Negada'
            else 'Matriculado'
          end as state,
          case
            when progress is null OR progress = 0 then 'notStarted'
            when progress > 0 and progress < 100 then 'inProgress'
            when progress = 100 and certifiedAt is null then 'waitingEvaluation'
            else 'completed'
          end as progressState,
          e.progress,
          e.assessmentScore,
          e.preAssessmentScore,
          e.postAssessmentScore,
          e.certifiedAt,
          e.enrolledOn as enrolledAt,
          c.name as courseName,
          cv.name as courseVersionName,
          cc.name as courseClassName,
          p.company,
          p.title,
          p.sex,
          p.birthDate,
          p.telephone,
          p.country,
          p.state as stateProvince,
          p.city,
          p.addressLine1,
          p.addressLine2,
          p.postalCode
        from
          Enrollment e
          join Person p on p.uuid = e.personUUID
          join CourseClass cc on cc.uuid = e.courseClassUUID
          join CourseVersion cv on cv.uuid = cc.courseVersionUUID
          join Course c on c.uuid = cv.courseUUID
          left join Password pw on pw.personUUID = p.uuid
        where
          (e.state = '${EnrollmentState.enrolled.toString}' or '${fileType}' = 'xls') and
          cc.state <> '${EntityState.deleted.toString}' and
          (cc.state = '${EntityState.active.toString}' or '${courseUUID}' = 'null') and
          (e.courseClassUUID = '${courseClassUUID}' or '${courseClassUUID}' = 'null') and
          (c.uuid = '${courseUUID}' or '${courseUUID}' = 'null') and
          cc.sandbox = 0 and
          e.state <> '${EnrollmentState.deleted.toString}'
      """

      var usernames = ""
      if (peopleTO != null && peopleTO.getSimplePeopleTO != null) {
        peopleTO.getSimplePeopleTO.asScala.foreach { person => usernames += "'" + person.getUsername + "'," }
        if (usernames.length > 1) {
          usernames = usernames.dropRight(1)
          sql += s"""and if(pw.username is not null, pw.username, p.email) in ( ${usernames} ) """
        }
      }

      if (sql.contains("--")) throw new EntityConflictException("invalidValue")

      sql += s"""
        order by
          case
            when e.state = '${EnrollmentState.enrolled.toString}' then 1
            when e.state = '${EnrollmentState.requested.toString}'  then 2
            when e.state = '${EnrollmentState.denied.toString}'  then 3
            when e.state = '${EnrollmentState.cancelled.toString}'  then 4
            else 5
            end,
          case
            when progressState = 'completed' then 1
            when progressState = 'waitingEvaluation'  then 2
            when progressState = 'inProgress'  then 3
            else 4
            end,
          c.name,
          cv.name,
          cc.name,
          e.certifiedAt desc,
          progress,
          p.fullName,
          pw.username,
          p.email"""
      val pstmt = new PreparedStmt(sql, List())
      val courseClassReportTO = pstmt.map[CourseClassReportTO](toCourseClassReportTO)

      val parameters = getTotalsAsParameters(courseUUID, courseClassUUID, fileType, usernames)
      addInfoParameters(courseUUID, courseClassUUID, parameters)

      val enrollmentBreakdowns: ListBuffer[EnrollmentsBreakdownTO] = ListBuffer()
      enrollmentBreakdowns += TOs.newEnrollmentsBreakdownTO("aa", new Integer(1))
      enrollmentBreakdowns.toList

      val cl = Thread.currentThread.getContextClassLoader
      val jasperStream = {
        if (fileType == "xls")
          cl.getResourceAsStream("reports/courseClassInfoXLS.jasper")
        else
          cl.getResourceAsStream("reports/courseClassInfo.jasper")
      }
      val report = getReportBytesFromStream(courseClassReportTO, parameters, jasperStream, fileType)
      val bs = new ByteArrayInputStream(report)
      val person = PersonRepo(ThreadLocalAuthenticator.getAuthenticatedPersonUUID.get).get
      val repo = ContentManagers.forRepository(ContentRepositoriesRepo.firstRepositoryByInstitution(person.getInstitutionUUID).get.getUUID)
      val filename = getFileName(courseUUID, courseClassUUID)
      val fileFullPath = getCourseClassInfoReportFileName(courseUUID: String, courseClassUUID: String, fileType: String)
      val contentType = {
        if (fileType == "xls")
          "application/octet-stream"
        else
          "application/pdf"
      }
      repo.put(
        bs,
        report.length,
        contentType,
        "Content-Disposition: attachment; filename=\"" + filename + "\"",
        Map("certificatedata" -> "09/01/1980", "requestedby" -> person.getFullName), fileFullPath)

      getCourseClassInfoReportURL(courseUUID, courseClassUUID, fileType)
    } else {
      throw new EntityNotFoundException("notFound")
    }
  }

  type ReportHeaderData = (String, String, String, Date, String, String, Date, String, String)
  implicit def headerDataConvertion(rs: ResultSet): ReportHeaderData = (rs.getString(1), rs.getString(2), rs.getString(3), rs.getDate(4), rs.getString(5), rs.getString(6), rs.getDate(7), rs.getString(8), rs.getString(9))

  private def addInfoParameters(courseUUID: String, courseClassUUID: String, parameters: util.HashMap[String, Object]) = {
    val headerInfo = sql"""
      select
        i.fullName as 'institutionName',
        c.name as 'courseName',
        cc.name as 'courseClassName',
        cc.createdAt,
        cc.maxEnrollments,
        i.assetsRepositoryUUID,
        (select eventFiredAt from CourseClassStateChanged
          where toState = 'inactive' and courseClassUUID = cc.uuid
          order by eventFiredAt desc) as disabledAt,
        (select replace(GROUP_CONCAT(p.fullName),',',', ')
          from Role r
          join Person p on p.uuid = r.personUUID
          where courseClassUUID = cc.uuid
          group by courseClassUUID) as courseClassAdminNames,
        i.baseURL
      from
        CourseClass cc
        join CourseVersion cv on cc.courseVersionUUID = cv.uuid
        join Course c on cv.courseUUID = c.uuid
        join Institution i on i.uuid = cc.institutionUUID
      where (cc.uuid = ${courseClassUUID} or ${courseClassUUID} is null) and
        (cv.courseUUID = ${courseUUID} or ${courseUUID} is null) and
          cc.state <> ${EntityState.deleted.toString} and
          (cc.state = ${EntityState.active.toString} or ${courseUUID} is null)
    """.first[ReportHeaderData](headerDataConvertion)

    if (headerInfo.isDefined) {
      parameters.put("institutionName", headerInfo.get._1)
      parameters.put("courseTitle", headerInfo.get._2)
      parameters.put("assetsURL", mkurl(headerInfo.get._9.split("Kornell.nocache.html").head, "repository", headerInfo.get._6, ContentService.PREFIX, ContentService.INSTITUTION, ""))
      if (courseClassUUID != null) {
        parameters.put("courseClassName", headerInfo.get._3)
        parameters.put("createdAt", headerInfo.get._4)
        parameters.put("maxEnrollments", headerInfo.get._5)
        parameters.put("disabledAt", headerInfo.get._7)
        parameters.put("courseClassAdminNames", headerInfo.get._8)
      }
    }
    parameters
  }

  private def getTotalsAsParameters(courseUUID: String, courseClassUUID: String, fileType: String, usernames: String): util.HashMap[String, Object] = {

    var sql = s"""
        select
          case
            when progress is null OR progress = 0 then 'notStarted'
            when progress > 0 and progress < 100 then 'inProgress'
            when progress = 100 and certifiedAt is null then 'waitingEvaluation'
            else 'completed'
          end as progressState,
          count(*) as total
        from
          Enrollment e
          join Person p on p.uuid = e.personUUID
          left join Password pw on pw.personUUID = p.uuid
          join CourseClass cc on cc.uuid = e.courseClassUUID
          join CourseVersion cv on cv.uuid = cc.courseVersionUUID
        where
          (e.state = '${EnrollmentState.enrolled.toString}' or '${fileType}' = 'xls') and
          cc.state = '${EntityState.active.toString}' and
          (e.courseClassUUID = '${courseClassUUID}' or '${courseClassUUID}' = 'null') and
          (cv.courseUUID = '${courseUUID}' or '${courseUUID}' = 'null') and
          cc.sandbox = 0 and
          e.state <> '${EnrollmentState.deleted.toString}'
      """

    if (usernames != null && usernames.length > 0)
      sql += s"""and if(pw.username is not null, pw.username, p.email) in ( $usernames ) """

    if (sql.contains("--")) throw new EntityConflictException("invalidValue")

    sql += s"""
        group by
          case
            when progress is null OR progress = 0 then 'notStarted'
            when progress > 0 and progress < 100 then 'inProgress'
            when progress = 100 and certifiedAt is null then 'waitingEvaluation'
            else 'completed'
          end"""
    val pstmt = new PreparedStmt(sql, List())
    val enrollmentStateBreakdown = pstmt.map[BreakdownData](breakdownConvertion)

    val parameters: util.HashMap[String, Object] = new util.HashMap()
    enrollmentStateBreakdown.foreach(rd => parameters.put(rd._1, rd._2))
    parameters
  }

  def courseClassInfoExists(courseUUID: String, courseClassUUID: String, fileType: String): String = {
    try {
      val url = getCourseClassInfoReportURL(courseUUID, courseClassUUID, fileType)
      HttpURLConnection.setFollowRedirects(false)
      val con = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
      con.setRequestMethod("HEAD")
      if (con.getResponseCode == HttpURLConnection.HTTP_OK) url else ""
    } catch {
      case e: Exception => throw new ServerErrorException("errorCheckingClassInfo", e)
    }
  }

  def getFileName(courseUUID: String, courseClassUUID: String): String = {
    val title = if (courseUUID != null) CourseRepo(courseUUID).get.getName else CourseClassRepo(courseClassUUID).get.getName
    val normalizeTitle = Normalizer.normalize(title, Normalizer.Form.NFD)
    val replaceTitle = normalizeTitle.replaceAll("[^\\p{ASCII}]", "").replaceAll(" ", "_")
    replaceTitle + "_" + new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date())
  }

  def getCourseClassInfoReportFileName(courseUUID: String, courseClassUUID: String, fileType: String): String = {
    val fileNamePrefix = getFileName(courseUUID, courseClassUUID)
    mkurl(ContentService.PREFIX, ContentService.REPORTS, ContentService.CLASS_INFO, fileNamePrefix + "_" + ThreadLocalAuthenticator.getAuthenticatedPersonUUID.get +
      Option(courseClassUUID).getOrElse("") + Option(courseUUID).getOrElse("") + "." + getFileType(fileType))
  }

  def getCourseClassInfoReportURL(courseUUID: String, courseClassUUID: String, fileType: String): String = {
    val institutionUUID = getInstitutionUUID(courseClassUUID, courseUUID)
    val repo = ContentManagers.forRepository(ContentRepositoriesRepo.firstRepositoryByInstitution(institutionUUID).get.getUUID)
    val key = getCourseClassInfoReportFileName(courseUUID, courseClassUUID, fileType)
    mkurl(InstitutionRepo(institutionUUID).get.getBaseURL.split("Kornell.nocache.html").head, repo.url(key))
  }
}
