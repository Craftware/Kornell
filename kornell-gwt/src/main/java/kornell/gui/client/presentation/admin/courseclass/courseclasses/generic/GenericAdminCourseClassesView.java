package kornell.gui.client.presentation.admin.courseclass.courseclasses.generic;

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
import kornell.core.to.CourseClassTO;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.presentation.admin.common.ConfirmModalView;
import kornell.gui.client.presentation.admin.course.course.AdminCoursePlace;
import kornell.gui.client.presentation.admin.courseclass.courseclass.AdminCourseClassPlace;
import kornell.gui.client.presentation.admin.courseclass.courseclass.generic.GenericCourseClassConfigView;
import kornell.gui.client.presentation.admin.courseclass.courseclasses.AdminCourseClassesView;
import kornell.gui.client.presentation.admin.courseversion.courseversion.AdminCourseVersionPlace;
import kornell.gui.client.util.EnumTranslator;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.view.table.KornellTable;

public class GenericAdminCourseClassesView extends Composite implements AdminCourseClassesView {

    interface MyUiBinder extends UiBinder<Widget, GenericAdminCourseClassesView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private PlaceController placeCtrl;
    private KornellTable<CourseClassTO> table;
    private AdminCourseClassesView.Presenter presenter;
    private FormHelper formHelper = GWT.create(FormHelper.class);

    @UiField
    FlowPanel adminHomePanel;
    @UiField
    Label title;
    @UiField
    FlowPanel courseClassesPanel;
    @UiField
    FlowPanel courseClassesWrapper;
    @UiField
    FlowPanel createClassPanel;
    @UiField
    Button btnAddCourseClass;

    ConfirmModalView confirmModal;

    Tab adminsTab;
    FlowPanel adminsPanel;

