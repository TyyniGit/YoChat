package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import lombok.Setter;
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.MutedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MuteManager {

    @Getter
    private static MuteManager instance;
    @Getter
    private static Map<String, MutedPlayer> mutedPlayers = new ConcurrentHashMap<>();
    private final YoChat plugin;
    private final File file;
    private final YamlConfiguration config;

    public MuteManager(YoChat plugin) {
        this.plugin = plugin;
        instance = this;

        file = new File(plugin.getDataFolder(), "mutedplayers.yml");
        if (!file.exists()) {
            plugin.saveResource("mutedplayers.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public static void addMutedPlayer(MutedPlayer mutedPlayer) {
        mutedPlayers.put(mutedPlayer.getUuid().toString(), mutedPlayer);
    }

    public static MutedPlayer getMutedPlayer(String uuid) {
       return mutedPlayers.get(uuid);
    }
    public static void removeMutedPlayer(MutedPlayer mutedPlayer) {
        mutedPlayers.remove(mutedPlayer.getUuid().toString());
    }

    public static boolean isMuted(Player player) {
        return mutedPlayers.containsKey(player.getUniqueId().toString());
    }

    public static boolean isMuted(String uuid) {
        return mutedPlayers.containsKey(uuid);
    }

    public static List<String> getMutedPlayerNames() {
        return mutedPlayers.values().stream()
                .map(s -> Bukkit.getOfflinePlayer(s.getUuid()).getName())
                .toList();
    }

    public void load() {
        mutedPlayers.clear();

        ConfigurationSection section = config.getConfigurationSection("mutedplayers");
        if (section == null) return;

        for (String key : section.getKeys(false)) {

            long duration = section.getLong(key + ".duration");
            long whenStarted = section.getLong(key + ".whenStarted");
            String reason = section.getString(key + ".reason");
            String punisher = section.getString(key + ".sender");
            UUID uuid = UUID.fromString(key);

            MutedPlayer mutedPlayer = new MutedPlayer(uuid, duration, whenStarted, reason, punisher);
            mutedPlayers.put(uuid.toString(), mutedPlayer);
        }
    }

    public void save() {
        config.set("mutedplayers", null);
        ConfigurationSection section = config.createSection("mutedplayers");

        for (Map.Entry<String, MutedPlayer> entry : mutedPlayers.entrySet()) {
            String key = entry.getKey();
            MutedPlayer mutedPlayer = entry.getValue();

            section.set(key + ".duration", mutedPlayer.getDuration());
            section.set(key + ".whenStarted", mutedPlayer.getWhenStarted());
            section.set(key + ".reason", mutedPlayer.getReason());
            section.set(key + ".sender", mutedPlayer.getPunisher());
        }

        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save config: " + e.getMessage());
        }
    }

    public void checkMutes() {
        for (MutedPlayer mp : mutedPlayers.values()) {
            if (mp.hasExpired()) {
                removeMutedPlayer(mp);

                if (ConfigManager.getInstance().isUseTimeEndedMessage()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {

                        Player onlinePlayer = Bukkit.getPlayer(mp.getUuid());
                        if (onlinePlayer != null) {
                            onlinePlayer.sendMessage(YoChatAPI.getPlugin().getChatManager().formatTimeEndedMessage(ConfigManager.getInstance().getTimeEndedMessage(), onlinePlayer));
                        }
                    });
                }
            }
        }
    }

    @Setter
    @Getter
    long interval;

    public void startMuteChecker() {
        long millis = YoChatAPI.getPlugin().getChatManager().parseDuration(ConfigManager.getInstance().getMuteCheckerInterval());
        setInterval((millis / 1000L) * 20L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkMutes, 0L, getInterval());
    }
}
