package kornell.server.util

import java.io.File
import java.net.URL
import org.apache.commons.io.FileUtils
import kornell.core.entity.Institution
import kornell.core.entity.Person
import kornell.core.entity.Course
import kornell.core.entity.PersonCategory
import kornell.core.entity.CourseClass
import  kornell.core.util.StringUtils._
import kornell.server.jdbc.repository.PersonRepo
import kornell.core.entity.Enrollment
import kornell.server.jdbc.repository.InstitutionEmailWhitelistRepo
import kornell.core.entity.ChatThread
import kornell.server.util.Settings._
import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Logger
import java.util.logging.Level
import kornell.core.util.StringUtils
import kornell.server.service.S3Service
import kornell.server.jdbc.repository.InstitutionsRepo
import kornell.server.jdbc.repository.CourseClassRepo
import kornell.server.jdbc.repository.CourseVersionRepo
import kornell.server.jdbc.repository.CourseRepo
import java.text.SimpleDateFormat
import kornell.server.jdbc.repository.RolesRepo
import collection.JavaConverters._
import kornell.core.entity.EmailTemplateType
import kornell.server.jdbc.repository.EmailTemplatesRepo


object EmailService {

  val logger = Logger.getLogger("kornell.server.email")

  def sendEmailBatchEnrollment(person: Person, institution: Institution, courseClass: CourseClass) = {
    if (checkWhitelistForDomain(institution, person.getEmail)) {
      val values = scala.collection.mutable.Map[String, String]()
      values("PERSON_FULLNAME") =  person.getFullName
      values("CLASS_NAME") = courseClass.getName
      values("BUTTON_LINK") = institution.getBaseURL + "#a.courseClass:" + courseClass.getUUID
      values("INSTITUTION_NAME") = institution.getFullName

      val from = getFromEmail(institution)
      val to = person.getEmail
      val template = processTemplate(EmailTemplateType.BATCH_ENROLLMENT_CONFIRMATION, values)
      val imgFile = getInstitutionLogoImage(institution)
      EmailSender.sendEmail(template._1, from, to, from, template._2, imgFile)
    }
  }

  def sendEmailConfirmation(person: Person, institution: Institution) = {
    if (checkWhitelistForDomain(institution, person.getEmail)) {
      val values = scala.collection.mutable.Map[String, String]()
      values("PERSON_FULLNAME") =  person.getFullName
      values("GENDER_MODIFIER") = PersonCategory.getSexSuffix(person)
      values("BUTTON_LINK") = institution.getBaseURL + "#vitrine:"
      values("INSTITUTION_NAME") = institution.getFullName

      val from = getFromEmail(institution)
      val to = person.getEmail
      val template = processTemplate(EmailTemplateType.SIGNUP_CONFIRMATION, values)
      val imgFile = getInstitutionLogoImage(institution)
      EmailSender.sendEmail(template._1, from, to, from, template._2, imgFile)
    }
  }

  def sendEmailRequestPasswordChange(person: Person, institution: Institution, requestPasswordChangeUUID: String) = {
    if (checkWhitelistForDomain(institution, person.getEmail)) {
      val values = scala.collection.mutable.Map[String, String]()
      values("PERSON_FULLNAME") =  person.getFullName
      values("BUTTON_LINK") = institution.getBaseURL + "#vitrine:" + requestPasswordChangeUUID
      values("INSTITUTION_NAME") = institution.getFullName

      val from = getFromEmail(institution)
      val to = person.getEmail
      val template = processTemplate(EmailTemplateType.PASSWORD_RESET, values)
      val imgFile = getInstitutionLogoImage(institution)
      EmailSender.sendEmail(template._1, from, to, from, template._2, imgFile)
    }
  }

