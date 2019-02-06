package kornell.gui.client.presentation.admin.assets.generic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.github.gwtbootstrap.client.ui.FileUpload;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
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
import kornell.core.entity.CourseDetailsEntityType;
import kornell.core.entity.CourseDetailsHint;
import kornell.core.entity.CourseDetailsLibrary;
import kornell.core.entity.CourseDetailsSection;
import kornell.core.error.KornellErrorTO;
import kornell.core.util.StringUtils;
import kornell.gui.client.GenericClientFactoryImpl;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.presentation.admin.assets.AdminAssetsPresenter;
import kornell.gui.client.presentation.admin.assets.AdminAssetsView.Presenter;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.forms.formfield.KornellFormFieldWrapper;
import kornell.gui.client.util.view.KornellNotification;

public class GenericAssetFormView extends Composite {
    interface MyUiBinder extends UiBinder<Widget, GenericAssetFormView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    private static EventBus bus;
    private FormHelper formHelper;

    @UiField
    static Modal assetModal;
    @UiField
    FlowPanel assetFields;
    @UiField
    Button btnOK;
    @UiField
    Button btnCancel;

    private KornellFormFieldWrapper title, text, fontAwesomeClassName;
    private List<KornellFormFieldWrapper> fields;
    private static Presenter presenter;
    private String assetType;
    private CourseDetailsHint courseDetailsHint;
    private CourseDetailsSection courseDetailsSection;
    private static CourseDetailsLibrary courseDetailsLibrary;
    private static KornellSession session;
    private static String saveMode;

    private String filePath;

    public GenericAssetFormView() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void initializeModal(EventBus bus, KornellSession session, Presenter presenter) {
        GenericAssetFormView.bus = bus;
        GenericAssetFormView.session = session;
        GenericAssetFormView.presenter = presenter;
        formHelper = new FormHelper();
        fields = new ArrayList<KornellFormFieldWrapper>();
        btnCancel.setText("Cancelar".toUpperCase());
        btnOK.setText("Salvar".toUpperCase());
    }

    private boolean validateFields() {

        if (AdminAssetsPresenter.SECTION.equals(assetType) || AdminAssetsPresenter.HINT.equals(assetType)) {
            if (!formHelper.isLengthValid(title.getFieldPersistText(), 2)) {
                title.setError("Insira o título");
            } else {
                if (AdminAssetsPresenter.SECTION.equals(assetType)) {
                    for (CourseDetailsSection cds : presenter.getCourseDetailsSectionsTO().getCourseDetailsSections()) {
                        if (cds.getTitle().equals(title.getFieldPersistText())
                                && cds.getUUID() != courseDetailsSection.getUUID()) {
                            title.setError("Já existe uma seção com esse título.");
                        }
                    }

                } else if (AdminAssetsPresenter.HINT.equals(assetType)) {
                    for (CourseDetailsHint cdh : presenter.getCourseDetailsHintsTO().getCourseDetailsHints()) {
                        if (cdh.getTitle().equals(title.getFieldPersistText())
                                && cdh.getUUID() != this.courseDetailsHint.getUUID()) {
                            title.setError("Já existe uma dica com esse título.");
                        }
                    }
                } else {
                    title.setError("");
                }
            }

            if (!formHelper.isLengthValid(text.getFieldPersistText(), 2)) {
                text.setError("Insira o conteúdo");
            } else
                text.setError("");
        }

        if (AdminAssetsPresenter.HINT.equals(assetType)) {
            if (!formHelper.isLengthValid(fontAwesomeClassName.getFieldPersistText(), 4)) {
                fontAwesomeClassName.setError("Escolha um ícone");
            } else
                fontAwesomeClassName.setError("");
        }

        if (AdminAssetsPresenter.LIBRARY.equals(assetType)) {
            this.filePath = StringUtils.mkurl(session.getRepositoryAssetsURL(), AdminAssetsPresenter.entityName,
                    courseDetailsLibrary.getEntityUUID(), "library");
            courseDetailsLibrary.setPath(filePath);
            if (AdminAssetsPresenter.ADD.equals(saveMode) || getFileName().length() > 0) {
                String title = getFileName();
                courseDetailsLibrary.setTitle(title);
                courseDetailsLibrary.setSize(getFileSize());
                if (title.length() <= 0 || courseDetailsLibrary.getSize() <= 0) {
                    KornellNotification.showError("Por favor selecione um arquivo.");
                    return false;
                } else if (title.indexOf('/') >= 0 || title.indexOf('\\') >= 0 || title.indexOf(':') >= 0
                        || title.indexOf(';') >= 0) {
                    KornellNotification.showError("Nome de arquivo inválido.");
                    return false;
                } else if (courseDetailsLibrary.getSize() > (50 * 1024 * 1024)) {
                    KornellNotification.showError("Tamanho de arquivo inválido. Máximo permitido: 50 MB.");
                    return false;
                }
                courseDetailsLibrary.setFontAwesomeClassName(getFontAwesomeClassName());
                if (courseDetailsLibrary.getFontAwesomeClassName() == null) {
                    KornellNotification.show(
                            "Tipo de arquivo inválido. Tipos suportados: PDF, PNG, JPG, DOC, DOCX, PPT, PPTX, XLS, XLSX, MPG e MP4.",
                            AlertType.ERROR, 5000);
                    return false;
                }
            }

            for (CourseDetailsLibrary cdl : presenter.getCourseDetailsLibrariesTO().getCourseDetailsLibraries()) {
                if (cdl.getTitle().equals(courseDetailsLibrary.getTitle())
                        && cdl.getUUID() != GenericAssetFormView.courseDetailsLibrary.getUUID()) {
                    KornellNotification.showError("Já existe um arquivo com esse nome.");
                    return false;
                }
            }
        }

        return !checkErrors();
    }

