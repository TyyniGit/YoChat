package me.tyyni.yoChat.objects;

import org.bukkit.entity.Player;

public interface ChatChannel {

    String getName();

    boolean canJoin(Player player);

    void send(Player player, String message);
}
