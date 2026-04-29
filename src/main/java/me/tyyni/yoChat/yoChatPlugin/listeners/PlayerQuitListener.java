package me.tyyni.yoChat.yoChatPlugin.listeners;

import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatAPI.events.YoChatChannelLeaveEvent;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import org.bukkit.Bukkit;
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
            YoChatChannelLeaveEvent leaveEvent = new YoChatChannelLeaveEvent(player, channel);
            Bukkit.getPluginManager().callEvent(leaveEvent);

            channel.removeMember(player);
            me.tyyni.yoChat.yoChatPlugin.ConfigManager.getInstance().debug("Removed %s from channel %s on quit", player.getName(), channel.getName());
        } else {
            me.tyyni.yoChat.yoChatPlugin.ConfigManager.getInstance().debug("Player %s quit without an active channel", player.getName());
        }
    }
}
