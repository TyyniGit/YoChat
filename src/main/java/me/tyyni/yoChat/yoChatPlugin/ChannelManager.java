package me.tyyni.yoChat.yoChatPlugin;

import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yochatAPI.YoChatAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChannelManager {
    private final Map<String, ChatChannel> channels = new HashMap<>();
    private final File file;
    private final FileConfiguration config;
    private final Plugin plugin;
    public ChannelManager(YoChat plugin) {
        this.plugin = plugin;
        ChatChannel channel = createChannel("global", null, -1);
        register(channel);

        file = new File(plugin.getDataFolder(), "channels.yml");
        if (!file.exists()) {
            plugin.saveResource("channels.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }
    public void register(ChatChannel channel) {
        channels.put(channel.getName().toLowerCase(Locale.ROOT), channel);
    }

    public ChatChannel getChannel(String name) {
        return channels.get(name.toLowerCase(Locale.ROOT));
    }
    public Collection<ChatChannel> getChannels() {
        return channels.values();
    }

    public void sendToChannel(ChatChannel channel, Player sender, Component message) {
        Component msg = YoChatAPI.getInstance().getChatManager().formatChannelMessage(channel, sender, message);

        for (Player p : Bukkit.getOnlinePlayers()) {

            if (!channel.canJoin(p)) continue;

            // radius check
            if (channel.getRadius() > 0) {
                if (!p.getWorld().equals(sender.getWorld())) continue;

                if (p.getLocation().distance(sender.getLocation()) > channel.getRadius()) continue;
            }

            p.sendMessage(msg);
        }
    }

    public ChatChannel createChannel(String channelName, String permission, int radius) {
        return new ChatChannel(channelName, permission, radius);
    }
    public void deleteChannel(String channelName) {
        channels.remove(channelName.toLowerCase(Locale.ROOT));
    }

    public ChatChannel getChannelByPlayer(Player player) {
        for (ChatChannel channel : channels.values()) {
            if (channel.getMembers().contains(player)) {
                return channel;
            }
        }
        return null; // Palauta null vasta, kun KAIKKI kanavat on katsottu
    }

    public void saveChannels() {
        ConfigurationSection section = config.getConfigurationSection("channels");
        if(section == null) {
            section = config.createSection("channels");
        }

        for(Map.Entry<String, ChatChannel> entry : channels.entrySet()) {
            String key = entry.getKey();
            ChatChannel channel = entry.getValue();

            section.set(key + ".permission", channel.getPermission());
            section.set(key + ".radius", channel.getRadius());
        }

        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save channels.yml: " + e.getMessage());
        }

        if(ConfigManager.getInstance().isDebug()) {
            plugin.getLogger().info("[DEBUG] " + "Saved channels!");
        }
    }
    public void loadChannels() {
        channels.clear();

        // Luodaan global aina uudestaan, jotta se on olemassa vaikka tiedosto olisi tyhjä
        register(createChannel("global", null, -1));

        ConfigurationSection section = config.getConfigurationSection("channels");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            // Huom: varmista että haet radiuksen oikeasta polusta
            int radius = section.getInt(key + ".radius", -1);
            String permission = section.getString(key + ".permission");
            ChatChannel channel = createChannel(key, permission, radius);
            register(channel);
        }
    }
}
