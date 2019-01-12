package kornell.gui.client.presentation.admin.institution.generic;

import com.github.gwtbootstrap.client.ui.FileUpload;
import com.github.gwtbootstrap.client.ui.Form;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.EntityFactory;
import kornell.core.entity.Institution;
import kornell.core.util.StringUtils;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.forms.formfield.PeopleMultipleSelect;

public class GenericInstitutionAssetsView extends Composite {
    interface MyUiBinder extends UiBinder<Widget, GenericInstitutionAssetsView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    public static final EntityFactory entityFactory = GWT.create(EntityFactory.class);

    private KornellSession session;
    private static EventBus bus;
    boolean isCurrentUser, showContactDetails, isRegisteredWithCPF;

    PeopleMultipleSelect peopleMultipleSelect;

    @UiField
    Form form;
    @UiField
    FlowPanel assetsFields;
    @UiField
    Button btnOK;
    @UiField
    Button btnCancel;

    private Institution institution;

    public GenericInstitutionAssetsView(final KornellSession session, final EventBus bus,
            kornell.gui.client.presentation.admin.institution.AdminInstitutionView.Presenter presenter,
            Institution institution) {
        this.session = session;
        GenericInstitutionAssetsView.bus = bus;
        this.institution = institution;
        initWidget(uiBinder.createAndBindUi(this));

        // i18n
        btnOK.setText("Atualizar");
        btnCancel.setText("Cancelar");

        initData();
    }

    public void initData() {
        assetsFields.clear();
        assetsFields.add(buildFileUploadPanel("logo250x45.png", "image/png", "Logo 250x45 - escura"));
        assetsFields.add(buildFileUploadPanel("logo250x45_light.png", "image/png", "Logo 250x45 - clara"));
        assetsFields.add(buildFileUploadPanel("logo300x80.png", "image/png", "Logo 300x80 - escura"));
        assetsFields.add(buildFileUploadPanel("logo300x80_light.png", "image/png", "Logo 300x80 - clara"));
        assetsFields.add(buildFileUploadPanel("bgVitrine.jpg", "image/jpg", "Background da Vitrine"));
        assetsFields.add(buildFileUploadPanel("favicon.ico", "image/x-icon", "Favicon"));
    }

    private FlowPanel buildFileUploadPanel(final String fileName, final String contentType, String label) {
        // Create a FormPanel and point it at a service
        final FormPanel form = new FormPanel();
        final String elementId = fileName.replace('.', '-');

        // Create a panel to hold all of the form widgets
        FlowPanel fieldPanelWrapper = new FlowPanel();
        fieldPanelWrapper.addStyleName("fieldPanelWrapper fileUploadPanel");
        form.setWidget(fieldPanelWrapper);

        // Create the label panel
        FlowPanel labelPanel = new FlowPanel();
        labelPanel.addStyleName("labelPanel");
        Label lblLabel = new Label(label);
        lblLabel.addStyleName("lblLabel");
        labelPanel.add(lblLabel);
        fieldPanelWrapper.add(labelPanel);

        // Create the FileUpload component
        FlowPanel fileUploadPanel = new FlowPanel();
        FileUpload fileUpload = new FileUpload();
        fileUpload.setName("uploadFormElement");
        fileUpload.setId(elementId);
        fileUploadPanel.add(fileUpload);
        fieldPanelWrapper.add(fileUpload);

        // Add a submit button to the form
        com.github.gwtbootstrap.client.ui.Button btnOK = new com.github.gwtbootstrap.client.ui.Button();
        FormHelper.createIcon(btnOK, "fa-floppy-o");
        btnOK.addStyleName("btnAction");
        btnOK.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                String test = fileName.replace(".", "%2E");
                String[] allowedFileExtensions = {"jpg", "jpeg", "png", "gif"};
                session.institution(institution.getUUID()).getUploadURL(test, new Callback<String>() {
                    @Override
                    public void ok(String url) {
                        getFile(elementId, contentType, url, allowedFileExtensions);
                    }
                });
            }
        });
        fieldPanelWrapper.add(btnOK);

        Anchor anchor = new Anchor();
        anchor.setHTML("<icon class=\"fa fa-eye\"></i>");
        anchor.setTitle("Visualizar");
        anchor.setHref(
                StringUtils.mkurl(session.getInstitutionAssetsURL(), fileName) + "?" + System.currentTimeMillis());
        anchor.getElement().setId(elementId+"-anchor");
        anchor.setTarget("_blank");
        fieldPanelWrapper.add(anchor);

        return fieldPanelWrapper;
    }

    public static native void getFile(String elementId, String contentType, String url, String[] allowedFileExtensions) /*-{
        if ($wnd.document.getElementById(elementId).files.length != 1) {
            @kornell.gui.client.util.view.KornellNotification::showError(Ljava/lang/String;)("Por favor selecione uma imagem");
        } else {
            @kornell.gui.client.presentation.admin.institution.generic.GenericInstitutionAssetsView::showPacifier()();
            var file = $wnd.document.getElementById(elementId).files[0],
                splitFileName = file.name.split(".");

            if (!splitFileName || allowedFileExtensions.indexOf(splitFileName[1]) == -1) {
                @kornell.gui.client.util.view.KornellNotification::showError(Ljava/lang/String;)("Faça o upload de uma imagem com formato válido: " + allowedFileExtensions.join(", ") + ".");
                @kornell.gui.client.presentation.admin.institution.generic.GenericInstitutionAssetsView::hidePacifier()();
            } else {
                var req = new XMLHttpRequest();
                req.open('PUT', url);
                req.setRequestHeader("Content-type", contentType);
                req.setRequestHeader("x-ms-blob-type", "BlockBlob");
                req.onreadystatechange = function() {
                    if (req.readyState == 4 && (req.status == 200 || req.status == 201)) {
                        // update the href on the view button to bypass cache
                        var oldHref = $doc.getElementById(elementId + "-anchor").href,
                            newHref = oldHref.split("?") + "?" + (new Date()).getTime();
                        $doc.getElementById(elementId + "-anchor").href = newHref;

                        @kornell.gui.client.presentation.admin.institution.generic.GenericInstitutionAssetsView::hidePacifier()();
                        @kornell.gui.client.util.view.KornellNotification::show(Ljava/lang/String;)("Atualização de imagem completa");
                    }
                }
                req.send(file);
            }
        }
    }-*/;

    public static void showPacifier() {
        bus.fireEvent(new ShowPacifierEvent(true));
    }

    public static void hidePacifier() {
        bus.fireEvent(new ShowPacifierEvent(false));
    }

}