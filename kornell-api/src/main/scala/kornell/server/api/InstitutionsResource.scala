package kornell.server.api

import javax.ws.rs._
import javax.ws.rs.core.Response
import kornell.core.entity.Institution
import kornell.core.to.CreateInstitutionTO
import kornell.server.jdbc.repository.InstitutionsRepo
import kornell.server.service.InstitutionService
import kornell.server.util.AccessDeniedErr
import kornell.server.util.Conditional.toConditional

@Path("institutions")
class InstitutionsResource {

  @Path("{uuid}")
  def get(@PathParam("uuid") uuid: String): InstitutionResource = new InstitutionResource(uuid)

  @POST
  @Produces(Array(Institution.TYPE))
  @Consumes(Array(Institution.TYPE))
  def create(institution: Institution): Institution = {
    InstitutionsRepo.create(institution)
  }.requiring(isControlPanelAdmin, AccessDeniedErr()).get

  @POST
  @Path("create")
  @Produces(Array("text/plain"))
  @Consumes(Array(CreateInstitutionTO.TYPE))
  def addNewInstitution(createInstitutionTO: CreateInstitutionTO): Response = {
    InstitutionService.create(createInstitutionTO)
    Response.noContent.build
  }.requiring(isControlPanelAdmin, AccessDeniedErr()).get

}

object InstitutionsResource {
  def apply() = new InstitutionsResource()
}
