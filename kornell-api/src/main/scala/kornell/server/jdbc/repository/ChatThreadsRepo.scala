package kornell.server.jdbc.repository

import java.sql.ResultSet
import java.util.{Date, UUID}

import kornell.core.entity._
import kornell.core.entity.role.{RoleCategory, RoleType}
import kornell.core.to.{ChatThreadMessageTO, ChatThreadMessagesTO, UnreadChatThreadTO, UnreadChatThreadsTO}
import kornell.server.jdbc.SQL.SQLHelper
import kornell.server.repository.{Entities, TOs}
import kornell.server.util.EmailService

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.immutable.List

object ChatThreadsRepo {

  def postMessageToCourseClassThread(personUUID: String, courseClassUUID: String, message: String, threadType: ChatThreadType): Unit = {
    val courseClass = CourseClassRepo(courseClassUUID).get
    val chatThreadUUID = getCourseClassChatThreadUUID(personUUID, courseClass.getUUID, threadType)
    if (chatThreadUUID.isEmpty) {
      val course = CoursesRepo.byCourseClassUUID(courseClassUUID).get
      val chatThread = createChatThread(courseClass.getInstitutionUUID, courseClass.getUUID, personUUID, threadType)
      updateChatThreadParticipants(chatThread.getUUID, personUUID, courseClass, threadType, courseClass.getInstitutionUUID)
      createChatThreadMessage(chatThread.getUUID, personUUID, message)
      sendEmailForThreadCreation(courseClass, course, chatThread, threadType, message, courseClass.getInstitutionUUID)
    } else {
      createChatThreadMessage(chatThreadUUID.get, personUUID, message)
    }
  }

  def postMessageToInstitutionThread(personUUID: String, institutionUUID: String, message: String, threadType: ChatThreadType): Unit = {
    val chatThreadUUID = getInstitutionChatThreadUUID(personUUID, institutionUUID, threadType)
    if (chatThreadUUID.isEmpty) {
      val chatThread = createChatThread(institutionUUID, null, personUUID, threadType)
      updateChatThreadParticipants(chatThread.getUUID, personUUID, null, threadType, institutionUUID)
      createChatThreadMessage(chatThread.getUUID, personUUID, message)
      sendEmailForThreadCreation(null, null, chatThread, threadType, message, institutionUUID)
    } else {
      createChatThreadMessage(chatThreadUUID.get, personUUID, message)
    }
  }

  def postMessageToDirectThread(fromPersonUUID: String, toPersonUUID: String, message: String): Unit = {
    val fromPerson = new PersonRepo(fromPersonUUID).get
    val toPerson = new PersonRepo(toPersonUUID).get
    if (!fromPerson.getInstitutionUUID.equals(toPerson.getInstitutionUUID)) {
      throw new IllegalStateException("Cannot send message to person in another institution")
    }
    //the message may be coming from any of the participants, got to check both sides
    val chatThreadUUID = {
      val tempThreadUUID = getDirectChatThreadUUID(fromPersonUUID, toPersonUUID)
      if (tempThreadUUID.isDefined) {
        tempThreadUUID
      } else {
        null
      }
    }
    if (chatThreadUUID == null) {
      val chatThread = createChatThread(fromPerson.getInstitutionUUID, null, fromPersonUUID, ChatThreadType.DIRECT)
      createChatThreadParticipant(chatThread.getUUID, fromPersonUUID)
      createChatThreadParticipant(chatThread.getUUID, toPersonUUID)
      createChatThreadMessage(chatThread.getUUID, fromPersonUUID, message)
    } else {
      createChatThreadMessage(chatThreadUUID.get, fromPersonUUID, message)
    }
  }

