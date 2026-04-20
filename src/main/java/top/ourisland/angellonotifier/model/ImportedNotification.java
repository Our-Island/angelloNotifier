package top.ourisland.angellonotifier.model;

import java.util.List;

public record ImportedNotification(
        String title,
        List<String> body,
        Integer lifetime,
        String createdBy,
        Long createdAtEpochMs
) {

}
