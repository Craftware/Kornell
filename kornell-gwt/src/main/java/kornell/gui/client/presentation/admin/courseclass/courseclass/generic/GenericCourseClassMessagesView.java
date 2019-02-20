package kornell.gui.client.presentation.admin.courseclass.courseclass.generic;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.KornellSession;
import kornell.core.to.CourseClassTO;
import kornell.gui.client.ViewFactory;
import kornell.gui.client.presentation.admin.courseclass.courseclass.AdminCourseClassPlace;
import kornell.gui.client.presentation.message.MessagePresenter;

public class GenericCourseClassMessagesView extends Composite {
    interface MyUiBinder extends UiBinder<Widget, GenericCourseClassMessagesView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    PlaceController placeCtrl;
    ViewFactory viewFactory;
    boolean isCurrentUser, showContactDetails, isRegisteredWithCPF;

    private MessagePresenter presenter;

    @UiField
    FlowPanel messagesPanel;

    public GenericCourseClassMessagesView(final KornellSession session, EventBus bus, PlaceController placeCtrl,
            ViewFactory viewFactory, MessagePresenter presenter, CourseClassTO courseClassTO) {
        this.placeCtrl = placeCtrl;
        this.viewFactory = viewFactory;
        this.presenter = presenter;
        initWidget(uiBinder.createAndBindUi(this));
        initData();
    }

    public void initData() {
        messagesPanel.clear();
        messagesPanel.setVisible(false);
        messagesPanel.add(getPanel());
        messagesPanel.setVisible(true);

    }

    private FlowPanel getPanel() {
        FlowPanel panel = new FlowPanel();
        panel.addStyleName("reportPanel");
        if (placeCtrl.getWhere() instanceof AdminCourseClassPlace)
            panel.add(getMessagesInfo());
        panel.add(getMessagesContent());
        return panel;
    }

    private FlowPanel getMessagesInfo() {
        FlowPanel messagesInfo = new FlowPanel();
        messagesInfo.addStyleName("titlePanel");

        Label infoTitle = new Label("Mensagens");
        infoTitle.addStyleName("title");
        messagesInfo.add(infoTitle);

        Label infoText = new Label("Mensagens entre os alunos e os responsáveis pelo grupo.");
        infoText.addStyleName("subTitle");
        messagesInfo.add(infoText);

        return messagesInfo;
    }

    private FlowPanel getMessagesContent() {
        FlowPanel reportContentPanel = new FlowPanel();
        reportContentPanel.add(presenter.asWidget());
        return reportContentPanel;
    }

}