package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.UUID;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class ConfigManager {

    @Getter
    private static ConfigManager instance;
    private final File file;
    private final FileConfiguration config;
    private final YoChat plugin;

    @Getter
    private boolean debug;
    @Getter
    private boolean isEnabled;
    @Getter
    private boolean useLuckPerms;
    @Getter
    private boolean usePlaceholderAPI;
    @Getter
    private String chatFormat;

    public ConfigManager(YoChat plugin) {
        this.plugin = plugin;
        instance = this;

        file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
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
        if (prefixes == null) config.createSection("prefixes");

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

        debug = config.getBoolean("debug", false);
        isEnabled = config.getBoolean("enabled", true);
        useLuckPerms = config.getBoolean("useLuckPerms", true);
        usePlaceholderAPI = config.getBoolean("usePlaceholderAPI", true);
        chatFormat = config.getString("chat-format", "{player}: {message}");

        String prefix = config.getString("prefix");
        if (prefix == null) {
            prefix = plugin.getAlternativePrefix();
        }

        Component prefixComponent = parse(prefix);
        plugin.setYoChatPrefix(prefixComponent);

        if (debug) {
            plugin.getLogger().info("[DEBUG] " + "Loaded config!");
        }
    }

    public void save() {
        PrefixManager prefixManager = plugin.getPrefixManager();
        ConfigurationSection prefixes = config.getConfigurationSection("prefixes");
        if (prefixes == null) prefixes = config.createSection("prefixes");

        for (UUID uuid : prefixManager.getPrefixes().keySet()) {
            String prefix = prefixManager.getPrefixes().get(uuid);
            prefixes.set(uuid.toString(), prefix);
        }

        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save config: " + e.getMessage());
        }

        if (debug) {
            plugin.getLogger().info("[DEBUG] " + "Saved config!");
        }
    }

    public Component parse(String input) {
        if (input == null) return Component.empty();

        if (input.contains("<") && input.contains(">")) {
            try {
                return MiniMessage.miniMessage().deserialize(input);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse MiniMessage: " + e.getMessage());
                return Component.text(input);
            }
        }

        final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

        input = HEX_PATTERN.matcher(input).replaceAll((MatchResult match) -> {
            String hex = match.group(1);
            StringBuilder builder = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                builder.append('§').append(c);
            }
            return builder.toString();
        });


        input = input.replace('&', '§');

        return LegacyComponentSerializer.legacySection().deserialize(input);
    }
}