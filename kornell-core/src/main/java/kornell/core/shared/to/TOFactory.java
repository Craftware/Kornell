package kornell.core.shared.to;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;

public interface TOFactory extends AutoBeanFactory {
	  AutoBean<CourseTO>  courseTO();
	  AutoBean<CoursesTO> coursesTO();
}
