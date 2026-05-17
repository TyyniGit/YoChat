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

/**
 * Mutable state for a single chat message as it moves through YoChat's pipeline.
 *
 * <p>Pipeline steps and API consumers may read and update this object to alter
 * how a message is validated, formatted, and delivered.</p>
 */
public class ChatContext {
    /** The player who sent the message. */
    @Setter
    @Getter
    private Player sender;

    /** The sender display name captured for the current message. */
    @Getter
    @Setter
    private Component senderDisplayName;

    /** The unmodified plain-text message captured from the chat event. */
    @Getter
    @Setter
    private String rawMessage;

    /** The mutable string form after preprocessing steps have run. */
    @Getter
    @Setter
    private String processedMessage;

    /** The final rendered component, if one has already been produced. */
    @Getter
    @Setter
    private Component component;

    /** The resolved chat channel for the message, if channel handling is in use. */
    @Getter
    @Setter
    @Nullable
    private ChatChannel channel;

    /** Whether further processing and delivery should stop. */
    @Getter
    @Setter
    private boolean cancelled;

    /** The currently resolved audience set for the message. */
    @Getter
    @Setter
    private Set<Audience> viewers = new LinkedHashSet<>();
}
