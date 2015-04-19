package kornell.core.scorm12.rte.cmi.core;

import java.util.Map;

import kornell.core.scorm12.rte.DMElement;
import kornell.core.scorm12.rte.DataType;
import kornell.core.scorm12.rte.SCOAccess;

public class LessonLocation extends DMElement {
	public static LessonLocation dme = new LessonLocation();
	
	private LessonLocation() {
		super("lesson_location",true,DataType.CMIString255,SCOAccess.RW);
	}
	
	@Override
	public Map<String, String> initializeMap(Map<String, String> entries) {
		return defaultTo(entries, "");
	}
}
