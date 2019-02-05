package kornell.server.jdbc.repository

import java.util.UUID

import kornell.core.entity.{AuditedEntityType, Course, EntityState}
import kornell.core.error.exception.EntityConflictException
import kornell.core.to.{CourseTO, CoursesTO}
import kornell.server.jdbc.PreparedStmt
import kornell.server.jdbc.SQL._
import kornell.server.repository.TOs

import scala.collection.JavaConverters._

object CoursesRepo {

  def create(course: Course): Course = {
    val courseExists = sql"""
      select count(*) from Course where
        institutionUUID = ${course.getInstitutionUUID} and
        (name = ${course.getName} or code = ${course.getCode})
        and state <> ${EntityState.deleted.toString}
      """.first[String].get
    if (courseExists == "0") {
      if (course.getUUID == null) {
        course.setUUID(UUID.randomUUID.toString)
      }
      sql"""
      | insert into Course (uuid,code,name,description,infoJson,state,institutionUUID, thumbUrl, contentSpec)
      | values(
      | ${course.getUUID},
      | ${course.getCode},
      | ${course.getName},
      | ${course.getDescription},
      | ${course.getInfoJson},
      | ${course.getState.toString},
      | ${course.getInstitutionUUID},
      | ${course.getThumbUrl},
      | ${course.getContentSpec.toString})""".executeUpdate

      //log creation event
      EventsRepo.logEntityChange(course.getInstitutionUUID, AuditedEntityType.course, course.getUUID, null, course)

      course
    } else {
      throw new EntityConflictException("courseAlreadyExists")
    }
  }

  def byCourseClassUUID(courseClassUUID: String): Option[Course] = sql"""
    select c.* from Course c join
    CourseVersion cv on cv.courseUUID = c.uuid join
    CourseClass cc on cc.courseVersionUUID = cv.uuid
    where cc.uuid = $courseClassUUID
    and c.state <> ${EntityState.deleted.toString}
    and cv.state <> ${EntityState.deleted.toString}
    and cc.state <> ${EntityState.deleted.toString}
  """.first[Course]

  def byCourseVersionUUID(courseVersionUUID: String): Option[Course] = sql"""
    select * from Course c join
    CourseVersion cv on cv.courseUUID = c.uuid
    where cv.uuid = $courseVersionUUID
    and c.state <> ${EntityState.deleted.toString}
  """.first[Course]

  def byInstitution(fetchChildCourses: Boolean, institutionUUID: String, searchTerm: String, pageSize: Int, pageNumber: Int, orderBy: String, asc: Boolean): CoursesTO = {
    val resultOffset = (pageNumber.max(1) - 1) * pageSize
    val filteredSearchTerm = '%' + Option(searchTerm).getOrElse("") + '%'
    val orderColumn = if (orderBy != null && !orderBy.contains(";")) orderBy else "c.code"
    val order = orderColumn + (if (asc) " asc" else " desc")

    val courses = new PreparedStmt(s"""
      select c.* from Course c
      join Institution i on c.institutionUUID = i.uuid
      where c.institutionUUID = '${institutionUUID}'
      and c.state <> '${EntityState.deleted.toString}'
      and (childCourse = false or $fetchChildCourses = true)
      and (c.name like '${filteredSearchTerm}'
              or c.code like '${filteredSearchTerm}')
      order by ${order}, c.name limit ${resultOffset}, ${pageSize}
    """, List[String]()).map[CourseTO](toCourseTO)

    val coursesTO = TOs.newCoursesTO
    coursesTO.setCourses(courses.asJava)
    coursesTO.setPageSize(pageSize)
    coursesTO.setPageNumber(pageNumber.max(1))
    coursesTO.setCount({
      sql"""select count(c.uuid) from Course c where c.institutionUUID = ${institutionUUID}
            and c.state <> ${EntityState.deleted.toString}
            and (childCourse = false or $fetchChildCourses = true)"""
        .first[String].get.toInt
    })
    coursesTO.setSearchCount({
      if (searchTerm == "")
        0
      else
        sql"""select count(c.uuid) from Course c
            where c.institutionUUID = ${institutionUUID}
            and c.state <> ${EntityState.deleted.toString}
            and (childCourse = false or $fetchChildCourses = true)
            and (c.name like ${filteredSearchTerm}
              or c.code like ${filteredSearchTerm})"""
          .first[String].get.toInt
    })

    bindCourseVersionsCounts(coursesTO)

    coursesTO
  }

  private def bindCourseVersionsCounts(coursesTO: CoursesTO): CoursesTO = {
    val courses = coursesTO.getCourses.asScala
    courses.foreach(cv => cv.setCourseVersionsCount(CourseVersionsRepo.countByCourse(cv.getCourse.getUUID)))
    coursesTO.setCourses(courses.asJava)
    coursesTO
  }
}
