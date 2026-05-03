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

    public YoChatPipelineEvent(Player sender, Component message, ChatChannel channel, ChatContext context) {
        super(true); // Async
        this.sender = sender;
        this.message = message;
        this.channel = channel;
        this.context = context;
    }

    public void setMessage(Component message) {
        this.message = message;
        context.setComponent(message);
    }

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

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
