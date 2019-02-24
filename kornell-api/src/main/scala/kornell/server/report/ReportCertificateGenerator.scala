package kornell.server.report

import java.io.ByteArrayInputStream
import java.net.{HttpURLConnection, URL}
import java.sql.ResultSet
import java.util
import java.util.Date

import javax.servlet.http.HttpServletResponse
import kornell.core.entity.{CertificateDetails, CertificateType, CourseDetailsEntityType, RepositoryType}
import kornell.core.error.exception.{EntityConflictException, ServerErrorException}
import kornell.core.to.SimplePeopleTO
import kornell.core.to.report.CertificateInformationTO
import kornell.core.util.StringUtils.mkurl
import kornell.server.authentication.ThreadLocalAuthenticator
import kornell.server.content.ContentManagers
import kornell.server.jdbc.PreparedStmt
import kornell.server.jdbc.SQL.SQLHelper
import kornell.server.jdbc.repository.{CertificatesDetailsRepo, ContentRepositoriesRepo, EnrollmentsRepo, InstitutionRepo, PersonRepo}
import kornell.server.repository.Entities
import kornell.server.service.ContentService
import kornell.server.util.DateConverter

object ReportCertificateGenerator {

  def newCertificateInformationTO: CertificateInformationTO = new CertificateInformationTO

  def newCertificateInformationTO(personFullName: String, personCPF: String, courseName: String, courseClassName: String, institutionName: String, courseClassFinishedDate: Date, assetsRepositoryUUID: String, distributionPrefix: String, courseVersionUUID: String, courseClassUUID: String, courseUUID: String, baseURL: String, repositoryType: RepositoryType, courseCode: String): CertificateInformationTO = {
    val to = newCertificateInformationTO
    to.setPersonFullName(personFullName)
    to.setPersonCPF(personCPF)
    to.setCourseTitle(courseName)
    to.setCourseClassName(courseClassName)
    to.setInstitutionName(institutionName)
    to.setAssetsRepositoryUUID(assetsRepositoryUUID)
    to.setDistributionPrefix(distributionPrefix)
    to.setCourseVersionUUID(courseVersionUUID)
    to.setCourseClassUUID(courseClassUUID)
    to.setCourseUUID(courseUUID)
    to.setBaseURL(baseURL)
    to.setCourseClassFinishedDate(DateConverter.convertDate(courseClassFinishedDate))
    to.setRepositoryType(repositoryType)
    to.setCourseCode(courseCode)
    to
  }

  implicit def toCertificateInformationTO(rs: ResultSet): CertificateInformationTO =
    newCertificateInformationTO(
      rs.getString("fullName"),
      rs.getString("cpf"),
      rs.getString("courseName"),
      rs.getString("name"),
      rs.getString("institutionName"),
      rs.getTimestamp("certifiedAt"),
      rs.getString("assetsRepositoryUUID"),
      rs.getString("distributionPrefix"),
      rs.getString("courseVersionUUID"),
      rs.getString("courseClassUUID"),
      rs.getString("courseUUID"),
      rs.getString("baseURL"),
      RepositoryType.valueOf(rs.getString("repositoryType")),
      rs.getString("code"))

  def findCertificateDetails(certificateInformationTO: CertificateInformationTO): CertificateDetails = {
    val certificateDetails = CertificatesDetailsRepo.findCertificateDetails(certificateInformationTO.getCourseUUID, certificateInformationTO.getCourseVersionUUID, certificateInformationTO.getCourseClassUUID)
    if (certificateDetails.isDefined)
      certificateDetails.get
    else
      Entities.newCertificateDetails(null, "reports/", CertificateType.NO_BG, CourseDetailsEntityType.COURSE_CLASS, null)
  }

  def generateCertificate(userUUID: String, courseClassUUID: String, resp: HttpServletResponse): Array[Byte] = {
    resp.addHeader("Content-disposition", "attachment; filename=Certificado.pdf")

    val certificateData = sql"""
        select p.fullName, c.name as courseName, cc.name, i.fullName as institutionName, i.assetsRepositoryUUID, cv.distributionPrefix, p.cpf, e.certifiedAt, cv.uuid as courseVersionUUID, cc.uuid as courseClassUUID, c.uuid as courseUUID, i.baseURL, s.repositoryType, c.code
          from Person p
          join Enrollment e on p.uuid = e.personUUID
          join CourseClass cc on cc.uuid = e.courseClassUUID
          join CourseVersion cv on cv.uuid = cc.courseVersionUUID
          join Course c on c.uuid = cv.courseUUID
        join Institution i on i.uuid = cc.institutionUUID
        join ContentRepository s on s.uuid = i.assetsRepositoryUUID
        where e.certifiedAt is not null and
              p.uuid = $userUUID and
          cc.uuid = $courseClassUUID
        """.map[CertificateInformationTO](toCertificateInformationTO)

    val certificateDetails = findCertificateDetails(certificateData.head)
    generateCertificateReport(certificateData, certificateDetails)

  }

  def generateCertificate(certificateInformationTOs: List[CertificateInformationTO]): Array[Byte] = {
    val certificateDetails = findCertificateDetails(certificateInformationTOs.head)
    generateCertificateReport(certificateInformationTOs, certificateDetails)

  }

