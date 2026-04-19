package me.tyyni.yoChat.yoChatPlugin.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yoChatPlugin.MuteManager;
import me.tyyni.yoChat.yochatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {
    private final Pattern blockedPattern;

    {
        blockedPattern = Pattern.compile("\\b(" + String.join("|", ConfigManager.getInstance().getBlockedwords()) + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        event.setCancelled(true);

        if (ConfigManager.getInstance().isUseMuteMessage()) {
            if (MuteManager.isMuted(player)) {
                Component formattedMuteMessage = YoChatAPI.getInstance().getChatManager().formatMuteMessage(ConfigManager.getInstance().getMuteMessage(), player);
                player.sendMessage(formattedMuteMessage);
                return;
            }
        }

        if (ConfigManager.getInstance().isModerationEnabled()) {

            String plainText = PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).toLowerCase(Locale.ROOT);
            Matcher matcher = blockedPattern.matcher(plainText);

            if (matcher.find()) {
                String blockedword = matcher.group();
                Component formattedBadWordMessage = YoChatAPI.getInstance().getChatManager().formatMessage(ConfigManager.getInstance().getBlockedWordMessage(), player, blockedword);
                player.sendMessage(formattedBadWordMessage);
                return;
            }
        }

        Component formattedMessage = YoChatAPI.getInstance().getChatManager().formatMessage(player, event.originalMessage());

        if (!ConfigManager.getInstance().isUseChannelSystem()) {
            YoChatAPI.sendGlobalMessage(formattedMessage);
            return;
        }

        ChatChannel channel = YoChatAPI.getInstance().getChannelManager().getChannelByPlayer(player);

        if (channel == null) {
            player.sendMessage(YoChatAPI.getInstance().getYoChatPrefix().append(
                    Component.text("You don't belong to any channels! Please contact administrators.", NamedTextColor.RED)));
            return;
        }

        YoChatAPI.getInstance().getChannelManager().sendToChannel(channel, player, event.originalMessage());

    }
}