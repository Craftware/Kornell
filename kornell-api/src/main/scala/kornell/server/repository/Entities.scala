package kornell.server.repository

import java.math.BigDecimal
import java.util
import java.util.{Date, UUID}

import com.google.web.bindery.autobean.vm.AutoBeanFactorySource
import kornell.core.entity._
import kornell.core.entity.role.{Role, RoleType, Roles}
import kornell.server.util.DateConverter
import org.joda.time.LocalDate

import scala.collection.JavaConverters.seqAsJavaListConverter

//TODO: Remove this class without spreading dependency on AutoBeanFactorySource
object Entities {
  val factory: EntityFactory = AutoBeanFactorySource.create(classOf[EntityFactory])

  def randUUID: String = UUID.randomUUID.toString

  def newPerson: Person = factory.newPerson.as

  def newPerson(
    uuid: String = null,
    fullName: String = null,
    lastPlaceVisited: String = null,
    email: String = null,
    company: String = null,
    title: String = null,
    sex: String = null,
    birthDate: Date = null,
    confirmation: String = null,
    telephone: String = null,
    country: String = null,
    state: String = null,
    city: String = null,
    addressLine1: String = null,
    addressLine2: String = null,
    postalCode: String = null,
    cpf: String = null,
    institutionUUID: String = null,
    termsAcceptedOn: Date = null,
    registrationType: RegistrationType = null,
    institutionRegistrationPrefixUUID: String = null,
    receiveEmailCommunication: Boolean = true,
    forcePasswordUpdate: Boolean = false): Person = {
    //in some case, we new a person and no one is authenticated
    val person = factory.newPerson.as
    person.setUUID(uuid)
    person.setFullName(fullName)
    person.setLastPlaceVisited(lastPlaceVisited)
    person.setEmail(email)
    person.setCompany(company)
    person.setTitle(title)
    person.setSex(sex)
    person.setBirthDate(new LocalDate(birthDate).toDate)
    person.setConfirmation(confirmation)
    person.setTelephone(telephone)
    person.setCountry(country)
    person.setState(state)
    person.setCity(city)
    person.setAddressLine1(addressLine1)
    person.setAddressLine2(addressLine2)
    person.setPostalCode(postalCode)
    person.setCPF(cpf)
    person.setInstitutionUUID(institutionUUID)
    person.setTermsAcceptedOn(DateConverter.convertDate(termsAcceptedOn))
    person.setRegistrationType(registrationType)
    person.setInstitutionRegistrationPrefixUUID(institutionRegistrationPrefixUUID)
    person.setReceiveEmailCommunication(receiveEmailCommunication)
    person.setForcePasswordUpdate(forcePasswordUpdate)
    person
  }

  def newPeople(people: List[Person]): People = {
    val ps = factory.newPeople.as
    ps.setPeople(people.asJava)
    ps
  }

  def newPrincipal(uuid: String, personUUID: String, username: String): Principal = {
    val principal = factory.newPrincipal.as
    principal.setUUID(uuid)
    principal.setPersonUUID(personUUID)
    principal.setUsername(username)
    principal
  }

  def newCourse(uuid: String = randUUID, code: String = null,
    name: String = null, description: String = null,
    infoJson: String = null,
    state: EntityState = null,
    institutionUUID: String = null,
    childCourse: Boolean,
    thumbUrl: String = null,
    contentSpec: ContentSpec): Course = {
    val c = factory.newCourse.as
    c.setUUID(uuid)
    c.setCode(code)
    c.setDescription(description)
    c.setName(name)
    c.setInfoJson(infoJson)
    c.setState(state)
    c.setInstitutionUUID(institutionUUID)
    c.setChildCourse(childCourse)
    c.setThumbUrl(thumbUrl)
    c.setContentSpec(contentSpec)
    c
  }

  implicit def toEnrollments(enrollments: List[Enrollment]): Enrollments = {
    val es = factory.newEnrollments.as
    es.setEnrollments(enrollments.asJava)
    es
  }

  def newRoles(roles: List[Role]): Roles = {
    val rs = factory.newRoles.as
    rs.setRoles(roles.asJava)
    rs
  }

