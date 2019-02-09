package kornell.api.client;

import kornell.core.entity.CourseClass;
import kornell.core.entity.Institution;
import kornell.core.entity.role.Roles;
import kornell.core.to.InstitutionEmailWhitelistTO;
import kornell.core.to.InstitutionHostNamesTO;
import kornell.core.to.InstitutionRegistrationPrefixesTO;
import kornell.core.to.RolesTO;

public class InstitutionClient extends RESTClient {

    private String institutionUUID;

    public InstitutionClient(String uuid) {
        this.institutionUUID = uuid;
    }

    public void get(Callback<Institution> cb) {
        GET("/institutions/" + institutionUUID).sendRequest(null, cb);
    }

    public void update(Institution institution, Callback<Institution> cb) {
        PUT("/institutions/" + institutionUUID).withContentType(Institution.TYPE).withEntityBody(institution).go(cb);
    }

    public void getRegistrationPrefixes(Callback<InstitutionRegistrationPrefixesTO> cb) {
        GET("/institutions/" + institutionUUID + "/registrationPrefixes").sendRequest(null, cb);
    }

    public void getAdmins(String bindMode, Callback<RolesTO> cb) {
        GET("institutions", institutionUUID, "admins" + "?bind=" + bindMode).withContentType(CourseClass.TYPE).go(cb);
    }

    public void updateAdmins(Roles roles, Callback<Roles> cb) {
        PUT("institutions", institutionUUID, "admins").withContentType(Roles.TYPE).withEntityBody(roles).go(cb);
    }

    public void getPublishers(String bindMode, Callback<RolesTO> cb) {
        GET("institutions", institutionUUID, "publishers" + "?bind=" + bindMode).withContentType(CourseClass.TYPE).go(cb);
    }

    public void updatePublishers(Roles roles, Callback<Roles> cb) {
        PUT("institutions", institutionUUID, "publishers").withContentType(Roles.TYPE).withEntityBody(roles).go(cb);
    }

    public void getInstitutionCourseClassesAdmins(String bindMode, Callback<RolesTO> cb) {
        GET("institutions", institutionUUID, "institutionCourseClassesAdmins" + "?bind=" + bindMode).withContentType(CourseClass.TYPE).go(cb);
    }

    public void updateInstitutionCourseClassesAdmins(Roles roles, Callback<Roles> cb) {
        PUT("institutions", institutionUUID, "institutionCourseClassesAdmins").withContentType(Roles.TYPE).withEntityBody(roles).go(cb);
    }

    public void getInstitutionCourseClassesObservers(String bindMode, Callback<RolesTO> cb) {
        GET("institutions", institutionUUID, "institutionCourseClassesObservers" + "?bind=" + bindMode).withContentType(CourseClass.TYPE).go(cb);
    }

    public void updateInstitutionCourseClassesObservers(Roles roles, Callback<Roles> cb) {
        PUT("institutions", institutionUUID, "institutionCourseClassesObservers").withContentType(Roles.TYPE).withEntityBody(roles).go(cb);
    }

    public void getHostnames(Callback<InstitutionHostNamesTO> cb) {
        GET("institutions", institutionUUID, "hostnames").go(cb);
    }

    public void updateHostnames(InstitutionHostNamesTO institutionHostNamesTO, Callback<InstitutionHostNamesTO> cb) {
        PUT("institutions", institutionUUID, "hostnames").withContentType(InstitutionHostNamesTO.TYPE)
        .withEntityBody(institutionHostNamesTO).go(cb);
    }

    public void getEmailWhitelist(Callback<InstitutionEmailWhitelistTO> cb) {
        GET("institutions", institutionUUID, "emailWhitelist").go(cb);
    }

    public void updateEmailWhitelist(InstitutionEmailWhitelistTO institutionEmailWhitelistTO,
            Callback<InstitutionEmailWhitelistTO> cb) {
        PUT("institutions", institutionUUID, "emailWhitelist").withContentType(InstitutionEmailWhitelistTO.TYPE)
        .withEntityBody(institutionEmailWhitelistTO).go(cb);
    }

    public void getUploadURL(String filename, Callback<String> callback) {
        GET("institutions", institutionUUID, "uploadUrl", "?filename=" + filename).go(callback);
    }
}
