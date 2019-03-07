package kornell.server.repository

import kornell.core.to.{LibraryFileTO, LibraryFilesTO}
import kornell.core.util.StringUtils
import kornell.server.content.ContentManagers
import kornell.server.dev.util.LibraryFilesParser
import kornell.server.jdbc.repository.{ContentRepositoriesRepo, CourseClassesRepo, CourseRepo}
import kornell.server.service.ContentService

object LibraryFilesRepository {
  //TODO: Review
  def findLibraryFiles(courseClassUUID: String): LibraryFilesTO = {
    val classRepo = CourseClassesRepo(courseClassUUID)
    val institutionRepo = classRepo.institution
    val contentRepo = ContentRepositoriesRepo.firstRepositoryByInstitution(institutionRepo.get.getUUID).get
    val repo = ContentManagers.forRepository(contentRepo.getUUID)
    val versionRepo = classRepo.version
    val version = versionRepo.get
    val course = CourseRepo(version.getCourseUUID).get
    val filesURL = StringUtils.mkurl(ContentService.CLASSROOMS, course.getCode, version.getDistributionPrefix, "classroom/library")
    try {
      val librarySrc = repo.source(filesURL, "libraryFiles.knl")
      val libraryFilesText = librarySrc.get.mkString("")
      val contents = LibraryFilesParser.parse(repo.url(filesURL), libraryFilesText)
      contents
    } catch {
      case _: Exception => TOs.newLibraryFilesTO(List[LibraryFileTO]())
    }
  }
}
