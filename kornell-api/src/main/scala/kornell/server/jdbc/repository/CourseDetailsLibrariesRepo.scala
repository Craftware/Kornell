package kornell.server.jdbc.repository

import java.text.Normalizer
import java.util.UUID

import kornell.core.entity.{CourseDetailsEntityType, CourseDetailsLibrary}
import kornell.core.to.CourseDetailsLibrariesTO
import kornell.server.jdbc.SQL._
import kornell.server.repository.TOs

object CourseDetailsLibrariesRepo {

  def create(courseDetailsLibrary: CourseDetailsLibrary): CourseDetailsLibrary = {
    if (courseDetailsLibrary.getUUID == null) {
      courseDetailsLibrary.setUUID(UUID.randomUUID.toString)
    }
    val normalizedTitle = Normalizer.normalize(courseDetailsLibrary.getTitle, Normalizer.Form.NFC)
    sql"""
    | insert into CourseDetailsLibrary (uuid,title,entityType,entityUUID,`index`,description,size,path,uploadDate,fontAwesomeClassName)
    | values(
    | ${courseDetailsLibrary.getUUID},
    | ${normalizedTitle},
    | ${courseDetailsLibrary.getEntityType.toString},
    | ${courseDetailsLibrary.getEntityUUID},
    | ${courseDetailsLibrary.getIndex},
    | ${courseDetailsLibrary.getDescription},
    | ${courseDetailsLibrary.getSize},
    | ${courseDetailsLibrary.getPath},
    | ${courseDetailsLibrary.getUploadDate},
    | ${courseDetailsLibrary.getFontAwesomeClassName})""".executeUpdate

    courseDetailsLibrary
  }

  def getForEntity(entityUUID: String, entityType: CourseDetailsEntityType): CourseDetailsLibrariesTO = {
    TOs.newCourseDetailsLibrariesTO(sql"""
      select * from CourseDetailsLibrary where entityUUID = ${entityUUID} and entityType = ${entityType.toString} order by `index`
    """.map[CourseDetailsLibrary](toCourseDetailsLibrary))
  }

  def moveUp(entityUUID: String, entityType: CourseDetailsEntityType, index: Int): Unit = {
    val courseDetailsLibraries = CourseDetailsLibrariesRepo.getForEntity(entityUUID, entityType).getCourseDetailsLibraries
    if (index >= 0 && courseDetailsLibraries.size > 1) {
      val currentLibrary = courseDetailsLibraries.get(index)
      val previousLibrary = courseDetailsLibraries.get(index - 1)

      currentLibrary.setIndex(index - 1)
      previousLibrary.setIndex(index)

      CourseDetailsLibraryRepo(currentLibrary.getUUID).update(currentLibrary)
      CourseDetailsLibraryRepo(previousLibrary.getUUID).update(previousLibrary)
    }
  }

  def moveDown(entityUUID: String, entityType: CourseDetailsEntityType, index: Int): Unit = {
    val courseDetailsLibraries = CourseDetailsLibrariesRepo.getForEntity(entityUUID, entityType).getCourseDetailsLibraries
    if (index < (courseDetailsLibraries.size - 1) && courseDetailsLibraries.size > 1) {
      val currentLibrary = courseDetailsLibraries.get(index)
      val nextLibrary = courseDetailsLibraries.get(index + 1)

      currentLibrary.setIndex(index + 1)
      nextLibrary.setIndex(index)

      CourseDetailsLibraryRepo(currentLibrary.getUUID).update(currentLibrary)
      CourseDetailsLibraryRepo(nextLibrary.getUUID).update(nextLibrary)
    }
  }
}
