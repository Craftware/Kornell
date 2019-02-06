package kornell.server.jdbc.repository

import java.util.UUID

import kornell.core.entity.{CertificateDetails, CourseDetailsEntityType}
import kornell.server.jdbc.SQL._

object CertificatesDetailsRepo {

  def create(certificateDetails: CertificateDetails): CertificateDetails = {
    if (certificateDetails.getUUID == null) {
      certificateDetails.setUUID(UUID.randomUUID.toString)
    }
    sql"""
         | insert into CertificateDetails (uuid,bgImage,certificateType,entityType,entityUUID)
         | values(
         | ${certificateDetails.getUUID},
         | ${certificateDetails.getBgImage},
         | ${certificateDetails.getCertificateType.toString},
         | ${certificateDetails.getEntityType.toString},
         | ${certificateDetails.getEntityUUID})""".executeUpdate

    certificateDetails
  }

  def getForEntity(entityUUID: String, entityType: CourseDetailsEntityType): Option[CertificateDetails] = {
    sql"""
      select * from CertificateDetails where entityUUID = ${entityUUID} and entityType = ${entityType.toString}
    """.first[CertificateDetails]
  }

  def findCertificateDetails(courseUUID: String, courseVersionUUID: String, courseClassUUID: String): Option[CertificateDetails] = {
    val courseClassCertificateDetails = getForEntity(courseClassUUID, CourseDetailsEntityType.COURSE_CLASS)
    val courseVersionCertificateDetails = getForEntity(courseVersionUUID, CourseDetailsEntityType.COURSE_VERSION)
    val courseCertificateDetails = getForEntity(courseUUID, CourseDetailsEntityType.COURSE)
    if (courseClassCertificateDetails.isDefined)
      courseClassCertificateDetails
    else if (courseVersionCertificateDetails.isDefined)
      courseVersionCertificateDetails
    else
      courseCertificateDetails
  }
}
