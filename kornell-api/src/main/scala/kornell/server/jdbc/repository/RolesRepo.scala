package kornell.server.jdbc.repository

import java.util.UUID

import kornell.core.entity.AuditedEntityType
import kornell.core.entity.role.{Role, RoleCategory, RoleType, Roles}
import kornell.core.error.exception.EntityConflictException
import kornell.core.to.{RoleTO, RolesTO}
import kornell.core.util.StringUtils
import kornell.server.jdbc.SQL._
import kornell.server.repository.TOs
import kornell.server.service.SandboxService

import scala.collection.JavaConverters._

class RolesRepo {

  def getUserRoles(personUUID: String, bindMode: String): RolesTO =
    TOs.newRolesTO(sql"""
      | select r.*, pw.username, cc.name as courseClassName
      | from Role r
      | join Password pw on pw.personUUID = r.personUUID
      | left join CourseClass cc on r.courseClassUUID = cc.uuid
      | where pw.personUUID = ${personUUID}
      | order by r.role, pw.username
      """.map[RoleTO](toRoleTO(_, bindMode)))

  def getUsersForCourseClassByRole(courseClassUUID: String, roleType: RoleType, bindMode: String): RolesTO =
    TOs.newRolesTO(sql"""
      | select r.*,
      | if(pw.username is not null, pw.username, p.email) as username,
      | cc.name as courseClassName
      | from Role r
      | join Person p on p.uuid = r.personUUID
      | left join Password pw on pw.personUUID = r.personUUID
      | left join CourseClass cc on r.courseClassUUID = cc.uuid
      | where r.courseClassUUID = ${courseClassUUID}
      | and r.role = ${roleType.toString}
      | order by r.role, if(pw.username is not null, pw.username, p.email)
    """.map[RoleTO](toRoleTO(_, bindMode)))

  def getAllUsersWithRoleForCourseClass(courseClassUUID: String): RolesTO =
    TOs.newRolesTO(sql"""
      | select r.*, pw.username, cc.name as courseClassName
      | from Role r
      | join Password pw on pw.personUUID = r.personUUID
      | left join CourseClass cc on r.courseClassUUID = cc.uuid
      | where r.courseClassUUID = ${courseClassUUID}
      | order by r.role, pw.username
    """.map[RoleTO](toRoleTO(_, RoleCategory.BIND_DEFAULT)))

  def getInstitutionAdmins(institutionUUID: String, bindMode: String): RolesTO =
    TOs.newRolesTO(sql"""
      | select r.*, pw.username, null as courseClassName
      | from Role r
      | join Password pw on pw.personUUID = r.personUUID
      | where r.institutionUUID = ${institutionUUID}
      | and r.role = ${RoleType.institutionAdmin.toString}
      | order by r.role, pw.username
    """.map[RoleTO](toRoleTO(_, bindMode)))

  def getPublishers(institutionUUID: String, bindMode: String): RolesTO =
    TOs.newRolesTO(sql"""
      | select r.*, pw.username, null as courseClassName
      | from Role r
      | join Password pw on pw.personUUID = r.personUUID
      | where r.institutionUUID = ${institutionUUID}
      | and r.role = ${RoleType.publisher.toString}
      | order by r.role, pw.username
    """.map[RoleTO](toRoleTO(_, bindMode)))

  def getPlatformAdmins(institutionUUID: String, bindMode: String): RolesTO =
    TOs.newRolesTO(sql"""
      | select r.*, pw.username, null as courseClassName
      | from Role r
      | join Password pw on pw.personUUID = r.personUUID
      | where r.institutionUUID = ${institutionUUID}
      | and r.role = ${RoleType.platformAdmin.toString}
      | order by r.role, pw.username
    """.map[RoleTO](toRoleTO(_, bindMode)))

  def getCourseClassSupportThreadParticipants(courseClassUUID: String, institutionUUID: String, bindMode: String): RolesTO =
    TOs.newRolesTO(sql"""
      | select r.*, pw.username, cc.name as courseClassName
      | from (select * from Role
      | where (courseClassUUID = ${courseClassUUID} and role = ${RoleType.courseClassAdmin.toString})
      |   or (institutionUUID = ${institutionUUID} and role = ${RoleType.institutionAdmin.toString})
      |   or (institutionUUID = ${institutionUUID} and role = ${RoleType.platformAdmin.toString})
      | order by case `role`
      |  when 'platformAdmin' then 1
      |  when 'institutionAdmin' then 2
      |  when 'courseClassAdmin' then 3
      |  END) r
      | join Password pw on pw.personUUID = r.personUUID
      | left join CourseClass cc on r.courseClassUUID = cc.uuid
    """.map[RoleTO](toRoleTO(_, bindMode)))

  def getPlatformSupportThreadParticipants(institutionUUID: String, bindMode: String): RolesTO =
    TOs.newRolesTO(sql"""
      | select r.*, pw.username, null as courseClassName
      | from (select * from Role
      |    order by case `role`
      |  when 'platformAdmin' then 1
      |  when 'institutionAdmin' then 2
      |  END) r
      | join Password pw on pw.personUUID = r.personUUID
      | where (r.institutionUUID = ${institutionUUID}
      |   and r.role = ${RoleType.institutionAdmin.toString})
      | or (r.institutionUUID = ${institutionUUID}
      |   and r.role = ${RoleType.platformAdmin.toString})
    """.map[RoleTO](toRoleTO(_, bindMode)))