  def getCertificateInformationTOsByCourseClass(courseClassUUID: String, enrollments: String): List[CertificateInformationTO] = {

    var sql =
      """select p.fullName, c.name as courseName, cc.name, i.fullName as institutionName, i.assetsRepositoryUUID, cv.distributionPrefix, p.cpf, e.certifiedAt, cv.uuid as courseVersionUUID, cc.uuid as courseClassUUID, c.uuid as courseUUID, i.baseURL, s.repositoryType, c.code
      from Person p
      join Enrollment e on p.uuid = e.personUUID
      join CourseClass cc on cc.uuid = e.courseClassUUID
      join CourseVersion cv on cv.uuid = cc.courseVersionUUID
      join Course c on c.uuid = cv.courseUUID
      join Institution i on i.uuid = cc.institutionUUID
      join ContentRepository s on s.uuid = i.assetsRepositoryUUID
      where e.certifiedAt is not null and
      e.state <> 'cancelled' and """ +
        s"""cc.uuid = '$courseClassUUID' """
    if (enrollments != null)
      sql += s"""and e.uuid in ( $enrollments )"""
    if (sql.contains("--")) throw new EntityConflictException("invalidValue")
    val pstmt = new PreparedStmt(sql, List())
    pstmt.map[CertificateInformationTO](toCertificateInformationTO)
  }

  private def generateCertificateReport(certificateData: List[CertificateInformationTO], certificateDetails: CertificateDetails): Array[Byte] = {
    if (certificateData.isEmpty) {
      return null
    }
    val parameters: util.HashMap[String, Object] = new util.HashMap()
    val baseURL = certificateData.head.getBaseURL.split("Kornell.nocache.html").head
    //TODO: both urls NEED the extra slash because the jasper files count on it
    parameters.put("institutionURL", mkurl(baseURL, "repository", certificateData.head.getAssetsRepositoryUUID, ContentService.PREFIX, ContentService.INSTITUTION) + "/")
    parameters.put("assetsURL", mkurl(baseURL, certificateDetails.getBgImage) + "/")

    val cl = Thread.currentThread.getContextClassLoader
    val stream = cl.getResourceAsStream(certificateDetails.getCertificateType.getPath)
    getReportBytesFromStream(certificateData, parameters, stream, "pdf")

  }

  def generateCourseClassCertificates(courseClassUUID: String, peopleTO: SimplePeopleTO): String = {
    try {
      val people = peopleTO.getSimplePeopleTO
      val enrollmentUUIDs = {
        if (people != null && people.size > 0) {
          var enrollmentUUIDsVar = ""
          for (i <- 0 until people.size) {
            val person = people.get(i)
            val enrollmentUUID = EnrollmentsRepo.byCourseClassAndUsername(courseClassUUID, person.getUsername)
            if (enrollmentUUID.isDefined) {
              if (enrollmentUUIDsVar.length != 0) enrollmentUUIDsVar += ","
              enrollmentUUIDsVar += "'" + enrollmentUUID.get + "'"
            }
          }
          enrollmentUUIDsVar
        } else null
      }

      val certificateInformationTOsByCourseClass = ReportCertificateGenerator.getCertificateInformationTOsByCourseClass(courseClassUUID, enrollmentUUIDs)
      if (certificateInformationTOsByCourseClass.isEmpty) {
        throw new ServerErrorException("errorGeneratingReport")
      } else {
        val report = generateCertificate(certificateInformationTOsByCourseClass)
        val bs = new ByteArrayInputStream(report)
        val person = PersonRepo(ThreadLocalAuthenticator.getAuthenticatedPersonUUID.get).get
        val repo = ContentManagers.forRepository(ContentRepositoriesRepo.firstRepositoryByInstitution(person.getInstitutionUUID).get.getUUID)
        val filename = ReportCertificateGenerator.getCourseClassCertificateReportFileName(courseClassUUID)

        repo.put(
          bs,
          report.length,
          "application/pdf",
          "Content-Disposition: attachment; filename=\"" + filename + "\"",
          Map("certificatedata" -> "09/01/1980", "requestedby" -> person.getFullName), filename)

        getCourseClassCertificateReportURL(courseClassUUID)
      }
    } catch {
      case e: Exception =>
        throw new ServerErrorException("errorGeneratingReport", e)
    }
  }

  def courseClassCertificateExists(courseClassUUID: String): String = {
    try {
      val url = getCourseClassCertificateReportURL(courseClassUUID)
      HttpURLConnection.setFollowRedirects(false)
      val con = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
      con.setRequestMethod("HEAD")
      if (con.getResponseCode == HttpURLConnection.HTTP_OK) url else ""
    } catch {
      case e: Exception => throw new ServerErrorException("errorCheckingCerts", e)
    }
  }

  def getCourseClassCertificateReportFileName(courseClassUUID: String): String = {
    mkurl(ContentService.PREFIX, ContentService.REPORTS, ContentService.CERTIFICATES, "certificates-" + ThreadLocalAuthenticator.getAuthenticatedPersonUUID.get + courseClassUUID + ".pdf")
  }

  def getCourseClassCertificateReportURL(courseClassUUID: String): String = {
    val institutionUUID = getInstitutionUUID(courseClassUUID)
    val repo = ContentManagers.forRepository(ContentRepositoriesRepo.firstRepositoryByInstitution(institutionUUID).get.getUUID)
    val key = getCourseClassCertificateReportFileName(courseClassUUID)
    mkurl(InstitutionRepo(institutionUUID).get.getBaseURL.split("Kornell.nocache.html").head, repo.url(key))
  }
}
