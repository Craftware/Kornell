package kornell.server.jdbc.repository

import java.sql.ResultSet
import java.util
import java.util.{Date, UUID}

import kornell.core.entity._
import kornell.core.entity.role.{Role, RoleCategory, RoleType}
import kornell.core.error.exception.{EntityConflictException, EntityNotFoundException}
import kornell.core.to.{CourseClassTO, CourseClassesTO}
import kornell.core.util.StringUtils
import kornell.server.jdbc.PreparedStmt
import kornell.server.jdbc.SQL.SQLHelper
import kornell.server.repository.TOs

import scala.collection.JavaConverters._
import scala.collection.mutable
import kornell.server.dev.util.WizardParser

object CourseClassesRepo {

  def apply(uuid: String) = CourseClassRepo(uuid)

  def create(courseClass: CourseClass): CourseClass = {
    val courseClassExists = sql"""
      select count(*) from CourseClass where courseVersionUUID = ${courseClass.getCourseVersionUUID} and name = ${courseClass.getName}
      """.first[String].get
    if (courseClassExists == "0") {
      if (courseClass.getUUID == null) {
        courseClass.setUUID(UUID.randomUUID.toString)
      }
      if (courseClass.isSandbox == null) {
        courseClass.setSandbox(false)
      }
      val course = CoursesRepo.byCourseVersionUUID(courseClass.getCourseVersionUUID).get
      val isWizard = ContentSpec.WIZARD == course.getContentSpec
      if(isWizard){
        val courseVersion = CourseVersionRepo(courseClass.getCourseVersionUUID).get
        courseClass.setRequiredScore(WizardParser.getRequiredScore(courseVersion, courseClass.isSandbox))
      }
      val ecommerceIdentifier = UUID.randomUUID.toString.replace("-", "").substring(0, 20)
      courseClass.setEcommerceIdentifier(ecommerceIdentifier)
      sql"""
        insert into CourseClass(uuid,
                name,
                courseVersionUUID,
                institutionUUID,
                publicClass,
                requiredScore,
                overrideEnrollments,
                invisible,
                maxEnrollments,
                createdAt,
                createdBy,
                registrationType,
                institutionRegistrationPrefixUUID,
                courseClassChatEnabled,
                chatDockEnabled,
                allowBatchCancellation,
                tutorChatEnabled,
                approveEnrollmentsAutomatically,
                startDate,
                ecommerceIdentifier,
                thumbUrl,
                sandbox,
                enrollmentExpiryDays)
        values(${courseClass.getUUID},
               ${courseClass.getName},
               ${courseClass.getCourseVersionUUID},
               ${courseClass.getInstitutionUUID},
               ${courseClass.isPublicClass},
               ${courseClass.getRequiredScore},
               ${courseClass.isOverrideEnrollments},
               ${courseClass.isInvisible},
               ${courseClass.getMaxEnrollments},
               ${new Date()},
               ${courseClass.getCreatedBy},
               ${courseClass.getRegistrationType.toString},
               ${courseClass.getInstitutionRegistrationPrefixUUID},
               ${courseClass.isCourseClassChatEnabled},
               ${courseClass.isChatDockEnabled},
               ${courseClass.isAllowBatchCancellation},
               ${courseClass.isTutorChatEnabled},
               ${courseClass.isApproveEnrollmentsAutomatically},
               ${courseClass.getStartDate},
               ${courseClass.getEcommerceIdentifier},
               ${courseClass.getThumbUrl},
               ${courseClass.isSandbox},
               ${courseClass.getEnrollmentExpiryDays}
               )
      """.executeUpdate
      ChatThreadsRepo.addParticipantsToCourseClassThread(courseClass)

      //log creation event
      EventsRepo.logEntityChange(courseClass.getInstitutionUUID, AuditedEntityType.courseClass, courseClass.getUUID, null, courseClass)
      courseClass
    } else {
      throw new EntityConflictException("courseClassAlreadyExists")
    }
  }

  private def getAllClassesByInstitution(institutionUUID: String): CourseClassesTO =
    getAllClassesByInstitutionPaged(institutionUUID, "", Int.MaxValue, 1, "cc.name", true, "", null, null, showSandbox = false)

