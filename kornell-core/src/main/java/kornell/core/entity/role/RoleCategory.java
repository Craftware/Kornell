package kornell.core.entity.role;

import java.util.List;

import kornell.core.to.RoleTO;


public class RoleCategory {

    public static final String BIND_DEFAULT = "DEFAULT";
    public static final String BIND_WITH_PERSON = "PERSON";

    public static boolean isValidRole(Role role, RoleType type, String institutionUUID, String courseClassUUID) {
        switch (role.getRoleType()) {
        case user:
            if (RoleType.user.equals(type)) {
                return true;
            }
            break;
        case courseClassAdmin:
            if (RoleType.courseClassAdmin.equals(type)
                    && courseClassUUID != null
                    && role.getCourseClassAdminRole()
                    .getCourseClassUUID()
                    .equals(courseClassUUID)) {
                return true;
            }
            break;
        case courseClassObserver:
            if (RoleType.courseClassObserver.equals(type)
                    && courseClassUUID != null
                    && role.getCourseClassObserverRole()
                    .getCourseClassUUID()
                    .equals(courseClassUUID)) {
                return true;
            }
            break;
        case tutor:
            if (RoleType.tutor.equals(type)
                    && courseClassUUID != null
                    && role.getTutorRole()
                    .getCourseClassUUID()
                    .equals(courseClassUUID)) {
                return true;
            }
            break;
        case institutionAdmin:
            if (RoleType.institutionAdmin.equals(type)
                    && institutionUUID != null
                    && role.getInstitutionAdminRole()
                    .getInstitutionUUID()
                    .equals(institutionUUID)) {
                return true;
            }
            break;
        case platformAdmin:
            if (RoleType.platformAdmin.equals(type)
                    && institutionUUID != null
                    && role.getPlatformAdminRole()
                    .getInstitutionUUID()
                    .equals(institutionUUID)) {
                return true;
            }
            break;
        case controlPanelAdmin:
            if (RoleType.controlPanelAdmin.equals(type)) {
                return true;
            }
            break;
        case publisher:
            if (RoleType.publisher.equals(type)
                    && institutionUUID != null
                    && role.getPublisherRole()
                    .getInstitutionUUID()
                    .equals(institutionUUID)) {
                return true;
            }
            break;
        case institutionCourseClassesAdmin:
            if (RoleType.institutionCourseClassesAdmin.equals(type)
                    && institutionUUID != null
                    && role.getInstitutionCourseClassesAdminRole()
                    .getInstitutionUUID()
                    .equals(institutionUUID)) {
                return true;
            }
            break;
        case institutionCourseClassesObserver:
            if (RoleType.institutionCourseClassesObserver.equals(type)
                    && institutionUUID != null
                    && role.getInstitutionCourseClassesObserverRole()
                    .getInstitutionUUID()
                    .equals(institutionUUID)) {
                return true;
            }
            break;
        default:
            break;
        }
        return false;
    }

    public static boolean isRole(Role role, RoleType type) {
        switch (role.getRoleType()) {
        case user:
            if (RoleType.user.equals(type)) {
                return true;
            }
            break;
        case courseClassAdmin:
            if (RoleType.courseClassAdmin.equals(type)) {
                return true;
            }
            break;
        case courseClassObserver:
            if (RoleType.courseClassObserver.equals(type)) {
                return true;
            }
            break;
        case tutor:
            if (RoleType.tutor.equals(type)) {
                return true;
            }
            break;
        case institutionAdmin:
            if (RoleType.institutionAdmin.equals(type)) {
                return true;
            }
            break;
        case platformAdmin:
            if (RoleType.platformAdmin.equals(type)) {
                return true;
            }
            break;
        case controlPanelAdmin:
            if (RoleType.controlPanelAdmin.equals(type)) {
                return true;
            }
            break;
        case publisher:
            if (RoleType.publisher.equals(type)) {
                return true;
            }
            break;
        case institutionCourseClassesAdmin:
            if (RoleType.institutionCourseClassesAdmin.equals(type)) {
                return true;
            }
            break;
        case institutionCourseClassesObserver:
            if (RoleType.institutionCourseClassesObserver.equals(type)) {
                return true;
            }
            break;
        default:
            break;
        }
        return false;
    }

    public static boolean isControlPanelAdmin(List<RoleTO> roles) {
        return isValidRole(roles, RoleType.controlPanelAdmin, null, null);
    }

    public static boolean isPlatformAdmin(List<RoleTO> roleTOs, String institutionUUID) {
        return isValidRole(roleTOs, RoleType.platformAdmin, institutionUUID, null);
    }

    public static boolean isInstitutionAdmin(List<RoleTO> roleTOs, String institutionUUID) {
        return isValidRole(roleTOs, RoleType.institutionAdmin, institutionUUID, null);
    }

    public static boolean isCourseClassAdmin(List<RoleTO> roleTOs, String courseClassUUID) {
        return isValidRole(roleTOs, RoleType.courseClassAdmin, null, courseClassUUID);
    }

    public static boolean isCourseClassObserver(List<RoleTO> roleTOs, String courseClassUUID) {
        return isValidRole(roleTOs, RoleType.courseClassObserver, null, courseClassUUID);
    }

    public static boolean isCourseClassTutor(List<RoleTO> roleTOs, String courseClassUUID) {
        return isValidRole(roleTOs, RoleType.tutor, null, courseClassUUID);
    }

    public static boolean isPublisher(List<RoleTO> roleTOs, String institutionUUID) {
        return isValidRole(roleTOs, RoleType.publisher, institutionUUID, null);
    }

    public static boolean isInstitutionCourseClassesAdmin(List<RoleTO> roleTOs, String institutionUUID) {
        return isValidRole(roleTOs, RoleType.institutionCourseClassesAdmin, institutionUUID, null);
    }

    public static boolean isInstitutionCourseClassesObserver(List<RoleTO> roleTOs, String institutionUUID) {
        return isValidRole(roleTOs, RoleType.institutionCourseClassesObserver, institutionUUID, null);
    }

    public static boolean isValidRole(List<RoleTO> roleTOs, RoleType type, String institutionUUID, String courseClassUUID) {
        for (RoleTO roleTO : roleTOs) {
            if(isValidRole(roleTO.getRole(), type, institutionUUID, courseClassUUID)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasRole(List<RoleTO> roleTOs, RoleType type) {
        for (RoleTO roleTO : roleTOs) {
            if(RoleCategory.isRole(roleTO.getRole(), type)) {
                return true;
            }
        }
        return false;
    }

    public static void setCourseClassUUID(Role role, String courseClassUUID){
        if (RoleType.courseClassAdmin.equals(role.getRoleType())) {
            role.getCourseClassAdminRole().setCourseClassUUID(courseClassUUID);
        } else if (RoleType.tutor.equals(role.getRoleType())) {
            role.getTutorRole().setCourseClassUUID(courseClassUUID);
        } else if (RoleType.courseClassObserver.equals(role.getRoleType())) {
            role.getCourseClassObserverRole().setCourseClassUUID(courseClassUUID);
        }
    }

    public static String getCourseClassUUID(Role role){
        if (RoleType.courseClassAdmin.equals(role.getRoleType())) {
            return role.getCourseClassAdminRole().getCourseClassUUID();
        } else if (RoleType.tutor.equals(role.getRoleType())) {
            return role.getTutorRole().getCourseClassUUID();
        } else if (RoleType.courseClassObserver.equals(role.getRoleType())) {
            return role.getCourseClassObserverRole().getCourseClassUUID();
        } else {
            return "";
        }
    }

}
