package kornell.server.service

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import kornell.core.entity._
import kornell.core.to.CourseClassTO
import kornell.core.util.StringUtils
import kornell.server.authentication.ThreadLocalAuthenticator
import kornell.server.jdbc.repository.{CourseClassesRepo, CourseDetailsHintsRepo, CourseDetailsSectionsRepo, CourseVersionsRepo, CoursesRepo}
import kornell.server.repository.{Entities, TOs}

object CourseCreationService {

  def generateCourseDetails(courseUUID: String, to: Course): Unit = {
    CourseDetailsHintsRepo.create(Entities.newCourseDetailsHint(UUID.randomUUID.toString, "duration",
      "Carga de estudo: XXX horas", CourseDetailsEntityType.COURSE, courseUUID, 0, "fa fa-clock-o"))
    CourseDetailsHintsRepo.create(Entities.newCourseDetailsHint(UUID.randomUUID.toString, "help",
      "Se precisar entrar em contato, clique em ajuda no menu acima.", CourseDetailsEntityType.COURSE, courseUUID, 1, "fa fa-question-circle"))
    CourseDetailsHintsRepo.create(Entities.newCourseDetailsHint(UUID.randomUUID.toString, "multimedia-warning",
      "Este conteúdo contém vídeo e áudio, certifique que seu dispositivo possa reproduzi-los.", CourseDetailsEntityType.COURSE, courseUUID, 2, "fa fa-warning"))
    CourseDetailsHintsRepo.create(Entities.newCourseDetailsHint(UUID.randomUUID.toString, "certificate",
      "Conteúdo com certificação.", CourseDetailsEntityType.COURSE, courseUUID, 3, "fa fa-certificate"))

    CourseDetailsSectionsRepo.create(Entities.newCourseDetailsSection(UUID.randomUUID.toString, "Apresentação", to.getDescription, CourseDetailsEntityType.COURSE, courseUUID, 0))
  }

  def createCourse(institutionUUID: String, to: Course): Course = {
    val courseUUID = UUID.randomUUID.toString
    if (StringUtils.isNone(to.getCode)) {
      val formattedDate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date)
      to.setCode(to.getName.toLowerCase.replaceAll(" ", "-") + "-" + formattedDate)
    }

    val course = CoursesRepo.create(Entities.newCourse(
      courseUUID,
      to.getCode,
      to.getName,
      to.getDescription,
      null,
      EntityState.active,
      institutionUUID,
      false,
      null,
      to.getContentSpec))
    generateCourseDetails(courseUUID, to)
    course
  }

  def createCourseVersion(institutionUUID: String, courseUUID: String, to: Course): CourseVersion = {
    val versionUUID = UUID.randomUUID.toString
    val formattedDate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date)
    val distributionPrefix = to.getName.toLowerCase.replaceAll(" ", "-") + "-" + formattedDate
    val version = CourseVersionsRepo.create(Entities.newCourseVersion(
      versionUUID,
      "v1",
      courseUUID,
      new Date(),
      distributionPrefix,
      EntityState.active,
      false,
      null,
      1,
      null), institutionUUID)
    version
  }

  def createCourseClass(institutionUUID: String, versionUUID: String, to: Course): CourseClass = {
    val courseClass = CourseClassesRepo.create(Entities.newCourseClass(
      UUID.randomUUID.toString,
      to.getName,
      versionUUID,
      institutionUUID,
      new BigDecimal(0.00),
      false,
      false,
      false,
      1000,
      new Date(),
      ThreadLocalAuthenticator.getAuthenticatedPersonUUID.get,
      EntityState.active,
      RegistrationType.email,
      null,
      false,
      false,
      false,
      false,
      false,
      null,
      null,
      null,
      true,
      0))
    courseClass
  }

  def simpleCreation(institutionUUID: String, to: Course): CourseClassTO = {
    val course = createCourse(institutionUUID, to)
    val courseVersion = createCourseVersion(institutionUUID, course.getUUID, to)
    val courseClass = createCourseClass(institutionUUID, courseVersion.getUUID, to)
    TOs.newCourseClassTO(course, courseVersion, courseClass, null)
  }
}
