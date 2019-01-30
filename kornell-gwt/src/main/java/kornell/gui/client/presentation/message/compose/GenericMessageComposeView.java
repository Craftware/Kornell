package kornell.gui.client.presentation.message.compose;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import kornell.api.client.KornellSession;
import kornell.core.entity.role.RoleType;
import kornell.core.to.CourseClassTO;
import kornell.core.to.RoleTO;
import kornell.gui.client.KornellConstants;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.forms.formfield.KornellFormFieldWrapper;
import kornell.gui.client.util.forms.formfield.ListBoxFormField;

public class GenericMessageComposeView extends Composite implements MessageComposeView {

    interface GenericMessageComposeUiBinder extends UiBinder<Widget, GenericMessageComposeView> {
    }

    private static GenericMessageComposeUiBinder uiBinder = GWT.create(GenericMessageComposeUiBinder.class);
    private static FormHelper formHelper = GWT.create(FormHelper.class);
    private static KornellConstants constants = GWT.create(KornellConstants.class);
    private MessageComposeView.Presenter presenter;

    KornellFormFieldWrapper recipient, messageText;
    private List<KornellFormFieldWrapper> fields;
    private KornellSession session;

    @UiField
    Anchor helpFilePanel;
    @UiField
    Label lblTitle;
    @UiField
    Label lblSubTitle;
    @UiField
    Image separatorBar;
    @UiField
    FlowPanel fieldsPanel;
    @UiField
    Button btnOK;
    @UiField
    Button btnCancel;

    public GenericMessageComposeView(KornellSession session) {
        this.session = session;
        initWidget(uiBinder.createAndBindUi(this));
        ensureDebugId("genericMessageComposeView");
    }

    @Override
    public void show(ArrayList<CourseClassTO> helpCourseClasses, String courseClassUUID) {
        lblTitle.setText(constants.composeTitle());
        lblSubTitle.setText(constants.composeSubTitle());
        separatorBar.setUrl(FormHelper.SEPARATOR_BAR_IMG_PATH);
        separatorBar.addStyleName(FormHelper.SEPARATOR_BAR_CLASS);
        buildHelpFilePanel();

        this.fields = new ArrayList<KornellFormFieldWrapper>();
        fieldsPanel.clear();

        boolean hasPlatformThreadAccess = false;
        boolean hasInstitutionThreadAccess = false;

        final ListBox recipients = new ListBox();

        for (RoleTO roleTO : session.getCurrentUser().getRoles()) {
            if (RoleType.institutionAdmin.equals(roleTO.getRole().getRoleType())) {
                hasPlatformThreadAccess = true;
            } else if (RoleType.courseClassAdmin.equals(roleTO.getRole().getRoleType()) || RoleType.institutionCourseClassesAdmin.equals(roleTO.getRole().getRoleType())) {
                hasInstitutionThreadAccess = true;
            }
        }

        if (hasPlatformThreadAccess) {
            recipients.addItem(constants.platformAdminLabel(), "platformSupport");
        }

        if (hasInstitutionThreadAccess) {
            recipients.addItem(constants.institutionAdmin() + ": " + session.getInstitution().getName(),
                    "institutionSupport");
        }

        for (CourseClassTO courseClassTO : helpCourseClasses) {
            recipients.addItem(constants.courseClassAdmin() + ": " + courseClassTO.getCourseClass().getName(),
                    courseClassTO.getCourseClass().getUUID());
        }

        if (courseClassUUID == null && recipients.getItemCount() <= 0)
            this.setVisible(false);

        recipients.setSelectedValue(courseClassUUID);
        recipient = new KornellFormFieldWrapper(constants.recipient(), new ListBoxFormField(recipients),
                recipients.getItemCount() > 1 && (hasPlatformThreadAccess || courseClassUUID == null));
        fields.add(recipient);
        fieldsPanel.add(recipient);

        messageText = new KornellFormFieldWrapper(constants.message(), formHelper.createTextAreaFormField(""), true);
        fields.add(messageText);
        fieldsPanel.add(messageText);
    }

    private void buildHelpFilePanel() {
        FlowPanel panel = new FlowPanel();

        Icon iconFile = new Icon();
        iconFile.setStyleName("fa fa-file-pdf-o fileIcon");
        panel.add(iconFile);

        Label helpFileLabel = new Label(constants.helpFileCaption());
        helpFileLabel.addStyleName("fileCaption");
        panel.add(helpFileLabel);

        helpFilePanel.setHTML(panel.getElement().getInnerHTML());
        helpFilePanel.setHref("http://eduvem.com/help/" + constants.helpFileName());
        helpFilePanel.setTarget("_blank");
    }

    @Override
    protected void onEnsureDebugId(String baseID) {
        recipient.ensureDebugId(baseID + "-recipient");
        messageText.ensureDebugId(baseID + "-messageText");
        btnOK.ensureDebugId(baseID + "-btnOK");
        btnCancel.ensureDebugId(baseID + "-btnCancel");
    }

    @UiHandler("btnOK")
    void onOkButtonClicked(ClickEvent e) {
        presenter.okButtonClicked();
    }

    @UiHandler("btnCancel")
    void onCancelButtonClicked(ClickEvent e) {
        presenter.cancelButtonClicked();
    }

    @Override
    public void setPresenter(Presenter p) {
        presenter = p;
    }

    @Override
    public KornellFormFieldWrapper getRecipient() {
        return recipient;
    }

    @Override
    public KornellFormFieldWrapper getMessageText() {
        return messageText;
    }

    @Override
    public boolean checkErrors() {
        return formHelper.checkErrors(fields);
    }

    @Override
    public void clearErrors() {
        formHelper.clearErrors(fields);
    }
}