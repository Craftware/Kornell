package kornell.gui.client.presentation.admin.institution.generic;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.EntityFactory;
import kornell.core.entity.Institution;
import kornell.core.entity.role.*;
import kornell.core.to.RoleTO;
import kornell.core.to.RolesTO;
import kornell.gui.client.event.ShowPacifierEvent;
import kornell.gui.client.util.forms.formfield.PeopleMultipleSelect;
import kornell.gui.client.util.view.KornellNotification;

public class GenericInstitutionAdminsView extends Composite {
    interface MyUiBinder extends UiBinder<Widget, GenericInstitutionAdminsView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    public static final EntityFactory entityFactory = GWT.create(EntityFactory.class);

    private KornellSession session;
    boolean isCurrentUser, showContactDetails, isRegisteredWithCPF;

    PeopleMultipleSelect institutionAdminMultipleSelect;
    PeopleMultipleSelect publisherMultipleSelect;
    PeopleMultipleSelect institutionCourseClassesAdminMultipleSelect;
    PeopleMultipleSelect institutionCourseClassesObserverMultipleSelect;

    @UiField
    Form institutionAdminForm;
    @UiField
    FlowPanel institutionAdminFields;
    @UiField
    Button institutionAdminBtnOK;
    @UiField
    Button institutionAdminBtnCancel;

    @UiField
    Form publisherForm;
    @UiField
    FlowPanel publisherFields;
    @UiField
    Button publisherBtnOK;
    @UiField
    Button publisherBtnCancel;

    @UiField
    Form institutionCourseClassesAdminForm;
    @UiField
    FlowPanel institutionCourseClassesAdminFields;
    @UiField
    Button institutionCourseClassesAdminBtnOK;
    @UiField
    Button institutionCourseClassesAdminBtnCancel;

    @UiField
    Form institutionCourseClassesObserverForm;
    @UiField
    FlowPanel institutionCourseClassesObserverFields;
    @UiField
    Button institutionCourseClassesObserverBtnOK;
    @UiField
    Button institutionCourseClassesObserverBtnCancel;

    private Institution institution;
    private EventBus bus;

    public GenericInstitutionAdminsView(final KornellSession session, EventBus bus,
            kornell.gui.client.presentation.admin.institution.AdminInstitutionView.Presenter presenter,
            Institution institution) {
        this.session = session;
        this.bus = bus;
        this.institution = institution;
        initWidget(uiBinder.createAndBindUi(this));

        // i18n
        institutionAdminBtnOK.setText("Salvar Alterações");
        institutionAdminBtnCancel.setText("Cancelar Alterações");
        publisherBtnOK.setText("Salvar Alterações");
        publisherBtnCancel.setText("Cancelar Alterações");
        institutionCourseClassesAdminBtnOK.setText("Salvar Alterações");
        institutionCourseClassesAdminBtnCancel.setText("Cancelar Alterações");
        institutionCourseClassesObserverBtnOK.setText("Salvar Alterações");
        institutionCourseClassesObserverBtnCancel.setText("Cancelar Alterações");

        initInstitutionAdminsData();
        initPublishersData();
        initInstitutionCourseClassesAdminData();
        initInstitutionCourseClassesObserverData();
    }

    public void initInstitutionAdminsData() {
        institutionAdminFields.clear();
        FlowPanel fieldPanelWrapper = new FlowPanel();
        fieldPanelWrapper.addStyleName("fieldPanelWrapper");

        FlowPanel labelPanel = new FlowPanel();
        labelPanel.addStyleName("labelPanel");
        Label lblLabel = new Label("Administradores da Instituição");
        lblLabel.addStyleName("lblLabel");
        labelPanel.add(lblLabel);
        fieldPanelWrapper.add(labelPanel);

        bus.fireEvent(new ShowPacifierEvent(true));
        session.institution(institution.getUUID()).getAdmins(RoleCategory.BIND_WITH_PERSON, new Callback<RolesTO>() {
            @Override
            public void ok(RolesTO to) {
                for (RoleTO roleTO : to.getRoleTOs()) {
                    String item = roleTO.getUsername();
                    if (roleTO.getPerson().getFullName() != null && !"".equals(roleTO.getPerson().getFullName())) {
                        item += " (" + roleTO.getPerson().getFullName() + ")";
                    }
                    institutionAdminMultipleSelect.addItem(item, roleTO.getPerson().getUUID());
                }
                bus.fireEvent(new ShowPacifierEvent(false));
            }
        });
        institutionAdminMultipleSelect = new PeopleMultipleSelect(session);
        fieldPanelWrapper.add(institutionAdminMultipleSelect.asWidget());
        institutionAdminFields.add(fieldPanelWrapper);
    }

