package kornell.server.api

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.SecurityContext
import kornell.core.entity.Assessment
import kornell.core.entity.Enrollment
import kornell.core.entity.RoleCategory
import kornell.core.lom.Contents
import kornell.server.jdbc.SQL._
import kornell.server.jdbc.repository.AuthRepo
import kornell.server.jdbc.repository.CourseClassRepo
import kornell.server.jdbc.repository.EnrollmentRepo
import kornell.server.jdbc.repository.PersonRepo
import kornell.server.jdbc.repository.RegistrationsRepo
import scala.collection.JavaConverters._
import kornell.server.util.Errors._
import kornell.server.util.Conditional.toConditional

@Produces(Array(Enrollment.TYPE))
class EnrollmentResource(uuid: String) {
  lazy val enrollment = get
  lazy val enrollmentRepo = EnrollmentRepo(uuid)

  def get = enrollmentRepo.get

  @GET
  def first = enrollmentRepo.first

  @PUT
  @Produces(Array("text/plain"))
  @Path("acceptTerms")
  def acceptTerms(implicit @Context sc: SecurityContext) = AuthRepo.withPerson { p =>
    RegistrationsRepo(p.getUUID, uuid).acceptTerms
  }

  @PUT
  @Produces(Array("text/plain"))
  @Consumes(Array(Enrollment.TYPE))
  def update(implicit @Context sc: SecurityContext, 
      @Context resp: HttpServletResponse, enrollment: Enrollment) = AuthRepo.withPerson { p =>
    if(!PersonRepo(p.getUUID).hasPowerOver(enrollment.getPerson.getUUID))
    	resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized attempt to update an enrollment of another person.");
    else
    	EnrollmentRepo(enrollment.getUUID).update(enrollment)
  }

  @Path("actoms/{actomKey}")
  def actom(@PathParam("actomKey") actomKey: String) = ActomResource(uuid, actomKey)

  @GET
  @Path("contents")
  @Produces(Array(Contents.TYPE))
  def contents(implicit @Context sc: SecurityContext): Option[Contents] = first map { e =>
    CourseClassResource(e.getCourseClassUUID).getLatestContents(sc)
  }

  @GET
  @Path("approved")
  @Produces(Array("application/boolean"))
  def approved =  first map { Assessment.PASSED == _.getAssessment } get
  
  
  @DELETE
  @Produces(Array(Enrollment.TYPE))
  def delete(implicit @Context sc: SecurityContext) = {
    val enrollmentRepo = EnrollmentRepo(uuid)
    val enrollment = enrollmentRepo.get   
    enrollmentRepo.delete(uuid)
    enrollment
  }.requiring(isPlatformAdmin, UserNotInRole)
    .or(isInstitutionAdmin(CourseClassRepo(EnrollmentRepo(uuid).get.getCourseClassUUID).get.getInstitutionUUID), UserNotInRole)
    .or(isCourseClassAdmin(EnrollmentRepo(uuid).get.getCourseClassUUID), UserNotInRole)
    
  
}