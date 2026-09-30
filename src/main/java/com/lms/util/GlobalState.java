package com.lms.util;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GlobalState {
    private static final StringProperty syncStatusText = new SimpleStringProperty("● Offline");
    private static final StringProperty syncStatusColor = new SimpleStringProperty("#71717a");

    public static StringProperty syncStatusTextProperty() { return syncStatusText; }
    public static StringProperty syncStatusColorProperty() { return syncStatusColor; }

    public static void setSyncStatus(String text, String colorHex) {
        javafx.application.Platform.runLater(() -> {
            syncStatusText.set(text);
            syncStatusColor.set(colorHex);
        });
    }

    public static void refreshSyncState() {
        com.lms.db.SyncConfigDAO syncDao = new com.lms.db.SyncConfigDAO();
        com.lms.db.SyncConfigDAO.SyncConfig config = syncDao.getConfig();
        String initialStatus = "● Offline";
        String initialColor = "#71717a";
        
        if (config != null && config.isEnabled()) {
            if (config.lastSyncTime() != null && !config.lastSyncTime().isEmpty()) {
                String shortTime = config.lastSyncTime();
                if (shortTime.contains(" ")) {
                    shortTime = shortTime.substring(shortTime.indexOf(" ") + 1);
                    if (shortTime.lastIndexOf(":") > 0 && shortTime.lastIndexOf(":") != shortTime.indexOf(":")) {
                        shortTime = shortTime.substring(0, shortTime.lastIndexOf(":"));
                    }
                }
                initialStatus = "● Synced " + shortTime;
                initialColor = "#22c55e";
            } else {
                initialStatus = "● Sync Pending";
                initialColor = "#039ED3";
            }
        }
        setSyncStatus(initialStatus, initialColor);
    }
}
