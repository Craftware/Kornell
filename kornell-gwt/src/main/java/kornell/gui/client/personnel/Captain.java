package kornell.gui.client.personnel;

import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceChangeRequestEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.KornellSession;
import kornell.core.entity.ContentSpec;
import kornell.core.entity.EnrollmentState;
import kornell.core.entity.EntityState;
import kornell.core.to.CourseClassTO;
import kornell.core.to.UserInfoTO;
import kornell.gui.client.KornellConstants;
import kornell.gui.client.event.*;
import kornell.gui.client.presentation.classroom.ClassroomPlace;
import kornell.gui.client.presentation.vitrine.VitrinePlace;
import kornell.gui.client.util.ClientProperties;
import kornell.gui.client.util.view.KornellMaintenance;
import kornell.gui.client.util.view.KornellNotification;

public class Captain implements LogoutEventHandler, LoginEventHandler, DisableInstitutionEventHandler {
    Logger logger = Logger.getLogger(Captain.class.getName());
    private PlaceController placeCtrl;
    private KornellSession session;

    private static KornellConstants constants = GWT.create(KornellConstants.class);

    public Captain(EventBus bus, final KornellSession session, final PlaceController placeCtrl) {
        this.placeCtrl = placeCtrl;
        this.session = session;
        bus.addHandler(LogoutEvent.TYPE, this);
        bus.addHandler(LoginEvent.TYPE, this);
        bus.addHandler(DisableInstitutionEvent.TYPE, this);

        bus.addHandler(PlaceChangeRequestEvent.TYPE, new PlaceChangeRequestEvent.Handler() {
            @Override
            public void onPlaceChangeRequest(PlaceChangeRequestEvent event) {
                // if the user tries to go from one classroom to another, don't
                // show this message
                if (!(placeCtrl.getWhere() instanceof ClassroomPlace
                        && event.getNewPlace() instanceof ClassroomPlace)) {
                    // if the user is inside the classroom and doesn't try to go
                    // to the vitrine (by logging out, for example)
                    if (placeCtrl.getWhere() instanceof ClassroomPlace
                            && !(event.getNewPlace() instanceof VitrinePlace)) {
                        // if the courseClassTO is null, it's a Dashboard
                        // institution on a child course (enrollment is attached on the version)
                        // if the user hasn't passed the class and the type of the
                        // version isn't KNL (small htmls, user won't lose progress)
                        CourseClassTO courseClassTO = session.getCurrentCourseClass();
                        if (courseClassTO == null || (courseClassTO.getCourseClass() != null
                                && ContentSpec.SCORM12.equals(
                                        courseClassTO.getCourseVersionTO().getCourseTO().getCourse().getContentSpec())
                                && courseClassTO.getEnrollment() != null
                                && courseClassTO.getEnrollment().getCertifiedAt() == null
                                && EntityState.active.equals(courseClassTO.getCourseClass().getState())
                                && EnrollmentState.enrolled.equals(courseClassTO.getEnrollment().getState()))) {
                            event.setWarning(constants.leavingTheClassroom());
                        }
                    }
                }
                // since the new place might have a placebar or not, reposition
                // the notifications
                KornellNotification.repositionPopups();
            }
        });
    }

    @Override
    public void onLogout() {
        session.logout();
        placeCtrl.goTo(VitrinePlace.instance);
        Window.Location.reload();
    }

    @Override
    public void onLogin(UserInfoTO user) {
        logger.info("User logged in as " + user.getUsername());
    }

    @Override
    public void onServiceUnavailable() {
        ClientProperties.remove(ClientProperties.X_KNL_TOKEN);
        this.session.setCurrentUser(null);
        placeCtrl.goTo(VitrinePlace.instance);
        Window.Location.reload();
        KornellMaintenance.show();
    }
}
