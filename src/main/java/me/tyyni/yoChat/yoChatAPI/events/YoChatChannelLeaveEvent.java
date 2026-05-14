package me.tyyni.yoChat.yoChatAPI.events;

import lombok.Getter;
import lombok.Setter;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before a player is removed from a YoChat channel.
 *
 * <p>Cancelling this event prevents the leave from happening.</p>
 */
public class YoChatChannelLeaveEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final Player player;
    @Getter @Setter
    private ChatChannel channel;
    private boolean cancelled;

    /**
     * Creates a new channel leave event.
     *
     * @param player the player leaving the channel
     * @param channel the channel being left
     */
    public YoChatChannelLeaveEvent(Player player, ChatChannel channel) {
        this.player = player;
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
