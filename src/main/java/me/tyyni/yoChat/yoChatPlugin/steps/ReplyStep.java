package me.tyyni.yoChat.yoChatPlugin.steps;

import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import me.tyyni.yoChat.yoChatPlugin.ReplyManager;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class ReplyStep implements ChatPipelineStep {

    @Override
    public void process(@NonNull ChatContext context) {
        if (context.isCancelled()) return;

        ReplyManager replyManager = ReplyManager.getInstance();
        UUID playerUuid = context.getSender().getUniqueId();

        Component targetMessage = replyManager.getPendingReply(playerUuid);

        if (targetMessage != null) {
            replyManager.clearPendingReply(playerUuid);

            Component replyContent = context.getComponent();
            if (replyContent == null) {
                String raw = context.getRawMessage();
                replyContent = Component.text(raw != null ? raw : "");
            }

            Component formattedReply = replyManager.reply(targetMessage, replyContent).join();

            context.setComponent(formattedReply);
        }
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }
}
