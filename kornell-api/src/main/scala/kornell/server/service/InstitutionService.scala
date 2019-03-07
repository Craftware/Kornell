package kornell.server.service

import java.util.UUID

import kornell.core.to.CreateInstitutionTO
import kornell.server.jdbc.SQL._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import kornell.core.entity.{BillingType, InstitutionType}
import kornell.server.jdbc.repository.InstitutionsRepo
import kornell.server.repository.Entities
import org.joda.time.DateTime


object InstitutionService {

  def create(createInstitutionTO: CreateInstitutionTO): Unit = {
    // load platform config
    var configString = sql""" select config from PlatformConfig limit 1""".first[String].get
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val parsedJson = mapper.readValue[Map[String, Object]](configString)

    //create institution
    var baseUrl = ""
    var institution = InstitutionsRepo.create(Entities.newInstitution(
      uuid = UUID.randomUUID.toString,
      name = createInstitutionTO.getShortName,
      fullName = createInstitutionTO.getFullName,
      terms = "",
      baseUrl = baseUrl,
      demandsPersonContactDetails = false,
      validatePersonContactDetails = false,
      allowRegistration = false,
      allowRegistrationByUsername = false,
      activatedAt = new DateTime().toDate,
      skin = "_green",
      billingType = BillingType.monthly,
      institutionType = InstitutionType.DEFAULT,
      dashboardVersionUUID = null,
      useEmailWhitelist = false,
      timeZone = "America/Sao_Paulo",
      institutionSupportEmail = "",  // add this
      advancedMode = false,
      notifyInstitutionAdmins = false,
      allowedLanguages = "pt_BR",
      disabled = false,
      enforceSequentialProgress = true,
      showPlatformPanel = false
    ))
  }
}
