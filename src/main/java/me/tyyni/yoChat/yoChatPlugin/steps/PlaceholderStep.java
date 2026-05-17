package me.tyyni.yoChat.yoChatPlugin.steps;

import me.clip.placeholderapi.PlaceholderAPI;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class PlaceholderStep implements ChatPipelineStep {

    @Override
    public void process(@NonNull ChatContext context) {
        Player player = context.getSender();

        if (context.isCancelled()) return;

        String message = context.getProcessedMessage();

        if (message == null) {
            message = context.getRawMessage();
        }

        if (ConfigManager.getInstance().isUsePlaceholderAPI() && org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            ConfigManager.getInstance().debug("Applying PlaceholderAPI placeholders for %s", player.getName());
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        context.setProcessedMessage(message);
    }
}