  def getCourseClassTO(institutionUUID: String, courseClassUUID: String): Option[CourseClassTO] = {
    val courseClassesTO = getAllClassesByInstitutionPaged(institutionUUID, "", Int.MaxValue, 1, "cc.name", true, "", null, courseClassUUID)
    if (courseClassesTO.getCourseClasses.size > 0) {
      Some(courseClassesTO.getCourseClasses.get(0))
    } else {
      None
    }
  }

  def getAllClassesByInstitutionPaged(institutionUUID: String, searchTerm: String, pageSize: Int, pageNumber: Int, orderBy: String, asc: Boolean, adminUUID: String, courseVersionUUID: String, courseClassUUID: String, showSandbox: Boolean = false): CourseClassesTO = {
    val resultOffset = (pageNumber.max(1) - 1) * pageSize
    val filteredSearchTerm = '%' + Option(searchTerm).getOrElse("") + '%'
    val orderColumn = if (orderBy != null && !orderBy.contains(";")) orderBy else "cc.name"
    val orderMod = if (asc) " asc" else " desc"
    val order = orderColumn + orderMod + (if (orderColumn.contains("cc.state")) ", cc.publicClass desc, cc.invisible desc " else "")

    val courseClassesTO = TOs.newCourseClassesTO(
      new PreparedStmt(s"""
      select
        c.uuid as courseUUID,
          c.code,
          c.name,
          c.description,
          c.contentSpec as contentSpec,
          c.infoJson,
          c.state,
          c.childCourse,
          c.thumbUrl as courseThumbUrl,
          cv.uuid as courseVersionUUID,
          cv.name as courseVersionName,
          cv.versionCreatedAt as versionCreatedAt,
          cv.distributionPrefix as distributionPrefix,
          cv.state as courseVersionState,
          cv.disabled as disabled,
          cv.parentVersionUUID as parentVersionUUID,
          cv.instanceCount as instanceCount,
          cv.classroomJson as classroomJson,
          cv.classroomJsonPublished as classroomJsonPublished,
          cv.label as label,
          cv.thumbUrl as courseVersionThumbUrl,
          cc.uuid as courseClassUUID,
          cc.name as courseClassName,
          cc.institutionUUID as institutionUUID,
          cc.requiredScore as requiredScore,
          cc.publicClass as publicClass,
          cc.overrideEnrollments as overrideEnrollments,
          cc.invisible as invisible,
          cc.maxEnrollments as maxEnrollments,
          cc.createdAt as createdAt,
          cc.createdBy as createdBy,
          cc.state as courseClassState,
          cc.registrationType as registrationType,
          cc.institutionRegistrationPrefixUUID as institutionRegistrationPrefixUUID,
          cc.courseClassChatEnabled as courseClassChatEnabled,
          cc.chatDockEnabled as chatDockEnabled,
          cc.allowBatchCancellation as allowBatchCancellation,
          cc.tutorChatEnabled as tutorChatEnabled,
          cc.approveEnrollmentsAutomatically as approveEnrollmentsAutomatically,
          cc.ecommerceIdentifier as ecommerceIdentifier,
          cc.thumbUrl as courseClassThumbUrl,
          cc.sandbox as sandbox,
          cc.enrollmentExpiryDays as enrollmentExpiryDays,
          irp.name as institutionRegistrationPrefixName
      from Course c
        join CourseVersion cv on cv.courseUUID = c.uuid
        join CourseClass cc on cc.courseVersionUUID = cv.uuid and cc.institutionUUID = '${institutionUUID}'
          left join InstitutionRegistrationPrefix irp on irp.uuid = cc.institutionRegistrationPrefixUUID
            where cc.state <> '${EntityState.deleted.toString}' and cc.sandbox in (false, ${showSandbox}) and
                (cc.courseVersionUUID = '${courseVersionUUID}' or ${StringUtils.isNone(courseVersionUUID)}) and
                (cc.uuid = '${courseClassUUID}' or ${StringUtils.isNone(courseClassUUID)}) and
          cc.institutionUUID = '${institutionUUID}' and
              (cv.name like '${filteredSearchTerm}' or cc.name like '${filteredSearchTerm}') and
              (${StringUtils.isNone(adminUUID)} or
        (select count(*) from Role r where personUUID = '${adminUUID}' and (
          (r.role = '${RoleType.platformAdmin.toString}' and r.institutionUUID = '${institutionUUID}') or
          (r.role = '${RoleType.institutionAdmin.toString}' and r.institutionUUID = '${institutionUUID}') or
        ( (r.role = '${RoleType.courseClassAdmin.toString}' or r.role = '${RoleType.courseClassObserver.toString}' or r.role = '${RoleType.tutor.toString}') and r.courseClassUUID = cc.uuid)
      )) > 0)
            order by ${order}, cc.state, c.name, cv.versionCreatedAt desc, cc.name limit ${resultOffset}, ${pageSize};
    """, List[String]()).map[CourseClassTO](toCourseClassTO))
    courseClassesTO.setCount(
      sql"""select count(cc.uuid) from CourseClass cc where cc.state <> ${EntityState.deleted.toString} and cc.sandbox in (false, ${showSandbox}) and (${StringUtils.isSome(adminUUID)} and
          (select count(*) from Role r where personUUID = ${adminUUID} and (
            (r.role = ${RoleType.platformAdmin.toString} and r.institutionUUID = ${institutionUUID}) or
            (r.role = ${RoleType.institutionAdmin.toString} and r.institutionUUID = ${institutionUUID}) or
            ( (r.role = ${RoleType.courseClassAdmin.toString} or r.role = ${RoleType.courseClassObserver.toString} or r.role = ${RoleType.tutor.toString}) and r.courseClassUUID = cc.uuid)
          )) > 0)
                and (cc.courseVersionUUID = ${courseVersionUUID} or ${StringUtils.isNone(courseVersionUUID)})
                and (cc.uuid = ${courseClassUUID} or ${StringUtils.isNone(courseClassUUID)})
          and cc.institutionUUID = ${institutionUUID}""".first[String].get.toInt)
    courseClassesTO.setPageSize(pageSize)
    courseClassesTO.setPageNumber(pageNumber.max(1))
    courseClassesTO.setSearchCount({
      if (searchTerm == "")
        0
      else
        sql"""select count(cc.uuid) from CourseClass cc
          join CourseVersion cv on cc.courseVersionUUID = cv.uuid
          where cc.state <> ${EntityState.deleted.toString} and
              (cv.name like ${filteredSearchTerm}
              or cc.name like ${filteredSearchTerm}) and (${StringUtils.isSome(adminUUID)} and
        (select count(*) from Role r where personUUID = ${adminUUID} and (
          (r.role = ${RoleType.platformAdmin.toString} and r.institutionUUID = ${institutionUUID}) or
          (r.role = ${RoleType.institutionAdmin.toString} and r.institutionUUID = ${institutionUUID}) or
          ( (r.role = ${RoleType.courseClassAdmin.toString} or r.role = ${RoleType.courseClassObserver.toString} or r.role = ${RoleType.tutor.toString}) and r.courseClassUUID = cc.uuid)
        )) > 0)
                and (cc.courseVersionUUID = ${courseVersionUUID} or ${StringUtils.isNone(courseVersionUUID)})
                and (cc.uuid = ${courseClassUUID} or ${StringUtils.isNone(courseClassUUID)})
              and cc.institutionUUID = ${institutionUUID}""".first[String].get.toInt
    })

    if (courseClassUUID != null && courseClassesTO.getCourseClasses.size == 1) {
      bindClassroomDetails(courseClassesTO.getCourseClasses.get(0))
    } else if (StringUtils.isSome(adminUUID)) {
      bindEnrollmentCounts(courseClassesTO)
    }

    courseClassesTO
  }

