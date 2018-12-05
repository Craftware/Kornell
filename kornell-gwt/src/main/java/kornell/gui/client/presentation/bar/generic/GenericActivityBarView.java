package kornell.gui.client.presentation.bar.generic;

import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import kornell.api.client.KornellSession;
import kornell.core.entity.EnrollmentState;
import kornell.core.entity.EntityState;
import kornell.core.to.CourseClassTO;
import kornell.core.to.UserInfoTO;
import kornell.gui.client.ClientFactory;
import kornell.gui.client.KornellConstants;
import kornell.gui.client.event.HideSouthBarEvent;
import kornell.gui.client.event.NavigationAuthorizationEvent;
import kornell.gui.client.event.NavigationAuthorizationEventHandler;
import kornell.gui.client.event.ProgressEvent;
import kornell.gui.client.event.ProgressEventHandler;
import kornell.gui.client.event.ShowChatDockEvent;
import kornell.gui.client.event.ShowChatDockEventHandler;
import kornell.gui.client.event.ShowDetailsEvent;
import kornell.gui.client.event.ShowDetailsEventHandler;
import kornell.gui.client.mvp.HistoryMapper;
import kornell.gui.client.presentation.bar.ActivityBarView;
import kornell.gui.client.presentation.classroom.generic.notes.NotesPopup;
import kornell.gui.client.sequence.NavigationRequest;

public class GenericActivityBarView extends Composite implements ActivityBarView, ProgressEventHandler, ShowDetailsEventHandler, ShowChatDockEventHandler, NavigationAuthorizationEventHandler {

    interface MyUiBinder extends UiBinder<Widget, GenericActivityBarView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    private String page;

    private NotesPopup notesPopup;
    private FlowPanel progressBarPanel;
    private final HistoryMapper historyMapper = GWT.create(HistoryMapper.class);

    private static KornellConstants constants = GWT.create(KornellConstants.class);

    private static String BUTTON_PREVIOUS = constants.previous();
    private static String BUTTON_NEXT = constants.next();
    private static String BUTTON_DETAILS = constants.details();
    private static String BUTTON_NOTES = constants.notes();
    private static String BUTTON_CHAT = constants.chat();

    private boolean showDetails = true;
    private boolean chatDockEnabled = false;
    private boolean enableNext = false;
    private boolean enablePrev = false;
    private boolean allowedNext = false;
    private boolean allowedPrev = false;

    @UiField
    FocusPanel btnPrevious;
    @UiField
    FocusPanel btnNext;
    @UiField
    FocusPanel btnDetails;
    @UiField
    FocusPanel btnNotes;
    @UiField
    FocusPanel btnChat;
    @UiField
    FocusPanel btnProgress;

    @UiField
    FlowPanel activityBar;

    private UserInfoTO user;
    private ClientFactory clientFactory;

    private Integer currentPage = 0, totalPages = 0, progressPercent = 0;

    private boolean shouldShowActivityBar, isEnrolled;

    private KornellSession session;

    public GenericActivityBarView(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        initWidget(uiBinder.createAndBindUi(this));
        clientFactory.getEventBus().addHandler(ProgressEvent.TYPE, this);
        clientFactory.getEventBus().addHandler(ShowDetailsEvent.TYPE, this);
        clientFactory.getEventBus().addHandler(ShowChatDockEvent.TYPE, this);
        clientFactory.getEventBus().addHandler(NavigationAuthorizationEvent.TYPE, this);

        session = clientFactory.getKornellSession();
        if (session.isAuthenticated()) {
            user = clientFactory.getKornellSession().getCurrentUser();
        }
        display();

        setupArrowsNavigation();

    }

