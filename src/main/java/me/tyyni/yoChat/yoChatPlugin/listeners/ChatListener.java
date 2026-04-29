package me.tyyni.yoChat.yoChatPlugin.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.tyyni.yoChat.yoChatPlugin.ChatManager;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yoChatPlugin.MuteManager;
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yoChatAPI.events.YoChatMessageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
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
        ConfigManager config = ConfigManager.getInstance();

        config.debug("Incoming chat from %s in world=%s: '%s'", player.getName(), player.getWorld().getName(), plainText);

        if (MuteManager.isMuted(player)) {
            event.setCancelled(true);
            config.debug("Cancelled chat from muted player %s", player.getName());
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
                config.debug("Blocked word detected from %s: '%s'", player.getName(), blockedword);
                Component formattedBadWordMessage = chatManager.formatMessage(ConfigManager.getInstance().getBlockedWordMessage(), player, blockedword);
                player.sendMessage(formattedBadWordMessage);
                return;
            }
        }

        ChatChannel channel = null;

        if (ConfigManager.getInstance().isUseChannelSystem()) {
            channel = YoChatAPI.getPlugin().getChannelManager().getChannelByPlayer(player);
        }

        YoChatMessageEvent yoChatEvent = new YoChatMessageEvent(player, event.message(), channel);
        Bukkit.getPluginManager().callEvent(yoChatEvent);

        if (yoChatEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        channel = yoChatEvent.getChannel();
        event.message(yoChatEvent.getMessage());

        if (ConfigManager.getInstance().isUseChannelSystem()) {
            config.debug("Resolved channel for %s -> %s", player.getName(), channel != null ? channel.getName() : "null");

            if (channel == null) {
                event.setCancelled(true);
                config.debug("Cancelled chat because %s does not belong to a channel", player.getName());
                player.sendMessage(YoChatAPI.getPlugin().getChatManager().formatMessage(ConfigManager.getInstance().getNoChannelMessage(), player, ""));
                return;
            }

            ChatChannel finalChannel = channel;
            int initialViewerCount = event.viewers().size();
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
            config.debug("Filtered viewers for channel %s: %d -> %d", finalChannel.getName(), initialViewerCount, event.viewers().size());
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
