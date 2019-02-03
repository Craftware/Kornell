package kornell.gui.client.presentation.admin.assets.generic;

import java.util.List;
import java.util.Map;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FileUpload;
import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.AssetEntity;
import kornell.core.entity.CertificateDetails;
import kornell.core.entity.CourseDetailsEntityType;
import kornell.core.entity.CourseDetailsHint;
import kornell.core.entity.CourseDetailsLibrary;
import kornell.core.entity.CourseDetailsSection;
import kornell.core.entity.EntityFactory;
import kornell.core.error.KornellErrorTO;
import kornell.core.to.CourseDetailsHintsTO;
import kornell.core.to.CourseDetailsLibrariesTO;
import kornell.core.to.CourseDetailsSectionsTO;
import kornell.core.util.StringUtils;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.presentation.admin.assets.AdminAssetsPresenter;
import kornell.gui.client.presentation.admin.assets.AdminAssetsView;
import kornell.gui.client.presentation.admin.common.ConfirmModalView;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.view.KornellNotification;

public class GenericAdminAssetsView extends Composite implements AdminAssetsView {
    interface MyUiBinder extends UiBinder<Widget, GenericAdminAssetsView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    public static final EntityFactory entityFactory = GWT.create(EntityFactory.class);

    boolean isCurrentUser, showContactDetails, isRegisteredWithCPF;
    int sectionsCount, sectionsLoaded = 0;

    @UiField
    FlowPanel assetsWrapper;
    @UiField
    Label thumbSubTitle;
    @UiField
    FlowPanel thumbFieldPanel;
    @UiField
    Label certificateDetailsSubTitle;
    @UiField
    FlowPanel certificateDetailsFieldPanel;
    @UiField
    Button sectionsAddButton;
    @UiField
    Label sectionsSubTitle;
    @UiField
    FlowPanel sectionsFieldPanel;
    @UiField
    Button hintsAddButton;
    @UiField
    Label hintsSubTitle;
    @UiField
    FlowPanel hintsFieldPanel;
    @UiField
    Button librariesAddButton;
    @UiField
    Label librariesSubTitle;
    @UiField
    FlowPanel librariesFieldPanel;
    @UiField
    GenericAssetFormView assetModal;

    ConfirmModalView confirmModal;

    private Presenter presenter;
    private Map<String, String> info;
    private EventBus bus;
    private KornellSession session;
    private CourseDetailsEntityType courseDetailsEntityType;
    private String entityUUID;
    private List<CourseDetailsSection> courseDetailsSections;
    private List<CourseDetailsHint> courseDetailsHints;
    private List<CourseDetailsLibrary> courseDetailsLibraries;

    public GenericAdminAssetsView(KornellSession session, EventBus bus, ViewFactory viewFactory) {
        this.bus = bus;
        this.session = session;
        this.confirmModal = viewFactory.getConfirmModalView();
        sectionsCount = 5;
        initWidget(uiBinder.createAndBindUi(this));
        FormHelper.createIcon(sectionsAddButton, "fa-plus-circle");
        FormHelper.createIcon(hintsAddButton, "fa-plus-circle");
        FormHelper.createIcon(librariesAddButton, "fa-plus-circle");
    }

    @Override
    public void initData(CourseDetailsEntityType entityType, String entityUUID) {
        this.courseDetailsEntityType = entityType;
        this.entityUUID = entityUUID;
        info = presenter.getInfo();
        bus.fireEvent(new ShowPacifierEvent(true));
        assetsWrapper.addStyleName("shy");
        sectionsLoaded = 0;
        assetModal.initializeModal(bus, session, presenter);
    }

    @Override
    public void initThumb(boolean exists) {
        thumbSubTitle.setText(info.get("thumbSubTitle"));
        thumbFieldPanel.clear();
        thumbFieldPanel.add(buildFileUploadPanel(AdminAssetsPresenter.THUMB_FILENAME, AdminAssetsPresenter.IMAGE_JPG,
                AdminAssetsPresenter.THUMB_DESCRIPTION, exists));
        sectionLoaded();
    }