    private void display() {

        this.setVisible(false);
        CourseClassTO courseClassTO = session.getCurrentCourseClass();

        isEnrolled = courseClassTO != null && courseClassTO.getEnrollment() != null
                && EnrollmentState.enrolled.equals(courseClassTO.getEnrollment().getState());
        shouldShowActivityBar = isEnrolled && session.getCurrentCourseClass() != null
                && !EntityState.inactive.equals(session.getCurrentCourseClass().getCourseClass().getState());

        showDetails = !isEnrolled;

        displayButton(btnPrevious, BUTTON_PREVIOUS, new Icon(IconType.CARET_LEFT));

        displayButton(btnNext, BUTTON_NEXT, new Icon(IconType.CARET_RIGHT), true);

        displayButton(btnDetails, showDetails ? constants.course() : BUTTON_DETAILS, new Icon(IconType.BOOK));
        setupBtnDetailsEvent(showDetails);

        Icon iconChat = new Icon();
        iconChat.setStyleName("fa fa-comments");
        displayButton(btnChat, BUTTON_CHAT, iconChat);
        btnChat.addStyleName("activityBarButtonChat");

        Icon iconNotes = new Icon();
        iconNotes.setStyleName("fa fa-sticky-note-o");
        displayButton(btnNotes, BUTTON_NOTES, iconNotes);

        if (session.getCurrentCourseClass().getCourseClass().isCourseClassChatEnabled()
                && !session.getCurrentCourseClass().getCourseClass().isChatDockEnabled()) {
            btnChat.addStyleName("activityBarButtonSmall");
            btnNotes.addStyleName("activityBarButtonSmall");
            btnChat.removeStyleName("shy");
        } else {
            btnChat.addStyleName("shy");
        }

        displayProgressButton();

        if (showDetails) {
            btnDetails.addStyleName("btnAction");
            enableButton(BUTTON_PREVIOUS, false);
            enableButton(BUTTON_NEXT, false);
        }
    }

