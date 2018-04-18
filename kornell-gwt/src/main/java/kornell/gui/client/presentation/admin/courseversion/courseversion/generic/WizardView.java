package kornell.gui.client.presentation.admin.courseversion.courseversion.generic;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.CourseVersion;
import kornell.core.util.StringUtils;
import kornell.gui.client.presentation.admin.courseversion.courseversion.AdminCourseVersionContentView.Presenter;
import kornell.gui.client.util.view.KornellNotification;

public class WizardView extends Composite {
    interface MyUiBinder extends UiBinder<Widget, WizardView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    private KornellSession session;
    private EventBus bus;
    private Presenter presenter;
    private IFrameElement iframe;

    @UiField
    FlowPanel wizardPanel;

    public WizardView(final KornellSession session, EventBus bus) {
        this.session = session;
        this.bus = bus;
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void init(Presenter presenter) {
        this.presenter = presenter;
        createIFrame();
    }

    private void createIFrame() {
        if (iframe == null) {
            String iframeSrc = "/angular/knl.html?cache-buster="+session.getCurrentVersionAPI()+"#!/wizard";
            if(Window.Location.getHostName().indexOf("localhost") >= 0){
                iframeSrc = "http://localhost:8008" + iframeSrc;
            }
            iframe = Document.get().createIFrameElement();
            iframe.setSrc(iframeSrc);
            iframe.setId("angularFrame");
            iframe.addClassName("externalContent");
            iframe.setAttribute("allowtransparency", "true");
            iframe.setAttribute("style", "background-color: transparent;width: 100%;height: calc(100vh - 169px);");
            //allowing html5 video player to work on fullscreen inside the iframe
            iframe.setAttribute("allowFullScreen", "true");
            iframe.setAttribute("webkitallowfullscreen", "true");
            iframe.setAttribute("mozallowfullscreen", "true");
            Event.sinkEvents(iframe, Event.ONLOAD);
            wizardPanel.getElement().appendChild(iframe);
            injectEventListener(this);
        }
    }

    public void iframeIsReady(String message) {
        if (StringUtils.isSome(presenter.getCourseVersion().getClassroomJson())){
            sendIFrameMessage("classroomJsonLoad", presenter.getCourseVersion().getClassroomJson());
        } else if(StringUtils.isSome(presenter.getCourseVersion().getClassroomJsonPublished())){
            sendIFrameMessage("classroomJsonLoad", presenter.getCourseVersion().getClassroomJsonPublished());
        } else {
            sendIFrameMessage("classroomJsonNew", presenter.getCourse().getName());
        }
    }

    public void publishWizard(String wizardData) {
        presenter.getCourseVersion().setClassroomJson(null);
        presenter.getCourseVersion().setClassroomJsonPublished(wizardData);
        session.courseVersion(presenter.getCourseVersion().getUUID()).update(presenter.getCourseVersion(), new Callback<CourseVersion>() {
            @Override
            public void ok(CourseVersion courseVersionSaved) {
                presenter.setCourseVersion(courseVersionSaved);
                sendIFrameMessage("classroomJsonSaved", "");
                KornellNotification.show("Publicação feita com sucesso.");
            }
        });
    }

    public void saveWizard(String wizardData) {
        presenter.getCourseVersion().setClassroomJson(wizardData);
        session.courseVersion(presenter.getCourseVersion().getUUID()).update(presenter.getCourseVersion(), true, new Callback<CourseVersion>() {
            @Override
            public void ok(CourseVersion courseVersionSaved) {
                presenter.setCourseVersion(courseVersionSaved);
                sendIFrameMessage("classroomJsonSaved", "");
            }
        });
    }

    public void discardWizard() {
        presenter.getCourseVersion().setClassroomJson(null);
        session.courseVersion(presenter.getCourseVersion().getUUID()).update(presenter.getCourseVersion(), new Callback<CourseVersion>() {
            @Override
            public void ok(CourseVersion courseVersionSaved) {
                presenter.setCourseVersion(courseVersionSaved);
                sendIFrameMessage("classroomJsonLoad", presenter.getCourseVersion().getClassroomJsonPublished());
                KornellNotification.show("Alterações descartadas com sucesso.");
            }
        });
    }

    public void requestUploadPath(String filename) {
        session.course(presenter.getCourseVersion().getCourseUUID()).getWizardContentUploadURL(filename, new Callback<String>() {
            @Override
            public void ok(String url) {
                sendIFrameMessage("responseUploadPath", url);
            }
        });
    }

    private native void sendIFrameMessage(String type, String message) /*-{
	    var domain = $wnd.location;
	    if(domain.host.indexOf('localhost:') >= 0){
	    	domain = '*';
	    }
	    var iframe = $wnd.document.getElementById('angularFrame').contentWindow;
	    var data = { type: type, message: message};
	    iframe.postMessage(data, domain);
	}-*/;

    private native void injectEventListener(WizardView v) /*-{
	    function postMessageListener(e) {
	        var curUrl = $wnd.location;
	        if(e.data.type === "wizardReady"){
	        	v.@kornell.gui.client.presentation.admin.courseversion.courseversion.generic.WizardView::iframeIsReady(Ljava/lang/String;)(e.data.message);
	        } else if(e.data.type === "wizardSave"){
                v.@kornell.gui.client.presentation.admin.courseversion.courseversion.generic.WizardView::saveWizard(Ljava/lang/String;)(e.data.message);
            } else if(e.data.type === "wizardPublish"){
                v.@kornell.gui.client.presentation.admin.courseversion.courseversion.generic.WizardView::publishWizard(Ljava/lang/String;)(e.data.message);
            } else if(e.data.type === "wizardDiscard"){
                v.@kornell.gui.client.presentation.admin.courseversion.courseversion.generic.WizardView::discardWizard()();
            } else if(e.data.type === "requestUploadPath"){
                v.@kornell.gui.client.presentation.admin.courseversion.courseversion.generic.WizardView::requestUploadPath(Ljava/lang/String;)(e.data.message);
            } else if(e.data.type === "kornellNotification"){
                if(e.data.notificationType === "success"){
                    @kornell.gui.client.util.view.KornellNotification::show(Ljava/lang/String;)(e.data.message);
                } else {
                    @kornell.gui.client.util.view.KornellNotification::showError(Ljava/lang/String;)(e.data.message);
                }
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