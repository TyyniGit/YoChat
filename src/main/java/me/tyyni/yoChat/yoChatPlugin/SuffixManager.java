package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SuffixManager {
    private final File file;
    private final Plugin plugin;

    @Getter
    private final Map<UUID, String> suffixes = new ConcurrentHashMap<>();

    public SuffixManager(@NonNull YoChat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        if (!file.exists()) {
            plugin.saveResource("players.yml", false);
        }

    }

    public void setSuffix(OfflinePlayer player, String suffix) {
        if (suffix == null || suffix.isBlank()) {
            suffixes.remove(player.getUniqueId());
            return;
        }

        suffixes.put(player.getUniqueId(), suffix);
    }

    public String getSuffix(@UnknownNullability OfflinePlayer player) {
        return suffixes.getOrDefault(player.getUniqueId(), "");
    }

    public void save() {
        FileConfiguration liveConfig = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = liveConfig.getConfigurationSection("players");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    if (!suffixes.containsKey(uuid)) {
                        liveConfig.set("players." + key + ".suffix", null);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        for (Map.Entry<UUID, String> entry : suffixes.entrySet()) {
            liveConfig.set("players." + entry.getKey() + ".suffix", entry.getValue());
        }
        try {
            liveConfig.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save suffixes: " + e.getMessage());
        }
    }

    public void load() {
        suffixes.clear();

        FileConfiguration liveConfig = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = liveConfig.getConfigurationSection("players");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                String suffix = liveConfig.getString("players." + key + ".suffix");
                if (suffix != null && !suffix.isBlank()) {
                    suffixes.put(UUID.fromString(key), suffix);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Skipping invalid player UUID in players.yml: " + key);
            }
        }
    }
}
