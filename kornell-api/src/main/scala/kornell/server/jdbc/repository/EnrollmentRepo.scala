package kornell.server.jdbc.repository

import java.math.BigDecimal
import java.math.BigDecimal._
import java.util.Date

import kornell.core.entity.ContentSpec._
import kornell.core.entity.{Assessment, ChatThreadType, Enrollment}
import kornell.core.lom.ContentsOps
import kornell.server.jdbc.SQL._
import kornell.server.repository.ContentRepository
import kornell.server.util.EmailService
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.matching.Regex

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
      assessment = ${Option(e.getAssessment).map(_.toString).orNull},
      lastAssessmentUpdate = ${e.getLastAssessmentUpdate},
      assessmentScore = ${e.getAssessmentScore},
      certifiedAt = ${e.getCertifiedAt},
      parentEnrollmentUUID = ${e.getParentEnrollmentUUID},
      startDate = ${e.getStartDate},
      endDate = ${e.getEndDate}
    where uuid = ${e.getUUID} """.executeUpdate

    EnrollmentsRepo.updateCache(sql" SELECT * FROM Enrollment e WHERE uuid = ${uuid}".first[Enrollment].get)
    ChatThreadsRepo.addParticipantsToCourseClassThread(CourseClassesRepo(e.getCourseClassUUID).get)
    e
  }

  // to prevent that deadlock
  def updateProgressAndAssessment(): Unit = for {
    e <- first
    cc <- CourseClassesRepo(e.getCourseClassUUID).first
    cv <- CourseVersionRepo(cc.getCourseVersionUUID).first
    c <- CourseRepo(cv.getCourseUUID).first
  } c.getContentSpec match {
    case KNL => updateKNLProgress(e, false, true)
    case WIZARD => updateKNLProgress(e, true, true)
    case SCORM12 => updateSCORM12Progress(e, true)
  }

  def setEnrollmentProgressAndAssessment(e: Enrollment, newProgress: Int): Unit = {
    val currentProgress = e.getProgress
    val isProgress = currentProgress == null || newProgress > currentProgress
    val isProgressValid = newProgress >= 0 && newProgress <= 100
    val notPassed = !Assessment.PASSED.equals(e.getAssessment)
    if (isProgress) {
      if (isProgressValid) {
        e.setProgress(newProgress)
      } else {
        logger.warning(s"Invalid progress [${currentProgress} to ${newProgress}] on enrollment [${e.getUUID}]")
      }
    }

    if (notPassed && e.getCourseClassUUID != null) {
      val (maxScore, assessment) = assess(e)
      e.setAssessmentScore(maxScore)
      e.setAssessment(assessment)
    }

    val isPassed = Assessment.PASSED == e.getAssessment
    val isCompleted = e.getProgress == 100
    val isUncertified = e.getCertifiedAt == null
    if (isPassed && isCompleted && isUncertified) {
      e.setCertifiedAt(DateTime.now.toDate)
    }

    if ((isProgress && isProgressValid)
      || (notPassed && e.getCourseClassUUID != null)
      || (isPassed && isCompleted && isUncertified)) {
      update(e)
    }

    if (isPassed && isCompleted && isUncertified) {
      EmailService.sendEmailClassCompletion(e)
    }
  }

  //TODO: Convert to map/flatmat and dedup updateAssessment
  def updateProgress(): Unit = for {
    e <- first
    cc <- CourseClassesRepo(e.getCourseClassUUID).first
    cv <- CourseVersionRepo(cc.getCourseVersionUUID).first
    c <- CourseRepo(cv.getCourseUUID).first
  } c.getContentSpec match {
    case KNL => updateKNLProgress(e, false)
    case WIZARD => updateKNLProgress(e, true)
    case SCORM12 => updateSCORM12Progress(e)
  }

  def updateKNLProgress(e: Enrollment, isWizard: Boolean, setAssessment: Boolean = false): Unit = {
    val contents = ContentRepository.findKNLVisitedContent(e, PersonRepo(e.getPersonUUID).get, isWizard)
    val actoms = ContentsOps.collectActoms(contents).asScala
    val visited = actoms.count(_.isVisited)
    val newProgress = visited / actoms.size.toDouble
    val newProgressPerc = math.max((newProgress * 100).floor.toInt, 1)
    if (setAssessment) {
      setEnrollmentProgressAndAssessment(e, newProgressPerc)
    } else {
      setEnrollmentProgress(e, newProgressPerc)
    }
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
      """.map[Int] { rs => rs.getInt("progress") }

    progress.headOption
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

  val progress_r: Regex = """.*::progress,(\d+).*""".r
  def parseProgress(sdata: String): Option[Int] =
    sdata match {
      case progress_r(matched) => Try { matched.toInt }.toOption
      case _ => None
    }

  def progressFromSuspendData(e: Enrollment): Option[Int] = {
    val suspend_datas = ActomEntriesRepo.getValues(e.getUUID, "%", "cmi.suspend_data")
    val progresses = suspend_datas.flatMap { parseProgress }
    val progress = if (progresses.isEmpty)
      None
    else
      Some(progresses.max)
    progress
  }

  def progressFromLessonStatus(e: Enrollment): Option[Int] = {
    val lesson_statuses = ActomEntriesRepo.getValues(e.getUUID, "%", "cmi.core.lesson_status")
    val progresses = lesson_statuses.flatMap {
      _ match {
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

  def updateSCORM12Progress(e: Enrollment, setAssessment: Boolean = false): Unit =
    progressFromSuspendData(e)
      .orElse(progressFromMilestones(e))
      .orElse(progressFromLessonStatus(e))
      .orElse(Some(1))
      .foreach { p => if (setAssessment) setEnrollmentProgressAndAssessment(e, p) else setEnrollmentProgress(e, p) }

  def setEnrollmentProgress(e: Enrollment, newProgress: Int): Unit = {
    val currentProgress = e.getProgress
    val isProgress = currentProgress == null || newProgress > currentProgress
    if (isProgress) {
      val isValid = newProgress >= 0 && newProgress <= 100
      if (isValid) {
        e.setProgress(newProgress)
      } else {
        logger.warning(s"Invalid progress [${currentProgress} to ${newProgress}] on enrollment [${e.getUUID}]")
      }
      val isPassed = Assessment.PASSED == e.getAssessment
      val isCompleted = e.getProgress == 100
      val isUncertified = e.getCertifiedAt == null
      if (isPassed && isCompleted && isUncertified) {
        e.setCertifiedAt(DateTime.now.toDate)
      }

      if (isValid || (isPassed && isCompleted && isUncertified)) {
        update(e)
      }

      if (isPassed && isCompleted && isUncertified) {
        EmailService.sendEmailClassCompletion(EnrollmentRepo(e.getUUID).get)
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

  def updateAssessment(): Unit = first map { e =>
    val notPassed = !Assessment.PASSED.equals(e.getAssessment)
    if (notPassed && e.getCourseClassUUID != null) {
      val (maxScore, assessment) = assess(e)
      e.setAssessmentScore(maxScore)
      e.setAssessment(assessment)

      val isPassed = Assessment.PASSED == e.getAssessment
      val isCompleted = e.getProgress == 100
      val isUncertified = e.getCertifiedAt == null
      if (isPassed && isCompleted && isUncertified) {
        e.setCertifiedAt(DateTime.now.toDate)
      }
      update(e)
      if (isPassed && isCompleted && isUncertified) {
        EmailService.sendEmailClassCompletion(EnrollmentRepo(e.getUUID).get)
      }
    }
  }

  def assess(e: Enrollment): (BigDecimal, Assessment) = {
    val cc = CourseClassRepo(e.getCourseClassUUID).get
    val reqScore: BigDecimal = Option(cc.getRequiredScore).getOrElse(ZERO)
    val maxScore = findMaxScore(e.getUUID).getOrElse(ZERO)
    val assessment = if (maxScore.compareTo(reqScore) >= 0)
      Assessment.PASSED
    else
      Assessment.FAILED
    (maxScore, assessment)
  }

  def checkExistingEnrollment(courseClassUUID: String): Boolean = {
    sql"""select count(*) as enrollmentExists from Enrollment where  personUUID = ${first.get.getPersonUUID} and courseClassUUID = ${courseClassUUID}"""
      .first[Integer] { rs => rs.getInt("enrollmentExists") }.get >= 1
  }

  def transfer(fromCourseClassUUID: String, toCourseClassUUID: String): Unit = {
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

  def updatePreAssessmentScore(score: BigDecimal): Unit = sql"""
      update Enrollment
      set preAssessmentScore = ${score}
        where uuid = ${uuid}
  """.executeUpdate

  def updatePostAssessmentScore(score: BigDecimal): Unit = sql"""
      update Enrollment
      set postAssessmentScore = ${score}
        where uuid = ${uuid}
  """.executeUpdate

  def addDaysToExpiryDate(numberOfDays: String): Unit = {
    val enrollment = first.get
    val newEndDate = new DateTime(enrollment.getEndDate).plusDays(Integer.parseInt(numberOfDays)).toDate

    sql"""
      update Enrollment
      set endDate = ${newEndDate}
        where uuid = ${uuid}
    """.executeUpdate

    EnrollmentsRepo.updateCache(sql" SELECT * FROM Enrollment e WHERE uuid = ${uuid}".first[Enrollment].get)
  }

}

object EnrollmentRepo {
  def apply(uuid: String) = new EnrollmentRepo(uuid)
}
