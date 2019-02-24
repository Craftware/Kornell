package kornell.gui.client;

import com.google.gwt.i18n.client.ConstantsWithLookup;

public interface KornellConstants extends ConstantsWithLookup {

    @DefaultStringValue("Loading...")
    String loading();

    @DefaultStringValue("Yes")
    String yes();

    @DefaultStringValue("No")
    String no();

    @DefaultStringValue("A new version of the site is available.")
    String newVersionAvailable();

    @DefaultStringValue("Refresh")
    String refresh();

    @DefaultStringValue(" · ")
    String nameSeparator();

    /**
     * GenericClientFactoryImpl
     */

    @DefaultStringValue("Institution not found.")
    String institutionNotFound();

    @DefaultStringValue("We are currently under maintenance. Please check again later. If the problem persists, contact us at suporte@eduvem.com. Thanks for your patience.")
    String maintenanceMessage();

    /**
     * VitrinePlace
     */

    @DefaultStringValue("Select a language")
    String selectLanguage();

    @DefaultStringValue("Use the same email in which you receive the registration.")
    String registrationEmailMessage();

    @DefaultStringValue("Invalid username or password, please try again.")
    String badUsernamePassword();

    @DefaultStringValue("Your name must be at least 2 characters long.")
    String nameTooShort();

    @DefaultStringValue("Invalid email address.")
    String invalidEmail();

    @DefaultStringValue("Your password must be at least 6 characters long")
    String invalidPasswordTooShort();

    @DefaultStringValue("Your password contains invalid characters.")
    String invalidPasswordBadChar();

    @DefaultStringValue("Your passwords don't match.")
    String passwordMismatch();

    @DefaultStringValue("User created successfully.")
    String userCreated();

    @DefaultStringValue("Email address already exists.")
    String emailExists();

    @DefaultStringValue("Request completed. Please check your email.")
    String requestPasswordReset();

    @DefaultStringValue("The request was not successful. Please check that the email was entered correctly.")
    String requestPasswordResetError();

    @DefaultStringValue("Password changed successfully.")
    String passwordChangeComplete();

    @DefaultStringValue("We could not update your password. Please check your email or make another request.")
    String passwordChangeError();

    /**
     *
     * GenericWelcomeView
     *
     */
    @DefaultStringValue("All")
    String allClasses();

    @DefaultStringValue("Finished")
    String finished();

    @DefaultStringValue("To start")
    String toStart();

    @DefaultStringValue("In progress")
    String inProgress();

    @DefaultStringValue("Available")
    String available();

    @DefaultStringValue("Sandbox")
    String sandbox();

    @DefaultStringValue("Content")
    String homeTitle();

    @DefaultStringValue("Select below")
    String homeDescription();

    @DefaultStringValue("You are not enrolled in a class and there are no classes available to request a new registration.")
    String noClassesAvailable();

    /**
     *
     * GenericCourseDetailsView
     *
     */
    @DefaultStringValue("Course details: ")
    String detailsHeader();

    @DefaultStringValue("About")
    String about();

    @DefaultStringValue("About the course")
    String btnAbout();

    @DefaultStringValue("Topics")
    String btnTopics();

    @DefaultStringValue("Certification")
    String btnCertification();

    @DefaultStringValue("Chat")
    String btnChat();

    @DefaultStringValue("Tutoring")
    String btnTutor();

    @DefaultStringValue("Library")
    String btnLibrary();

    @DefaultStringValue("General view")
    String btnAboutInfo();

    @DefaultStringValue("Main topics covered on this course")
    String btnTopicsInfo();

    @DefaultStringValue("Evaluations and tests")
    String btnCertificationInfo();

    @DefaultStringValue("Close Details")
    String closeDetails();

    @DefaultStringValue("Topic")
    String topic();

    @DefaultStringValue("Certification")
    String certification();

    @DefaultStringValue("You can generate the certificate when you finish the course.")
    String certificationInfoText();

    @DefaultStringValue("Info")
    String certificationTableInfo();

    @DefaultStringValue("Status")
    String certificationTableStatus();

    @DefaultStringValue("Grade")
    String certificationTableGrade();

    @DefaultStringValue("Actions")
    String certificationTableActions();

    @DefaultStringValue("Print certificate")
    String printCertificateButton();

    @DefaultStringValue("With all the participants of this class")
    String classChatButton();

    @DefaultStringValue("With a specialist")
    String tutorChatButton();

    @DefaultStringValue("Supplementary material")
    String libraryButton();

    @DefaultStringValue("Go to class")
    String goToClassButton();

    @DefaultStringValue("Go to version")
    String goToVersionButton();

