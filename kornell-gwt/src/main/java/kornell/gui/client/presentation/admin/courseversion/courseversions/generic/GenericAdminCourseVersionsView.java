package kornell.gui.client.presentation.admin.courseversion.courseversions.generic;

import static com.google.gwt.dom.client.BrowserEvents.CLICK;

import java.util.LinkedList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.ActionCell.Delegate;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.KornellSession;
import kornell.core.entity.CourseDetailsEntityType;
import kornell.core.to.CourseVersionTO;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.presentation.admin.common.ConfirmModalView;
import kornell.gui.client.presentation.admin.course.course.AdminCoursePlace;
import kornell.gui.client.presentation.admin.courseversion.courseversion.AdminCourseVersionPlace;
import kornell.gui.client.presentation.admin.courseversion.courseversion.AdminCourseVersionPresenter;
import kornell.gui.client.presentation.admin.courseversion.courseversion.AdminCourseVersionView;
import kornell.gui.client.presentation.admin.courseversion.courseversions.AdminCourseVersionsView;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.view.table.KornellTable;

public class GenericAdminCourseVersionsView extends Composite implements AdminCourseVersionsView {

    interface MyUiBinder extends UiBinder<Widget, GenericAdminCourseVersionsView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private PlaceController placeCtrl;
    private KornellTable<CourseVersionTO> table;
    private AdminCourseVersionsView.Presenter presenter;
    private FormHelper formHelper = GWT.create(FormHelper.class);

    @UiField
    FlowPanel adminHomePanel;
    @UiField
    Label title;
    @UiField
    FlowPanel courseVersionsPanel;
    @UiField
    FlowPanel courseVersionsWrapper;
    @UiField
    FlowPanel createVersionPanel;
    @UiField
    Button btnAddCourseVersion;

    ConfirmModalView confirmModal;

    Tab adminsTab;
    FlowPanel adminsPanel;
    private AdminCourseVersionView view;
    private AdminCourseVersionPresenter adminCourseVersionPresenter;