  def sendEmailForThreadCreation(courseClass: CourseClass, course: Course, chatThread: ChatThread, threadType: ChatThreadType, message: String, institutionUUID: String): Unit = {
    val institution = new InstitutionRepo(chatThread.getInstitutionUUID).get
    threadType match {
      case ChatThreadType.SUPPORT => new RolesRepo().getCourseClassSupportThreadParticipants(courseClass.getUUID, courseClass.getInstitutionUUID, RoleCategory.BIND_WITH_PERSON)
        .getRoleTOs.asScala.filter(role => !chatThread.getPersonUUID.equals(role.getPerson.getUUID)).foreach(role => EmailService.sendEmailNewChatThread(role.getPerson, institution, courseClass, course, chatThread, message))
      case ChatThreadType.TUTORING => new RolesRepo().getUsersForCourseClassByRole(courseClass.getUUID, RoleType.tutor, RoleCategory.BIND_WITH_PERSON)
        .getRoleTOs.asScala.filter(role => !chatThread.getPersonUUID.equals(role.getPerson.getUUID)).foreach(role => EmailService.sendEmailNewChatThread(role.getPerson, institution, courseClass, course, chatThread, message))
      case ChatThreadType.INSTITUTION_SUPPORT => new RolesRepo().getPlatformSupportThreadParticipants(institutionUUID, RoleCategory.BIND_WITH_PERSON)
        .getRoleTOs.asScala.filter(role => !chatThread.getPersonUUID.equals(role.getPerson.getUUID)).foreach(role => EmailService.sendEmailNewChatThread(role.getPerson, institution, courseClass, course, chatThread, message))
      case ChatThreadType.PLATFORM_SUPPORT => new RolesRepo().getPlatformSupportThreadParticipants(institutionUUID, RoleCategory.BIND_WITH_PERSON)
        .getRoleTOs.asScala.filter(role => !chatThread.getPersonUUID.equals(role.getPerson.getUUID)).foreach(role => EmailService.sendEmailNewChatThread(role.getPerson, institution, courseClass, course, chatThread, message))
      case ChatThreadType.COURSE_CLASS | ChatThreadType.DIRECT => throw new IllegalStateException("not-supported-for-this-type")
    }
  }

  def updateChatThreadParticipants(chatThreadUUID: String, threadCreatorUUID: String, courseClass: CourseClass, threadType: ChatThreadType, institutionUUID: String): Unit = {
    val participantsThatShouldExist = threadType match {
      case ChatThreadType.SUPPORT => new RolesRepo().getCourseClassSupportThreadParticipants(courseClass.getUUID, courseClass.getInstitutionUUID, null)
        .getRoleTOs.asScala.map(_.getRole.getPersonUUID).+:(threadCreatorUUID).toList.distinct
      case ChatThreadType.TUTORING => new RolesRepo().getUsersForCourseClassByRole(courseClass.getUUID, RoleType.tutor, RoleCategory.BIND_WITH_PERSON)
        .getRoleTOs.asScala.map(_.getRole.getPersonUUID).+:(threadCreatorUUID).toList.distinct
      case ChatThreadType.INSTITUTION_SUPPORT => new RolesRepo().getPlatformSupportThreadParticipants(institutionUUID, null)
        .getRoleTOs.asScala.map(_.getRole.getPersonUUID).+:(threadCreatorUUID).toList.distinct
      case ChatThreadType.PLATFORM_SUPPORT => new RolesRepo().getPlatformSupportThreadParticipants(institutionUUID, null)
        .getRoleTOs.asScala.map(_.getRole.getPersonUUID).+:(threadCreatorUUID).toList.distinct
      case ChatThreadType.COURSE_CLASS | ChatThreadType.DIRECT => throw new IllegalStateException("not-supported-for-this-type")
    }

    val participants = getChatTreadParticipantsUUIDs(chatThreadUUID).distinct
    val createThese = participantsThatShouldExist.filterNot(participants.contains)
    val removeThese = participants.filterNot(participantsThatShouldExist.contains)
    createThese.foreach(createChatThreadParticipant(chatThreadUUID, _))
    removeThese.foreach(removeChatThreadParticipant(chatThreadUUID, _))
  }

