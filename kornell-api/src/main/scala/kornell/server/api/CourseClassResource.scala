package kornell.server.api

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import javax.ws.rs._
import javax.ws.rs.core.{Context, SecurityContext}
import kornell.core.entity.role.{RoleCategory, RoleType, Roles}
import kornell.core.entity.{ChatThreadType, CourseClass}
import kornell.core.error.exception.{EntityConflictException, EntityNotFoundException, UnauthorizedAccessException}
import kornell.core.to.{CourseClassTO, LibraryFilesTO, RolesTO}
import kornell.server.jdbc.repository.{AuthRepo, ChatThreadsRepo, CourseClassRepo, CourseClassesRepo, RolesRepo}
import kornell.server.repository.LibraryFilesRepository
import kornell.server.service.ContentService
import kornell.server.util.AccessDeniedErr
import kornell.server.util.Conditional.toConditional

class CourseClassResource(uuid: String) {

  @GET
  @Path("to")
  @Produces(Array(CourseClassTO.TYPE))
  def getTO(implicit @Context sc: SecurityContext): Option[CourseClassTO] =
    AuthRepo().withPerson { person =>
      CourseClassesRepo.getCourseClassTO(person.getInstitutionUUID, uuid)
    }.requiring(isPlatformAdmin, AccessDeniedErr())
      .or(isInstitutionAdmin, AccessDeniedErr())
      .or(isCourseClassAdmin(uuid), AccessDeniedErr())
      .or(isCourseClassTutor(uuid), AccessDeniedErr())
      .or(isCourseClassObserver(uuid), AccessDeniedErr())
      .get

  @PUT
  @Consumes(Array(CourseClass.TYPE))
  @Produces(Array(CourseClass.TYPE))
  def update(courseClass: CourseClass): CourseClass = AuthRepo().withPerson { p =>
    val roles = new RolesRepo().getUserRoles(p.getUUID, RoleCategory.BIND_DEFAULT).getRoleTOs
    if (!(RoleCategory.isPlatformAdmin(roles, courseClass.getInstitutionUUID) ||
      RoleCategory.isInstitutionAdmin(roles, courseClass.getInstitutionUUID)))
      throw new UnauthorizedAccessException("classNoRights")
    else
      try {
        CourseClassRepo(uuid).update(courseClass)
      } catch {
        case _: MySQLIntegrityConstraintViolationException =>
          throw new EntityConflictException("constraintViolatedUUIDName")
      }
  }

  @DELETE
  @Produces(Array(CourseClass.TYPE))
  def delete(): CourseClass = AuthRepo().withPerson { p =>
    val courseClass = CourseClassRepo(uuid).get
    if (courseClass == null)
      throw new EntityNotFoundException("classNotFound")

    val roles = new RolesRepo().getUserRoles(p.getUUID, RoleCategory.BIND_DEFAULT).getRoleTOs
    val institutionUUID = CourseClassRepo(uuid).get.getInstitutionUUID
    if (!(RoleCategory.isPlatformAdmin(roles, institutionUUID) ||
      RoleCategory.isInstitutionAdmin(roles, institutionUUID)))
      throw new UnauthorizedAccessException("classNoRights")
    else
      try {
        CourseClassRepo(uuid).delete
        courseClass
      } catch {
        case _: MySQLIntegrityConstraintViolationException =>
          throw new EntityConflictException("constraintViolatedUUIDName")
      }
  }

  @POST
  @Path("copy")
  @Produces(Array(CourseClass.TYPE))
  def copy: CourseClass = {
    CourseClassRepo(uuid).copy
  }.requiring(isPlatformAdmin, AccessDeniedErr())
    .or(isInstitutionAdmin, AccessDeniedErr())
    .get

  @Produces(Array(LibraryFilesTO.TYPE))
  @Path("libraryFiles")
  @GET
  def getLibraryFiles: LibraryFilesTO = LibraryFilesRepository.findLibraryFiles(uuid)

  @PUT
  @Consumes(Array(Roles.TYPE))
  @Produces(Array(Roles.TYPE))
  @Path("admins")
  def updateAdmins(roles: Roles): Roles = AuthRepo().withPerson { person =>
    {
      val r = new RolesRepo().updateCourseClassAdmins(person.getInstitutionUUID, uuid, roles)
      ChatThreadsRepo.updateParticipantsInThreads(uuid, person.getInstitutionUUID, ChatThreadType.SUPPORT)
      ChatThreadsRepo.updateParticipantsInThreads(uuid, person.getInstitutionUUID, ChatThreadType.INSTITUTION_SUPPORT)
      r
    }
  }.requiring(isPlatformAdmin, AccessDeniedErr())
    .or(isInstitutionAdmin, AccessDeniedErr())
    .get

  @GET
  @Produces(Array(RolesTO.TYPE))
  @Path("admins")
  def getAdmins(@QueryParam("bind") bindMode: String): RolesTO = {
    new RolesRepo().getUsersForCourseClassByRole(uuid, RoleType.courseClassAdmin, bindMode)
  }.requiring(isPlatformAdmin, AccessDeniedErr())
    .or(isInstitutionAdmin, AccessDeniedErr())
    .get

  @PUT
  @Consumes(Array(Roles.TYPE))
  @Produces(Array(Roles.TYPE))
  @Path("tutors")
  def updateTutors(roles: Roles): Roles = AuthRepo().withPerson { person =>
    {
      val r = new RolesRepo().updateTutors(person.getInstitutionUUID, uuid, roles)
      ChatThreadsRepo.updateParticipantsInThreads(uuid, person.getInstitutionUUID, ChatThreadType.TUTORING)
      r
    }
  }.requiring(isPlatformAdmin, AccessDeniedErr())
    .or(isInstitutionAdmin, AccessDeniedErr())
    .get

  @GET
  @Produces(Array(RolesTO.TYPE))
  @Path("tutors")
  def getTutors(@QueryParam("bind") bindMode: String): RolesTO = {
    new RolesRepo().getUsersForCourseClassByRole(uuid, RoleType.tutor, bindMode)
  }.requiring(isPlatformAdmin, AccessDeniedErr())
    .or(isInstitutionAdmin, AccessDeniedErr())
    .get

  @PUT
  @Consumes(Array(Roles.TYPE))
  @Produces(Array(Roles.TYPE))
  @Path("observers")
  def updateObservers(roles: Roles): Roles = AuthRepo().withPerson { person =>
    new RolesRepo().updateCourseClassObservers(person.getInstitutionUUID, uuid, roles)
  }.requiring(isPlatformAdmin, AccessDeniedErr())
    .or(isInstitutionAdmin, AccessDeniedErr())
    .get

  @GET
  @Produces(Array(RolesTO.TYPE))
  @Path("observers")
  def getObservers(@QueryParam("bind") bindMode: String): RolesTO = {
    new RolesRepo().getUsersForCourseClassByRole(uuid, RoleType.courseClassObserver, bindMode)
  }.requiring(isPlatformAdmin, AccessDeniedErr())
    .or(isInstitutionAdmin, AccessDeniedErr())
    .get

  @GET
  @Path("uploadUrl")
  @Produces(Array("text/plain"))
  def getUploadUrl(@QueryParam("filename") filename: String, @QueryParam("path") path: String): String = {
    ContentService.getCourseClassUploadUrl(uuid, filename, path)
  }.requiring(isPlatformAdmin, AccessDeniedErr())
    .or(isInstitutionAdmin, AccessDeniedErr())
    .get
}

object CourseClassResource {
  def apply(uuid: String) = new CourseClassResource(uuid)
}