  private def bindEnrollmentCounts(courseClassesTO: CourseClassesTO): CourseClassesTO = {
    val classes = courseClassesTO.getCourseClasses.asScala
    classes.foreach(cc => cc.setEnrollmentCount(EnrollmentsRepo.countByCourseClass(cc.getCourseClass.getUUID)))
    courseClassesTO.setCourseClasses(classes.asJava)
    courseClassesTO
  }

  def bindClassroomDetails(courseClassTO: CourseClassTO): Unit = {
    val courseUUID = courseClassTO.getCourseVersionTO.getCourseTO.getCourse.getUUID
    val courseVersionUUID = courseClassTO.getCourseVersionTO.getCourseVersion.getUUID
    val courseClassUUID = courseClassTO.getCourseClass.getUUID

    courseClassTO.setCourseDetailsSections(mergeSections(courseUUID, courseVersionUUID, courseClassUUID))
    courseClassTO.setCourseDetailsHints(mergeHints(courseUUID, courseVersionUUID, courseClassUUID))
    courseClassTO.setCourseDetailsLibraries(mergeLibraries(courseUUID, courseVersionUUID, courseClassUUID))
  }

  def mergeSections(courseUUID: String, courseVersionUUID: String, courseClassUUID: String): util.List[CourseDetailsSection] = {
    val sectionsCourse = CourseDetailsSectionsRepo.getForEntity(courseUUID, CourseDetailsEntityType.COURSE).getCourseDetailsSections.asScala.asInstanceOf[mutable.Buffer[AssetEntity]]
    val sectionsCourseVersion = CourseDetailsSectionsRepo.getForEntity(courseVersionUUID, CourseDetailsEntityType.COURSE_VERSION).getCourseDetailsSections.asScala.asInstanceOf[mutable.Buffer[AssetEntity]]
    val sectionsCourseClass = CourseDetailsSectionsRepo.getForEntity(courseClassUUID, CourseDetailsEntityType.COURSE_CLASS).getCourseDetailsSections.asScala.asInstanceOf[mutable.Buffer[AssetEntity]]
    mergeAssets(sectionsCourse, sectionsCourseVersion, sectionsCourseClass).asInstanceOf[mutable.Buffer[CourseDetailsSection]].asJava
  }

