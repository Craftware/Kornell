package kornell.server.jdbc.repository

import kornell.core.entity.{AuditedEntityType, Institution, InstitutionRegistrationPrefix}
import kornell.core.to.InstitutionRegistrationPrefixesTO
import kornell.server.jdbc.SQL._
import kornell.server.repository.TOs

class InstitutionRepo(uuid: String) {

  def get: Institution = InstitutionsRepo.getByUUID(uuid).get

  def update(institution: Institution): Institution = {
    //get previous version
    val oldInstitution = InstitutionsRepo.getByUUID(institution.getUUID).get

    sql"""
    | update Institution i
    | set i.name = ${institution.getName},
    | i.fullName = ${institution.getFullName},
    | i.terms = ${institution.getTerms},
    | i.baseURL = ${institution.getBaseURL},
    | i.demandsPersonContactDetails = ${institution.isDemandsPersonContactDetails},
    | i.validatePersonContactDetails = ${institution.isValidatePersonContactDetails},
    | i.allowRegistration = ${institution.isAllowRegistration},
    | i.allowRegistrationByUsername = ${institution.isAllowRegistrationByUsername},
    | i.activatedAt = ${institution.getActivatedAt},
    | i.skin = ${institution.getSkin},
    | i.billingType = ${institution.getBillingType.toString},
    | i.institutionType = ${institution.getInstitutionType.toString},
    | i.dashboardVersionUUID = ${institution.getDashboardVersionUUID},
    | i.useEmailWhitelist = ${institution.isUseEmailWhitelist},
    | i.timeZone = ${institution.getTimeZone},
    | i.institutionSupportEmail = ${institution.getInstitutionSupportEmail},
    | i.advancedMode = ${institution.isAdvancedMode},
    | i.notifyInstitutionAdmins = ${institution.isNotifyInstitutionAdmins},
    | i.allowedLanguages = ${institution.getAllowedLanguages}
    | where i.uuid = ${institution.getUUID}""".executeUpdate

    //log entity change
    EventsRepo.logEntityChange(institution.getUUID, AuditedEntityType.institution, institution.getUUID, oldInstitution, institution)

    InstitutionsRepo.updateCaches(institution)

    InstitutionsRepo.cleanUpHostNameCache()

    institution
  }

  def getInstitutionRegistrationPrefixes: InstitutionRegistrationPrefixesTO = {
    TOs.newInstitutionRegistrationPrefixesTO(sql"""
      | select * from InstitutionRegistrationPrefix
      | where institutionUUID = ${uuid}
          """.map[InstitutionRegistrationPrefix](toInstitutionRegistrationPrefix))
  }

}

object InstitutionRepo {
  def apply(uuid: String) = new InstitutionRepo(uuid)
}
