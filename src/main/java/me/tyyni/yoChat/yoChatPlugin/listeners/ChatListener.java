package me.tyyni.yoChat.yoChatPlugin.listeners;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
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
        ChatRenderer originalRenderer = event.renderer();
        event.setCancelled(true);

        Component formattedMessage = YoChatAPI.getInstance().getChatManager().formatMessage(player, event.originalMessage());

        ChatChannel channel = YoChatAPI.getInstance().getChannelManager().getChannelByPlayer(player);

        if(channel == null) {
            player.sendMessage(YoChatAPI.getInstance().getYoChatPrefix().append(
                    Component.text("Et kuulu mihinkään kanavaan! Ota yhteyttä ylläpitoon.", NamedTextColor.RED)));
            return;
        }

        YoChatAPI.getInstance().getChannelManager().sendToChannel(channel, player, );
    }
}
