package kornell.gui.client.mvp;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;

/**
*
* @author Saeed Zarinfam
*/
public interface AsyncActivityMapper{
	
	void getActivity(Place place, ActivityCallbackHandler activityCallbackHandler);

}