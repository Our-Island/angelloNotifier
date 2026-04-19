package top.ourisland.angellonotifier.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import top.ourisland.angellonotifier.service.NotificationService;

public class PlayerConnectionListener {

    NotificationService notificationService;

    public PlayerConnectionListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Subscribe
    public void onPostConnect(ServerPostConnectEvent event) {
        notificationService.handlePlayerConnectedToServer(event.getPlayer());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        notificationService.handlePlayerDisconnected(event.getPlayer());
    }

}
