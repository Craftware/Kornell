package kornell.server.repository

import kornell.server.dev.util.ContentsParser
import kornell.server.jdbc.repository.CourseClassesRepo
import kornell.core.entity.Person
import scala.collection.mutable.ListBuffer
import kornell.core.util.StringUtils._
import kornell.server.jdbc.repository.PersonRepo
import kornell.core.entity.Enrollment
import kornell.server.jdbc.repository.CourseVersionRepo
import kornell.server.content.ContentManagers
import kornell.server.jdbc.repository.InstitutionsRepo
import kornell.server.jdbc.repository.InstitutionRepo
import kornell.server.jdbc.repository.CourseRepo
import kornell.server.service.S3Service
import scala.xml.XML
import kornell.server.scorm12.ManifestParser
import kornell.server.jdbc.repository.CoursesRepo
import kornell.core.entity.ContentSpec
import kornell.core.error.exception.ServerErrorException
import kornell.server.jdbc.repository.CourseVersionsRepo
import kornell.server.dev.util.WizardParser
import kornell.core.entity.CourseVersion

object ContentRepository {

  def findVisitedContent(enrollment: Enrollment, person: Person) = {
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

  def findSCORM12Actoms(courseClassUUID: String) = {
    val structureIn = getClassManifestContent(courseClassUUID, "imsmanifest.xml")
    val contents = XML.load(structureIn)

    val nodes = (contents \\ "resource")
    val result = ListBuffer[String]()
    nodes.map(x => result += x.attribute("href").get.toString)
    result.toList
  }

  def findKNLVisitedContent(enrollment: Enrollment, person: Person, isWizard: Boolean) = {
    val visited = getVisited(enrollment)
    if (isWizard) {
      val courseVersion = CourseVersionsRepo.byCourseClassUUID(enrollment.getCourseClassUUID).get
      WizardParser.parse(courseVersion.getClassroomJson, visited)
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

  def findSCORM12VisitedContent(enrollment: Enrollment, person: Person) = {
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
    val institutionRepo = InstitutionRepo(person.getInstitutionUUID)
    val repositoryUUID = institutionRepo.get.getAssetsRepositoryUUID
    val repo = ContentManagers.forRepository(repositoryUUID)
    val version = {
      if (enrollment.getCourseVersionUUID != null)
        CourseVersionRepo(enrollment.getCourseVersionUUID)
      else
        CourseClassesRepo(enrollment.getCourseClassUUID).version
    }.get
    val course = CourseRepo(version.getCourseUUID).get
    val prefix = repo.url(S3Service.CLASSROOMS, course.getCode, version.getDistributionPrefix)
    prefix
  }

  private def getVisited(enrollment: Enrollment): List[String] = {
    val personRepo = PersonRepo(enrollment.getPersonUUID)
    val visited = personRepo.actomsVisitedBy(enrollment.getUUID)
    visited
  }

  private def getVersionManifestContent(courseVersionUUID: String, filename: String) = {
    val version = CourseVersionRepo(courseVersionUUID).get
    val course = CourseRepo(version.getCourseUUID).get
    val institution = InstitutionRepo(course.getInstitutionUUID).get
    val repositoryUUID = institution.getAssetsRepositoryUUID
    val repo = ContentManagers.forRepository(repositoryUUID)
    val url = mkurl(course.getCode, version.getDistributionPrefix, filename)
    val structureIn = repo.inputStream(S3Service.CLASSROOMS, url).get
    structureIn
  }

  private def getClassManifestContent(courseClassUUID: String, filename: String) = {
    val classRepo = CourseClassesRepo(courseClassUUID)
    val institution = classRepo.institution.get
    val repositoryUUID = institution.getAssetsRepositoryUUID
    val versionRepo = classRepo.version
    val version = versionRepo.get
    val repo = ContentManagers.forRepository(repositoryUUID)
    val course = CourseRepo(version.getCourseUUID).get
    val structureIn = repo.inputStream(S3Service.CLASSROOMS, mkurl(course.getCode, version.getDistributionPrefix, filename)).get
    structureIn
  }
}