package kornell.gui.client.presentation.profile.generic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Logger;

import com.github.gwtbootstrap.client.ui.Alert;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.Country;
import kornell.core.entity.InstitutionRegistrationPrefix;
import kornell.core.entity.InstitutionType;
import kornell.core.entity.Person;
import kornell.core.entity.RegistrationType;
import kornell.core.entity.role.RoleCategory;
import kornell.core.entity.role.RoleType;
import kornell.core.error.KornellErrorTO;
import kornell.core.to.UserHelloTO;
import kornell.core.to.UserInfoTO;
import kornell.core.util.StringUtils;
import kornell.gui.client.ClientFactory;
import kornell.gui.client.GenericClientFactoryImpl;
import kornell.gui.client.KornellConstants;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.event.LogoutEvent;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.presentation.profile.ProfilePlace;
import kornell.gui.client.presentation.profile.ProfileView;
import kornell.gui.client.util.AsciiUtils;
import kornell.gui.client.util.EnumTranslator;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.forms.formfield.KornellFormFieldWrapper;
import kornell.gui.client.util.forms.formfield.ListBoxFormField;
import kornell.gui.client.util.forms.formfield.SimpleDatePicker;
import kornell.gui.client.util.forms.formfield.SimpleDatePickerFormField;
import kornell.gui.client.util.forms.formfield.TextBoxFormField;
import kornell.gui.client.util.validation.CPFValidator;
import kornell.gui.client.util.validation.EmailValidator;
import kornell.gui.client.util.validation.ValidationChangedHandler;
import kornell.gui.client.util.view.FlagsPanel;
import kornell.gui.client.util.view.KornellNotification;

public class GenericProfileView extends Composite implements ProfileView, ValidationChangedHandler {

    interface MyUiBinder extends UiBinder<Widget, GenericProfileView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private static final Logger logger = Logger.getLogger(GenericProfileView.class.getName());
    private static KornellConstants constants = GWT.create(KornellConstants.class);

    private KornellSession session;
    private ViewFactory viewFactory;
    private PlaceController placeCtrl;
    private final EventBus bus;
    private FormHelper formHelper;
    private boolean isEditMode, isCurrentUser, isAdmin, hasPowerOver, showEmail = true, showCPF = true,
            showContactDetails, validateContactDetails;

    private String DISABLED_CLASS = "btnNotSelected";
    private String ENABLED_CLASS = "btnAction";
    private String CURSOR_DEFAULT_CLASS = "cursorDefault";
    private String CURSOR_POINTER_CLASS = "cursorPointer";

    @UiField
    Form form;
    @UiField
    FlowPanel profileFields;
    @UiField
    FlowPanel btnPanelBottom;
    @UiField
    GenericPasswordChangeView passwordChangeWidget;
    @UiField
    GenericSendMessageView sendMessageWidget;

    private UserInfoTO user;

    private KornellFormFieldWrapper cpf, email, username, fullName, telephone, country, state, city, addressLine1,
            addressLine2, postalCode, company, position, sex, birthDate, receiveEmailCommunication;
    private List<KornellFormFieldWrapper> fields;
    private Button btnChangePassword, btnSendMessage, btnEdit, btnClose, btnOK, btnCancel;
    private Button btnChangePassword2, btnSendMessage2, btnEdit2, btnClose2, btnOK2, btnCancel2;
    private ClientFactory clientFactory;

    private String profileUserUUID;
    private FlagsPanel flagsPanel;

