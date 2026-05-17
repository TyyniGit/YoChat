package me.tyyni.yoChat.yoChatPlugin.objects;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a mute entry managed by YoChat.
 *
 * @param UUID the muted player's unique identifier
 * @param duration the mute duration in milliseconds, or {@code -1} for a permanent mute
 * @param whenStarted the epoch millisecond timestamp when the mute began
 * @param reason the optional reason attached to the mute
 * @param punisher the name of the player or console source that applied the mute
 */
public record MutedPlayer(@Getter UUID UUID,
                          @Getter long duration,
                          @Getter long whenStarted,
                          @Nullable @Getter String reason,
                          @Getter String punisher) {

    /**
     * Returns the epoch millisecond timestamp when this mute expires.
     *
     * @return the expiry timestamp, or {@link Long#MAX_VALUE} for permanent mutes
     */
    public long getExpiryTime() {
        if (duration == -1) return Long.MAX_VALUE;
        return whenStarted + duration;
    }

    /**
     * Checks whether this mute has already expired.
     *
     * @return {@code true} if the mute is expired, otherwise {@code false}
     */
    public boolean hasExpired() {
        return System.currentTimeMillis() >= getExpiryTime();
    }
}
