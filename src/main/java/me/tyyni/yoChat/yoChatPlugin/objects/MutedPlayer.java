package me.tyyni.yoChat.yoChatPlugin.objects;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record MutedPlayer(@Getter UUID uuid,
                          @Getter long duration,
                          @Getter long whenStarted,
                          @Nullable @Getter String reason,
                          @Getter String punisher) { }
