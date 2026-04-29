package me.tyyni.yoChat.yoChatPlugin.objects;

import lombok.Getter;
import lombok.Setter;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a chat channel with specific settings such as range,
 * access permissions, and active members.
 */
public class ChatChannel {

    /** The display name of the channel. */
    @Getter
    @Setter
    private String name;

    /** The permission node required to join this channel.
     * If null, the channel is public.
     */
    @Getter
    @Setter
    @Nullable
    private String permission;

    /** The broadcast radius in blocks.
     * Values of 0 or less typically represent global chat.
     */
    @Getter
    @Setter
    private int radius;

    /** A set of players currently residing in this channel. */
    @Getter
    private final Set<Player> members = ConcurrentHashMap.newKeySet();

    /** Whether both the message sender and the receiver have to be in some of the worlds defined in the worlds Set. */
    @Getter
    @Setter
    private boolean strictWorld;

    /** The set of world names where this channel is accessible. */
    @Getter
    @Setter
    @Nullable
    Set<String> worlds;

    /** The custom chat format for this channel. If null, the default format is used. */
    @Getter
    @Setter
    @Nullable
    private String format = null;

    /**
     * Constructs a new ChatChannel.
     *
     * @param name        The name of the channel.
     * @param radius      The chat broadcast radius.
     * @param strictWorld Whether world restriction is active.
     * @param worlds      The allowed worlds (can be null).
     */
    public ChatChannel(String name, int radius, boolean strictWorld, @Nullable Set<String> worlds) {
        this.name = name;
        this.radius = radius;
        this.strictWorld = strictWorld;
        this.worlds = worlds;
    }

    /**
     * Checks if a player has the necessary permissions to join this channel.
     *
     * @param player The player to check.
     * @return true if the player has the required permission or if no permission is set.
     */
    public boolean canJoin(Player player) {
        boolean allowed = permission == null || player.hasPermission(permission);
        if (!allowed) {
            debug("Player %s cannot join channel %s because they lack permission %s", player.getName(), name, permission);
        }
        return allowed;
    }

    /**
     * Adds a player to the channel's member list.
     *
     * @param player The player to add.
     */
    public void addMember(Player player) {
        members.add(player);
        debug("Player %s added to channel %s", player.getName(), name);
    }

    /**
     * Removes a player from the channel's member list.
     *
     * @param player The player to remove.
     */
    public void removeMember(Player player) {
        members.remove(player);
        debug("Player %s removed from channel %s", player.getName(), name);
    }

    /**
     * Checks if a player is currently a member of this channel.
     *
     * @param player The player to check.
     * @return true if the player is a member.
     */
    public boolean isMember(Player player) {
        return members.contains(player);
    }

    /**
     * Adds a world to the channel's world list.
     * @param worldName - The world to add.
     */
    public void addWorld(String worldName) {
        if (worlds != null) {
            worlds.add(worldName);
        } else {
            worlds = new HashSet<>();
            worlds.add(worldName);
        }
        debug("Added world %s to channel %s", worldName, name);
    }
    /**
     * Removes a world from the channel's world list.
     *
     * @param worldName The world to remove.
     */
    public void removeWorld(String worldName) {
        if (worlds != null) {
            worlds.remove(worldName);

            if(worlds.isEmpty()) worlds = null;
            debug("Removed world %s from channel %s", worldName, name);
        }
    }
    /**
     * Checks if the worlds list contains a world.
     *
     * @param worldName The world to check.
     * @return true if the list contains the world.
     */
    public boolean hasWorld(String worldName) {
        if (worlds != null) {
            return worlds.contains(worldName);
        }
        return false;
    }

    private void debug(String format, Object... args) {
        ConfigManager configManager = ConfigManager.getInstance();
        if (configManager != null) {
            configManager.debug(String.format(Locale.ROOT, format, args));
        }
    }
}
