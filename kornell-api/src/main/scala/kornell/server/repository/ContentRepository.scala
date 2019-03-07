package kornell.server.repository

import java.io.InputStream

import kornell.core.lom.Contents
import kornell.core.entity.{ContentSpec, Enrollment, Person}
import kornell.core.util.StringUtils._
import kornell.server.content.ContentManagers
import kornell.server.dev.util.{ContentsParser, WizardParser}
import kornell.server.jdbc.repository.{ContentRepositoriesRepo, CourseClassRepo, CourseClassesRepo, CourseRepo, CourseVersionRepo, CoursesRepo, PersonRepo}
import kornell.server.scorm12.ManifestParser
import kornell.server.service.ContentService

import scala.collection.mutable.ListBuffer
import scala.xml.XML

object ContentRepository {

  def findVisitedContent(enrollment: Enrollment, person: Person): Contents = {
    val contentSpec = {
      if (enrollment.getCourseVersionUUID != null)
        CoursesRepo.byCourseVersionUUID(enrollment.getCourseVersionUUID).get.getContentSpec
      else
        CoursesRepo.byCourseClassUUID(enrollment.getCourseClassUUID).get.getContentSpec
    }
    contentSpec match {
      case ContentSpec.KNL => findKNLVisitedContent(enrollment, person, false)
      case ContentSpec.WIZARD => findKNLVisitedContent(enrollment, person, true)
      case ContentSpec.SCORM12 => findSCORM12VisitedContent(enrollment, person)
    }
  }

  def findSCORM12Actoms(courseClassUUID: String): List[String] = {
    val structureIn = getClassManifestContent(courseClassUUID, "imsmanifest.xml")
    val contents = XML.load(structureIn)

    val nodes = contents \\ "resource"
    val result = ListBuffer[String]()
    nodes.map(x => result += x.attribute("href").get.toString)
    result.toList
  }

  def findKNLVisitedContent(enrollment: Enrollment, person: Person, isWizard: Boolean): Contents = {
    val visited = getVisited(enrollment)
    if (isWizard) {
      val courseClass = CourseClassRepo(enrollment.getCourseClassUUID).get
      WizardParser.findVisitedContent(courseClass, visited)
    } else {
      val prefix = getPrefix(enrollment, person)
      val structureSrc = {
        if (enrollment.getCourseClassUUID != null) {
          getClassManifestContent(enrollment.getCourseClassUUID, "structure.knl")
        } else {
          getVersionManifestContent(enrollment.getCourseVersionUUID, "structure.knl")
        }
      }
      ContentsParser.parse(prefix, structureSrc, visited)
    }
  }

  def findSCORM12VisitedContent(enrollment: Enrollment, person: Person): Contents = {
    val prefix = getPrefix(enrollment, person)
    val visited = getVisited(enrollment)
    val structureSrc = {
      if (enrollment.getCourseClassUUID != null) {
        getClassManifestContent(enrollment.getCourseClassUUID, "imsmanifest.xml")
      } else {
        getVersionManifestContent(enrollment.getCourseVersionUUID, "imsmanifest.xml")
      }
    }
    val contents = ManifestParser.parse(prefix, structureSrc, visited)
    contents
  }

  private def getPrefix(enrollment: Enrollment, person: Person): String = {
    val contentRepo = ContentRepositoriesRepo.firstRepositoryByInstitution(person.getInstitutionUUID).get
    val repo = ContentManagers.forRepository(contentRepo.getUUID)
    val version = {
      if (enrollment.getCourseVersionUUID != null)
        CourseVersionRepo(enrollment.getCourseVersionUUID)
      else
        CourseClassesRepo(enrollment.getCourseClassUUID).version
    }.get
    val course = CourseRepo(version.getCourseUUID).get
    val prefix = repo.url(ContentService.CLASSROOMS, course.getCode, version.getDistributionPrefix)
    prefix
  }

  private def getVisited(enrollment: Enrollment): List[String] = {
    val personRepo = PersonRepo(enrollment.getPersonUUID)
    val visited = personRepo.actomsVisitedBy(enrollment.getUUID)
    visited
  }

  private def getVersionManifestContent(courseVersionUUID: String, filename: String): InputStream = {
    val version = CourseVersionRepo(courseVersionUUID).get
    val course = CourseRepo(version.getCourseUUID).get
    val contentRepo = ContentRepositoriesRepo.firstRepositoryByInstitution(course.getInstitutionUUID).get
    val repo = ContentManagers.forRepository(contentRepo.getUUID)
    val url = mkurl(course.getCode, version.getDistributionPrefix, filename)
    val structureIn = repo.inputStream(ContentService.CLASSROOMS, url).get
    structureIn
  }

  private def getClassManifestContent(courseClassUUID: String, filename: String): InputStream = {
    val classRepo = CourseClassesRepo(courseClassUUID)
    val institution = classRepo.institution.get
    val contentRepo = ContentRepositoriesRepo.firstRepositoryByInstitution(institution.getUUID).get
    val versionRepo = classRepo.version
    val version = versionRepo.get
    val repo = ContentManagers.forRepository(contentRepo.getUUID)
    val course = CourseRepo(version.getCourseUUID).get
    val structureIn = repo.inputStream(ContentService.CLASSROOMS, mkurl(course.getCode, version.getDistributionPrefix, filename)).get
    structureIn
  }
}
