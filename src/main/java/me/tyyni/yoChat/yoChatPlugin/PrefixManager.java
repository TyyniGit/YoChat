package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrefixManager {
    /**
     * Right now this class is not in use!
     **/

    @Getter
    private final Map<UUID, String> prefixes = new ConcurrentHashMap<>();

    public void setPrefix(Player player, String prefix) {
        prefixes.put(player.getUniqueId(), prefix);
    }

    public String getPrefix(Player player) {
        return prefixes.getOrDefault(player.getUniqueId(), "");
    }
}
