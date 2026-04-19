package top.ourisland.angellonotifier.model;

import java.util.List;

public record Notification(
        long id,
        String title,
        List<String> body,
        String createdBy,
        long createdAtEpochMs,
        int lifetime
) {

}
