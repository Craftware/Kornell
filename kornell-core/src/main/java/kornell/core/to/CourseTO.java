package kornell.core.to;

import kornell.core.entity.Course;

public interface CourseTO extends CourseDetails {
	public static final String TYPE = TOFactory.PREFIX + "course+json";

	Course getCourse();
	void setCourse(Course course);
}