    @DefaultStringValue("This class has been disabled by the institution.<br><br> The material in this class is inaccessible.<br>")
    String inactiveCourseClass();

    @DefaultStringValue("Your registration was canceled by the institution.<br><br> The material in this class is inaccessible.<br>")
    String cancelledEnrollment();

    @DefaultStringValue("Your registration has expired. Get in touch with support if you with to extend it.<br><br> The material in this class is inaccessible.<br>")
    String expiredEnrollment();

    @DefaultStringValue("Ainda não existe conteúdo publicado para este grupo.<br><br> Entre em contato com o suporte clicando na interrogação no menu.<br>")
    String noPublishedContentForClassroom();

    @DefaultStringValue("Your registration has not yet been approved by the institution.")
    String enrollmentNotApproved();

    @DefaultStringValue("You will receive an email at the time of approval.")
    String enrollmentConfirmationEmail();

    @DefaultStringValue("Description")
    String description();

    /**
     *
     * GenericBarView
     */

    @DefaultStringValue("course")
    String course();

    @DefaultStringValue("details")
    String details();

    @DefaultStringValue("library")
    String library();

    @DefaultStringValue("forum")
    String forum();

    @DefaultStringValue("chat")
    String chat();

    @DefaultStringValue("specialists")
    String specialists();

    @DefaultStringValue("notes")
    String notes();

    @DefaultStringValue("back")
    String back();

    @DefaultStringValue("next")
    String next();

    @DefaultStringValue("previous")
    String previous();

    @DefaultStringValue("institution")
    String institution();

    @DefaultStringValue("courses")
    String courses();

    @DefaultStringValue("versions")
    String versions();

    @DefaultStringValue("classes")
    String classes();

    /**
     * GenericActivityBarView
     */
    @DefaultStringValue("completed")
    String completed();

    @DefaultStringValue("Page")
    String pageForPagination();

    /**
     * ClassroomPresenter
     */
    @DefaultStringValue("Loading the course...")
    String loadingTheCourse();

    @DefaultStringValue("This class was set as invisible by the institution.")
    String classSetAsInvisible();

    /**
     * GenericCertificationItemVIew
     */
    @DefaultStringValue("Certificate")
    String certificateName();

    @DefaultStringValue("Generate")
    String generate();

    @DefaultStringValue("Wait a minute...")
    String waitAMinute();

    @DefaultStringValue("Available")
    String certificateAvailable();

    @DefaultStringValue("Unavailable")
    String certificateNotAvailable();

    /**
     * GenericCourseLibraryView
     */
    @DefaultStringValue("Library")
    String libraryTitle();

    @DefaultStringValue("Here you can find the supplementary material for the course.")
    String libraryInfo();

    @DefaultStringValue("Type")
    String libraryEntryIcon();

    @DefaultStringValue("File name")
    String libraryEntryName();

    @DefaultStringValue("Size")
    String libraryEntrySize();

    @DefaultStringValue("Publication date")
    String libraryEntryDate();

    /**
     * GenericIncludeFileView
     */
    @DefaultStringValue("--TODO--")
    String fileFormInfoTitle();

    @DefaultStringValue("--TODO--")
    String fileFormInfoText();

    @DefaultStringValue("--TODO--")
    String fileDescription();

    @DefaultStringValue("Relevance:")
    String starsLabelText();

    @DefaultStringValue("Publish")
    String btnPublish();

    /**
     * NotesPopup
     */
    @DefaultStringValue("< Use this space to make your notes during the course. >")
    String notesPopupPlaceholder();

    /**
     * MessagePresenter
     */
    @DefaultStringValue("Messages")
    String messagesTitle();

    @DefaultStringValue("Keep track of your conversations with other platform participants")
    String messagesDescription();

    @DefaultStringValue("You have no established conversations.")
    String noThreadsMessage();

    @DefaultStringValue("Enter your question here and a tutor will contact you soon.")
    String tutorPlaceholderMessage();

    /**
     * GenericMessageComposeView
     */
    @DefaultStringValue("Institution admin")
    String institutionAdminLabel();

    @DefaultStringValue("Platform admin")
    String platformAdminLabel();

    /**
     * MessageComposePresenter
     */
    @DefaultStringValue("Message sent successfully!")
    String messageSentSuccess();

    @DefaultStringValue("Please fill the message body.")
    String noMessageBodyError();

    /**
     * GenericMessageView
     */
    @DefaultStringValue("Filter conversations...")
    String filterConversationPlaceholder();

    @DefaultStringValue("Global class chat:")
    String courseClassChatThreadLabel();

    @DefaultStringValue("Direct chat with:")
    String directChatLabel();

    @DefaultStringValue("Support chat:")
    String supportChatThreadLabel();

