package me.tyyni.yoChat.yoChatPlugin;

import me.tyyni.yoChat.yoChatAPI.events.YoChatChannelJoinEvent;
import me.tyyni.yoChat.yoChatAPI.events.YoChatChannelLeaveEvent;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelManager {
    private final Map<String, ChatChannel> channels = new ConcurrentHashMap<>();
    private final File file;
    private final FileConfiguration config;
    private final Plugin plugin;

    public ChannelManager(YoChat plugin) {
        this.plugin = plugin;
        ChatChannel channel = createChannel("global", -1, false, null);
        register(channel);

        file = new File(plugin.getDataFolder(), "channels.yml");
        if (!file.exists()) {
            plugin.saveResource("channels.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }
    public void register(ChatChannel channel) {
        channels.put(channel.getName().toLowerCase(Locale.ROOT), channel);
        if (ConfigManager.getInstance() != null) {
            ConfigManager.getInstance().debug("Registered channel %s", channel.getName());
        }
    }

    public ChatChannel getChannel(String name) {
        return channels.get(name.toLowerCase(Locale.ROOT));
    }

    public Map<String, ChatChannel> getChannelsList() {
        return channels;
    }

    public Collection<ChatChannel> getChannels() {
        return channels.values();
    }

    public void sendToChannel(ChatChannel channel, Player sender, Component message) {
        if (ConfigManager.getInstance() != null) {
            ConfigManager.getInstance().debug("Sending direct channel message to %s from %s", channel.getName(), sender.getName());
        }
        Set<String> worlds = channel.getWorlds();

        if (worlds != null && !worlds.isEmpty() && channel.isStrictWorld() && !worlds.contains(sender.getWorld().getName())) {
            if (ConfigManager.getInstance() != null) {
                ConfigManager.getInstance().debug("Blocked sendToChannel: sender %s is not in a valid world", sender.getName());
            }
            return;
        }

        Bukkit.getConsoleSender().sendMessage(YoChatAPI.getPlugin().getChatManager().formatChannelMessage(channel, sender, message));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if(!channel.getMembers().contains(p)) continue;
            if (!channel.canJoin(p)) continue;
            if (channel.getRadius() > 0) {
                if (!p.getWorld().equals(sender.getWorld())) continue;
                if (p.getLocation().distance(sender.getLocation()) > channel.getRadius()) continue;
            }

            if(worlds != null && !worlds.isEmpty()) {
                if(!worlds.contains(p.getWorld().getName())) continue;
            }

            Component viewerMessage = YoChatAPI.getPlugin().getChatManager().applyMentionFormatting(sender, p, message);
            p.sendMessage(YoChatAPI.getPlugin().getChatManager().formatChannelMessage(channel, sender, viewerMessage));
        }
    }

    public ChatChannel createChannel(String channelName, int radius, boolean strictWorld, @Nullable Set<String> worlds) {
        if (ConfigManager.getInstance() != null) {
            ConfigManager.getInstance().debug("Creating channel %s radius=%d strictWorld=%s worlds=%s", channelName, radius, strictWorld, worlds);
        }
        return new ChatChannel(channelName, radius, strictWorld, worlds);
    }
    public void deleteChannel(String channelName) {
        channels.remove(channelName.toLowerCase(Locale.ROOT));
        if (ConfigManager.getInstance() != null) {
            ConfigManager.getInstance().debug("Deleted channel %s", channelName);
        }
    }

    public ChatChannel getChannelByPlayer(Player player) {
        for (ChatChannel channel : getChannels()) {
            if (channel.getMembers().contains(player)) {
                return channel;
            }
        }
        ConfigManager.getInstance().debug("No channel found for player %s", player.getName());
        return null;
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
            section.set(key + ".strict-world", channel.isStrictWorld());
            section.set(key + ".worlds", channel.getWorlds());
        }

        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save channels.yml: " + e.getMessage());
        }

        if (ConfigManager.getInstance() != null) {
            ConfigManager.getInstance().debug("Saved %d channels", channels.size());
        }
    }
    public void loadChannels() {
        channels.clear();

        register(createChannel("global", -1, false, null));

        ConfigurationSection section = config.getConfigurationSection("channels");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            int radius = section.getInt(key + ".radius", -1);
            String permission = section.getString(key + ".permission", null);
            boolean strictWorld = section.getBoolean(key + ".strict-world", false);

            List<String> worldsList = section.getStringList(key + ".worlds");
            Set<String> worlds = new HashSet<>(worldsList);

            ChatChannel channel = createChannel(key, radius, strictWorld, worlds);
            channel.setPermission(permission);
            register(channel);
        }

        ConfigManager.getInstance().debug("Loaded %d channels", channels.size());
    }

    public void joinChannel(Player player, ChatChannel channel) {
        ChatChannel channelByPlayer = getChannelByPlayer(player);
        if(channelByPlayer != null) {
            YoChatChannelLeaveEvent leaveEvent = new YoChatChannelLeaveEvent(player, channelByPlayer);
            Bukkit.getPluginManager().callEvent(leaveEvent);
            if (leaveEvent.isCancelled()) return;

            channelByPlayer.removeMember(player);
            ConfigManager.getInstance().debug("Removed %s from channel %s", player.getName(), channelByPlayer.getName());
        }

        YoChatChannelJoinEvent joinEvent = new YoChatChannelJoinEvent(player, channel);
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) return;

        joinEvent.getChannel().addMember(player);
        ConfigManager.getInstance().debug("Joined %s to channel %s", player.getName(), joinEvent.getChannel().getName());
    }
}
