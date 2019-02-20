package kornell.gui.client.presentation.admin.courseclass.courseclasses;

import java.util.List;
import java.util.logging.Logger;

import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.*;
import kornell.core.entity.role.RoleCategory;
import kornell.core.entity.role.RoleType;
import kornell.core.error.KornellErrorTO;
import kornell.core.to.CourseClassTO;
import kornell.core.to.CourseClassesTO;
import kornell.core.to.EnrollmentTO;
import kornell.core.to.TOFactory;
import kornell.core.util.StringUtils;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.presentation.admin.common.ConfirmModalView;
import kornell.gui.client.presentation.admin.courseclass.courseclass.AdminCourseClassPlace;
import kornell.gui.client.util.ClientProperties;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.view.KornellNotification;
import kornell.gui.client.util.view.table.PaginationPresenterImpl;

public class AdminCourseClassesPresenter extends PaginationPresenterImpl<CourseClassTO>
implements AdminCourseClassesView.Presenter {
    Logger logger = Logger.getLogger(AdminCourseClassesPresenter.class.getName());
    private AdminCourseClassesView view;
    FormHelper formHelper;
    private KornellSession session;
    private PlaceController placeController;
    private Place defaultPlace;
    TOFactory toFactory;
    private ViewFactory viewFactory;
    private CourseClassesTO courseClassesTO;
    private EventBus bus;
    private boolean blockActions;
    private ConfirmModalView confirmModal;

    public AdminCourseClassesPresenter(KornellSession session, EventBus bus, PlaceController placeController,
            Place defaultPlace, TOFactory toFactory, ViewFactory viewFactory) {
        this.session = session;
        this.bus = bus;
        this.placeController = placeController;
        this.defaultPlace = defaultPlace;
        this.toFactory = toFactory;
        this.viewFactory = viewFactory;
        this.confirmModal = viewFactory.getConfirmModalView();
        formHelper = new FormHelper();

        init();
    }

    private void init() {
        if (RoleCategory.hasRole(session.getCurrentUser().getRoles(), RoleType.courseClassAdmin)
                || RoleCategory.hasRole(session.getCurrentUser().getRoles(), RoleType.courseClassObserver)
                || RoleCategory.hasRole(session.getCurrentUser().getRoles(), RoleType.tutor)
                || session.isPublisher()
                || session.isInstitutionCourseClassesAdmin()
                || session.isInstitutionCourseClassesObserver()
                || session.isInstitutionAdmin()) {
            initializeProperties("cc.name");
            view = getView();
            view.setPresenter(this);
            String selectedCourseClass = "";
            updateCourseClass(selectedCourseClass);
        } else {
            logger.warning("Hey, only admins are allowed to see this! " + this.getClass().getName());
            placeController.goTo(defaultPlace);
        }
    }

    @Override
    public void updateCourseClass(final String courseClassUUID) {
        bus.fireEvent(new ShowPacifierEvent(true));
        session.courseClasses().getAdministratedCourseClassesTOPaged(pageSize, pageNumber, searchTerm, orderBy, asc,
                new Callback<CourseClassesTO>() {
            @Override
            public void ok(CourseClassesTO to) {
                courseClassesTO = to;
                view.setCourseClasses(courseClassesTO.getCourseClasses());
                bus.fireEvent(new ShowPacifierEvent(false));
                updateProperties();
            }
        });
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }

    private AdminCourseClassesView getView() {
        return viewFactory.getAdminCourseClassesView();
    }

    @Override
    public void updateData() {
        updateCourseClass("");
    }

    @Override
    public String getClientPropertyName(String property) {
        return session.getAdminHomePropertyPrefix() + "courseClasses" + ClientProperties.SEPARATOR + property;
    }

    @Override
    public int getTotalRowCount() {
        return StringUtils.isNone(getSearchTerm()) ? courseClassesTO.getCount() : courseClassesTO.getSearchCount();
    }

    @Override
    public List<CourseClassTO> getRowData() {
        return courseClassesTO.getCourseClasses();
    }

    @Override
    public void deleteCourseClass(CourseClassTO courseClassTO) {
        if (!blockActions) {
            blockActions = true;

            confirmModal.showModal(
                    "Tem certeza que deseja excluir o grupo \"" + courseClassTO.getCourseClass().getName() + "\"?",
                    new com.google.gwt.core.client.Callback<Void, Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bus.fireEvent(new ShowPacifierEvent(true));
                            session.courseClass(courseClassTO.getCourseClass().getUUID())
                            .delete(new Callback<CourseClass>() {
                                @Override
                                public void ok(CourseClass to) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show("Grupo excluído com sucesso.");
                                    updateData();
                                }

                                @Override
                                public void internalServerError(KornellErrorTO error) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show("Erro ao tentar excluir o grupo.",
                                            AlertType.ERROR);
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
    public boolean showActionButton(String actionName, CourseClassTO courseClassTO) {
        if ("Gerenciar".equals(actionName)) {
            return session.isCourseClassAdmin(courseClassTO.getCourseClass().getUUID());
        } else if ("Duplicar".equals(actionName)) {
            return session.isInstitutionAdmin();
        } else if ("Excluir".equals(actionName)) {
            return session.isInstitutionAdmin() && courseClassTO.getEnrollmentCount() == 0;
        }
        return false;
    }

    @Override
    public void duplicateCourseClass(CourseClassTO courseClassTO) {
        if (!blockActions) {
            blockActions = false;

            confirmModal.showModal(
                    "Tem certeza que deseja duplicar o grupo \"" + courseClassTO.getCourseClass().getName() + "\"?",
                    new com.google.gwt.core.client.Callback<Void, Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bus.fireEvent(new ShowPacifierEvent(true));
                            session.courseClass(courseClassTO.getCourseClass().getUUID())
                            .copy(new Callback<CourseClass>() {
                                @Override
                                public void ok(CourseClass courseClass) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show("Grupo duplicado com sucesso.");
                                    placeController.goTo(new AdminCourseClassPlace(courseClass.getUUID()));
                                }

                                @Override
                                public void internalServerError(KornellErrorTO error) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show("Erro ao tentar duplicar o grupo.",
                                            AlertType.ERROR);
                                }

                                @Override
                                public void conflict(KornellErrorTO error) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show(
                                            "Erro ao tentar duplicar o grupo. Verifique se já existe um grupo com o nome \""
                                                    + courseClassTO.getCourseClass().getName() + "\" (2).",
                                                    AlertType.ERROR, 5000);
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