    public void initPublishersData() {
        publisherFields.clear();
        FlowPanel fieldPanelWrapper = new FlowPanel();
        fieldPanelWrapper.addStyleName("fieldPanelWrapper");

        FlowPanel labelPanel = new FlowPanel();
        labelPanel.addStyleName("labelPanel");
        Label lblLabel = new Label("Publicadores de conteúdo");
        lblLabel.addStyleName("lblLabel");
        labelPanel.add(lblLabel);
        fieldPanelWrapper.add(labelPanel);

        bus.fireEvent(new ShowPacifierEvent(true));
        session.institution(institution.getUUID()).getPublishers(RoleCategory.BIND_WITH_PERSON, new Callback<RolesTO>() {
            @Override
            public void ok(RolesTO to) {
                for (RoleTO roleTO : to.getRoleTOs()) {
                    String item = roleTO.getUsername();
                    if (roleTO.getPerson().getFullName() != null && !"".equals(roleTO.getPerson().getFullName())) {
                        item += " (" + roleTO.getPerson().getFullName() + ")";
                    }
                    publisherMultipleSelect.addItem(item, roleTO.getPerson().getUUID());
                }
                bus.fireEvent(new ShowPacifierEvent(false));
            }
        });
        publisherMultipleSelect = new PeopleMultipleSelect(session);
        fieldPanelWrapper.add(publisherMultipleSelect.asWidget());
        publisherFields.add(fieldPanelWrapper);
    }

    public void initInstitutionCourseClassesAdminData() {
        institutionCourseClassesAdminFields.clear();
        FlowPanel fieldPanelWrapper = new FlowPanel();
        fieldPanelWrapper.addStyleName("fieldPanelWrapper");

        FlowPanel labelPanel = new FlowPanel();
        labelPanel.addStyleName("labelPanel");
        Label lblLabel = new Label("Administradores globais de grupos");
        lblLabel.addStyleName("lblLabel");
        labelPanel.add(lblLabel);
        fieldPanelWrapper.add(labelPanel);

        bus.fireEvent(new ShowPacifierEvent(true));
        session.institution(institution.getUUID()).getInstitutionCourseClassesAdmins(RoleCategory.BIND_WITH_PERSON, new Callback<RolesTO>() {
            @Override
            public void ok(RolesTO to) {
                for (RoleTO roleTO : to.getRoleTOs()) {
                    String item = roleTO.getUsername();
                    if (roleTO.getPerson().getFullName() != null && !"".equals(roleTO.getPerson().getFullName())) {
                        item += " (" + roleTO.getPerson().getFullName() + ")";
                    }
                    institutionCourseClassesAdminMultipleSelect.addItem(item, roleTO.getPerson().getUUID());
                }
                bus.fireEvent(new ShowPacifierEvent(false));
            }
        });
        institutionCourseClassesAdminMultipleSelect = new PeopleMultipleSelect(session);
        fieldPanelWrapper.add(institutionCourseClassesAdminMultipleSelect.asWidget());
        institutionCourseClassesAdminFields.add(fieldPanelWrapper);
    }

    public void initInstitutionCourseClassesObserverData() {
        institutionCourseClassesObserverFields.clear();
        FlowPanel fieldPanelWrapper = new FlowPanel();
        fieldPanelWrapper.addStyleName("fieldPanelWrapper");

        FlowPanel labelPanel = new FlowPanel();
        labelPanel.addStyleName("labelPanel");
        Label lblLabel = new Label("Administradores globais de grupos");
        lblLabel.addStyleName("lblLabel");
        labelPanel.add(lblLabel);
        fieldPanelWrapper.add(labelPanel);

        bus.fireEvent(new ShowPacifierEvent(true));
        session.institution(institution.getUUID()).getInstitutionCourseClassesObservers(RoleCategory.BIND_WITH_PERSON, new Callback<RolesTO>() {
            @Override
            public void ok(RolesTO to) {
                for (RoleTO roleTO : to.getRoleTOs()) {
                    String item = roleTO.getUsername();
                    if (roleTO.getPerson().getFullName() != null && !"".equals(roleTO.getPerson().getFullName())) {
                        item += " (" + roleTO.getPerson().getFullName() + ")";
                    }
                    institutionCourseClassesObserverMultipleSelect.addItem(item, roleTO.getPerson().getUUID());
                }
                bus.fireEvent(new ShowPacifierEvent(false));
            }
        });
        institutionCourseClassesObserverMultipleSelect = new PeopleMultipleSelect(session);
        fieldPanelWrapper.add(institutionCourseClassesObserverMultipleSelect.asWidget());
        institutionCourseClassesObserverFields.add(fieldPanelWrapper);
    }

    @UiHandler("institutionAdminBtnOK")
    void doOKInstituionAdmin(ClickEvent e) {
        if (session.isInstitutionAdmin()) {
            Roles roles = entityFactory.newRoles().as();
            List<Role> rolesList = new ArrayList<>();
            ListBox multipleSelect = institutionAdminMultipleSelect.getMultipleSelect();
            for (int i = 0; i < multipleSelect.getItemCount(); i++) {
                String personUUID = multipleSelect.getValue(i);
                Role role = entityFactory.newRole().as();
                InstitutionAdminRole institutionAdminRole = entityFactory.newInstitutionAdminRole().as();
                role.setPersonUUID(personUUID);
                role.setRoleType(RoleType.institutionAdmin);
                institutionAdminRole.setInstitutionUUID(institution.getUUID());
                role.setInstitutionAdminRole(institutionAdminRole);
                rolesList.add(role);
            }
            roles.setRoles(rolesList);
            session.institution(institution.getUUID()).updateAdmins(roles, new Callback<Roles>() {
                @Override
                public void ok(Roles to) {
                    KornellNotification.show("Os administradores da instituição foram atualizados com sucesso.");
                }
            });
        }
    }