  def mergeHints(courseUUID: String, courseVersionUUID: String, courseClassUUID: String): util.List[CourseDetailsHint] = {
    val hintsCourse = CourseDetailsHintsRepo.getForEntity(courseUUID, CourseDetailsEntityType.COURSE).getCourseDetailsHints.asScala.asInstanceOf[mutable.Buffer[AssetEntity]]
    val hintsCourseVersion = CourseDetailsHintsRepo.getForEntity(courseVersionUUID, CourseDetailsEntityType.COURSE_VERSION).getCourseDetailsHints.asScala.asInstanceOf[mutable.Buffer[AssetEntity]]
    val hintsCourseClass = CourseDetailsHintsRepo.getForEntity(courseClassUUID, CourseDetailsEntityType.COURSE_CLASS).getCourseDetailsHints.asScala.asInstanceOf[mutable.Buffer[AssetEntity]]
    mergeAssets(hintsCourse, hintsCourseVersion, hintsCourseClass).asInstanceOf[mutable.Buffer[CourseDetailsHint]].asJava
  }

  def mergeLibraries(courseUUID: String, courseVersionUUID: String, courseClassUUID: String): util.List[CourseDetailsLibrary] = {
    val libraryFilesCourse = CourseDetailsLibrariesRepo.getForEntity(courseUUID, CourseDetailsEntityType.COURSE).getCourseDetailsLibraries.asScala.asInstanceOf[mutable.Buffer[AssetEntity]]
    val libraryFilesCourseVersion = CourseDetailsLibrariesRepo.getForEntity(courseVersionUUID, CourseDetailsEntityType.COURSE_VERSION).getCourseDetailsLibraries.asScala.asInstanceOf[mutable.Buffer[AssetEntity]]
    val libraryFilesCourseClass = CourseDetailsLibrariesRepo.getForEntity(courseClassUUID, CourseDetailsEntityType.COURSE_CLASS).getCourseDetailsLibraries.asScala.asInstanceOf[mutable.Buffer[AssetEntity]]
    mergeAssets(libraryFilesCourse, libraryFilesCourseVersion, libraryFilesCourseClass).asInstanceOf[mutable.Buffer[CourseDetailsLibrary]].asJava
  }

  def mergeAssets(assetsCourse: mutable.Buffer[AssetEntity], assetsCourseVersion: mutable.Buffer[AssetEntity], assetsCourseClass: mutable.Buffer[AssetEntity]): mutable.Buffer[AssetEntity] = {

    var assetsCourseVersionNew = assetsCourseVersion
    var assetsCourseClassNew = assetsCourseClass
    val assetsCourseWithVersion = assetsCourse.map { sc =>
      {
        val assetCV = assetsCourseVersion.filter { _.getTitle.equals(sc.getTitle) }
        if (assetCV.nonEmpty) {
          assetsCourseVersionNew = assetsCourseVersionNew.filterNot { _.getTitle.equals(sc.getTitle) }
          assetCV.head
        } else sc
      }
    }

    val assetsCourseWithClass = assetsCourseWithVersion.map { sc =>
      {
        val assetCC = assetsCourseClass.filter { _.getTitle.equals(sc.getTitle) }
        if (assetCC.nonEmpty) {
          assetsCourseClassNew = assetsCourseClassNew.filterNot { _.getTitle.equals(sc.getTitle) }
          assetCC.head
        } else sc
      }
    }

    assetsCourseWithClass ++ assetsCourseVersionNew ++ assetsCourseClassNew
  }

