package kornell.gui.client.presentation.welcome.generic;

import static kornell.core.util.StringUtils.mkurl;

import java.util.Date;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.ContentSpec;
import kornell.core.entity.Course;
import kornell.core.entity.EntityState;
import kornell.core.entity.Enrollment;
import kornell.core.entity.EnrollmentProgress;
import kornell.core.entity.EnrollmentProgressDescription;
import kornell.core.entity.EnrollmentSource;
import kornell.core.entity.EnrollmentState;
import kornell.core.entity.EntityFactory;
import kornell.core.to.CourseClassTO;
import kornell.core.to.EnrollmentTO;
import kornell.core.to.TOFactory;
import kornell.gui.client.Kornell;
import kornell.gui.client.KornellConstants;
import kornell.gui.client.personnel.classroom.Student;
import kornell.gui.client.personnel.classroom.Teacher;
import kornell.gui.client.personnel.classroom.Teachers;
import kornell.gui.client.presentation.classroom.ClassroomPlace;
import kornell.gui.client.util.ClientConstants;
import kornell.gui.client.util.EnumTranslator;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.view.KornellNotification;

public class GenericCourseSummaryView extends Composite {
    interface MyUiBinder extends UiBinder<Widget, GenericCourseSummaryView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    private KornellConstants constants = GWT.create(KornellConstants.class);

    @UiField
    Heading hTitle;

    @UiField
    Label lblSubTitle;

    @UiField
    Paragraph pDescription;

    @UiField
    Paragraph pStatus;

    @UiField
    Paragraph pStatusInfo;

    @UiField
    Paragraph pStatusErr;

    @UiField
    Paragraph pStatus2;

    @UiField
    Paragraph pStatusInfo2;

    @UiField
    ProgressBar progressBar;

    @UiField
    Image imgThumb;

    @UiField
    Image imgIconCourse;

    @UiField
    FlowPanel pnlCourseSummaryBar;

    String ICON_COURSE_URL = mkurl(ClientConstants.IMAGES_PATH, "welcomeCourses");
    String iconCourseURL;

    private CourseClassTO courseClassTO;
    private PlaceController placeCtrl;
    private KornellSession session;

    public GenericCourseSummaryView(final PlaceController placeCtrl, final CourseClassTO courseClassTO,
            final KornellSession session) {
        initWidget(uiBinder.createAndBindUi(this));

        this.courseClassTO = courseClassTO;
        this.placeCtrl = placeCtrl;
        this.session = session;
        Course course = courseClassTO.getCourseVersionTO().getCourseTO().getCourse();
        hTitle.setText(course.getName());
        lblSubTitle.setText(courseClassTO.getCourseClass().getName());
        pDescription.setText(course.getDescription());

        final Teacher teacher = Teachers.of(courseClassTO);
        Student student = teacher.student(session.getCurrentUser());
        if (courseClassTO.getEnrollment() != null
                && EnrollmentState.cancelled.equals(courseClassTO.getEnrollment().getState())) {
            pStatusErr.setText(constants.cancelledClassLabel());
            pStatusErr.removeStyleName("shy");
        } else if (courseClassTO.getEnrollment() != null
                && FormHelper.isEnrollmentExpired(courseClassTO)) {
            pStatusErr.setText(constants.expiredClassLabel() + " " + DateTimeFormat.getFormat("yyyy-MM-dd").format(courseClassTO.getEnrollment().getEndDate()) + ")");
            pStatusErr.removeStyleName("shy");
        } else if (courseClassTO.getEnrollment() != null
                && !FormHelper.isEnrollmentExpired(courseClassTO) && FormHelper.isEnrollmentExpires(courseClassTO)) {
            pStatusErr.setText(constants.expiresClassLabel() + " " + DateTimeFormat.getFormat("yyyy-MM-dd").format(courseClassTO.getEnrollment().getEndDate()) + ")");
            pStatusErr.removeStyleName("shy");
        }
        if (!EntityState.active.equals(courseClassTO.getCourseClass().getState())) {
            pStatus.setText(constants.inactiveClassLabel());
            iconCourseURL = mkurl(ICON_COURSE_URL, "iconNotStarted.png");
        } else if (student.isEnrolled()) {
            onEnrolled(student);
        } else {
            onNotEnrolled();
        }
        onEnrolledOrNot();
    }

