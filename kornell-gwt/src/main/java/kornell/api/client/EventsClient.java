package kornell.api.client;

import com.google.gwt.core.client.GWT;

import kornell.core.entity.EntityState;
import kornell.core.entity.EnrollmentState;
import kornell.core.event.ActomEntered;
import kornell.core.event.AttendanceSheetSigned;
import kornell.core.event.CourseClassStateChanged;
import kornell.core.event.EnrollmentStateChanged;
import kornell.core.event.EnrollmentTransferred;
import kornell.core.event.Event;
import kornell.core.event.EventFactory;
import kornell.core.to.EntityChangedEventsTO;

public class EventsClient extends RESTClient {
    // TODO: Move all object creation to client factory
    static final EventFactory factory = GWT.create(EventFactory.class);

    public EventClient actomEntered(String enrollmentUUID, final String actomKey) {
        ActomEntered actomEntered = factory.newActomEntered().as();
        actomEntered.setEnrollmentUUID(enrollmentUUID);
        actomEntered.setActomKey(actomKey);
        return withEvent("/events/actomEntered", ActomEntered.TYPE, actomEntered);
    }

    public EventClient attendanceSheetSigned(String institutionUUID, String personUUID) {
        AttendanceSheetSigned attendanceSheetSigned = factory.newAttendanceSheetSigned().as();
        attendanceSheetSigned.setInstitutionUUID(institutionUUID);
        attendanceSheetSigned.setPersonUUID(personUUID);
        return withEvent("/events/attendanceSheetSigned", AttendanceSheetSigned.TYPE, attendanceSheetSigned);
    }

    public EventClient enrollmentStateChanged(String enrollmentUUID, String personUUID, EnrollmentState fromState,
            EnrollmentState toState) {
        EnrollmentStateChanged enrollmentStateChanged = factory.newEnrollmentStateChanged().as();
        enrollmentStateChanged.setEnrollmentUUID(enrollmentUUID);
        enrollmentStateChanged.setFromPersonUUID(personUUID);
        enrollmentStateChanged.setFromState(fromState);
        enrollmentStateChanged.setToState(toState);
        return withEvent("/events/enrollmentStateChanged", EnrollmentStateChanged.TYPE, enrollmentStateChanged);
    }

    public EventClient courseClassStateChanged(String courseClassUUID, String personUUID, EntityState fromState,
            EntityState toState) {
        CourseClassStateChanged courseClassStateChanged = factory.newCourseClassStateChanged().as();
        courseClassStateChanged.setCourseClassUUID(courseClassUUID);
        courseClassStateChanged.setFromPersonUUID(personUUID);
        courseClassStateChanged.setFromState(fromState);
        courseClassStateChanged.setToState(toState);
        return withEvent("/events/courseClassStateChanged", CourseClassStateChanged.TYPE, courseClassStateChanged);
    }

    public EventClient enrollmentTransfered(String enrollmentUUID, String toCourseClassUUID, String fromCourseClassUUID,
            String personUUID) {
        EnrollmentTransferred enrollmentTransferred = factory.newEnrollmentTransferred().as();
        enrollmentTransferred.setFromPersonUUID(personUUID);
        enrollmentTransferred.setEnrollmentUUID(enrollmentUUID);
        enrollmentTransferred.setFromCourseClassUUID(fromCourseClassUUID);
        enrollmentTransferred.setToCourseClassUUID(toCourseClassUUID);
        return withEvent("/events/enrollmentTransferred", EnrollmentTransferred.TYPE, enrollmentTransferred);
    }

    public void getEntityChangedEvents(String entityType, String ps, String pn, Callback<EntityChangedEventsTO> cb) {
        GET("/events/entityChanged/?entityType=" + entityType + "&ps=" + ps + "&pn=" + pn).sendRequest(null, cb);
    }

    private EventClient withEvent(String path, String contentType, Event event) {
        return new EventClient(path, contentType, event);
    }

}