    public GenericAdminCourseVersionsView(final KornellSession session, final EventBus bus,
            final PlaceController placeCtrl, final ViewFactory viewFactory) {
        this.placeCtrl = placeCtrl;
        initWidget(uiBinder.createAndBindUi(this));
        btnAddCourseVersion.setText("Criar Nova Versão");

        bus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
            @Override
            public void onPlaceChange(PlaceChangeEvent event) {
                if (createVersionPanel.getWidgetCount() > 0) {
                    createVersionPanel.clear();
                }
                courseVersionsPanel.setVisible(true);
                courseVersionsWrapper.setVisible(true);
                view = null;
            }
        });

        btnAddCourseVersion.setVisible(session.hasPublishingRole());
        btnAddCourseVersion.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (session.hasPublishingRole()) {
                    courseVersionsPanel.setVisible(false);
                    courseVersionsWrapper.setVisible(false);
                    adminCourseVersionPresenter = viewFactory.getAdminCourseVersionPresenter();
                    view = adminCourseVersionPresenter.getView();
                    view.setPresenter(adminCourseVersionPresenter);
                    view.init();
                    createVersionPanel.add(view);
                }
            }
        });
    }

    private void initTable() {
        table = new KornellTable<>(presenter, "courseVersionsCellTable");

        table.initColumn("Conteúdo", 20, "c.name", new Column<CourseVersionTO, CourseVersionTO>(buildCourseCell()) {
            @Override
            public CourseVersionTO getValue(CourseVersionTO courseVersionTO) {
                return courseVersionTO;
            }
        });

        table.initColumn("Versão", 20, "cv.name",
                new Column<CourseVersionTO, CourseVersionTO>(buildCourseVersionCell()) {
            @Override
            public CourseVersionTO getValue(CourseVersionTO courseVersionTO) {
                return courseVersionTO;
            }
        });

        table.initColumn("Código", 20, "cv.distributionPrefix", new TextColumn<CourseVersionTO>() {
            @Override
            public String getValue(CourseVersionTO courseVersionTO) {
                return courseVersionTO.getCourseVersion().getDistributionPrefix();
            }
        });

        table.initColumn("Status", 10, "cv.disabled", new TextColumn<CourseVersionTO>() {
            @Override
            public String getValue(CourseVersionTO courseVersionTO) {
                return courseVersionTO.getCourseVersion().isDisabled() ? "Desativada" : "Ativa";
            }
        });

        table.initColumn("Criada em", 8, "cv.versionCreatedAt", false, new TextColumn<CourseVersionTO>() {
            @Override
            public String getValue(CourseVersionTO courseVersionTO) {
                return formHelper.dateToString(courseVersionTO.getCourseVersion().getVersionCreatedAt());
            }
        });

        table.initColumn("Grupos", 8, new TextColumn<CourseVersionTO>() {
            @Override
            public String getValue(CourseVersionTO courseVersionTO) {
                return "" + courseVersionTO.getCourseClassesCount();
            }
        });

        table.initColumn("Ações", 14, new Column<CourseVersionTO, CourseVersionTO>(buildActionsCell()) {
            @Override
            public CourseVersionTO getValue(CourseVersionTO courseTO) {
                return courseTO;
            }
        });

        table.onColumnSetupFinished();
    }

    private CompositeCell<CourseVersionTO> buildCourseVersionCell() {
        List<HasCell<CourseVersionTO, ?>> cellsCV = new LinkedList<HasCell<CourseVersionTO, ?>>();
        cellsCV.add(new CourseVersionLinkHasCell(CourseDetailsEntityType.COURSE_VERSION.toString(),
                getGoToCourseVersionDelegate()));
        CompositeCell<CourseVersionTO> cellCV = new CompositeCell<CourseVersionTO>(cellsCV);
        return cellCV;
    }

    private CompositeCell<CourseVersionTO> buildCourseCell() {
        List<HasCell<CourseVersionTO, ?>> cellsC = new LinkedList<HasCell<CourseVersionTO, ?>>();
        cellsC.add(new CourseVersionLinkHasCell(CourseDetailsEntityType.COURSE.toString(), getGoToCourseDelegate()));
        CompositeCell<CourseVersionTO> cellC = new CompositeCell<CourseVersionTO>(cellsC);
        return cellC;
    }

    private CompositeCell<CourseVersionTO> buildActionsCell() {
        List<HasCell<CourseVersionTO, ?>> cells = new LinkedList<HasCell<CourseVersionTO, ?>>();
        cells.add(new CourseVersionActionsHasCell("Gerenciar", getGoToCourseVersionDelegate()));
        cells.add(new CourseVersionActionsHasCell("Reiniciar Matrículas do Grupo Sandbox", getResetSandboxEnrollmentsDelegate()));
        cells.add(new CourseVersionActionsHasCell("Duplicar", getDuplicateCourseVersionDelegate()));
        cells.add(new CourseVersionActionsHasCell("Excluir", getDeleteCourseVersionDelegate()));
        CompositeCell<CourseVersionTO> cell = new CompositeCell<CourseVersionTO>(cells);
        return cell;
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (table != null) {
            table.resetSearchTerm();
        }
    }

    @Override
    public void setCourseVersions(List<CourseVersionTO> courseVersionTOs) {
        courseVersionsWrapper.clear();

        if (table == null) {
            initTable();
        }
        table.build(courseVersionsWrapper, courseVersionTOs);

        title.setText("Gerenciar Versões de Conteúdo (" + presenter.getTotalRowCount() + ")");

        adminHomePanel.setVisible(true);
    }

    private Delegate<CourseVersionTO> getDeleteCourseVersionDelegate() {
        return new Delegate<CourseVersionTO>() {

            @Override
            public void execute(CourseVersionTO courseVersionTO) {
                presenter.deleteCourseVersion(courseVersionTO);
            }
        };
    }

    private Delegate<CourseVersionTO> getResetSandboxEnrollmentsDelegate() {
        return new Delegate<CourseVersionTO>() {

            @Override
            public void execute(CourseVersionTO courseVersionTO) {
                presenter.resetSandboxEnrollments(courseVersionTO);
            }
        };
    }

    private Delegate<CourseVersionTO> getDuplicateCourseVersionDelegate() {
        return new Delegate<CourseVersionTO>() {

            @Override
            public void execute(CourseVersionTO courseVersionTO) {
                presenter.duplicateCourseVersion(courseVersionTO);
            }
        };
    }

    private Delegate<CourseVersionTO> getGoToCourseDelegate() {
        return new Delegate<CourseVersionTO>() {

            @Override
            public void execute(CourseVersionTO courseVersionTO) {
                placeCtrl.goTo(new AdminCoursePlace(courseVersionTO.getCourseTO().getCourse().getUUID()));
            }
        };
    }

    private Delegate<CourseVersionTO> getGoToCourseVersionDelegate() {
        return new Delegate<CourseVersionTO>() {
            @Override
            public void execute(CourseVersionTO courseVersionTO) {
                placeCtrl.goTo(new AdminCourseVersionPlace(courseVersionTO.getCourseVersion().getUUID()));
            }
        };
    }

    @SuppressWarnings("hiding")
    private class CourseVersionActionsActionCell<CourseVersionTO> extends ActionCell<CourseVersionTO> {
        public CourseVersionActionsActionCell(String message, Delegate<CourseVersionTO> delegate) {
            super(message, delegate);
        }

        @Override
        public void onBrowserEvent(Context context, Element parent, CourseVersionTO value, NativeEvent event,
                ValueUpdater<CourseVersionTO> valueUpdater) {
            event.stopPropagation();
            event.preventDefault();
            super.onBrowserEvent(context, parent, value, event, valueUpdater);
            if (CLICK.equals(event.getType())) {
                EventTarget eventTarget = event.getEventTarget();
                if (!Element.is(eventTarget)) {
                    return;
                }
                if (parent.getFirstChildElement().isOrHasChild(Element.as(eventTarget))) {
                    // Ignore clicks that occur outside of the main element.
                    onEnterKeyDown(context, parent, value, event, valueUpdater);
                }
            }
        }
    }

    private class CourseVersionActionsHasCell implements HasCell<CourseVersionTO, CourseVersionTO> {
        private CourseVersionActionsActionCell<CourseVersionTO> cell;

        public CourseVersionActionsHasCell(String text, Delegate<CourseVersionTO> delegate) {
            final String actionName = text;
            cell = new CourseVersionActionsActionCell<CourseVersionTO>(text, delegate) {
                @Override
                public void render(com.google.gwt.cell.client.Cell.Context context, CourseVersionTO object,
                        SafeHtmlBuilder sb) {
                    if (!"Excluir".equals(actionName) || object.getCourseClassesCount() == 0) {
                        SafeHtml html = SafeHtmlUtils.fromTrustedString(buildButtonHTML(actionName));
                        sb.append(html);
                    } else {
                        sb.appendEscaped("");
                    }
                }

                private String buildButtonHTML(String actionName) {
                    Button btn = new Button();
                    btn.setSize(ButtonSize.SMALL);
                    btn.setTitle(actionName);
                    if ("Gerenciar".equals(actionName)) {
                        btn.setIcon(IconType.COG);
                        btn.addStyleName("btnAction");
                    } else if ("Excluir".equals(actionName)) {
                        btn.setIcon(IconType.TRASH);
                        btn.addStyleName("btnNotSelected");
                    } else if ("Duplicar".equals(actionName)) {
                        btn.setIcon(IconType.COPY);
                        btn.addStyleName("btnNotSelected");
                    } else if ("Reiniciar Matrículas do Grupo Sandbox".equals(actionName)) {
                        btn.setIcon(IconType.ERASER);
                        btn.addStyleName("btnNotSelected");
                    }
                    btn.addStyleName("btnIconSolo");
                    return btn.toString();
                }
            };
        }

        @Override
        public Cell<CourseVersionTO> getCell() {
            return cell;
        }

        @Override
        public FieldUpdater<CourseVersionTO, CourseVersionTO> getFieldUpdater() {
            return null;
        }

        @Override
        public CourseVersionTO getValue(CourseVersionTO object) {
            return object;
        }
    }

    private class CourseVersionLinkHasCell implements HasCell<CourseVersionTO, CourseVersionTO> {
        private CourseVersionActionsActionCell<CourseVersionTO> cell;

        public CourseVersionLinkHasCell(String text, Delegate<CourseVersionTO> delegate) {
            final String actionName = text;
            cell = new CourseVersionActionsActionCell<CourseVersionTO>(text, delegate) {
                @Override
                public void render(com.google.gwt.cell.client.Cell.Context context, CourseVersionTO courseVersionTO,
                        SafeHtmlBuilder sb) {
                    if (!"Excluir".equals(actionName) || courseVersionTO.getCourseClassesCount() == 0) {
                        SafeHtml html = SafeHtmlUtils.fromTrustedString(buildButtonHTML(actionName, courseVersionTO));
                        sb.append(html);
                    } else {
                        sb.appendEscaped("");
                    }
                }

                private String buildButtonHTML(String text, CourseVersionTO courseVersionTO) {
                    Anchor anchor = new Anchor();
                    if (CourseDetailsEntityType.COURSE.toString().equals(text)) {
                        anchor.setText(courseVersionTO.getCourseTO().getCourse().getName());
                    } else if (CourseDetailsEntityType.COURSE_VERSION.toString().equals(text)) {
                        anchor.setText(courseVersionTO.getCourseVersion().getName());
                    }
                    return anchor.toString();
                }
            };
        }

        @Override
        public Cell<CourseVersionTO> getCell() {
            return cell;
        }

        @Override
        public FieldUpdater<CourseVersionTO, CourseVersionTO> getFieldUpdater() {
            return null;
        }

        @Override
        public CourseVersionTO getValue(CourseVersionTO object) {
            return object;
        }
    }

}