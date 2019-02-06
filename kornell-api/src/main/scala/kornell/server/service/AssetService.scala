package kornell.server.service

import java.util.UUID

import kornell.core.entity.CourseDetailsEntityType
import kornell.core.util.StringUtils
import kornell.server.content.S3ContentManager
import kornell.server.jdbc.repository.{CertificatesDetailsRepo, ContentRepositoriesRepo, CourseDetailsHintsRepo, CourseDetailsLibrariesRepo, CourseDetailsSectionsRepo, InstitutionRepo}

import scala.collection.JavaConverters.asScalaBufferConverter

object AssetService {

  def copyAssets(institutionUUID: String, entityType: CourseDetailsEntityType, sourceEntityUUID: String, targetEntityUUID: String, thumbUrl: String): Unit = {
    val institution = InstitutionRepo(institutionUUID).get
    val bucketName = ContentRepositoriesRepo.firstRepository(institution.getAssetsRepositoryUUID).get.getBucketName
    val s3 = S3ContentManager.getAmazonS3Client(institution.getUUID)

    //copy thumbnail
    if (StringUtils.isSome(thumbUrl)) {
      val sourceThumbPath = ContentService.getCourseAssetUrl(institutionUUID, sourceEntityUUID, ContentService.THUMB_FILENAME, "")
      val targetThumbPath = ContentService.getCourseAssetUrl(institutionUUID, targetEntityUUID, ContentService.THUMB_FILENAME, "")
      try {
        s3.copyObject(bucketName, sourceThumbPath, bucketName, targetThumbPath)
      } catch { case _: Exception => }
    }

    //copy certificate bg
    val certificateDetailsOption = CertificatesDetailsRepo.getForEntity(sourceEntityUUID, entityType)
    if (certificateDetailsOption.isDefined) {
      val certificateDetails = certificateDetailsOption.get
      certificateDetails.setUUID(UUID.randomUUID.toString)
      certificateDetails.setBgImage(certificateDetails.getBgImage.replace(sourceEntityUUID, targetEntityUUID))
      certificateDetails.setEntityUUID(targetEntityUUID)
      CertificatesDetailsRepo.create(certificateDetails)

      val sourceCertificateBgPath = ContentService.getCourseAssetUrl(institutionUUID, sourceEntityUUID, ContentService.CERTIFICATE_FILENAME, "")
      val targetCertificateBgPath = ContentService.getCourseAssetUrl(institutionUUID, targetEntityUUID, ContentService.CERTIFICATE_FILENAME, "")
      try {
        s3.copyObject(bucketName, sourceCertificateBgPath, bucketName, targetCertificateBgPath)
      } catch { case _: Exception => }
    }

    //copy sections
    val sections = CourseDetailsSectionsRepo.getForEntity(sourceEntityUUID, entityType)
    sections.getCourseDetailsSections.asScala.foreach(courseDetailsSection => {
      courseDetailsSection.setEntityUUID(targetEntityUUID)
      courseDetailsSection.setUUID(UUID.randomUUID.toString)
      CourseDetailsSectionsRepo.create(courseDetailsSection)
    })

    //copy hints
    val hints = CourseDetailsHintsRepo.getForEntity(sourceEntityUUID, entityType)
    hints.getCourseDetailsHints.asScala.foreach(courseDetailsHint => {
      courseDetailsHint.setEntityUUID(targetEntityUUID)
      courseDetailsHint.setUUID(UUID.randomUUID.toString)
      CourseDetailsHintsRepo.create(courseDetailsHint)
    })

    //copy library files
    val libraries = CourseDetailsLibrariesRepo.getForEntity(sourceEntityUUID, entityType)
    libraries.getCourseDetailsLibraries.asScala.foreach(courseDetailsLibrary => {
      courseDetailsLibrary.setEntityUUID(targetEntityUUID)
      courseDetailsLibrary.setUUID(UUID.randomUUID.toString)
      CourseDetailsLibrariesRepo.create(courseDetailsLibrary)

      val sourceLibraryPath = ContentService.getCourseAssetUrl(institutionUUID, sourceEntityUUID, ContentService.THUMB_FILENAME, "library")
      val targetLibraryPath = ContentService.getCourseAssetUrl(institutionUUID, targetEntityUUID, ContentService.THUMB_FILENAME, "library")
      try {
        s3.copyObject(bucketName, sourceLibraryPath, bucketName, targetLibraryPath)
      } catch { case _: Exception => }
    })
  }
}