package kornell.server.api

import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.POST
import javax.ws.rs.Consumes
import javax.ws.rs.Produces
import kornell.core.entity.CertificateDetails
import kornell.server.jdbc.repository.PersonRepo
import kornell.server.util.AccessDeniedErr
import kornell.server.jdbc.repository.CertificatesDetailsRepo
import kornell.server.util.Conditional.toConditional


@Path("certificatesDetails")
class CertificatesDetailsResource {
  
  @Path("{uuid}")
  def get(@PathParam("uuid") uuid: String) = CertificateDetailsResource(uuid)
   
  @POST
  @Consumes(Array(CertificateDetails.TYPE))
  @Produces(Array(CertificateDetails.TYPE))
  def create(certificateDetails: CertificateDetails) = {
    CertificatesDetailsRepo.create(certificateDetails)
  }.requiring(isPlatformAdmin(PersonRepo(getAuthenticatedPersonUUID).get.getInstitutionUUID), AccessDeniedErr())
   .or(isInstitutionAdmin(PersonRepo(getAuthenticatedPersonUUID).get.getInstitutionUUID), AccessDeniedErr())
   .get
}

object CertificatesDetailsResource {
  def apply(uuid: String) = new CertificateDetailsResource(uuid)
}