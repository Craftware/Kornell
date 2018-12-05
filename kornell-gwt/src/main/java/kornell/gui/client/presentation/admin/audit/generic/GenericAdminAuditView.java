package kornell.gui.client.presentation.admin.audit.generic;

import static com.google.gwt.dom.client.BrowserEvents.CLICK;
import static kornell.core.util.StringUtils.mkurl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.ListBox;
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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.KornellSession;
import kornell.core.entity.AuditedEntityType;
import kornell.core.event.EntityChanged;
import kornell.core.to.EntityChangedEventsTO;
import kornell.core.util.StringUtils;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.presentation.admin.audit.AdminAuditPresenter;
import kornell.gui.client.presentation.admin.audit.AdminAuditView;
import kornell.gui.client.presentation.admin.course.course.AdminCoursePlace;
import kornell.gui.client.presentation.admin.courseclass.courseclass.AdminCourseClassPlace;
import kornell.gui.client.presentation.admin.courseversion.courseversion.AdminCourseVersionPlace;
import kornell.gui.client.presentation.admin.courseversion.courseversion.AdminCourseVersionView;
import kornell.gui.client.presentation.admin.institution.AdminInstitutionPlace;
import kornell.gui.client.presentation.profile.ProfilePlace;
import kornell.gui.client.util.ClientConstants;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.view.table.KornellPagination;

public class GenericAdminAuditView extends Composite implements AdminAuditView {

    Logger logger = Logger.getLogger(GenericAdminAuditView.class.getName());

    interface MyUiBinder extends UiBinder<Widget, GenericAdminAuditView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private PlaceController placeCtrl;
    final CellTable<EntityChanged> table;
    private KornellPagination<EntityChanged> pagination;
    private AdminAuditView.Presenter presenter;
    private FormHelper formHelper = GWT.create(FormHelper.class);
    private ListBox entityTypesList;
    private Timer updateTimer;
    private boolean jsondiffpatchLoaded;

    @UiField
    FlowPanel adminHomePanel;
    @UiField
    FlowPanel entitiesChangedPanel;
    @UiField
    FlowPanel entitiesChangedWrapper;
    @UiField
    FlowPanel diffWrapper;

    Tab adminsTab;
    FlowPanel adminsPanel;
    private AdminCourseVersionView view;
    private AdminAuditPresenter adminAuditPresenter;
    private FlowPanel tableTools;
    private EntityChangedEventsTO entityChangedEventsTO;

