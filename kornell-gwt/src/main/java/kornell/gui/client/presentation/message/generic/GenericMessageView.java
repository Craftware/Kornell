package kornell.gui.client.presentation.message.generic;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.KornellSession;
import kornell.core.entity.role.RoleType;
import kornell.core.to.ChatThreadMessageTO;
import kornell.core.to.ChatThreadMessagesTO;
import kornell.core.to.UnreadChatThreadTO;
import kornell.gui.client.KornellConstants;
import kornell.gui.client.event.ShowChatDockEvent;
import kornell.gui.client.event.ShowChatDockEventHandler;
import kornell.gui.client.event.UnreadMessagesCountChangedEvent;
import kornell.gui.client.presentation.message.MessagePanelType;
import kornell.gui.client.presentation.message.MessageView;
import kornell.gui.client.util.EnumTranslator;
import kornell.gui.client.util.forms.FormHelper;
import kornell.gui.client.util.view.Positioning;

public class GenericMessageView extends Composite implements MessageView, ShowChatDockEventHandler {

    interface GenericMessageUiBinder extends UiBinder<Widget, GenericMessageView> {
    }

    private static GenericMessageUiBinder uiBinder = GWT.create(GenericMessageUiBinder.class);
    private static KornellConstants constants = GWT.create(KornellConstants.class);
    private static FormHelper formHelper = GWT.create(FormHelper.class);
    private MessageView.Presenter presenter;
    private EventBus bus;
    private KornellSession session;

    private List<Label> sideItems;
    private Map<String, MessageItem> dateLabelsMap;
    private HashMap<String, Label> sidePanelItemsMap;
    private TextBox txtSearch;
    private Timer updateTimer;
    private Label currentLabel;
    private com.github.gwtbootstrap.client.ui.Button btnClear;

    @UiField
    FlowPanel sidePanel;
    @UiField
    FlowPanel threadPanel;
    @UiField
    ScrollPanel sidePanelScroll;
    @UiField
    ScrollPanel threadPanelItemsScroll;
    @UiField
    FlowPanel threadPanelItems;
    @UiField
    Label threadTitle;
    @UiField
    TextArea messageTextArea;
    @UiField
    Button btnSend;

    private static String HIGHLIGHT_CLASS = "highlightText";
    private static String HIGHLIGHT_DISCRETE_CLASS = "highlightTextDiscreteColor";
    private String PLAIN_CLASS = "plainDiscreteTextColor";
    private FlowPanel searchPanel;
    @SuppressWarnings("unused")
    private MessagePanelType messagePanelType;
    private List<UnreadChatThreadTO> unreadChatThreadsTO;
    private String currentUserFullName;

    public GenericMessageView(EventBus eventBus, KornellSession session) {
        this.bus = eventBus;
        this.session = session;
        this.bus.addHandler(ShowChatDockEvent.TYPE, this);
        initWidget(uiBinder.createAndBindUi(this));
        ensureDebugId("genericMessageInboxView");

        dateLabelsMap = new HashMap<String, MessageItem>();

        updateTimer = new Timer() {
            @Override
            public void run() {
                filter();
            }
        };

        threadPanelItemsScroll.addScrollHandler(new ScrollHandler() {
            @Override
            public void onScroll(ScrollEvent event) {
                if (threadPanelItemsScroll.getVerticalScrollPosition() == 0) {
                    presenter.onScrollToTop(false);
                }
            }
        });
    }

    @Override
    public void displayThreadPanel(boolean display) {
        threadPanel.setVisible(display);
    }

    @Override
    protected void onEnsureDebugId(String baseID) {
        sidePanel.ensureDebugId(baseID + "-sidePanel");
        threadPanel.ensureDebugId(baseID + "-threadPanel");
    }

    @Override
    public void setPresenter(Presenter p) {
        presenter = p;
    }

