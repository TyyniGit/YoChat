package me.tyyni.yoChat;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PrefixManager {
    @Getter
    private final Map<UUID, String> prefixes = new HashMap<>();

    public void setPrefix(Player player, String prefix) {
        prefixes.put(player.getUniqueId(), prefix);
    }

    public String getPrefix(Player player) {
        return prefixes.getOrDefault(player.getUniqueId(), "");
    }
}
