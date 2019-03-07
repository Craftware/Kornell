package kornell.server.service

import java.util.Date

import kornell.core.util.StringUtils._
import kornell.server.content.ContentManagers
import kornell.server.jdbc.repository.{ContentRepositoriesRepo, CourseClassRepo, CourseRepo, CourseVersionRepo, CoursesRepo, InstitutionRepo}

object ContentService {

  def PREFIX = "knl"
  def CLASSROOMS = "classrooms"
  def INSTITUTION = "institution"
  def COURSES = "courses"
  def COURSE_VERSIONS = "courseVersions"
  def COURSE_CLASSES = "courseClasses"
  def REPORTS = "reports"
  def CERTIFICATES = "certificates"
  def CLASS_INFO = "classInfo"
  def CERTIFICATE_FILENAME = "certificate-bg.jpg"
  def THUMB_FILENAME = "thumb.jpg"

  def getCourseVersionContentUploadUrl(courseVersionUUID: String): String = {
    val courseVersion = CourseVersionRepo(courseVersionUUID).get
    val course = CourseRepo(courseVersion.getCourseUUID).get
    val fullPath = mkurl(getRepositoryUrl(course.getInstitutionUUID), CLASSROOMS, course.getCode, courseVersion.getDistributionPrefix, "upload" + new Date().getTime + ".zip")
    val institutionUUID = CourseRepo(courseVersion.getCourseUUID).get.getInstitutionUUID
    val contentRepo = ContentManagers.forInstitution(institutionUUID)
    contentRepo.getUploadUrl(fullPath, "application/zip")
  }

  def getCourseWizardContentUploadUrl(courseUUID: String, fileName: String): String = {
    val course = CourseRepo(courseUUID).get
    getCourseUploadUrl(course.getUUID, fileName, "__wizard", isAngularComponent = true)
  }

  def getCourseAssetUrl(institutionUUID: String, courseUUID: String, fileName: String, path: String): String = {
    mkurl(getRepositoryUrl(institutionUUID), mkurl(PREFIX, COURSES, courseUUID, path, fileName))
  }

  def getCourseUploadUrl(courseUUID: String, fileName: String, path: String, isAngularComponent: Boolean = false): String = {
    val institutionUUID = CourseRepo(courseUUID).get.getInstitutionUUID
    val contentRepo = ContentManagers.forInstitution(institutionUUID)
    contentRepo.getUploadUrl(getCourseAssetUrl(institutionUUID, courseUUID, fileName, path), getContentType(fileName, isAngularComponent))
  }

  def getCourseVersionAssetUrl(institutionUUID: String, courseVersionUUID: String, fileName: String, path: String): String = {
    mkurl(getRepositoryUrl(institutionUUID), mkurl(PREFIX, COURSE_VERSIONS, courseVersionUUID, path, fileName))
  }

  def getCourseVersionUploadUrl(courseVersionUUID: String, fileName: String, path: String): String = {
    val institutionUUID = CoursesRepo.byCourseVersionUUID(courseVersionUUID).get.getInstitutionUUID
    val contentRepo = ContentManagers.forInstitution(institutionUUID)
    contentRepo.getUploadUrl(getCourseVersionAssetUrl(institutionUUID, courseVersionUUID, fileName, path), getContentType(fileName))
  }

  def getCourseClassAssetUrl(institutionUUID: String, courseClassUUID: String, fileName: String, path: String): String = {
    mkurl(getRepositoryUrl(institutionUUID), mkurl(PREFIX, COURSE_CLASSES, courseClassUUID, path, fileName))
  }

  def getCourseClassUploadUrl(courseClassUUID: String, fileName: String, path: String): String = {
    val institutionUUID = CourseClassRepo(courseClassUUID).get.getInstitutionUUID
    val contentRepo = ContentManagers.forInstitution(institutionUUID)
    contentRepo.getUploadUrl(getCourseClassAssetUrl(institutionUUID, courseClassUUID, fileName, path), getContentType(fileName))
  }

  def getInstitutionUploadUrl(institutionUUID: String, fileName: String): String = {
    val path = mkurl(getRepositoryUrl(institutionUUID), PREFIX, INSTITUTION, fileName)
    val contentRepo = ContentManagers.forInstitution(institutionUUID)
    contentRepo.getUploadUrl(path, getContentType(fileName))
  }

  def getRepositoryUrl(institutionUUID: String): String = {
    val institution = InstitutionRepo(institutionUUID).get
    val repo = ContentRepositoriesRepo.firstRepositoryByInstitution(institution.getUUID).get
    mkurl("repository", repo.getUUID)
  }

  def getContentType(fileName: String, isAngularComponent: Boolean = false): String = {
    if (isAngularComponent) {
      getFileExtension(fileName) match {
        case "png" => "image/png"
        case "jpg" => "image/jpeg"
        case "jpeg" => "image/jpeg"
        case "mp4" => "video/mp4"
        case _ => "application/octet-stream"
      }
    } else {
      getFileExtension(fileName) match {
        case "png" => "image/png"
        case "jpg" => "image/jpg"
        case "jpeg" => "image/jpg"
        case "ico" => "image/x-icon"
        case "mp4" => "video/mp4"
        case _ => "application/octet-stream"
      }
    }
  }

  def getFileExtension(fileName: String): String = {
    val fileNameSplit = fileName.split('.')
    if (fileNameSplit.length > 1)
      fileNameSplit(1)
    else
      fileNameSplit(0)
  }
}
