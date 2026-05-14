package me.tyyni.yoChat.yoChatAPI.events;

import lombok.Getter;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired immediately after YoChat's internal pipeline has executed.
 *
 * <p>This event exposes the full {@link ChatContext} so listeners can inspect
 * or modify the pipeline output before the send stage continues.</p>
 */
public class YoChatPipelineEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final Player sender;
    @Getter
    private Component message;
    @Getter
    private ChatChannel channel;
    @Getter
    private final ChatContext context;
    private boolean cancelled;

    /**
     * Creates a new pipeline event.
     *
     * @param sender the player who sent the message
     * @param message the current rendered message component
     * @param channel the resolved channel, if any
     * @param context the mutable pipeline context
     */
    public YoChatPipelineEvent(Player sender, Component message, ChatChannel channel, ChatContext context) {
        super(true); // Async
        this.sender = sender;
        this.message = message;
        this.channel = channel;
        this.context = context;
    }

    /**
     * Replaces the current message component and mirrors the change back into the context.
     *
     * @param message the replacement component
     */
    public void setMessage(Component message) {
        this.message = message;
        context.setComponent(message);
    }

    /**
     * Replaces the current channel and mirrors the change back into the context.
     *
     * @param channel the replacement channel
     */
    public void setChannel(ChatChannel channel) {
        this.channel = channel;
        context.setChannel(channel);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Returns the static Bukkit handler list for this event type.
     *
     * @return the handler list
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
