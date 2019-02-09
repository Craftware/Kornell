package kornell.gui.client.presentation.vitrine;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.Widget;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.Institution;
import kornell.core.entity.InstitutionType;
import kornell.core.entity.RegistrationType;
import kornell.core.entity.role.RoleCategory;
import kornell.core.entity.role.RoleType;
import kornell.core.error.KornellErrorTO;
import kornell.core.to.RegistrationRequestTO;
import kornell.core.to.UserHelloTO;
import kornell.core.to.UserInfoTO;
import kornell.core.util.StringUtils;
import kornell.gui.client.ClientFactory;
import kornell.gui.client.GenericClientFactoryImpl;
import kornell.gui.client.KornellConstantsHelper;
import kornell.gui.client.event.CourseClassesFetchedEvent;
import kornell.gui.client.event.LoginEvent;
import kornell.gui.client.presentation.admin.courseclass.courseclass.AdminCourseClassPlace;
import kornell.gui.client.presentation.admin.courseclass.courseclasses.AdminCourseClassesPlace;
import kornell.gui.client.presentation.profile.ProfilePlace;
import kornell.gui.client.presentation.terms.TermsPlace;
import kornell.gui.client.presentation.welcome.WelcomePlace;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.view.KornellNotification;

public class VitrinePresenter implements VitrineView.Presenter {
    Logger logger = Logger.getLogger(VitrinePresenter.class.getName());
    private final ClientFactory clientFactory;
    private VitrineView view;
    private String passwordChangeUUID;
    private KornellSession session;
    private String registrationEmail;

    public VitrinePresenter(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        this.session = clientFactory.getKornellSession();
        view = getView();
        view.setPresenter(this);

        if (clientFactory.getPlaceController().getWhere() instanceof VitrinePlace) {
            VitrinePlace place = ((VitrinePlace) clientFactory.getPlaceController().getWhere());
            this.registrationEmail = place.getEmail();
            this.passwordChangeUUID = place.getPasswordChangeUUID();
            if (StringUtils.isSome(registrationEmail)) {
                view.setRegistrationEmail(registrationEmail);
                view.displayView(VitrineViewType.register);
            } else if (StringUtils.isSome(passwordChangeUUID)) {
                view.displayView(VitrineViewType.newPassword);
            }
        }

        String assetsURL = session.getInstitutionAssetsURL();
        view.setLogoURL(assetsURL);
        view.showRegistrationOption(session.getInstitution().isAllowRegistration());
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }

    private VitrineView getView() {
        return clientFactory.getViewFactory().getVitrineView();
    }

    @Override
    public void onLoginButtonClicked() {
        view.showMessage();
        doLogin();
    }

    private void doLogin() {
        view.hideMessage();

        Callback<UserHelloTO> userHelloCallback = new Callback<UserHelloTO>() {
            @Override
            public void ok(UserHelloTO userHello) {
                postLogin(userHello);
            }

            @Override
            protected void unauthorized(KornellErrorTO kornellErrorTO) {
                logger.severe(
                        this.getClass().getName() + " - " + KornellConstantsHelper.getErrorMessage(kornellErrorTO));
                view.setMessage(KornellConstantsHelper.getMessage("badUsernamePassword"));
                view.showMessage();
            }

            @Override
            protected void forbidden(KornellErrorTO kornellErrorTO) {
                logger.info(this.getClass().getName() + " - " + KornellConstantsHelper.getErrorMessage(kornellErrorTO));
                view.setForcedPasswordUpdate(true);
                view.displayView(VitrineViewType.newPassword);
                view.setMessage(KornellConstantsHelper.getMessage("forcedPasswordChange"));
                view.showMessage();
            }
        };
        String email = view.getEmail().toLowerCase().trim().replace(FormHelper.USERNAME_ALTERNATE_SEPARATOR,
                FormHelper.USERNAME_SEPARATOR);
        String password = view.getPassword();
        session.login(email, password, userHelloCallback);
    }

