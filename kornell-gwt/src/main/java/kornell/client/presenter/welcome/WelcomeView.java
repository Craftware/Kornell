package kornell.client.presenter.welcome;

import com.google.gwt.user.client.ui.IsWidget;

public interface WelcomeView  extends IsWidget {
	public interface Presenter extends IsWidget {
	}

	void setPresenter(Presenter presenter);
}