    private void onEnrolledOrNot() {
        String thumbURL = courseClassTO.getCourseClass().getThumbUrl();
        thumbURL = thumbURL != null ? thumbURL : courseClassTO.getCourseVersionTO().getCourseVersion().getThumbUrl();
        thumbURL = thumbURL != null ? thumbURL
                : courseClassTO.getCourseVersionTO().getCourseTO().getCourse().getThumbUrl();
        thumbURL = thumbURL != null ? thumbURL : mkurl(ICON_COURSE_URL, "thumb.png");
        imgThumb.setUrl(thumbURL);
        imgIconCourse.setUrl(iconCourseURL);

        sinkEvents(Event.ONCLICK);
        addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (courseClassTO.getEnrollment() == null
                        && courseClassTO.getCourseClass().isApproveEnrollmentsAutomatically()) {
                    requestEnrollment();
                    return;
                } else if (courseClassTO.getEnrollment() != null) {
                    session.setCurrentCourseClass(courseClassTO);
                    placeCtrl.goTo(new ClassroomPlace(courseClassTO.getEnrollment().getUUID()));
                }
            }
        }, ClickEvent.getType());

    }

    private void onNotEnrolled() {
        Button requestEnrollmentBtn = new Button(courseClassTO.getCourseClass().isApproveEnrollmentsAutomatically()
                ? constants.startCourseLabel() : constants.requestEnrollmentLabel());
        requestEnrollmentBtn.addStyleName("right btnAction");
        pnlCourseSummaryBar.add(requestEnrollmentBtn);

        pStatus.setText(constants.availableClassLabel());
        iconCourseURL = mkurl(ICON_COURSE_URL, "iconAcquire.png");
    }

    private void onEnrolled(Student student) {
        EnrollmentProgress progress = student.getEnrollmentProgress();
        switch (progress.getDescription()) {
        case notStarted:
            onCourseNotStarted();
            break;
        case completed:
            onCourseCompleted(progress.getCertifiedAt());
            break;
        case inProgress:
            onCourseInProgress(progress.getProgress());
            break;
        }
    }

    private void onCourseInProgress(Integer progress) {
        if (progress >= 100) {
            pStatus.setText(constants.pendingGradeLabel());
        } else {
            pStatus.setText(EnumTranslator.translateEnum(EnrollmentProgressDescription.inProgress));
            if (ContentSpec.KNL.equals(courseClassTO.getCourseVersionTO().getCourseTO().getCourse().getContentSpec())) {
                progressBar.removeStyleName("shy");
                progressBar.setPercent(progress);
                pStatusInfo.setText(progress + "% ");
                pStatus.setText(pStatus.getText() + ": ");
            }
        }
        iconCourseURL = mkurl(ICON_COURSE_URL, "iconInProgress.png");

    }

    private void onCourseNotStarted() {
        if (courseClassTO.getEnrollment() != null
                && EnrollmentState.requested.equals(courseClassTO.getEnrollment().getState())) {
            pStatus.setText(constants.pendingEnrollmentApproval());
            iconCourseURL = mkurl(ICON_COURSE_URL, "iconWaiting.png");
        } else {
            pStatus.setText(constants.toStart());
            iconCourseURL = mkurl(ICON_COURSE_URL, "iconNotStarted.png");
        }
    }

    private void onCourseCompleted(Date certifiedAt) {
        String statusText = EnumTranslator.translateEnum(EnrollmentProgressDescription.completed);
        if (certifiedAt != null) {
            statusText += " " + constants.completedOnToken() + ": ";
            pStatusInfo.setText(DateTimeFormat.getFormat("yyyy-MM-dd").format(certifiedAt));
        }
        if (courseClassTO.getCourseClass().getRequiredScore() != null
                && courseClassTO.getCourseClass().getRequiredScore().intValue() != 0
                && courseClassTO.getEnrollment().getAssessmentScore() != null) {
            pStatus2.getElement().setInnerHTML("&nbsp;&nbsp;&nbsp;&nbsp;" + constants.completedCourseGradeLabel() + ":");
            pStatusInfo2.setText("" + courseClassTO.getEnrollment().getAssessmentScore().intValue());
        }
        pStatus.setText(statusText);
        iconCourseURL = mkurl(ICON_COURSE_URL, "iconFinished.png");
    }

    private void requestEnrollment() {
        EntityFactory entityFactory = GWT.create(EntityFactory.class);
        Enrollment enrollment = entityFactory.enrollment().as();
        enrollment.setCourseClassUUID(courseClassTO.getCourseClass().getUUID());
        enrollment.setPersonUUID(session.getCurrentUser().getPerson().getUUID());
        enrollment.setState(courseClassTO.getCourseClass().isApproveEnrollmentsAutomatically()
                ? EnrollmentState.enrolled : EnrollmentState.requested);
        enrollment.setEnrollmentSource(EnrollmentSource.WEBSITE);
        session.enrollments().createEnrollment(enrollment, new Callback<Enrollment>() {
            @Override
            public void ok(Enrollment enrollment) {
                updateEnrollmentOnCourseClassTO(enrollment);
                placeCtrl.goTo(new ClassroomPlace(enrollment.getUUID()));
            }
        });
    }

    private void updateEnrollmentOnCourseClassTO(Enrollment enrollment) {
        TOFactory toFactory = GWT.create(TOFactory.class);
        EnrollmentTO enrollmentTO = toFactory.newEnrollmentTO().as();
        enrollmentTO.setEnrollment(enrollment);
        enrollmentTO.setPersonUUID(session.getCurrentUser().getPerson().getUUID());
        enrollmentTO.setFullName(session.getCurrentUser().getPerson().getFullName());
        enrollmentTO.setUsername(session.getCurrentUser().getUsername());
        courseClassTO.setEnrollment(enrollment);
        session.setCurrentCourseClass(courseClassTO);
    }
}
