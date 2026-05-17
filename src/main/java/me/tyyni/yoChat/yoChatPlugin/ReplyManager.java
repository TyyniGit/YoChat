package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the creation, storage, and formatting of chat replies within the plugin.
 */
public class ReplyManager {
    @Getter
    private static ReplyManager instance;

    /**
     * Map structure: Sender UUID -> (Message UUID -> Message Content).
     * Stores historical messages to allow players to reply to specific past messages.
     */
    private final Map<UUID, Map<UUID, Component>> lastMessages = new ConcurrentHashMap<>();

    /**
     * Map structure: Replier UUID -> Targeted Message UUID.
     * Stores the state of which player is currently replying to which specific message.
     */
    private final Map<UUID, UUID> pendingReplies = new ConcurrentHashMap<>();

    public ReplyManager() {
        instance = this;
    }

    /**
     * Saves a player's message to memory for future reply references.
     *
     * @param senderUUID  The UUID of the player who sent the message.
     * @param messageUUID The unique identifier for this specific message.
     * @param message     The component content of the message.
     */
    public void saveLastMessage(@Nullable UUID senderUUID, @Nullable UUID messageUUID, @Nullable Component message) {
        if (senderUUID == null || messageUUID == null || message == null) return;

        lastMessages.computeIfAbsent(senderUUID, uuid -> new ConcurrentHashMap<>())
            .put(messageUUID, message);
    }

    /**
     * Retrieves the most recent message sent by a specific player.
     *
     * @param playerUUID The UUID of the player.
     * @return The latest message component, or null if no messages are stored.
     */
    public @Nullable Component getLastMessage(@Nullable UUID playerUUID) {
        if (playerUUID == null) return null;
        Map<UUID, Component> messages = lastMessages.get(playerUUID);
        if (messages == null || messages.isEmpty()) return null;

        // Returns the last element added to the map
        return messages.values().stream().reduce((first, second) -> second).orElse(null);
    }

    /**
     * Puts a player into "reply mode" targeting a specific message.
     *
     * @param playerUUID  The UUID of the player who is replying.
     * @param messageUUID The UUID of the message being replied to.
     */
    public void setReplying(@Nullable UUID playerUUID, @Nullable UUID messageUUID) {
        if (playerUUID == null || messageUUID == null) return;
        pendingReplies.put(playerUUID, messageUUID);
    }

    /**
     * Retrieves a specific message from a player's message history.
     *
     * @param playerUUID  The UUID of the message author.
     * @param messageUUID The unique ID of the message.
     * @return The message component, or null if not found.
     */
    public @Nullable Component getExactPendingMessage(@Nullable UUID playerUUID, @Nullable UUID messageUUID) {
        if (playerUUID == null || messageUUID == null) return null;

        Map<UUID, Component> playerMessages = lastMessages.get(playerUUID);
        return (playerMessages != null) ? playerMessages.get(messageUUID) : null;
    }

    /**
     * Checks if a player has an active pending reply and returns the original message.
     *
     * @param playerUUID The UUID of the player checking for a reply state.
     * @return The original message component being replied to, or null if not replying.
     */
    public @Nullable Component getPendingReply(@Nullable UUID playerUUID) {
        if (playerUUID == null) return null;
        UUID targetMessageUUID = pendingReplies.get(playerUUID);
        if (targetMessageUUID == null) return null;

        for (Map<UUID, Component> authorMessages : lastMessages.values()) {
            if (authorMessages.containsKey(targetMessageUUID)) {
                return authorMessages.get(targetMessageUUID);
            }
        }
        return null;
    }

    public UUID getPendingReplyMessageUUID(@Nullable UUID playerUUID) {
        if (playerUUID == null) return null;
        return pendingReplies.get(playerUUID);
    }

    /**
     * Clears the player's current reply state.
     *
     * @param playerUUID The UUID of the player.
     */
    public void clearPendingReply(@Nullable UUID playerUUID) {
        if (playerUUID == null) return;
        pendingReplies.remove(playerUUID);
    }

    /**
     * Formats a reply by combining an icon, the truncated original message, and the new reply.
     *
     * @param message The original message component.
     * @param reply   The new reply content.
     * @return A CompletableFuture containing the fully formatted multi-line component.
     */
    public CompletableFuture<Component> reply(@NotNull Component message, @NotNull Component reply) {
        int maxLength = ConfigManager.getInstance().getMaxReplyLength();
        String icon = ConfigManager.getInstance().getReplyIcon();

        return truncate(message, maxLength).thenApply(truncated -> Component.text(icon)
            .append(Component.space())
            .append(truncated)
            .appendNewline()
            .append(reply));
    }

    /**
     * Asynchronously truncates a component to a maximum character length.
     *
     * @param original  The source component.
     * @param maxLength The maximum character limit.
     * @return A future containing the truncated component with ellipsis if needed.
     */
    @Contract("_, _ -> new")
    public static @NonNull CompletableFuture<Component> truncate(@NotNull Component original, int maxLength) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicInteger remaining = new AtomicInteger(maxLength);
            Component result = truncateRecursive(original, remaining);

            if (remaining.get() <= 0) {
                return result.append(Component.text("...").style(original.style()));
            }
            return result;
        });
    }

    /**
     * Recursively traverses components to enforce character limits while preserving styles.
     */
    private static Component truncateRecursive(Component component, @NonNull AtomicInteger remaining) {
        if (remaining.get() <= 0) return Component.empty();

        if (component instanceof TextComponent textComp) {
            String content = textComp.content();
            int len = content.length();

            if (len <= remaining.get()) {
                remaining.addAndGet(-len);
                List<Component> newChildren = new ArrayList<>();
                for (Component child : component.children()) {
                    Component processedChild = truncateRecursive(child, remaining);
                    if (processedChild != Component.empty()) {
                        newChildren.add(processedChild);
                    }
                    if (remaining.get() <= 0) break;
                }
                return textComp.children(newChildren);
            } else {
                String truncated = content.substring(0, Math.max(0, remaining.get()));
                remaining.set(0);
                return textComp.content(truncated).children(List.of());
            }
        }
        return component;
    }

    /**
     * Handles the initiation of a reply, updating the state and notifying the player.
     *
     * @param player      The player who is replying.
     * @param targetUUID  The UUID of the author being replied to.
     * @param messageUUID The unique ID of the message being replied to.
     */
    public void handleReply(@NotNull Player player, @NotNull UUID targetUUID, @NotNull UUID messageUUID) {
        setReplying(player.getUniqueId(), messageUUID);

        String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
        if (targetName == null) targetName = "Unknown Player";

        player.sendMessage(Component.text("You are now replying to: ").append(Component.text(targetName)));
    }
}