  def getChatTreadParticipantsUUIDs(chatThreadUUID: String): List[String] = sql"""
        | select p.uuid
          | from ChatThreadParticipant ctp
          | join Person p on p.uuid = ctp.personUUID
          | where ctp.chatThreadUUID = ${chatThreadUUID}
        """.map[String]

  def getChatTreadParticipant(personUUID: String, chatThreadUUID: String): Option[ChatThreadParticipant] = sql"""
            | select *
            | from ChatThreadParticipant ctp
            | where ctp.chatThreadUUID = ${chatThreadUUID}
        | and ctp.personUUID = ${personUUID}
            """.first[ChatThreadParticipant](toChatThreadParticipant)

  def updateChatThreadParticipantActivation(chatThreadParticipantUUID: String, active: Boolean): Unit = {
    if (active) {
      sql"""update ChatThreadParticipant set active = ${active}, lastJoinDate = now() where uuid = ${chatThreadParticipantUUID}""".executeUpdate
    } else {
      sql"""update ChatThreadParticipant set active = ${active} where uuid = ${chatThreadParticipantUUID}""".executeUpdate
    }

  }

  def createChatThreadParticipant(chatThreadUUID: String, personUUID: String): Unit = {
    sql"""
    insert into ChatThreadParticipant (uuid, chatThreadUUID, personUUID, lastReadAt, active, lastJoinDate)
    values (${UUID.randomUUID.toString}, ${chatThreadUUID} , ${personUUID}, now(), 1, now())""".executeUpdate
  }

  def removeChatThreadParticipant(chatThreadUUID: String, personUUID: String): Unit = {
    sql"""
      delete from ChatThreadParticipant
      where chatThreadUUID = ${chatThreadUUID}
      and personUUID = ${personUUID}""".executeUpdate
  }

  def createChatThread(institutionUUID: String, courseClassUUID: String, personUUID: String, threadType: ChatThreadType): ChatThread = {
    createChatThread(Entities.newChatThread(UUID.randomUUID.toString, new Date(), institutionUUID, courseClassUUID, personUUID, threadType.toString))
  }

  def createChatThread(chatThread: ChatThread): ChatThread = {
    if (chatThread.getUUID == null)
      chatThread.setUUID(UUID.randomUUID.toString)
    sql"""
    insert into ChatThread (uuid, createdAt, institutionUUID, courseClassUUID, personUUID, threadType, active)
    values (${chatThread.getUUID}, ${chatThread.getCreatedAt}, ${chatThread.getInstitutionUUID}, ${chatThread.getCourseClassUUID}, ${chatThread.getPersonUUID}, ${chatThread.getThreadType}, 1)
    """.executeUpdate
    chatThread
  }

  def isParticipant(chatThreadUUID: String, personUUID: String): Boolean =
    sql"""select count(*) as participantExists from ChatThreadParticipant where chatThreadUUID = ${chatThreadUUID} and personUUID = ${personUUID}"""
      .first[Integer] { rs => rs.getInt("participantExists") }.get >= 1

  def createChatThreadMessage(chatThreadUUID: String, personUUID: String, message: String): Unit = {
    sql"""
    insert into ChatThreadMessage (uuid, chatThreadUUID, sentAt, personUUID, message)
    values (${UUID.randomUUID.toString}, ${chatThreadUUID} , now(), ${personUUID}, ${message})
    """.executeUpdate

    sql"""
      update ChatThread set lastSentAt = now() where uuid = ${chatThreadUUID}
    """.executeUpdate

    sql"""
      | update ChatThreadParticipant set
      | unreadCount = unreadCount+1
      | where chatThreadUUID = ${chatThreadUUID}
      | and personUUID <> ${personUUID}
    """.executeUpdate
  }