    @UiHandler("publisherBtnOK")
    void doOKPublisher(ClickEvent e) {
        if (session.isInstitutionAdmin()) {
            Roles roles = entityFactory.newRoles().as();
            List<Role> rolesList = new ArrayList<>();
            ListBox multipleSelect = publisherMultipleSelect.getMultipleSelect();
            for (int i = 0; i < multipleSelect.getItemCount(); i++) {
                String personUUID = multipleSelect.getValue(i);
                Role role = entityFactory.newRole().as();
                PublisherRole publisherRole = entityFactory.newPublisherRole().as();
                role.setPersonUUID(personUUID);
                role.setRoleType(RoleType.publisher);
                publisherRole.setInstitutionUUID(institution.getUUID());
                role.setPublisherRole(publisherRole);
                rolesList.add(role);
            }
            roles.setRoles(rolesList);
            session.institution(institution.getUUID()).updatePublishers(roles, new Callback<Roles>() {
                @Override
                public void ok(Roles to) {
                    KornellNotification.show("Os publicadores de conteúdo foram atualizados com sucesso.");
                }
            });
        }
    }

    @UiHandler("institutionCourseClassesAdminBtnOK")
    void doOKInstitutionCourseClassesAdmin(ClickEvent e) {
        if (session.isInstitutionCourseClassesAdmin()) {
            Roles roles = entityFactory.newRoles().as();
            List<Role> rolesList = new ArrayList<>();
            ListBox multipleSelect = institutionCourseClassesAdminMultipleSelect.getMultipleSelect();
            for (int i = 0; i < multipleSelect.getItemCount(); i++) {
                String personUUID = multipleSelect.getValue(i);
                Role role = entityFactory.newRole().as();
                InstitutionCourseClassesAdminRole institutionCourseClassesAdminRole = entityFactory.newInstitutionCourseClassesAdminRole().as();
                role.setPersonUUID(personUUID);
                role.setRoleType(RoleType.institutionCourseClassesAdmin);
                institutionCourseClassesAdminRole.setInstitutionUUID(institution.getUUID());
                role.setInstitutionCourseClassesAdminRole(institutionCourseClassesAdminRole);
                rolesList.add(role);
            }
            roles.setRoles(rolesList);
            session.institution(institution.getUUID()).updateInstitutionCourseClassesAdmins(roles, new Callback<Roles>() {
                @Override
                public void ok(Roles to) {
                    KornellNotification.show("Os administradores globais de grupos foram atualizados com sucesso.");
                }
            });
        }
    }

    @UiHandler("institutionCourseClassesObserverBtnOK")
    void doOKInstitutionCourseClassesObserver(ClickEvent e) {
        if (session.isInstitutionCourseClassesAdmin()) {
            Roles roles = entityFactory.newRoles().as();
            List<Role> rolesList = new ArrayList<>();
            ListBox multipleSelect = institutionCourseClassesObserverMultipleSelect.getMultipleSelect();
            for (int i = 0; i < multipleSelect.getItemCount(); i++) {
                String personUUID = multipleSelect.getValue(i);
                Role role = entityFactory.newRole().as();
                InstitutionCourseClassesObserverRole institutionCourseClassesObserverRole = entityFactory.newInstitutionCourseClassesObserverRole().as();
                role.setPersonUUID(personUUID);
                role.setRoleType(RoleType.institutionCourseClassesObserver);
                institutionCourseClassesObserverRole.setInstitutionUUID(institution.getUUID());
                role.setInstitutionCourseClassesObserverRole(institutionCourseClassesObserverRole);
                rolesList.add(role);
            }
            roles.setRoles(rolesList);
            session.institution(institution.getUUID()).updateInstitutionCourseClassesObservers(roles, new Callback<Roles>() {
                @Override
                public void ok(Roles to) {
                    KornellNotification.show("Os observadores globais de grupos foram atualizados com sucesso.");
                }
            });
        }
    }

    @UiHandler("institutionAdminBtnCancel")
    void doCancelInstitutionAdmin(ClickEvent e) {
        initInstitutionAdminsData();
    }

    @UiHandler("publisherBtnCancel")
    void doCancelPublisher(ClickEvent e) {
        initPublishersData();
    }

    @UiHandler("institutionCourseClassesAdminBtnCancel")
    void doCancelInstitutionCourseClassesAdmin(ClickEvent e) {
        initInstitutionCourseClassesAdminData();
    }

    @UiHandler("institutionCourseClassesObserverBtnCancel")
    void doCancelInstitutionCourseClassesObserver(ClickEvent e) {
        initInstitutionCourseClassesObserverData();
    }

}