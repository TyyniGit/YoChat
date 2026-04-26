package me.tyyni.yoChat.yoChatPlugin.listeners;

import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ChatChannel channel = YoChatAPI.getChannelByPlayer(player);
        if (channel != null) {
            channel.removeMember(player);
        }
    }
}