  def getCourseClassChatThreadUUID(personUUID: String, courseClassUUID: String, threadType: ChatThreadType): Option[String] = {
    sql"""
        | select uuid
          | from ChatThread
          | where ( personUUID = ${personUUID} or ${personUUID} is null )
          | and ( courseClassUUID = ${courseClassUUID} or ${courseClassUUID} is null )
          | and threadType = ${threadType.toString}
        """.first[String]
  }

  def getInstitutionChatThreadUUID(personUUID: String, institutionUUID: String, threadType: ChatThreadType): Option[String] = {
    sql"""
        | select uuid
          | from ChatThread
          | where ( personUUID = ${personUUID} or ${personUUID} is null )
          | and ( institutionUUID = ${institutionUUID} or ${institutionUUID} is null )
          | and threadType = ${threadType.toString}
        """.first[String]
  }

  def getDirectChatThreadUUID(fromPersonUUID: String, toPersonUUID: String): Option[String] = {
    sql"""
        | select t.uuid
        | from ChatThread t
        | where ( select count(uuid) from ChatThreadParticipant ctp where personUUID = ${fromPersonUUID}
          | and t.uuid = ctp.chatThreadUUID) = 1
        | and ( select count(uuid) from ChatThreadParticipant ctp where personUUID = ${toPersonUUID}
          | and t.uuid = ctp.chatThreadUUID) = 1
        | and t.threadType = ${ChatThreadType.DIRECT.toString}
        """.first[String]
  }

  def updateParticipantsInCourseClassSupportThreadsForInstitution(institutionUUID: String, threadType: ChatThreadType): Unit = {
    sql"""select uuid from CourseClass where institutionUUID = ${institutionUUID} and state <> ${EntityState.deleted.toString}""".map[String](toString)
      .foreach(cc => updateParticipantsInThreads(cc, institutionUUID, threadType))
  }

  def updateParticipantsInThreads(courseClassUUID: String, institutionUUID: String, threadType: ChatThreadType): Unit = {
    type CourseClassSupportThreadData = (String, String)
    implicit def courseClassSupportThreadConvertion(rs: ResultSet): CourseClassSupportThreadData = (rs.getString(1), rs.getString(2))

    if (!ChatThreadType.PLATFORM_SUPPORT.equals(threadType)) {
      val courseClass = CourseClassRepo(courseClassUUID).get
      sql"""
      select uuid, personUUID from ChatThread
        where courseClassUUID = ${courseClassUUID}
          and threadType = ${threadType.toString}
        """.map[CourseClassSupportThreadData](courseClassSupportThreadConvertion)
        .foreach(ct => updateChatThreadParticipants(ct._1, ct._2, courseClass, threadType, courseClass.getInstitutionUUID))
    } else {
      sql"""
      select uuid, personUUID from ChatThread
        where institutionUUID = ${institutionUUID}
          and threadType = ${threadType.toString}
        """.map[CourseClassSupportThreadData](courseClassSupportThreadConvertion)
        .foreach(ct => updateChatThreadParticipants(ct._1, ct._2, null, threadType, institutionUUID))
    }
  }

  def getTotalUnreadCountByPerson(personUUID: String, institutionUUID: String): String = {
    sql"""
        select sum(unreadCount) as unreadMessages from ChatThreadParticipant tp where tp.personUUID = ${personUUID}
        """.first[String].get
  }

