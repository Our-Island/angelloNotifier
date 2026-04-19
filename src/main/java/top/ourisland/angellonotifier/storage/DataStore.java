package top.ourisland.angellonotifier.storage;

import top.ourisland.angellonotifier.model.Notification;
import top.ourisland.angellonotifier.model.PlayerMailbox;

import java.util.*;

public class DataStore {

    List<Notification> notifications = new ArrayList<>();
    Map<UUID, PlayerMailbox> playerMailboxes = new LinkedHashMap<>();
    long nextNotificationId = 1L;

    public long nextNotificationId() {
        return nextNotificationId;
    }

    public void nextNotificationId(long nextNotificationId) {
        this.nextNotificationId = nextNotificationId;
    }

    public List<Notification> notifications() {
        return notifications;
    }

    public Map<UUID, PlayerMailbox> playerMailboxes() {
        return playerMailboxes;
    }

}
