package top.ourisland.angellonotifier.util;

import org.yaml.snakeyaml.Yaml;
import top.ourisland.angellonotifier.model.ImportedNotification;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static top.ourisland.angellonotifier.util.DataUtils.*;

public class NotificationImportUtils {

    static final int MAX_BYTES = 1024 * 1024;
    static final DateTimeFormatter SIMPLE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private NotificationImportUtils() {
    }

    public static ImportedNotification loadFromUri(URI uri) {
        String content = readText(uri);
        return parse(content);
    }

    static String readText(URI uri) {
        try {
            if (uri.getScheme() == null || uri.getScheme().isBlank()) {
                Path path = Path.of(uri.toString());
                return Files.readString(path, StandardCharsets.UTF_8);
            }
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return Files.readString(Path.of(uri), StandardCharsets.UTF_8);
            }

            var connection = uri.toURL().openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);

            try (var inputStream = connection.getInputStream()) {
                return readLimited(inputStream);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not read the imported file from the given URI.", ex);
        }
    }

    public static ImportedNotification parse(String content) {
        Object loaded = new Yaml().load(content);
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("The imported file must contain a JSON object.");
        }

        Map<String, Object> root = new LinkedHashMap<>(castMap(map));
        String title = string(root.get("title"), "").trim();
        List<String> body = parseBody(root.get("body"));
        Integer lifetime = parseLifetime(root.get("lifetime"));
        String createdBy = string(root.get("created_by"), string(root.get("created-by"), "")).trim();
        Long createdAtEpochMs = parseCreatedAt(root.get("created_at"));
        if (createdAtEpochMs == null) {
            createdAtEpochMs = parseCreatedAt(root.get("created-at"));
        }

        if (title.isEmpty()) {
            throw new IllegalArgumentException("The imported file is missing a valid title.");
        }
        if (body.isEmpty()) {
            throw new IllegalArgumentException("The imported file is missing a valid body.");
        }

        return new ImportedNotification(
                title,
                List.copyOf(body),
                lifetime,
                createdBy.isEmpty() ? null : createdBy,
                createdAtEpochMs
        );
    }

    static String readLimited(InputStream inputStream) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                total += len;
                if (total > MAX_BYTES) {
                    throw new IllegalArgumentException("The imported file is too large.");
                }
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toString(StandardCharsets.UTF_8);
        }
    }

    static List<String> parseBody(Object bodyValue) {
        List<String> lines = new ArrayList<>();
        if (bodyValue instanceof String text) {
            collectLines(lines, text);
            return lines;
        }
        if (bodyValue instanceof List<?> list) {
            for (Object entry : list) {
                if (entry == null) {
                    continue;
                }
                collectLines(lines, String.valueOf(entry));
            }
        }
        return lines;
    }

    static Integer parseLifetime(Object value) {
        if (value == null) {
            return null;
        }
        int lifetime = integer(value, -1);
        if (lifetime <= 0) {
            throw new IllegalArgumentException("The imported lifetime must be greater than 0.");
        }
        return lifetime;
    }

    static Long parseCreatedAt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return normalizeEpoch(number.longValue());
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.matches("^-?\\d+$")) {
            return normalizeEpoch(Long.parseLong(text));
        }

        try {
            var localDateTime = LocalDateTime.parse(text, SIMPLE_TIME);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception _) {
        }

        try {
            return Instant.parse(text).toEpochMilli();
        } catch (Exception _) {
        }

        try {
            return OffsetDateTime.parse(text).toInstant().toEpochMilli();
        } catch (Exception _) {
        }

        try {
            return ZonedDateTime.parse(text).toInstant().toEpochMilli();
        } catch (Exception _) {
        }

        throw new IllegalArgumentException("The imported created_at value is invalid.");
    }

    static void collectLines(List<String> lines, String text) {
        if (text == null) {
            return;
        }
        for (String line : text.split("\\R", -1)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
    }

    static long normalizeEpoch(long value) {
        long abs = Math.abs(value);
        if (abs < 100_000_000_000L) {
            return value * 1000L;
        }
        return value;
    }

}