    @DefaultStringValue("Support")
    String supportLabel();

    @DefaultStringValue("Tutor chat:")
    String tutorChatThreadLabel();

    @DefaultStringValue("Tutor")
    String tutorLabel();

    @DefaultStringValue("Institution support:")
    String institutionSupportChatThreadLabel();

    @DefaultStringValue("Platform support:")
    String platformSupportChatThreadLabel();

    /**
     * GenericPasswordChangeView
     */
    @DefaultStringValue("New Password")
    String newPassword();

    @DefaultStringValue("Confirm Password")
    String confirmPassword();

    @DefaultStringValue("Password change successful!")
    String confirmPasswordChange();

    /**
     * GenericProfileView
     */
    @DefaultStringValue("Edit")
    String editButton();

    @DefaultStringValue("Close")
    String closeButton();

    @DefaultStringValue("Save")
    String saveButton();

    @DefaultStringValue("Cancel")
    String cancelButton();

    @DefaultStringValue("OK")
    String okButton();

    @DefaultStringValue("Change password")
    String changePasswordButton();

    @DefaultStringValue("Send message")
    String sendMessageButton();

    @DefaultStringValue("Profile")
    String profileTitle();

    @DefaultStringValue("Keep your details updated")
    String profileDescription();

    @DefaultStringValue("Enter a name.")
    String missingNameMessage();

    @DefaultStringValue("Enter a telephone number.")
    String missingTelephoneMessage();

    @DefaultStringValue("Enter a country.")
    String missingCountryMessage();

    @DefaultStringValue("Select a state.")
    String selectStateMessage();

    @DefaultStringValue("Enter a state.")
    String missingStateMessage();

    @DefaultStringValue("Enter a city.")
    String missingCityMessage();

    @DefaultStringValue("Enter an address.")
    String missingAddressMessage();

    @DefaultStringValue("Enter a postal code.")
    String missingPostalCodeMessage();

    @DefaultStringValue("Successfully saved changes!")
    String confirmSaveProfile();

    @DefaultStringValue("Errors when saving user.")
    String errorSaveProfile();

    @DefaultStringValue("There are errors with your entries.")
    String formContainsErrors();

    @DefaultStringValue("User not found.")
    String userNotFound();

    @DefaultStringValue("Please complete registration")
    String pleaseCompleteRegistrationMessage();

    @DefaultStringValue("Username")
    String usernameLabel();

    @DefaultStringValue("Full name")
    String fullnameLabel();

    @DefaultStringValue("Email")
    String emailLabel();

    @DefaultStringValue("CPF")
    String cpfLabel();

    @DefaultStringValue("Company")
    String companyLabel();

    @DefaultStringValue("Position")
    String posititonLabel();

    @DefaultStringValue("Gender")
    String genderLabel();

    @DefaultStringValue("Date of birth")
    String birthDateLabel();

    @DefaultStringValue("Receive email communication")
    String receiveEmailCommunicationLabel();

    @DefaultStringValue("Telephone")
    String telephoneLabel();

    @DefaultStringValue("Country")
    String countryLabel();

    @DefaultStringValue("State")
    String stateLabel();

    @DefaultStringValue("City")
    String cityLabel();

    @DefaultStringValue("Address")
    String address1Label();

    @DefaultStringValue("Suite/App")
    String address2Label();

    @DefaultStringValue("Postal code")
    String postalCodeLabel();

    /**
     * GenericTermsView
     */
    @DefaultStringValue("Accept")
    String agreeTerms();

    @DefaultStringValue("Refuse")
    String refuseTerms();

    @DefaultStringValue("Terms of Use")
    String termsTitle();

    @DefaultStringValue("Read and sign the terms of use before proceeding")
    String termsDescription();

    /**
     * GenericCourseSummaryView
     */
    @DefaultStringValue("(CANCELLED)")
    String cancelledClassLabel();

    @DefaultStringValue("(Expired on:")
    String expiredClassLabel();

    @DefaultStringValue("(Expires on:")
    String expiresClassLabel();

    @DefaultStringValue("Available")
    String availableClassLabel();

    @DefaultStringValue("Awaiting grade")
    String pendingGradeLabel();

    @DefaultStringValue("Awaiting enrollment approval")
    String pendingEnrollmentApproval();

    @DefaultStringValue("Start class")
    String startCourseLabel();

    @DefaultStringValue("Request enrollment")
    String requestEnrollmentLabel();

    @DefaultStringValue("Inactive class")
    String inactiveClassLabel();

    @DefaultStringValue("Grade")
    String completedCourseGradeLabel();

    @DefaultStringValue("on")
    String completedOnToken();

