package me.tyyni.yoChat.yoChatPlugin.steps;

import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MentionStep implements ChatPipelineStep {

    @Override
    public void process(ChatContext context) {
        if (context.isCancelled()) return;

        Component component = context.getComponent();
        ConfigManager config = ConfigManager.getInstance();

        if (component == null) return;

        String text = PlainTextComponentSerializer.plainText().serialize(component);

        context.getViewers().forEach(viewer -> {
            if(!(viewer instanceof Player mentioned)) return;

            if(!ConfigManager.getInstance().isUseMentioning()) return;

            if(YoChatAPI.getPlugin().getChatManager().containsName(mentioned, text)) {
                if (config.isUseSound()) {
                    Bukkit.getScheduler().runTask(YoChatAPI.getPlugin(), () ->
                        mentioned.playSound(mentioned, config.getSound(), config.getSoundVolume(), config.getSoundPitch())
                    );
                }
            }
        });
    }
}
