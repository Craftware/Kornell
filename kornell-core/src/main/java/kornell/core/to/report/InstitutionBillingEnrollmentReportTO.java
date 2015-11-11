package kornell.core.to.report;



public class InstitutionBillingEnrollmentReportTO {
	private String enrollmentUUID;
	private String courseTitle;
	private String courseVersionName;
	private String courseClassName;
	private String fullName;
	private String username;
	private String firstEventFiredAt;
	
	public String getEnrollmentUUID() {
		return enrollmentUUID;
	}
	public void setEnrollmentUUID(String enrollmentUUID) {
		this.enrollmentUUID = enrollmentUUID;
	}
	public String getCourseTitle() {
		return courseTitle;
	}
	public void setCourseTitle(String courseTitle) {
		this.courseTitle = courseTitle;
	}
	public String getCourseVersionName() {
		return courseVersionName;
	}
	public void setCourseVersionName(String courseVersionName) {
		this.courseVersionName = courseVersionName;
	}
	public String getCourseClassName() {
		return courseClassName;
	}
	public void setCourseClassName(String courseClassName) {
		this.courseClassName = courseClassName;
	}
	public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getFirstEventFiredAt() {
		return firstEventFiredAt;
	}
	public void setFirstEventFiredAt(String firstEventFiredAt) {
		this.firstEventFiredAt = firstEventFiredAt;
	}
}