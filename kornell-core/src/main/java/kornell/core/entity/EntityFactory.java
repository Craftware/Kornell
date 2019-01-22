package kornell.core.entity;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;

import kornell.core.entity.role.*;

public interface EntityFactory extends AutoBeanFactory {
    public static String PREFIX = "application/vnd.kornell.v1.entity.";

    AutoBean<Person> newPerson();
    AutoBean<People> newPeople();
    AutoBean<Principal> newPrincipal();
    AutoBean<Course> newCourse();
    AutoBean<Enrollment> enrollment();
    AutoBean<Enrollments> newEnrollments();
    AutoBean<Institution> newInstitution();
    AutoBean<Role> newRole();
    AutoBean<Roles> newRoles();
    AutoBean<UserRole> newUserRole();
    AutoBean<PlatformAdminRole> newPlatformAdminRole();
    AutoBean<InstitutionAdminRole> newInstitutionAdminRole();
    AutoBean<CourseClassAdminRole> newCourseClassAdminRole();
    AutoBean<TutorRole> newTutorRole();
    AutoBean<CourseClassObserverRole> newCourseClassObserverRole();
    AutoBean<ControlPanelAdminRole> newControlPanelAdminRole();
    AutoBean<PublisherRole> newPublisherRole();
    AutoBean<InstitutionCourseClassesAdminRole> newInstitutionCourseClassesAdminRole();
    AutoBean<InstitutionCourseClassesObserverRole> newInstitutionCourseClassesObserverRole();
    AutoBean<CourseVersion> newCourseVersion();
    AutoBean<CourseClass> newCourseClass();
    AutoBean<ActomEntries> newActomEntries();
    AutoBean<EnrollmentProgress> newEnrollmentProgress();
    AutoBean<ContentRepository> newContentRepository();
    AutoBean<ChatThread> newChatThread();
    AutoBean<ChatThreadParticipant> newChatThreadParticipant();
    AutoBean<ChatThreadMessage> newChatThreadMessage();
    AutoBean<InstitutionRegistrationPrefix> newInstitutionRegistrationPrefix();
    AutoBean<EnrollmentsEntries> newEnrollmentsEntries();
    AutoBean<EnrollmentEntries> newEnrollmentEntries();
    AutoBean<CourseDetailsHint> newCourseDetailsHint();
    AutoBean<CourseDetailsSection> newCourseDetailsSection();
    AutoBean<CourseDetailsLibrary> newCourseDetailsLibrary();
    AutoBean<CertificateDetails> newCertificateDetails();
    AutoBean<PostbackConfig> newPostbackConfig();
    AutoBean<EmailTemplate> newEmailTemplate();
    AutoBean<Track> newTrack();
    AutoBean<TrackEnrollment> newTrackEnrollment();
    AutoBean<TrackItem> newTrackItem();
}