  def getTotalUnreadCountsByPersonPerThread(personUUID: String, institutionUUID: String): UnreadChatThreadsTO = {
    TOs.newUnreadChatThreadsTO(sql"""
       | select pt.unreadCount as unreadMessages, pt.chatThreadUUID, t.threadType, p.fullName as creatorName, t.lastSentAt,
       |   (case t.threadType when ${ChatThreadType.DIRECT.toString} then
       |     (select pin.fullName
       |       from ChatThreadParticipant ctpin
       |         join Person pin on ctpin.personUUID = pin.uuid
       |       where ctpin.personUUID <> ${personUUID}
       |         and t.uuid = ctpin.chatThreadUUID
       |     ) else concat(c.name, ' · ', cc.name) end) as entityName,
       |    (case t.threadType when ${ChatThreadType.DIRECT.toString} then
       |     (select cpin2.personUUID
       |       from ChatThreadParticipant cpin2
       |       where cpin2.personUUID <> ${personUUID}
       |         and t.uuid = chatThreadUUID
       |     ) else  cc.uuid end) as entityUUID
       | from ChatThreadParticipant pt
       | join ChatThread t on t.uuid = pt.chatThreadUUID
       | join Person p on pt.personUUID = p.uuid
       | left join CourseClass cc on t.courseClassUUID = cc.uuid
       | left join CourseVersion cv on cc.courseVersionUUID = cv.uuid
       | left join Course c on cv.courseUUID = c.uuid
       | where pt.personUUID = ${personUUID}
       | order by t.lastSentAt desc
        """.map[UnreadChatThreadTO](toUnreadChatThreadTO))
  }

  def getChatThreadMessagesSince(chatThreadUUID: String, lastFetchedMessageSentAt: Date): ChatThreadMessagesTO = TOs.newChatThreadMessagesTO(sql"""
    select
      p.fullName as senderFullName,
      (select role
      from Role r
      where r.personUUID = p.uuid and
      (r.institutionUUID = t.institutionUUID or r.courseClassUUID = t.courseClassUUID) and
      (r.role <> ${RoleType.tutor.toString} or t.threadType = ${ChatThreadType.TUTORING.toString})
      order by
        case
          when (r.role = ${RoleType.tutor.toString} and
            (t.threadType = ${ChatThreadType.TUTORING.toString} or
             t.threadType = ${ChatThreadType.COURSE_CLASS.toString})) then 1
          when r.role = ${RoleType.platformAdmin.toString} then 2
          when r.role = ${RoleType.institutionAdmin.toString} then 3
          when (r.role = ${RoleType.courseClassAdmin.toString}  || r.role = ${RoleType.institutionCourseClassesAdmin.toString}) then 4
          else 5
          end
      limit 1
      ) as senderRole,
      tm.sentAt,
        tm.message
    from ChatThreadMessage tm
    join ChatThread t on t.uuid = tm.chatThreadUUID
    join Person p on p.uuid = tm.personUUID
      where tm.chatThreadUUID = ${chatThreadUUID}
        and tm.sentAt > ${lastFetchedMessageSentAt}
      order by tm.sentAt
    """.map[ChatThreadMessageTO](toChatThreadMessageTO))

  def getChatThreadMessagesBefore(chatThreadUUID: String, firstFetchedMessageSentAt: Date): ChatThreadMessagesTO = TOs.newChatThreadMessagesTO(sql"""
    select
      p.fullName as senderFullName,
      (select role
      from Role r
      where r.personUUID = p.uuid and
      (r.institutionUUID = t.institutionUUID or r.courseClassUUID = t.courseClassUUID) and
      (r.role <> ${RoleType.tutor.toString} or t.threadType = ${ChatThreadType.TUTORING.toString})
      order by
        case
          when (r.role = ${RoleType.tutor.toString} and t.threadType = ${ChatThreadType.TUTORING.toString}) then 1
          when r.role = ${RoleType.platformAdmin.toString} then 2
          when r.role = ${RoleType.institutionAdmin.toString}  then 3
          when (r.role = ${RoleType.courseClassAdmin.toString} || r.role = ${RoleType.institutionCourseClassesAdmin.toString}) then 4
          else 5
          end
      limit 1
      ) as senderRole,
      tm.sentAt,
        tm.message
    from ChatThreadMessage tm
    join ChatThread t on t.uuid = tm.chatThreadUUID
    join Person p on p.uuid = tm.personUUID
     where tm.chatThreadUUID = ${chatThreadUUID}
        and tm.sentAt < ${firstFetchedMessageSentAt}
     order by tm.sentAt desc limit 20
    """.map[ChatThreadMessageTO](toChatThreadMessageTO))

