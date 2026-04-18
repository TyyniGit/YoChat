package me.tyyni.yoChat.yoChatPlugin.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yochatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        event.setCancelled(true);

        Component formattedMessage = YoChatAPI.getInstance().getChatManager().formatMessage(player, event.originalMessage());

        if(!ConfigManager.getInstance().isUseChannelSystem()) {
            YoChatAPI.getInstance().getChatManager().broadcast(formattedMessage);
            return;
        }

        ChatChannel channel = YoChatAPI.getInstance().getChannelManager().getChannelByPlayer(player);

        if(channel == null) {
            player.sendMessage(YoChatAPI.getInstance().getYoChatPrefix().append(
                    Component.text("You don't belong to any channels! Please contact administrators.", NamedTextColor.RED)));
            return;
        }

        YoChatAPI.getInstance().getChannelManager().sendToChannel(channel, player, event.originalMessage());
    }
}