    private String getFontAwesomeClassName() {
        String fontAwesomeClassName = null;
        String extension = courseDetailsLibrary.getTitle()
                .substring(courseDetailsLibrary.getTitle().lastIndexOf('.') + 1);
        if (extension.equals("pdf")) {
            fontAwesomeClassName = "fa-file-pdf-o";
        } else if (extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg")) {
            fontAwesomeClassName = "fa-file-image-o";
        } else if (extension.equals("doc") || extension.equals("docx")) {
            fontAwesomeClassName = "fa-file-word-o";
        } else if (extension.equals("ppt") || extension.equals("pptx")) {
            fontAwesomeClassName = "fa-file-powerpoint-o";
        } else if (extension.equals("xls") || extension.equals("xlsx")) {
            fontAwesomeClassName = "fa-file-excel-o";
        } else if (extension.equals("mpg") || extension.equals("mp4")) {
            fontAwesomeClassName = "fa-file-video-o";
        }
        return fontAwesomeClassName;
    }

    @UiHandler("btnOK")
    void doOK(ClickEvent e) {
        formHelper.clearErrors(fields);

        if (validateFields()) {
            Callback<CourseDetailsSection> courseDetailsSectionCallback = new Callback<CourseDetailsSection>() {
                @Override
                public void ok(CourseDetailsSection to) {
                    presenter.initCourseDetailsSections();
                    KornellNotification.show("Seção salva com sucesso.");
                    assetModal.hide();
                    bus.fireEvent(new ShowPacifierEvent(false));
                }

                @Override
                public void internalServerError(KornellErrorTO kornellErrorTO) {
                    KornellNotification.show("Erro ao tentar salvar a seção.", AlertType.ERROR);
                    assetModal.hide();
                    bus.fireEvent(new ShowPacifierEvent(false));
                }
            };
            Callback<CourseDetailsHint> courseDetailsHintCallback = new Callback<CourseDetailsHint>() {
                @Override
                public void ok(CourseDetailsHint to) {
                    presenter.initCourseDetailsHints();
                    KornellNotification.show("Dica salva com sucesso.");
                    assetModal.hide();
                    bus.fireEvent(new ShowPacifierEvent(false));
                }

                @Override
                public void internalServerError(KornellErrorTO kornellErrorTO) {
                    KornellNotification.show("Erro ao tentar salvar a dica.", AlertType.ERROR);
                    assetModal.hide();
                    bus.fireEvent(new ShowPacifierEvent(false));
                }
            };

            if (assetType.equals(AdminAssetsPresenter.SECTION)) {
                bus.fireEvent(new ShowPacifierEvent(true));
                getCourseDetailsSectionFromForm();
                if (saveMode.equals(AdminAssetsPresenter.ADD)) {
                    session.courseDetailsSections().create(courseDetailsSection, courseDetailsSectionCallback);
                } else if (saveMode.equals(AdminAssetsPresenter.EDIT)) {
                    session.courseDetailsSection(courseDetailsSection.getUUID()).update(courseDetailsSection,
                            courseDetailsSectionCallback);
                }
            } else if (assetType.equals(AdminAssetsPresenter.HINT)) {
                bus.fireEvent(new ShowPacifierEvent(true));
                getCourseDetailsHintFromForm();
                if (saveMode.equals(AdminAssetsPresenter.ADD)) {
                    session.courseDetailsHints().create(courseDetailsHint, courseDetailsHintCallback);
                } else if (saveMode.equals(AdminAssetsPresenter.EDIT)) {
                    session.courseDetailsHint(courseDetailsHint.getUUID()).update(courseDetailsHint,
                            courseDetailsHintCallback);
                }
            } else if (assetType.equals(AdminAssetsPresenter.LIBRARY)) {
                getCourseDetailsLibraryFromForm();
                getUploadURL();
            }
        }
    }