    /**
     * Captain
     */
    @DefaultStringValue("Are you sure you wish to leave the classroom? Your progress since last save may be lost.")
    String leavingTheClassroom();

    /**
     * Util
     */
    @DefaultStringValue("January")
    String january();

    @DefaultStringValue("February")
    String february();

    @DefaultStringValue("March")
    String march();

    @DefaultStringValue("April")
    String april();

    @DefaultStringValue("May")
    String may();

    @DefaultStringValue("June")
    String june();

    @DefaultStringValue("July")
    String july();

    @DefaultStringValue("August")
    String august();

    @DefaultStringValue("September")
    String september();

    @DefaultStringValue("October")
    String october();

    @DefaultStringValue("November")
    String november();

    @DefaultStringValue("December")
    String december();

    /**
     * Message
     */
    @DefaultStringValue("Help")
    String composeTitle();

    @DefaultStringValue("Leave your questions or suggestions here.")
    String composeSubTitle();

    @DefaultStringValue("en/user-guide.pdf")
    String helpFileName();

    @DefaultStringValue("User Guide")
    String helpFileCaption();

    @DefaultStringValue("Class")
    String courseClassAdmin();

    @DefaultStringValue("Institution")
    String institutionAdmin();

    @DefaultStringValue("Recipient:")
    String recipient();

    @DefaultStringValue("Message:")
    String message();

    @DefaultStringValue("Back")
    String backButton();

    /**
     * Validations
     */
    @DefaultStringValue("Invalid CPF")
    String invalidCPF();

    @DefaultStringValue("CPF already exists")
    String existingCPF();

    @DefaultStringValue("Email already exists")
    String existingEmail();

    /**
     * FormHelper
     */
    @DefaultStringValue("Select:")
    String selectboxDefault();

    @DefaultStringValue("Female")
    String genderFemale();

    @DefaultStringValue("Male")
    String genderMale();

    /**
     * EntityState
     */
    @DefaultStringValue("Active")
    String EntityState_active();

    @DefaultStringValue("Inactive")
    String EntityState_inactive();

    @DefaultStringValue("Deleted")
    String EntityState_deleted();

    /**
     * EnrollmentState
     */
    @DefaultStringValue("notEnrolled")
    String EnrollmentState_notEnrolled();

    @DefaultStringValue("enrolled")
    String EnrollmentState_enrolled();

    @DefaultStringValue("requested")
    String EnrollmentState_requested();

    @DefaultStringValue("denied")
    String EnrollmentState_denied();

    @DefaultStringValue("cancelled")
    String EnrollmentState_cancelled();

    /**
     * EnrollmentProgressDescription
     */
    @DefaultStringValue("Not started")
    String EnrollmentProgressDescription_notStarted();

    @DefaultStringValue("In progress")
    String EnrollmentProgressDescription_inProgress();

    @DefaultStringValue("Completed")
    String EnrollmentProgressDescription_completed();

    /**
     * RegistrationType
     */
    @DefaultStringValue("Email")
    String RegistrationType_email();

    @DefaultStringValue("CPF")
    String RegistrationType_cpf();

    @DefaultStringValue("Username")
    String RegistrationType_username();

    /**
     * RoleType
     */
    @DefaultStringValue("Participant")
    String RoleType_user();

    @DefaultStringValue("Course Class Admin")
    String RoleType_courseClassAdmin();

    @DefaultStringValue("Institution Admin")
    String RoleType_institutionAdmin();

    @DefaultStringValue("Platform Admin")
    String RoleType_platformAdmin();

    @DefaultStringValue("Tutor")
    String RoleType_tutor();

    @DefaultStringValue("Course Class Observer")
    String RoleType_courseClassObserver();

    /**
     * Country
     */
    @DefaultStringValue("Andorra")
    String Country_AD();

    @DefaultStringValue("United Arab Emirates")
    String Country_AE();

    @DefaultStringValue("Afghanistan")
    String Country_AF();

    @DefaultStringValue("Antigua and Barbuda")
    String Country_AG();

    @DefaultStringValue("Anguilla")
    String Country_AI();

    @DefaultStringValue("Albania")
    String Country_AL();

    @DefaultStringValue("Armenia")
    String Country_AM();

    @DefaultStringValue("Netherlands Antilles")
    String Country_AN();

    @DefaultStringValue("Angola")
    String Country_AO();

    @DefaultStringValue("Antarctica")
    String Country_AQ();

    @DefaultStringValue("Argentina")
    String Country_AR();

    @DefaultStringValue("American Samoa")
    String Country_AS();

    @DefaultStringValue("Austria")
    String Country_AT();

    @DefaultStringValue("Australia")
    String Country_AU();

