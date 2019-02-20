package kornell.gui.client.presentation.admin.course.course.generic;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Collapse;
import com.github.gwtbootstrap.client.ui.CollapseTrigger;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.github.gwtbootstrap.client.ui.event.ShownEvent;
import com.github.gwtbootstrap.client.ui.event.ShownHandler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.Course;
import kornell.core.error.KornellErrorTO;
import kornell.core.to.SimplePeopleTO;
import kornell.core.to.SimplePersonTO;
import kornell.core.to.TOFactory;
import kornell.core.util.StringUtils;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.util.ClientConstants;
import kornell.gui.client.util.view.KornellNotification;

public class GenericCourseReportItemView extends Composite {
    interface MyUiBinder extends UiBinder<Widget, GenericCourseReportItemView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private String ADMIN_IMAGES_PATH = StringUtils.mkurl(ClientConstants.IMAGES_PATH, "admin/");
    private String LIBRARY_IMAGES_PATH = StringUtils.mkurl(ClientConstants.IMAGES_PATH, "courseLibrary/");
    private KornellSession session;
    private EventBus bus;
    private Course course;
    private String type;
    private String name;
    private String description;
    public static final TOFactory toFactory = GWT.create(TOFactory.class);

    private CheckBox checkBoxCollapse;
    private CheckBox checkBoxExcel;

    private HandlerRegistration downloadHandler;

    public static final String COURSE_CLASS_INFO = "courseClassInfo";

    @UiField
    Image certificationIcon;
    @UiField
    Label lblName;
    @UiField
    Label lblDescription;
    @UiField
    FlowPanel optionPanel;
    @UiField
    Anchor lblGenerate;
    @UiField
    Anchor lblDownload;
    private TextArea usernamesTextArea;

    public GenericCourseReportItemView(EventBus bus, KornellSession session, Course course, String type) {
        this.session = session;
        this.course = course;
        this.type = type;
        this.bus = bus;
        initWidget(uiBinder.createAndBindUi(this));
        display();
    }

    private void display() {
        if (COURSE_CLASS_INFO.equals(this.type)) {
            displayCourseClassInfo();
        }
    }

    private void displayCourseClassInfo() {
        this.name = "Relatório de detalhes do grupo";
        this.description = "Geração do relatório de detalhes do grupo e de suas matrículas. Por padrão ele é gerado em PDF contendo somente matriculas ativas.";

        certificationIcon.setUrl(StringUtils.mkurl(ADMIN_IMAGES_PATH, type + ".png"));
        lblName.setText(name);
        lblDescription.setText(description);
        lblGenerate.setText("Gerar");
        lblGenerate.addStyleName("cursorPointer");

        lblDownload.setText("-");
        lblDownload.removeStyleName("cursorPointer");
        lblDownload.addStyleName("anchorToLabel");
        lblDownload.setEnabled(false);

        Image img = new Image(StringUtils.mkurl(LIBRARY_IMAGES_PATH, "xls.png"));
        checkBoxExcel = new CheckBox("Gerar em formato Excel (inclui matrículas canceladas)");
        checkBoxExcel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                displayActionCell(null);
            }
        });

        optionPanel.add(img);
        optionPanel.add(checkBoxExcel);

        CollapseTrigger trigger = new CollapseTrigger();
        final Collapse collapse = new Collapse();
        trigger.setTarget("#toggleClassInfoUsernames");
        collapse.setId("toggleClassInfoUsernames");

        checkBoxCollapse = new CheckBox("Gerar somente para um conjunto de participantes deste grupo");

        FlowPanel triggerPanel = new FlowPanel();
        triggerPanel.add(checkBoxCollapse);
        trigger.add(triggerPanel);

        FlowPanel collapsePanel = new FlowPanel();
        Label infoLabel = new Label(
                "Digite os usuários, cada um em uma linha.");
        usernamesTextArea = new TextArea();
        collapsePanel.add(infoLabel);
        collapsePanel.add(usernamesTextArea);
        collapse.add(collapsePanel);

        optionPanel.add(trigger);
        optionPanel.add(collapse);

        displayActionCell(null);

        img.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                checkBoxExcel.setValue(!checkBoxExcel.getValue());
                displayActionCell(null);
            }
        });

        collapse.addShownHandler(new ShownHandler() {
            @Override
            public void onShown(ShownEvent shownEvent) {
                checkBoxCollapse.setValue(true);
            }
        });

        collapse.addHiddenHandler(new HiddenHandler() {
            @Override
            public void onHidden(HiddenEvent hiddenEvent) {
                checkBoxCollapse.setValue(false);
            }
        });

        lblGenerate.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                displayActionCell(null);
                bus.fireEvent(new ShowPacifierEvent(true));
                SimplePeopleTO simplePeopleTO = buildSimplePeopleTO();
                session.report().generateCourseInfo(course.getUUID(), (checkBoxExcel.getValue() ? "xls" : "pdf"),
                        simplePeopleTO, new Callback<String>() {

                    @Override
                    public void ok(String url) {
                        KornellNotification.show("O relatório de detalhes da classe foi gerado.", AlertType.WARNING, 2000);
                        displayActionCell(url);
                        bus.fireEvent(new ShowPacifierEvent(false));
                    }

                    @Override
                    public void internalServerError(KornellErrorTO kornellErrorTO) {
                        KornellNotification.show(
                                "Erro na geração do relatório. Tente novamente ou entre em contato com o suporte.",
                                AlertType.ERROR, 3000);
                        displayActionCell(null);
                        bus.fireEvent(new ShowPacifierEvent(false));
                    }
                });
            }
        });
    }

    private SimplePeopleTO buildSimplePeopleTO() {
        SimplePeopleTO simplePeopleTO = toFactory.newSimplePeopleTO().as();

        if (checkBoxCollapse.getValue()) {
            String usernames = usernamesTextArea.getValue();
            String[] usernamesArr = usernames.trim().split("\n");
            List<SimplePersonTO> simplePeopleTOList = new ArrayList<SimplePersonTO>();
            SimplePersonTO simplePersonTO;
            String username;
            for (int i = 0; i < usernamesArr.length; i++) {
                username = usernamesArr[i].trim();
                if (username.length() > 0) {
                    simplePersonTO = toFactory.newSimplePersonTO().as();
                    simplePersonTO.setUsername(username);
                    simplePeopleTOList.add(simplePersonTO);
                }
            }
            simplePeopleTO.setSimplePeopleTO(simplePeopleTOList);
        }
        return simplePeopleTO;
    }

    private void displayActionCell(final String url) {
        if (url != null && !"".equals(url)) {
            lblDownload.setText("Baixar");
            lblDownload.addStyleName("cursorPointer");
            lblDownload.removeStyleName("anchorToLabel");
            downloadHandler = lblDownload.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    Window.open(url, "", "");
                }
            });
        } else {
            lblDownload.setText("Não disponível");
            lblDownload.removeStyleName("cursorPointer");
            lblDownload.addStyleName("anchorToLabel");
            lblDownload.setEnabled(false);
            if (downloadHandler != null) {
                downloadHandler.removeHandler();
            }
        }
    }

}