    private void setupBtnDetailsEvent(boolean showDetails) {
        btnDetails.getElement().setId("btnClassroomDetails");

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                setupBtnDetailsEventNative();
            }
        });
    }

    private static native void setupBtnDetailsEventNative() /*-{
        var btnClassroomDetails = $wnd.document.getElementById("btnClassroomDetails");
        btnClassroomDetails && btnClassroomDetails.addEventListener("click", courseClassDetailsShown, false);

        function courseClassDetailsShown(e) {
            e.preventDefault();
            var courseClassDetailsShown = $wnd.document.getElementsByClassName("courseClassDetailsShown").length > 0;
            if (window.CustomEvent) {
                try{
                    var event = new CustomEvent("courseClassDetailsShown", {
                        detail: {
                            courseClassDetailsShown: courseClassDetailsShown,
                            time: new Date(),
                        },
                        bubbles: true,
                        cancelable: true
                    });
                    e.currentTarget.dispatchEvent(event);
                } catch (err) {

                }
            }
        }
    }-*/;

    private void displayProgressButton() {
        progressBarPanel = new FlowPanel();
        progressBarPanel.addStyleName("progressBarPanel");

        btnProgress.add(progressBarPanel);
        btnProgress.removeStyleName("btn");
        btnProgress.removeStyleName("btn-link");
    }

    private void updateProgressBarPanel(Integer currentPage, Integer totalPages, Integer progressPercent) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.progressPercent = progressPercent;
        updateProgressBarPanel();
    }

    private void updateProgressBarPanel() {
        progressBarPanel.clear();

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPercent(progressPercent);

        FlowPanel pagePanel = new FlowPanel();
        pagePanel.addStyleName("pagePanel");
        pagePanel.addStyleName("activityBarButtonItem");

        if (showDetails) {
            pagePanel.add(createSpan(progressPercent + "%", true, false));
            pagePanel.add(createSpan(constants.completed(), false, true));
        } else {
            pagePanel.add(createSpan(constants.pageForPagination(), false, true));
            pagePanel.add(createSpan(currentPage == 0 ? "-" : "" + currentPage, true, false));
            pagePanel.add(createSpan("/", false, false));
            pagePanel.add(createSpan("" + totalPages, false, false));
        }

        progressBarPanel.add(pagePanel);
        progressBarPanel.add(progressBar);
    }

    private HTMLPanel createSpan(String text, boolean isHighlighted, boolean hideWhenSmall) {
        HTMLPanel pageCaption = new HTMLPanel(text);
        pageCaption.addStyleName("marginLeft7");
        if (isHighlighted) {
            pageCaption.addStyleName("highlightText");
        }
        if (hideWhenSmall) {
            pageCaption.addStyleName("hideSmall");
        }
        return pageCaption;
    }

    private void displayButton(final FocusPanel btn, final String buttonType, Icon icon) {
        displayButton(btn, buttonType, icon, false);
    }

    private void displayButton(final FocusPanel btn, final String buttonType, Icon icon, boolean invertIcon) {
        btn.clear();
        FlowPanel buttonPanel = new FlowPanel();
        buttonPanel.addStyleName("btnPanel");
        buttonPanel.addStyleName(getItemName(buttonType));

        icon.addStyleName("activityBarButtonItem font16");

        Label label = new Label(buttonType.toUpperCase());
        label.addStyleName("activityBarButtonItem label");

        if (invertIcon) {
            buttonPanel.add(label);
            buttonPanel.add(icon);
        } else {
            buttonPanel.add(icon);
            buttonPanel.add(label);
        }

        btn.add(buttonPanel);

    }

    private String getItemName(String constant) {
        if (constant.equals(BUTTON_NEXT)) {
            return "next";
        } else if (constant.equals(BUTTON_PREVIOUS)) {
            return "previous";
        } else if (constant.equals(BUTTON_DETAILS)) {
            return "details";
        } else if (constant.equals(BUTTON_CHAT)) {
            return "chat";
        } else {
            return "notes";
        }
    }

    private static native void setupArrowsNavigation() /*-{
        $doc.onkeydown = function() {
            if ($wnd.event && $wnd.event.target
                    && $wnd.event.target.nodeName != "TEXTAREA") {
                switch ($wnd.event.keyCode) {
                case 37: //LEFT ARROW
                    $doc.getElementsByClassName("btnPanel previous")[0].click();
                    break;
                case 39: //RIGHT ARROW
                    $doc.getElementsByClassName("btnPanel next")[0].click();
                    break;
                }
            }
        };
    }-*/;

    @UiHandler("btnNext")
    public void btnNextClicked(ClickEvent e) {
        if (!showDetails && btnNext.getStyleName().indexOf("disabled") < 0) {
            clientFactory.getEventBus().fireEvent(NavigationRequest.next());
            clientFactory.getEventBus().fireEvent(new ShowChatDockEvent(chatDockEnabled));
        }
    }

    @UiHandler("btnPrevious")
    public void btnPrevClicked(ClickEvent e) {
        if (!showDetails && btnPrevious.getStyleName().indexOf("disabled") < 0) {
            clientFactory.getEventBus().fireEvent(NavigationRequest.prev());
            clientFactory.getEventBus().fireEvent(new ShowChatDockEvent(chatDockEnabled));
        }
    }

    @UiHandler("btnNotes")
    void handleClickBtnNotes(ClickEvent e) {
        if (notesPopup == null) {
            notesPopup = new NotesPopup(clientFactory.getKornellSession(),
                    session.getCurrentCourseClass().getCourseClass().getUUID(),
                    session.getCurrentCourseClass().getEnrollment().getNotes());
            notesPopup.show();
        } else {
            notesPopup.show();
        }
    }

    @UiHandler("btnChat")
    void handleClickBtnChat(ClickEvent e) {
        if (!(chatDockEnabled && session.getCurrentCourseClass().getCourseClass().isChatDockEnabled()) && !showDetails
                && btnChat.getStyleName().indexOf("disabled") < 0) {
            clientFactory.getEventBus().fireEvent(new ShowChatDockEvent(!chatDockEnabled));
        }
    }

    private void enableButton(String btn, boolean enable) {
        if (BUTTON_NEXT.equals(btn)) {
            if (enable && !showDetails) {
                btnNext.removeStyleName("disabled");
            } else {
                btnNext.addStyleName("disabled");
            }
        } else if (BUTTON_PREVIOUS.equals(btn)) {
            if (enable && !showDetails) {
                btnPrevious.removeStyleName("disabled");
            } else {
                btnPrevious.addStyleName("disabled");
            }
        } else if (BUTTON_CHAT.equals(btn)) {
            if (enable && !showDetails) {
                btnChat.removeStyleName("disabled");
            } else {
                btnChat.addStyleName("disabled");
            }
        }
    }

    @Override
    public void setPresenter(Presenter presenter) {
    }

    @Override
    public void onProgress(ProgressEvent event) {
        boolean isMonoSCO = (event.getTotalPages() <= 1);
        if (event.getCurrentPage() != 0) {
            updateProgressBarPanel(event.getCurrentPage(), event.getTotalPages(), event.getProgressPercent());
            enablePrev = event.hasPrevious();
            enableNext = event.hasNext();
        }
        btnNext.setVisible(!isMonoSCO);
        btnPrevious.setVisible(!isMonoSCO);
        progressBarPanel.setVisible(!isMonoSCO);
        if (isMonoSCO) {
            btnDetails.addStyleName("firstMonoSCO");
            btnNotes.addStyleName("last");
        } else {
            btnDetails.removeStyleName("firstMonoSCO");
            btnNotes.removeStyleName("last");
        }
        shouldShowActivityBar = isEnrolled && session.getCurrentCourseClass() != null
                && !EntityState.inactive.equals(session.getCurrentCourseClass().getCourseClass().getState())
                && session.getCurrentCourseClass().getCourseVersionTO().getCourseVersion().getParentVersionUUID() == null;
        clientFactory.getEventBus().fireEvent(new HideSouthBarEvent(!shouldShowActivityBar));
        this.setVisible(shouldShowActivityBar);

        enableButton(BUTTON_PREVIOUS, !showDetails && enablePrev && allowedPrev);
        enableButton(BUTTON_NEXT, !showDetails && enableNext && allowedNext);
    }

    @UiHandler("btnDetails")
    void handleClickBtnDetails(ClickEvent e) {
        if (shouldShowActivityBar) {
            clientFactory.getEventBus().fireEvent(new ShowDetailsEvent(!showDetails));
        }
    }

    @Override
    public void onShowDetails(ShowDetailsEvent event) {
        this.showDetails = event.isShowDetails();
        if (showDetails) {
            clientFactory.getEventBus().fireEvent(new ShowChatDockEvent(false));
            btnDetails.addStyleName("btnAction courseClassDetailsShown");
            displayButton(btnDetails, constants.course(), new Icon(IconType.BOOK));
        } else {
            if (!chatDockEnabled && session.getCurrentCourseClass() != null
                    && session.getCurrentCourseClass().getCourseClass().isChatDockEnabled()) {
                clientFactory.getEventBus().fireEvent(new ShowChatDockEvent(true));
            }
            btnDetails.removeStyleName("btnAction");
            btnDetails.removeStyleName("courseClassDetailsShown");
            displayButton(btnDetails, BUTTON_DETAILS, new Icon(IconType.BOOK));
        }
        enableButton(BUTTON_PREVIOUS, !showDetails && enablePrev && allowedPrev);
        enableButton(BUTTON_NEXT, !showDetails && enableNext && allowedNext);
        enableButton(BUTTON_CHAT, !showDetails);
        updateProgressBarPanel();
    }

    @Override
    public void onShowChatDock(ShowChatDockEvent event) {
        this.chatDockEnabled = event.isShowChatDock();
        if (chatDockEnabled) {
            if (showDetails) {
                clientFactory.getEventBus().fireEvent(new ShowDetailsEvent(false));
            }
            btnChat.addStyleName("btnAction");
        } else {
            btnChat.removeStyleName("btnAction");
        }
    }

    @Override
    public void onNextActivityOK(NavigationAuthorizationEvent evt) {
        allowedNext = true;
        enableButton(BUTTON_NEXT, enableNext && allowedNext);
    }

    @Override
    public void onNextActivityNotOK(NavigationAuthorizationEvent evt) {
        allowedNext = false;
        enableButton(BUTTON_NEXT, enableNext && allowedNext);
    }

    @Override
    public void onPrevActivityOK(NavigationAuthorizationEvent evt) {
        allowedPrev = true;
        enableButton(BUTTON_PREVIOUS, enablePrev && allowedPrev);
    }

    @Override
    public void onPrevActivityNotOK(NavigationAuthorizationEvent evt) {
        allowedPrev = false;
        enableButton(BUTTON_PREVIOUS, enablePrev && allowedPrev);
    }

}