    public void getUploadURL() {
        String fileName = courseDetailsLibrary.getTitle().replaceAll("[ &]+", "_");
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        String contentType = getContentType(extension);
        session.assets().getUploadURL(AdminAssetsPresenter.entityName, courseDetailsLibrary.getEntityUUID(), fileName,
                "library", new Callback<String>() {
                    @Override
                    public void ok(String url) {
                        getFile(url, contentType);
                    }
                });
    }

    private String getContentType(String extension) {
        String contentType = "application/octet-stream";
        if (extension.equals("png")) {
            contentType = "image/png";
        } else if (extension.equals("jpg") || extension.equals("jpeg")) {
            contentType = "image/jpg";
        }
        return contentType;
    }

    public static native String getFileName() /*-{
        var files = $wnd.document.getElementById("uploadFormElement").files;
        if (files.length != 1) {
            return "";
        } else {
            return files[0].name;
        }
    }-*/;

    public static native int getFileSize() /*-{
        var files = $wnd.document.getElementById("uploadFormElement").files;
        if (files.length != 1) {
            return 0;
        } else {
            return files[0].size;
        }
    }-*/;

    public static native void getFile(String url, String contentType) /*-{
        @kornell.gui.client.presentation.admin.assets.AdminAssetsPresenter::showPacifier()();
        var req = new XMLHttpRequest();
        req.open('PUT', url);
        req.setRequestHeader("Content-type", contentType);
        req.setRequestHeader("x-ms-blob-type", "BlockBlob");
        req.onreadystatechange = function () {
            if (req.readyState == 4 && (req.status == 200 || req.status == 201)) {
                @kornell.gui.client.presentation.admin.assets.generic.GenericAssetFormView::postProcessImageUpload()();
            } else if (req.readyState != 2) {
                @kornell.gui.client.presentation.admin.assets.AdminAssetsPresenter::hidePacifier()();
                @kornell.gui.client.presentation.admin.assets.AdminAssetsPresenter::errorUpdatingImage()();
            }
        }
        req.send($wnd.document.getElementById("uploadFormElement").files[0]);
    }-*/;

    public static void setFileSize(String size) {
        courseDetailsLibrary.setSize(new Integer(size));
    }

    public static void setFileName(String fileName) {
        courseDetailsLibrary.setTitle(fileName);
    }

