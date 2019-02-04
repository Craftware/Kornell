package kornell.server.jdbc.repository

import java.util.Date

import kornell.core.entity.{AuditedEntityType, CourseDetailsEntityType, CourseVersion, EntityState}
import kornell.core.error.exception.EntityConflictException
import kornell.core.util.{StringUtils, UUID}
import kornell.server.content.ContentManagers
import kornell.server.jdbc.SQL._
import kornell.server.service.{AssetService, ContentService}
import java.util.logging.Logger


class CourseVersionRepo(uuid: String) {

  val logger: Logger = Logger.getLogger(classOf[CourseVersionRepo].getName)

  val finder = sql"select * from CourseVersion where uuid = $uuid and state <> ${EntityState.deleted.toString}"

  def get: CourseVersion = finder.get[CourseVersion]

  def first: Option[CourseVersion] = finder.first[CourseVersion]

  def update(courseVersion: CourseVersion, skipAudit: Boolean, institutionUUID: String): CourseVersion = {
    //get previous version
    val oldCourseVersion = CourseVersionRepo(courseVersion.getUUID).first.get

    val courseVersionExists = sql"""
      select count(*) from CourseVersion
      where courseUUID = ${courseVersion.getCourseUUID}
      and (name = ${courseVersion.getName} or
        distributionPrefix = ${courseVersion.getDistributionPrefix})
      and uuid <> ${courseVersion.getUUID}
      and state <> ${EntityState.deleted.toString}
    """.first[String].get
    if (courseVersionExists == "0") {
      sql"""
      | update CourseVersion c
      | set c.name = ${courseVersion.getName},
      | c.courseUUID = ${courseVersion.getCourseUUID},
      | c.versionCreatedAt = ${courseVersion.getVersionCreatedAt},
      | c.distributionPrefix = ${courseVersion.getDistributionPrefix},
      | c.state = ${courseVersion.getState.toString},
      | c.disabled = ${courseVersion.isDisabled},
      | c.parentVersionUUID = ${courseVersion.getParentVersionUUID},
      | c.instanceCount = ${courseVersion.getInstanceCount},
      | c.classroomJson = ${courseVersion.getClassroomJson},
      | c.classroomJsonPublished = ${courseVersion.getClassroomJsonPublished},
      | c.label = ${courseVersion.getLabel},
      | c.thumbUrl = ${courseVersion.getThumbUrl}
      | where c.uuid = ${courseVersion.getUUID}""".executeUpdate

      //log entity change
      if (!skipAudit) {
        EventsRepo.logEntityChange(institutionUUID, AuditedEntityType.courseVersion, courseVersion.getUUID, oldCourseVersion, courseVersion)
      }

      CourseClassesRepo.updateWizardClassesRequiredScore(courseVersion, skipAudit)

      courseVersion
    } else {
      throw new EntityConflictException("courseVersionAlreadyExists")
    }
  }

  def delete: CourseVersion = {
    val courseVersion = get
    if (CourseClassesRepo.countByCourseVersion(uuid) == 0) {
      sql"""
        update CourseVersion
        set state = ${EntityState.deleted.toString},
        name = concat(name, " - ", uuid),
        distributionPrefix = concat(distributionPrefix, " - ", uuid)
        where uuid = ${uuid}
      """.executeUpdate

      sql"""
        update CourseClass
        set state = ${EntityState.deleted.toString}
        where sandbox = 1 and courseVersionUUID = ${uuid}
      """.executeUpdate

      val course = CourseRepo(courseVersion.getCourseUUID).get
      val repo = ContentRepositoriesRepo.firstRepositoryByInstitution(course.getInstitutionUUID).get
      val cm = ContentManagers.forRepository(repo.getUUID)

      cm.deleteFolder(ContentService.CLASSROOMS, course.getCode, courseVersion.getDistributionPrefix)
      courseVersion
    } else {
      throw new EntityConflictException("versionHasClasses")
    }

  }

  def copy: CourseVersion = {
    val courseVersion = CourseVersionRepo(uuid).first.get
    val institutionUUID = CoursesRepo.byCourseVersionUUID(courseVersion.getUUID).get.getInstitutionUUID
    val sourceCourseVersionUUID = courseVersion.getUUID
    val targetCourseVersionUUID = UUID.random

    logger.fine(courseVersion.getThumbUrl)
    //copy courseVersion
    courseVersion.setUUID(targetCourseVersionUUID)
    courseVersion.setDistributionPrefix(targetCourseVersionUUID)
    courseVersion.setName(courseVersion.getName + " (2)")
    courseVersion.setVersionCreatedAt(new Date())
    if (StringUtils.isSome(courseVersion.getThumbUrl)) {
      courseVersion.setThumbUrl(courseVersion.getThumbUrl.replace(sourceCourseVersionUUID + "/thumb.jpg", targetCourseVersionUUID + "/thumb.jpg"))
    }
    logger.fine(courseVersion.getThumbUrl)
    CourseVersionsRepo.create(courseVersion, institutionUUID)

    AssetService.copyAssets(institutionUUID, CourseDetailsEntityType.COURSE_VERSION, sourceCourseVersionUUID, targetCourseVersionUUID, courseVersion.getThumbUrl)

    courseVersion
  }

  def getChildren: List[CourseVersion] = {
    sql"""
      select * from CourseVersion where parentVersionUUID = ${uuid}"""
      .map[CourseVersion](toCourseVersion)
  }

}

object CourseVersionRepo {
  def apply(uuid: String) = new CourseVersionRepo(uuid: String)
}
