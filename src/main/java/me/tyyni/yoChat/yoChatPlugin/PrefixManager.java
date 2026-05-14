package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.UnknownNullability;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrefixManager {
    private final File file;
    private final Plugin plugin;

    @Getter
    private final Map<UUID, String> prefixes = new ConcurrentHashMap<>();

    public PrefixManager(YoChat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        if (!file.exists()) {
            plugin.saveResource("players.yml", false);
        }
    }

    public void setPrefix(OfflinePlayer player, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            prefixes.remove(player.getUniqueId());
            return;
        }

        prefixes.put(player.getUniqueId(), prefix);
    }

    public String getPrefix(@UnknownNullability OfflinePlayer player) {
        return prefixes.getOrDefault(player.getUniqueId(), "");
    }

    public void save() {
        FileConfiguration liveConfig = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = liveConfig.getConfigurationSection("players");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    if (!prefixes.containsKey(uuid)) {
                        liveConfig.set("players." + key + ".prefix", null);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        for (Map.Entry<UUID, String> entry : prefixes.entrySet()) {
            liveConfig.set("players." + entry.getKey() + ".prefix", entry.getValue());
        }
        try {
            liveConfig.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save prefixes: " + e.getMessage());
        }
    }

    public void load() {
        prefixes.clear();

        FileConfiguration liveConfig = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = liveConfig.getConfigurationSection("players");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                String prefix = liveConfig.getString("players." + key + ".prefix");
                if (prefix != null && !prefix.isBlank()) {
                    prefixes.put(UUID.fromString(key), prefix);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Skipping invalid player UUID in players.yml: " + key);
            }
        }
    }
}