    @DefaultStringValue("Aruba")
    String Country_AW();

    @DefaultStringValue("Aland Islands")
    String Country_AX();

    @DefaultStringValue("Azerbaijan")
    String Country_AZ();

    @DefaultStringValue("Bosnia and Herzegovina")
    String Country_BA();

    @DefaultStringValue("Barbados")
    String Country_BB();

    @DefaultStringValue("Bangladesh")
    String Country_BD();

    @DefaultStringValue("Belgium")
    String Country_BE();

    @DefaultStringValue("Burkina Faso")
    String Country_BF();

    @DefaultStringValue("Bulgaria")
    String Country_BG();

    @DefaultStringValue("Bahrain")
    String Country_BH();

    @DefaultStringValue("Burundi")
    String Country_BI();

    @DefaultStringValue("Benin")
    String Country_BJ();

    @DefaultStringValue("Saint-Barthélemy")
    String Country_BL();

    @DefaultStringValue("Bermuda")
    String Country_BM();

    @DefaultStringValue("Brunei Darussalam")
    String Country_BN();

    @DefaultStringValue("Bolivia")
    String Country_BO();

    @DefaultStringValue("Brazil")
    String Country_BR();

    @DefaultStringValue("Bahamas")
    String Country_BS();

    @DefaultStringValue("Bhutan")
    String Country_BT();

    @DefaultStringValue("Bouvet Island")
    String Country_BV();

    @DefaultStringValue("Botswana")
    String Country_BW();

    @DefaultStringValue("Belarus")
    String Country_BY();

    @DefaultStringValue("Belize")
    String Country_BZ();

    @DefaultStringValue("Canada")
    String Country_CA();

    @DefaultStringValue("Cocos Keeling Islands")
    String Country_CC();

    @DefaultStringValue("Congo Kinshasa")
    String Country_CD();

    @DefaultStringValue("Central African Republic")
    String Country_CF();

    @DefaultStringValue("Congo Brazzaville")
    String Country_CG();

    @DefaultStringValue("Switzerland")
    String Country_CH();

    @DefaultStringValue("Côte d'Ivoire")
    String Country_CI();

    @DefaultStringValue("Cook Islands")
    String Country_CK();

    @DefaultStringValue("Chile")
    String Country_CL();

    @DefaultStringValue("Cameroon")
    String Country_CM();

    @DefaultStringValue("China")
    String Country_CN();

    @DefaultStringValue("Colombia")
    String Country_CO();

    @DefaultStringValue("Costa Rica")
    String Country_CR();

    @DefaultStringValue("Cuba")
    String Country_CU();

    @DefaultStringValue("Cape Verde")
    String Country_CV();

    @DefaultStringValue("Christmas Island")
    String Country_CX();

    @DefaultStringValue("Cyprus")
    String Country_CY();

    @DefaultStringValue("Czech Republic")
    String Country_CZ();

    @DefaultStringValue("Germany")
    String Country_DE();

    @DefaultStringValue("Djibouti")
    String Country_DJ();

    @DefaultStringValue("Denmark")
    String Country_DK();

    @DefaultStringValue("Dominica")
    String Country_DM();

    @DefaultStringValue("Dominican Republic")
    String Country_DO();

    @DefaultStringValue("Algeria")
    String Country_DZ();

    @DefaultStringValue("Ecuador")
    String Country_EC();

    @DefaultStringValue("Estonia")
    String Country_EE();

    @DefaultStringValue("Egypt")
    String Country_EG();

    @DefaultStringValue("Western Sahara")
    String Country_EH();

    @DefaultStringValue("Eritrea")
    String Country_ER();

    @DefaultStringValue("Spain")
    String Country_ES();

    @DefaultStringValue("Ethiopia")
    String Country_ET();

    @DefaultStringValue("Finland")
    String Country_FI();

    @DefaultStringValue("Fiji")
    String Country_FJ();

    @DefaultStringValue("Falkland Islands Malvinas")
    String Country_FK();

    @DefaultStringValue("Micronesia Federated States of")
    String Country_FM();

    @DefaultStringValue("Faroe Islands")
    String Country_FO();

    @DefaultStringValue("France")
    String Country_FR();

    @DefaultStringValue("Gabon")
    String Country_GA();

    @DefaultStringValue("United Kingdom")
    String Country_GB();

    @DefaultStringValue("Grenada")
    String Country_GD();

    @DefaultStringValue("Georgia")
    String Country_GE();

    @DefaultStringValue("French Guiana")
    String Country_GF();

    @DefaultStringValue("Guernsey")
    String Country_GG();

    @DefaultStringValue("Ghana")
    String Country_GH();

