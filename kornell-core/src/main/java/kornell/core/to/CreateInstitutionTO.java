package kornell.core.to;

public interface CreateInstitutionTO {
    public static final String TYPE = TOFactory.PREFIX + "createInstitution+json";

    String getShortName();
    void setShortName(String shortName);

    String getFullName();
    void setFullName(String fullName);

    String getInstitutionAdminEmail();
    void setInstitutionAdminEmail(String institutionAdminEmail);

    String getInstitutionAdminPassword();
    void setInstitutionAdminPassword(String institutionAdminPassword);
}
