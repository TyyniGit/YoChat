package me.tyyni.yoChat.yoChatPlugin.steps;

import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

public class FormatStep implements ChatPipelineStep {
    @Override
    public void process(@NonNull ChatContext context) {
        if (context.isCancelled()) return;

        String message = context.getProcessedMessage();
        if (message == null) {
            message = context.getRawMessage();
        }

        Component component = YoChatAPI.getPlugin().getMessageParseManager().parse(context.getSender(), message);
        context.setComponent(component);
    }
}
