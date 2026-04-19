package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.UUID;

@Slf4j
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
    @Getter
    private String channelFormat;
    @Getter

    private boolean useChannelSystem;
    @Getter
    private ChatChannel defaultChannel;

    @Getter
    private List<String> blockedwords;
    @Getter
    private boolean isModerationEnabled;
    @Getter
    private String blockedWordMessage;
    @Getter
    private String muteChannelUrl;
    @Getter
    private String unmuteChannelUrl;
    @Getter
    private boolean sendResponseCode;
    @Getter
    private boolean sendResponseBody;
    @Getter
    private boolean webhookEnabled;

    @Getter
    private boolean useMuteMessage;
    @Getter
    private String muteMessage;

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
        try {
            config.load(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not reload config.yml: " + e.getMessage());
        }

        PrefixManager prefixManager = plugin.getPrefixManager();
        ChannelManager channelManager = plugin.getChannelManager();

        prefixManager.getPrefixes().clear();

        debug = config.getBoolean("general.debug", false);
        isEnabled = config.getBoolean("general.enabled", true);
        useLuckPerms = config.getBoolean("Addidional.useLuckPerms", true);
        usePlaceholderAPI = config.getBoolean("Addidional.usePlaceholderAPI", true);

        chatFormat = config.getString("formatting.chat-format", "{player}: {message}");
        channelFormat = config.getString("formatting.channel-format", "{player}: {message}");

        useChannelSystem = config.getBoolean("general.use-channel-system", true);
        defaultChannel = channelManager.getChannel(config.getString("general.default-channel", "global"));

        isModerationEnabled = config.getBoolean("moderation.enabled", true);
        blockedwords = config.getStringList("moderation.blocked-words");
        blockedWordMessage = config.getString("moderation.bad-word-message", "<red>Please speak respectfully on this server!</red>");
        sendResponseCode = config.getBoolean("moderation.webhook-send-response-code", true);
        sendResponseBody = config.getBoolean("moderation.webhook-send-response-body", true);
        unmuteChannelUrl = config.getString("moderation.webhook-unmute-channel-url");
        muteChannelUrl = config.getString("moderation.webhook-mute-channel-url");
        webhookEnabled = config.getBoolean("moderation.discord-webhook.enabled", true);

        useMuteMessage = config.getBoolean("moderation.use-mute-message", true);
        muteMessage = config.getString("moderation.mute-message", "<red>You are muted for the reason <b>'{reason}'</b></red>");

        if (defaultChannel == null) {
            plugin.getLogger().warning("Default channel not found! Using global.");
            defaultChannel = channelManager.getChannel("global");
        }

        String prefix = config.getString("general.chat-prefix");
        if (prefix == null) {
            prefix = plugin.getAlternativePrefix();
        }

        Component prefixComponent = parse(prefix);
        plugin.setYoChatPrefix(prefixComponent);

        ConfigurationSection prefixes = config.getConfigurationSection("prefixes");
        if (prefixes != null) {
            for (String key : prefixes.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String p = prefixes.getString(key, "");
                    prefixManager.getPrefixes().put(uuid, p);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in config: " + key);
                }
            }
        }

        if (debug) {
            plugin.getLogger().info("[DEBUG] Config reloaded and variables updated!");
        }

        if(!isEnabled) {
            plugin.getLogger().warning("Disabled plugin!");
            Bukkit.getPluginManager().disablePlugin(plugin);
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
        if (input == null || input.isEmpty()) return Component.empty();

        String legacyProcessed = input.replace('&', '§');

        Component legacyComponent = LegacyComponentSerializer.legacySection().deserialize(legacyProcessed.replace('&', '§'));

        if (input.contains("<") && input.contains(">")) {
            return MiniMessage.miniMessage().deserialize(input);
        }

        return legacyComponent;
    }
}