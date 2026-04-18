package me.tyyni.yoChat.yoChatPlugin.listeners;

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
        if (ConfigManager.getInstance().isUseChannelSystem()) {

            ChatChannel defaultChannel = ConfigManager.getInstance().getDefaultChannel();
            if (defaultChannel != null && defaultChannel.canJoin(player)) {
                defaultChannel.addMember(player);
            }
        }
    }
}
