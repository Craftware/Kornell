package kornell.core.entity;

import java.util.Date;


public interface Institution extends Named {
    public static String TYPE = EntityFactory.PREFIX + "institution+json";

    String getFullName();
    void setFullName(String fullName);

    String getTerms();
    void setTerms(String terms);

    String getAssetsRepositoryUUID();
    void setAssetsRepositoryUUID(String assetsRepositoryUUID);

    String getBaseURL();
    void setBaseURL(String baseURL);

    boolean isDemandsPersonContactDetails();
    void setDemandsPersonContactDetails(boolean demandsPersonContactDetails);

    boolean isValidatePersonContactDetails();
    void setValidatePersonContactDetails(boolean validatePersonContactDetails);

    boolean isAllowRegistration();
    void setAllowRegistration(boolean allowRegistration);

    boolean isAllowRegistrationByUsername();
    void setAllowRegistrationByUsername(boolean allowRegistrationByUsername);

    Date getActivatedAt();
    void setActivatedAt(Date activatedAt);

    String getSkin();
    void setSkin(String skin);

    BillingType getBillingType();
    void setBillingType(BillingType billingType);

    InstitutionType getInstitutionType();
    void setInstitutionType(InstitutionType institutionType);

    String getDashboardVersionUUID();
    void setDashboardVersionUUID(String dashboardVersionUUID);

    boolean isUseEmailWhitelist();
    void setUseEmailWhitelist(boolean useEmailWhitelist);

    String getTimeZone();
    void setTimeZone(String timeZone);

    String getInstitutionSupportEmail();
    void setInstitutionSupportEmail(String institutionSupportEmail);

    boolean isAdvancedMode();
    void setAdvancedMode(boolean advancedMode);

    boolean isNotifyInstitutionAdmins();
    void setNotifyInstitutionAdmins(boolean notifyInstitutionAdmins);

    String getAllowedLanguages();
    void setAllowedLanguages(String allowedLanguages);

    boolean isDisabled();
    void setDisabled(boolean disabled);

    boolean isEnforceSequentialProgress();
    void setEnforceSequentialProgress(boolean enforceSequentialProgress);
}
