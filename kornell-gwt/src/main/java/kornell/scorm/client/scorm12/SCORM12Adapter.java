package kornell.scorm.client.scorm12;

import static kornell.core.util.StringUtils.isSome;
import static kornell.scorm.client.scorm12.Scorm12.logger;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Timer;

import kornell.api.client.Callback;
import kornell.api.client.KornellSession;
import kornell.core.entity.ActomEntries;
import kornell.core.entity.ContentSpec;
import kornell.gui.client.presentation.classroom.ClassroomPlace;

public class SCORM12Adapter implements CMIConstants {

    /**
     * How much time a client can stay unsynced with the server after setting a
     * data model value, in milliseconds.
     */
    private static final int DIRTY_TOLERANCE = 2000;

    public static SCORM12Adapter create(SCORM12Runtime rte, KornellSession session, PlaceController placeCtrl,
            String enrollmentUUID, String actomKey, ActomEntries entries) {

        return new SCORM12Adapter(rte, session, placeCtrl, enrollmentUUID, actomKey, entries);
    }

    // Scope Parameters
    private String enrollmentUUID;
    private String actomKey;

    // State
    private String lastError = NoError;

    // UI/Client
    private KornellSession client;
    private PlaceController placeCtrl;
    private SCORM12Runtime rte;
    private Timer timer;

    public SCORM12Adapter(SCORM12Runtime rte, KornellSession session, PlaceController placeCtrl, String enrollmentUUID,
            String actomKey, ActomEntries ae) {
        logger.info("SCORM API 1.2.2015_05_07_20_00");
        this.rte = rte;
        this.enrollmentUUID = enrollmentUUID;
        this.actomKey = actomKey;
        this.client = session;
        this.placeCtrl = placeCtrl;
    }

    public String LMSInitialize(String param) {
        String result = TRUE;
        logger.finer("LMSInitialize[" + param + "] = " + result);
        return result;
    }

    public String LMSFinish(String param) {
        String result = TRUE;
        logger.finer("LMSFinish[" + param + "]");
        return result;
    }

    public String LMSGetLastError() {
        logger.finer("LMSGetLastError[]");
        return lastError;
    }

    public String LMSGetValue(String param, String moduleUUID) {
        String targetUUID = getEnrollmentUUID(moduleUUID);
        CMITree dataModel = getDataModel(targetUUID, actomKey);
        String result = "";

        if (dataModel != null)
            result = dataModel.getValue(param);
        else
            logger.warning("Null data model for LMSGetValue[" + param + "]@[" + moduleUUID + "/" + actomKey + "]");
        logger.finer("LMSGetValue[" + param + "]@[" + moduleUUID + "/" + actomKey + "] = " + result);
        return result;
    }

    public String LMSCommit(String param) {
        String result = TRUE;
        syncOnLMSCommit();
        logger.finer("LMSCommit[" + param + "] = " + result);
        return result;
    }

    private void syncOnLMSCommit() {
        scheduleSync(enrollmentUUID, actomKey, "syncOnLMSCommit");
    }

    public String LMSSetDouble(String key, Double value, String moduleUUID) {
        String strValue = Double.toString(value);
        return LMSSetString(key, strValue, moduleUUID);
    }

    // TODO: Consider API changes for MultiSCO
    public String LMSSetString(String key, String value, String moduleUUID) {
        String result = FALSE;
        String targetUUID = getEnrollmentUUID(moduleUUID);
        CMITree dataModel = getDataModel(targetUUID, actomKey);
        if (dataModel != null) {
            result = dataModel.setValue(key, value);
            rte.onLMSSetValue(key, value);
        } else
            logger.warning("Null data model for LMSSetValue [" + key + " = " + value + "]@[" + targetUUID + "/"
                    + actomKey + "] = " + result);
        scheduleSync(targetUUID, actomKey);
        logger.finer("LMSSetValue [" + key + " = " + value + "]@[" + targetUUID + "/" + actomKey + "] = " + result);
        return result;
    }

    private CMITree getDataModel(String moduleUUID, String moduleActomKey) {
        String targetUUID = getEnrollmentUUID(moduleUUID);
        CMITree dataModel = rte.getDataModel(targetUUID, moduleActomKey);
        return dataModel;
    }

    private String getEnrollmentUUID(String moduleUUID) {
        return isSome(moduleUUID) ? moduleUUID : enrollmentUUID;
    }

    public void launch(String enrollmentUUID) {
        placeCtrl.goTo(new ClassroomPlace(enrollmentUUID, ContentSpec.SCORM12));
    }

    private void scheduleSync(final String moduleUUID, final String moduleActomKey) {
        scheduleSync(moduleUUID, moduleActomKey, "scheduledSync");
    }

    private void scheduleSync(final String moduleUUID, final String moduleActomKey, final String syncCause) {
        if (timer == null)
            timer = new Timer() {
                public void run() {
                    syncAfterSet(moduleUUID, moduleActomKey);
                }

                private void syncAfterSet(String moduleUUID, String moduleActomKey) {
                    sync(moduleUUID, moduleActomKey, syncCause);
                    timer = null;
                }
            };
        timer.schedule(DIRTY_TOLERANCE);
    }

    public void onActomEntered() {
        scheduleSync(enrollmentUUID, actomKey, "onActomEntered");
    }

    private void sync(final String syncEnrollmentUUID, final String syncActomKey, final String syncCause) {
        class Scrub extends Callback<ActomEntries> {
            @Override
            public void ok(ActomEntries to) {
                // TODO: Scrub only verified
                getDataModel(syncEnrollmentUUID, syncActomKey).scrub();
            }
        }
        CMITree dataModel = getDataModel(syncEnrollmentUUID, syncActomKey);
        boolean isDirty = dataModel.isDirty();
        if (dataModel != null && isDirty) {
            // GWT.debugger();
            client.enrollment(syncEnrollmentUUID).actom(syncActomKey).put(CMITree.collectDirty(dataModel), syncCause,
                    new Scrub());
        }
    }

}
