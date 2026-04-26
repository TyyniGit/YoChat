package me.tyyni.yoChat.yoChatPlugin.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.tyyni.yoChat.yoChatPlugin.ChatManager;
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
import java.util.Set;
import java.util.regex.Matcher;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String plainText = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        String lowerCaseText = plainText.toLowerCase(Locale.ROOT);
        ChatManager chatManager = YoChatAPI.getPlugin().getChatManager();
        
        if (MuteManager.isMuted(player)) {
            event.setCancelled(true);
            if (ConfigManager.getInstance().isUseMutedMessage()) {
                Component formattedMuteMessage = chatManager.formatMuteMessage(ConfigManager.getInstance().getMutedMessage(), player);
                player.sendMessage(formattedMuteMessage);
            }
            return;
        }

        if (ConfigManager.getInstance().isModerationEnabled()) {
            Matcher matcher = chatManager.getBlockedPattern().matcher(lowerCaseText);
            if (matcher.find()) {
                event.setCancelled(true);
                String blockedword = matcher.group();
                Component formattedBadWordMessage = chatManager.formatMessage(ConfigManager.getInstance().getBlockedWordMessage(), player, blockedword);
                player.sendMessage(formattedBadWordMessage);
                return;
            }
        }

        ChatChannel channel = null;

        if (ConfigManager.getInstance().isUseChannelSystem()) {
            channel = YoChatAPI.getPlugin().getChannelManager().getChannelByPlayer(player);

            if (channel == null) {
                event.setCancelled(true);
                player.sendMessage(YoChatAPI.getPlugin().getYoChatPrefix().append(
                        Component.text("You don't belong to any channels! Please contact administrators.", NamedTextColor.RED)));
                return;
            }

            ChatChannel finalChannel = channel;
            event.viewers().removeIf(viewer -> {
                if (viewer instanceof Player targetPlayer) {
                    if (!finalChannel.getMembers().contains(targetPlayer)) return true;
                    if (!finalChannel.canJoin(targetPlayer)) return true;

                    Set<String> worlds = finalChannel.getWorlds();
                    if (worlds != null && !worlds.isEmpty()) {
                        if (finalChannel.isStrictWorld() && !worlds.contains(player.getWorld().getName())) return true;
                        if (!worlds.contains(targetPlayer.getWorld().getName())) return true;
                    }

                    if (finalChannel.getRadius() > 0) {
                        if (!targetPlayer.getWorld().equals(player.getWorld())) return true;
                        return targetPlayer.getLocation().distance(player.getLocation()) > finalChannel.getRadius();
                    }
                }
                return false;
            });
        }

        ChatChannel finalChannelForRenderer = channel;
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component finalParsedMessage = chatManager.applyMentionFormatting(
                    player,
                    viewer instanceof Player viewerPlayer ? viewerPlayer : null,
                    message
            );

            if (ConfigManager.getInstance().isUseChannelSystem() && finalChannelForRenderer != null) {
                return chatManager.formatChannelMessage(finalChannelForRenderer, player, finalParsedMessage);
            } else {
                return chatManager.formatMessage(player, finalParsedMessage);
            }
        });
    }
}