  def newEnrollment(uuid: String = randUUID, enrolledOn: Date = null,
    courseClassUUID: String, personUUID: String,
    progress: Integer = 0, notes: String = null,
    state: EnrollmentState, lastProgressUpdate: Date = null,
    assessment: Assessment = null, lastAssessmentUpdate: Date = null,
    assessmentScore: BigDecimal = null, certifiedAt: Date = null,
    courseVersionUUID: String = null, parentEnrollmentUUID: String = null,
    startDate: Date = null, endDate: Date = null,
    preAssessment: BigDecimal = null, postAssessment: BigDecimal = null, enrollmentSource: EnrollmentSource = null): Enrollment = {
    val e = factory.enrollment.as
    e.setUUID(uuid)
    e.setCourseClassUUID(courseClassUUID)
    e.setPersonUUID(personUUID)
    e.setProgress(progress)
    e.setNotes(notes)
    e.setState(state)
    e.setAssessment(assessment)
    e.setAssessmentScore(assessmentScore)
    e.setCourseVersionUUID(courseVersionUUID)
    e.setParentEnrollmentUUID(parentEnrollmentUUID)
    e.setStartDate(startDate)
    e.setEndDate(endDate)
    e.setPreAssessmentScore(preAssessment)
    e.setPostAssessmentScore(postAssessment)
    e.setEnrollmentSource(enrollmentSource)
    e.setEnrolledOn(DateConverter.convertDate(enrolledOn))
    e.setLastProgressUpdate(DateConverter.convertDate(lastProgressUpdate))
    e.setLastAssessmentUpdate(DateConverter.convertDate(lastAssessmentUpdate))
    e.setCertifiedAt(DateConverter.convertDate(certifiedAt))
    e
  }

  def newEnrollments(enrollments: List[Enrollment]): Enrollments = {
    val ps = factory.newEnrollments.as
    ps.setEnrollments(enrollments.asJava)
    ps
  }

  //FTW: Default parameter values
  def newInstitution(uuid: String = randUUID, name: String, fullName: String, terms: String, baseURL: String,
    demandsPersonContactDetails: Boolean, validatePersonContactDetails: Boolean, allowRegistration: Boolean, allowRegistrationByUsername: Boolean,
    activatedAt: Date, skin: String, billingType: BillingType, institutionType: InstitutionType, dashboardVersionUUID: String,
    useEmailWhitelist: Boolean = false, assetsRepositoryUUID: String = null, timeZone: String, institutionSupportEmail: String, advancedMode: Boolean,
    notifyInstitutionAdmins: Boolean, allowedLanguages: String, disabled: Boolean): Institution = {
    val i = factory.newInstitution.as
    i.setName(name)
    i.setFullName(fullName)
    i.setUUID(uuid)
    if (terms != null)
      i.setTerms(terms.stripMargin)
    i.setAssetsRepositoryUUID(assetsRepositoryUUID)
    i.setBaseURL(baseURL)
    i.setDemandsPersonContactDetails(demandsPersonContactDetails)
    i.setValidatePersonContactDetails(validatePersonContactDetails)
    i.setAllowRegistration(allowRegistration)
    i.setAllowRegistrationByUsername(allowRegistrationByUsername)
    i.setActivatedAt(activatedAt)
    i.setSkin(skin)
    i.setBillingType(billingType)
    i.setInstitutionType(institutionType)
    i.setDashboardVersionUUID(dashboardVersionUUID)
    i.setUseEmailWhitelist(useEmailWhitelist)
    i.setTimeZone(timeZone)
    i.setInstitutionSupportEmail(institutionSupportEmail)
    i.setAdvancedMode(advancedMode)
    i.setNotifyInstitutionAdmins(notifyInstitutionAdmins)
    i.setAllowedLanguages(allowedLanguages)
    i.setDisabled(disabled)
    i
  }

  lazy val newUserRole: Role = {
    val role = factory.newRole().as
    role.setRoleType(RoleType.user)
    role.setUserRole(factory.newUserRole().as())
    role
  }

  def newRoleAsPlatformAdmin(personUUID: String, institutionUUID: String): Role = {
    val role = factory.newRole().as
    role.setPersonUUID(personUUID)
    val platformAdminRole = factory.newPlatformAdminRole().as
    platformAdminRole.setInstitutionUUID(institutionUUID)
    role.setRoleType(RoleType.platformAdmin)
    role.setPlatformAdminRole(platformAdminRole)
    role
  }

  def newInstitutionAdminRole(personUUID: String, institutionUUID: String): Role = {
    val role = factory.newRole().as
    role.setPersonUUID(personUUID)
    val institutionAdminRole = factory.newInstitutionAdminRole().as
    institutionAdminRole.setInstitutionUUID(institutionUUID)
    role.setRoleType(RoleType.institutionAdmin)
    role.setInstitutionAdminRole(institutionAdminRole)
    role
  }