    private void initSearch() {
        if (searchPanel == null) {
            txtSearch = new TextBox();
            txtSearch.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    scheduleFilter();
                }
            });
            txtSearch.addKeyUpHandler(new KeyUpHandler() {
                @Override
                public void onKeyUp(KeyUpEvent event) {
                    scheduleFilter();
                }
            });
            txtSearch.addValueChangeHandler(new ValueChangeHandler<String>() {
                @Override
                public void onValueChange(ValueChangeEvent<String> event) {
                    scheduleFilter();
                }
            });
            searchPanel = new FlowPanel();

            searchPanel.add(txtSearch);
            searchPanel.addStyleName("filterPanel");
            searchPanel.add(new Icon(IconType.SEARCH));
            txtSearch.setPlaceholder(constants.filterConversationPlaceholder());
            btnClear = new com.github.gwtbootstrap.client.ui.Button("");
            btnClear.removeStyleName("btn");
            btnClear.setVisible(false);
            btnClear.setIcon(IconType.REMOVE);
            btnClear.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    txtSearch.setText("");
                    filter();
                }
            });
            searchPanel.add(btnClear);
        }
    }

    private void scheduleFilter() {
        updateTimer.cancel();
        updateTimer.schedule(300);
    }

    private void filter() {
        String searchTerm = txtSearch.getText().trim().toLowerCase();
        btnClear.setVisible(searchTerm.length() > 0);
        for (Map.Entry<String, Label> entry : sidePanelItemsMap.entrySet()) {
            if (entry.getKey().indexOf(searchTerm) >= 0) {
                entry.getValue().removeStyleName("shy");
            } else {
                entry.getValue().addStyleName("shy");
            }
        }
    }

    @Override
    public void updateSidePanel(List<UnreadChatThreadTO> unreadChatThreadsTO, final String currentUserFullName) {
        if (unreadChatThreadsTO == null)
            return;
        this.unreadChatThreadsTO = unreadChatThreadsTO;
        this.currentUserFullName = currentUserFullName;

        sidePanel.clear();
        initSearch();
        sidePanel.add(searchPanel);
        sideItems = new ArrayList<Label>();
        sidePanelItemsMap = new HashMap<String, Label>();
        for (final UnreadChatThreadTO unreadChatThreadTO : unreadChatThreadsTO) {
            final Label label = new Label();
            label.addStyleName("threadListItem");
            label.addClickHandler(new ClickHandler() {
                boolean enableClick = true;

                @Override
                public void onClick(ClickEvent event) {
                    if (!enableClick)
                        return;
                    enableClick = false;
                    Timer preventDoubleClickTimer = new Timer() {
                        public void run() {
                            enableClick = true;
                        }
                    };
                    preventDoubleClickTimer.schedule(300);

                    for (Label lbl : sideItems) {
                        lbl.removeStyleName("selected");
                    }
                    label.addStyleName("selected");
                    presenter.threadClicked(unreadChatThreadTO);
                    determineMobileChatBehaviour(false);
                    setLabelContent(unreadChatThreadTO, label, true, currentUserFullName);

                    currentLabel = label;
                }
            });
            String selectedChatThreadUUID = presenter.getThreadSelection() == null ? null
                    : presenter.getThreadSelection().getChatThreadUUID();
            if (unreadChatThreadTO.getChatThreadUUID() != null && unreadChatThreadTO.getChatThreadUUID().equals(selectedChatThreadUUID)) {
                label.addStyleName("selected");
                setLabelContent(unreadChatThreadTO, label, true, currentUserFullName);
            } else {
                setLabelContent(unreadChatThreadTO, label, false, currentUserFullName);
            }
            sidePanel.add(label);
            sideItems.add(label);
        }
        filter();
    }

    @Override
    public void sendSidePanelItemToTop(String chatThreadUUID) {
        if (currentLabel != null && sidePanel != null) {
            if (sidePanel.remove(currentLabel)) {
                sidePanel.insert(currentLabel, 1);
            }
        }
    }

    private String getThreadTitle(final UnreadChatThreadTO unreadChatThreadTO, String currentUserFullName,
            boolean lineBreak) {
        switch (unreadChatThreadTO.getThreadType()) {
        case COURSE_CLASS:
            return span(constants.courseClassChatThreadLabel(), PLAIN_CLASS) + separator(lineBreak)
                    + span(unreadChatThreadTO.getEntityName(), HIGHLIGHT_CLASS);
        case DIRECT:
            return span(constants.directChatLabel(), PLAIN_CLASS) + separator(lineBreak)
                    + span(unreadChatThreadTO.getEntityName(), HIGHLIGHT_CLASS);
        case SUPPORT:
            if (unreadChatThreadTO.getChatThreadCreatorName().equals(currentUserFullName)
                    && !session.isCourseClassAdmin(unreadChatThreadTO.getEntityUUID())) {
                return span(constants.supportChatThreadLabel(), PLAIN_CLASS) + separator(lineBreak)
                        + span(unreadChatThreadTO.getEntityName(), HIGHLIGHT_CLASS);
            } else {
                return span(unreadChatThreadTO.getChatThreadCreatorName(), HIGHLIGHT_DISCRETE_CLASS)
                        + (MessagePanelType.courseClassSupport.equals(presenter.getMessagePanelType())
                                ? separator(lineBreak, true) + span(constants.supportLabel(), PLAIN_CLASS)
                                : separator(lineBreak, true) + span(constants.supportChatThreadLabel(), PLAIN_CLASS)
                                        + separator(lineBreak)
                                        + span(unreadChatThreadTO.getEntityName(), HIGHLIGHT_CLASS));
            }
        case TUTORING:
            if (unreadChatThreadTO.getChatThreadCreatorName().equals(currentUserFullName)
                    && !session.isCourseClassTutor(unreadChatThreadTO.getEntityUUID())) {
                return span(constants.tutorChatThreadLabel(), PLAIN_CLASS) + separator(lineBreak)
                        + span(unreadChatThreadTO.getEntityName(), HIGHLIGHT_CLASS);
            } else {
                return span(unreadChatThreadTO.getChatThreadCreatorName(), HIGHLIGHT_DISCRETE_CLASS)
                        + (MessagePanelType.courseClassSupport.equals(presenter.getMessagePanelType())
                                || MessagePanelType.courseClassTutor.equals(presenter.getMessagePanelType())
                                        ? separator(lineBreak, true) + span(constants.tutorLabel(), PLAIN_CLASS)
                                        : separator(lineBreak, true)
                                                + span(constants.tutorChatThreadLabel(), PLAIN_CLASS)
                                                + separator(lineBreak)
                                                + span(unreadChatThreadTO.getEntityName(), HIGHLIGHT_CLASS));
            }
        case INSTITUTION_SUPPORT:
            return span(constants.institutionSupportChatThreadLabel(), PLAIN_CLASS) + separator(lineBreak)
                    + span(session.getInstitution().getName(), HIGHLIGHT_CLASS);
        case PLATFORM_SUPPORT:
            return span(constants.platformSupportChatThreadLabel(), PLAIN_CLASS) + separator(lineBreak)
                    + span(session.getInstitution().getName(), HIGHLIGHT_CLASS);
        default:
            return "";
        }
    }

    private String separator(boolean lineBreak) {
        return separator(lineBreak, false);
    }

    private String separator(boolean lineBreak, boolean dash) {
        return lineBreak ? "<br>" : (dash ? "&nbsp;&nbsp;-&nbsp;&nbsp;" : "&nbsp;&nbsp;&nbsp;");
    }

    private String span(String str, String className) {
        return "<span class=\"" + className + "\">" + str + "</span>";
    }

    private void setLabelContent(final UnreadChatThreadTO unreadChatThreadTO, final Label label, boolean markAsRead,
            String currentUserFullName) {
        String appendCount = !"0".equals(unreadChatThreadTO.getUnreadMessages()) && !markAsRead
                ? " (" + unreadChatThreadTO.getUnreadMessages() + ")&nbsp;&nbsp;" : "";
        appendCount = "<span class=\"unreadCount\">" + appendCount + "</span>";
        String title = getThreadTitle(unreadChatThreadTO, currentUserFullName, true);
        String titleStripped = title.replaceAll(separator(true), " ").replaceAll("\\<[^>]*>", "")
                .replaceAll(separator(false, true), " ").replaceAll(separator(false, false), " ").toLowerCase();
        sidePanelItemsMap.put(titleStripped, label);
        label.getElement().setInnerHTML(appendCount + title);
        // if it's supposed to be marked as read and there were messages on the
        // thread, update the envelope count
        if (markAsRead && !"0".equals(unreadChatThreadTO.getUnreadMessages())) {
            bus.fireEvent(
                    new UnreadMessagesCountChangedEvent(Integer.parseInt(unreadChatThreadTO.getUnreadMessages())));
            unreadChatThreadTO.setUnreadMessages("0");
        }
    }

    @Override
    public void updateThreadPanel(UnreadChatThreadTO unreadChatThreadTO, String currentUserFullName) {
        threadTitle.getElement().setInnerHTML(getThreadTitle(unreadChatThreadTO, currentUserFullName, false));
        dateLabelsMap = new HashMap<String, MessageItem>();
        threadPanelItems.clear();
        prepareTextArea(false);
        messageTextArea.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && event.isAnyModifierKeyDown()
                        && event.isControlKeyDown())
                    doSend(null);
            }
        });

    }

    @Override
    public void scrollToBottom() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                threadPanelItemsScroll.scrollToBottom();
            }
        });
    }

    @Override
    public void setPlaceholder(String placeholder) {
        messageTextArea.setPlaceholder(placeholder);
    }

    @Override
    public void addMessagesToThreadPanel(ChatThreadMessagesTO chatThreadMessagesTO, String currentUserFullName,
            final boolean insertOnTop) {
        final int oldPosition = threadPanelItemsScroll.getMaximumVerticalScrollPosition();
        boolean shouldScrollToBottom = (threadPanelItemsScroll
                .getMaximumVerticalScrollPosition() == threadPanelItemsScroll.getVerticalScrollPosition())
                && !insertOnTop;

        for (final ChatThreadMessageTO chatThreadMessageTO : chatThreadMessagesTO.getChatThreadMessageTOs()) {
            FlowPanel threadMessageWrapper = new FlowPanel();
            threadMessageWrapper.addStyleName("threadMessageWrapper");
            Label header = new Label("");

            header.addStyleName("threadMessageHeader");
            if (currentUserFullName.equals(chatThreadMessageTO.getSenderFullName())) {
                header.addStyleName("rightText");
                threadMessageWrapper.addStyleName("overrideWrapper");
            }
            threadMessageWrapper.add(header);

            Label item = new Label(chatThreadMessageTO.getMessage());
            item.addStyleName("threadMessageItem");
            threadMessageWrapper.add(item);

            if (!dateLabelsMap.containsKey(chatThreadMessageTO.getSentAt().getTime() + "")) {
                dateLabelsMap.put(chatThreadMessageTO.getSentAt().getTime() + "",
                        new MessageItem(header, chatThreadMessageTO));
                if (insertOnTop) {
                    threadPanelItems.insert(threadMessageWrapper, 0);
                } else {
                    threadPanelItems.add(threadMessageWrapper);
                }
            }
        }

        updateDateLabelValues(chatThreadMessagesTO.getServerTime());

        if (insertOnTop) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    threadPanelItemsScroll.setVerticalScrollPosition(
                            threadPanelItemsScroll.getMaximumVerticalScrollPosition() - oldPosition);
                }
            });
        }
        if (shouldScrollToBottom) {
            scrollToBottom();
        }
    }

    private void updateDateLabelValues(Date serverTime) {
        Iterator<Entry<String, MessageItem>> it = dateLabelsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, MessageItem> pairs = (Map.Entry<String, MessageItem>) it.next();
            pairs.getValue().getLabel().getElement()
                    .setInnerHTML(getDateLabelValue(serverTime, pairs.getValue().getChatThreadMessageTO()));
        }
    }

    private String getDateLabelValue(Date serverTime, final ChatThreadMessageTO chatThreadMessageTO) {
        if (chatThreadMessageTO.getSentAt() == null)
            return "";
        String dateStr = span(chatThreadMessageTO.getSenderFullName(), HIGHLIGHT_CLASS)
                + getIcons(chatThreadMessageTO.getSenderRole()) + separator(false, false)
                + span(formHelper.getElapsedTimeSince(chatThreadMessageTO.getSentAt(), serverTime), PLAIN_CLASS);
        return dateStr;
    }

    private String getIcons(RoleType type) {
        switch (type) {
        case platformAdmin:
            return getIcons(3, "fa fa-star", EnumTranslator.translateEnum(type));
        case institutionAdmin:
            return getIcons(2, "fa fa-star", EnumTranslator.translateEnum(type));
        case courseClassAdmin:
        case institutionCourseClassesAdmin:
            return getIcons(1, "fa fa-star", EnumTranslator.translateEnum(RoleType.courseClassAdmin));
        case tutor:
            return getIcons(1, "fa fa-graduation-cap", EnumTranslator.translateEnum(type));
        default:
            return "";
        }
    }

    private String getIcons(int count, String classes, String title) {
        String icons = "&nbsp;<nobr>";
        for (int i = 0; i < count; i++) {
            icons += "<i class=\"" + classes + " plainDiscreteTextColor\" title=\"" + title + "\"></i>";
        }
        return icons + "</nobr>";
    }

    @UiHandler("btnSend")
    void doSend(ClickEvent e) {
        if (messageTextArea.getText().trim().length() > 0) {
            presenter.sendMessage(messageTextArea.getText());
            prepareTextArea(true);
        }
    }

    private void prepareTextArea(boolean setFocus) {
        messageTextArea.setText("");
        if (setFocus) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    messageTextArea.setFocus(true);
                }
            });
        }
    }

    @Override
    public void setMessagePanelType(MessagePanelType messagePanelType) {
        this.messagePanelType = messagePanelType;
    }

    @Override
    public void onShowChatDock(ShowChatDockEvent event) {
        if (event.isShowChatDock()) {
            scrollToBottom();
        }
    }

    @Override
    public void determineMobileChatBehaviour(boolean forceSidePanelAppearance) {
        if (Window.getClientWidth() <= Positioning.MOBILE_CHAT_THRESHOLD) {
            if (presenter.getThreadSelection() != null) {
                sidePanelScroll.setVisible(false);
                threadPanel.setVisible(true);
            } else {
                sidePanelScroll.setVisible(true);
                threadPanel.setVisible(false);
            }
        } else {
            threadPanel.setVisible(true);
            sidePanelScroll.setVisible(true);
        }

        if (forceSidePanelAppearance) {
            for (Label lbl : sideItems) {
                lbl.removeStyleName("selected");
            }
        }

        refreshSidePanel();
    }

    @Override
    public void refreshSidePanel() {
        updateSidePanel(unreadChatThreadsTO, currentUserFullName);
    }
}