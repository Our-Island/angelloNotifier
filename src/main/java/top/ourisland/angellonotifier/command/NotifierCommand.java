package top.ourisland.angellonotifier.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import top.ourisland.angellonotifier.service.NotificationService;
import top.ourisland.angellonotifier.util.I18n;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class NotifierCommand implements SimpleCommand {

    static final String ADMIN_PERMISSION = "angello.admin";
    static final String USE_PERMISSION = "angello.use";

    NotificationService notificationService;

    public NotifierCommand(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            notificationService.sendHelp(source);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "inbox", "mailbox" -> handleInbox(source, args);
            case "show", "view", "open" -> handleShow(source, args);
            case "unread" -> handleUnread(source);
            case "send", "create", "set" -> handleSend(source, args);
            case "list" -> handleList(source, args);
            case "delete", "del", "remove" -> handleDelete(source, args);
            case "reload" -> handleReload(source);
            default -> notificationService.sendHelp(source);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length <= 1) {
            List<String> suggestions = new ArrayList<>(List.of("help", "inbox", "show", "unread"));
            if (invocation.source().hasPermission(ADMIN_PERMISSION)) {
                suggestions.addAll(List.of("send", "list", "delete", "reload"));
            }
            return filterByPrefix(suggestions, args.length == 0 ? "" : args[0]);
        }

        if ((args[0].equalsIgnoreCase("inbox") || args[0].equalsIgnoreCase("list")) && args.length == 2) {
            return List.of("1", "2", "3", "4");
        }

        if ((args[0].equalsIgnoreCase("show") || args[0].equalsIgnoreCase("delete")
                || args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("remove")) && args.length == 2) {
            return notificationService.notificationsDescending().stream()
                    .limit(20)
                    .map(notification -> String.valueOf(notification.id()))
                    .toList();
        }

        return List.of();
    }

    static List<String> filterByPrefix(List<String> candidates, String prefix) {
        String lowered = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(lowered))
                .distinct()
                .toList();
    }

    void handleInbox(CommandSource source, String[] args) {
        if (!(source instanceof Player player)) {
            source.sendRichMessage(I18n.lang("command.only-player-inbox"));
            return;
        }
        int page = parsePage(args, 1);
        notificationService.sendInboxPage(source, player.getUniqueId(), page);
    }

    void handleShow(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendRichMessage(I18n.lang("command.usage.show"));
            return;
        }
        try {
            long id = Long.parseLong(args[1]);
            notificationService.showNotificationToSource(source, id, true);
        } catch (NumberFormatException _) {
            source.sendRichMessage(I18n.lang("command.id-must-number"));
        }
    }

    void handleUnread(CommandSource source) {
        if (!(source instanceof Player player)) {
            source.sendRichMessage(I18n.lang("command.only-player-unread"));
            return;
        }
        int unread = notificationService.unreadCount(player.getUniqueId());
        source.sendRichMessage(I18n.lang("command.unread-count", unread));
    }

    void handleSend(CommandSource source, String[] args) {
        if (!source.hasPermission(ADMIN_PERMISSION)) {
            source.sendRichMessage(I18n.lang("command.no-permission-send"));
            return;
        }
        if (args.length < 2) {
            source.sendRichMessage(I18n.lang("command.usage.send"));
            return;
        }

        String payload = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String[] parts = payload.split("\\s*\\|\\|\\s*", 3);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            source.sendRichMessage(I18n.lang("command.usage.send"));
            return;
        }

        Integer lifetime = null;
        if (parts.length >= 3 && !parts[2].isBlank()) {
            try {
                lifetime = Integer.parseInt(parts[2].trim());
                if (lifetime <= 0) {
                    source.sendRichMessage(I18n.lang("command.lifetime-must-positive"));
                    return;
                }
            } catch (NumberFormatException _) {
                source.sendRichMessage(I18n.lang("command.lifetime-must-positive"));
                return;
            }
        }

        String title = parts[0].trim();
        List<String> bodyLines = Arrays.stream(parts[1].split("\\\\n|\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
        if (bodyLines.isEmpty()) {
            source.sendRichMessage(I18n.lang("command.body-empty"));
            return;
        }

        String createdBy = source instanceof Player player ? player.getUsername() : "CONSOLE";
        var notification = notificationService.createNotification(
                createdBy,
                title,
                bodyLines,
                lifetime
        );
        notificationService.broadcastNotification(notification);
        source.sendRichMessage(I18n.lang("command.sent", notification.id()));
    }

    void handleList(CommandSource source, String[] args) {
        if (!source.hasPermission(ADMIN_PERMISSION)) {
            source.sendRichMessage(I18n.lang("command.no-permission-list"));
            return;
        }
        int page = parsePage(args, 1);
        notificationService.sendAdminListPage(source, page);
    }

    void handleDelete(CommandSource source, String[] args) {
        if (!source.hasPermission(ADMIN_PERMISSION)) {
            source.sendRichMessage(I18n.lang("command.no-permission-delete"));
            return;
        }
        if (args.length < 2) {
            source.sendRichMessage(I18n.lang("command.usage.delete"));
            return;
        }
        try {
            long id = Long.parseLong(args[1]);
            boolean deleted = notificationService.deleteNotification(id);
            if (!deleted) {
                source.sendRichMessage(I18n.lang("notification.not-found", id));
                return;
            }
            source.sendRichMessage(I18n.lang("command.deleted", id));
        } catch (NumberFormatException _) {
            source.sendRichMessage(I18n.lang("command.id-must-number"));
        }
    }

    void handleReload(CommandSource source) {
        if (!source.hasPermission(ADMIN_PERMISSION)) {
            source.sendRichMessage(I18n.lang("command.no-permission-reload"));
            return;
        }
        notificationService.reloadConfig();
        source.sendRichMessage(I18n.lang("command.reloaded"));
    }

    static int parsePage(String[] args, int fallback) {
        if (args.length < 2) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(args[1]));
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

}