    public GenericAdminAuditView(final KornellSession session, final EventBus bus, final PlaceController placeCtrl,
            final ViewFactory viewFactory) {
        this.placeCtrl = placeCtrl;
        initWidget(uiBinder.createAndBindUi(this));
        table = new CellTable<EntityChanged>();

        bus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
            @Override
            public void onPlaceChange(PlaceChangeEvent event) {
                entitiesChangedPanel.setVisible(true);
                entitiesChangedWrapper.setVisible(true);
                view = null;
            }
        });

        displayDiff(null);
    }

    @Override
    public EntityChangedEventsTO getEntityChangedEventsTO() {
        return entityChangedEventsTO;
    }

    private void filter() {
        presenter.setPageNumber("1");
        presenter.setSearchTerm(entityTypesList.getValue());
        presenter.updateData();
    }

    private void initSearch() {
        if (entityTypesList == null) {
            entityTypesList = new ListBox();
            List<AuditedEntityType> auditedEntityTypes = Arrays.asList(AuditedEntityType.values());
            entityTypesList.addItem("[Selecione]", "");
            for (AuditedEntityType auditedEntityType : auditedEntityTypes) {
                entityTypesList.addItem(auditedEntityType.toString());
            }
            entityTypesList.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    displayDiff(null);
                    filter();
                }
            });
        }
    }

    private void initTable() {

        table.addStyleName("adminCellTable");
        table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);
        for (int i = 0; table.getColumnCount() > 0;) {
            table.removeColumn(i);
        }

        table.addColumn(new TextColumn<EntityChanged>() {
            @Override
            public String getValue(EntityChanged entityChanged) {
                return entityChanged.getEntityType().toString();
            }
        }, "Tipo");

        table.addColumn(new TextColumn<EntityChanged>() {
            @Override
            public String getValue(EntityChanged entityChanged) {
                return entityChanged.getEntityName();
            }
        }, "Nome");

        table.addColumn(new TextColumn<EntityChanged>() {
            @Override
            public String getValue(EntityChanged entityChanged) {
                return entityChanged.getFromUsername() + "(" + entityChanged.getFromPersonName() + ")";
            }
        }, "Usuário");

        table.addColumn(new TextColumn<EntityChanged>() {
            @Override
            public String getValue(EntityChanged entityChanged) {
                return DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(entityChanged.getEventFiredAt());
            }
        }, "Data");

        List<HasCell<EntityChanged, ?>> cells = new LinkedList<HasCell<EntityChanged, ?>>();
        cells.add(new AuditActionsHasCell("Comparar", getAuditDelegate()));
        cells.add(new AuditActionsHasCell("Gerenciar", getGoToPlaceDelegate()));

        CompositeCell<EntityChanged> cell = new CompositeCell<EntityChanged>(cells);
        table.addColumn(new Column<EntityChanged, EntityChanged>(cell) {
            @Override
            public EntityChanged getValue(EntityChanged entityChanged) {
                return entityChanged;
            }
        }, "Ações");

        // Add a selection model to handle user selection.
        final SingleSelectionModel<EntityChanged> selectionModel = new SingleSelectionModel<EntityChanged>();
        table.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            public void onSelectionChange(SelectionChangeEvent event) {
                EntityChanged selected = selectionModel.getSelectedObject();
                if (selected != null) {
                    displayDiff(selected);
                }
            }
        });
    }

    private void displayDiff(final EntityChanged entityChanged) {
        diffWrapper.setVisible(entityChanged != null);
        if (jsondiffpatchLoaded) {
            if (entityChanged != null)
                display(entityChanged.getFromValue(), entityChanged.getToValue());
        } else {
            ScriptInjector.fromUrl(mkurl(ClientConstants.JS_PATH, "jsondiffpatch.js"))
                    .setCallback(new com.google.gwt.core.client.Callback<Void, Exception>() {

                        public void onFailure(Exception reason) {
                            logger.severe("Screeenful script load failed.");
                        }

                        public void onSuccess(Void result) {
                            Timer screenfulJsTimer = new Timer() {
                                public void run() {
                                    if (entityChanged != null)
                                        display(entityChanged.getFromValue(), entityChanged.getToValue());
                                    jsondiffpatchLoaded = true;
                                }
                            };

                            // wait 2 secs after loading the javascript file
                            screenfulJsTimer.schedule((int) (3 * 1000));
                        }
                    }).setWindow(ScriptInjector.TOP_WINDOW).inject();
        }

    }

    private static native String display(String from, String to) /*-{
        $wnd.init(from, to);
    }-*/;

    public void goToEntityPlace(EntityChanged entityChanged) {
        switch (entityChanged.getEntityType()) {
        case person:
        case password:
            placeCtrl.goTo(new ProfilePlace(entityChanged.getEntityUUID(), false));
            break;
        case institution:
        case institutionAdmin:
        case institutionHostName:
        case institutionEmailWhitelist:
            placeCtrl.goTo(new AdminInstitutionPlace());
            break;
        case course:
            placeCtrl.goTo(new AdminCoursePlace(entityChanged.getEntityUUID()));
            break;
        case courseVersion:
            placeCtrl.goTo(new AdminCourseVersionPlace(entityChanged.getEntityUUID()));
            break;
        case courseClass:
        case courseClassAdmin:
        case courseClassObserver:
        case courseClassTutor:
            placeCtrl.goTo(new AdminCourseClassPlace(entityChanged.getEntityUUID()));
            break;
        default:
            break;
        }
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        pagination = new KornellPagination<EntityChanged>(table, presenter);
    }

    private Delegate<EntityChanged> getAuditDelegate() {
        return new Delegate<EntityChanged>() {
            @Override
            public void execute(EntityChanged entityChanged) {
                displayDiff(entityChanged);
            }
        };
    }

    private Delegate<EntityChanged> getGoToPlaceDelegate() {
        return new Delegate<EntityChanged>() {
            @Override
            public void execute(EntityChanged entityChanged) {
                goToEntityPlace(entityChanged);
            }
        };
    }

    private class AuditActionsActionCell<CourseVersion> extends ActionCell<EntityChanged> {

        public AuditActionsActionCell(String message, Delegate<EntityChanged> delegate) {
            super(message, delegate);
        }

        @Override
        public void onBrowserEvent(Context context, Element parent, EntityChanged value, NativeEvent event,
                ValueUpdater<EntityChanged> valueUpdater) {
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

    private class AuditActionsHasCell implements HasCell<EntityChanged, EntityChanged> {
        private AuditActionsActionCell<EntityChanged> cell;

        public AuditActionsHasCell(String text, Delegate<EntityChanged> delegate) {
            final String actionName = text;
            cell = new AuditActionsActionCell<EntityChanged>(text, delegate) {
                @Override
                public void render(com.google.gwt.cell.client.Cell.Context context, EntityChanged entityChanged,
                        SafeHtmlBuilder sb) {
                    if (!("Gerenciar".equals(actionName) && "DELETED_ENTITY".equals(entityChanged.getEntityName()))) {
                        SafeHtml html = SafeHtmlUtils.fromTrustedString(buildButtonHTML(actionName));
                        sb.append(html);
                    }
                }

                private String buildButtonHTML(String actionName) {
                    Button btn = new Button();
                    btn.setSize(ButtonSize.SMALL);
                    btn.setTitle(actionName);
                    if ("Comparar".equals(actionName)) {
                        btn.setIcon(IconType.EYE_OPEN);
                        btn.addStyleName("btnAction");
                    } else if ("Gerenciar".equals(actionName)) {
                        btn.setIcon(IconType.COG);
                        btn.addStyleName("btnAction");
                    }
                    btn.addStyleName("btnIconSolo");
                    return btn.toString();
                }
            };
        }

        @Override
        public Cell<EntityChanged> getCell() {
            return cell;
        }

        @Override
        public FieldUpdater<EntityChanged, EntityChanged> getFieldUpdater() {
            return null;
        }

        @Override
        public EntityChanged getValue(EntityChanged object) {
            return object;
        }
    }

    @Override
    public void setEntitiesChangedEvents(EntityChangedEventsTO entityChangedEventsTO) {
        this.entityChangedEventsTO = entityChangedEventsTO;
        table.setVisible(false);
        pagination.setVisible(true);
        VerticalPanel panel = new VerticalPanel();
        panel.setWidth("400");
        panel.add(table);

        initTableTools();

        entitiesChangedWrapper.clear();
        entitiesChangedWrapper.add(tableTools);
        entitiesChangedWrapper.add(panel);
        entitiesChangedWrapper.add(pagination);

        if (entityChangedEventsTO != null) {
            pagination.setRowData(entityChangedEventsTO.getEntitiesChanged(),
                    StringUtils.isSome(presenter.getSearchTerm()) ? entityChangedEventsTO.getSearchCount()
                            : entityChangedEventsTO.getCount());
            initTable();
            table.setVisible(true);
            pagination.setVisible(true);
        }

        adminHomePanel.setVisible(true);
    }

    private void initTableTools() {
        if (tableTools == null) {
            tableTools = new FlowPanel();
            final ListBox pageSizeListBox = new ListBox();
            // pageSizeListBox.addItem("1");
            // pageSizeListBox.addItem("10");
            pageSizeListBox.addStyleName("pageSizeListBox");
            pageSizeListBox.addItem("20");
            pageSizeListBox.addItem("50");
            pageSizeListBox.addItem("100");
            pageSizeListBox.setSelectedValue(presenter.getPageSize());
            pageSizeListBox.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    if (pageSizeListBox.getValue().matches("[0-9]*")) {
                        presenter.setPageNumber("1");
                        presenter.setPageSize(pageSizeListBox.getValue());
                        presenter.updateData();
                    }
                }
            });

            initSearch();
            tableTools.addStyleName("marginTop25");
            tableTools.add(entityTypesList);
            tableTools.add(pageSizeListBox);
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                }
            });
        }
    }

}
