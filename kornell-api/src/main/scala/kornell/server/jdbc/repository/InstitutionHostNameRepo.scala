package kornell.server.jdbc.repository

import java.util.UUID

import kornell.core.entity.AuditedEntityType
import kornell.core.to.InstitutionHostNamesTO
import kornell.server.jdbc.SQL._
import kornell.server.repository.TOs

import scala.collection.JavaConverters._

class InstitutionHostNameRepo(institutionUUID: String) {

  def get: InstitutionHostNamesTO = {
    TOs.newInstitutionHostNamesTO(
      sql"""
        | select hostName from InstitutionHostName
        | where institutionUUID = ${institutionUUID}
        | order by hostName"""
        .map[String])
  }

  def updateHostnames(hostnames: InstitutionHostNamesTO): InstitutionHostNamesTO = {
    val from = get

    removeHostnames(institutionUUID)
    hostnames.getInstitutionHostNames.asScala.foreach(hostname => addHostname(hostname))

    //log entity change
    EventsRepo.logEntityChange(institutionUUID, AuditedEntityType.institutionHostName, institutionUUID, from, hostnames)

    hostnames
  }

  def removeHostnames(institutionUUID: String): InstitutionHostNameRepo = {
    sql"""delete from InstitutionHostName where institutionUUID = ${institutionUUID}""".executeUpdate
    InstitutionsRepo.cleanUpHostNameCache()
    this
  }

  def addHostname(hostName: String): Unit = {
    sql"""insert into InstitutionHostName (uuid, hostName, institutionUUID) values
    (${UUID.randomUUID.toString},
    ${hostName},
    ${institutionUUID})""".executeUpdate
    InstitutionsRepo.updateHostNameCache(institutionUUID, hostName)
  }
}

object InstitutionHostNameRepo {
  def apply(institutionUUID: String) = new InstitutionHostNameRepo(institutionUUID)
}