  def newCourseClassAdminRole(personUUID: String, courseClassUUID: String): Role = {
    val role = factory.newRole().as
    role.setPersonUUID(personUUID)
    val courseClassAdminRole = factory.newCourseClassAdminRole().as
    courseClassAdminRole.setCourseClassUUID(courseClassUUID)
    role.setRoleType(RoleType.courseClassAdmin)
    role.setCourseClassAdminRole(courseClassAdminRole)
    role
  }

  def newTutorRole(personUUID: String, courseClassUUID: String): Role = {
    val role = factory.newRole().as
    role.setPersonUUID(personUUID)
    val tutorRole = factory.newTutorRole().as
    tutorRole.setCourseClassUUID(courseClassUUID)
    role.setRoleType(RoleType.tutor)
    role.setTutorRole(tutorRole)
    role
  }

  def newCourseClassObserverRole(personUUID: String, courseClassUUID: String): Role = {
    val role = factory.newRole().as
    role.setPersonUUID(personUUID)
    val courseClassObserverRole = factory.newCourseClassObserverRole().as
    courseClassObserverRole.setCourseClassUUID(courseClassUUID)
    role.setRoleType(RoleType.courseClassObserver)
    role.setCourseClassObserverRole(courseClassObserverRole)
    role
  }

  def newControlPanelAdminRole(personUUID: String): Role = {
    val role = factory.newRole().as
    role.setPersonUUID(personUUID)
    val controlPanelAdminRole = factory.newControlPanelAdminRole().as
    role.setRoleType(RoleType.controlPanelAdmin)
    role.setControlPanelAdminRole(controlPanelAdminRole)
    role
  }

  def newPublisherRole(personUUID: String, institutionUUID: String): Role = {
    val role = factory.newRole().as
    role.setPersonUUID(personUUID)
    val publisherRole = factory.newPublisherRole().as
    publisherRole.setInstitutionUUID(institutionUUID)
    role.setRoleType(RoleType.publisher)
    role.setPublisherRole(publisherRole)
    role
  }

  def newCourseVersion(
    uuid: String = randUUID, name: String = null,
    courseUUID: String = null, versionCreatedAt: Date = new Date, distributionPrefix: String = null,
    state: EntityState = null, disabled: Boolean = false, parentVersionUUID: String = null,
    instanceCount: Integer = 1, classroomJson: String = null, classroomJsonPublished: String = null,
    label: String = null, thumbUrl: String = null): CourseVersion = {
    val version = factory.newCourseVersion.as
    version.setUUID(uuid)
    version.setName(name)
    version.setCourseUUID(courseUUID)
    version.setVersionCreatedAt(DateConverter.convertDate(versionCreatedAt))
    version.setDistributionPrefix(distributionPrefix)
    version.setState(state)
    version.setDisabled(disabled)
    version.setParentVersionUUID(parentVersionUUID)
    version.setInstanceCount(instanceCount)
    version.setClassroomJson(classroomJson)
    version.setClassroomJsonPublished(classroomJsonPublished)
    version.setLabel(label)
    version.setThumbUrl(thumbUrl)
    version
  }

  def newCourseClass(uuid: String = null, name: String = null,
    courseVersionUUID: String = null, institutionUUID: String = null,
    requiredScore: BigDecimal = null, publicClass: Boolean = false,
    overrideEnrollments: Boolean = false,
    invisible: Boolean = false, maxEnrollments: Integer = null,
    createdAt: Date = null, createdBy: String = null,
    state: EntityState = null,
    registrationType: RegistrationType = null,
    institutionRegistrationPrefixUUID: String = null,
    courseClassChatEnabled: Boolean = false,
    chatDockEnabled: Boolean = false,
    allowBatchCancellation: Boolean = false,
    tutorChatEnabled: Boolean = false,
    approveEnrollmentsAutomatically: Boolean = false,
    startDate: Date = null, ecommerceIdentifier: String = null,
    thumbUrl: String = null, sandbox: Boolean = false, enrollmentExpiryDays: Integer = null): CourseClass = {
    val clazz = factory.newCourseClass.as
    clazz.setUUID(uuid)
    clazz.setName(name)
    clazz.setCourseVersionUUID(courseVersionUUID)
    clazz.setInstitutionUUID(institutionUUID)
    clazz.setRequiredScore(requiredScore)
    clazz.setPublicClass(publicClass)
    clazz.setOverrideEnrollments(overrideEnrollments)
    clazz.setInvisible(invisible)
    clazz.setMaxEnrollments(maxEnrollments)
    clazz.setCreatedAt(DateConverter.convertDate(createdAt))
    clazz.setCreatedBy(createdBy)
    clazz.setState(state)
    clazz.setRegistrationType(registrationType)
    clazz.setInstitutionRegistrationPrefixUUID(institutionRegistrationPrefixUUID)
    clazz.setCourseClassChatEnabled(courseClassChatEnabled)
    clazz.setChatDockEnabled(chatDockEnabled)
    clazz.setAllowBatchCancellation(allowBatchCancellation)
    clazz.setTutorChatEnabled(tutorChatEnabled)
    clazz.setApproveEnrollmentsAutomatically(approveEnrollmentsAutomatically)
    clazz.setStartDate(startDate)
    clazz.setEcommerceIdentifier(ecommerceIdentifier)
    clazz.setThumbUrl(thumbUrl)
    clazz.setSandbox(sandbox)
    clazz.setEnrollmentExpiryDays(enrollmentExpiryDays)
    clazz
  }

