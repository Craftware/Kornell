package kornell.server.jdbc.repository

import java.util.UUID

import kornell.core.entity.{AuditedEntityType, Course, CourseDetailsEntityType, EntityState}
import kornell.core.error.exception.EntityConflictException
import kornell.core.util.StringUtils
import kornell.server.jdbc.SQL._
import kornell.server.service.AssetService

class CourseRepo(uuid: String) {

  val finder = sql"select * from Course where uuid = $uuid and state <> ${EntityState.deleted.toString}"

  def get: Course = finder.get[Course]
  def first: Option[Course] = finder.first[Course]

  def update(course: Course): Course = {
    //get previous course
    val oldCourse = CourseRepo(course.getUUID).first.get

    val courseExists = sql"""
      select count(*) from Course
      where institutionUUID = ${course.getInstitutionUUID}
      and (name = ${course.getName} or
        code = ${course.getCode})
      and uuid <> ${course.getUUID}
      and state <> ${EntityState.deleted.toString}
    """.first[String].get
    if (courseExists == "0") {
      sql"""
      | update Course c set
      | c.code = ${course.getCode},
      | c.name = ${course.getName},
      | c.description = ${course.getDescription},
      | c.infoJson = ${course.getInfoJson},
      | c.state = ${course.getState.toString},
      | c.institutionUUID = ${course.getInstitutionUUID},
      | c.childCourse = ${course.isChildCourse},
      | c.thumbUrl = ${course.getThumbUrl},
      | c.contentSpec = ${course.getContentSpec.toString}
      | where c.uuid = ${course.getUUID}""".executeUpdate

      //log entity change
      EventsRepo.logEntityChange(course.getInstitutionUUID, AuditedEntityType.course, course.getUUID, oldCourse, course)

      course
    } else {
      throw new EntityConflictException("courseAlreadyExists")
    }
  }

  def delete: Course = {
    val course = get
    if (CourseVersionsRepo.countByCourse(uuid) == 0) {
      sql"""
        update Course
        set state = ${EntityState.deleted.toString},
        name = concat(name, " - ", uuid),
        code = concat(code, " - ", uuid)
        where uuid = ${uuid}
      """.executeUpdate
      course
    } else {
      throw new EntityConflictException("courseHasVersions")
    }
  }

  def copy: Course = {
    val course = CourseRepo(uuid).first.get
    val sourceCourseUUID = course.getUUID
    val targetCourseUUID = UUID.randomUUID.toString

    //copy course
    course.setUUID(targetCourseUUID)
    course.setCode(targetCourseUUID)
    course.setName(course.getName + " (2)")
    if (StringUtils.isSome(course.getThumbUrl)) {
      course.setThumbUrl(course.getThumbUrl.replace(sourceCourseUUID, targetCourseUUID))
    }
    CoursesRepo.create(course)

    AssetService.copyAssets(course.getInstitutionUUID, CourseDetailsEntityType.COURSE, sourceCourseUUID, targetCourseUUID, course.getThumbUrl)

    course
  }

}

object CourseRepo {
  def apply(uuid: String) = new CourseRepo(uuid)
}
