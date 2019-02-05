package kornell.server.jdbc.repository

import java.util.{Date, UUID}

import kornell.core.entity._
import kornell.core.entity.role.RoleCategory
import kornell.core.error.exception.EntityConflictException
import kornell.core.util.StringUtils
import kornell.server.authentication.ThreadLocalAuthenticator
import kornell.server.jdbc.SQL.{SQLHelper, rsToString}
import kornell.server.service.AssetService
import java.math.BigDecimal

import scala.collection.JavaConverters.asScalaBufferConverter

class CourseClassRepo(uuid: String) {

  val finder = sql"""select * from CourseClass where uuid = $uuid and state <> ${EntityState.deleted.toString}"""

  def get: CourseClass = finder.get[CourseClass]
  def first: Option[CourseClass] = finder.first[CourseClass]

  def version = CourseVersionRepo(get.getCourseVersionUUID)

  def institution = InstitutionRepo(get.getInstitutionUUID)

  def update(courseClass: CourseClass): CourseClass = {
    //get previous version
    val oldCourseClass = CourseClassRepo(courseClass.getUUID).first.get

    val courseClassExists = sql"""
      select count(*) from CourseClass
      where courseVersionUUID = ${courseClass.getCourseVersionUUID}
      and name = ${courseClass.getName}
      and uuid <> ${courseClass.getUUID}
      and state <> ${EntityState.deleted.toString}
    """.first[String].get
    if (courseClassExists == "0") {
      sql"""
        update CourseClass cc set
          cc.name = ${courseClass.getName},
          cc.institutionUUID = ${courseClass.getInstitutionUUID},
          cc.requiredScore = ${courseClass.getRequiredScore},
          cc.publicClass = ${courseClass.isPublicClass},
          cc.overrideEnrollments = ${courseClass.isOverrideEnrollments},
          cc.invisible = ${courseClass.isInvisible},
          cc.maxEnrollments = ${courseClass.getMaxEnrollments},
          cc.registrationType = ${courseClass.getRegistrationType.toString},
          cc.institutionRegistrationPrefixUUID = ${courseClass.getInstitutionRegistrationPrefixUUID},
          cc.courseClassChatEnabled = ${courseClass.isCourseClassChatEnabled},
          cc.chatDockEnabled = ${courseClass.isChatDockEnabled},
          cc.allowBatchCancellation = ${courseClass.isAllowBatchCancellation},
          cc.tutorChatEnabled = ${courseClass.isTutorChatEnabled},
          cc.approveEnrollmentsAutomatically = ${courseClass.isApproveEnrollmentsAutomatically},
          cc.thumbUrl = ${courseClass.getThumbUrl},
          cc.enrollmentExpiryDays = ${courseClass.getEnrollmentExpiryDays}
        where cc.uuid = ${courseClass.getUUID}""".executeUpdate

      //update course class threads active states per threadType and add participants to the global class chat, if applicable
      ChatThreadsRepo.updateCourseClassChatThreadStatusByThreadType(courseClass.getUUID, ChatThreadType.COURSE_CLASS, courseClass.isCourseClassChatEnabled)
      ChatThreadsRepo.updateCourseClassChatThreadStatusByThreadType(courseClass.getUUID, ChatThreadType.TUTORING, courseClass.isTutorChatEnabled)
      ChatThreadsRepo.addParticipantsToCourseClassThread(courseClass)

      //log entity change
      EventsRepo.logEntityChange(courseClass.getInstitutionUUID, AuditedEntityType.courseClass, courseClass.getUUID, oldCourseClass, courseClass)

      courseClass
    } else {
      throw new EntityConflictException("courseClassAlreadyExists")
    }
  }

  def delete: CourseClass = {
    val courseClass = get
    if (EnrollmentsRepo.countByCourseClass(uuid) == 0) {
      sql"""
        update CourseClass
        set state = ${EntityState.deleted.toString}
        where uuid = ${uuid}
      """.executeUpdate
      courseClass
    } else {
      throw new EntityConflictException("classHasEnrollments")
    }
  }

  def copy: CourseClass = {
    val courseClass = CourseClassRepo(uuid).first.get
    val sourceCourseClassUUID = courseClass.getUUID
    val targetCourseClassUUID = UUID.randomUUID.toString
    val ecommerceIdentifier = UUID.randomUUID.toString.replace("-", "").substring(0, 20)

    //copy courseClass
    courseClass.setUUID(targetCourseClassUUID)
    courseClass.setName(courseClass.getName + " (2)")
    courseClass.setCreatedBy(ThreadLocalAuthenticator.getAuthenticatedPersonUUID.get)
    courseClass.setCreatedAt(new Date())
    courseClass.setEcommerceIdentifier(ecommerceIdentifier)
    if (StringUtils.isSome(courseClass.getThumbUrl)) {
      courseClass.setThumbUrl(courseClass.getThumbUrl.replace(sourceCourseClassUUID + "/thumb.jpg", targetCourseClassUUID + "/thumb.jpg"))
    }
    CourseClassesRepo.create(courseClass)

    //copy roles
    val roles = new RolesRepo().getAllUsersWithRoleForCourseClass(sourceCourseClassUUID)
    roles.getRoleTOs.asScala.foreach(roleTO => {
      val role = roleTO.getRole
      role.setUUID(UUID.randomUUID.toString)
      RoleCategory.setCourseClassUUID(role, targetCourseClassUUID)
      new RolesRepo().create(role)
    })

    AssetService.copyAssets(courseClass.getInstitutionUUID, CourseDetailsEntityType.COURSE_CLASS, sourceCourseClassUUID, targetCourseClassUUID, courseClass.getThumbUrl)

    courseClass
  }

  def updateRequiredScore(courseClass: CourseClass, wizardRequiredScore: BigDecimal) = {
    if(courseClass.getRequiredScore != wizardRequiredScore) {
      courseClass.setRequiredScore(wizardRequiredScore)
      update(courseClass)
    }
  }
}

object CourseClassRepo extends App {
  def apply(uuid: String) = new CourseClassRepo(uuid)
}
