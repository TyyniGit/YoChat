package me.tyyni.yoChat.yoChatAPI;

import lombok.Setter;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.Stage;
import me.tyyni.yoChat.yoChatPlugin.MuteManager;
import me.tyyni.yoChat.yoChatPlugin.ChatPipelineManager;
import me.tyyni.yoChat.yoChatPlugin.YoChat;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yoChatPlugin.objects.MutedPlayer;
import me.tyyni.yoChat.yoChatAPI.interfaces.YoChatProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class YoChatAPI {

    @Setter
    private static YoChatProvider provider;

    /**
     * Returns the active YoChat plugin instance provided by the current API provider.
     *
     * @return the YoChat plugin instance
     */
    public static YoChat getPlugin() {
        checkProvider();
        return provider.getYoChat();
    }

    /**
     * Broadcasts a formatted message to the global chat.
     *
     * @param message the message component to send
     * @param sender the player who sent the message
     */
    public static void sendGlobalMessage(Component message, Player sender) {
        checkProvider();
        provider.getChatManager().broadcast(message, sender);
    }

    /**
     * Registers a chat channel so it can be used by YoChat.
     *
     * @param channel the channel to register
     */
    public static void registerChannel(ChatChannel channel) {
        checkProvider();
        provider.getChannelManager().register(channel);
    }

    /**
     * Creates a new {@link ChatChannel} instance using YoChat's channel manager.
     *
     * @param channelName the channel name
     * @param radius the channel radius in blocks, or a non-positive value for unrestricted range
     * @param strictWorld whether the sender must also be inside an allowed world
     * @param worlds the allowed world names, or {@code null} for no world restriction
     * @return the created channel instance
     */
    public static ChatChannel createChannel(String channelName, int radius, boolean strictWorld, @Nullable java.util.Set<String> worlds) {
        checkProvider();
        return provider.getChannelManager().createChannel(channelName, radius, strictWorld, worlds);
    }

    /**
     * Looks up a registered channel by name.
     *
     * @param name the channel name
     * @return the matching channel, or {@code null} if none exists
     */
    public static @Nullable ChatChannel getChannel(String name) {
        checkProvider();
        return provider.getChannelManager().getChannel(name);
    }

    /**
     * Returns a snapshot of all currently registered channels.
     *
     * @return an immutable collection of registered channels
     */
    public static Collection<ChatChannel> getChannels() {
        checkProvider();
        return List.copyOf(provider.getChannelManager().getChannels());
    }

    /**
     * Deletes a registered channel by name.
     *
     * @param channelName the channel name to delete
     */
    public static void deleteChannel(String channelName) {
        checkProvider();
        provider.getChannelManager().deleteChannel(channelName);
    }

    /**
     * Moves a player into the given channel.
     *
     * @param player the player to move
     * @param channel the target channel
     */
    public static void joinChannel(Player player, ChatChannel channel) {
        checkProvider();
        provider.getChannelManager().joinChannel(player, channel);
    }

    /**
     * Returns the channel the given player currently belongs to.
     *
     * @param player the player to check
     * @return the player's current channel, or {@code null} if the player is not in any channel
     */
    public static @Nullable ChatChannel getChannelByPlayer(Player player) {
        checkProvider();
        return provider.getChannelManager().getChannelByPlayer(player);
    }

    /**
     * Sends a message through a specific channel using YoChat's channel formatting and delivery rules.
     *
     * @param channel the channel to send to
     * @param sender the player who sent the message
     * @param message the message component to send
     */
    public static void sendToChannel(ChatChannel channel, Player sender, Component message) {
        checkProvider();
        provider.getChannelManager().sendToChannel(channel, sender, message);
    }

    /**
     * Removes a muted player entry from YoChat.
     *
     * @param mutedPlayer the muted player entry to remove
     */
    public static void removeMutedPlayer(MutedPlayer mutedPlayer) {
        checkProvider();
        MuteManager.removeMutedPlayer(mutedPlayer);
    }

    /**
     * Removes a muted player entry by UUID if it exists.
     *
     * @param uuid the UUID of the muted player
     */
    public static void removeMutedPlayer(UUID uuid) {
        checkProvider();
        MutedPlayer mutedPlayer = MuteManager.getMutedPlayer(uuid.toString());
        if (mutedPlayer != null) {
            MuteManager.removeMutedPlayer(mutedPlayer);
        }
    }

    /**
     * Adds a muted player entry to YoChat.
     *
     * @param mutedPlayer the muted player entry to add
     */
    public static void addMutedPlayer(MutedPlayer mutedPlayer) {
        checkProvider();
        MuteManager.addMutedPlayer(mutedPlayer);
    }

    /**
     * Returns the muted player entry for the given UUID.
     *
     * @param uuid the UUID to look up
     * @return the muted player entry, or {@code null} if the player is not muted
     */
    public static @Nullable MutedPlayer getMutedPlayer(UUID uuid) {
        checkProvider();
        return MuteManager.getMutedPlayer(uuid.toString());
    }

    /**
     * Checks whether a player is currently muted.
     *
     * @param player the player to check
     * @return {@code true} if the player is muted
     */
    public static boolean isMuted(Player player) {
        checkProvider();
        return MuteManager.isMuted(player);
    }

    /**
     * Checks whether a UUID is currently muted.
     *
     * @param uuid the UUID to check
     * @return {@code true} if the UUID is muted
     */
    public static boolean isMuted(UUID uuid) {
        checkProvider();
        return MuteManager.isMuted(uuid.toString());
    }

    /**
     * Returns a snapshot of all muted player entries.
     *
     * @return a collection containing all muted players
     */
    public static Collection<MutedPlayer> getMutedPlayers() {
        checkProvider();
        return new ArrayList<>(MuteManager.getMutedPlayers().values());
    }

    /**
     * Returns the names of all muted players that can be resolved by Bukkit.
     *
     * @return a list of muted player names
     */
    public static List<String> getMutedPlayerNames() {
        checkProvider();
        return MuteManager.getMutedPlayerNames();
    }

    /**
     * Formats a global chat message using YoChat's chat format.
     *
     * @param sender the player who sent the message
     * @param message the raw message component
     * @return the formatted message component
     */
    public static Component formatMessage(Player sender, Component message) {
        checkProvider();
        return provider.getChatManager().formatMessage(sender, message);
    }

    /**
     * Formats a message using a specific channel's format.
     *
     * @param channel the channel whose format should be used
     * @param sender the player who sent the message
     * @param message the raw message component
     * @return the formatted channel message component
     */
    public static Component formatChannelMessage(ChatChannel channel, Player sender, Component message) {
        checkProvider();
        return provider.getChatManager().formatChannelMessage(channel, sender, message);
    }

    /**
     * Parses a player-provided message using YoChat's MiniMessage rules and permissions.
     *
     * @param player the player whose permissions are used for parsing
     * @param input the raw input string
     * @return the parsed component
     */
    public static Component parseMessage(Player player, String input) {
        checkProvider();
        return provider.getMessageParseManager().parse(player, input);
    }

    /**
     * Parses an administrative string using YoChat's full admin parser.
     *
     * @param input the raw input string
     * @return the parsed component
     */
    public static Component parseAdminMessage(String input) {
        checkProvider();
        return provider.getMessageParseManager().parseAdmin(input);
    }

    public static void registerStep(Stage stage, ChatPipelineStep nextStage) {
        checkProvider();
        provider.getChatPipelineManager().registerStep(stage, nextStage);
    }

    public static void registerStep(Stage stage, ChatPipelineStep nextStage, int priority) {
        checkProvider();
        provider.getChatPipelineManager().registerStep(stage, nextStage, priority);
    }

    public static List<ChatPipelineManager.RegisteredPipelineStep> getRegisteredPipelineSteps(Stage stage) {
        checkProvider();
        return provider.getChatPipelineManager().getSteps(stage);
    }

    private static void checkProvider() {
        if (provider == null) {
            throw new IllegalStateException("YoChatProvider is not set! Please set it before using the API.");
        }
    }

    // Prefix and suffix systems might be implemented in the future.
    /* public static void setPrefix(Player player, String prefix) {
    *    checkProvider();
    *    provider.getPrefixManager().setPrefix(player, prefix);
    * }
    *
    * public static String getPrefix(Player player) {
    *     checkProvider();
    *   return provider.getPrefixManager().getPrefix(player);
    * }
    *
    * public static String getSuffix(Player player) {
    *    checkProvider();
    *    return provider.getSuffixManager().getSuffix(player);
    * }
    *
    * public static void setSuffix(Player player, String suffix) {
    *    checkProvider();
    *    provider.getSuffixManager().setSuffix(player, suffix);
    * }
    *
     */
}
