package me.tyyni.yoChat.yoChatPlugin.steps;

import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import me.tyyni.yoChat.yoChatPlugin.ChatManager;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yoChatPlugin.MuteManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class MuteStep implements ChatPipelineStep {

    @Override
    public void process(ChatContext context) {
        Player player = context.getSender();
        ChatManager chatManager = YoChatAPI.getPlugin().getChatManager();

        if (context.isCancelled()) return;

        if (MuteManager.isMuted(context.getSender())) {
            context.setCancelled(true);
            ConfigManager.getInstance().debug("Cancelled chat from muted player %s", player.getName());
            Component formattedMuteMessage = chatManager.formatMuteMessage(ConfigManager.getInstance().getMutedMessage(), player);
            player.sendMessage(formattedMuteMessage);
        }
    }
}