    @DefaultStringValue("Gibraltar")
    String Country_GI();

    @DefaultStringValue("Greenland")
    String Country_GL();

    @DefaultStringValue("Gambia")
    String Country_GM();

    @DefaultStringValue("Guinea")
    String Country_GN();

    @DefaultStringValue("Guadeloupe")
    String Country_GP();

    @DefaultStringValue("Equatorial Guinea")
    String Country_GQ();

    @DefaultStringValue("Greece")
    String Country_GR();

    @DefaultStringValue("South Georgia and the South Sandwich Islands")
    String Country_GS();

    @DefaultStringValue("Guatemala")
    String Country_GT();

    @DefaultStringValue("Guam")
    String Country_GU();

    @DefaultStringValue("Guinea-Bissau")
    String Country_GW();

    @DefaultStringValue("Guyana")
    String Country_GY();

    @DefaultStringValue("Hong Kong SAR China")
    String Country_HK();

    @DefaultStringValue("Heard and Mcdonald Islands")
    String Country_HM();

    @DefaultStringValue("Honduras")
    String Country_HN();

    @DefaultStringValue("Croatia")
    String Country_HR();

    @DefaultStringValue("Haiti")
    String Country_HT();

    @DefaultStringValue("Hungary")
    String Country_HU();

    @DefaultStringValue("Indonesia")
    String Country_ID();

    @DefaultStringValue("Ireland")
    String Country_IE();

    @DefaultStringValue("Israel")
    String Country_IL();

    @DefaultStringValue("Isle of Man")
    String Country_IM();

    @DefaultStringValue("India")
    String Country_IN();

    @DefaultStringValue("British Indian Ocean Territory")
    String Country_IO();

    @DefaultStringValue("Iraq")
    String Country_IQ();

    @DefaultStringValue("Iran Islamic Republic of")
    String Country_IR();

    @DefaultStringValue("Iceland")
    String Country_IS();

    @DefaultStringValue("Italy")
    String Country_IT();

    @DefaultStringValue("Jersey")
    String Country_JE();

    @DefaultStringValue("Jamaica")
    String Country_JM();

    @DefaultStringValue("Jordan")
    String Country_JO();

    @DefaultStringValue("Japan")
    String Country_JP();

    @DefaultStringValue("Kenya")
    String Country_KE();

    @DefaultStringValue("Kyrgyzstan")
    String Country_KG();

    @DefaultStringValue("Cambodia")
    String Country_KH();

    @DefaultStringValue("Kiribati")
    String Country_KI();

    @DefaultStringValue("Comoros")
    String Country_KM();

    @DefaultStringValue("Saint Kitts and Nevis")
    String Country_KN();

    @DefaultStringValue("Korea North")
    String Country_KP();

    @DefaultStringValue("Korea South")
    String Country_KR();

    @DefaultStringValue("Kuwait")
    String Country_KW();

    @DefaultStringValue("Cayman Islands")
    String Country_KY();

    @DefaultStringValue("Kazakhstan")
    String Country_KZ();

    @DefaultStringValue("Lao PDR")
    String Country_LA();

    @DefaultStringValue("Lebanon")
    String Country_LB();

    @DefaultStringValue("Saint Lucia")
    String Country_LC();

    @DefaultStringValue("Liechtenstein")
    String Country_LI();

    @DefaultStringValue("Sri Lanka")
    String Country_LK();

    @DefaultStringValue("Liberia")
    String Country_LR();

    @DefaultStringValue("Lesotho")
    String Country_LS();

    @DefaultStringValue("Lithuania")
    String Country_LT();

    @DefaultStringValue("Luxembourg")
    String Country_LU();

    @DefaultStringValue("Latvia")
    String Country_LV();

    @DefaultStringValue("Libya")
    String Country_LY();

    @DefaultStringValue("Morocco")
    String Country_MA();

    @DefaultStringValue("Monaco")
    String Country_MC();

    @DefaultStringValue("Moldova")
    String Country_MD();

    @DefaultStringValue("Montenegro")
    String Country_ME();

    @DefaultStringValue("Saint-Martin French part")
    String Country_MF();

    @DefaultStringValue("Madagascar")
    String Country_MG();

    @DefaultStringValue("Marshall Islands")
    String Country_MH();

    @DefaultStringValue("Macedonia Republic of")
    String Country_MK();

    @DefaultStringValue("Mali")
    String Country_ML();

    @DefaultStringValue("Myanmar")
    String Country_MM();

    @DefaultStringValue("Mongolia")
    String Country_MN();

    @DefaultStringValue("Macao SAR China")
    String Country_MO();

    @DefaultStringValue("Northern Mariana Islands")
    String Country_MP();

