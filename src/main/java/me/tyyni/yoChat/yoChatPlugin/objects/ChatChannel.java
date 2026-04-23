package me.tyyni.yoChat.yoChatPlugin.objects;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

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
    private Set<Player> members = new HashSet<>();

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
     * @param permission  The required permission (can be null).
     * @param radius      The chat broadcast radius.
     * @param strictWorld Whether world restriction is active.
     * @param worlds      The allowed worlds (can be null).
     */
    public ChatChannel(String name, @Nullable String permission, int radius, boolean strictWorld, @Nullable Set<String> worlds) {
        this.name = name;
        this.permission = permission;
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
        return permission == null || player.hasPermission(permission);
    }

    /**
     * Adds a player to the channel's member list.
     *
     * @param player The player to add.
     */
    public void addMember(Player player) {
        members.add(player);
    }

    /**
     * Removes a player from the channel's member list.
     *
     * @param player The player to remove.
     */
    public void removeMember(Player player) {
        members.remove(player);
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
}