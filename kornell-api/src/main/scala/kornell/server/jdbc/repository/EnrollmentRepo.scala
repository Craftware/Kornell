package kornell.server.jdbc.repository

import kornell.core.entity.Enrollment
import java.sql.ResultSet
import kornell.server.repository.Entities
import kornell.server.jdbc.SQL._
import kornell.server.repository.Entities._
import kornell.core.entity.Enrollment
import kornell.core.entity.EnrollmentState
import kornell.core.entity.ContentSpec._
import kornell.core.entity.Person
import kornell.server.repository.ContentRepository
import kornell.core.lom.ContentsOps
import scala.collection.JavaConverters._
import kornell.core.entity.ActomEntries
import kornell.core.entity.Assessment
import java.util.Date
import kornell.server.ep.EnrollmentSEP
import java.math.BigDecimal
import java.math.BigDecimal._
import kornell.core.entity.ChatThreadType
import scala.util.Try
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalDateTime
import kornell.server.util.EmailService

//TODO: Specific column names and proper sql
class EnrollmentRepo(uuid: String) {
  
  lazy val finder = sql" SELECT * FROM Enrollment e WHERE uuid = ${uuid} "

  def get: Enrollment = first.get

  def first: Option[Enrollment] = EnrollmentsRepo.getByUUID(uuid)

  def update(e: Enrollment): Enrollment = {
    e.setLastProgressUpdate(DateTime.now.toDate)
    sql"""
    update Enrollment    
    set 
  		progress = ${e.getProgress},
  		notes = ${e.getNotes},
  		state = ${e.getState.toString},
  		lastProgressUpdate = ${e.getLastProgressUpdate},
  		assessment = ${Option(e.getAssessment).map(_.toString).getOrElse(null)},
  		lastAssessmentUpdate = ${e.getLastAssessmentUpdate},
  		assessmentScore = ${e.getAssessmentScore},
  		certifiedAt = ${e.getCertifiedAt},
      parentEnrollmentUUID = ${e.getParentEnrollmentUUID},
      start_date = ${e.getStartDate},
      end_date = ${e.getEndDate}
    where uuid = ${e.getUUID} """.executeUpdate
    
    EnrollmentsRepo.updateCache(sql" SELECT * FROM Enrollment e WHERE uuid = ${uuid}".first[Enrollment].get)
	ChatThreadsRepo.addParticipantsToCourseClassThread(CourseClassesRepo(e.getCourseClassUUID).get)
    e
  }

  //TODO: Convert to map/flatmat and dedup updateAssessment
  def updateProgress = for {
    e <- first
    cc <- CourseClassesRepo(e.getCourseClassUUID).first
    cv <- CourseVersionRepo(cc.getCourseVersionUUID).first
    c <- CourseRepo(cv.getCourseUUID).first
  } c.getContentSpec match {
    case KNL => updateKNLProgress(e, false)
    case WIZARD => updateKNLProgress(e, true)
    case SCORM12 => updateSCORM12Progress(e)
  }

  def updateKNLProgress(e: Enrollment, isWizard: Boolean) = {
    val contents = ContentRepository.findKNLVisitedContent(e, PersonRepo(e.getPersonUUID).get, isWizard)
    val actoms = ContentsOps.collectActoms(contents).asScala
    val visited = actoms.filter(_.isVisited).size
    val newProgress = visited / actoms.size.toDouble
    val newProgressPerc = math.max((newProgress * 100).floor.toInt, 1)
    setEnrollmentProgress(e, newProgressPerc)
  }

  def findProgressMilestone(e: Enrollment, actomKey: String): Option[Int] = {
      val actomLike = "%" + actomKey
      val enrollmentUUID = e.getUUID
      val progress = sql"""
    	  select progress from ActomEntries AE
    		join ProgressMilestone PM on AE.actomKey = PM.actomKey and AE.entryValue = PM.entryValue
          where AE.enrollmentUUID = ${enrollmentUUID}
    		and AE.actomKey LIKE ${actomLike}
    		and AE.entryKey = 'cmi.core.lesson_location'
      """.map[Integer] { rs => rs.getInt("progress") }

      if (progress.isEmpty) None else Some(progress.head)
    }

  def progressFromMilestones(e: Enrollment): Option[Int] = {
    val actomKeys = ContentRepository.findSCORM12Actoms(e.getCourseClassUUID)
    val progresses = actomKeys
      .flatMap { actomKey => findProgressMilestone(e, actomKey) }
    if (progresses.isEmpty)
      None
    else
      Some(progresses.foldLeft(1)(_ max _))
  }

  val progress_r = """.*::progress,(\d+).*""".r
  def parseProgress(sdata: String) = 
    sdata match {
      case progress_r(matched) => Try {matched.toInt}.toOption
      case _ => None
  }


  def progressFromSuspendData(e: Enrollment): Option[Int] = {
    val suspend_datas = ActomEntriesRepo.getValues(e.getUUID, "%", "cmi.suspend_data")
    val progresses = suspend_datas.flatMap { parseProgress(_) }
    val progress = if (progresses.isEmpty)
      None
    else
      Some(progresses.max)
    progress
  }

