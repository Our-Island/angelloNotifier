package top.ourisland.angellonotifier.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtils {

    static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private TimeUtils() {
    }

    public static String format(long epochMs) {
        return FORMATTER.format(Instant.ofEpochMilli(epochMs));
    }

}
