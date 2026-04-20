package top.ourisland.angellonotifier.service;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.slf4j.Logger;
import top.ourisland.angellonotifier.AngelloNotifier;
import top.ourisland.angellonotifier.config.ConfigManager;
import top.ourisland.angellonotifier.model.*;
import top.ourisland.angellonotifier.storage.DataManager;
import top.ourisland.angellonotifier.storage.DataStore;
import top.ourisland.angellonotifier.util.I18n;
import top.ourisland.angellonotifier.util.NotificationImportUtils;
import top.ourisland.angellonotifier.util.TimeUtils;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NotificationService {

    AngelloNotifier plugin;
    ProxyServer server;
    Logger logger;
    ConfigManager configManager;
    DataManager dataManager;
    Map<UUID, SessionState> sessionStates = new ConcurrentHashMap<>();

    ScheduledTask autoSaveTask;

    public NotificationService(
            AngelloNotifier plugin,
            ProxyServer server,
            Logger logger,
            ConfigManager configManager,
            DataManager dataManager
    ) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.configManager = configManager;
        this.dataManager = dataManager;
    }

    public void startAutoSaveTask() {
        autoSaveTask = server.getScheduler()
                .buildTask(plugin, dataManager::saveIfDirty)
                .repeat(5L, TimeUnit.SECONDS)
                .schedule();
    }

    public void stopAutoSaveTask() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }

    public synchronized void reloadConfig() {
        configManager.reload();
        I18n.reload();
    }

    public synchronized Notification createNotification(
            String createdBy,
            String title,
            List<String> bodyLines,
            Integer lifetime
    ) {
        return createNotification(createdBy, title, bodyLines, lifetime, null);
    }

    public synchronized Notification createNotification(
            String createdBy,
            String title,
            List<String> bodyLines,
            Integer lifetime,
            Long createdAtEpochMs
    ) {
        DataStore store = dataManager.store();
        long id = store.nextNotificationId();
        store.nextNotificationId(id + 1L);

        int resolvedLifetime = resolveLifetime(lifetime);
        long resolvedCreatedAt = createdAtEpochMs != null ? createdAtEpochMs : System.currentTimeMillis();
        var notification = new Notification(
                id,
                title,
                List.copyOf(bodyLines),
                createdBy,
                resolvedCreatedAt,
                resolvedLifetime
        );
        store.notifications().add(notification);
        dataManager.markDirty();
        logger.info("Notification #{} created by {}", id, createdBy);
        return notification;
    }

    int resolveLifetime(Integer configuredValue) {
        if (configuredValue != null && configuredValue > 0) {
            return configuredValue;
        }
        return Math.max(1, configManager.config().defaultNotificationLifetime());
    }

    public void importNotificationFromUri(CommandSource source, String requesterName, String rawUri) {
        URI uri = normalizeUri(rawUri);
        source.sendRichMessage(I18n.lang("command.import.started", escapeMini(uri.toString())));

        server.getScheduler().buildTask(plugin, () -> {
            try {
                ImportedNotification importedNotification = NotificationImportUtils.loadFromUri(uri);
                String createdBy = importedNotification.createdBy();
                if (createdBy == null || createdBy.isBlank()) {
                    createdBy = requesterName;
                }

                Notification notification = createNotification(
                        createdBy,
                        importedNotification.title(),
                        importedNotification.body(),
                        importedNotification.lifetime(),
                        importedNotification.createdAtEpochMs()
                );
                broadcastNotification(notification);
                source.sendRichMessage(I18n.lang("command.import.success", notification.id(), escapeMini(uri.toString())));
            } catch (Exception ex) {
                logger.warn("Failed to import notification from URI {}", uri, ex);
                String reason = ex.getMessage();
                if (reason == null || reason.isBlank()) {
                    reason = "Unknown error.";
                }
                source.sendRichMessage(I18n.lang("command.import.failed", escapeMini(reason)));
            }
        }).schedule();
    }

    URI normalizeUri(String rawUri) {
        try {
            URI uri = URI.create(rawUri);
            if (uri.getScheme() != null && !uri.getScheme().isBlank()) {
                return uri;
            }
        } catch (Exception _) {
        }

        try {
            return java.nio.file.Path.of(rawUri).toUri();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid URI", ex);
        }
    }

    static String escapeMini(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("<", "\\<")
                .replace(">", "\\>");
    }

    public synchronized void broadcastNotification(Notification notification) {
        for (Player player : server.getAllPlayers()) {
            if (isIgnoredServer(player)) {
                continue;
            }
            previewNotification(player, notification, true);
        }
    }

    public boolean isIgnoredServer(Player player) {
        var config = configManager.config();
        String currentServer = player.getCurrentServer()
                .map(serverConnection -> serverConnection.getServerInfo().getName())
                .orElse("")
                .toLowerCase(Locale.ROOT);
        return config.ignoredServers().contains(currentServer);
    }

    synchronized void previewNotification(Player player, Notification notification, boolean immediateSend) {
        touchPlayer(player);
        var state = stateFor(player.getUniqueId(), notification.id());
        if (state.read()) {
            return;
        }
        if (state.firstSeenAtEpochMs() == 0L) {
            state.firstSeenAtEpochMs(System.currentTimeMillis());
        }
        state.previewCount(state.previewCount() + 1);
        printNotification(player, notification);
        if (configManager.config().playSound()) {
            playNoticeSound(player);
        }
        if (state.previewCount() >= effectiveLifetime(notification)) {
            state.read(true);
            state.readAtEpochMs(System.currentTimeMillis());
        }
        dataManager.markDirty();
    }

    public synchronized void touchPlayer(Player player) {
        var mailbox = mailboxFor(player.getUniqueId());
        if (!player.getUsername().equals(mailbox.lastKnownName())) {
            mailbox.lastKnownName(player.getUsername());
            dataManager.markDirty();
        }
    }

    synchronized PlayerNotificationState stateFor(UUID playerId, long notificationId) {
        var mailbox = mailboxFor(playerId);
        return mailbox.states().computeIfAbsent(notificationId, ignored -> new PlayerNotificationState());
    }

    void printNotification(CommandSource source, Notification notification) {
        List<String> lines = new ArrayList<>();
        lines.add(I18n.langNP("notification.separator"));
        lines.add(I18n.langNP("notification.title-line", notification.id(), notification.title()));
        lines.add(I18n.langNP(
                "notification.meta-line",
                escapeMini(notification.createdBy()),
                TimeUtils.format(notification.createdAtEpochMs())
        ));
        lines.addAll(notification.body());
        lines.add(I18n.langNP("notification.separator"));
        source.sendRichMessage(I18n.block(lines));
    }

    void playNoticeSound(Player player) {
        try {
            var config = configManager.config();
            var sound = Sound.sound(
                    Key.key(config.soundName()),
                    Sound.Source.MASTER,
                    config.soundVolume(),
                    config.soundPitch()
            );
            player.playSound(sound);
        } catch (Exception ex) {
            logger.debug("Failed to play sound to {}", player.getUsername(), ex);
        }
    }

    int effectiveLifetime(Notification notification) {
        if (notification.lifetime() > 0) {
            return notification.lifetime();
        }
        return Math.max(1, configManager.config().defaultNotificationLifetime());
    }

    synchronized PlayerMailbox mailboxFor(UUID playerId) {
        return dataManager.store().playerMailboxes()
                .computeIfAbsent(playerId, ignored -> new PlayerMailbox());
    }

    public synchronized int unreadCount(UUID playerId) {
        int unread = 0;
        for (Notification notification : dataManager.store().notifications()) {
            if (!stateFor(playerId, notification.id()).read()) {
                unread++;
            }
        }
        return unread;
    }

    public synchronized List<Notification> unreadNotifications(UUID playerId) {
        List<Notification> unread = new ArrayList<>();
        for (Notification notification : notificationsDescending()) {
            if (!stateFor(playerId, notification.id()).read()) {
                unread.add(notification);
            }
        }
        return unread;
    }

    public synchronized void previewUnreadOnEligibleJoin(Player player) {
        touchPlayer(player);
        var config = configManager.config();
        List<Notification> unread = unreadNotifications(player.getUniqueId());
        if (unread.isEmpty()) {
            return;
        }

        int limit = Math.min(config.joinPreviewBatchSize(), unread.size());
        for (int i = 0; i < limit; i++) {
            previewNotification(player, unread.get(i), false);
        }

        int remaining = unreadCount(player.getUniqueId());
        if (remaining > 0) {
            player.sendRichMessage(I18n.lang("join.remaining-unread", remaining));
        } else {
            player.sendRichMessage(I18n.lang("join.all-read"));
        }
    }

    public synchronized void handlePlayerConnectedToServer(Player player) {
        var sessionState = sessionStates.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new SessionState()
        );

        if (sessionState.joinProcessedOnEligibleServer()) {
            return;
        }
        if (isIgnoredServer(player)) {
            return;
        }

        sessionState.joinProcessedOnEligibleServer(true);
        previewUnreadOnEligibleJoin(player);
    }

    public void handlePlayerDisconnected(Player player) {
        sessionStates.remove(player.getUniqueId());
    }

    public synchronized void showNotificationToSource(
            CommandSource source,
            long notificationId,
            boolean markReadIfPlayer
    ) {
        Optional<Notification> optionalNotification = getNotification(notificationId);
        if (optionalNotification.isEmpty()) {
            source.sendRichMessage(I18n.lang("notification.not-found", notificationId));
            return;
        }

        var notification = optionalNotification.get();
        printNotification(source, notification);
        if (markReadIfPlayer && source instanceof Player player) {
            markRead(player, notification.id());
        }
    }

    public synchronized Optional<Notification> getNotification(long id) {
        return notificationsDescending().stream()
                .filter(notification -> notification.id() == id)
                .findFirst();
    }

    public synchronized void markRead(Player player, long notificationId) {
        touchPlayer(player);
        var state = stateFor(player.getUniqueId(), notificationId);
        state.read(true);
        if (state.firstSeenAtEpochMs() == 0L) {
            state.firstSeenAtEpochMs(System.currentTimeMillis());
        }
        state.readAtEpochMs(System.currentTimeMillis());
        dataManager.markDirty();
    }

    public synchronized List<Notification> notificationsDescending() {
        return dataManager.store().notifications().stream()
                .sorted(Comparator.comparingLong(Notification::id).reversed())
                .toList();
    }

    public synchronized boolean deleteNotification(long id) {
        boolean removed = dataManager.store().notifications().removeIf(notification -> notification.id() == id);
        if (!removed) {
            return false;
        }

        for (PlayerMailbox mailbox : dataManager.store().playerMailboxes().values()) {
            mailbox.states().remove(id);
        }

        dataManager.markDirty();
        logger.info("Notification #{} deleted", id);
        return true;
    }

    public synchronized void sendInboxPage(CommandSource source, UUID playerId, int requestedPage) {
        List<Notification> notifications = notificationsDescending();
        if (notifications.isEmpty()) {
            source.sendRichMessage(I18n.lang("inbox.empty"));
            return;
        }

        int pageSize = Math.max(1, configManager.config().inboxPageSize());
        int maxPage = Math.max(1, (int) Math.ceil(notifications.size() / (double) pageSize));
        int page = Math.clamp(requestedPage, 1, maxPage);
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, notifications.size());

        List<String> lines = new ArrayList<>();
        lines.add(I18n.langNP("inbox.header", page, maxPage));
        for (int i = fromIndex; i < toIndex; i++) {
            Notification notification = notifications.get(i);
            PlayerNotificationState state = stateFor(playerId, notification.id());
            String readMark = state.read() ? I18n.langNP("state.read") : I18n.langNP("state.unread");
            lines.add(I18n.langNP(
                    "inbox.entry",
                    readMark,
                    notification.id(),
                    notification.title(),
                    TimeUtils.format(notification.createdAtEpochMs())
            ));
        }
        lines.add(I18n.langNP("inbox.footer"));
        source.sendRichMessage(I18n.block(lines));
    }

    public synchronized void sendAdminListPage(CommandSource source, int requestedPage) {
        List<Notification> notifications = notificationsDescending();
        if (notifications.isEmpty()) {
            source.sendRichMessage(I18n.lang("inbox.empty"));
            return;
        }

        int pageSize = Math.max(1, configManager.config().inboxPageSize());
        int maxPage = Math.max(1, (int) Math.ceil(notifications.size() / (double) pageSize));
        int page = Math.clamp(requestedPage, 1, maxPage);
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, notifications.size());

        List<String> lines = new ArrayList<>();
        lines.add(I18n.langNP("admin.header", page, maxPage));
        for (int i = fromIndex; i < toIndex; i++) {
            Notification notification = notifications.get(i);
            lines.add(I18n.langNP(
                    "admin.entry",
                    notification.id(),
                    notification.title(),
                    escapeMini(notification.createdBy()),
                    TimeUtils.format(notification.createdAtEpochMs()),
                    effectiveLifetime(notification)
            ));
        }
        source.sendRichMessage(I18n.block(lines));
    }

    public synchronized void sendHelp(CommandSource source) {
        source.sendRichMessage(I18n.block(I18n.helpLines()));
    }

}
