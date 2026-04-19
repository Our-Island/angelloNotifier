package top.ourisland.angellonotifier.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerMailbox {

    Map<Long, PlayerNotificationState> states = new LinkedHashMap<>();
    String lastKnownName;

    public String lastKnownName() {
        return lastKnownName;
    }

    public void lastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public Map<Long, PlayerNotificationState> states() {
        return states;
    }

}
