package kornell.server.jdbc.repository

import java.util.UUID

import kornell.core.entity.{CourseDetailsEntityType, CourseDetailsSection}
import kornell.core.to.CourseDetailsSectionsTO
import kornell.server.jdbc.SQL._
import kornell.server.repository.TOs

object CourseDetailsSectionsRepo {

  def create(courseDetailsSection: CourseDetailsSection): CourseDetailsSection = {
    if (courseDetailsSection.getUUID == null) {
      courseDetailsSection.setUUID(UUID.randomUUID.toString)
    }
    sql"""
    | insert into CourseDetailsSection (uuid,text,entityType,entityUUID,`index`,title)
    | values(
    | ${courseDetailsSection.getUUID},
    | ${courseDetailsSection.getText},
    | ${courseDetailsSection.getEntityType.toString},
    | ${courseDetailsSection.getEntityUUID},
    | ${courseDetailsSection.getIndex},
    | ${courseDetailsSection.getTitle})""".executeUpdate

    courseDetailsSection
  }

  def getForEntity(entityUUID: String, entityType: CourseDetailsEntityType): CourseDetailsSectionsTO = {
    TOs.newCourseDetailsSectionsTO(sql"""
      select * from CourseDetailsSection where entityUUID = ${entityUUID} and entityType = ${entityType.toString} order by `index`
    """.map[CourseDetailsSection](toCourseDetailsSection))
  }

  def moveUp(entityUUID: String, entityType: CourseDetailsEntityType, index: Int): Unit = {
    val courseDetailsSections = CourseDetailsSectionsRepo.getForEntity(entityUUID, entityType).getCourseDetailsSections
    if (index >= 0 && courseDetailsSections.size > 1) {
      val currentSection = courseDetailsSections.get(index)
      val previousSection = courseDetailsSections.get(index - 1)

      currentSection.setIndex(index - 1)
      previousSection.setIndex(index)

      CourseDetailsSectionRepo(currentSection.getUUID).update(currentSection)
      CourseDetailsSectionRepo(previousSection.getUUID).update(previousSection)
    }
  }

  def moveDown(entityUUID: String, entityType: CourseDetailsEntityType, index: Int): Unit = {
    val courseDetailsSections = CourseDetailsSectionsRepo.getForEntity(entityUUID, entityType).getCourseDetailsSections
    if (index < (courseDetailsSections.size - 1) && courseDetailsSections.size > 1) {
      val currentSection = courseDetailsSections.get(index)
      val nextSection = courseDetailsSections.get(index + 1)

      currentSection.setIndex(index + 1)
      nextSection.setIndex(index)

      CourseDetailsSectionRepo(currentSection.getUUID).update(currentSection)
      CourseDetailsSectionRepo(nextSection.getUUID).update(nextSection)
    }
  }
}
