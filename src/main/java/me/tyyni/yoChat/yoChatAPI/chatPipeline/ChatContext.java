package me.tyyni.yoChat.yoChatAPI.chatPipeline;

import lombok.Getter;
import lombok.Setter;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public class ChatContext {
    @Setter
    @Getter
    private Player sender;

    @Getter
    @Setter
    private Component senderDisplayName;

    @Getter
    @Setter
    private String rawMessage;

    @Getter
    @Setter
    private String processedMessage;

    @Getter
    @Setter
    private Component component;

    @Getter
    @Setter
    @Nullable
    private ChatChannel channel;

    @Getter
    @Setter
    private boolean cancelled;

    @Getter
    @Setter
    private Set<Audience> viewers = new LinkedHashSet<>();
}