    @Override
    public void initCertificateDetails(CertificateDetails certificateDetails) {
        certificateDetailsSubTitle.setText(info.get("certificateDetailsSubTitle"));
        certificateDetailsFieldPanel.clear();
        certificateDetailsFieldPanel
                .add(buildFileUploadPanel(AdminAssetsPresenter.CERTIFICATE_FILENAME, AdminAssetsPresenter.IMAGE_JPG,
                        AdminAssetsPresenter.CERTIFICATE_DESCRIPTION, certificateDetails.getUUID() != null));
        sectionLoaded();
    }

    @Override
    public void initCourseDetailsSections(CourseDetailsSectionsTO courseDetailsSectionsTO) {
        sectionsSubTitle.setText(info.get("sectionsSubTitle"));
        sectionsFieldPanel.clear();
        this.courseDetailsSections = courseDetailsSectionsTO.getCourseDetailsSections();
        for (CourseDetailsSection courseDetailsSection : courseDetailsSections) {
            sectionsFieldPanel.add(buildSectionItem(courseDetailsSection, courseDetailsSections.size()));
        }
        sectionLoaded();
    }

    private FlowPanel buildSectionItem(CourseDetailsSection courseDetailsSection, int count) {
        ClickHandler editClickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                assetModal.initSection(AdminAssetsPresenter.EDIT, (CourseDetailsSection) courseDetailsSection);
            }
        };
        ClickHandler deleteClickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                confirmModal.showModal("Tem certeza que deseja excluir essa seção?",
                        new com.google.gwt.core.client.Callback<Void, Void>() {

                            @Override
                            public void onSuccess(Void result) {
                                bus.fireEvent(new ShowPacifierEvent(true));
                                session.courseDetailsSection(courseDetailsSection.getUUID())
                                        .delete(new Callback<CourseDetailsSection>() {
                                            @Override
                                            public void ok(CourseDetailsSection to) {
                                                KornellNotification.show("Seção excluída com sucesso.");
                                                presenter.initCourseDetailsSections();
                                                bus.fireEvent(new ShowPacifierEvent(false));
                                            }

                                            @Override
                                            public void internalServerError(KornellErrorTO kornellErrorTO) {
                                                KornellNotification.show("Erro ao tentar excluir a seção.",
                                                        AlertType.ERROR);
                                                bus.fireEvent(new ShowPacifierEvent(false));
                                            }
                                        });
                            }

                            @Override
                            public void onFailure(Void reason) {
                            }
                        });
            }
        };

        FlowPanel section = new FlowPanel();
        section.addStyleName("assetWrapper");

        Label title = new Label(courseDetailsSection.getTitle());
        title.addStyleName("assetTitle");
        section.add(title);

        section.add(getButtonsBar(AdminAssetsPresenter.SECTION, (AssetEntity) courseDetailsSection, count,
                editClickHandler, deleteClickHandler));

        Label text = new Label();
        text.getElement().setInnerHTML(courseDetailsSection.getText());
        text.addStyleName("assetText");
        section.add(text);

        return section;
    }

    @Override
    public void initCourseDetailsHints(CourseDetailsHintsTO courseDetailsHintsTO) {
        hintsSubTitle.setText(info.get("hintsSubTitle"));
        hintsFieldPanel.clear();
        this.courseDetailsHints = courseDetailsHintsTO.getCourseDetailsHints();
        for (CourseDetailsHint courseDetailsHint : courseDetailsHints) {
            hintsFieldPanel.add(buildHintItem(courseDetailsHint, courseDetailsHints.size()));
        }
        sectionLoaded();
    }

    private FlowPanel buildHintItem(CourseDetailsHint courseDetailsHint, int count) {
        ClickHandler editClickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                assetModal.initHint(AdminAssetsPresenter.EDIT, (CourseDetailsHint) courseDetailsHint);
            }
        };
        ClickHandler deleteClickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                confirmModal.showModal("Tem certeza que deseja excluir essa dica?",
                        new com.google.gwt.core.client.Callback<Void, Void>() {

                            @Override
                            public void onSuccess(Void result) {
                                bus.fireEvent(new ShowPacifierEvent(true));
                                session.courseDetailsHint(courseDetailsHint.getUUID())
                                        .delete(new Callback<CourseDetailsHint>() {
                                            @Override
                                            public void ok(CourseDetailsHint to) {
                                                KornellNotification.show("Dica excluída com sucesso.");
                                                presenter.initCourseDetailsHints();
                                                bus.fireEvent(new ShowPacifierEvent(false));
                                            }

                                            @Override
                                            public void internalServerError(KornellErrorTO kornellErrorTO) {
                                                KornellNotification.show("Erro ao tentar excluir a dica.",
                                                        AlertType.ERROR);
                                                bus.fireEvent(new ShowPacifierEvent(false));
                                            }
                                        });
                            }

                            @Override
                            public void onFailure(Void reason) {
                            }
                        });
            }
        };

        FlowPanel hint = new FlowPanel();
        hint.addStyleName("assetWrapper");

        Label title = new Label(courseDetailsHint.getTitle());
        title.addStyleName("assetTitle");
        hint.add(title);

        hint.add(getButtonsBar(AdminAssetsPresenter.HINT, (AssetEntity) courseDetailsHint, count, editClickHandler,
                deleteClickHandler));

        Icon icon = new Icon();
        icon.addStyleName("fa " + courseDetailsHint.getFontAwesomeClassName());

        Label text = new Label();
        text.getElement().setInnerHTML(icon.asWidget().getElement().getString() + courseDetailsHint.getText());
        text.addStyleName("assetText hintText");
        hint.add(text);

        return hint;
    }

    @Override
    public void initCourseDetailsLibraries(CourseDetailsLibrariesTO courseDetailsLibrariesTO) {
        librariesSubTitle.setText(info.get("librariesSubTitle"));
        librariesFieldPanel.clear();
        this.courseDetailsLibraries = courseDetailsLibrariesTO.getCourseDetailsLibraries();
        for (CourseDetailsLibrary courseDetailsLibrary : courseDetailsLibraries) {
            librariesFieldPanel.add(buildLibraryItem(courseDetailsLibrary, courseDetailsLibraries.size()));
        }
        sectionLoaded();
    }

    private FlowPanel buildLibraryItem(CourseDetailsLibrary courseDetailsLibrary, int count) {
        ClickHandler editClickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                assetModal.initLibrary(AdminAssetsPresenter.EDIT, (CourseDetailsLibrary) courseDetailsLibrary);
            }
        };
        ClickHandler deleteClickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                confirmModal.showModal("Tem certeza que deseja excluir esse arquivo?",
                        new com.google.gwt.core.client.Callback<Void, Void>() {

                            @Override
                            public void onSuccess(Void result) {
                                bus.fireEvent(new ShowPacifierEvent(true));
                                session.courseDetailsLibrary(courseDetailsLibrary.getUUID())
                                        .delete(new Callback<CourseDetailsLibrary>() {
                                            @Override
                                            public void ok(CourseDetailsLibrary to) {
                                                KornellNotification.show("Arquivo excluído com sucesso.");
                                                presenter.initCourseDetailsLibraries();
                                                bus.fireEvent(new ShowPacifierEvent(false));
                                            }

                                            @Override
                                            public void internalServerError(KornellErrorTO kornellErrorTO) {
                                                KornellNotification.show("Erro ao tentar excluir o arquivo.",
                                                        AlertType.ERROR);
                                                bus.fireEvent(new ShowPacifierEvent(false));
                                            }
                                        });
                            }

                            @Override
                            public void onFailure(Void reason) {
                            }
                        });
            }
        };

        FlowPanel library = new FlowPanel();
        library.addStyleName("assetWrapper");

        Label title = new Label(courseDetailsLibrary.getTitle());
        title.addStyleName("assetTitle");
        library.add(title);

        library.add(getButtonsBar(AdminAssetsPresenter.LIBRARY, (AssetEntity) courseDetailsLibrary, count,
                editClickHandler, deleteClickHandler));

        Icon icon = new Icon();
        icon.addStyleName("fa " + courseDetailsLibrary.getFontAwesomeClassName());

        Label text = new Label();
        String description = courseDetailsLibrary.getDescription();
        description = (description == null) ? "" : description;
        text.getElement().setInnerHTML(icon.asWidget().getElement().getString() + description);
        text.addStyleName("assetText hintText");
        library.add(text);

        return library;
    }

    private FlowPanel getButtonsBar(String assetType, AssetEntity assetEntity, int entityCount,
            ClickHandler editClickHandler, ClickHandler deleteClickHandler) {
        FlowPanel buttonsBar = new FlowPanel();
        buttonsBar.addStyleName("buttonsBar");

        if (AdminAssetsPresenter.LIBRARY.equals(assetType)) {
            Tooltip tooltipMoveUp = new Tooltip("Baixar");
            tooltipMoveUp.setPlacement(Placement.TOP);
            Button btnMoveUp = new Button();
            btnMoveUp.addStyleName("btnSelected");
            FormHelper.createIcon(btnMoveUp, "fa-download");
            tooltipMoveUp.add(btnMoveUp);
            buttonsBar.add(tooltipMoveUp);
            btnMoveUp.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    CourseDetailsLibrary cdl = (CourseDetailsLibrary) assetEntity;
                    String fileName = StringUtils.mkurl(cdl.getPath(), cdl.getTitle().replaceAll("[ &]+", "_"));
                    Window.open(fileName, "_blank", "");
                }
            });
        }

        Tooltip tooltipDelete = new Tooltip("Excluir");
        tooltipDelete.setPlacement(Placement.TOP);
        Button btnDelete = new Button();
        btnDelete.addClickHandler(deleteClickHandler);
        btnDelete.addStyleName("btnSelected");
        FormHelper.createIcon(btnDelete, "fa-trash");
        tooltipDelete.add(btnDelete);
        buttonsBar.add(tooltipDelete);

        Tooltip tooltipEdit = new Tooltip("Editar");
        tooltipEdit.setPlacement(Placement.TOP);
        Button btnEdit = new Button();
        btnEdit.addClickHandler(editClickHandler);
        btnEdit.addStyleName("btnAction");
        FormHelper.createIcon(btnEdit, "fa-pencil-square-o");
        tooltipEdit.add(btnEdit);
        buttonsBar.add(tooltipEdit);

        if ((assetEntity.getIndex() + 1) != entityCount) {
            Tooltip tooltipMoveDown = new Tooltip("Mover para baixo");
            tooltipMoveDown.setPlacement(Placement.TOP);
            Button btnMoveDown = new Button();
            btnMoveDown.addStyleName("btnSelected");
            FormHelper.createIcon(btnMoveDown, "fa-arrow-down");
            tooltipMoveDown.add(btnMoveDown);
            buttonsBar.add(tooltipMoveDown);
            btnMoveDown.addClickHandler(buildMoveClickHandler(assetType, assetEntity, "Down"));
        }

        if (assetEntity.getIndex() != 0) {
            Tooltip tooltipMoveUp = new Tooltip("Mover para cima");
            tooltipMoveUp.setPlacement(Placement.TOP);
            Button btnMoveUp = new Button();
            btnMoveUp.addStyleName("btnSelected");
            FormHelper.createIcon(btnMoveUp, "fa-arrow-up");
            tooltipMoveUp.add(btnMoveUp);
            buttonsBar.add(tooltipMoveUp);
            btnMoveUp.addClickHandler(buildMoveClickHandler(assetType, assetEntity, "Up"));
        }

        return buttonsBar;
    }

    private FlowPanel buildFileUploadPanel(final String fileName, final String contentType, String label,
            boolean exists) {
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

        // Add an ok button to the form
        Button btnOK = new Button();
        FormHelper.createIcon(btnOK, "fa-floppy-o");
        btnOK.addStyleName("btnAction");
        btnOK.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.getUploadURL(contentType, elementId, fileName);
            }
        });
        Tooltip tooltipOK = new Tooltip("Salvar");
        tooltipOK.setPlacement(Placement.TOP);
        tooltipOK.add(btnOK);
        fieldPanelWrapper.add(tooltipOK);

        if (exists) {
            // Add an delete button to the form
            Button btnDelete = new Button();
            FormHelper.createIcon(btnDelete, "fa-trash");
            btnDelete.addStyleName("btnSelected");
            btnDelete.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    confirmModal.showModal("Tem certeza que deseja excluir esta imagem?",
                            new com.google.gwt.core.client.Callback<Void, Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    presenter.delete(fileName);
                                }

                                @Override
                                public void onFailure(Void reason) {
                                }
                            });
                }
            });
            Tooltip tooltipDelete = new Tooltip("Excluir");
            tooltipDelete.setPlacement(Placement.TOP);
            tooltipDelete.add(btnDelete);
            fieldPanelWrapper.add(tooltipDelete);

            Anchor anchor = new Anchor();
            anchor.setHTML("<icon class=\"fa fa-eye\"></i>");
            anchor.setTitle("Visualizar");
            anchor.setHref(presenter.getFileURL(fileName) + "?" + System.currentTimeMillis());
            anchor.setTarget("_blank");
            anchor.getElement().setId(elementId+"-anchor");
            Tooltip tooltipView = new Tooltip("Visualizar");
            tooltipView.setPlacement(Placement.TOP);
            tooltipView.add(anchor);
            fieldPanelWrapper.add(tooltipView);
        }

        return fieldPanelWrapper;
    }

    private ClickHandler buildMoveClickHandler(String assetType, AssetEntity assetEntity, String direction) {
        return new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                bus.fireEvent(new ShowPacifierEvent(true));
                session.assets().move(assetType, assetEntity.getEntityType().toString(), assetEntity.getEntityUUID(),
                        direction, assetEntity.getIndex(), new Callback<String>() {
                            @Override
                            public void ok(String str) {
                                if (AdminAssetsPresenter.SECTION.equals(assetType)) {
                                    presenter.initCourseDetailsSections();
                                } else if (AdminAssetsPresenter.HINT.equals(assetType)) {
                                    presenter.initCourseDetailsHints();
                                } else if (AdminAssetsPresenter.LIBRARY.equals(assetType)) {
                                    presenter.initCourseDetailsLibraries();
                                }
                                bus.fireEvent(new ShowPacifierEvent(false));
                            }
                        });
            }
        };
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    private void sectionLoaded() {
        sectionsLoaded++;
        if (sectionsLoaded == sectionsCount) {
            bus.fireEvent(new ShowPacifierEvent(false));
            assetsWrapper.removeStyleName("shy");
        }
    }

    @UiHandler("sectionsAddButton")
    void doAddSection(ClickEvent e) {
        assetModal.newSection(courseDetailsEntityType, entityUUID);
    }

    @UiHandler("hintsAddButton")
    void doAddHint(ClickEvent e) {
        assetModal.newHint(courseDetailsEntityType, entityUUID);
    }

    @UiHandler("librariesAddButton")
    void doAddLibraryFile(ClickEvent e) {
        assetModal.newLibrary(courseDetailsEntityType, entityUUID);
    }

}