  def byPersonAndInstitution(personUUID: String, institutionUUID: String): CourseClassesTO = {
    bindEnrollments(personUUID, getAllClassesByInstitution(institutionUUID))
  }

  def byEcommerceIdentifier(ecommerceIdentifier: String): Option[CourseClass] = {
    sql"""select * from CourseClass where ecommerceIdentifier = ${ecommerceIdentifier}""".first[CourseClass](toCourseClass)
  }

  def byEnrollment(enrollmentUUID: String): Option[CourseClass] = {
    sql"""
      | select cc.* from
      | CourseClass cc
      | join Enrollment e on e.courseClassUUID = cc.uuid
      | where e.uuid = ${enrollmentUUID}
      | and cc.state <> ${EntityState.deleted.toString}
      """.first[CourseClass](toCourseClass)
  }

  def byEnrollment(enrollmentUUID: String, personUUID: String, institutionUUID: String, showSandbox: Boolean = false): CourseClassTO = {
    val courseClass = byEnrollment(enrollmentUUID)

    if (courseClass.isDefined) {
      val courseClassesTO = getAllClassesByInstitutionPaged(institutionUUID, "", Int.MaxValue, 1, "cc.name", true, "", null, courseClass.get.getUUID, showSandbox)
      bindEnrollments(personUUID, courseClassesTO).getCourseClasses.get(0)
    } else {
      //@TODO what about disabled versions?
      val courseVersion = sql"""
        | select cv.* from
        | CourseVersion cv
        | join Enrollment e on e.courseVersionUUID = cv.uuid
        | where e.uuid = ${enrollmentUUID}
          """.first[CourseVersion](toCourseVersion)

      if (courseVersion.isDefined) {
        val course = sql"""
            | select c.* from
            | Course c
            | join CourseVersion cv on cv.courseUUID = c.uuid
            | where cv.uuid = ${courseVersion.get.getUUID}
          """.first[Course](toCourse)

        val parentCourseClass = sql"""
            select cc.* from Enrollment e
            left join Enrollment pe on e.parentEnrollmentUUID = pe.uuid
            left join CourseClass cc on cc.uuid = pe.courseClassUUID
            where e.uuid = ${enrollmentUUID}
          """.first[CourseClass]

        val courseClassesTO = TOs.newCourseClassesTO
        val list = new util.ArrayList[CourseClassTO]
        val courseClassTO = TOs.newCourseClassTO(course.get, courseVersion.get, parentCourseClass.get, null)
        courseClassTO.setEnrolledOnCourseVersion(true)
        bindEnrollment(personUUID, courseClassTO)
        list.add(courseClassTO)
        courseClassesTO.setCourseClasses(list)
        courseClassesTO.setCount(1)
        courseClassesTO.setPageSize(1)
        courseClassesTO.setPageNumber(1)
        courseClassesTO.setSearchCount(1)
        courseClassTO
      } else {
        throw new EntityNotFoundException("")
      }
    }
  }

  def byCourseVersion(courseVersionUUID: String): List[CourseClass] = {
    sql"""
      select * from CourseClass where courseVersionUUID = ${courseVersionUUID} and state <> ${EntityState.deleted.toString}
    """.map[CourseClass](toCourseClass)
  }

  def countByCourseVersion(courseVersionUUID: String): Int =
    sql"""select count(*)
      from CourseClass cc
      where cc.courseVersionUUID = ${courseVersionUUID}
      and cc.state <> ${EntityState.deleted.toString}
      and sandbox = 0
    """.first[String].get.toInt

  def sandboxForVersion(courseVersionUUID: String): Option[CourseClass] = {
    sql"""
      select * from CourseClass where sandbox = 1 and courseVersionUUID = ${courseVersionUUID}
    """.first[CourseClass]
  }

  def sandboxClassesForInstitution(institutionUUID: String): List[CourseClass] = {
    sql"""
      select * from CourseClass where sandbox = 1 and institutionUUID = ${institutionUUID} and state <> ${EntityState.deleted.toString}
    """.map[CourseClass](toCourseClass)
  }

