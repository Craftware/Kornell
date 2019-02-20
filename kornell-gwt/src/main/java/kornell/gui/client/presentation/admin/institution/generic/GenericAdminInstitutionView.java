package kornell.gui.client.presentation.admin.institution.generic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.KornellSession;
import kornell.core.entity.BillingType;
import kornell.core.entity.ContentRepository;
import kornell.core.entity.EntityFactory;
import kornell.core.entity.Institution;
import kornell.core.entity.InstitutionType;
import kornell.core.util.StringUtils;
import kornell.gui.client.GenericClientFactoryImpl;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.personnel.Dean;
import kornell.gui.client.presentation.admin.institution.AdminInstitutionPlace;
import kornell.gui.client.presentation.admin.institution.AdminInstitutionView;
import kornell.gui.client.util.CSSInjector;
import kornell.gui.client.util.entity.TermsLanguageItem;
import kornell.gui.client.util.entity.TermsLanguageItems;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.forms.formfield.KornellFormFieldWrapper;
import kornell.gui.client.util.forms.formfield.ListBoxFormField;

public class GenericAdminInstitutionView extends Composite implements AdminInstitutionView {

    interface MyUiBinder extends UiBinder<Widget, GenericAdminInstitutionView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    public static final EntityFactory entityFactory = GWT.create(EntityFactory.class);

    private KornellSession session;
    private FormHelper formHelper = GWT.create(FormHelper.class);
    private boolean isCreationMode, isPlatformAdmin, isInstitutionAdmin;
    boolean isCurrentUser, showContactDetails, isRegisteredWithCPF;

    private Presenter presenter;

    @UiField
    TabPanel tabsPanel;
    @UiField
    Tab editTab;
    @UiField
    Tab hostnamesTab;
    @UiField
    FlowPanel hostnamesPanel;
    @UiField
    Tab emailWhitelistTab;
    @UiField
    FlowPanel emailWhitelistPanel;
    @UiField
    Tab reportsTab;
    @UiField
    FlowPanel reportsPanel;
    @UiField
    Tab adminsTab;
    @UiField
    FlowPanel adminsPanel;
    @UiField
    Tab assetsTab;
    @UiField
    FlowPanel assetsPanel;
    @UiField
    Tab repoTab;
    @UiField
    FlowPanel repoPanel;

    @UiField
    HTMLPanel titleEdit;
    @UiField
    Form form;
    @UiField
    FlowPanel institutionFields;
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

    private Institution institution;
    private ContentRepository repo;

    private KornellFormFieldWrapper name, fullName, institutionType, assetsRepositoryUUID, baseURL, billingType,
    demandsPersonContactDetails, validatePersonContactDetails, allowRegistration, allowRegistrationByUsername,
    advancedMode, useEmailWhitelist, timeZone, skin, institutionSupportEmail, notifyInstitutionAdmins,
    allowedLanguages;

    private List<KornellFormFieldWrapper> fields;
    private Map<String, KornellFormFieldWrapper> termsFieldsMap;
    private GenericInstitutionReportsView reportsView;
    private GenericInstitutionAdminsView adminsView;
    private GenericInstitutionAssetsView assetsView;
    private GenericInstitutionHostnamesView hostnamesView;
    private GenericInstitutionEmailWhitelistView emailWhitelistView;
    private GenericInstitutionRepositoryView repoView;
    private EventBus bus;

    public GenericAdminInstitutionView(final KornellSession session, EventBus bus, PlaceController placeCtrl,
            ViewFactory viewFactory) {
        this.session = session;
        this.bus = bus;
        initWidget(uiBinder.createAndBindUi(this));

        // i18n
        btnOK.setText("Salvar".toUpperCase());
        btnCancel.setText(isCreationMode ? "Cancelar".toUpperCase() : "Limpar".toUpperCase());

        btnModalOK.setText("OK".toUpperCase());
        btnModalCancel.setText("Cancelar".toUpperCase());
    }

