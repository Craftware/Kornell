package kornell.gui.client.presentation.admin.course.courses;

import java.util.List;
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
import kornell.core.to.CourseTO;
import kornell.core.to.CoursesTO;
import kornell.core.to.TOFactory;
import kornell.core.util.StringUtils;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.presentation.admin.common.ConfirmModalView;
import kornell.gui.client.presentation.admin.course.course.AdminCoursePlace;
import kornell.gui.client.util.ClientProperties;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.view.KornellNotification;
import kornell.gui.client.util.view.table.PaginationPresenterImpl;

public class AdminCoursesPresenter extends PaginationPresenterImpl<CourseTO> implements AdminCoursesView.Presenter {
    Logger logger = Logger.getLogger(AdminCoursesPresenter.class.getName());
    private AdminCoursesView view;
    FormHelper formHelper;
    private KornellSession session;
    private PlaceController placeController;
    private Place defaultPlace;
    TOFactory toFactory;
    private ViewFactory viewFactory;
    private CoursesTO coursesTO;
    private EventBus bus;
    private boolean blockActions;
    private ConfirmModalView confirmModal;

    public AdminCoursesPresenter(KornellSession session, EventBus bus, PlaceController placeController,
            TOFactory toFactory, ViewFactory viewFactory) {
        this.session = session;
        this.bus = bus;
        this.placeController = placeController;
        this.toFactory = toFactory;
        this.viewFactory = viewFactory;
        this.confirmModal = viewFactory.getConfirmModalView();
        formHelper = new FormHelper();
        init();
    }

    private void init() {
        if (session.hasPublishingRole()) {
            initializeProperties("c.name");
            view = getView();
            view.setPresenter(this);
            bus.fireEvent(new ShowPacifierEvent(true));
            getCourses();
        } else {
            logger.warning("Hey, only admins are allowed to see this! " + this.getClass().getName());
            placeController.goTo(defaultPlace);
        }
    }

    private void getCourses() {
        bus.fireEvent(new ShowPacifierEvent(true));
        session.courses().get(true, pageSize, pageNumber, searchTerm, orderBy, asc, new Callback<CoursesTO>() {
            @Override
            public void ok(CoursesTO to) {
                coursesTO = to;
                view.setCourses(coursesTO.getCourses());
                bus.fireEvent(new ShowPacifierEvent(false));
                updateProperties();
            }
        });
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }

    private AdminCoursesView getView() {
        return viewFactory.getAdminCoursesView();
    }

    @Override
    public void updateData() {
        getCourses();
    }

    @Override
    public String getClientPropertyName(String property) {
        return session.getAdminHomePropertyPrefix() + "courses" + ClientProperties.SEPARATOR + property;
    }

    @Override
    public int getTotalRowCount() {
        return StringUtils.isNone(getSearchTerm()) ? coursesTO.getCount() : coursesTO.getSearchCount();
    }

    @Override
    public List<CourseTO> getRowData() {
        return coursesTO.getCourses();
    }

    @Override
    public void deleteCourse(CourseTO courseTO) {
        if (!blockActions) {
            blockActions = true;

            confirmModal.showModal("Tem certeza que deseja excluir o conteúdo \"" + courseTO.getCourse().getName() + "\"?",
                    new com.google.gwt.core.client.Callback<Void, Void>() {
                @Override
                public void onSuccess(Void result) {
                    bus.fireEvent(new ShowPacifierEvent(true));
                    session.course(courseTO.getCourse().getUUID()).delete(new Callback<Course>() {
                        @Override
                        public void ok(Course to) {
                            blockActions = false;
                            bus.fireEvent(new ShowPacifierEvent(false));
                            KornellNotification.show("Conteúdo excluído com sucesso.");
                            updateData();
                        }

                        @Override
                        public void internalServerError(KornellErrorTO error) {
                            blockActions = false;
                            bus.fireEvent(new ShowPacifierEvent(false));
                            KornellNotification.show("Erro ao tentar excluir o conteúdo.", AlertType.ERROR);
                        }
                    });
                }

                @Override
                public void onFailure(Void reason) {
                    blockActions = false;
                }
            });
        }
    }

    @Override
    public void duplicateCourse(CourseTO courseTO) {
        if (!blockActions) {
            blockActions = true;

            confirmModal.showModal(
                    "Tem certeza que deseja duplicar o conteúdo \"" + courseTO.getCourse().getName() + "\"?",
                    new com.google.gwt.core.client.Callback<Void, Void>() {
                        @Override
                        public void onSuccess(Void result) {

                            bus.fireEvent(new ShowPacifierEvent(true));
                            session.course(courseTO.getCourse().getUUID()).copy(new Callback<Course>() {
                                @Override
                                public void ok(Course course) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show("Conteúdo duplicado com sucesso.");
                                    placeController.goTo(new AdminCoursePlace(course.getUUID()));
                                }

                                @Override
                                public void internalServerError(KornellErrorTO error) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show("Erro ao tentar duplicar o conteúdo.", AlertType.ERROR);
                                }
                            });
                        }

                        @Override
                        public void onFailure(Void reason) {
                            blockActions = false;
                        }
                    });
        }
    }

}