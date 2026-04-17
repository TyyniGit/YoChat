package me.tyyni.yoChat;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.UUID;

public class ConfigManager {

    @Getter
    private static ConfigManager instance;
    private final File file;
    private final FileConfiguration config;
    private final YoChat plugin;

    public ConfigManager(YoChat plugin) {
        this.plugin = plugin;
        instance = this;

        file = new File(plugin.getDataFolder(), "config.yml");
        if(!file.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public void load() {
        PrefixManager prefixManager = plugin.getPrefixManager();
        ChatManager chatManager = plugin.getChatManager();
        ChannelManager channelManager = plugin.getChannelManager();

        prefixManager.getPrefixes().clear();

        ConfigurationSection prefixes = config.getConfigurationSection("prefixes");
        if(prefixes == null) config.createSection("prefixes");

        if (prefixes != null) {
            for (String key : prefixes.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String prefix = prefixes.getString(key, "");
                    prefixManager.getPrefixes().put(uuid, prefix);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in config: " + key);
                }
            }
        }
    }

    public void save() {
        PrefixManager prefixManager = plugin.getPrefixManager();
        ConfigurationSection prefixes = config.getConfigurationSection("prefixes");
        if(prefixes == null) prefixes = config.createSection("prefixes");

        for (UUID uuid : prefixManager.getPrefixes().keySet()) {
            String prefix = prefixManager.getPrefixes().get(uuid);
            prefixes.set(uuid.toString(), prefix);
        }

        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save config: " + e.getMessage());
        }
    }
}
