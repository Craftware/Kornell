package kornell.server.api

import scala.collection.JavaConverters.setAsJavaSetConverter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.SecurityContext
import kornell.core.entity.CourseClass
import kornell.core.entity.RoleCategory
import kornell.server.jdbc.repository.AuthRepo
import kornell.server.jdbc.repository.CourseClassesRepo
import kornell.server.util.Conditional.toConditional
import kornell.core.to.CourseClassesTO
import kornell.server.util.Errors._

@Path("courseClasses")
class CourseClassesResource {

  @PUT
  @Consumes(Array(CourseClass.TYPE))
  @Produces(Array(CourseClass.TYPE))
  def create(implicit @Context sc: SecurityContext, courseClass: CourseClass) = {
    CourseClassesRepo.create(courseClass)
  }.requiring(isPlatformAdmin, UserNotInRole)
    .or(isInstitutionAdmin(courseClass.getInstitutionUUID), UserNotInRole)
  
  @Path("{uuid}")
  def get(@PathParam("uuid") uuid: String):CourseClassResource = CourseClassResource(uuid)

  @GET
  @Produces(Array(CourseClassesTO.TYPE))
  def getClasses(implicit @Context sc: SecurityContext, @QueryParam("institutionUUID") institutionUUID: String) =
    AuthRepo().withPerson { person =>
      {
        if (institutionUUID != null) {
          CourseClassesRepo.byPersonAndInstitution(person.getUUID, institutionUUID)
        }
      }
    }

  @GET
  @Produces(Array(CourseClassesTO.TYPE))
  @Path("administrated")
  def getAdministratedClasses(implicit @Context sc: SecurityContext, @QueryParam("institutionUUID") institutionUUID: String) =
    AuthRepo().withPerson { person =>
      {
        if (institutionUUID != null) {
          val roles = AuthRepo().userRoles
          CourseClassesRepo.administratedByPersonOnInstitution(person, institutionUUID, roles.toList )
        }
      }
    }
}
