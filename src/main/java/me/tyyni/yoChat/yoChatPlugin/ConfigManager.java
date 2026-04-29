package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class ConfigManager {

    @Getter
    private static ConfigManager instance;
    private final File file;
    @Getter
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
    private boolean useVault;

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
    private String muteWebhookUrl;
    @Getter
    private String unmuteWebhookUrl;
    @Getter
    private boolean sendResponseCode;
    @Getter
    private boolean sendResponseBody;
    @Getter
    private boolean webhookEnabled;

    @Getter
    private boolean useMutedMessage;
    @Getter
    private String mutedMessage;
    @Getter
    private boolean useYouGotMutedMessage;
    @Getter
    private String youGotMutedMessage;
    @Getter
    private boolean useYouGotUnmutedMessage;
    @Getter
    private String youGotUnmutedMessage;
    @Getter
    private String muteCheckerInterval;

    @Getter
    private String noChannelMessage;
    @Getter
    private String noPermissionMessage;

    @Getter
    private boolean useChannelSpecificFormatting;
    @Getter
    private Map<ChatChannel, String> channelsFormats;
    @Getter
    private boolean minimessageStrictMode;
    @Getter
    private boolean useTimeEndedMessage;
    @Getter
    private String timeEndedMessage;

    @Getter
    private boolean useMentioning;
    @Getter
    private String mentioningFormat;
    @Getter
    private boolean useSound;
    @Getter
    private Sound sound;
    @Getter
    private Float soundVolume;
    @Getter
    private Float soundPitch;

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

        ChannelManager channelManager = plugin.getChannelManager();

        debug = config.getBoolean("general.debug", false);
        isEnabled = config.getBoolean("general.enabled", true);
        useChannelSystem = config.getBoolean("general.use-channel-system", true);
        defaultChannel = channelManager.getChannel(config.getString("general.default-channel", "global"));
        noChannelMessage = config.getString("general.no-channel-message", "<red>You don't belong to any channels! Please contact administrators.</red>");
        noPermissionMessage = config.getString("general.no-permission-message", "<red>You don't have permission to execute this command!</red>");

        debug("Starting config load from %s", file.getAbsolutePath());

        useLuckPerms = config.getBoolean("additional.useLuckPerms", true);
        useVault = config.getBoolean("additional.useVault", false);
        usePlaceholderAPI = config.getBoolean("additional.usePlaceholderAPI", true);

        chatFormat = config.getString("formatting.chat-format", "{player}: {message}");
        channelFormat = config.getString("formatting.channel-format", "{player}: {message}");

        isModerationEnabled = config.getBoolean("moderation.enabled", true);
        blockedwords = config.getStringList("moderation.blocked-words");
        blockedWordMessage = config.getString("moderation.blocked-word-message", "<red>Please speak respectfully on this server!</red>");

        sendResponseCode = config.getBoolean("moderation.discord-webhook.webhook-send-response-code", true);
        sendResponseBody = config.getBoolean("moderation.discord-webhook.webhook-send-response-body", true);
        unmuteWebhookUrl = config.getString("moderation.discord-webhook.unmute-webhook-url");
        muteWebhookUrl = config.getString("moderation.discord-webhook.mute-webhook-url");
        webhookEnabled = config.getBoolean("moderation.discord-webhook.enabled", true);

        useMutedMessage = config.getBoolean("moderation.use-muted-message", true);
        mutedMessage = config.getString("moderation.muted-message", "<red>You are muted for the reason <b>'{reason}'</b></red>");
        useYouGotMutedMessage = config.getBoolean("moderation.use-yougotmuted-message", true);
        youGotMutedMessage = config.getString("moderation.yougotmuted-message", "<red>You are muted for the reason <b>'{reason}'</b></red>");
        useYouGotUnmutedMessage = config.getBoolean("moderation.use-yougotunmuted-message", true);
        youGotUnmutedMessage = config.getString("moderation.yougotunmuted-message", "<green>You got unmuted by <b>'{pardoner}'</b></green>");
        useTimeEndedMessage = config.getBoolean("moderation.use-timeended-message", true);
        timeEndedMessage = config.getString("moderation.timeended-message", "<green>Your mute is over!</green>");
        muteCheckerInterval = config.getString("moderation.mute-checker-interval", "10s");

        useChannelSpecificFormatting = config.getBoolean("formatting.channel-specific-formatting.enabled", false);

        useMentioning = config.getBoolean("mentioning.enabled", true);
        mentioningFormat = config.getString("mentioning.format", "<blue>@{name}</blue>");
        useSound = config.getBoolean("mentioning.use-sound", true);

        String soundInput = config.getString("mentioning.sound", "entity.experience_orb.pickup").toLowerCase();

        if (soundInput.contains(":")) {
            soundInput = soundInput.split(":")[1];
        }

        NamespacedKey soundKey = NamespacedKey.minecraft(soundInput.replace(".", "_"));

        this.sound = Registry.SOUNDS.get(soundKey);

        if (this.sound == null) {
            this.sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundInput));
        }

        if (this.sound == null) {
            plugin.getLogger().warning("Sound '" + soundInput + "' was not found. Using default: 'entity.experience_orb.pickup'");
            this.sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }

        soundVolume = (float) config.getDouble("mentioning.volume", 1.0);
        soundPitch = (float) config.getDouble("mentioning.pitch", 1.0);

        minimessageStrictMode = config.getBoolean("minimessage-customization.strict-mode", false);
        ConfigurationSection channelSpecificFormats = config.getConfigurationSection("formatting.channel-specific-formatting.formats");
        if(channelSpecificFormats != null) {
            for(String key : channelSpecificFormats.getKeys(false)) {
                ChatChannel channel = YoChatAPI.getPlugin().getChannelManager().getChannel(key);
                String format = channelSpecificFormats.getString(key + ".format", null);
                if(channel != null) {
                    channel.setFormat(format);
                    debug("Loaded channel-specific format for channel '%s'", key);
                } else {
                    debug("Skipped channel-specific format for unknown channel '%s'", key);
                }
            }
        }

        if (defaultChannel == null) {
            plugin.getLogger().warning("Default channel not found! Using global.");
            defaultChannel = channelManager.getChannel("global");
        }

        String prefix = config.getString("general.chat-prefix");
        if (prefix == null) {
            prefix = plugin.getAlternativePrefix();
        }

        Component prefixComponent = YoChatAPI.getPlugin().getMessageParseManager().parseAdmin(prefix);
        plugin.setYoChatPrefix(prefixComponent);

        debug("Config reloaded and variables updated");
        debug("general: enabled=%s, debug=%s, useChannelSystem=%s, defaultChannel=%s",
                isEnabled, debug, useChannelSystem, defaultChannel != null ? defaultChannel.getName() : "null");
        debug("hooks: useLuckPerms=%s, useVault=%s, usePlaceholderAPI=%s",
                useLuckPerms, useVault, usePlaceholderAPI);
        debug("moderation: enabled=%s, blockedWords=%d, webhookEnabled=%s",
                isModerationEnabled, blockedwords != null ? blockedwords.size() : 0, webhookEnabled);
        debug("mentioning: enabled=%s, useSound=%s, configuredSound=%s, resolvedSound=%s, volume=%s, pitch=%s",
                useMentioning, useSound, soundInput, sound != null, soundVolume, soundPitch);

        if(!useLuckPerms && !useVault) {
            plugin.getLogger().severe("Configuration error!");
            plugin.getLogger().severe("LuckPerms and Vault not enabled");
            plugin.getLogger().severe("YoChat needs at least one of them to work");

            if(Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                useVault = true;
                debug("Recovered from invalid config by enabling Vault because plugin is present");
            } else if(Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                useLuckPerms = true;
                debug("Recovered from invalid config by enabling LuckPerms because plugin is present");
            } else {
                plugin.getLogger().warning("Disabled plugin because neither LuckPerms nor Vault is enabled!");
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        }

        if(usePlaceholderAPI) {
            if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
                plugin.getLogger().warning("PlaceholderAPI plugin not found!");
                usePlaceholderAPI = false;
                debug("PlaceholderAPI support disabled because the plugin was not found");
            }
        }

        if(!isEnabled) {
            plugin.getLogger().warning("Disabled plugin!");
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    public void debug(String message) {
        if (!debug) {
            return;
        }
        plugin.getLogger().info("[DEBUG] " + message);
    }

    public void debug(String format, Object... args) {
        if (!debug) {
            return;
        }

        try {
            debug(String.format(Locale.ROOT, format, args));
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "[DEBUG] Failed to format debug message: " + format, ex);
        }
    }
}
