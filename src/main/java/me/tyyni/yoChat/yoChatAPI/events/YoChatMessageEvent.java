package me.tyyni.yoChat.yoChatAPI.events;

import lombok.Getter;
import lombok.Setter;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after the pipeline has produced the message component but before final send handling.
 *
 * <p>This event is asynchronous and may be cancelled.</p>
 */
public class YoChatMessageEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final Player sender;
    @Getter @Setter
    private Component message;
    @Getter @Setter
    private ChatChannel channel;
    private boolean cancelled;

    /**
     * Creates a new message event.
     *
     * @param sender the player who sent the message
     * @param message the current message component
     * @param channel the resolved channel, if any
     */
    public YoChatMessageEvent(Player sender, Component message, ChatChannel channel) {
        super(true); // Async
        this.sender = sender;
        this.message = message;
        this.channel = channel;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
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
