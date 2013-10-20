package kornell.gui.client.sequence;

import kornell.gui.client.presentation.course.CoursePlace;

import com.google.gwt.user.client.ui.FlowPanel;

public interface Sequencer 
	extends NavigationRequest.Handler {
	Sequencer withPanel(FlowPanel contentPanel);
	Sequencer withPlace(CoursePlace place);
	void go();
}