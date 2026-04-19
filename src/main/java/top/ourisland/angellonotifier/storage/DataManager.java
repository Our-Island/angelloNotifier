package top.ourisland.angellonotifier.storage;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;
import top.ourisland.angellonotifier.model.Notification;
import top.ourisland.angellonotifier.model.PlayerMailbox;
import top.ourisland.angellonotifier.model.PlayerNotificationState;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DataManager {

    Logger logger;
    Path dataPath;
    DataStore store = new DataStore();
    boolean dirty;

    public DataManager(Logger logger, Path dataPath) {
        this.logger = logger;
        this.dataPath = dataPath;
    }

    public synchronized void load() {
        try {
            ensureDataExists();

            var yaml = new Yaml();
            Map<String, Object> root;
            try (InputStream inputStream = Files.newInputStream(dataPath)) {
                Object loaded = yaml.load(inputStream);
                root = loaded instanceof Map<?, ?> map ? castMap(map) : new LinkedHashMap<>();
            }

            store.notifications().clear();
            store.playerMailboxes().clear();
            store.nextNotificationId(longValue(root.get("next-notification-id"), 1L));

            Object notificationsObj = root.get("notifications");
            if (notificationsObj instanceof List<?> list) {
                for (Object element : list) {
                    Map<String, Object> entry = map(element);
                    long id = longValue(entry.get("id"), 0L);
                    String title = string(entry.get("title"), "<gold>Untitled Notice</gold>");
                    List<String> body = stringList(entry.get("body"));
                    String createdBy = string(entry.get("created-by"), "CONSOLE");
                    long createdAt = longValue(entry.get("created-at"), System.currentTimeMillis());
                    int lifetime = intValue(entry.get("lifetime"), 0);
                    if (id > 0L) {
                        store.notifications().add(new Notification(id, title, body, createdBy, createdAt, lifetime));
                    }
                }
            }

            Map<String, Object> players = map(root.get("players"));
            for (Map.Entry<String, Object> entry : players.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    Map<String, Object> playerMap = map(entry.getValue());
                    PlayerMailbox mailbox = new PlayerMailbox();
                    mailbox.lastKnownName(string(playerMap.get("last-known-name"), null));

                    Map<String, Object> states = map(playerMap.get("states"));
                    for (Map.Entry<String, Object> stateEntry : states.entrySet()) {
                        long notificationId = Long.parseLong(stateEntry.getKey());
                        Map<String, Object> stateMap = map(stateEntry.getValue());
                        PlayerNotificationState state = new PlayerNotificationState();
                        state.previewCount(intValue(stateMap.get("preview-count"), 0));
                        state.read(bool(stateMap.get("read"), false));
                        state.firstSeenAtEpochMs(longValue(stateMap.get("first-seen-at"), 0L));
                        state.readAtEpochMs(longValue(stateMap.get("read-at"), 0L));
                        mailbox.states().put(notificationId, state);
                    }

                    store.playerMailboxes().put(uuid, mailbox);
                } catch (Exception ignored) {
                    logger.warn("Skipped malformed player entry in data.yml: {}", entry.getKey());
                }
            }

            dirty = false;
            logger.info("angelloNotifier data loaded from {}", dataPath.toAbsolutePath());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load data.yml", ex);
        }
    }

    void ensureDataExists() throws IOException {
        if (Files.exists(dataPath)) {
            return;
        }
        Files.createDirectories(dataPath.getParent());
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("data.yml")) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing packaged resource data.yml");
            }
            Files.copy(inputStream, dataPath);
        }
    }

    static Map<String, Object> castMap(Map<?, ?> input) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    static long longValue(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

    static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return castMap(map);
        }
        return new LinkedHashMap<>();
    }

    static String string(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object object : list) {
            result.add(String.valueOf(object));
        }
        return result;
    }

    static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

    static boolean bool(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    public synchronized DataStore store() {
        return store;
    }

    public synchronized void markDirty() {
        dirty = true;
    }

    public synchronized void saveIfDirty() {
        if (!dirty) {
            return;
        }
        saveNow();
    }

    public synchronized void saveNow() {
        try {
            Files.createDirectories(dataPath.getParent());
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("next-notification-id", store.nextNotificationId());

            List<Map<String, Object>> notifications = new ArrayList<>();
            for (Notification notification : store.notifications()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", notification.id());
                entry.put("title", notification.title());
                entry.put("body", notification.body());
                entry.put("created-by", notification.createdBy());
                entry.put("created-at", notification.createdAtEpochMs());
                entry.put("lifetime", notification.lifetime());
                notifications.add(entry);
            }
            root.put("notifications", notifications);

            Map<String, Object> players = new LinkedHashMap<>();
            for (Map.Entry<UUID, PlayerMailbox> playerEntry : store.playerMailboxes().entrySet()) {
                PlayerMailbox mailbox = playerEntry.getValue();
                Map<String, Object> playerMap = new LinkedHashMap<>();
                playerMap.put("last-known-name", mailbox.lastKnownName());

                Map<String, Object> states = new LinkedHashMap<>();
                for (Map.Entry<Long, PlayerNotificationState> stateEntry : mailbox.states().entrySet()) {
                    PlayerNotificationState state = stateEntry.getValue();
                    Map<String, Object> stateMap = new LinkedHashMap<>();
                    stateMap.put("preview-count", state.previewCount());
                    stateMap.put("read", state.read());
                    stateMap.put("first-seen-at", state.firstSeenAtEpochMs());
                    stateMap.put("read-at", state.readAtEpochMs());
                    states.put(String.valueOf(stateEntry.getKey()), stateMap);
                }
                playerMap.put("states", states);
                players.put(playerEntry.getKey().toString(), playerMap);
            }
            root.put("players", players);

            try (Writer writer = Files.newBufferedWriter(dataPath, StandardCharsets.UTF_8)) {
                new Yaml().dump(root, writer);
            }
            dirty = false;
        } catch (Exception ex) {
            logger.error("Failed to save data.yml", ex);
        }
    }

}