  def newActomEntries(enrollmentUUID: String, actomKey: String, entriesMap: util.Map[String, String]): ActomEntries = {
    val entries = factory.newActomEntries.as
    entries.setActomKey(actomKey)
    entries.setEnrollmentUUID(enrollmentUUID)
    entries.setEntries(entriesMap)
    entries
  }

  def newContentRepository(uuid: String = null,
    repositoryType: RepositoryType = null,
    accessKeyId: String = null,
    secretAccessKey: String = null,
    bucketName: String = null,
    prefix: String = null,
    region: String = null,
    institutionUUID: String = null,
    path: String = null,
    accountName: String = null,
    accountKey: String = null,
    container: String = null): ContentRepository = {
    val repo = factory.newContentRepository.as
    repo.setUUID(uuid)
    repo.setRepositoryType(repositoryType)
    repo.setAccessKeyId(accessKeyId)
    repo.setBucketName(bucketName)
    repo.setPrefix(prefix)
    repo.setRegion(region)
    repo.setSecretAccessKey(secretAccessKey)
    repo.setInstitutionUUID(institutionUUID)
    repo.setPath(path)
    repo.setAccountName(accountName)
    repo.setAccountKey(accountKey)
    repo.setContainer(container)
    repo
  }

  def newChatThread(uuid: String = null, createdAt: Date = null, institutionUUID: String = null, courseClassUUID: String = null, personUUID: String = null, threadType: String = null, active: Boolean = true, lastSentAt: Date = null): ChatThread = {
    val chatThread = factory.newChatThread.as
    chatThread.setUUID(uuid)
    chatThread.setCreatedAt(DateConverter.convertDate(createdAt))
    chatThread.setInstitutionUUID(institutionUUID)
    chatThread.setCourseClassUUID(courseClassUUID)
    chatThread.setPersonUUID(personUUID)
    chatThread.setThreadType(threadType)
    chatThread.setActive(active)
    chatThread.setLastSentAt(lastSentAt)
    chatThread
  }

  def newChatThreadParticipant(uuid: String = null, chatThreadUUID: String = null, personUUID: String = null,
    lastReadAt: Date = null, active: Boolean = false, lastJoinDate: Date = null, unreadCount: Integer = 0): ChatThreadParticipant = {
    val chatThreadParticipant = factory.newChatThreadParticipant.as
    chatThreadParticipant.setUUID(uuid)
    chatThreadParticipant.setThreadUUID(chatThreadUUID)
    chatThreadParticipant.setPersonUUID(personUUID)
    chatThreadParticipant.setLastReadAt(DateConverter.convertDate(lastReadAt))
    chatThreadParticipant.setActive(active)
    chatThreadParticipant.setLastJoinDate(DateConverter.convertDate(lastJoinDate))
    chatThreadParticipant.setUnreadCount(unreadCount)
    chatThreadParticipant
  }

  def newInstitutionRegistrationPrefix(uuid: String, name: String, institutionUUID: String = null, showEmailOnProfile: Boolean, showCPFOnProfile: Boolean, showContactInformationOnProfile: Boolean): InstitutionRegistrationPrefix = {
    val institutionRegistrationPrefix = factory.newInstitutionRegistrationPrefix.as
    institutionRegistrationPrefix.setUUID(uuid)
    institutionRegistrationPrefix.setName(name)
    institutionRegistrationPrefix.setInstitutionUUID(institutionUUID)
    institutionRegistrationPrefix.setShowEmailOnProfile(showEmailOnProfile)
    institutionRegistrationPrefix.setShowCPFOnProfile(showCPFOnProfile)
    institutionRegistrationPrefix.setShowContactInformationOnProfile(showContactInformationOnProfile)
    institutionRegistrationPrefix
  }

