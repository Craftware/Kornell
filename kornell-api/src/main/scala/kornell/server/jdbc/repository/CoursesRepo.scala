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
import kornell.core.entity.AuditedEntityType

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
	    
    //log creation event
    EventsRepo.logEntityChange(course.getInstitutionUUID, AuditedEntityType.course, course.getUUID, null, course)
    
    course
  }  
  
  def byCourseClassUUID(courseClassUUID: String) = sql"""
	  select * from Course c join
	  CourseVersion cv on cv.course_uuid = c.uuid join
	  CourseClass cc on cc.courseVersion_uuid = cv.uuid where cc.uuid = $courseClassUUID
  """.first[Course]
  
  def byInstitution(fetchChildCourses: Boolean, institutionUUID: String, searchTerm: String, pageSize: Int, pageNumber: Int): CoursesTO = {
    val resultOffset = (pageNumber.max(1) - 1) * pageSize
    val filteredSearchTerm = '%' + Option(searchTerm).getOrElse("") + '%'
    
    val coursesTO = newCoursesTO(sql"""
	  	select c.* from Course c
		join Institution i on c.institutionUUID = i.uuid
		where c.institutionUUID = ${institutionUUID}
		and (childCourse = false or $fetchChildCourses = true)
		and (c.title like ${filteredSearchTerm}
            or c.code like ${filteredSearchTerm})
		order by c.code limit ${resultOffset}, ${pageSize} 
	  """.map[Course](toCourse))
	  coursesTO.setPageSize(pageSize)
	  coursesTO.setPageNumber(pageNumber.max(1))
	  coursesTO.setCount({
	    sql"""select count(c.uuid) from Course c where c.institutionUUID = ${institutionUUID}
	    	and (childCourse = false or $fetchChildCourses = true)"""
	    	.first[String].get.toInt
	  })
	  coursesTO.setSearchCount({
    	  if (searchTerm == "")
    		  0
		  else
		    sql"""select count(c.uuid) from Course c where c.institutionUUID = ${institutionUUID}
    	  		and c.title like ${filteredSearchTerm} 
    	  		and (childCourse = false or $fetchChildCourses = true)"""
    	  		.first[String].get.toInt
    	})
   coursesTO
  }
}