package kornell.server.jdbc.repository

import java.util.UUID

import kornell.core.entity.AuditedEntityType
import kornell.core.to.InstitutionEmailWhitelistTO
import kornell.server.jdbc.SQL._
import kornell.server.repository.TOs

import scala.collection.JavaConverters._

class InstitutionEmailWhitelistRepo(institutionUUID: String) {

  def get: InstitutionEmailWhitelistTO = {
    TOs.newInstitutionEmailWhitelistTO(
      sql"""
        | select domain from InstitutionEmailWhitelist
        | where institutionUUID = ${institutionUUID}
        | order by domain"""
        .map[String])
  }

  def updateDomains(domains: InstitutionEmailWhitelistTO): InstitutionEmailWhitelistTO = {
    val from = get

    removeDomains(institutionUUID)
    domains.getDomains.asScala.foreach(domain => addDomain(domain))

    //log entity change
    EventsRepo.logEntityChange(institutionUUID, AuditedEntityType.institutionEmailWhitelist, institutionUUID, from, domains)
    domains
  }

  def removeDomains(institutionUUID: String): InstitutionEmailWhitelistRepo = {
    sql"""delete from InstitutionEmailWhitelist where institutionUUID = ${institutionUUID}""".executeUpdate
    this
  }

  def addDomain(domain: String): Unit = {
    sql"""insert into InstitutionEmailWhitelist (uuid, domain, institutionUUID) values
    (${UUID.randomUUID.toString},
    ${domain},
    ${institutionUUID})""".executeUpdate
  }
}

object InstitutionEmailWhitelistRepo {
  def apply(institutionUUID: String) = new InstitutionEmailWhitelistRepo(institutionUUID)
}
