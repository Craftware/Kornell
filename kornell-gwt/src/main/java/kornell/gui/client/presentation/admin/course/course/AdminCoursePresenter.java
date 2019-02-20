package kornell.gui.client.presentation.admin.course.course;

import java.util.logging.Logger;

import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.Course;
import kornell.core.error.KornellErrorTO;
import kornell.core.to.CourseClassTO;
import kornell.gui.client.GenericClientFactoryImpl;
import kornell.gui.client.KornellConstantsHelper;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.mvp.PlaceUtils;
import kornell.gui.client.presentation.admin.course.courses.AdminCoursesPlace;
import kornell.gui.client.presentation.admin.courseversion.courseversion.AdminCourseVersionPlace;
import kornell.gui.client.util.view.KornellNotification;

public class AdminCoursePresenter implements AdminCourseView.Presenter {
    Logger logger = Logger.getLogger(AdminCoursePresenter.class.getName());
    private AdminCourseView view;
    private KornellSession session;
    private PlaceController placeController;
    private EventBus bus;
    Place defaultPlace;
    private ViewFactory viewFactory;

    public AdminCoursePresenter(KornellSession session, PlaceController placeController, EventBus bus,
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
            view = getView();
            view.setPresenter(this);
            view.init();
        } else {
            logger.warning("Hey, only admins are allowed to see this! " + this.getClass().getName());
            placeController.goTo(defaultPlace);
        }
    }

    @Override
    public Course getNewCourse() {
        return GenericClientFactoryImpl.ENTITY_FACTORY.newCourse().as();
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }

    private AdminCourseView getView() {
        return viewFactory.getAdminCourseView();
    }

    @Override
    public void upsertCourse(Course course) {
        bus.fireEvent(new ShowPacifierEvent(true));
        if (course.getUUID() == null) {
            session.courses().create(course, new Callback<CourseClassTO>() {
                @Override
                public void ok(CourseClassTO courseClassTO) {
                    bus.fireEvent(new ShowPacifierEvent(false));
                    placeController.goTo(new AdminCourseVersionPlace(courseClassTO.getCourseVersionTO().getCourseVersion().getUUID()));
                    KornellNotification.show("Conteúdo criado com sucesso! Você pode gerenciar sua versão se desejar, ou pode publicar seu material.", 5000);
                    PlaceUtils.reloadCurrentPlace(bus, placeController);
                }

                @Override
                public void unauthorized(KornellErrorTO kornellErrorTO) {
                    bus.fireEvent(new ShowPacifierEvent(false));
                    KornellNotification.show(KornellConstantsHelper.getErrorMessage(kornellErrorTO), AlertType.ERROR,
                            2500);
                }

                @Override
                public void conflict(KornellErrorTO kornellErrorTO) {
                    bus.fireEvent(new ShowPacifierEvent(false));
                    KornellNotification.show(KornellConstantsHelper.getErrorMessage(kornellErrorTO), AlertType.ERROR,
                            2500);
                }
            });
        } else {
            session.course(course.getUUID()).update(course, new Callback<Course>() {
                @Override
                public void ok(Course course) {
                    bus.fireEvent(new ShowPacifierEvent(false));
                    KornellNotification.show("Alterações salvas com sucesso!");
                    placeController.goTo(new AdminCoursesPlace());
                }

                @Override
                public void unauthorized(KornellErrorTO kornellErrorTO) {
                    bus.fireEvent(new ShowPacifierEvent(false));
                    KornellNotification.show(KornellConstantsHelper.getErrorMessage(kornellErrorTO), AlertType.ERROR,
                            2500);
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
}