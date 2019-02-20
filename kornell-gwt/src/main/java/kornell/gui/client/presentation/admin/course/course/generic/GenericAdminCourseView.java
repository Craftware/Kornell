package kornell.gui.client.presentation.admin.course.course.generic;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.ContentSpec;
import kornell.core.entity.Course;
import kornell.core.entity.CourseDetailsEntityType;
import kornell.core.entity.EntityFactory;
import kornell.core.entity.InstitutionType;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.presentation.admin.assets.AdminAssetsPresenter;
import kornell.gui.client.presentation.admin.course.course.AdminCoursePlace;
import kornell.gui.client.presentation.admin.course.course.AdminCourseView;
import kornell.gui.client.presentation.admin.course.courses.AdminCoursesPlace;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.forms.formfield.KornellFormFieldWrapper;
import kornell.gui.client.util.forms.formfield.ListBoxFormField;

public class GenericAdminCourseView extends Composite implements AdminCourseView {

    interface MyUiBinder extends UiBinder<Widget, GenericAdminCourseView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    public static final EntityFactory entityFactory = GWT.create(EntityFactory.class);

    private KornellSession session;
    private PlaceController placeCtrl;
    private FormHelper formHelper = GWT.create(FormHelper.class);
    private boolean isCreationMode, hasPublishingRole, isPlatformAdmin, isAdvancedMode;
    boolean isCurrentUser, showContactDetails, isRegisteredWithCPF;

    private Presenter presenter;

    @UiField
    TabPanel tabsPanel;
    @UiField
    Tab editTab;
    @UiField
    Tab reportsTab;
    @UiField
    FlowPanel reportsPanel;
    @UiField
    Tab assetsTab;
    @UiField
    FlowPanel assetsPanel;

    @UiField
    HTMLPanel titleEdit;
    @UiField
    HTMLPanel titleCreate;
    @UiField
    HTMLPanel subtitleCreate;
    @UiField
    Form form;
    @UiField
    FlowPanel courseFields;
    @UiField
    Button btnOK;
    @UiField
    Button btnCancel;

    @UiField
    Modal confirmModal;
    @UiField
    Label confirmText;
    @UiField
    Button btnModalOK;
    @UiField
    Button btnModalCancel;

    private Course course;

    private KornellFormFieldWrapper code, title, description, contentSpec, childCourse;

    private List<KornellFormFieldWrapper> fields;
    private String courseUUID;
    private GenericCourseReportsView reportsView;
    private AdminAssetsPresenter adminAssetsPresenter;
    private EventBus bus;
    private boolean initializing = false;
    private ViewFactory viewFactory;