  def progressFromLessonStatus(e: Enrollment): Option[Int] = {
    val lesson_statuses = ActomEntriesRepo.getValues(e.getUUID, "%", "cmi.core.lesson_status")
    val progresses = lesson_statuses.flatMap { _ match {
        case "passed" => Option(100)
        case _ => None
      } 
    }
    val progress = if (progresses.isEmpty)
      None
    else
      Some(progresses.max)
    progress
  }

  def updateSCORM12Progress(e: Enrollment) = 
    progressFromSuspendData(e)
      .orElse(progressFromMilestones(e))
      .orElse(progressFromLessonStatus(e))
      .orElse(Some(1))
      .foreach { p => setEnrollmentProgress(e, p) }
  

  def setEnrollmentProgress(e: Enrollment, newProgress: Int) = {
    val currentProgress = e.getProgress
    val isProgress = newProgress > currentProgress    
    if (isProgress) {
      val isValid = newProgress >= 0 && newProgress <= 100
      if (isValid){
        e.setProgress(newProgress)
        update(e)
        checkCompletion(e);
      }else {
        logger.warning(s"Invalid progress [${currentProgress} to ${newProgress}] on enrollment [${e.getUUID}]")
      }
    } 
  }

  //TODO: WRONG ASSUMPTION: Courses can have multiple assessments, should be across all grades
  def findMaxScore(enrollmentUUID: String): Option[BigDecimal] = sql"""
  		SELECT  MAX(CAST(entryValue AS DECIMAL(8,5))) as maxScore
  		FROM ActomEntries
  		WHERE enrollmentUUID = ${enrollmentUUID}
  		AND entryKey = 'cmi.core.score.raw'
  """.first[BigDecimal] { rs => rs.getBigDecimal("maxScore") }

  
  def updateAssessment = first map { e =>
    val notPassed = !Assessment.PASSED.equals(e.getAssessment)
    if (notPassed && e.getCourseClassUUID != null) {
      val (maxScore, assessment) = assess(e)
      e.setAssessmentScore(maxScore)
      e.setAssessment(assessment)
      update(e)
      checkCompletion(e)
    }
  }

  def assess(e: Enrollment) = {
    val cc = CourseClassRepo(e.getCourseClassUUID).get
    val reqScore: BigDecimal = Option(cc.getRequiredScore).getOrElse(ZERO)
    val maxScore = findMaxScore(e.getUUID).getOrElse(ZERO)
    val assessment = if (maxScore.compareTo(reqScore) >= 0)
      Assessment.PASSED
    else
      Assessment.FAILED
    (maxScore, assessment)
  }

  def checkCompletion(e: Enrollment) = {
    val isPassed = Assessment.PASSED == e.getAssessment
    val isCompleted = e.getProgress() == 100
    val isUncertified = e.getCertifiedAt() == null
    if (isPassed && isCompleted && isUncertified) {
      e.setCertifiedAt(DateTime.now.toDate)
      update(e)
      EmailService.sendEmailClassCompletion(EnrollmentRepo(e.getUUID).get)
    }
  }  
  
  def checkExistingEnrollment(courseClassUUID: String):Boolean = {
    sql"""select count(*) as enrollmentExists from Enrollment where  personUUID = ${first.get.getPersonUUID} and courseClassUUID = ${courseClassUUID}"""
    	.first[Integer] { rs => rs.getInt("enrollmentExists") }.get >= 1
  }
  
  def transfer(fromCourseClassUUID: String, toCourseClassUUID: String) = {
    val enrollment = first.get
    //disable participation to global class thread for old class
    ChatThreadsRepo.disableParticipantFromCourseClassThread(enrollment)

    //update enrollment
    sql"""update Enrollment set courseClassUUID = ${toCourseClassUUID} where uuid = ${uuid}""".executeUpdate

    //disable old support and tutoring threads
    sql"""update ChatThread set active = 0 where courseClassUUID = ${fromCourseClassUUID} and personUUID = ${enrollment.getPersonUUID} and threadType in  (${ChatThreadType.SUPPORT.toString}, ${ChatThreadType.TUTORING.toString})""".executeUpdate

    EnrollmentsRepo.invalidateCache(uuid)
    
    //add participation to global class thread for new class
    ChatThreadsRepo.addParticipantToCourseClassThread(enrollment)
  }
  
  def updatePreAssessmentScore(score:BigDecimal) = sql"""
		  update Enrollment 
		  set preAssessmentScore = ${score}
  		  where uuid = ${uuid}
  """.executeUpdate
  
  def updatePostAssessmentScore(score:BigDecimal) = sql"""
		  update Enrollment 
		  set postAssessmentScore = ${score}
  		  where uuid = ${uuid}
  """.executeUpdate

}

object EnrollmentRepo {
  def apply(uuid: String) = new EnrollmentRepo(uuid)
}