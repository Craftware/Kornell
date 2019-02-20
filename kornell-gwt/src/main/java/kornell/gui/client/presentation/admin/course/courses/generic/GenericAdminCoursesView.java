package kornell.gui.client.presentation.admin.course.courses.generic;

import static com.google.gwt.dom.client.BrowserEvents.CLICK;

import java.util.LinkedList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Button;
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
import kornell.core.to.CourseTO;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.presentation.admin.course.course.AdminCoursePlace;
import kornell.gui.client.presentation.admin.course.course.AdminCourseView;
import kornell.gui.client.presentation.admin.course.courses.AdminCoursesView;
import kornell.gui.client.util.view.table.KornellTable;

public class GenericAdminCoursesView extends Composite implements AdminCoursesView {

    interface MyUiBinder extends UiBinder<Widget, GenericAdminCoursesView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private KornellTable<CourseTO> table;
    private AdminCoursesView.Presenter presenter;
    private AdminCourseView view;
    private PlaceController placeCtrl;

    @UiField
    FlowPanel adminHomePanel;
    @UiField
    Label title;
    @UiField
    FlowPanel coursesPanel;
    @UiField
    FlowPanel coursesWrapper;
    @UiField
    FlowPanel createCoursePanel;
    @UiField
    Button btnAddCourse;

