package kornell.server.content

import java.io.InputStream
import java.util.logging.Logger

import com.amazonaws.HttpMethod
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{DeleteObjectsRequest, GeneratePresignedUrlRequest, ObjectMetadata}
import kornell.core.entity.ContentRepository
import kornell.core.util.StringUtils._
import kornell.server.jdbc.repository.{ContentRepositoriesRepo, InstitutionRepo}
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.io.{BufferedSource, Source}
import scala.util.Try

class S3ContentManager(repo: ContentRepository)
    extends SyncContentManager {

  val logger: Logger = Logger.getLogger(classOf[S3ContentManager].getName)

  lazy val s3: AmazonS3Client = if (isSome(repo.getAccessKeyId))
    new AmazonS3Client(new BasicAWSCredentials(repo.getAccessKeyId, repo.getSecretAccessKey))
  else
    new AmazonS3Client

  def source(keys: String*): Try[BufferedSource] =
    inputStream(keys: _*).map { Source.fromInputStream(_, "UTF-8") }

  def inputStream(keys: String*): Try[InputStream] = Try {
    val fqkn = url(keys: _*)
    logger.finest(s"loading key [ ${fqkn} ]")
    try {
      s3.getObject(repo.getBucketName, fqkn).getObjectContent
    } catch {
      case e: Throwable => {
        val cmd = s"aws s3api get-object --bucket ${repo.getBucketName} --key ${fqkn} --region ${repo.getRegion} file.out"
        logger.warning("Could not load object. Try [ " + cmd + " ]")
        throw e
      }
    }
  }

  def put(value: InputStream, contentLength: Int, contentType: String, contentDisposition: String, metadataMap: Map[String, String], keys: String*): Unit = {
    val metadata = new ObjectMetadata()
    metadata.setUserMetadata(metadataMap asJava)
    Option(contentType).foreach { metadata.setContentType }
    Option(contentDisposition).foreach { metadata.setContentDisposition }
    s3.putObject(repo.getBucketName, url(keys: _*), value, metadata)
  }

  def delete(keys: String*): Unit = {
    // keys we support delete for already have repo prefix appended
    logger.info("Trying to delete object [ " + mkurl("", keys: _*) + " ]")
    s3.deleteObject(repo.getBucketName, mkurl("", keys: _*))
  }

  def deleteFolder(keys: String*): Unit = {
    val path = url(keys: _*)
    logger.info("Trying to delete folder object [ " + path + " ]")
    val objects = s3.listObjects(repo.getBucketName, path)
    val keyPaths = objects.getObjectSummaries.asScala.map(f => f.getKey)
    val deleteRequest = new DeleteObjectsRequest(repo.getBucketName).withKeys(keyPaths: _*)
    try {
      s3.deleteObjects(deleteRequest)
    } catch {
      case _: Throwable => {
        logger.info("Could not delete folder object. [ " + path + " ]")
      }
    }
  }

  def getUploadUrl(path: String, contentType: String): String = {
    logger.fine(path)
    val presignedRequest = new GeneratePresignedUrlRequest(repo.getBucketName, path)
    presignedRequest.setMethod(HttpMethod.PUT)
    presignedRequest.setExpiration(new DateTime().plusMinutes(1).toDate)
    presignedRequest.setContentType(contentType)
    s3.generatePresignedUrl(presignedRequest).toString
  }

  def getPrefix: String = repo.getPrefix

}

object S3ContentManager {
  def getAmazonS3Client(institutionUUID: String): AmazonS3Client = {
    val institution = InstitutionRepo(institutionUUID).get
    val repo = ContentRepositoriesRepo.firstRepositoryByInstitution(institution.getUUID).get
    val s3 = if (isSome(repo.getAccessKeyId))
      new AmazonS3Client(new BasicAWSCredentials(repo.getAccessKeyId, repo.getSecretAccessKey))
    else
      new AmazonS3Client

    s3
  }
}
