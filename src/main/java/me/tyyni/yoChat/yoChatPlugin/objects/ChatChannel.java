package me.tyyni.yoChat.yoChatPlugin.objects;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class ChatChannel {
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String permission;
    @Getter
    @Setter
    private int radius;
    @Getter
    private Set<Player> members = new HashSet<>();

    public ChatChannel(String name, String permission, int radius) {
        this.name = name;
        this.permission = permission;
        this.radius = radius;
    }

    public boolean canJoin(Player player) {
        return permission == null || player.hasPermission(permission);
    }

    public void addMember(Player player) {
        members.add(player);
    }

    public void removeMember(Player player) {
        members.remove(player);
    }
}