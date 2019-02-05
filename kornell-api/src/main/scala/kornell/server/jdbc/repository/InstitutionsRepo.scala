package kornell.server.jdbc.repository

import java.util.{Date, UUID}
import java.util.concurrent.TimeUnit.MINUTES

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import kornell.core.entity.{AuditedEntityType, Institution, InstitutionType}
import kornell.server.jdbc.SQL._

object InstitutionsRepo {

  val cacheBuilder: CacheBuilder[AnyRef, AnyRef] = CacheBuilder
    .newBuilder()
    .expireAfterAccess(5, MINUTES)
    .maximumSize(1000)

  /*uuid cache*/
  val uuidLoader: CacheLoader[String, Option[Institution]] = new CacheLoader[String, Option[Institution]]() {
    override def load(uuid: String): Option[Institution] = lookupByUUID(uuid)
  }
  val uuidCache: LoadingCache[String, Option[Institution]] = cacheBuilder.build(uuidLoader)
  def getByUUID(uuid: String): Option[Institution] = uuidCache.get(uuid)
  def lookupByUUID(UUID: String): Option[Institution] =
    sql"select * from Institution where uuid = ${UUID}".first[Institution]

  /*name cache*/
  val nameLoader: CacheLoader[String, Option[Institution]] = new CacheLoader[String, Option[Institution]]() {
    override def load(name: String): Option[Institution] = lookupByName(name)
  }
  val nameCache: LoadingCache[String, Option[Institution]] = cacheBuilder.build(nameLoader)
  def getByName(name: String): Option[Institution] = nameCache.get(name)
  def lookupByName(institutionName: String): Option[Institution] =
    sql"select * from Institution where name = ${institutionName}".first[Institution]

  /*hostName cache*/
  val hostNameLoader: CacheLoader[String, Option[Institution]] = new CacheLoader[String, Option[Institution]]() {
    override def load(hostName: String): Option[Institution] = lookupByHostName(hostName)
  }
  val hostNameCache: LoadingCache[String, Option[Institution]] = cacheBuilder.build(hostNameLoader)
  def getByHostName(hostName: String): Option[Institution] = hostNameCache.get(hostName)
  def lookupByHostName(hostName: String): Option[Institution] =
    sql"""
        | select i.* from Institution i
        | join InstitutionHostName ihn on i.uuid = ihn.institutionUUID
        | where ihn.hostName = ${hostName}
      """.first[Institution]

  def clearCache(): Unit = {
    hostNameCache.invalidateAll()
    nameCache.invalidateAll()
    uuidCache.invalidateAll()
  }

  def byType(institutionType: InstitutionType): Option[Institution] =
    sql"""
        select * from Institution where institutionType = ${institutionType.toString}
    """.first[Institution]

  def create(institution: Institution): Institution = {
    if (institution.getUUID == null) {
      institution.setUUID(UUID.randomUUID.toString)
    }
    if (institution.getActivatedAt == null) {
      institution.setActivatedAt(new Date)
    }
    sql"""
    | insert into Institution (uuid,name,terms,baseURL,demandsPersonContactDetails,validatePersonContactDetails,fullName,allowRegistration,allowRegistrationByUsername,activatedAt,skin,billingType,institutionType,dashboardVersionUUID,useEmailWhitelist,assetsRepositoryUUID,timeZone,institutionSupportEmail,advancedMode, notifyInstitutionAdmins, allowedLanguages)
    | values(
    | ${institution.getUUID},
    | ${institution.getName},
    | ${institution.getTerms},
    | ${institution.getBaseURL},
    | ${institution.isDemandsPersonContactDetails},
    | ${institution.isValidatePersonContactDetails},
    | ${institution.getFullName},
    | ${institution.isAllowRegistration},
    | ${institution.isAllowRegistrationByUsername},
    | ${institution.getActivatedAt},
    | ${institution.getSkin},
    | ${institution.getBillingType.toString},
    | ${institution.getInstitutionType.toString},
    | ${institution.getDashboardVersionUUID},
    | ${institution.isUseEmailWhitelist},
    | ${institution.getAssetsRepositoryUUID},
    | ${institution.getTimeZone},
    | ${institution.getInstitutionSupportEmail},
    | ${institution.isAdvancedMode},
    | ${institution.isNotifyInstitutionAdmins},
    | ${institution.getAllowedLanguages})""".executeUpdate

    //log creation event
    EventsRepo.logEntityChange(institution.getUUID, AuditedEntityType.institution, institution.getUUID, null, institution)

    institution
  }

  def updateCaches(i: Institution): Unit = {
    val oi = Some(i)
    uuidCache.put(i.getUUID, oi)
    nameCache.put(i.getName, oi)
  }

  def cleanUpHostNameCache(): Unit = {
    hostNameCache.cleanUp()
  }

  def updateHostNameCache(institutionUUID: String, hostName: String): Unit = {
    val oi = getByUUID(institutionUUID)
    hostNameCache.put(hostName, oi)
  }
}
