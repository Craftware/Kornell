package kornell.server.api

import javax.ws.rs._
import javax.ws.rs.core.Response
import kornell.core.entity.{Enrollment, EnrollmentState}
import kornell.core.to.{DashboardLeaderboardTO, EnrollmentRequestsTO, EnrollmentsTO, SimplePeopleTO}
import kornell.server.jdbc.SQL.SQLHelper
import kornell.server.jdbc.repository.{AuthRepo, CourseClassRepo, EnrollmentRepo, EnrollmentsRepo, EventsRepo, PersonRepo}
import kornell.server.service.RegistrationEnrollmentService
import kornell.server.util.AccessDeniedErr
import kornell.server.util.Conditional.toConditional

import scala.collection.JavaConverters._

@Path("enrollments")
@Produces(Array(Enrollment.TYPE))
class EnrollmentsResource {

  @Path("{uuid}")
  def get(@PathParam("uuid") uuid: String): EnrollmentResource = new EnrollmentResource(uuid)

  @POST
  @Consumes(Array(Enrollment.TYPE))
  @Produces(Array(Enrollment.TYPE))
  def create(enrollment: Enrollment): Enrollment = {
    val existingEnrollment = EnrollmentsRepo.find(enrollment.getPersonUUID, enrollment.getCourseClassUUID)
    if (existingEnrollment.isDefined) {
      val oldState = existingEnrollment.get.getState
      existingEnrollment.get.setState(enrollment.getState)
      EnrollmentRepo(existingEnrollment.get.getUUID).update(existingEnrollment.get)
      EventsRepo.logEnrollmentStateChanged(getAuthenticatedPersonUUID, existingEnrollment.get.getUUID, oldState, existingEnrollment.get.getState, false, null)
      existingEnrollment.get
    } else {
      val e = EnrollmentsRepo.create(enrollment)
      EventsRepo.logEnrollmentStateChanged(getAuthenticatedPersonUUID, e.getUUID, EnrollmentState.notEnrolled, e.getState, false, null)
      e
    }
  }.requiring(PersonRepo(getAuthenticatedPersonUUID).hasPowerOver(enrollment.getPersonUUID), AccessDeniedErr())
    .requiring(CourseClassRepo(enrollment.getCourseClassUUID).get.isPublicClass == true, AccessDeniedErr())
    .requiring(enrollment.getState.equals(EnrollmentState.requested) ||
      (enrollment.getState.equals(EnrollmentState.enrolled) && CourseClassRepo(enrollment.getCourseClassUUID).get.isApproveEnrollmentsAutomatically == true), AccessDeniedErr())
    .get

  @GET
  @Produces(Array(EnrollmentsTO.TYPE))
  def getByCourseClassUUID(@QueryParam("courseClassUUID") courseClassUUID: String, @QueryParam("searchTerm") searchTerm: String,
    @QueryParam("ps") pageSize: Int, @QueryParam("pn") pageNumber: Int, @QueryParam("orderBy") orderBy: String,
                           @QueryParam("asc") asc: String, @QueryParam("bind") bindMode: String): EnrollmentsTO = {
    EnrollmentsRepo.byCourseClassPaged(courseClassUUID, searchTerm, pageSize, pageNumber, orderBy, asc == "true", bindMode)
  }.requiring(isPlatformAdmin(CourseClassRepo(courseClassUUID).get.getInstitutionUUID), AccessDeniedErr())
    .or(isInstitutionAdmin(CourseClassRepo(courseClassUUID).get.getInstitutionUUID), AccessDeniedErr())
    .or(isCourseClassAdmin(courseClassUUID), AccessDeniedErr())
    .or(isCourseClassTutor(courseClassUUID), AccessDeniedErr())
    .or(isCourseClassObserver(courseClassUUID), AccessDeniedErr()).get

  @PUT
  @Path("requests")
  @Consumes(Array(kornell.core.to.EnrollmentRequestsTO.TYPE))
  def putEnrollments(enrollmentRequests: EnrollmentRequestsTO): Response =
    AuthRepo().withPerson { p =>
      //TODO: <strike>Understand and refactor</strike> Return an error
      if (enrollmentRequests.getEnrollmentRequests.asScala exists (e => !RegistrationEnrollmentService.isInvalidRequestEnrollment(e, p.getUUID))) {
        RegistrationEnrollmentService.deanRequestEnrollments(enrollmentRequests, p)
      }
      Response.noContent.build
    }

  @PUT
  @Path("{courseClassUUID}/notesUpdated")
  def putNotesChange(@PathParam("courseClassUUID") courseClassUUID: String, notes: String): Response = {
    AuthRepo().withPerson { p =>
      sql"""
      update Enrollment set notes=$notes
      where personUUID=${p.getUUID}
      and courseClassUUID=${courseClassUUID}
      """.executeUpdate
      Response.noContent.build
    }
  }

  @GET
  @Path("{courseClassUUID}/simpleEnrollments")
  @Produces(Array(SimplePeopleTO.TYPE))
  def getEnrollmentsList(@PathParam("courseClassUUID") courseClassUUID: String): SimplePeopleTO = {
    EnrollmentsRepo.simplePersonList(courseClassUUID)
  }.requiring(isPlatformAdmin(CourseClassRepo(courseClassUUID).get.getInstitutionUUID), AccessDeniedErr())
    .or(isInstitutionAdmin(CourseClassRepo(courseClassUUID).get.getInstitutionUUID), AccessDeniedErr())
    .or(isCourseClassAdmin(courseClassUUID), AccessDeniedErr())
    .or(isCourseClassTutor(courseClassUUID), AccessDeniedErr())
    .or(isCourseClassObserver(courseClassUUID), AccessDeniedErr()).get

  @GET
  @Path("leaderboard/{dashboardEnrollmentUUID}")
  @Produces(Array(DashboardLeaderboardTO.TYPE))
  def leaderboard(@PathParam("dashboardEnrollmentUUID") dashboardEnrollmentUUID: String): DashboardLeaderboardTO =
    EnrollmentsRepo.getLeaderboardForDashboard(dashboardEnrollmentUUID)

  @GET
  @Path("leaderboardPosition/{dashboardEnrollmentUUID}")
  @Produces(Array("text/plain"))
  def leaderboardPosition(@PathParam("dashboardEnrollmentUUID") dashboardEnrollmentUUID: String): String =
    EnrollmentsRepo.getLeaderboardPosition(dashboardEnrollmentUUID)

}

object EnrollmentsResource {
  def apply() = new EnrollmentsResource()
}
