package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SuffixManager {

    @Getter
    private final Map<UUID, String> suffixes = new HashMap<>();

    public void setSuffix(Player player, String prefix) {
        suffixes.put(player.getUniqueId(), prefix);
    }

    public String getSuffix(Player player) {
        return suffixes.getOrDefault(player.getUniqueId(), "");
    }
}
