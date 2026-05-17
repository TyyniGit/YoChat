package me.tyyni.yoChat.yoChatPlugin.listeners;

import me.tyyni.yoChat.yoChatPlugin.ReplyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;

public class ReplyCommandListener implements Listener {

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (!message.startsWith("/_yochat_reply")) return;

        try {
            String uuidPart = message.substring("/_yochat_reply ".length());
            UUID targetUUID = UUID.fromString(uuidPart);

            var substring = message.substring("/_yochat_reply ".length() + uuidPart.length() + 1);
            UUID messageUUID = substring.isEmpty() ? null : UUID.fromString(substring);
            ReplyManager.getInstance().handleReply(event.getPlayer(), targetUUID, messageUUID);
        } catch (Exception ignored) {}
    }
}
