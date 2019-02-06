package kornell.server.jdbc.repository

import java.util.UUID

import kornell.core.entity.{CourseDetailsEntityType, CourseDetailsHint}
import kornell.core.to.CourseDetailsHintsTO
import kornell.server.jdbc.SQL._
import kornell.server.repository.TOs

object CourseDetailsHintsRepo {

  def create(courseDetailsHint: CourseDetailsHint): CourseDetailsHint = {
    if (courseDetailsHint.getUUID == null) {
      courseDetailsHint.setUUID(UUID.randomUUID.toString)
    }
    sql"""
    | insert into CourseDetailsHint (uuid,title,text,entityType,entityUUID,`index`,fontAwesomeClassName)
    | values(
    | ${courseDetailsHint.getUUID},
    | ${courseDetailsHint.getTitle},
    | ${courseDetailsHint.getText},
    | ${courseDetailsHint.getEntityType.toString},
    | ${courseDetailsHint.getEntityUUID},
    | ${courseDetailsHint.getIndex},
    | ${courseDetailsHint.getFontAwesomeClassName})""".executeUpdate

    courseDetailsHint
  }

  def getForEntity(entityUUID: String, entityType: CourseDetailsEntityType): CourseDetailsHintsTO = {
    TOs.newCourseDetailsHintsTO(sql"""
      select * from CourseDetailsHint where entityUUID = ${entityUUID} and entityType = ${entityType.toString} order by `index`
    """.map[CourseDetailsHint](toCourseDetailsHint))
  }

  def moveUp(entityUUID: String, entityType: CourseDetailsEntityType, index: Int): Unit = {
    val courseDetailsHints = CourseDetailsHintsRepo.getForEntity(entityUUID, entityType).getCourseDetailsHints
    if (index >= 0 && courseDetailsHints.size > 1) {
      val currentHint = courseDetailsHints.get(index)
      val previousHint = courseDetailsHints.get(index - 1)

      currentHint.setIndex(index - 1)
      previousHint.setIndex(index)

      CourseDetailsHintRepo(currentHint.getUUID).update(currentHint)
      CourseDetailsHintRepo(previousHint.getUUID).update(previousHint)
    }
  }

  def moveDown(entityUUID: String, entityType: CourseDetailsEntityType, index: Int): Unit = {
    val courseDetailsHints = CourseDetailsHintsRepo.getForEntity(entityUUID, entityType).getCourseDetailsHints
    if (index < (courseDetailsHints.size - 1) && courseDetailsHints.size > 1) {
      val currentHint = courseDetailsHints.get(index)
      val nextHint = courseDetailsHints.get(index + 1)

      currentHint.setIndex(index + 1)
      nextHint.setIndex(index)

      CourseDetailsHintRepo(currentHint.getUUID).update(currentHint)
      CourseDetailsHintRepo(nextHint.getUUID).update(nextHint)
    }
  }
}