    @DefaultStringValue("Martinique")
    String Country_MQ();

    @DefaultStringValue("Mauritania")
    String Country_MR();

    @DefaultStringValue("Montserrat")
    String Country_MS();

    @DefaultStringValue("Malta")
    String Country_MT();

    @DefaultStringValue("Mauritius")
    String Country_MU();

    @DefaultStringValue("Maldives")
    String Country_MV();

    @DefaultStringValue("Malawi")
    String Country_MW();

    @DefaultStringValue("Mexico")
    String Country_MX();

    @DefaultStringValue("Malaysia")
    String Country_MY();

    @DefaultStringValue("Mozambique")
    String Country_MZ();

    @DefaultStringValue("Namibia")
    String Country_NA();

    @DefaultStringValue("New Caledonia")
    String Country_NC();

    @DefaultStringValue("Niger")
    String Country_NE();

    @DefaultStringValue("Norfolk Island")
    String Country_NF();

    @DefaultStringValue("Nigeria")
    String Country_NG();

    @DefaultStringValue("Nicaragua")
    String Country_NI();

    @DefaultStringValue("Netherlands")
    String Country_NL();

    @DefaultStringValue("Norway")
    String Country_NO();

    @DefaultStringValue("Nepal")
    String Country_NP();

    @DefaultStringValue("Nauru")
    String Country_NR();

    @DefaultStringValue("Niue")
    String Country_NU();

    @DefaultStringValue("New Zealand")
    String Country_NZ();

    @DefaultStringValue("Oman")
    String Country_OM();

    @DefaultStringValue("Panama")
    String Country_PA();

    @DefaultStringValue("Peru")
    String Country_PE();

    @DefaultStringValue("French Polynesia")
    String Country_PF();

    @DefaultStringValue("Papua New Guinea")
    String Country_PG();

    @DefaultStringValue("Philippines")
    String Country_PH();

    @DefaultStringValue("Pakistan")
    String Country_PK();

    @DefaultStringValue("Poland")
    String Country_PL();

    @DefaultStringValue("Saint Pierre and Miquelon")
    String Country_PM();

    @DefaultStringValue("Pitcairn")
    String Country_PN();

    @DefaultStringValue("Puerto Rico")
    String Country_PR();

    @DefaultStringValue("Palestinian Territory")
    String Country_PS();

    @DefaultStringValue("Portugal")
    String Country_PT();

    @DefaultStringValue("Palau")
    String Country_PW();

    @DefaultStringValue("Paraguay")
    String Country_PY();

    @DefaultStringValue("Qatar")
    String Country_QA();

    @DefaultStringValue("Réunion")
    String Country_RE();

    @DefaultStringValue("Romania")
    String Country_RO();

    @DefaultStringValue("Serbia")
    String Country_RS();

    @DefaultStringValue("Russian Federation")
    String Country_RU();

    @DefaultStringValue("Rwanda")
    String Country_RW();

    @DefaultStringValue("Saudi Arabia")
    String Country_SA();

    @DefaultStringValue("Solomon Islands")
    String Country_SB();

    @DefaultStringValue("Seychelles")
    String Country_SC();

    @DefaultStringValue("Sudan")
    String Country_SD();

    @DefaultStringValue("Sweden")
    String Country_SE();

    @DefaultStringValue("Singapore")
    String Country_SG();

    @DefaultStringValue("Saint Helena")
    String Country_SH();

    @DefaultStringValue("Slovenia")
    String Country_SI();

    @DefaultStringValue("Svalbard and Jan Mayen Islands")
    String Country_SJ();

    @DefaultStringValue("Slovakia")
    String Country_SK();

    @DefaultStringValue("Sierra Leone")
    String Country_SL();

    @DefaultStringValue("San Marino")
    String Country_SM();

    @DefaultStringValue("Senegal")
    String Country_SN();

    @DefaultStringValue("Somalia")
    String Country_SO();

    @DefaultStringValue("Suriname")
    String Country_SR();

    @DefaultStringValue("South Sudan")
    String Country_SS();

    @DefaultStringValue("Sao Tome and Principe")
    String Country_ST();

    @DefaultStringValue("El Salvador")
    String Country_SV();

    @DefaultStringValue("Syrian Arab Republic Syria")
    String Country_SY();

    @DefaultStringValue("Swaziland")
    String Country_SZ();

    @DefaultStringValue("Turks and Caicos Islands")
    String Country_TC();

    @DefaultStringValue("Chad")
    String Country_TD();

    @DefaultStringValue("French Southern Territories")
    String Country_TF();

    @DefaultStringValue("Togo")
    String Country_TG();

    @DefaultStringValue("Thailand")
    String Country_TH();