  def sendEmailEnrolled(person: Person, institution: Institution, course: Course, enrollment: Enrollment, courseClass: CourseClass) = {
    if (checkWhitelistForDomain(institution, person.getEmail) && person.isReceiveEmailCommunication) {
      val hasPassword = PersonRepo(person.getUUID).hasPassword(institution.getUUID)
      val actionLink = if(hasPassword) {
        institution.getBaseURL + "#classroom:" + enrollment.getUUID
      } else {
        institution.getBaseURL + "#vitrine:" + person.getEmail
      }

      val values = scala.collection.mutable.Map[String, String]()
      values("PERSON_FULLNAME") =  person.getFullName
      values("GENDER_MODIFIER") = PersonCategory.getSexSuffix(person)
      values("BUTTON_LINK") = actionLink
      values("INSTITUTION_NAME") = institution.getFullName
      values("COURSE_NAME") = course.getName
      values("CLASS_NAME") = courseClass.getName

      val from = getFromEmail(institution)
      val to = person.getEmail
      val template = processTemplate(EmailTemplateType.ENROLLMENT_CONFIRMATION, values)
      val imgFile = getInstitutionLogoImage(institution)
      EmailSender.sendEmail(template._1, from, to, from, template._2, imgFile)
    }
  }

  def sendEmailNewChatThread(person: Person, institution: Institution, courseClass: CourseClass, chatThread: ChatThread, message: String) = {
    val participant = PersonRepo(chatThread.getPersonUUID).get
    if (checkWhitelistForDomain(institution, person.getEmail) && person.isReceiveEmailCommunication && !participant.getUUID.equals(person.getUUID)) {
      val templateType = {
        if("SUPPORT".equalsIgnoreCase(chatThread.getThreadType))
          EmailTemplateType.NEW_SUPPORT_CHAT_THREAD
        else if("INSTITUTION_SUPPORT".equalsIgnoreCase(chatThread.getThreadType))
          EmailTemplateType.NEW_INSTITUTION_SUPPORT_CHAT_THREAD
        else if("PLATFORM_SUPPORT".equalsIgnoreCase(chatThread.getThreadType))
          EmailTemplateType.NEW_PLATFORM_SUPPORT_CHAT_THREAD
        else if("TUTORING".equalsIgnoreCase(chatThread.getThreadType))
          EmailTemplateType.NEW_TUTORING_CHAT_THREAD
        else
          throw new Exception()
      }

      val values = scala.collection.mutable.Map[String, String]()
      values("PERSON_FULLNAME") =  person.getFullName
      values("BUTTON_LINK") = institution.getBaseURL + "#message:"
      values("INSTITUTION_SHORTNAME") = institution.getName
      values("CLASS_NAME") = courseClass.getName
      values("PARTICIPANT_FULLNAME") = participant.getFullName
      values("PARTICIPANT_EMAIL") = participant.getEmail
      values("THREAD_MESSAGE") = message.replace("\n", "<br />\n")
      values("THREAD_SUBJECT") = processTitle(templateType, values)

      val from = getFromEmail(institution)
      val to = person.getEmail
      val template = processTemplate(templateType, values)
      val imgFile = getInstitutionLogoImage(institution)
      EmailSender.sendEmail(template._1, from, to, from, template._2, imgFile)
    }
  }

  def sendEmailEspinafreReminder(person: Person, institution: Institution) = {
    if (person.isReceiveEmailCommunication) {
      val values = scala.collection.mutable.Map[String, String]()
      values("BUTTON_LINK") = institution.getBaseURL
      values("INSTITUTION_NAME") = institution.getFullName

      val from = getFromEmail(institution)
      val to = person.getEmail
      val template = processTemplate(EmailTemplateType.ESPINAFRE_REMINDER, values)
      val imgFile = getInstitutionLogoImage(institution)
      EmailSender.sendEmail(template._1, from, to, from, template._2, imgFile)
    }
  }

