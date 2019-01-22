package kornell.core.entity.role;

import kornell.core.entity.Entity;
import kornell.core.entity.EntityFactory;

public interface Role extends Entity {
    public static String TYPE = EntityFactory.PREFIX + "role+json";

    RoleType getRoleType();
    void setRoleType(RoleType roleType);

    String getPersonUUID();
    void setPersonUUID(String personUUID);

    UserRole getUserRole();
    void setUserRole(UserRole userRole);

    PlatformAdminRole getPlatformAdminRole();
    void setPlatformAdminRole(PlatformAdminRole platformRole);

    InstitutionAdminRole getInstitutionAdminRole();
    void setInstitutionAdminRole(InstitutionAdminRole institutionAdminRole);

    CourseClassAdminRole getCourseClassAdminRole();
    void setCourseClassAdminRole(CourseClassAdminRole courseClassAdminRole);

    TutorRole getTutorRole();
    void setTutorRole(TutorRole tutorRole);

    CourseClassObserverRole getCourseClassObserverRole();
    void setCourseClassObserverRole(CourseClassObserverRole courseClassObserverRole);

    ControlPanelAdminRole getControlPanelAdminRole();
    void setControlPanelAdminRole(ControlPanelAdminRole controlPanelAdminRole);

    PublisherRole getPublisherRole();
    void setPublisherRole(PublisherRole publisherRole);

    InstitutionCourseClassesAdminRole getInstitutionCourseClassesAdminRole();
    void setInstitutionCourseClassesAdminRole(InstitutionCourseClassesAdminRole institutionCourseClassesAdminRole);

    InstitutionCourseClassesObserverRole getInstitutionCourseClassesObserverRole();
    void setInstitutionCourseClassesObserverRole(InstitutionCourseClassesObserverRole institutionCourseClassesObserverRole);
}