    private void postLogin(UserHelloTO userHello) {
        view.displayView(null);
        clientFactory.getEventBus().fireEvent(new LoginEvent(userHello.getUserInfoTO()));
        clientFactory.getEventBus().fireEvent(new CourseClassesFetchedEvent(userHello.getCourseClassesTO()));

        Institution institution = session.getInstitution();
        UserInfoTO userInfoTO = session.getCurrentUser();
        clientFactory.setDefaultPlace(new WelcomePlace());
        Place newPlace;
        Place welcomePlace = new WelcomePlace();
        if (StringUtils.isSome(institution.getTerms()) && !session.hasSignedTerms()) {
            newPlace = new TermsPlace();
        } else if ((institution.isDemandsPersonContactDetails()
                && (!RegistrationType.username.equals(userInfoTO.getPerson().getRegistrationType())
                        || userInfoTO.getInstitutionRegistrationPrefix().isShowContactInformationOnProfile()))
                && userInfoTO.getPerson().getCity() == null) {
            newPlace = new ProfilePlace(userInfoTO.getPerson().getUUID(), true);
        } else if (RoleCategory.hasRole(session.getCurrentUser().getRoles(), RoleType.courseClassAdmin)
                || session.isInstitutionCourseClassesAdmin()
                || session.isInstitutionAdmin()) {
            newPlace = new AdminCourseClassesPlace();
        } else {
            newPlace = welcomePlace;
        }
        clientFactory.setDefaultPlace(newPlace instanceof AdminCourseClassPlace ? newPlace : welcomePlace);
        clientFactory.setHomePlace(welcomePlace, userHello.getCourseClassesTO());
        clientFactory.getPlaceController().goTo(InstitutionType.DASHBOARD.equals(institution.getInstitutionType())
                && !(newPlace instanceof AdminCourseClassPlace) ? clientFactory.getHomePlace() : newPlace);
    }

    @Override
    public void onRegisterButtonClicked() {
        if (session.getInstitution().isAllowRegistration()) {
            view.hideMessage();
            view.displayView(VitrineViewType.register);
        }
    }

    @Override
    public void onForgotPasswordButtonClicked() {
        view.hideMessage();
        view.displayView(VitrineViewType.forgotPassword);
    }

    private List<String> validateFields() {
        FormHelper validator = new FormHelper();
        List<String> errors = new ArrayList<String>();
        if (!validator.isLengthValid(view.getSuName(), 2, 50)) {
            errors.add(KornellConstantsHelper.getMessage("nameTooShort"));
        }
        if (!FormHelper.isEmailValid(view.getSuEmail())) {
            errors.add(KornellConstantsHelper.getMessage("invalidEmail"));
        }
        if (!validator.isPasswordValid(view.getSuPassword())) {
            errors.add(KornellConstantsHelper.getMessage("invalidPasswordTooShort"));
        }
        if (view.getSuPassword().indexOf(':') >= 0) {
            errors.add(KornellConstantsHelper.getMessage("invalidPasswordBadChar"));
        }
        if (!view.getSuPassword().equals(view.getSuPasswordConfirm())) {
            errors.add(KornellConstantsHelper.getMessage("passwordMismatch"));
        }
        return errors;
    }

    @Override
    public void onSignUpButtonClicked() {
        if (StringUtils.isSome(registrationEmail) || session.getInstitution().isAllowRegistration()) {
            view.hideMessage();
            List<String> errors = validateFields();
            if (errors.size() == 0) {
                signUp();
            } else {
                view.setMessage(errors);
                view.showMessage();
            }
        }
    }

    private void signUp() {
        final String suName = view.getSuName().trim();
        final String suEmail = view.getSuEmail().toLowerCase().trim();
        final String suPassword = view.getSuPassword();
        final Callback<UserInfoTO> registrationCallback = new Callback<UserInfoTO>() {
            @Override
            public void ok(UserInfoTO user) {
                logger.info("User created");
                KornellNotification.show(KornellConstantsHelper.getMessage("userCreated"), 2000);
                view.displayView(VitrineViewType.login);
                view.setEmail(view.getSuEmail());
                view.setPassword(suPassword);
                doLogin();
            }
        };
        // TODO improve
        Callback<UserInfoTO> checkUserCallback = new Callback<UserInfoTO>() {
            @Override
            public void ok(UserInfoTO user) {
                if (user.getUsername() != null) {
                    view.setMessage(KornellConstantsHelper.getMessage("emailExists"));
                    view.showMessage();
                    return;
                }
                RegistrationRequestTO registrationRequestTO = buildRegistrationRequestTO(suName, suEmail, suPassword);
                session.user().requestRegistration(registrationRequestTO, registrationCallback);
            }
        };
        session.user().checkUser(session.getInstitution().getUUID(), suEmail, checkUserCallback);
    }

