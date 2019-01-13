package kornell.gui.client.presentation.admin.institution.generic;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.ContentRepository;
import kornell.core.entity.EntityFactory;
import kornell.core.entity.Institution;
import kornell.core.entity.RepositoryType;
import kornell.core.to.InstitutionHostNamesTO;
import kornell.core.to.TOFactory;
import kornell.core.util.StringUtils;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.forms.formfield.KornellFormFieldWrapper;
import kornell.gui.client.util.forms.formfield.ListBoxFormField;
import kornell.gui.client.util.view.KornellNotification;

public class GenericInstitutionRepositoryView extends Composite {
    interface MyUiBinder extends UiBinder<Widget, GenericInstitutionRepositoryView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    public static final TOFactory toFactory = GWT.create(TOFactory.class);
    public static final EntityFactory entityFactory = GWT.create(EntityFactory.class);
    private FormHelper formHelper = GWT.create(FormHelper.class);

    private KornellSession session;
    boolean isCurrentUser, showContactDetails, isRegisteredWithCPF;
    private boolean isPlatformAdmin, isCreationMode;

    private KornellFormFieldWrapper repositoryType;
    private KornellFormFieldWrapper accessKeyId, secretAccessKey, bucketName, prefix, region;
    private KornellFormFieldWrapper fsPath, fsPrefix;

    private List<KornellFormFieldWrapper> s3Fields, fsFields;

    @UiField
    Form form;
    @UiField
    FlowPanel s3FieldsPanel, fsFieldsPanel, generalFieldsPanel;
    @UiField
    Button btnOK;
    @UiField
    Button btnCancel;

    ListBox repositoryTypes;

    private Institution institution;
    private ContentRepository repo;

    public GenericInstitutionRepositoryView(final KornellSession session,
            kornell.gui.client.presentation.admin.institution.AdminInstitutionView.Presenter presenter,
            Institution institution, ContentRepository repo) {
        this.session = session;
        this.institution = institution;
        this.isPlatformAdmin = session.isPlatformAdmin();
        this.isCreationMode = repo == null;
        initWidget(uiBinder.createAndBindUi(this));

        if (isCreationMode) {
            this.repo = entityFactory.newContentRepository().as();
        } else {
            this.repo = repo;
        }

        // i18n
        btnOK.setText("OK".toUpperCase());
        btnCancel.setText("Limpar".toUpperCase());

        initData();
    }