  def newEnrollmentsEntries(): EnrollmentsEntries = {
    val esEntries = factory.newEnrollmentsEntries().as
    esEntries.setEnrollmentEntriesMap(new util.HashMap[String, EnrollmentEntries]())
    esEntries
  }

  def newEnrollmentEntries: EnrollmentEntries = {
    val eEntries = factory.newEnrollmentEntries.as
    eEntries.setActomEntriesMap(new util.HashMap[String, ActomEntries]())
    eEntries
  }

  def newCourseDetailsHint(uuid: String, title: String, text: String, entityType: CourseDetailsEntityType, entityUUID: String, index: Integer, fontAwesomeClassName: String): CourseDetailsHint = {
    val hint = factory.newCourseDetailsHint.as
    hint.setUUID(uuid)
    hint.setTitle(title)
    hint.setText(text)
    hint.setEntityType(entityType)
    hint.setEntityUUID(entityUUID)
    hint.setIndex(index)
    hint.setFontAwesomeClassName(fontAwesomeClassName)
    hint
  }

  def newCourseDetailsLibrary(uuid: String, title: String, description: String, entityType: CourseDetailsEntityType, entityUUID: String, index: Integer, size: Integer, path: String, uploadDate: Date, fontAwesomeClassName: String): CourseDetailsLibrary = {
    val library = factory.newCourseDetailsLibrary.as
    library.setUUID(uuid)
    library.setTitle(title)
    library.setDescription(description)
    library.setEntityType(entityType)
    library.setEntityUUID(entityUUID)
    library.setIndex(index)
    library.setFontAwesomeClassName(fontAwesomeClassName)
    library.setSize(size)
    library.setPath(path)
    library.setUploadDate(uploadDate)
    library
  }

  def newCourseDetailsSection(uuid: String, title: String, text: String, entityType: CourseDetailsEntityType, entityUUID: String, index: Integer): CourseDetailsSection = {
    val section = factory.newCourseDetailsSection.as
    section.setUUID(uuid)
    section.setTitle(title)
    section.setText(text)
    section.setEntityType(entityType)
    section.setEntityUUID(entityUUID)
    section.setIndex(index)
    section
  }

  def newCertificateDetails(uuid: String, bgImage: String, certificateType: CertificateType, entityType: CourseDetailsEntityType, entityUUID: String): CertificateDetails = {
    val certificateDetails = factory.newCertificateDetails().as
    certificateDetails.setUUID(uuid)
    certificateDetails.setBgImage(bgImage)
    certificateDetails.setCertificateType(certificateType)
    certificateDetails.setEntityType(entityType)
    certificateDetails.setEntityUUID(entityUUID)
    certificateDetails
  }

  def newPostbackConfig(uuid: String, instutionUUID: String, postbackType: PostbackType, contents: String): PostbackConfig = {
    val config = factory.newPostbackConfig.as
    config.setUUID(uuid)
    config.setInstitutionUUID(uuid)
    config.setPostbackType(postbackType)
    config.setContents(contents)
    config
  }

  def newEmailTemplate(uuid: String, templateType: EmailTemplateType, locale: String, title: String, template: String): EmailTemplate = {
    val emailTemplate = factory.newEmailTemplate.as
    emailTemplate.setUUID(uuid)
    emailTemplate.setTemplateType(templateType)
    emailTemplate.setLocale(locale)
    emailTemplate.setTitle(title)
    emailTemplate.setTemplate(template)
    emailTemplate
  }

  def newTrack(uuid: String, institutionUUID: String, name: String): Track = {
    val track = factory.newTrack.as
    track.setUUID(uuid)
    track.setInstitutionUUID(institutionUUID)
    track.setName(name)
    track
  }

  def newTrackEnrollment(uuid: String, personUUID: String, trackUUID: String): TrackEnrollment = {
    val trackEnrollment = factory.newTrackEnrollment.as
    trackEnrollment.setUUID(uuid)
    trackEnrollment.setPersonUUID(personUUID)
    trackEnrollment.setTrackUUID(trackUUID)
    trackEnrollment
  }

  def newTrackItem(uuid: String, courseVersionUUID: String, trackUUID: String, parentUUID: String, order: Integer, havingPreRequirements: Boolean, startDate: Date): TrackItem = {
    val trackItem = factory.newTrackItem.as
    trackItem.setUUID(uuid)
    trackItem.setCourseVersionUUID(courseVersionUUID)
    trackItem.setTrackUUID(trackUUID)
    trackItem.setParentUUID(parentUUID)
    trackItem.setOrder(order)
    trackItem.setHavingPreRequirements(havingPreRequirements)
    trackItem.setStartDate(startDate)
    trackItem
  }
}
