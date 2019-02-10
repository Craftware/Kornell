package kornell.gui.client.presentation.classroom.generic.details;

import static kornell.core.util.StringUtils.mkurl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import kornell.api.client.KornellSession;
import kornell.core.lom.ExternalPage;
import kornell.core.to.CourseClassTO;
import kornell.gui.client.event.ProgressEvent;
import kornell.gui.client.event.ProgressEventHandler;
import kornell.gui.client.event.ShowDetailsEvent;
import kornell.gui.client.sequence.NavigationRequest;
import kornell.gui.client.util.ClientConstants;

public class GenericPageView extends Composite implements ProgressEventHandler {
    interface MyUiBinder extends UiBinder<Widget, GenericPageView> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    private EventBus bus;
    private KornellSession session;
    private String IMAGES_PATH = mkurl(ClientConstants.IMAGES_PATH, "courseDetails");

    @UiField
    FlowPanel topicWrapper;
    @UiField
    Image topicImg;
    @UiField
    FlowPanel topicPanel;
    @UiField
    Image topicIcon;
    @UiField
    FlowPanel lblPage;

    private ExternalPage page;

    public GenericPageView(EventBus eventBus, KornellSession session, final PlaceController placeCtrl,
            final ExternalPage page, CourseClassTO currentCourse, boolean enableAnchor) {
        this.bus = eventBus;
        this.page = page;
        this.session = session;
        bus.addHandler(ProgressEvent.TYPE, this);
        initWidget(uiBinder.createAndBindUi(this));
        display(enableAnchor);
    }

    private void display(boolean enableAnchor) {
        String status = (page.isVisited() ? "finished" : "toStart") + session.getAssetsSkinModifier();
        topicImg.setUrl(mkurl(IMAGES_PATH, "pageIdent.png"));
        topicIcon.setUrl(mkurl(IMAGES_PATH, "status_" + status + ".png"));
        lblPage.clear();
        int index = page.getIndex();
        String title = index + ".  " + page.getTitle();
        if (enableAnchor || page.isVisited()) {
            Anchor pageAnchor = new Anchor(title);
            pageAnchor.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    bus.fireEvent(NavigationRequest.direct(page.getKey()));
                    bus.fireEvent(new ShowDetailsEvent(false));
                }
            });
            lblPage.add(pageAnchor);
        } else {
            lblPage.add(new Label(title));
        }
    }

    @Override
    public void onProgress(ProgressEvent event) {
        int index = page.getIndex();
        page.setVisited(index <= event.getPagesVisitedCount().intValue());
        // enable the anchor until the next one after the current
        display(!session.getInstitution().isEnforceSequentialProgress() || index == event.getPagesVisitedCount());
    }

}