  def updateWizardClassesRequiredScore(courseVersion: CourseVersion, updateOnlySandbox: Boolean) = {
    val wizardRequiredScoreSandbox = WizardParser.getRequiredScore(courseVersion, true)
    if (updateOnlySandbox) {
      // save sandbox class with requiredScore
      val sandboxCourseClass = CourseClassesRepo.sandboxForVersion(courseVersion.getUUID)
      if (sandboxCourseClass.isDefined) {
        CourseClassRepo(sandboxCourseClass.get.getUUID).updateRequiredScore(sandboxCourseClass.get, wizardRequiredScoreSandbox)
      }
    } else {
      // save all classes with requiredScore
      val wizardRequiredScore = WizardParser.getRequiredScore(courseVersion, false)
      CourseClassesRepo.byCourseVersion(courseVersion.getUUID).foreach(courseClass => {
        CourseClassRepo(courseClass.getUUID).updateRequiredScore(courseClass,
          if(courseClass.isSandbox) wizardRequiredScoreSandbox else wizardRequiredScore
        )
      })
    }
  }

  private def bindEnrollments(personUUID: String, courseClassesTO: CourseClassesTO): CourseClassesTO = {
    val classes = courseClassesTO.getCourseClasses.asScala
    //bind enrollment if it exists
    val enrollments = EnrollmentsRepo.byPerson(personUUID)
    enrollments.foreach { e =>
      {
        val clazz = classes.filter(_.getCourseClass.getUUID.equals(e.getCourseClassUUID)).asJava
        if (clazz.size() > 0) clazz.get(0).setEnrollment(e)
      }
    }
    //only return the valid classes for the user (for example, hide private classes)
    courseClassesTO.setCourseClasses(classes.filter(isValidClass).asJava)
    courseClassesTO
  }

  private def isValidClass(cc: CourseClassTO): Boolean = {
    cc.getCourseClass.isPublicClass || cc.getEnrollment != null
  }

  private def isPlatformAdmin(institutionUUID: String, roles: List[Role]): Boolean = {
    var hasRole: Boolean = false
    roles.foreach(role => hasRole = hasRole
      || RoleCategory.isValidRole(role, RoleType.platformAdmin, institutionUUID, null))
    hasRole
  }

  private def isInstitutionAdmin(institutionUUID: String, roles: List[Role]): Boolean = {
    var hasRole: Boolean = isPlatformAdmin(institutionUUID, roles)
    roles.foreach(role => hasRole = hasRole
      || RoleCategory.isValidRole(role, RoleType.institutionAdmin, institutionUUID, null))
    hasRole
  }

  private def isCourseClassAdmin(courseClassUUID: String, institutionUUID: String, roles: List[Role]): Boolean = {
    var hasRole: Boolean = isInstitutionAdmin(institutionUUID, roles)
    roles.foreach(role => hasRole = hasRole
      || RoleCategory.isValidRole(role, RoleType.courseClassAdmin, null, courseClassUUID))
    hasRole
  }

  private def isCourseClassObserver(courseClassUUID: String, institutionUUID: String, roles: List[Role]): Boolean = {
    var hasRole: Boolean = isInstitutionAdmin(institutionUUID, roles)
    roles.foreach(role => hasRole = hasRole
      || RoleCategory.isValidRole(role, RoleType.courseClassObserver, null, courseClassUUID))
    hasRole
  }

  private def isCourseClassTutor(courseClassUUID: String, institutionUUID: String, roles: List[Role]): Boolean = {
    var hasRole: Boolean = isInstitutionAdmin(institutionUUID, roles)
    roles.foreach(role => hasRole = hasRole
      || RoleCategory.isValidRole(role, RoleType.tutor, null, courseClassUUID))
    hasRole
  }

  private def bindEnrollment(personUUID: String, courseClassTO: CourseClassTO): Unit = {
    val enrollment =
      if (courseClassTO.getCourseClass != null)
        EnrollmentsRepo.byCourseClassAndPerson(courseClassTO.getCourseClass.getUUID, personUUID, false)
      else
        EnrollmentsRepo.byCourseVersionAndPerson(courseClassTO.getCourseVersionTO.getCourseVersion.getUUID, personUUID)

    enrollment foreach courseClassTO.setEnrollment
  }

  implicit def toString(rs: ResultSet): String = rs.getString(1)
}