    @Override
    public void init() {
        institution = session.getInstitution();
        isPlatformAdmin = session.isPlatformAdmin();
        isInstitutionAdmin = session.isInstitutionAdmin();

        if (isPlatformAdmin) {
            session.repository().getRepository(institution.getAssetsRepositoryUUID(),
                    new kornell.api.client.Callback<ContentRepository>() {
                @Override
                public void ok(ContentRepository to) {
                    repo = to;
                    buildRepoView();
                    repoTab.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            buildRepoView();
                        }
                    });
                }
            });

            buildReportsView();
            reportsTab.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    buildReportsView();
                }
            });
        } else {
            FormHelper.hideTab(reportsTab);
            FormHelper.hideTab(repoTab);
        }

        initData();

        bus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
            @Override
            public void onPlaceChange(PlaceChangeEvent event) {
                if (event.getNewPlace() instanceof AdminInstitutionPlace) {
                    initData();
                }
            }
        });

        if (session.isInstitutionAdmin()) {
            hostnamesTab.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    buildHostnamesView();
                }
            });

            emailWhitelistTab.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    buildEmailWhitelistView();
                }
            });

            adminsTab.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    buildAdminsView();
                }
            });

            assetsTab.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    buildAssetsView();
                }
            });
        }
    }

    public void buildHostnamesView() {
        hostnamesView = new GenericInstitutionHostnamesView(session, bus, presenter, institution);
        hostnamesPanel.clear();
        hostnamesPanel.add(hostnamesView);
    }

    public void buildEmailWhitelistView() {
        emailWhitelistView = new GenericInstitutionEmailWhitelistView(session, bus, presenter, institution);
        emailWhitelistPanel.clear();
        emailWhitelistPanel.add(emailWhitelistView);
    }

    public void buildReportsView() {
        if (reportsView == null) {
            reportsView = new GenericInstitutionReportsView(session, bus, institution);
        }
        reportsPanel.clear();
        reportsPanel.add(reportsView);
    }

    public void buildRepoView() {
        if (repoView == null) {
            repoView = new GenericInstitutionRepositoryView(session, presenter, institution, repo);
        }
        repoPanel.clear();
        repoPanel.add(repoView);
    }

    public void buildAdminsView() {
        adminsView = new GenericInstitutionAdminsView(session, bus, presenter, institution);
        adminsPanel.clear();
        adminsPanel.add(adminsView);
    }

    public void buildAssetsView() {
        assetsView = new GenericInstitutionAssetsView(session, bus, presenter, institution);
        assetsPanel.clear();
        assetsPanel.add(assetsView);
    }

    private void initData() {
        institutionFields.setVisible(false);
        this.fields = new ArrayList<KornellFormFieldWrapper>();
        this.termsFieldsMap = new HashMap<>();

        institutionFields.clear();

        btnOK.setVisible(isInstitutionAdmin || isCreationMode);
        btnCancel.setVisible(isInstitutionAdmin);

        fullName = new KornellFormFieldWrapper("Nome da Instituição",
                formHelper.createTextBoxFormField(institution.getFullName()), isInstitutionAdmin);
        fields.add(fullName);
        institutionFields.add(fullName);

        name = new KornellFormFieldWrapper("Sub-domínio da Instituição",
                formHelper.createTextBoxFormField(institution.getName()), isPlatformAdmin);
        fields.add(name);
        institutionFields.add(name);

        baseURL = new KornellFormFieldWrapper("URL Base", formHelper.createTextBoxFormField(institution.getBaseURL()),
                isPlatformAdmin);
        fields.add(baseURL);
        institutionFields.add(baseURL);

        if (isPlatformAdmin) {
            final ListBox institutionTypes = new ListBox();
            institutionTypes.addItem("Padrão", InstitutionType.DEFAULT.toString());
            institutionTypes.addItem("Dashboard", InstitutionType.DASHBOARD.toString());
            if (!isCreationMode) {
                institutionTypes.setSelectedValue(institution.getInstitutionType().toString());
            }
            institutionType = new KornellFormFieldWrapper("Tipo de Instituição", new ListBoxFormField(institutionTypes),
                    isPlatformAdmin);
            fields.add(institutionType);
            institutionFields.add(institutionType);

            assetsRepositoryUUID = new KornellFormFieldWrapper("UUID do repositório",
                    formHelper.createTextBoxFormField(institution.getAssetsRepositoryUUID()), isPlatformAdmin);
            fields.add(assetsRepositoryUUID);
            institutionFields.add(assetsRepositoryUUID);

            final ListBox billingTypes = new ListBox();
            billingTypes.addItem("Mensal", BillingType.monthly.toString());
            billingTypes.addItem("Matrícula", BillingType.enrollment.toString());
            if (!isCreationMode) {
                billingTypes.setSelectedValue(institution.getBillingType().toString());
            }
            billingType = new KornellFormFieldWrapper("Tipo de Cobrança", new ListBoxFormField(billingTypes),
                    isPlatformAdmin);
            fields.add(billingType);
            institutionFields.add(billingType);
        }

        demandsPersonContactDetails = new KornellFormFieldWrapper("Exige Detalhes de Contato",
                formHelper.createCheckBoxFormField(institution.isDemandsPersonContactDetails()), isInstitutionAdmin);
        fields.add(demandsPersonContactDetails);
        institutionFields.add(demandsPersonContactDetails);
        ((CheckBox) demandsPersonContactDetails.getFieldWidget())
        .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                if (event.getValue()) {
                }
            }
        });

        validatePersonContactDetails = new KornellFormFieldWrapper("Validação dos Detalhes de Contato",
                formHelper.createCheckBoxFormField(institution.isValidatePersonContactDetails()), isInstitutionAdmin);
        fields.add(validatePersonContactDetails);
        institutionFields.add(validatePersonContactDetails);
        ((CheckBox) validatePersonContactDetails.getFieldWidget())
        .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                if (event.getValue()) {
                }
            }
        });

        allowRegistration = new KornellFormFieldWrapper("Permitir Registro",
                formHelper.createCheckBoxFormField(institution.isAllowRegistration()), isInstitutionAdmin);
        fields.add(allowRegistration);
        institutionFields.add(allowRegistration);
        ((CheckBox) allowRegistration.getFieldWidget()).addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                if (event.getValue()) {
                }
            }
        });

        final ListBox timeZones = formHelper.getTimeZonesList();
        if (institution.getTimeZone() != null) {
            timeZones.setSelectedValue(institution.getTimeZone());
        }
        timeZone = new KornellFormFieldWrapper("Fuso horário", new ListBoxFormField(timeZones), isInstitutionAdmin);
        fields.add(timeZone);
        institutionFields.add(timeZone);

        final ListBox skins = formHelper.getSkinsList();
        if (institution.getSkin() != null) {
            skins.setSelectedValue(institution.getSkin());
        } else {
            skins.setSelectedValue("");
        }
        skin = new KornellFormFieldWrapper("Tema visual", new ListBoxFormField(skins), isPlatformAdmin);
        fields.add(skin);
        institutionFields.add(skin);
        ((ListBox) skin.getFieldWidget()).addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                Dean.showContentNative(false);

                Callback<Void, Exception> callback = new Callback<Void, Exception>() {
                    @Override
                    public void onFailure(Exception reason) {
                        Window.Location.reload();
                    }

                    @Override
                    public void onSuccess(Void result) {
                        Dean.showContentNative(true);
                    }
                };

                CSSInjector.updateSkin(skin.getFieldPersistText(), callback);
            }
        });

        institutionSupportEmail = new KornellFormFieldWrapper("E-mail de suporte",
                formHelper.createTextBoxFormField(institution.getInstitutionSupportEmail()), isPlatformAdmin);
        fields.add(institutionSupportEmail);
        institutionFields.add(institutionSupportEmail);

        if (isPlatformAdmin) {
            useEmailWhitelist = new KornellFormFieldWrapper("Configurar domínios para emails",
                    formHelper.createCheckBoxFormField(institution.isUseEmailWhitelist()), isInstitutionAdmin);
            fields.add(useEmailWhitelist);
            institutionFields.add(useEmailWhitelist);
            ((CheckBox) useEmailWhitelist.getFieldWidget()).addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    if (event.getValue()) {
                    }
                }
            });

            allowRegistrationByUsername = new KornellFormFieldWrapper("Permitir Registro por Usuário",
                    formHelper.createCheckBoxFormField(institution.isAllowRegistrationByUsername()), isPlatformAdmin);
            fields.add(allowRegistrationByUsername);
            institutionFields.add(allowRegistrationByUsername);
            ((CheckBox) allowRegistrationByUsername.getFieldWidget())
            .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    if (event.getValue()) {
                    }
                }
            });

            advancedMode = new KornellFormFieldWrapper("Modo avançado",
                    formHelper.createCheckBoxFormField(institution.isAdvancedMode()), isPlatformAdmin);
            fields.add(advancedMode);
            institutionFields.add(advancedMode);
            ((CheckBox) advancedMode.getFieldWidget()).addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    if (event.getValue()) {
                    }
                }
            });

            notifyInstitutionAdmins = new KornellFormFieldWrapper("Notificar certificações",
                    formHelper.createCheckBoxFormField(institution.isNotifyInstitutionAdmins()), isPlatformAdmin, null,
                    "Enviar um email para todos os administradores da institução toda vez que um aluno concluir o conteúdo.");

            fields.add(notifyInstitutionAdmins);
            institutionFields.add(notifyInstitutionAdmins);
            ((CheckBox) notifyInstitutionAdmins.getFieldWidget())
            .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    if (event.getValue()) {
                    }
                }
            });
        }

        allowedLanguages = new KornellFormFieldWrapper("Línguas Disponíveis",
                formHelper.createTextBoxFormField(institution.getAllowedLanguages()), isInstitutionAdmin, null,
                "Línguas disponíveis: \"pt_BR\" e \"en\". Use vírgulas se deseja usar mais de uma língua.");
        fields.add(allowedLanguages);
        institutionFields.add(allowedLanguages);

        String[] allowedLanguagesArr = institution.getAllowedLanguages().split(",");
        if(StringUtils.isSome(allowedLanguagesArr[0])){
            for (int i = 0; i < allowedLanguagesArr.length; i++) {
                KornellFormFieldWrapper terms = new KornellFormFieldWrapper("Termos de Uso - " + allowedLanguagesArr[i],
                        formHelper.createTextAreaFormField(getTermsForLanguage(allowedLanguagesArr[i]), 20),
                        isInstitutionAdmin);
                terms.addStyleName("heightAuto");
                terms.addStyleName("marginBottom25");
                fields.add(terms);
                termsFieldsMap.put(allowedLanguagesArr[i], terms);
                institutionFields.add(terms);
            }
        }

        institutionFields.add(formHelper.getImageSeparator());
        institutionFields.setVisible(true);
    }

    private String getTermsForLanguage(String language) {
        AutoBeanFactory factory = GenericClientFactoryImpl.GUI_ENTITY_FACTORY;
        try {
            AutoBean<TermsLanguageItems> bean = AutoBeanCodex.decode(factory, TermsLanguageItems.class,
                    institution.getTerms());
            TermsLanguageItems list = bean.as();
            for (TermsLanguageItem termsLanguageItem : list.getTermsLanguageItems()) {
                if (termsLanguageItem.getLanguage().equals(language)) {
                    String terms = termsLanguageItem.getTerms();
                    terms = terms.replaceAll("\\\\\"", "\"");
                    return terms;
                }
            }
        } catch (Exception e) {
        }
        return "";
    }

    private boolean validateFields() {
        if (!formHelper.isLengthValid(name.getFieldPersistText(), 2, 20)) {
            name.setError("Insira o sub-domínio da instituição.");
        }
        if (!formHelper.isLengthValid(fullName.getFieldPersistText(), 2, 50)) {
            fullName.setError("Insira o nome da instituição.");
        }
        if (!formHelper.isLengthValid(baseURL.getFieldPersistText(), 10, 200)) {
            baseURL.setError("Insira a URL base.");
        }
        if (isPlatformAdmin) {
            if (!formHelper.isLengthValid(assetsRepositoryUUID.getFieldPersistText(), 10, 200)) {
                assetsRepositoryUUID.setError("Insira o UUID do repositório.");
            }
        }
        if (!formHelper.isLengthValid(timeZone.getFieldPersistText(), 2, 100)) {
            timeZone.setError("Escolha o fuso horário.");
        }

        /*for (Entry<String, KornellFormFieldWrapper> entry : termsFieldsMap.entrySet()) {
            String language = entry.getKey();
            String terms = entry.getValue().getFieldPersistText().replace("\"", "\\\"");
            if (!formHelper.isLengthValid(terms, 10)) {
                entry.getValue().setError(
                        "Coloque os termos para " + ("pt_BR".equals(language) ? "Português" : "Inglês") + ".");
            }
        }*/

        return !formHelper.checkErrors(fields);
    }

    @UiHandler("btnOK")
    void doOK(ClickEvent e) {
        formHelper.clearErrors(fields);
        if (isInstitutionAdmin && validateFields()) {
            bus.fireEvent(new ShowPacifierEvent(true));
            Institution institution = getInstitutionInfoFromForm();
            presenter.updateInstitution(institution);

        }
    }

    private Institution getInstitutionInfoFromForm() {
        institution.setName(name.getFieldPersistText());
        institution.setFullName(fullName.getFieldPersistText());
        institution.setTerms(getTermsJson());
        institution.setBaseURL(baseURL.getFieldPersistText());
        institution.setDemandsPersonContactDetails(demandsPersonContactDetails.getFieldPersistText().equals("true"));
        institution.setValidatePersonContactDetails(validatePersonContactDetails.getFieldPersistText().equals("true"));
        institution.setAllowRegistration(allowRegistration.getFieldPersistText().equals("true"));
        institution.setTimeZone(timeZone.getFieldPersistText());
        if (isPlatformAdmin) {
            institution.setAssetsRepositoryUUID(assetsRepositoryUUID.getFieldPersistText());
            institution.setBillingType(BillingType.valueOf(billingType.getFieldPersistText()));
            institution.setInstitutionType(InstitutionType.valueOf(institutionType.getFieldPersistText()));
            institution
            .setAllowRegistrationByUsername(allowRegistrationByUsername.getFieldPersistText().equals("true"));
            institution.setAdvancedMode(advancedMode.getFieldPersistText().equals("true"));
            institution.setUseEmailWhitelist(useEmailWhitelist.getFieldPersistText().equals("true"));
            institution.setSkin(skin.getFieldPersistText());
            institution.setInstitutionSupportEmail(institutionSupportEmail.getFieldPersistText());
            institution.setNotifyInstitutionAdmins(notifyInstitutionAdmins.getFieldPersistText().equals("true"));
            institution.setAllowedLanguages(allowedLanguages.getFieldPersistText());
        }
        return institution;
    }

    private String getTermsJson() {
        TermsLanguageItems termsLanguageItems = GenericClientFactoryImpl.GUI_ENTITY_FACTORY.newTermsLanguageItems()
                .as();
        List<TermsLanguageItem> termsLanguageItemsList = new ArrayList<>();
        TermsLanguageItem termsLanguageItem;
        for (Entry<String, KornellFormFieldWrapper> entry : termsFieldsMap.entrySet()) {
            termsLanguageItem = GenericClientFactoryImpl.GUI_ENTITY_FACTORY.newTermsLanguageItem().as();
            termsLanguageItem.setLanguage(entry.getKey());
            termsLanguageItem.setTerms(entry.getValue().getFieldPersistText().replace("\"", "\\\""));
            termsLanguageItemsList.add(termsLanguageItem);
        }
        termsLanguageItems.setTermsLanguageItems(termsLanguageItemsList);
        return AutoBeanCodex.encode(AutoBeanUtils.getAutoBean(termsLanguageItems)).getPayload();
    }

    @UiHandler("btnCancel")
    void doCancel(ClickEvent e) {
        initData();
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

}
