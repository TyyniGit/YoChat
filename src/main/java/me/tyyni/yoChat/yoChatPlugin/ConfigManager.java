package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
    ChatChannel defaultChannel;

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
        // 1. TÄRKEÄÄ: Lataa tiedosto uudelleen levyltä muistiin!
        try {
            config.load(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not reload config.yml: " + e.getMessage());
        }

        PrefixManager prefixManager = plugin.getPrefixManager();
        ChannelManager channelManager = plugin.getChannelManager();

        prefixManager.getPrefixes().clear();

        // 2. Haetaan arvot oikeista osioista (formatting.chat-format jne.)
        debug = config.getBoolean("general.debug", false);
        isEnabled = config.getBoolean("general.enabled", true);
        useLuckPerms = config.getBoolean("Addidional.useLuckPerms", true); // Huom: kirjoitusvirhe 'Addidional' konffissa
        usePlaceholderAPI = config.getBoolean("Addidional.usePlaceholderAPI", true);

        chatFormat = config.getString("formatting.chat-format", "{player}: {message}");
        channelFormat = config.getString("formatting.channel-format", "{player}: {message}");

        useChannelSystem = config.getBoolean("general.use-channel-system", true);
        defaultChannel = channelManager.getChannel(config.getString("general.default-channel", "global"));

        if (defaultChannel == null) {
            plugin.getLogger().warning("Default channel not found! Using global.");
            defaultChannel = channelManager.getChannel("global");
        }

        // Prefiksin haku 'general' osiosta
        String prefix = config.getString("general.chat-prefix");
        if (prefix == null) {
            prefix = plugin.getAlternativePrefix();
        }

        Component prefixComponent = parse(prefix);
        plugin.setYoChatPrefix(prefixComponent);

        // Prefiksien haku (jos ne ovat konffissa)
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

        // 1. Jos tekstissä on MiniMessage-tageja (kuten <red> tai <gradient>)
        // Käytetään MiniMessagea, mutta sallitaan myös Legacy-koodit sen sisällä
        // (Huom: MiniMessage ei oletuksena tykkää &-merkeistä, joten korjataan ne)

        // 2. Käsitellään Legacy-koodit (& -> §)
        String legacyProcessed = input.replace('&', '§');

        // 3. Muutetaan kaikki ensin komponentiksi Legacy-serialisoijan kautta (tukee Hexiä)
        Component legacyComponent = LegacyComponentSerializer.legacySection().deserialize(legacyProcessed.replace('&', '§'));

        // 4. Jos haluat tukea MiniMessagea SAMASSA merkkijonossa:
        // Muutetaan legacy takaisin MiniMessage-yhteensopivaksi tai käytetään yhdistelmää.
        // Helpoin tapa tukea molempia on katsoa, onko tekstissä MiniMessage-tageja.
        if (input.contains("<") && input.contains(">")) {
            return MiniMessage.miniMessage().deserialize(input);
        }

        return legacyComponent;
    }
}