package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SuffixManager {

    /**
     * Right now this class is not in use!
     **/

    @Getter
    private final Map<UUID, String> suffixes = new ConcurrentHashMap<>();

    public void setSuffix(Player player, String prefix) {
        suffixes.put(player.getUniqueId(), prefix);
    }

    public String getSuffix(Player player) {
        return suffixes.getOrDefault(player.getUniqueId(), "");
    }
}