    public GenericAdminCoursesView(final KornellSession session, final EventBus bus, final PlaceController placeCtrl,
            final ViewFactory viewFactory) {
        this.placeCtrl = placeCtrl;
        initWidget(uiBinder.createAndBindUi(this));
        btnAddCourse.setText("Criar Novo Conteúdo");

        bus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
            @Override
            public void onPlaceChange(PlaceChangeEvent event) {
                if (createCoursePanel.getWidgetCount() > 0) {
                    createCoursePanel.clear();
                }
                coursesPanel.setVisible(true);
                coursesWrapper.setVisible(true);
                view = null;
            }
        });

        btnAddCourse.setVisible(session.hasPublishingRole());
        btnAddCourse.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (session.hasPublishingRole()) {
                    coursesPanel.setVisible(false);
                    coursesWrapper.setVisible(false);
                    view = viewFactory.getAdminCourseView();
                    view.setPresenter(viewFactory.getAdminCoursePresenter());
                    view.init();
                    createCoursePanel.add(view);
                }
            }
        });
    }

    private void initTable() {
        table = new KornellTable<>(presenter, "coursesCellTable");

        table.initColumn("Código", 20, "c.code", new TextColumn<CourseTO>() {
            @Override
            public String getValue(CourseTO courseTO) {
                return courseTO.getCourse().getCode();
            }
        });

        table.initColumn("Conteúdo", 20, "c.name", new Column<CourseTO, CourseTO>(buildCourseCell()) {
            @Override
            public CourseTO getValue(CourseTO courseTO) {
                return courseTO;
            }
        });

        table.initColumn("Descrição", 35, "c.description", new TextColumn<CourseTO>() {
            @Override
            public String getValue(CourseTO courseTO) {
                String description = courseTO.getCourse().getDescription();
                if (description.length() > 100) {
                    description = description.substring(0, 90) + " ...";
                }
                return description;
            }
        });

        table.initColumn("Tipo", 8, "c.contentSpec", new TextColumn<CourseTO>() {
            @Override
            public String getValue(CourseTO courseTO) {
                return courseTO.getCourse().getContentSpec().toString();
            }
        });

        table.initColumn("Versões", 7, new TextColumn<CourseTO>() {
            @Override
            public String getValue(CourseTO courseTO) {
                return "" + courseTO.getCourseVersionsCount();
            }
        });

        table.initColumn("Ações", 10, new Column<CourseTO, CourseTO>(buildActionsCell()) {
            @Override
            public CourseTO getValue(CourseTO courseTO) {
                return courseTO;
            }
        });

        table.onColumnSetupFinished();
    }

    private CompositeCell<CourseTO> buildCourseCell() {
        List<HasCell<CourseTO, ?>> cellsC = new LinkedList<>();
        cellsC.add(new CourseLinkHasCell(CourseDetailsEntityType.COURSE.toString(), getGoToCourseDelegate()));
        CompositeCell<CourseTO> cellC = new CompositeCell<>(cellsC);
        return cellC;
    }

    private CompositeCell<CourseTO> buildActionsCell() {
        List<HasCell<CourseTO, ?>> cells = new LinkedList<>();
        cells.add(new CourseActionsHasCell("Gerenciar", getGoToCourseDelegate()));
        cells.add(new CourseActionsHasCell("Duplicar", getDuplicateCourseDelegate()));
        cells.add(new CourseActionsHasCell("Excluir", getDeleteCourseDelegate()));
        CompositeCell<CourseTO> cell = new CompositeCell<>(cells);
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
    public void setCourses(List<CourseTO> courseTOs) {
        coursesWrapper.clear();

        if (table == null) {
            initTable();
        }
        table.build(coursesWrapper, courseTOs);

        title.setText("Gerenciar Conteúdos   (" + presenter.getTotalRowCount() + ")");

        adminHomePanel.setVisible(true);
    }

    private Delegate<CourseTO> getDeleteCourseDelegate() {
        return new Delegate<CourseTO>() {
            @Override
            public void execute(CourseTO courseTO) {
                presenter.deleteCourse(courseTO);
            }
        };
    }

    private Delegate<CourseTO> getDuplicateCourseDelegate() {
        return new Delegate<CourseTO>() {
            @Override
            public void execute(CourseTO courseTO) {
                presenter.duplicateCourse(courseTO);
            }
        };
    }

    private Delegate<CourseTO> getGoToCourseDelegate() {
        return new Delegate<CourseTO>() {
            @Override
            public void execute(CourseTO courseTO) {
                placeCtrl.goTo(new AdminCoursePlace(courseTO.getCourse().getUUID()));
            }
        };
    }

    @SuppressWarnings("hiding")
    private class CourseActionsActionCell<CourseTO> extends ActionCell<CourseTO> {
        public CourseActionsActionCell(String message, Delegate<CourseTO> delegate) {
            super(message, delegate);
        }

        @Override
        public void onBrowserEvent(Context context, Element parent, CourseTO value, NativeEvent event,
                ValueUpdater<CourseTO> valueUpdater) {
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

    private class CourseActionsHasCell implements HasCell<CourseTO, CourseTO> {
        private CourseActionsActionCell<CourseTO> cell;

        public CourseActionsHasCell(String text, Delegate<CourseTO> delegate) {
            final String actionName = text;
            cell = new CourseActionsActionCell<CourseTO>(text, delegate) {
                @Override
                public void render(com.google.gwt.cell.client.Cell.Context context, CourseTO object,
                        SafeHtmlBuilder sb) {
                    if (!"Excluir".equals(actionName) || object.getCourseVersionsCount() == 0) {
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
        public Cell<CourseTO> getCell() {
            return cell;
        }

        @Override
        public FieldUpdater<CourseTO, CourseTO> getFieldUpdater() {
            return null;
        }

        @Override
        public CourseTO getValue(CourseTO object) {
            return object;
        }
    }

    private class CourseLinkHasCell implements HasCell<CourseTO, CourseTO> {
        private CourseActionsActionCell<CourseTO> cell;

        public CourseLinkHasCell(String text, Delegate<CourseTO> delegate) {
            final String entityType = text;
            cell = new CourseActionsActionCell<CourseTO>(text, delegate) {
                @Override
                public void render(com.google.gwt.cell.client.Cell.Context context, CourseTO courseTO,
                        SafeHtmlBuilder sb) {
                    sb.append(SafeHtmlUtils.fromTrustedString(buildButtonHTML(entityType, courseTO)));
                }

                private String buildButtonHTML(String entityType, CourseTO courseTO) {
                    Anchor anchor = new Anchor();
                    if (CourseDetailsEntityType.COURSE.toString().equals(entityType)) {
                        anchor.setText(courseTO.getCourse().getName());
                    }
                    return anchor.toString();
                }
            };
        }

        @Override
        public Cell<CourseTO> getCell() {
            return cell;
        }

        @Override
        public FieldUpdater<CourseTO, CourseTO> getFieldUpdater() {
            return null;
        }

        @Override
        public CourseTO getValue(CourseTO object) {
            return object;
        }
    }

}