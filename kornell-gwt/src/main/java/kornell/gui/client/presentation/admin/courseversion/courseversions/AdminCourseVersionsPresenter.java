package kornell.gui.client.presentation.admin.courseversion.courseversions;

import java.util.List;
import java.util.logging.Logger;

import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.CourseVersion;
import kornell.core.error.KornellErrorTO;
import kornell.core.to.CourseVersionTO;
import kornell.core.to.CourseVersionsTO;
import kornell.core.to.TOFactory;
import kornell.core.util.StringUtils;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.presentation.admin.common.ConfirmModalView;
import kornell.gui.client.presentation.admin.courseversion.courseversion.AdminCourseVersionPlace;
import kornell.gui.client.util.ClientProperties;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.view.KornellNotification;
import kornell.gui.client.util.view.table.PaginationPresenterImpl;

public class AdminCourseVersionsPresenter extends PaginationPresenterImpl<CourseVersionTO>
implements AdminCourseVersionsView.Presenter {
    Logger logger = Logger.getLogger(AdminCourseVersionsPresenter.class.getName());
    private AdminCourseVersionsView view;
    FormHelper formHelper;
    private KornellSession session;
    private PlaceController placeCtrl;
    private Place defaultPlace;
    TOFactory toFactory;
    private ViewFactory viewFactory;
    private CourseVersionsTO courseVersionsTO;
    private EventBus bus;
    private boolean blockActions;
    private ConfirmModalView confirmModal;

    public AdminCourseVersionsPresenter(KornellSession session, EventBus bus, PlaceController placeCtrl,
            Place defaultPlace, TOFactory toFactory, ViewFactory viewFactory) {
        this.session = session;
        this.bus = bus;
        this.placeCtrl = placeCtrl;
        this.defaultPlace = defaultPlace;
        this.toFactory = toFactory;
        this.viewFactory = viewFactory;
        this.confirmModal = viewFactory.getConfirmModalView();
        formHelper = new FormHelper();
        init();
    }

    private void init() {
        if (session.hasPublishingRole()) {
            initializeProperties("cv.name");
            view = getView();
            view.setPresenter(this);
            bus.fireEvent(new ShowPacifierEvent(true));
            getCourseVersions();
        } else {
            logger.warning("Hey, only admins are allowed to see this! " + this.getClass().getName());
            placeCtrl.goTo(defaultPlace);
        }
    }

    private void getCourseVersions() {
        bus.fireEvent(new ShowPacifierEvent(true));
        session.courseVersions().get(pageSize, pageNumber, searchTerm, orderBy, asc, new Callback<CourseVersionsTO>() {
            @Override
            public void ok(CourseVersionsTO to) {
                courseVersionsTO = to;
                view.setCourseVersions(courseVersionsTO.getCourseVersionTOs());
                bus.fireEvent(new ShowPacifierEvent(false));
                updateProperties();
            }
        });
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }

    private AdminCourseVersionsView getView() {
        return viewFactory.getAdminCourseVersionsView();
    }

    @Override
    public void updateData() {
        getCourseVersions();
    }

    @Override
    public String getClientPropertyName(String property) {
        return session.getAdminHomePropertyPrefix() + "courseVersions" + ClientProperties.SEPARATOR + property;
    }

    @Override
    public int getTotalRowCount() {
        return StringUtils.isNone(getSearchTerm()) ? courseVersionsTO.getCount() : courseVersionsTO.getSearchCount();
    }

    @Override
    public List<CourseVersionTO> getRowData() {
        return courseVersionsTO.getCourseVersionTOs();
    }

    @Override
    public void deleteCourseVersion(CourseVersionTO courseVersionTO) {
        if (!blockActions) {
            blockActions = true;

            confirmModal.showModal(
                    "Tem certeza que deseja excluir a versão \"" + courseVersionTO.getCourseVersion().getName() + "\"?",
                    new com.google.gwt.core.client.Callback<Void, Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bus.fireEvent(new ShowPacifierEvent(true));
                            session.courseVersion(courseVersionTO.getCourseVersion().getUUID())
                            .delete(new Callback<CourseVersion>() {
                                @Override
                                public void ok(CourseVersion to) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show("Versão excluída com sucesso.");
                                    updateData();
                                }

                                @Override
                                public void internalServerError(KornellErrorTO error) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show("Erro ao tentar excluir a versão.",
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
    public void resetSandboxEnrollments(CourseVersionTO courseVersionTO) {
        if (!blockActions) {
            blockActions = true;

            confirmModal.showModal(
                    "Tem certeza que deseja reiniciar todas as matrículas do grupo \"SANDBOX\" da versão \"" + courseVersionTO.getCourseVersion().getName() + "\"? O progresso de todos os administradores e publicadores de conteúdo será reiniciado. Essa operação não pode ser desfeita.",
                    new com.google.gwt.core.client.Callback<Void, Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bus.fireEvent(new ShowPacifierEvent(true));
                            session.courseVersion(courseVersionTO.getCourseVersion().getUUID())
                            .resetSandbox(new Callback<Void>() {
                                @Override
                                public void ok(Void str) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show("As matrículas do grupo sandbox foram reiniciadas com sucesso.");
                                    updateData();
                                }

                                @Override
                                public void internalServerError(KornellErrorTO error) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show("Erro ao tentar reiniciar as matrículas do grupo sandbox.",
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
    public void duplicateCourseVersion(CourseVersionTO courseVersionTO) {
        if (!blockActions) {
            blockActions = true;

            confirmModal.showModal("Tem certeza que deseja duplicar a versão \""
                    + courseVersionTO.getCourseVersion().getName() + "\"?",
                    new com.google.gwt.core.client.Callback<Void, Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bus.fireEvent(new ShowPacifierEvent(true));
                            session.courseVersion(courseVersionTO.getCourseVersion().getUUID())
                            .copy(new Callback<CourseVersion>() {
                                @Override
                                public void ok(CourseVersion courseVersion) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show("Versão duplicada com sucesso.");
                                    placeCtrl.goTo(new AdminCourseVersionPlace(courseVersion.getUUID()));
                                }

                                @Override
                                public void internalServerError(KornellErrorTO error) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show("Erro ao tentar duplicar a versão.",
                                            AlertType.ERROR);
                                }

                                @Override
                                public void conflict(KornellErrorTO error) {
                                    blockActions = false;
                                    bus.fireEvent(new ShowPacifierEvent(false));
                                    KornellNotification.show(
                                            "Erro ao tentar duplicar a versão. Verifique se já existe uma versão com o nome \""
                                                    + courseVersionTO.getCourseVersion().getName() + "\" (2).",
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