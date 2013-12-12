package kornell.api.client;

import kornell.core.entity.Enrollments;
import kornell.core.entity.Institution;
import kornell.core.to.CourseClassesTO;
import kornell.core.to.CoursesTO;
import kornell.core.to.EnrollmentRequestsTO;
import kornell.core.to.RegistrationRequestTO;
import kornell.core.to.RegistrationsTO;
import kornell.core.to.UserInfoTO;
import kornell.gui.client.event.LogoutEventHandler;
import kornell.gui.client.util.ClientProperties;

import com.google.gwt.core.client.GWT;

public class KornellClient extends RESTClient implements LogoutEventHandler {

	protected KornellClient() {
		KornellClient.bindToWindow(this);
	}

	public void getCourses(Callback<CoursesTO> callback) {
		GET("/courses").sendRequest(null, callback);
	}

	// TODO: Is this safe?
	public void getUser(String username, Callback<UserInfoTO> cb) {
		GET("/user/" + username).sendRequest(null, cb);
	}

	public void checkUser(String email, Callback<UserInfoTO> cb) {
		GET("/user/check/" + email).sendRequest(null, cb);
	}

	public void requestRegistration(RegistrationRequestTO registrationRequestTO, Callback<UserInfoTO> cb) {
		PUT("/user/registrationRequest").withContentType(RegistrationRequestTO.TYPE).withEntityBody(registrationRequestTO).go(cb);
	}

	public void requestPasswordChange(String email, String institutionName, Callback<Void> cb) {
		GET("/user/requestPasswordChange/" + email + "/" + institutionName).sendRequest(null, cb);
	}

	public void changePassword(String password, String passwordChangeUUID, Callback<Void> cb) {
		GET("/user/changePassword/" + password + "/" + passwordChangeUUID).sendRequest(null, cb);
	}

	public void sendWelcomeEmail(String userUUID, Callback<Void> cb) {
		GET("/email/welcome/" + userUUID).sendRequest(null, cb);
	}

	public void getCourseClassesTO(Callback<CourseClassesTO> cb) {
		GET("/courseClasses").sendRequest(null, cb);
	}

	public static KornellClient getInstance() {
		return new KornellClient();
	}

	public class RegistrationsClient {
		public void getUnsigned(Callback<RegistrationsTO> callback) {
			GET("/registrations").sendRequest("", callback);
		}
	}

	// TODO: extract those inner classes
	public class InstitutionClient {
		private String uuid;

		public InstitutionClient(String uuid) {
			this.uuid = uuid;
		}

		public void acceptTerms(Callback<Void> cb) {
			PUT("/institutions/" + uuid + "/acceptTerms").go(cb);
		}

		public void get(Callback<Institution> cb) {
			GET("/institutions/" + uuid).sendRequest(null, cb);
		}

		public void update(Institution institution, Callback<Institution> cb) {
			PUT("/institutions/" + uuid).withContentType(Institution.TYPE).withEntityBody(institution).go(cb);
		}
		
	}

	public void getInstitutionByName(String name, Callback<Institution> cb) {
		GET("/institutions/?name=" + name).sendRequest(null, cb);
	}

	public RegistrationsClient registrations() {
		// TODO: Consider lifecycle
		return new RegistrationsClient();
	}

	public InstitutionClient institution(String uuid) {
		return new InstitutionClient(uuid);
	}

	public void placeChanged(final String token) {
		PUT("/user/placeChange").sendRequest(token, new Callback<Void>() {
			@Override
			public void ok(Void v) {
				GWT.log("Place changed to [" + token + "]");
			}
		});
	}

	public void getEnrollmentsByCourseClass(String courseClassUUID, Callback<Enrollments> cb) {
		GET("/enrollment/?courseClassUUID=" + courseClassUUID).sendRequest(null, cb);
	}

	public void createEnrollments(EnrollmentRequestsTO enrollmentRequests, Callback<Enrollments> cb) {
		PUT("/enrollment/").withContentType(EnrollmentRequestsTO.TYPE).withEntityBody(enrollmentRequests).go(cb);
	}
	
	public void notesUpdated(String courseClassUUID, String notes) {
		PUT("/enrollment/" + courseClassUUID + "/notesUpdated").sendRequest(notes,
				new Callback<Void>() {
					@Override
					public void ok(Void v) {
						GWT.log("notes updated");
					}
				});
	}

	@Override
	public void onLogout() {
		forgetCredentials();
	}

	private void forgetCredentials() {
		ClientProperties.remove("Authorization");
	}

	public CourseClassClient courseClass(String courseClassUUID) {
		return new CourseClassClient(courseClassUUID);
	}

	static final EventsClient eventsClient = new EventsClient();

	public EventsClient events() {
		return eventsClient;
	}

	@SuppressWarnings("rawtypes")
	// TODO: Remove raw type
	public void check(String src, Callback callback) {
		HEAD(src).go(callback);
	}

	public String saySomething() {
		GWT.log("Bowties are cool");
		return "indeed they are";
	}

	public static native void bindToWindow(KornellClient kapi) /*-{
		$wnd.KAPI = {
			saySomething : function() {
				console
						.debug(kapi.@kornell.api.client.KornellClient::saySomething()());
			}
		}
	}-*/;

}
