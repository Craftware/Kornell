package kornell.server.api

import scala.collection.JavaConverters._
import javax.ws.rs._
import javax.ws.rs.core._
import kornell.core.shared.data._
import kornell.server.repository.jdbc.Courses
import kornell.core.shared.to.CoursesTO
import kornell.core.shared.to.CourseTO
import kornell.core.shared.to.CoursesTO
import kornell.core.shared.to.CourseTO
import kornell.server.repository.jdbc.Courses

@Path("courses")
class CoursesResource {
  @GET
  @Produces(Array(CoursesTO.TYPE))
  def getCourses(implicit @Context sc: SecurityContext) = ???
  
  @Path("{uuid}")
  def getCourse(@PathParam("uuid") uuid:String) = CourseResource(uuid)
}
