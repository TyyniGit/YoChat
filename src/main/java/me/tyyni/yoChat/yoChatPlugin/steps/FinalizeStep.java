package me.tyyni.yoChat.yoChatPlugin.steps;

import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import me.tyyni.yoChat.yoChatPlugin.ReplyManager;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

public class FinalizeStep implements ChatPipelineStep {

    @Override
    public void process(@NonNull ChatContext context) {
        ReplyManager replyManager = ReplyManager.getInstance();

        if(context.isCancelled()) return;

        if(context.getComponent() == null) {
            String message = context.getRawMessage();
            if(message != null) message = context.getRawMessage();

            context.setComponent(Component.text(message != null ? message : ""));
        }
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }
}
