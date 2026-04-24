package me.tyyni.yoChat.yoChatPlugin.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yoChatPlugin.MuteManager;
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Locale;
import java.util.regex.Matcher;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String plainText = PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).toLowerCase(Locale.ROOT);

        event.setCancelled(true);
        if (ConfigManager.getInstance().isUseMutedMessage()) {
            if (MuteManager.isMuted(player)) {
                Component formattedMuteMessage = YoChatAPI.getPlugin().getChatManager().formatMuteMessage(ConfigManager.getInstance().getMutedMessage(), player);
                player.sendMessage(formattedMuteMessage);
                return;
            }
        }

        if (ConfigManager.getInstance().isModerationEnabled()) {

            Matcher matcher = YoChatAPI.getPlugin().getChatManager().getBlockedPattern().matcher(plainText);

            if (matcher.find()) {
                String blockedword = matcher.group();
                Component formattedBadWordMessage = YoChatAPI.getPlugin().getChatManager().formatMessage(ConfigManager.getInstance().getBlockedWordMessage(), player, blockedword);
                player.sendMessage(formattedBadWordMessage);
                return;
            }
        }

        if (!ConfigManager.getInstance().isUseChannelSystem()) {
            YoChatAPI.sendGlobalMessage(event.originalMessage(), player);
            return;
        }

        ChatChannel channel = YoChatAPI.getPlugin().getChannelManager().getChannelByPlayer(player);

        if (channel == null) {
            player.sendMessage(YoChatAPI.getPlugin().getYoChatPrefix().append(
                    Component.text("You don't belong to any channels! Please contact administrators.", NamedTextColor.RED)));
            return;
        }

        YoChatAPI.getPlugin().getChannelManager().sendToChannel(channel, player, event.originalMessage());
    }
}