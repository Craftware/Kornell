package kornell.gui.client.presentation.admin.courseversion.courseversion;

import java.util.logging.Logger;

import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.Course;
import kornell.core.entity.CourseVersion;
import kornell.core.error.KornellErrorTO;
import kornell.core.to.CourseVersionTO;
import kornell.gui.client.KornellConstantsHelper;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.presentation.admin.courseversion.courseversions.AdminCourseVersionsPlace;
import kornell.gui.client.util.view.KornellNotification;

public class AdminCourseVersionPresenter implements AdminCourseVersionView.Presenter {
    private Logger logger = Logger.getLogger(AdminCourseVersionPresenter.class.getName());
    private AdminCourseVersionView view;
    private KornellSession session;
    private PlaceController placeController;
    private EventBus bus;
    private Place defaultPlace;
    private ViewFactory viewFactory;
    private AdminCourseVersionContentPresenter courseVersionContentPresenter;
    private CourseVersion courseVersion;
    private Course course;

    public AdminCourseVersionPresenter(KornellSession session, PlaceController placeController, EventBus bus,
            Place defaultPlace, ViewFactory viewFactory) {
        this.session = session;
        this.placeController = placeController;
        this.bus = bus;
        this.defaultPlace = defaultPlace;
        this.viewFactory = viewFactory;
        init();
    }

    private void init() {
        if (session.hasPublishingRole()) {
            view = viewFactory.getAdminCourseVersionView();
            if (view.getPresenter() == null) {
                view.setPresenter(this);
            }
        } else {
            logger.warning("Hey, only admins are allowed to see this! " + this.getClass().getName());
            placeController.goTo(defaultPlace);
        }
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }

    public AdminCourseVersionView getView() {
        return view;
    }

    @Override
    public void upsertCourseVersion(CourseVersion courseVersion) {
        if (courseVersion.getUUID() == null) {
            session.courseVersions().create(courseVersion, new Callback<CourseVersion>() {
                @Override
                public void ok(CourseVersion courseVersion) {
                    KornellNotification.show("Versão de conteúdo criada com sucesso!");
                    bus.fireEvent(new ShowPacifierEvent(false));
                    placeController.goTo(new AdminCourseVersionPlace(courseVersion.getUUID()));
                }

                @Override
                public void conflict(KornellErrorTO kornellErrorTO) {
                    bus.fireEvent(new ShowPacifierEvent(false));
                    KornellNotification.show(KornellConstantsHelper.getErrorMessage(kornellErrorTO), AlertType.ERROR,
                            2500);
                }
            });
        } else {
            session.courseVersion(courseVersion.getUUID()).update(courseVersion, new Callback<CourseVersion>() {
                @Override
                public void ok(CourseVersion courseVersion) {
                    bus.fireEvent(new ShowPacifierEvent(false));
                    KornellNotification.show("Alterações salvas com sucesso!");
                    placeController.goTo(new AdminCourseVersionsPlace());
                }

                @Override
                public void conflict(KornellErrorTO kornellErrorTO) {
                    bus.fireEvent(new ShowPacifierEvent(false));
                    KornellNotification.show(KornellConstantsHelper.getErrorMessage(kornellErrorTO), AlertType.ERROR,
                            2500);
                }
            });
        }
    }

    @Override
    public void buildContentView(String courseVersionUUID) {
        final AdminCourseVersionPresenter thiz = this;
        session.courseVersion(courseVersionUUID).get(new Callback<CourseVersionTO>() {
            @Override
            public void ok(CourseVersionTO to) {
                courseVersion = to.getCourseVersion();
                course = to.getCourseTO().getCourse();
                if (courseVersionContentPresenter == null) {
                    courseVersionContentPresenter = new AdminCourseVersionContentPresenter(session, placeController, bus,
                            defaultPlace, viewFactory);
                }
                courseVersionContentPresenter.init(thiz);
                view.addContentPanel((AdminCourseVersionContentView) courseVersionContentPresenter.asWidget());
            }
        });
    }

    @Override
    public CourseVersion getCourseVersion() {
        return this.courseVersion;
    }

    @Override
    public void setCourseVersion(CourseVersion courseVersion) {
        this.courseVersion = courseVersion;
    }

    @Override
    public Course getCourse() {
        return this.course;
    }
}