    private RegistrationRequestTO buildRegistrationRequestTO(String name, String email, String password) {
        RegistrationRequestTO registrationRequestTO = GenericClientFactoryImpl.TO_FACTORY.newRegistrationRequestTO()
                .as();
        registrationRequestTO.setFullName(name);
        registrationRequestTO.setEmail(email);
        registrationRequestTO.setPassword(password);
        registrationRequestTO.setRegistrationType(RegistrationType.email);
        registrationRequestTO.setInstitutionUUID(session.getInstitution().getUUID());
        return registrationRequestTO;
    }

    @Override
    public void onCancelSignUpButtonClicked() {
        view.hideMessage();
        view.displayView(VitrineViewType.login);
    }

    @Override
    public void onRequestPasswordChangeButtonClicked() {

        view.hideMessage();
        if (StringUtils.isNone(view.getFpEmail().trim())) {
            view.setMessage(KornellConstantsHelper.getMessage("invalidEmail"));
            view.showMessage();
            return;
        }

        session.user().requestPasswordChange(view.getFpEmail().toLowerCase().trim(), session.getInstitution().getName(),
                new Callback<Void>() {
                    @Override
                    public void ok(Void to) {
                        view.displayView(VitrineViewType.login);
                        KornellNotification.show(KornellConstantsHelper.getMessage("requestPasswordReset"));
                        view.hideMessage();
                    }

                    @Override
                    public void notFound(KornellErrorTO kornellErrorTO) {
                        logger.severe(this.getClass().getName() + " - not found");
                        KornellNotification.show(KornellConstantsHelper.getMessage("requestPasswordResetError"),
                                AlertType.ERROR);
                    }
                });
    }

    @Override
    public void onCancelPasswordChangeRequestButtonClicked() {
        view.hideMessage();
        view.displayView(VitrineViewType.login);
    }

    @Override
    public void onChangePasswordButtonClicked() {
        view.hideMessage();
        FormHelper validator = new FormHelper();
        List<String> errors = new ArrayList<String>();

        if (!validator.isPasswordValid(view.getNewPassword())) {
            errors.add(KornellConstantsHelper.getMessage("invalidPasswordTooShort"));
        }
        if (view.getNewPassword().indexOf(':') >= 0) {
            errors.add(KornellConstantsHelper.getMessage("invalidPasswordBadChar"));
        }
        if (!view.getNewPassword().equals(view.getNewPasswordConfirm())) {
            errors.add(KornellConstantsHelper.getMessage("passwordMismatch"));
        }
        if (errors.size() == 0) {
            Callback<UserInfoTO> passwordChangeCallback = new Callback<UserInfoTO>() {
                @Override
                public void ok(UserInfoTO to) {
                    view.setForcedPasswordUpdate(false);
                    view.displayView(VitrineViewType.login);
                    KornellNotification.show(KornellConstantsHelper.getMessage("passwordChangeComplete"));
                    view.setEmail(to.getUsername());
                    view.setPassword(view.getNewPassword());
                    doLogin();
                }

                @Override
                public void unauthorized(KornellErrorTO kornellErrorTO) {
                    logger.severe(
                            this.getClass().getName() + " - " + KornellConstantsHelper.getErrorMessage(kornellErrorTO));
                    KornellNotification.show(KornellConstantsHelper.getMessage("passwordChangeError"), AlertType.ERROR,
                            8000);
                }
            };
            if (this.view.isForcedPasswordUpdate()) {
                session.user().forcedPasswordChange(view.getEmail(), view.getNewPassword(), passwordChangeCallback);
            } else {
                session.user().changePassword(view.getNewPassword(), passwordChangeUUID, passwordChangeCallback);
            }
        } else {
            view.setMessage(errors);
            view.showMessage();
        }
    }

    @Override
    public void onCancelChangePasswordButtonClicked() {
        view.hideMessage();
        view.displayView(VitrineViewType.login);
    }

}