    public static void postProcessImageUpload() {
        bus.fireEvent(new ShowPacifierEvent(true));
        Callback<CourseDetailsLibrary> courseDetailsLibraryCallback = new Callback<CourseDetailsLibrary>() {
            @Override
            public void ok(CourseDetailsLibrary to) {
                presenter.initCourseDetailsLibraries();
                KornellNotification.show("Arquivo da biblioteca salvo com sucesso.");
                assetModal.hide();
                bus.fireEvent(new ShowPacifierEvent(false));
            }

            @Override
            public void internalServerError(KornellErrorTO kornellErrorTO) {
                KornellNotification.show("Erro ao tentar salvar o arquivo da biblioteca.", AlertType.ERROR);
                assetModal.hide();
                bus.fireEvent(new ShowPacifierEvent(false));
            }
        };

        if (saveMode.equals(AdminAssetsPresenter.ADD)) {
            session.courseDetailsLibraries().create(courseDetailsLibrary, courseDetailsLibraryCallback);
        } else if (saveMode.equals(AdminAssetsPresenter.EDIT)) {
            session.courseDetailsLibrary(courseDetailsLibrary.getUUID()).update(courseDetailsLibrary,
                    courseDetailsLibraryCallback);
        }
    }

    @UiHandler("btnCancel")
    void doCancel(ClickEvent e) {
        assetModal.hide();
    }

    private boolean checkErrors() {
        for (KornellFormFieldWrapper field : fields)
            if (!"".equals(field.getError()))
                return true;
        return false;
    }

    public void showModal(String saveMode, String assetType) {
        this.assetType = assetType;
        GenericAssetFormView.saveMode = saveMode;

        String modalTitle = AdminAssetsPresenter.ADD.equals(saveMode) ? "Adicionar " : "Editar ";
        modalTitle += AdminAssetsPresenter.SECTION.equals(assetType) ? "Seção"
                : (AdminAssetsPresenter.HINT.equals(assetType) ? "Dica" : "Arquivo da Biblioteca");
        assetModal.setTitle(modalTitle);

        fields.clear();
        assetFields.clear();

        assetModal.show();
    }

    public void newSection(CourseDetailsEntityType courseDetailsEntityType, String entityUUID) {
        CourseDetailsSection courseDetailsSection = GenericClientFactoryImpl.ENTITY_FACTORY.newCourseDetailsSection()
                .as();
        courseDetailsSection.setEntityType(courseDetailsEntityType);
        courseDetailsSection.setEntityUUID(entityUUID);
        courseDetailsSection.setIndex(presenter.getCourseDetailsSectionsTO().getCourseDetailsSections().size());
        initSection(AdminAssetsPresenter.ADD, courseDetailsSection);
    }

    public void initSection(String saveMode, CourseDetailsSection courseDetailsSection) {
        this.courseDetailsSection = courseDetailsSection;

        showModal(saveMode, AdminAssetsPresenter.SECTION);

        title = new KornellFormFieldWrapper("Título",
                formHelper.createTextBoxFormField(courseDetailsSection.getTitle()), true);
        fields.add(title);
        assetFields.add(title);

        text = new KornellFormFieldWrapper("Conteúdo",
                formHelper.createTextAreaFormField(courseDetailsSection.getText()), true);
        text.addStyleName("heightAuto");
        fields.add(text);
        assetFields.add(text);

        assetFields.add(formHelper.getImageSeparator());
    }

    private CourseDetailsSection getCourseDetailsSectionFromForm() {
        courseDetailsSection.setTitle(title.getFieldPersistText());
        courseDetailsSection.setText(text.getFieldPersistText());
        return courseDetailsSection;
    }

    public void newHint(CourseDetailsEntityType courseDetailsEntityType, String entityUUID) {
        CourseDetailsHint courseDetailsHint = GenericClientFactoryImpl.ENTITY_FACTORY.newCourseDetailsHint().as();
        courseDetailsHint.setEntityType(courseDetailsEntityType);
        courseDetailsHint.setEntityUUID(entityUUID);
        courseDetailsHint.setFontAwesomeClassName("fa-info-circle");
        courseDetailsHint.setIndex(presenter.getCourseDetailsHintsTO().getCourseDetailsHints().size());
        initHint(AdminAssetsPresenter.ADD, courseDetailsHint);
    }

