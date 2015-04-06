package kornell.server.jdbc.repository

import java.sql.ResultSet
import scala.collection.JavaConverters._
import kornell.core.entity.Course
import kornell.core.entity.Course
import kornell.server.jdbc.SQL._
import kornell.server.jdbc.SQL._
import kornell.server.repository.Entities._
import kornell.server.repository.TOs._
import kornell.server.repository.Entities
import kornell.core.util.UUID
import kornell.core.to.CoursesTO

object CoursesRepo {

  def create(course: Course): Course = {
    if (course.getUUID == null){
      course.setUUID(UUID.random)
    }    
    sql"""
    | insert into Course (uuid,code,title,description,infoJson,institutionUUID) 
    | values(
    | ${course.getUUID},
    | ${course.getCode},
    | ${course.getTitle}, 
    | ${course.getDescription},
    | ${course.getInfoJson},
    | ${course.getInstitutionUUID})""".executeUpdate
    course
  }  
  
  def byCourseClassUUID(courseClassUUID: String) = sql"""
	  select * from Course c join
	  CourseVersion cv on cv.course_uuid = c.uuid join
	  CourseClass cc on cc.courseVersion_uuid = cv.uuid where cc.uuid = $courseClassUUID
  """.first[Course]
  
  def byInstitution(institutionUUID: String): CoursesTO = byInstitution(true, institutionUUID)
  
  def byInstitution(fetchChildCourses: Boolean, institutionUUID: String): CoursesTO = newCoursesTO(
    sql"""
	  	select c.* from Course c
		join Institution i on c.institutionUUID = i.uuid
		where c.institutionUUID = $institutionUUID
		and (childCourse = false or $fetchChildCourses = true) 
		order by c.code
	  """.map[Course](toCourse))
  
}