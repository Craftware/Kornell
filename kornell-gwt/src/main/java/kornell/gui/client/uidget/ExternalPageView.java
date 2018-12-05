package kornell.gui.client.uidget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.FlowPanel;
import kornell.api.client.KornellSession;
import kornell.core.entity.ContentSpec;
import kornell.core.entity.InstitutionType;
import kornell.core.lom.ExternalPage;
import kornell.core.util.StringUtils;
import kornell.gui.client.GenericClientFactoryImpl;
import kornell.gui.client.event.ShowChatDockEvent;
import kornell.gui.client.event.ShowChatDockEventHandler;
import kornell.gui.client.personnel.classroom.WizardTeacher;
import kornell.gui.client.util.view.Positioning;

public class ExternalPageView extends Uidget implements ShowChatDockEventHandler {

    private IFrameElement iframe;
    private FlowPanel panel;
    private KornellSession session;

    public ExternalPageView(ExternalPage page) {
        session = GenericClientFactoryImpl.KORNELL_SESSION;

        createIFrame(page.getURL());

        panel = new FlowPanel();
        panel.setStyleName("contentWrapper");
        panel.getElement().appendChild(iframe);
        String url = StringUtils.mkurl("/", page.getURL());
        if(session.getCurrentCourseClass() != null &&
                ContentSpec.WIZARD.equals(session.getCurrentCourseClass().getCourseVersionTO().getCourseTO().getCourse().getContentSpec())){
            String classroomJson = new WizardTeacher(session.getCurrentCourseClass()).getClassroomJson();
            url += "&classroomInfo="+classroomJson;
            url += "&studentName="+session.getCurrentUser().getPerson().getFullName();
        }
        iframe.setSrc(url);
        initWidget(panel);


        GenericClientFactoryImpl.EVENT_BUS.addHandler(ShowChatDockEvent.TYPE, this);
        GenericClientFactoryImpl.EVENT_BUS.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
            @Override
            public void onPlaceChange(PlaceChangeEvent event) {
                placeIframe();
            }
        });
    }

    private void createIFrame(String url) {
        boolean isWizardClass = session.getCurrentCourseClass() != null &&
                ContentSpec.WIZARD.equals(session.getCurrentCourseClass().getCourseVersionTO().getCourseTO().getCourse().getContentSpec());
        if (iframe == null) {
            iframe = Document.get().createIFrameElement();
            iframe.addClassName("externalContent");
            iframe.setAttribute("allowtransparency", "true");
            iframe.setAttribute("style", "background-color: transparent; opacity:0;");
            // allowing html5 video player to work on fullscreen inside the iframe
            iframe.setAttribute("allowFullScreen", "true");
            iframe.setAttribute("webkitallowfullscreen", "true");
            iframe.setAttribute("mozallowfullscreen", "true");
            Timer preventWhiteFlashTimer = new Timer() {
                @Override
                public void run() {
                    iframe.setAttribute("style", "background-color: transparent; opacity:1;");
                }
            };
            preventWhiteFlashTimer.schedule(333);
            Event.sinkEvents(iframe, Event.ONLOAD);
            Event.setEventListener(iframe, new EventListener() {

                @Override
                public void onBrowserEvent(Event event) {
                    fireViewReady();
                }
            });
            if (isWizardClass) {
                injectEventListener(this);
            }
        }
        String iframeUrl = StringUtils.mkurl("/", url);
        if (isWizardClass && Window.Location.getHostName().indexOf("localhost") >= 0){
            iframeUrl = "http://localhost:8008" + iframeUrl;
        }
        iframe.setSrc(iframeUrl);
        iframe.setId("classroomFrame");
        placeIframe();

        // Weird yet simple way of solving FF's weird behavior
        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                Scheduler.get().scheduleDeferred(new Command() {
                    @Override
                    public void execute() {
                        placeIframe();
                    }
                });
            }
        });
    }

    private void placeIframe() {
        iframe.setPropertyString("width", "100%");
        int h = (Window.getClientHeight() - Positioning.NORTH_BAR);
        if (session.getCurrentCourseClass() != null
                && !(InstitutionType.DASHBOARD.equals(session.getInstitution().getInstitutionType()))) {
            h -= Positioning.SOUTH_BAR;
        }
        String height = h + "px";
        iframe.setPropertyString("height", height);
    }

    @Override
    public void onShowChatDock(ShowChatDockEvent event) {
        if (event.isShowChatDock()) {
            panel.addStyleName("chatDocked");
        } else {
            panel.removeStyleName("chatDocked");
        }
    }

    public void iframeIsReady(String message) {
        String classroomJson = new WizardTeacher(session.getCurrentCourseClass()).getClassroomJson();
        String iframeMessage = "{" +
                "\"classroomInfo\":\"" + classroomJson + "\"," +
                "\"studentName\":\"" + session.getCurrentUser().getPerson().getFullName() + "\"" +
                "}";
        sendIFrameMessage("initializeContents", iframeMessage);
    }

    private native void sendIFrameMessage(String type, String message) /*-{
        var domain = $wnd.location;
        if (domain.host.indexOf('localhost:') >= 0) {
            domain = '*';
        }
        if ($wnd.document.getElementById('classroomFrame')) {
            var iframe = $wnd.document.getElementById('classroomFrame').contentWindow;
            var data = {type: type, message: message};
            iframe.postMessage(JSON.stringify(data), domain);
        }
    }-*/;

    private native void injectEventListener(ExternalPageView v) /*-{
        function postMessageListener(e) {
            var messageData = JSON.parse(e.data);
            if (messageData.type === "classroomReady") {
                v.@kornell.gui.client.uidget.ExternalPageView::iframeIsReady(Ljava/lang/String;)(messageData.message);
            }
        }

        // Listen to message from child window
        if ($wnd.addEventListener) {
            $wnd.addEventListener("message", postMessageListener, false);
        } else if ($wnd.attachEvent) {
            $wnd.attachEvent("onmessage", postMessageListener, false);
        } else {
            $wnd["onmessage"] = postMessageListener;
        }
    }-*/;

}