  def getAllUsersWithRoleForInstitution(institutionUUID: String, bindMode: String): RolesTO =
    TOs.newRolesTO(sql"""
      | select r.*, pw.username, null as courseClassName
      | from Role r
      | join Password pw on pw.personUUID = r.personUUID
      | where r.institutionUUID = ${institutionUUID}
      | order by r.role, pw.username
    """.map[RoleTO](toRoleTO(_, bindMode)))

  def updateCourseClassAdmins(institutionUUID: String, courseClassUUID: String, roles: Roles): Roles = updateCourseClassRole(institutionUUID, courseClassUUID, RoleType.courseClassAdmin, roles)

  def updateTutors(institutionUUID: String, courseClassUUID: String, roles: Roles): Roles = updateCourseClassRole(institutionUUID, courseClassUUID, RoleType.tutor, roles)

  def updateCourseClassObservers(institutionUUID: String, courseClassUUID: String, roles: Roles): Roles = updateCourseClassRole(institutionUUID, courseClassUUID, RoleType.courseClassObserver, roles)

  def updateCourseClassRole(institutionUUID: String, courseClassUUID: String, roleType: RoleType, roles: Roles): Roles = {
    val from = getUsersForCourseClassByRole(courseClassUUID, roleType, RoleCategory.BIND_DEFAULT)

    removeCourseClassRole(courseClassUUID, roleType).addRoles(roles)

    val to = getUsersForCourseClassByRole(courseClassUUID, roleType, RoleCategory.BIND_DEFAULT)

    val auditedEntityType = {
      roleType match {
        case RoleType.courseClassAdmin => AuditedEntityType.courseClassAdmin
        case RoleType.tutor => AuditedEntityType.courseClassTutor
        case RoleType.courseClassObserver => AuditedEntityType.courseClassObserver
        case _ => throw new EntityConflictException("invalidValue")
      }
    }

    //log entity change
    EventsRepo.logEntityChange(institutionUUID, auditedEntityType, courseClassUUID, from, to)

    roles
  }

  def updateInstitutionAdmins(institutionUUID: String, roles: Roles): Roles = {
    val from = getInstitutionAdmins(institutionUUID, RoleCategory.BIND_DEFAULT)

    removeInstitutionAdmins(institutionUUID).addRoles(roles)

    val to = getInstitutionAdmins(institutionUUID, RoleCategory.BIND_DEFAULT)

    //log entity change
    EventsRepo.logEntityChange(institutionUUID, AuditedEntityType.institutionAdmin, institutionUUID, from, to)
    SandboxService.fixEnrollments(institutionUUID, from, to)

    roles
  }

  def updatePublishers(institutionUUID: String, roles: Roles): Roles = {
    val from = getPublishers(institutionUUID, RoleCategory.BIND_DEFAULT)

    removePublishers(institutionUUID).addRoles(roles)

    val to = getPublishers(institutionUUID, RoleCategory.BIND_DEFAULT)

    //log entity change
    EventsRepo.logEntityChange(institutionUUID, AuditedEntityType.publisher, institutionUUID, from, to)

    roles
  }

  def addRoles(roles: Roles): Roles = {
    roles.getRoles.asScala.foreach(create)
    roles
  }

  def create(role: Role): Unit = {
    if (StringUtils.isNone(role.getUUID)) {
      role.setUUID(UUID.randomUUID.toString)
    }
    if (RoleType.courseClassAdmin.equals(role.getRoleType) || RoleType.tutor.equals(role.getRoleType)
      || RoleType.courseClassObserver.equals(role.getRoleType)) {
      sql"""
        insert into Role (uuid, personUUID, role, courseClassUUID) values (
        ${role.getUUID},
        ${role.getPersonUUID},
        ${role.getRoleType.toString},
        ${RoleCategory.getCourseClassUUID(role)})
      """.executeUpdate
    }
    if (RoleType.institutionAdmin.equals(role.getRoleType)) {
      sql"""
        insert into Role (uuid, personUUID, role, institutionUUID) values (
        ${role.getUUID},
        ${role.getPersonUUID},
        ${role.getRoleType.toString},
        ${role.getInstitutionAdminRole.getInstitutionUUID})
      """.executeUpdate
    }

    if (RoleType.publisher.equals(role.getRoleType)) {
      sql"""
        insert into Role (uuid, personUUID, role, institutionUUID) values (
        ${role.getUUID},
        ${role.getPersonUUID},
        ${role.getRoleType.toString},
        ${role.getPublisherRole.getInstitutionUUID})
      """.executeUpdate
    }
  }

  def removeCourseClassRole(courseClassUUID: String, roleType: RoleType): RolesRepo = {
    sql"""
      delete from Role
      where courseClassUUID = ${courseClassUUID}
        and role = ${roleType.toString}
    """.executeUpdate
    this
  }

  def removeInstitutionAdmins(institutionUUID: String): RolesRepo = {
    sql"""
        delete from Role
        where institutionUUID = ${institutionUUID}
        and role = ${RoleType.institutionAdmin.toString}
    """.executeUpdate
    this
  }

  def removePublishers(institutionUUID: String): RolesRepo = {
    sql"""
        delete from Role
        where institutionUUID = ${institutionUUID}
        and role = ${RoleType.publisher.toString}
    """.executeUpdate
    this
  }

}