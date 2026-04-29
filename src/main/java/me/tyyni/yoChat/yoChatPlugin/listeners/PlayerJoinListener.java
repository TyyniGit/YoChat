package me.tyyni.yoChat.yoChatPlugin.listeners;

import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ConfigManager config = ConfigManager.getInstance();
        if (config.isUseChannelSystem()) {

            ChatChannel defaultChannel = config.getDefaultChannel();
            if (defaultChannel != null && defaultChannel.canJoin(player)) {
                YoChatAPI.getPlugin().getChannelManager().joinChannel(player, defaultChannel);
                config.debug("Auto-joined %s to default channel %s", player.getName(), defaultChannel.getName());
            } else {
                config.debug("Did not auto-join %s to default channel (channel=%s, canJoin=%s)",
                        player.getName(),
                        defaultChannel != null ? defaultChannel.getName() : "null",
                        defaultChannel != null && defaultChannel.canJoin(player));
            }
        }
    }
}