    public GenericProfileView(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        this.viewFactory = clientFactory.getViewFactory();
        this.bus = clientFactory.getEventBus();
        this.session = clientFactory.getKornellSession();
        this.user = session.getCurrentUser();
        this.placeCtrl = clientFactory.getPlaceController();
        this.fields = new ArrayList<KornellFormFieldWrapper>();
        formHelper = new FormHelper();
        initWidget(uiBinder.createAndBindUi(this));

        btnEdit = createButton(constants.editButton(), "btnAction btnPlaceBar", false, new ClickHandler() {
            @Override
            public void onClick(ClickEvent e) {
                doEdit(e);
            }
        });
        btnEdit2 = createButton(constants.editButton(), "btnAction btnBottom", false, new ClickHandler() {
            @Override
            public void onClick(ClickEvent e) {
                doEdit(e);
            }
        });
        btnClose = createButton(constants.closeButton(), "btnNotSelected btnPlaceBar", false, new ClickHandler() {
            @Override
            public void onClick(ClickEvent e) {
                doClose(e);
            }
        });
        btnClose2 = createButton(constants.closeButton(), "btnNotSelected btnBottom", false, new ClickHandler() {
            @Override
            public void onClick(ClickEvent e) {
                doClose(e);
            }
        });
        btnOK = createButton(constants.saveButton(), "btnAction btnPlaceBar", false, new ClickHandler() {
            @Override
            public void onClick(ClickEvent e) {
                doOK(e);
            }
        });
        btnOK2 = createButton(constants.saveButton(), "btnAction btnBottom", false, new ClickHandler() {
            @Override
            public void onClick(ClickEvent e) {
                doOK(e);
            }
        });
        btnCancel = createButton(constants.cancelButton(), "btnNotSelected btnPlaceBar", false, new ClickHandler() {
            @Override
            public void onClick(ClickEvent e) {
                doCancel(e);
            }
        });
        btnCancel2 = createButton(constants.cancelButton(), "btnNotSelected btnBottom", false, new ClickHandler() {
            @Override
            public void onClick(ClickEvent e) {
                doCancel(e);
            }
        });
        btnChangePassword = createButton(constants.changePasswordButton(), "btnSelected btnPlaceBar", false,
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent e) {
                        passwordChangeWidget.show();
                    }
                });
        btnChangePassword2 = createButton(constants.changePasswordButton(), "btnSelected btnBottom", false,
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent e) {
                        passwordChangeWidget.show();
                    }
                });
        btnSendMessage = createButton(constants.sendMessageButton(), "btnNotSelected btnPlaceBar", true,
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent e) {
                        sendMessageWidget.show();
                    }
                });
        btnSendMessage2 = createButton(constants.sendMessageButton(), "btnNotSelected btnBottom", true,
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent e) {
                        sendMessageWidget.show();
                    }
                });

        initData();

        bus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
            @Override
            public void onPlaceChange(PlaceChangeEvent event) {
                if (event.getNewPlace() instanceof ProfilePlace) {
                    initData();
                }
            }
        });
    }

    private Button createButton(String text, String className, boolean visible, ClickHandler clickHandler) {
        Button btn = new Button();
        btn.setVisible(visible);
        btn.setText(text);
        btn.addStyleName(className);
        btn.addStyleName("btnStandard");
        btn.addClickHandler(clickHandler);
        return btn;
    }

    private void initData() {

        viewFactory.getMenuBarView().initPlaceBar(IconType.USER, constants.profileTitle(),
                constants.profileDescription());

        isCurrentUser = session.getCurrentUser().getPerson().getUUID()
                .equals(((ProfilePlace) placeCtrl.getWhere()).getPersonUUID());
        isEditMode = ((ProfilePlace) placeCtrl.getWhere()).isEdit() && isCurrentUser;
        isAdmin = RoleCategory.hasRole(session.getCurrentUser().getRoles(), RoleType.courseClassAdmin)
                || session.isInstitutionCourseClassesAdmin()
                || session.isInstitutionAdmin()
                || session.isPlatformAdmin();

        form.addStyleName("shy");

        profileUserUUID = ((ProfilePlace) placeCtrl.getWhere()).getPersonUUID();

        if (isCurrentUser) {
            if (flagsPanel == null) {
                flagsPanel = new FlagsPanel();
                form.insert(flagsPanel, 0);
            }
            flagsPanel.setVisible(true);
        } else {
            if (flagsPanel != null) {
                flagsPanel.setVisible(false);
            }
        }

        session.user().hasPowerOver(profileUserUUID, new Callback<Boolean>() {
            @Override
            public void ok(Boolean hasPowerOverTargetUser) {
                List<IsWidget> widgets = new ArrayList<IsWidget>();
                List<IsWidget> widgets2 = new ArrayList<IsWidget>();
                if (!isCurrentUser) {
                    widgets.add(btnSendMessage);
                    widgets2.add(btnSendMessage2);
                }
                if (hasPowerOverTargetUser) {
                    widgets.add(btnChangePassword);
                    widgets2.add(btnChangePassword2);
                }
                widgets.add(btnCancel);
                widgets2.add(btnCancel2);
                // widgets.add(btnClose);
                // widgets2.add(btnClose2);
                widgets.add(btnEdit);
                widgets2.add(btnEdit2);
                widgets.add(btnOK);
                widgets2.add(btnOK2);
                viewFactory.getMenuBarView().setPlaceBarWidgets(widgets);
                buildButtonBar(widgets2);

                hasPowerOver = hasPowerOverTargetUser;
                session.user().getUser(profileUserUUID, new Callback<UserInfoTO>() {
                    @Override
                    public void ok(UserInfoTO to) {
                        user = to;
                        display();
                    }

                    @Override
                    public void notFound(KornellErrorTO kornellErrorTO) {
                        logger.severe(this.getClass().getName() + " - not found");
                        user = null;
                        display();
                    }
                });
            }
        });
    }

    private void buildButtonBar(List<IsWidget> widgets) {
        btnPanelBottom.clear();
        ListIterator<IsWidget> li = widgets.listIterator(widgets.size());
        // Iterate in reverse.
        while (li.hasPrevious()) {
            btnPanelBottom.add(li.previous());
        }
    }

    private boolean validateFields() {
        if (showEmail && !FormHelper.isEmailValid(email.getFieldPersistText())) {
            email.setError(constants.invalidEmail());
        }

        if (!formHelper.isLengthValid(fullName.getFieldPersistText(), 5, 50)) {
            fullName.setError(constants.missingNameMessage());
        }

        if (showContactDetails && validateContactDetails) {
            if (!formHelper.isLengthValid(telephone.getFieldPersistText(), 7, 20)) {
                telephone.setError(constants.missingTelephoneMessage());
            } else
                telephone.setError("");

            if (!formHelper.isLengthValid(country.getFieldPersistText(), 0, 2)) {
                country.setError(constants.missingCountryMessage());
            } else
                country.setError("");

            if ("BR".equals(country.getFieldPersistText())) {
                if (!formHelper.isListBoxSelected(((ListBox) state.getFieldWidget()))) {
                    state.setError(constants.selectStateMessage());
                } else
                    state.setError("");
            } else {
                if (!formHelper.isLengthValid(state.getFieldPersistText(), 2, 100)) {
                    state.setError(constants.missingStateMessage());
                } else
                    state.setError("");
            }
            if (!formHelper.isLengthValid(city.getFieldPersistText(), 2, 100)) {
                city.setError(constants.missingCityMessage());
            } else
                city.setError("");

            if (!formHelper.isLengthValid(addressLine1.getFieldPersistText(), 2, 100)) {
                addressLine1.setError(constants.missingAddressMessage());
            } else
                addressLine1.setError("");

            if (!formHelper.isLengthValid(postalCode.getFieldPersistText(), 2, 100)) {
                postalCode.setError(constants.missingPostalCodeMessage());
            } else
                postalCode.setError("");

        }

        return !checkErrors();
    }

    void doOK(ClickEvent e) {
        formHelper.clearErrors(fields);

        if (isEditMode && validateFields()) {
            bus.fireEvent(new ShowPacifierEvent(true));
            session.user().updateUser(getUserInfoFromForm(), new Callback<UserInfoTO>() {
                @Override
                public void ok(UserInfoTO userInfo) {
                    bus.fireEvent(new ShowPacifierEvent(false));
                    user = userInfo;
                    KornellNotification.show(constants.confirmSaveProfile());
                    btnOK.setEnabled(true);
                    btnOK2.setEnabled(true);
                    isEditMode = false;
                    display();
                    session.fetchUser(new Callback<UserHelloTO>() {
                        @Override
                        public void ok(UserHelloTO to) {
                            user = to.getUserInfoTO();
                        }
                    });
                    if (!InstitutionType.DASHBOARD.equals(session.getInstitution().getInstitutionType())) {
                        if (isCurrentUser) {
                            placeCtrl.goTo(clientFactory.getDefaultPlace());
                        } else {
                            History.back();
                        }
                    }
                }

                @Override
                public void unauthorized(KornellErrorTO kornellErrorTO) {
                    bus.fireEvent(new ShowPacifierEvent(false));
                    KornellNotification.show(constants.errorSaveProfile(), AlertType.ERROR);
                }
            });
        }
    }

    private UserInfoTO getUserInfoFromForm() {
        // "clone" user
        String userPayload = AutoBeanCodex.encode(AutoBeanUtils.getAutoBean(user)).getPayload();
        UserInfoTO userTmp = AutoBeanCodex
                .decode(GenericClientFactoryImpl.TO_FACTORY, kornell.core.to.UserInfoTO.class, userPayload).as();
        Person person = userTmp.getPerson();

        if (showCPF) {
            person.setCPF(FormHelper.stripCPF(cpf.getFieldPersistText()));
        }
        if (showEmail) {
            person.setEmail(email.getFieldPersistText());
        }
        person.setFullName(fullName.getFieldPersistText());
        person.setCompany(company.getFieldPersistText());
        person.setTitle(position.getFieldPersistText());
        person.setSex(sex.getFieldPersistText());
        if (StringUtils.isSome(birthDate.getFieldPersistText())) {
            person.setBirthDate(DateTimeFormat.getFormat("yyyy-MM-dd").parse(birthDate.getFieldPersistText()));
        }

        if (showContactDetails) {
            person.setTelephone(telephone.getFieldPersistText());
            person.setCountry(country.getFieldPersistText());
            person.setState(state.getFieldPersistText());
            person.setCity(city.getFieldPersistText());
            person.setAddressLine1(addressLine1.getFieldPersistText());
            person.setAddressLine2(addressLine2.getFieldPersistText());
            person.setPostalCode(postalCode.getFieldPersistText());
        }

        person.setReceiveEmailCommunication(receiveEmailCommunication.getFieldPersistText().equals("true"));

        if ("".equals(person.getCPF()))
            person.setCPF(null);
        if ("".equals(person.getEmail()))
            person.setEmail(null);

        userTmp.setPerson(person);
        return userTmp;
    }

    void doCancel(ClickEvent e) {
        if (showContactDetails && validateContactDetails && session.getCurrentUser().getPerson().getCity() == null) {
            bus.fireEvent(new LogoutEvent());
        } else {
            isEditMode = false;
            formHelper.clearErrors(fields);
            display();
        }
    }

    void doEdit(ClickEvent e) {
        isEditMode = true;
        formHelper.clearErrors(fields);
        display();
    }

    void doClose(ClickEvent e) {
        form.addStyleName("shy");
        History.back();
        // placeCtrl.goTo(clientFactory.getDefaultPlace());
    }

    private boolean checkErrors() {
        for (KornellFormFieldWrapper field : fields)
            if (!"".equals(field.getError())) {
                if (errorAlert == null || !errorAlert.isVisible()) {
                    errorAlert = KornellNotification.show(constants.formContainsErrors(), AlertType.WARNING, 3000);
                }
                if (field.getFieldWidget() instanceof FocusWidget)
                    ((FocusWidget) field.getFieldWidget()).setFocus(true);
                return true;
            }
        return false;
    }

    private void display() {

        showContactDetails = session.getInstitution().isDemandsPersonContactDetails();
        validateContactDetails = session.getInstitution().isValidatePersonContactDetails() && isCurrentUser;

        form.addStyleName("shy");

        btnOK.setVisible(isEditMode);
        btnOK2.setVisible(isEditMode);
        btnCancel.setVisible(isEditMode);
        btnCancel2.setVisible(isEditMode);
        btnClose.setVisible(!isEditMode);
        btnClose2.setVisible(!isEditMode);
        btnChangePassword.setVisible(isEditMode && (isCurrentUser || hasPowerOver));
        btnChangePassword2.setVisible(isEditMode && (isCurrentUser || hasPowerOver));
        btnEdit.setVisible(!isEditMode && (isCurrentUser || hasPowerOver));
        btnEdit2.setVisible(!isEditMode && (isCurrentUser || hasPowerOver));

        profileFields.clear();
        if (user == null) {
            KornellNotification.show(constants.userNotFound(), AlertType.ERROR);
            return;
        }

        if (RegistrationType.username.equals(user.getPerson().getRegistrationType())) {
            InstitutionRegistrationPrefix institutionRegistrationPrefix = user.getInstitutionRegistrationPrefix();
            showEmail = institutionRegistrationPrefix.isShowEmailOnProfile();
            showCPF = institutionRegistrationPrefix.isShowCPFOnProfile();
            showContactDetails = showContactDetails
                    && institutionRegistrationPrefix.isShowContactInformationOnProfile();
        }

        if (isEditMode && showContactDetails && validateContactDetails
                && session.getCurrentUser().getPerson().getCity() == null) {
            KornellNotification.show(constants.pleaseCompleteRegistrationMessage(), AlertType.WARNING, 5000);
        }

        // profileFields.add(getPictureUploadFormPanel());

        username = new KornellFormFieldWrapper(constants.usernameLabel(),
                formHelper.createTextBoxFormField(user.getUsername()), false);
        fields.add(username);
        profileFields.add(username);

        fullName = new KornellFormFieldWrapper(constants.fullnameLabel(),
                formHelper.createTextBoxFormField(user.getPerson().getFullName()), isEditMode);
        fields.add(fullName);
        profileFields.add(fullName);

        KornellSession session = clientFactory.getKornellSession();
        if (showEmail) {
            email = new KornellFormFieldWrapper(constants.emailLabel(),
                    formHelper.createTextBoxFormField(user.getPerson().getEmail()), isEditMode,
                    EmailValidator.unregisteredEmailValidator(profileUserUUID, session));
            requireValid(email);
            fields.add(email);
            profileFields.add(email);
        }
        if (showCPF && (isCurrentUser || isAdmin)) {
            cpf = new KornellFormFieldWrapper(constants.cpfLabel(),
                    formHelper.createTextBoxFormField(user.getPerson().getCPF()), isEditMode,
                    CPFValidator.unregisteredCPFValidator(profileUserUUID, session));
            requireValid(cpf);
            fields.add(cpf);
            profileFields.add(cpf);
        }

        company = new KornellFormFieldWrapper(constants.companyLabel(),
                formHelper.createTextBoxFormField(user.getPerson().getCompany()), isEditMode);
        fields.add(company);
        profileFields.add(company);

        position = new KornellFormFieldWrapper(constants.posititonLabel(),
                formHelper.createTextBoxFormField(user.getPerson().getTitle()), isEditMode);
        fields.add(position);
        profileFields.add(position);

        if (isCurrentUser || isAdmin) {
            final ListBox sexes = formHelper.getSexList();
            sexes.setSelectedValue(user.getPerson().getSex());
            sex = new KornellFormFieldWrapper(constants.genderLabel(), new ListBoxFormField(sexes), isEditMode);
            fields.add(sex);
            profileFields.add(sex);

            SimpleDatePicker datePicker = new SimpleDatePicker();
            if ((isEditMode || isCurrentUser || isAdmin) && user.getPerson().getBirthDate() != null) {
                datePicker.setFields(user.getPerson().getBirthDate());
            }
            birthDate = new KornellFormFieldWrapper(constants.birthDateLabel(),
                    new SimpleDatePickerFormField(datePicker), isEditMode);
            fields.add(birthDate);
            profileFields.add(birthDate);
        }

        receiveEmailCommunication = new KornellFormFieldWrapper(constants.receiveEmailCommunicationLabel(),
                formHelper.createCheckBoxFormField(user.getPerson().isReceiveEmailCommunication()), isEditMode);
        fields.add(receiveEmailCommunication);
        profileFields.add(receiveEmailCommunication);
        ((CheckBox) receiveEmailCommunication.getFieldWidget())
                .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                    @Override
                    public void onValueChange(ValueChangeEvent<Boolean> event) {
                        if (event.getValue()) {
                        }
                    }
                });

        if ((isCurrentUser || isAdmin) && showContactDetails) {
            displayContactDetails();
        }

        form.removeStyleName("shy");
        setValidity(true);

        passwordChangeWidget.initData(session, bus, user);
        sendMessageWidget.initData(session, bus, user, isCurrentUser);

    }

    List<KornellFormFieldWrapper> requiredFields = new ArrayList<KornellFormFieldWrapper>();
    private Alert errorAlert;

    private void requireValid(KornellFormFieldWrapper field) {
        requiredFields.add(field);
        field.addValidationListener(this);
    }

    @Override
    public void onValidationChanged() {
        boolean isValid = true;
        for (Iterator<KornellFormFieldWrapper> it = requiredFields.iterator(); it.hasNext();) {
            KornellFormFieldWrapper field = (KornellFormFieldWrapper) it.next();
            isValid = isValid && field.isValid();
        }
        checkErrors();
        setValidity(isValid);
    }

    void setValidity(boolean isValid) {
        btnOK.setEnabled(isValid);
        btnOK2.setEnabled(isValid);
        if (isValid) {
            btnOK.removeStyleName(DISABLED_CLASS);
            btnOK.addStyleName(ENABLED_CLASS);
            btnOK.removeStyleName(CURSOR_DEFAULT_CLASS);
            btnOK.addStyleName(CURSOR_POINTER_CLASS);
            btnOK2.removeStyleName(DISABLED_CLASS);
            btnOK2.addStyleName(ENABLED_CLASS);
            btnOK2.removeStyleName(CURSOR_DEFAULT_CLASS);
            btnOK2.addStyleName(CURSOR_POINTER_CLASS);
        } else {
            btnOK.addStyleName(DISABLED_CLASS);
            btnOK.removeStyleName(ENABLED_CLASS);
            btnOK.removeStyleName(CURSOR_POINTER_CLASS);
            btnOK.addStyleName(CURSOR_DEFAULT_CLASS);
            btnOK2.addStyleName(DISABLED_CLASS);
            btnOK2.removeStyleName(ENABLED_CLASS);
            btnOK2.removeStyleName(CURSOR_POINTER_CLASS);
            btnOK2.addStyleName(CURSOR_DEFAULT_CLASS);
            if (errorAlert == null || !errorAlert.isVisible()) {
                errorAlert = KornellNotification.show(constants.formContainsErrors(), AlertType.WARNING, 3000);
            }
        }
    }

    private void displayContactDetails() {
        profileFields.add(getImageSeparator());

        telephone = new KornellFormFieldWrapper(constants.telephoneLabel(),
                formHelper.createTextBoxFormField(user.getPerson().getTelephone()), isEditMode);
        fields.add(telephone);
        profileFields.add(telephone);

        ListBox countries = getCountries();

        if (isEditMode && user.getPerson().getCountry() == null) {
            countries.setSelectedValue("BR");
        } else {
            countries.setSelectedValue(user.getPerson().getCountry());
        }
        countries.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                if ("BR".equals(countries.getValue())) {
                    state.initData(new ListBoxFormField(formHelper.getBrazilianStatesList()));
                } else {
                    if (!(state.getFieldWidget() instanceof TextBox))
                        state.initData(new TextBoxFormField(new TextBox()));
                }
            }
        });
        country = new KornellFormFieldWrapper(constants.countryLabel(), new ListBoxFormField(countries), isEditMode);
        fields.add(country);
        profileFields.add(country);

        if ("BR".equals(countries.getValue())) {
            // state.getFormField().clear();
            final ListBox states = formHelper.getBrazilianStatesList();
            if (user.getPerson().getState() != null) {
                states.setSelectedValue(user.getPerson().getState());
            }
            state = new KornellFormFieldWrapper(constants.stateLabel(), new ListBoxFormField(states), isEditMode);
        } else {
            state = new KornellFormFieldWrapper(constants.stateLabel(),
                    formHelper.createTextBoxFormField(user.getPerson().getState()), isEditMode);
        }
        fields.add(state);
        profileFields.add(state);

        city = new KornellFormFieldWrapper(constants.cityLabel(),
                formHelper.createTextBoxFormField(user.getPerson().getCity()), isEditMode);
        fields.add(city);
        profileFields.add(city);

        addressLine1 = new KornellFormFieldWrapper(constants.address1Label(),
                formHelper.createTextBoxFormField(user.getPerson().getAddressLine1()), isEditMode);
        fields.add(addressLine1);
        profileFields.add(addressLine1);

        addressLine2 = new KornellFormFieldWrapper(constants.address2Label(),
                formHelper.createTextBoxFormField(user.getPerson().getAddressLine2()), isEditMode);
        fields.add(addressLine2);
        profileFields.add(addressLine2);

        postalCode = new KornellFormFieldWrapper(constants.postalCodeLabel(),
                formHelper.createTextBoxFormField(user.getPerson().getPostalCode()), isEditMode);
        fields.add(postalCode);
        profileFields.add(postalCode);
    }

    private ListBox getCountries() {
        ListBox countries = new ListBox();
        countries.addItem(constants.selectboxDefault(), "-");

        Country[] countryList = Country.values();
        Country countryEnum;
        Map<String, String> countriesMap = new HashMap<>();
        for (int i = 0; i < countryList.length; i++) {
            countryEnum = countryList[i];
            countriesMap.put(countryEnum.toString(), EnumTranslator.translateEnum(countryEnum));
        }
        countriesMap = sortByValue(countriesMap);

        for (String key : countriesMap.keySet()) {
            countries.addItem(countriesMap.get(key), key);
        }
        return countries;
    }

    private <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                String o1V = AsciiUtils.convertNonAscii((String) o1.getValue()).toLowerCase();
                String o2V = AsciiUtils.convertNonAscii((String) o2.getValue()).toLowerCase();
                return (o1V).compareTo(o2V);
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override
    public void setPresenter(Presenter presenter) {
    }

    private Image getImageSeparator() {
        Image image = new Image(FormHelper.SEPARATOR_BAR_IMG_PATH);
        image.addStyleName(FormHelper.SEPARATOR_BAR_CLASS);
        return image;
    }

}
