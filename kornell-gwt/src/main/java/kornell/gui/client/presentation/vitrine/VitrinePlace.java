package kornell.gui.client.presentation.vitrine;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class VitrinePlace extends Place{
	public static final VitrinePlace instance = new VitrinePlace();
	private String passwordChangeUUID;
	private boolean userCreated;

	public VitrinePlace(String passwordChangeUUID) {
		this.passwordChangeUUID = passwordChangeUUID;
	}

	public VitrinePlace() {
		this.passwordChangeUUID = "";
	}

	public String getPasswordChangeUUID() {
		return passwordChangeUUID;
	}

	public void setPasswordChangeUUID(String passwordChangeUUID) {
		this.passwordChangeUUID = passwordChangeUUID;
	}

	@Prefix("vitrine")
	public static class Tokenizer implements PlaceTokenizer<VitrinePlace> {
		public VitrinePlace getPlace(String token) {
			return new VitrinePlace(token);
		}

		public String getToken(VitrinePlace place) {
			return place.getPasswordChangeUUID();
		}
	}

	@Override
	public String toString() {		
		return new Tokenizer().getToken(this);
	}

	public boolean isUserCreated() {
		return userCreated;
	}

	public void setUserCreated(boolean userCreated) {
		this.userCreated = userCreated;
	}
}
