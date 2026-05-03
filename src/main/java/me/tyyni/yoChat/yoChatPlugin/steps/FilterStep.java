package me.tyyni.yoChat.yoChatPlugin.steps;

import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import me.tyyni.yoChat.yoChatPlugin.ChatManager;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;

public class FilterStep implements ChatPipelineStep {

    @Override
    public void process(ChatContext context) {
        if (context.isCancelled()) return;

        ChatManager chatManager = YoChatAPI.getPlugin().getChatManager();
        ConfigManager config = ConfigManager.getInstance();
        Player player = context.getSender();
        String message = context.getProcessedMessage();

        if (!config.isModerationEnabled() || message == null || message.isBlank()) {
            return;
        }

        String blockedword = containsBlacklistedWords(message);
        if (!blockedword.isEmpty()) {
            context.setCancelled(true);
            config.debug("Blocked word detected from %s: '%s'", player.getName(), blockedword);
            Component formattedBadWordMessage = chatManager.formatMessage(ConfigManager.getInstance().getBlockedWordMessage(), player, blockedword);
            player.sendMessage(formattedBadWordMessage);
        }
    }

    private String containsBlacklistedWords(String message) {
        ChatManager chatManager = YoChatAPI.getPlugin().getChatManager();

        Matcher matcher = chatManager.getBlockedPattern().matcher(message.toLowerCase());
        if (matcher.find()) {
            return matcher.group();
        }

        return StringUtils.EMPTY;
    }
}
