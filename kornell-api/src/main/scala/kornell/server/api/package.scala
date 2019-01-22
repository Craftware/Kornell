package kornell.server

import java.util.logging.Logger

import kornell.core.entity.role.RoleCategory
import kornell.server.authentication.ThreadLocalAuthenticator
import kornell.server.jdbc.repository.{CourseClassRepo, CourseRepo, PersonRepo, RolesRepo}

package object api {
  val logger: Logger = Logger.getLogger("kornell.server.api")

  def isControlPanelAdmin: Boolean =
    RoleCategory.isControlPanelAdmin(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs)

  def isPlatformAdmin(institutionUUID: String): Boolean =
    RoleCategory.isPlatformAdmin(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, institutionUUID)

  def isPlatformAdmin: Boolean =
    RoleCategory.isPlatformAdmin(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, getAutenticatedPersonInstitutionUUID)

  def isInstitutionAdmin(institutionUUID: String): Boolean =
    RoleCategory.isInstitutionAdmin(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, institutionUUID)

  def isInstitutionAdmin: Boolean =
    RoleCategory.isInstitutionAdmin(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, getAutenticatedPersonInstitutionUUID)

  def isCourseClassAdmin(courseClassUUID: String): Boolean =
    RoleCategory.isCourseClassAdmin(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, courseClassUUID)

  def isCourseClassTutor(courseClassUUID: String): Boolean =
    RoleCategory.isCourseClassTutor(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, courseClassUUID)

  def isCourseClassObserver(courseClassUUID: String): Boolean =
    RoleCategory.isCourseClassObserver(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, courseClassUUID)

  def isPublisher(institutionUUID: String): Boolean =
    RoleCategory.isPublisher(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, institutionUUID)

  def isPublisher: Boolean =
    RoleCategory.isPublisher(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, getAutenticatedPersonInstitutionUUID)

  def isInstitutionCourseClassesAdmin(institutionUUID: String): Boolean =
    RoleCategory.isInstitutionCourseClassesAdmin(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, institutionUUID)

  def isInstitutionCourseClassesAdmin: Boolean =
    RoleCategory.isInstitutionCourseClassesAdmin(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, getAutenticatedPersonInstitutionUUID)

  def isInstitutionCourseClassesObserver(institutionUUID: String): Boolean =
    RoleCategory.isInstitutionCourseClassesObserver(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, institutionUUID)

  def isInstitutionCourseClassesObserver: Boolean =
    RoleCategory.isInstitutionCourseClassesObserver(new RolesRepo().getUserRoles(getAuthenticatedPersonUUID, RoleCategory.BIND_DEFAULT).getRoleTOs, getAutenticatedPersonInstitutionUUID)

  def getAuthenticatedPersonUUID: String = ThreadLocalAuthenticator.getAuthenticatedPersonUUID.orNull

  def getAutenticatedPersonInstitutionUUID: String = PersonRepo(getAuthenticatedPersonUUID).get.getInstitutionUUID

  def getInstitutionUUID(courseClassUUID: String, courseUUID: String = null): String = if (courseUUID != null) CourseRepo(courseUUID).get.getInstitutionUUID else CourseClassRepo(courseClassUUID).get.getInstitutionUUID

}