    public GenericAdminCourseView(final KornellSession session, EventBus bus, PlaceController placeCtrl,
            ViewFactory viewFactory) {
        this.session = session;
        this.placeCtrl = placeCtrl;
        this.bus = bus;
        this.viewFactory = viewFactory;
        hasPublishingRole = session.hasPublishingRole();
        isPlatformAdmin = session.isPlatformAdmin();
        isAdvancedMode = session.getInstitution().isAdvancedMode();
        initWidget(uiBinder.createAndBindUi(this));

        // i18n
        btnOK.setText("Salvar".toUpperCase());
        btnCancel.setText("Cancelar".toUpperCase());

        btnModalOK.setText("OK".toUpperCase());
        btnModalCancel.setText("Cancelar".toUpperCase());

        bus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
            @Override
            public void onPlaceChange(PlaceChangeEvent event) {
                if (event.getNewPlace() instanceof AdminCoursePlace && !initializing) {
                    init();
                }
            }
        });
    }

    @Override
    public void init() {
        editTab.setActive(true);
        reportsTab.setActive(false);
        assetsTab.setActive(false);

        if (placeCtrl.getWhere() instanceof AdminCoursePlace
                && ((AdminCoursePlace) placeCtrl.getWhere()).getCourseUUID() != null) {
            this.courseUUID = ((AdminCoursePlace) placeCtrl.getWhere()).getCourseUUID();
            isCreationMode = false;
        } else {
            isCreationMode = true;
        }

        if (isCreationMode) {
            course = presenter.getNewCourse();
            initData();
        } else {
            session.course(courseUUID).get(new Callback<Course>() {
                @Override
                public void ok(Course to) {
                    course = to;
                    initData();
                }
            });
        }
    }

    private static native void showNavBar(boolean show) /*-{
        if($wnd.document.getElementsByClassName("nav-tabs") && $wnd.document.getElementsByClassName("nav-tabs")[0]){
            $wnd.document.getElementsByClassName("nav-tabs")[0].style.display = (show?"block":"none");
        }
    }-*/;

    public void initData() {

        titleEdit.setVisible(!isCreationMode);
        titleCreate.setVisible(isCreationMode);
        subtitleCreate.setVisible(isCreationMode);

        buildReportsView();
        reportsTab.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                buildReportsView();
            }
        });

        assetsTab.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                buildAssetsView();
            }
        });

        courseFields.setVisible(false);
        this.fields = new ArrayList<>();

        courseFields.clear();

        btnOK.setVisible(hasPublishingRole || isCreationMode);
        btnCancel.setVisible(hasPublishingRole);

        code = new KornellFormFieldWrapper("Código", formHelper.createTextBoxFormField(course.getCode()),
                hasPublishingRole);
        if (isPlatformAdmin || (hasPublishingRole && isAdvancedMode)) {
            fields.add(code);
            courseFields.add(code);
        }

        title = new KornellFormFieldWrapper("Nome", formHelper.createTextBoxFormField(course.getName()),
                hasPublishingRole);
        fields.add(title);
        courseFields.add(title);

        description = new KornellFormFieldWrapper("Descrição",
                formHelper.createTextAreaFormField(course.getDescription(), 6), hasPublishingRole);
        description.addStyleName("heightAuto");
        description.addStyleName("marginBottom25");
        fields.add(description);
        courseFields.add(description);

        final ListBox contentSpecTypes = new ListBox();
        contentSpecTypes.addItem("WIZARD", ContentSpec.WIZARD.toString());
        contentSpecTypes.addItem("SCORM12", ContentSpec.SCORM12.toString());
        contentSpecTypes.addItem("KNL", ContentSpec.KNL.toString());
        if (!isCreationMode) {
            contentSpecTypes.setSelectedValue(course.getContentSpec().toString());
        }
        contentSpec = new KornellFormFieldWrapper("Tipo", new ListBoxFormField(contentSpecTypes), hasPublishingRole);
        fields.add(contentSpec);
        courseFields.add(contentSpec);

        if (InstitutionType.DASHBOARD.equals(session.getInstitution().getInstitutionType())) {
            childCourse = new KornellFormFieldWrapper("Conteúdo Filho?",
                    formHelper.createCheckBoxFormField(course.isChildCourse()), hasPublishingRole);
            fields.add(childCourse);
            courseFields.add(childCourse);
            ((CheckBox) childCourse.getFieldWidget()).addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    if (event.getValue()) {
                    }
                }
            });
        }

        courseFields.add(formHelper.getImageSeparator());

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                showNavBar(!isCreationMode);
                courseFields.setVisible(true);
            }
        });
        initializing = false;
    }

    public void buildReportsView() {
        reportsView = new GenericCourseReportsView(session, bus, null, course);
        reportsPanel.clear();
        reportsPanel.add(reportsView);
    }

    public void buildAssetsView() {
        adminAssetsPresenter = new AdminAssetsPresenter(session, bus, viewFactory);
        assetsPanel.clear();
        adminAssetsPresenter.init(CourseDetailsEntityType.COURSE, course);
        assetsPanel.add(adminAssetsPresenter.asWidget());
    }

    private boolean validateFields() {
        if (!formHelper.isLengthValid(code.getFieldPersistText(), 2, 255)) {
            code.setError("O código deve ter entre 2 e 255 caracteres");
        }
        if (!formHelper.isLengthValid(title.getFieldPersistText(), 2, 100)) {
            title.setError("O título deve ter entre 2 e 255 caracteres");
        }
        if (!formHelper.isLengthValid(description.getFieldPersistText(), 2, 255)) {
            description.setError("A descrição deve ter entre 2 e 255 caracteres");
        }
        if (!formHelper.isLengthValid(contentSpec.getFieldPersistText(), 2, 20)) {
            contentSpec.setError("Insira o tipo");
        } else {
            try {
                ContentSpec.valueOf(contentSpec.getFieldPersistText());
            } catch (Exception e) {
                contentSpec.setError("Tipo inválido.");
            }
        }

        return !formHelper.checkErrors(fields);
    }

    @UiHandler("btnOK")
    void doOK(ClickEvent e) {
        formHelper.clearErrors(fields);
        if (hasPublishingRole && validateFields()) {
            bus.fireEvent(new ShowPacifierEvent(true));
            Course course = getCourseInfoFromForm();
            presenter.upsertCourse(course);
        }
    }

    private Course getCourseInfoFromForm() {
        course.setCode(code.getFieldPersistText());
        course.setName(title.getFieldPersistText());
        course.setDescription(description.getFieldPersistText());
        course.setContentSpec(ContentSpec.valueOf(contentSpec.getFieldPersistText()));
        if (InstitutionType.DASHBOARD.equals(session.getInstitution().getInstitutionType())) {
            course.setChildCourse(childCourse.getFieldPersistText().equals("true"));
        }
        course.setInstitutionUUID(session.getInstitution().getUUID());
        return course;
    }

    @UiHandler("btnCancel")
    void doCancel(ClickEvent e) {
        this.placeCtrl.goTo(new AdminCoursesPlace());
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

}