    public void initData() {
        this.s3FieldsPanel.setVisible(false);
        this.fsFieldsPanel.setVisible(false);
        this.fsFields = new ArrayList<KornellFormFieldWrapper>();
        this.s3Fields = new ArrayList<KornellFormFieldWrapper>();
        this.generalFieldsPanel.clear();
        this.s3FieldsPanel.clear();
        this.fsFieldsPanel.clear();

        repositoryTypes = new ListBox();
        repositoryTypes.addItem("Filesystem", RepositoryType.FS.toString());
        repositoryTypes.addItem("S3", RepositoryType.S3.toString());
        if (repo == null) {
            repositoryTypes.setSelectedValue(RepositoryType.S3.toString());
        } else {
            repositoryTypes.setSelectedValue(repo.getRepositoryType().toString());
        }

        // Common
        repositoryType = new KornellFormFieldWrapper("Repository Type", new ListBoxFormField(repositoryTypes),
                isPlatformAdmin);
        generalFieldsPanel.add(repositoryType);
        ((ListBox) repositoryType.getFieldWidget()).addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                toggleRepositoryTypeForm(repositoryTypes);
            }
        });

        // S3
        accessKeyId = new KornellFormFieldWrapper("Access Key ID",
                formHelper.createTextBoxFormField(repo.getAccessKeyId()), isPlatformAdmin);
        s3FieldsPanel.add(accessKeyId);
        s3Fields.add(accessKeyId);

        secretAccessKey = new KornellFormFieldWrapper("Secret Access Key",
                formHelper.createTextBoxFormField(repo.getSecretAccessKey()), isPlatformAdmin);
        s3FieldsPanel.add(secretAccessKey);
        s3Fields.add(secretAccessKey);

        bucketName = new KornellFormFieldWrapper("Bucket Name", formHelper.createTextBoxFormField(repo.getBucketName()),
                isPlatformAdmin);
        s3FieldsPanel.add(bucketName);
        s3Fields.add(bucketName);

        prefix = new KornellFormFieldWrapper("Prefix", formHelper.createTextBoxFormField(repo.getPrefix()),
                isPlatformAdmin);
        s3FieldsPanel.add(prefix);
        s3Fields.add(prefix);

        region = new KornellFormFieldWrapper("Region", formHelper.createTextBoxFormField(repo.getRegion()),
                isPlatformAdmin);
        s3FieldsPanel.add(region);
        s3Fields.add(region);

        s3FieldsPanel.add(formHelper.getImageSeparator());

        // FS
        fsPath = new KornellFormFieldWrapper("Path", formHelper.createTextBoxFormField(repo.getPath()),
                isPlatformAdmin);
        fsFieldsPanel.add(fsPath);
        fsFields.add(fsPath);

        fsPrefix = new KornellFormFieldWrapper("Prefix", formHelper.createTextBoxFormField(repo.getPrefix()),
                isPlatformAdmin);
        fsFieldsPanel.add(fsPrefix);
        fsFields.add(fsPrefix);

        fsFieldsPanel.add(formHelper.getImageSeparator());

        toggleRepositoryTypeForm(repositoryTypes);
    }

    private void toggleRepositoryTypeForm(final ListBox repositoryTypes) {
        boolean showS3Form = repositoryTypes.getSelectedValue().equals(RepositoryType.S3.toString());
        s3FieldsPanel.setVisible(showS3Form);
        fsFieldsPanel.setVisible(!showS3Form);
    }

    private boolean validateContentRepositoryForm() {
        if (repositoryTypes.getSelectedValue().equals(RepositoryType.S3.toString())) {
            if (StringUtils.isNone(accessKeyId.getFieldPersistText())) {
                accessKeyId.setError("Missing access key ID");
            } else {
                accessKeyId.setError("");
            }
            if (StringUtils.isNone(secretAccessKey.getFieldPersistText())) {
                secretAccessKey.setError("Missing secret access key");
            } else {
                secretAccessKey.setError("");
            }
            if (StringUtils.isNone(bucketName.getFieldPersistText())) {
                bucketName.setError("Missing bucket name");
            } else {
                bucketName.setError("");
            }
            if (StringUtils.isNone(prefix.getFieldPersistText())) {
                prefix.setError("Missing prefix");
            } else {
                prefix.setError("");
            }
            if (StringUtils.isNone(region.getFieldPersistText())) {
                region.setError("Missing region");
            } else {
                region.setError("");
            }
        } else {
            if (StringUtils.isNone(fsPrefix.getFieldPersistText())) {
                fsPrefix.setError("Missing prefix");
            } else {
                fsPrefix.setError("");
            }
            if (StringUtils.isNone(fsPath.getFieldPersistText())) {
                fsPath.setError("Missing path");
            } else {
                fsPath.setError("");
            }
        }
        return !checkErrors();
    }

    private boolean checkErrors() {
        List<KornellFormFieldWrapper> fields;
        if (repositoryTypes.getSelectedValue().equals(RepositoryType.S3.toString())) {
            fields = s3Fields;
        } else {
            fields = fsFields;
        }
        for (KornellFormFieldWrapper field : fields) {
            if (!"".equals(field.getError())) {
                KornellNotification.show("There are errors with your content repository configuration",
                        AlertType.WARNING);
                if (field.getFieldWidget() instanceof FocusWidget) {
                    ((FocusWidget) field.getFieldWidget()).setFocus(true);
                }
                return true;
            }
        }
        return false;
    }

    @UiHandler("btnOK")
    void doOK(ClickEvent e) {
        if (session.isInstitutionAdmin() && validateContentRepositoryForm()) {
            ContentRepository contentRepository = entityFactory.newContentRepository().as();
            contentRepository.setUUID(institution.getAssetsRepositoryUUID());
            if (repositoryTypes.getSelectedValue().equals(RepositoryType.S3.toString())) {
                contentRepository.setRepositoryType(RepositoryType.S3);
                contentRepository.setPrefix(prefix.getFieldPersistText());
            } else {
                contentRepository.setRepositoryType(RepositoryType.FS);
                contentRepository.setPrefix(fsPrefix.getFieldPersistText());
            }
            contentRepository.setAccessKeyId(accessKeyId.getFieldPersistText());
            contentRepository.setSecretAccessKey(secretAccessKey.getFieldPersistText());
            contentRepository.setBucketName(bucketName.getFieldPersistText());
            contentRepository.setRegion(region.getFieldPersistText());
            contentRepository.setPath(fsPath.getFieldPersistText());

            InstitutionHostNamesTO institutionHostNamesTO = toFactory.newInstitutionHostNamesTO().as();
            List<String> institutionHostNames = new ArrayList<String>();

            institutionHostNamesTO.setInstitutionHostNames(institutionHostNames);
            session.repository().updateRepository(institution.getAssetsRepositoryUUID(), contentRepository,
                    new Callback<ContentRepository>() {
                        @Override
                        public void ok(ContentRepository to) {
                            KornellNotification.show("O repositório da instituição foi atualizado com sucesso.");
                        }
                    });
        }

    }

    @UiHandler("btnCancel")
    void doCancel(ClickEvent e) {
        initData();
    }

}
