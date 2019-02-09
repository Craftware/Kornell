package kornell.gui.client.presentation.classroom.generic.details;

import static kornell.core.util.StringUtils.mkurl;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Collapse;
import com.github.gwtbootstrap.client.ui.CollapseTrigger;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.event.HideEvent;
import com.github.gwtbootstrap.client.ui.event.HideHandler;
import com.github.gwtbootstrap.client.ui.event.ShowEvent;
import com.github.gwtbootstrap.client.ui.event.ShowHandler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.KornellClient;
import kornell.api.client.KornellSession;
import kornell.core.entity.EntityState;
import kornell.core.entity.Enrollment;
import kornell.core.entity.EnrollmentState;
import kornell.core.lom.Content;
import kornell.core.lom.ContentFormat;
import kornell.core.lom.ExternalPage;
import kornell.core.lom.Topic;
import kornell.core.to.CourseClassTO;
import kornell.gui.client.util.ClientConstants;

public class GenericTopicView extends Composite {
    interface MyUiBinder extends UiBinder<Widget, GenericTopicView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private PlaceController placeCtrl;
    private EventBus bus;
    private String IMAGES_PATH = mkurl(ClientConstants.IMAGES_PATH, "courseDetails");
    private Content content;
    private KornellSession session;
    private CourseClassTO currentCourse;
    private int index;
    private boolean enableAnchorOnFirstChild;

    @UiField
    CollapseTrigger trigger;
    @UiField
    FlowPanel topicWrapper;
    @UiField
    FlowPanel topicPanel;
    @UiField
    Image topicIcon;
    @UiField
    Label lblTopic;
    @UiField
    Collapse collapse;
    @UiField
    FluidRow childrenPanel;

    public GenericTopicView(EventBus eventBus, KornellClient client, PlaceController placeCtrl, KornellSession session,
            CourseClassTO currentCourse, Content content, int index, boolean enableAnchorOnFirstChild) {
        this.bus = eventBus;
        this.placeCtrl = placeCtrl;
        this.session = session;
        this.content = content;
        this.currentCourse = currentCourse;
        this.index = index;
        this.enableAnchorOnFirstChild = enableAnchorOnFirstChild;
        initWidget(uiBinder.createAndBindUi(this));

        collapse.addShowHandler(new ShowHandler() {
            @Override
            public void onShow(ShowEvent showEvent) {
                updateIconURL(true);
            }
        });
        collapse.addHideHandler(new HideHandler() {
            @Override
            public void onHide(HideEvent hideEvent) {
                updateIconURL(false);
            }
        });
        Timer timer = new Timer() {
            public void run() {
                display();
            }
        };
        timer.schedule(500);
    }

    public void show(final boolean show) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                if (show)
                    collapse.show();
                else
                    collapse.hide();
            }
        });
    }

    private void display() {
        trigger.setTarget("#toggle" + index);
        collapse.setId("toggle" + index);
        updateIconURL(false);

        String topicName = "???Topic???";
        Topic topic = content.getTopic();
        List<Content> children = new ArrayList<Content>();
        if (ContentFormat.Topic.equals(content.getFormat())) {
            topicName = topic.getName();
            children = topic.getChildren();
        }
        lblTopic.setText(topicName);

        ExternalPage page;
        int childrenIndex = 0;

        for (Content contentItem : children) {
            page = contentItem.getExternalPage();
            if (!page.getTitle().startsWith("###")) { // TODO MDA
                Enrollment enrollment = currentCourse != null ? currentCourse.getEnrollment() : null;
                EnrollmentState state = enrollment != null ? enrollment.getState() : null;
                boolean enableAnchor = !session.getInstitution().isEnforceSequentialProgress() ||
                        ((page.isVisited() || (childrenIndex == 0 && enableAnchorOnFirstChild))
                        && EnrollmentState.enrolled.equals(state)
                        && !EntityState.inactive.equals(currentCourse.getCourseClass().getState()));
                childrenPanel.add(new GenericPageView(bus, session, placeCtrl, page, currentCourse, enableAnchor));
            }
            childrenIndex++;
        }

        if (childrenPanel.getWidgetCount() > 0) {
            this.addStyleName("cursorPointer");
        } else {
            topicIcon.addStyleName("shy");
            this.addStyleName("cursorDefault");
        }

        topicWrapper.removeStyleName("shy");
    }

    private void updateIconURL(boolean isOpened) {
        if (isOpened)
            topicIcon.setUrl(mkurl(IMAGES_PATH, "topic-expanded.png"));
        else
            topicIcon.setUrl(mkurl(IMAGES_PATH, "topic-contracted.png"));
    }
}
