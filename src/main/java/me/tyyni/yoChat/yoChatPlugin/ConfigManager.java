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

        useLuckPerms = config.getBoolean("Addidional.useLuckPerms", true);
        useVault = config.getBoolean("Addidional.useVault", false);
        usePlaceholderAPI = config.getBoolean("Addidional.usePlaceholderAPI", true);

        chatFormat = config.getString("formatting.chat-format", "{player}: {message}");
        channelFormat = config.getString("formatting.channel-format", "{player}: {message}");

        isModerationEnabled = config.getBoolean("moderation.enabled", true);
        blockedwords = config.getStringList("moderation.blocked-words");
        blockedWordMessage = config.getString("moderation.bad-word-message", "<red>Please speak respectfully on this server!</red>");

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

        useChannelSpecificFormatting = config.getBoolean("channel-specific-formatting.enabled", false);

        useMentioning = config.getBoolean("mentioning.enabled", true);
        mentioningFormat = config.getString("mentioning.format", "<blue>@{name}</blue>");
        useSound = config.getBoolean("mentioning.use-sound", true);

        String soundname = config.getString("mentioning.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        NamespacedKey soundKey = NamespacedKey.minecraft(soundname.toLowerCase().replace("_", "."));
        sound = Registry.SOUNDS.get(soundKey);
        soundVolume = (float) config.getDouble("mentioning.volume", 1.0);
        soundPitch = (float) config.getDouble("mentioning.pitch", 1.0);

        minimessageStrictMode = config.getBoolean("minimessage-customization.strict-mode", false);
        ConfigurationSection channelSpecificFormats = config.getConfigurationSection("channel-specific-formatting.formats");
        if(channelSpecificFormats != null) {
            for(String key : channelSpecificFormats.getKeys(false)) {
                ChatChannel channel = YoChatAPI.getPlugin().getChannelManager().getChannel(key);
                String format = channelSpecificFormats.getString("format", null);
                if(channel != null) {
                    channel.setFormat(format);
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

        if (debug) {
            plugin.getLogger().info("[DEBUG] Config reloaded and variables updated!");
        }

        if(!useLuckPerms && !useVault) {
            plugin.getLogger().severe("Configuration error!");
            plugin.getLogger().severe("LuckPerms and Vault not enabled");
            plugin.getLogger().severe("YoChat needs at least one of them to work");

            if(Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                useVault = true;
            } else if(Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                useLuckPerms = true;
            } else {
                plugin.getLogger().warning("Disabled plugin because LuckPerms ja LuckPerms both are not enabled!");
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        }

        if(usePlaceholderAPI) {
            if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
                plugin.getLogger().warning("PlaceholderAPI plugin not found!");
                usePlaceholderAPI = false;
            }
        }

        if(!isEnabled) {
            plugin.getLogger().warning("Disabled plugin!");
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }
}
