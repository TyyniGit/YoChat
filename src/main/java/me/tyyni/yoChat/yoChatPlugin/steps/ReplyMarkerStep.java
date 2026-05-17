package me.tyyni.yoChat.yoChatPlugin.steps;

import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yoChatPlugin.ReplyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class ReplyMarkerStep implements ChatPipelineStep {

    @Override
    public void process(ChatContext context) {
        if (context.isCancelled()) return;

        ReplyManager replyManager = ReplyManager.getInstance();
        ConfigManager config = ConfigManager.getInstance();
        UUID senderUuid = context.getSender().getUniqueId();
        Component currentComponent = context.getComponent();
        UUID messageUuid = UUID.randomUUID();

        replyManager.saveLastMessage(senderUuid, messageUuid,  currentComponent);

        if (config.isUseReplying()) {
            String command = "/_yochat_reply " + senderUuid + " " + messageUuid;

            Component replyButton = Component.text(config.getReplyButtonIcon())
                .color(NamedTextColor.GRAY)
                .hoverEvent(HoverEvent.showText(YoChatAPI.getPlugin().getMessageParseManager().parseAdmin(ConfigManager.getInstance().getReplyHoverText())
                .clickEvent(ClickEvent.runCommand(command))));

            context.setComponent(currentComponent.append(replyButton));
        }
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }
}