    @DefaultStringValue("Tajikistan")
    String Country_TJ();

    @DefaultStringValue("Tokelau")
    String Country_TK();

    @DefaultStringValue("Timor-Leste")
    String Country_TL();

    @DefaultStringValue("Turkmenistan")
    String Country_TM();

    @DefaultStringValue("Tunisia")
    String Country_TN();

    @DefaultStringValue("Tonga")
    String Country_TO();

    @DefaultStringValue("Turkey")
    String Country_TR();

    @DefaultStringValue("Trinidad and Tobago")
    String Country_TT();

    @DefaultStringValue("Tuvalu")
    String Country_TV();

    @DefaultStringValue("Taiwan Republic of China")
    String Country_TW();

    @DefaultStringValue("Tanzania United Republic of")
    String Country_TZ();

    @DefaultStringValue("Ukraine")
    String Country_UA();

    @DefaultStringValue("Uganda")
    String Country_UG();

    @DefaultStringValue("US Minor Outlying Islands")
    String Country_UM();

    @DefaultStringValue("United States of America")
    String Country_US();

    @DefaultStringValue("Uruguay")
    String Country_UY();

    @DefaultStringValue("Uzbekistan")
    String Country_UZ();

    @DefaultStringValue("Holy See Vatican City State")
    String Country_VA();

    @DefaultStringValue("Saint Vincent and Grenadines")
    String Country_VC();

    @DefaultStringValue("Venezuela Bolivarian Republic")
    String Country_VE();

    @DefaultStringValue("British Virgin Islands")
    String Country_VG();

    @DefaultStringValue("Virgin Islands US")
    String Country_VI();

    @DefaultStringValue("Viet Nam")
    String Country_VN();

    @DefaultStringValue("Vanuatu")
    String Country_VU();

    @DefaultStringValue("Wallis and Futuna Islands")
    String Country_WF();

    @DefaultStringValue("Samoa")
    String Country_WS();

    @DefaultStringValue("Yemen")
    String Country_YE();

    @DefaultStringValue("Mayotte")
    String Country_YT();

    @DefaultStringValue("South Africa")
    String Country_ZA();

    @DefaultStringValue("Zambia")
    String Country_ZM();

    @DefaultStringValue("Zimbabwe")
    String Country_ZW();

    /**
     * Errors 404
     */
    @DefaultStringValue("Person not found.")
    String personNotFound();

    @DefaultStringValue("Repository not found.")
    String repositoryNotFound();

    @DefaultStringValue("Class not found.")
    String classNotFound();

    @DefaultStringValue("Person or institution not found.")
    String personOrInstitutionNotFound();

    /**
     * Errors 401
     */
    @DefaultStringValue("Authentication failed.")
    String authenticationFailed();

    @DefaultStringValue("You must authenticate to access this path.")
    String mustAuthenticate();

    @DefaultStringValue("It wasn't possible to change your password.")
    String passwordChangeFailed();

    @DefaultStringValue("Unauthorized attempt to change the password.")
    String passwordChangeDenied();

    @DefaultStringValue("Unauthorized attempt to update a class without platformAdmin or institutionAdmin rights.")
    String classNoRights();

    @DefaultStringValue("Unauthorized attempt to generate the class' certificates without admin rights.")
    String unauthorizedAccessReport();

    @DefaultStringValue("Access denied.")
    String accessDenied();

    /**
     * Errors 403
     *
     */
    @DefaultStringValue("For security reasons, you must update your password.")
    String forcedPasswordChange();

    /**
     * Errors 409
     */
    @DefaultStringValue("A course with this name or code already exists.")
    String courseAlreadyExists();

    @DefaultStringValue("A course version with this name or code already exists.")
    String courseVersionAlreadyExists();

    @DefaultStringValue("A class with this name already exists.")
    String courseClassAlreadyExists();

    @DefaultStringValue("Invalid input value")
    String invalidValue();

    @DefaultStringValue("Constraint Violated (uuid or name).")
    String constraintViolatedUUIDName();

    @DefaultStringValue("User is already enrolled on the selected class.")
    String userAlreadyEnrolledInClass();

    @DefaultStringValue("Could not complete the request. Check the amount of enrollments available in this class.")
    String tooManyEnrollments();

    @DefaultStringValue("Enrollment for this class is only available on a Parent version.")
    String cannotEnrollOnChildVersion();

    /**
     * Errors 500
     */

    @DefaultStringValue("Error generating the report.")
    String errorGeneratingReport();

    @DefaultStringValue("Error checking for certificates.")
    String errorCheckingCerts();

    @DefaultStringValue("Error checking for the class info report.")
    String errorCheckingClassInfo();

}