    public void initHint(String saveMode, CourseDetailsHint courseDetailsHint) {
        this.courseDetailsHint = courseDetailsHint;

        showModal(saveMode, AdminAssetsPresenter.HINT);

        title = new KornellFormFieldWrapper("Título", formHelper.createTextBoxFormField(courseDetailsHint.getTitle()),
                true);
        fields.add(title);
        assetFields.add(title);

        text = new KornellFormFieldWrapper("Conteúdo", formHelper.createTextAreaFormField(courseDetailsHint.getText()),
                true);
        text.addStyleName("heightAuto");
        text.addStyleName("marginBottom25");
        fields.add(text);
        assetFields.add(text);

        fontAwesomeClassName = new KornellFormFieldWrapper("Ícone",
                formHelper.createTextBoxFormField(courseDetailsHint.getFontAwesomeClassName()), true, null,
                "Você pode customizar os ícones das dicas. Clique no link abaixo \"Escolha um ícone\" para ir no site do Font Awesome, depois coloque aqui o ícone desejado no formato \"fa-xxxx\".");
        fields.add(fontAwesomeClassName);
        assetFields.add(fontAwesomeClassName);

        Anchor faLink = new Anchor("Escolha um ícone", "https://fontawesome.com/v4.7.0/icons/", "_blank");
        faLink.addStyleName("faLink");
        assetFields.add(faLink);

        assetFields.add(formHelper.getImageSeparator());
    }

    private CourseDetailsHint getCourseDetailsHintFromForm() {
        courseDetailsHint.setTitle(title.getFieldPersistText());
        courseDetailsHint.setText(text.getFieldPersistText());
        courseDetailsHint.setFontAwesomeClassName(fontAwesomeClassName.getFieldPersistText());
        return courseDetailsHint;
    }

    public void newLibrary(CourseDetailsEntityType courseDetailsEntityType, String entityUUID) {
        CourseDetailsLibrary courseDetailsLibrary = GenericClientFactoryImpl.ENTITY_FACTORY.newCourseDetailsLibrary()
                .as();
        courseDetailsLibrary.setEntityType(courseDetailsEntityType);
        courseDetailsLibrary.setEntityUUID(entityUUID);
        courseDetailsLibrary.setFontAwesomeClassName("fa-info-circle");
        courseDetailsLibrary.setIndex(presenter.getCourseDetailsLibrariesTO().getCourseDetailsLibraries().size());
        courseDetailsLibrary.setUploadDate(new Date());
        initLibrary(AdminAssetsPresenter.ADD, courseDetailsLibrary);
    }

    public void initLibrary(String saveMode, CourseDetailsLibrary courseDetailsLibrary) {
        GenericAssetFormView.courseDetailsLibrary = courseDetailsLibrary;

        showModal(saveMode, AdminAssetsPresenter.LIBRARY);

        text = new KornellFormFieldWrapper("Descrição",
                formHelper.createTextAreaFormField(courseDetailsLibrary.getDescription()), true);
        text.addStyleName("heightAuto");
        text.addStyleName("marginBottom25");
        fields.add(text);
        assetFields.add(text);

        assetFields.add(buildFileUploadPanel("Arquivo"));

        if (AdminAssetsPresenter.EDIT.equals(saveMode)) {
            String fileUrl = StringUtils.mkurl(courseDetailsLibrary.getPath(), courseDetailsLibrary.getTitle().replaceAll("[ &]+", "_"));
            Anchor librayFileLink = new Anchor("Link para o arquivo", fileUrl, "_blank");
            librayFileLink.addStyleName("faLink");
            assetFields.add(librayFileLink);
        }

        assetFields.add(formHelper.getImageSeparator());
    }

    private CourseDetailsLibrary getCourseDetailsLibraryFromForm() {
        courseDetailsLibrary.setDescription(text.getFieldPersistText());
        return courseDetailsLibrary;
    }

    private FlowPanel buildFileUploadPanel(final String label) {
        // Create a FormPanel and point it at a service
        final FormPanel form = new FormPanel();

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
        fileUpload.addStyleName("uploadFormElement");
        fileUpload.setId("uploadFormElement");
        fileUploadPanel.add(fileUpload);
        fieldPanelWrapper.add(fileUpload);

        return fieldPanelWrapper;
    }

}