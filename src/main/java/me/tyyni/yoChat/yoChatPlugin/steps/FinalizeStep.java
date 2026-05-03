package me.tyyni.yoChat.yoChatPlugin.steps;

import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import net.kyori.adventure.text.Component;

public class FinalizeStep implements ChatPipelineStep {

    @Override
    public void process(ChatContext context) {
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
