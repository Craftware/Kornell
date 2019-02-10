package kornell.gui.client.presentation.admin.courseclass.courseclasses;

import java.util.List;

import com.google.gwt.user.client.ui.IsWidget;

import kornell.core.to.CourseClassTO;
import kornell.gui.client.util.view.table.PaginationPresenter;

public interface AdminCourseClassesView extends IsWidget {
    public interface Presenter extends PaginationPresenter<CourseClassTO> {
        void updateCourseClass(String courseClassUUID);

        void deleteCourseClass(CourseClassTO courseClassTO);

        void duplicateCourseClass(CourseClassTO courseClassTO);

        boolean showActionButton(String actionName, CourseClassTO courseClassTO);
    }

    void setPresenter(AdminCourseClassesView.Presenter presenter);

    void setCourseClasses(List<CourseClassTO> courseClasses);
}