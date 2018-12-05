package kornell.gui.client.util.view;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Alert;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.github.gwtbootstrap.client.ui.event.ClosedEvent;
import com.github.gwtbootstrap.client.ui.event.ClosedHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

import kornell.core.util.StringUtils;

public class KornellNotification {

    private static List<PopupPanel> popupList = new ArrayList<>();
    private static int DEFAULT_CLOSE_DELAY = 3000;
    private static AlertType DEFAULT_ALERT_TYPE = AlertType.SUCCESS;

    public static void show(String message) {
        show(message, DEFAULT_ALERT_TYPE);
    }

    public static void show(String message, AlertType alertType) {
        show(message, alertType, DEFAULT_CLOSE_DELAY);
    }

    public static void show(String message, int timer) {
        show(message, DEFAULT_ALERT_TYPE, timer);
    }

    public static Alert show(String message, AlertType alertType, int timer) {
        if (StringUtils.isNone(message)) {
            return null;
        }
        final PopupPanel popup = new PopupPanel();

        Alert alert = new Alert();
        alert.addStyleName("kornellMessage");
        alert.setType(alertType);
        alert.setText(message);
        alert.addClosedHandler(new ClosedHandler() {
            @Override
            public void onClosed(ClosedEvent closedEvent) {
                closePopup(popup);
            }
        });

        popup.setWidget(alert);
        popupList.add(popup);
        repositionPopups();
        scheduleCloseTimer(timer, popup);

        return alert;
    }

    private static void scheduleCloseTimer(int timer, final PopupPanel popup) {
        if (timer > 0) {
            new Timer() {
                @Override
                public void run() {
                    closePopup(popup);
                }
            }.schedule(timer);
        }
    }

    private static void closePopup(PopupPanel popup) {
        popupList.remove(popup);
        popup.hide();
        repositionPopups();
    }

    public static void repositionPopups() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {

            @Override
            public void execute() {
                int currentTopPosition = Positioning.hasPlaceBar() ? Positioning.NORTH_BAR_PLUS : Positioning.NORTH_BAR;
                for (int i = 0; i < popupList.size(); i++) {
                    PopupPanel popup = popupList.get(i);
                    popup.setPopupPositionAndShow(new PositionCallback() {
                        @Override
                        public void setPosition(int offsetWidth, int offsetHeight) {
                            popup.setPopupPosition((Window.getClientWidth() - offsetWidth) / 2, offsetHeight + 10);
                            popup.show();
                        }
                    });
                    popup.setPopupPosition((Window.getClientWidth() - popup.getOffsetWidth()) / 2, currentTopPosition);
                    currentTopPosition += popup.getOffsetHeight() + 10;
                }
            }
        });
    }

    public static void showError(String message) {
        show(message, AlertType.ERROR, 4000);
    }

}