    public GenericAdminCourseClassesView(final KornellSession session, final EventBus bus,
            final PlaceController placeCtrl, final ViewFactory viewFactory) {
        this.placeCtrl = placeCtrl;
        initWidget(uiBinder.createAndBindUi(this));
        btnAddCourseClass.setText("Criar Novo Grupo");

        bus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
            @Override
            public void onPlaceChange(PlaceChangeEvent event) {
                if (createClassPanel.getWidgetCount() > 0) {
                    createClassPanel.clear();
                    courseClassesPanel.setVisible(true);
                    courseClassesWrapper.setVisible(true);
                }
            }
        });

        btnAddCourseClass.setVisible(session.isInstitutionAdmin());
        btnAddCourseClass.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (session.isInstitutionAdmin()) {
                    courseClassesPanel.setVisible(false);
                    courseClassesWrapper.setVisible(false);
                    createClassPanel.add(new GenericCourseClassConfigView(session, bus, placeCtrl,
                            viewFactory.getAdminCourseClassPresenter(), null));
                }
            }
        });
    }

    private void initTable() {
        table = new KornellTable<>(presenter, "courseClassesCellTable");

        table.initColumn("Conteúdo", 20, "c.name", new Column<CourseClassTO, CourseClassTO>(buildCourseCell()) {
            @Override
            public CourseClassTO getValue(CourseClassTO courseClassTO) {
                return courseClassTO;
            }
        });

        table.initColumn("Versão", 15, "cv.name", new Column<CourseClassTO, CourseClassTO>(buildCourseVersionCell()) {
            @Override
            public CourseClassTO getValue(CourseClassTO courseClassTO) {
                return courseClassTO;
            }
        });

        table.initColumn("Grupo", 25, "cc.name", new Column<CourseClassTO, CourseClassTO>(buildCourseClassCell()) {
            @Override
            public CourseClassTO getValue(CourseClassTO courseClassTO) {
                return courseClassTO;
            }
        });

        table.initColumn("Status", 10, "cc.state", new TextColumn<CourseClassTO>() {
            @Override
            public String getValue(CourseClassTO courseClassTO) {
                String value = EnumTranslator.translateEnum(courseClassTO.getCourseClass().getState());
                value += courseClassTO.getCourseClass().isInvisible() ? " / Invísivel" : "";
                value += courseClassTO.getCourseClass().isPublicClass() ? " / Pública" : "";
                return value;
            }
        });

        table.initColumn("Criada em", 10, "cc.createdAt", false, new TextColumn<CourseClassTO>() {
            @Override
            public String getValue(CourseClassTO courseClassTO) {
                return formHelper.dateToString(courseClassTO.getCourseClass().getCreatedAt());
            }
        });

        table.initColumn("Matrículas", 10, new TextColumn<CourseClassTO>() {
            @Override
            public String getValue(CourseClassTO courseClassTO) {
                String text = courseClassTO.getEnrollmentCount() + " (por ";
                text += EnumTranslator.translateEnum(courseClassTO.getCourseClass().getRegistrationType()) + ")";
                return text;
            }
        });

        table.initColumn("Ações", 10, new Column<CourseClassTO, CourseClassTO>(buildActionsCell()) {
            @Override
            public CourseClassTO getValue(CourseClassTO courseTO) {
                return courseTO;
            }
        });

        table.onColumnSetupFinished();
    }

    private CompositeCell<CourseClassTO> buildCourseClassCell() {
        List<HasCell<CourseClassTO, ?>> cellsCC = new LinkedList<HasCell<CourseClassTO, ?>>();
        cellsCC.add(new CourseClassLinkHasCell(CourseDetailsEntityType.COURSE_CLASS.toString(),
                getGoToCourseClassDelegate()));
        CompositeCell<CourseClassTO> cellCC = new CompositeCell<CourseClassTO>(cellsCC);
        return cellCC;
    }

    private CompositeCell<CourseClassTO> buildCourseVersionCell() {
        List<HasCell<CourseClassTO, ?>> cellsCV = new LinkedList<HasCell<CourseClassTO, ?>>();
        cellsCV.add(new CourseClassLinkHasCell(CourseDetailsEntityType.COURSE_VERSION.toString(),
                getGoToCourseVersionDelegate()));
        CompositeCell<CourseClassTO> cellCV = new CompositeCell<CourseClassTO>(cellsCV);
        return cellCV;
    }

    private CompositeCell<CourseClassTO> buildCourseCell() {
        List<HasCell<CourseClassTO, ?>> cellsC = new LinkedList<HasCell<CourseClassTO, ?>>();
        cellsC.add(new CourseClassLinkHasCell(CourseDetailsEntityType.COURSE.toString(), getGoToCourseDelegate()));
        CompositeCell<CourseClassTO> cellC = new CompositeCell<CourseClassTO>(cellsC);
        return cellC;
    }

    private CompositeCell<CourseClassTO> buildActionsCell() {
        List<HasCell<CourseClassTO, ?>> cells = new LinkedList<HasCell<CourseClassTO, ?>>();
        cells.add(new CourseClassActionsHasCell("Gerenciar", getGoToCourseClassDelegate()));
        cells.add(new CourseClassActionsHasCell("Duplicar", getDuplicateCourseClassDelegate()));
        cells.add(new CourseClassActionsHasCell("Excluir", getDeleteCourseClassDelegate()));
        CompositeCell<CourseClassTO> cell = new CompositeCell<CourseClassTO>(cells);
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
    public void setCourseClasses(List<CourseClassTO> courseClassTOs) {
        courseClassesWrapper.clear();

        if (table == null) {
            initTable();
        }
        table.build(courseClassesWrapper, courseClassTOs);

        title.setText("Gerenciar Grupos (" + presenter.getTotalRowCount() + ")");
        adminHomePanel.setVisible(true);
    }

    private Delegate<CourseClassTO> getGoToCourseDelegate() {
        return new Delegate<CourseClassTO>() {
            @Override
            public void execute(CourseClassTO courseClassTO) {
                placeCtrl.goTo(
                        new AdminCoursePlace(courseClassTO.getCourseVersionTO().getCourseTO().getCourse().getUUID()));
            }
        };
    }

    private Delegate<CourseClassTO> getGoToCourseVersionDelegate() {
        return new Delegate<CourseClassTO>() {
            @Override
            public void execute(CourseClassTO courseClassTO) {
                placeCtrl.goTo(
                        new AdminCourseVersionPlace(courseClassTO.getCourseVersionTO().getCourseVersion().getUUID()));
            }
        };
    }

    private Delegate<CourseClassTO> getGoToCourseClassDelegate() {
        return new Delegate<CourseClassTO>() {
            @Override
            public void execute(CourseClassTO courseClassTO) {
                placeCtrl.goTo(new AdminCourseClassPlace(courseClassTO.getCourseClass().getUUID()));
            }
        };
    }

    private Delegate<CourseClassTO> getDeleteCourseClassDelegate() {
        return new Delegate<CourseClassTO>() {

            @Override
            public void execute(CourseClassTO courseClassTO) {
                presenter.deleteCourseClass(courseClassTO);
            }
        };
    }

    private Delegate<CourseClassTO> getDuplicateCourseClassDelegate() {
        return new Delegate<CourseClassTO>() {

            @Override
            public void execute(CourseClassTO courseClassTO) {
                presenter.duplicateCourseClass(courseClassTO);
            }
        };
    }

    @SuppressWarnings("hiding")
    private class CourseClassActionsActionCell<CourseClassTO> extends ActionCell<CourseClassTO> {
        public CourseClassActionsActionCell(String message, Delegate<CourseClassTO> delegate) {
            super(message, delegate);
        }

        @Override
        public void onBrowserEvent(Context context, Element parent, CourseClassTO value, NativeEvent event,
                ValueUpdater<CourseClassTO> valueUpdater) {
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

    private class CourseClassActionsHasCell implements HasCell<CourseClassTO, CourseClassTO> {
        private CourseClassActionsActionCell<CourseClassTO> cell;

        public CourseClassActionsHasCell(String text, Delegate<CourseClassTO> delegate) {
            final String actionName = text;
            cell = new CourseClassActionsActionCell<CourseClassTO>(text, delegate) {
                @Override
                public void render(com.google.gwt.cell.client.Cell.Context context, CourseClassTO object,
                        SafeHtmlBuilder sb) {
                    if (presenter.showActionButton(actionName, object)) {
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
                    }
                    btn.addStyleName("btnIconSolo");
                    return btn.toString();
                }
            };
        }

        @Override
        public Cell<CourseClassTO> getCell() {
            return cell;
        }

        @Override
        public FieldUpdater<CourseClassTO, CourseClassTO> getFieldUpdater() {
            return null;
        }

        @Override
        public CourseClassTO getValue(CourseClassTO object) {
            return object;
        }
    }

    private class CourseClassLinkHasCell implements HasCell<CourseClassTO, CourseClassTO> {
        private CourseClassActionsActionCell<CourseClassTO> cell;

        public CourseClassLinkHasCell(String text, Delegate<CourseClassTO> delegate) {
            cell = new CourseClassActionsActionCell<CourseClassTO>(text, delegate) {
                @Override
                public void render(com.google.gwt.cell.client.Cell.Context context, CourseClassTO courseClassTO,
                        SafeHtmlBuilder sb) {
                    SafeHtml html = SafeHtmlUtils.fromTrustedString(buildButtonHTML(text, courseClassTO));
                    sb.append(html);
                }

                private String buildButtonHTML(String text, CourseClassTO courseClassTO) {
                    Anchor anchor = new Anchor();
                    if (CourseDetailsEntityType.COURSE.toString().equals(text)) {
                        anchor.setText(courseClassTO.getCourseVersionTO().getCourseTO().getCourse().getName());
                    } else if (CourseDetailsEntityType.COURSE_VERSION.toString().equals(text)) {
                        anchor.setText(courseClassTO.getCourseVersionTO().getCourseVersion().getName());
                    } else if (CourseDetailsEntityType.COURSE_CLASS.toString().equals(text)) {
                        anchor.setText(courseClassTO.getCourseClass().getName());
                    }
                    return anchor.toString();
                }
            };
        }

        @Override
        public Cell<CourseClassTO> getCell() {
            return cell;
        }

        @Override
        public FieldUpdater<CourseClassTO, CourseClassTO> getFieldUpdater() {
            return null;
        }

        @Override
        public CourseClassTO getValue(CourseClassTO object) {
            return object;
        }
    }

}