  def sendEmailClassCompletion(enrollment: Enrollment) = {
    val person = PersonRepo(enrollment.getPersonUUID).get
    val institution = InstitutionsRepo.getByUUID(person.getInstitutionUUID).get
    if (enrollment.getCourseVersionUUID == null && institution.isNotifyInstitutionAdmins()) {
      val df = new SimpleDateFormat("yyyy-MM-dd hh:mm")
      val enrolledClass = CourseClassRepo(enrollment.getCourseClassUUID).get
      val course = CourseRepo(CourseVersionRepo(enrolledClass.getCourseVersionUUID).get.getCourseUUID).get

      val values = scala.collection.mutable.Map[String, String]()
      values("INSTITUTION_SHORTNAME") = institution.getName
      values("PERSON_FULLNAME") =  person.getFullName
      values("PERSON_EMAIL") = person.getEmail
      values("COURSE_NAME") = course.getName
      values("CLASS_NAME") = enrolledClass.getName
      values("ENROLLMENT_DATE") = df.format(enrollment.getEnrolledOn)
      values("COMPLETION_DATE") = df.format(enrollment.getLastProgressUpdate)

      val from = getFromEmail(institution)
      val to = person.getEmail
      val imgFile = getInstitutionLogoImage(institution)
      val template = processTemplate(EmailTemplateType.CLASS_COMPLETION, values)
      for (admin <- RolesRepo.getInstitutionAdmins(institution.getUUID, "PERSON").getRoleTOs.asScala) {
        EmailSender.sendEmail(template._1, from, admin.getPerson.getEmail, from, template._2, imgFile)
      }
    }
  }

  private def getFromEmail(institution: kornell.core.entity.Institution):String = {
    if (StringUtils.isSome(institution.getInstitutionSupportEmail))
      institution.getInstitutionSupportEmail
    else
      SMTP_FROM.get
  }

  private def processTemplate(templateType: EmailTemplateType, values: scala.collection.mutable.Map[String, String]) = {
    val template = EmailTemplatesRepo.getTemplate(templateType, UserLocale.getLocale.get).get
    var output = template.getTemplate
    var title = template.getTitle
    values foreach (x => {
      output = output.replaceAll("$$" + x._1 + "$$", x._2)
      title = title.replaceAll("$$" + x._1 + "$$", x._2)
    })
    (title, output)
  }

  private def processTitle(templateType: EmailTemplateType, values: scala.collection.mutable.Map[String, String]) = {
    val template = EmailTemplatesRepo.getTemplate(templateType, UserLocale.getLocale.get).get
    var title = template.getTitle
    values foreach (x => {
      title = title.replaceAll("$$" + x._1 + "$$", x._2)
    })
    title
  }

  private def getInstitutionLogoImage(institution: Institution): java.io.File = {
    val logoImageName: String = "logo300x80.png"
    val tempDir: Path = Paths.get(System.getProperty("java.io.tmpdir"))
    val imgPath = tempDir.resolve(institution.getUUID + "-" + logoImageName)
    val imgFile: File = imgPath.toFile()

    val purgeTime = System.currentTimeMillis - (1 * 24 * 60 * 60 * 1000) //one day
    if(imgFile.lastModified < purgeTime && !imgFile.delete)
      logger.warning("Unable to delete file: " + imgFile)

    if (!imgFile.exists) {
      //TODO: Use ContentStore API
      val url = new URL(mkurl(institution.getBaseURL, "repository", institution.getAssetsRepositoryUUID, S3Service.PREFIX, S3Service.INSTITUTION, logoImageName))
      try {
        FileUtils.copyURLToFile(url, imgFile)
      } catch {
        case e: Exception => logger.log(Level.SEVERE, "Cannot copy institution logo ", e)
      }
    }
    imgFile
  }

  def checkWhitelistForDomain(institution: Institution, email: String) = {
    //If we don't use the whitelist, just continue with the sending
    if (!institution.isUseEmailWhitelist) {
      true
    } else {
      //If we use the whitelist, we have to check that the domain works.
      InstitutionEmailWhitelistRepo(institution.getUUID).get.getDomains.contains(email.split("@")(1))
    }
  }

}