  def markAsRead(chatThreadUUID: String, personUUID: String): Unit = {
    sql"""
      | update ChatThreadParticipant p set
      | lastReadAt = ${new Date()},
      | unreadCount = 0
      | where p.chatThreadUUID = ${chatThreadUUID}
      | and p.personUUID = ${personUUID}""".executeUpdate
  }

  def getCourseClassGlobalChatThread(courseClassUUID: String): Option[ChatThread] = {
    sql"""select * from ChatThread where courseClassUUID = ${courseClassUUID} and threadType =  ${ChatThreadType.COURSE_CLASS.toString}""".first[ChatThread](toChatThread)
  }

  def createCourseClassGlobalChatThread(institutionUUID: String, courseClassUUID: String): Unit = {
    sql"""insert into ChatThread (uuid, institutionUUID, courseClassUUID, threadType, createdAt, active) values (
    ${UUID.randomUUID.toString}, ${institutionUUID}, ${courseClassUUID}, ${ChatThreadType.COURSE_CLASS.toString}, ${new Date}, 1)
    """.executeUpdate
  }

  def updateChatThreadParticipantStatus(chatThreadParticipantUUID: String, status: Boolean): Unit = {
    sql"""update ChatThreadParticipant set active = ${status} where uuid = ${chatThreadParticipantUUID}""".executeUpdate
  }

  def updateCourseClassChatThreadStatusByThreadType(courseClassUUID: String, chatThreadType: ChatThreadType, status: Boolean): Unit = {
    sql"""update ChatThread set active = ${status} where courseClassUUID = ${courseClassUUID} and threadType = ${chatThreadType.toString}""".executeUpdate
  }

  /**
   * Call this when enrolling a user
   */
  def addParticipantToCourseClassThread(enrollment: Enrollment): Unit = {
    val chatThread = getCourseClassGlobalChatThread(enrollment.getCourseClassUUID)
    if (chatThread.isDefined) {
      val participant = getChatTreadParticipant(enrollment.getPersonUUID, chatThread.get.getUUID)
      if (participant.isEmpty) {
        CourseClassRepo(enrollment.getCourseClassUUID).get
        createChatThreadParticipant(chatThread.get.getUUID, enrollment.getPersonUUID)
      } else {
        updateChatThreadParticipantStatus(participant.get.getUUID, true)
      }
    }
  }

  /**
   * Call this when un-enrolling a user
   */
  def disableParticipantFromCourseClassThread(enrollment: Enrollment): Unit = {
    val chatThread = getCourseClassGlobalChatThread(enrollment.getCourseClassUUID)
    if (chatThread.isDefined) {
      val participant = getChatTreadParticipant(enrollment.getPersonUUID, chatThread.get.getUUID)
      if (participant.isDefined) {
        updateChatThreadParticipantStatus(participant.get.getUUID, false)
      }
    }
  }

  /**
   * Call this when updating the course class properties
   */
  def addParticipantsToCourseClassThread(courseClass: CourseClass): Unit = {
    if (courseClass.isCourseClassChatEnabled) {
      val threadUUID = getCourseClassGlobalChatThread(courseClass.getUUID).
        getOrElse(createChatThread(courseClass.getInstitutionUUID, courseClass.getUUID, null, ChatThreadType.COURSE_CLASS)).getUUID
      EnrollmentsRepo.byCourseClass(courseClass.getUUID).getEnrollmentTOs.asScala.foreach(enrollment => {
        val participant = getChatTreadParticipant(enrollment.getPersonUUID, threadUUID)
        if (participant.isEmpty) {
          createChatThreadParticipant(threadUUID, enrollment.getPersonUUID)
        }
      })
    }
  }

  implicit def toString(rs: ResultSet): String = rs.getString(1)

}
