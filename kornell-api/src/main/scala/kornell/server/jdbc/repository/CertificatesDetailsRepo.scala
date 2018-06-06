package kornell.server.jdbc.repository

import kornell.core.entity.{CertificateDetails, CourseDetailsEntityType}
import kornell.core.util.UUID
import kornell.server.jdbc.SQL._

object CertificatesDetailsRepo {

  def create(certificateDetails: CertificateDetails): CertificateDetails = {
    if (certificateDetails.getUUID == null) {
      certificateDetails.setUUID(UUID.random)
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
}
