package me.tyyni.yoChat.yoChatPlugin;

import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import java.util.regex.Pattern;

public class ChannelManager {
    private final Map<String, ChatChannel> channels = new ConcurrentHashMap<>();
    private final File file;
    private final FileConfiguration config;
    private final Plugin plugin;

    public ChannelManager(YoChat plugin) {
        this.plugin = plugin;
        ChatChannel channel = createChannel("global", null, -1, false, null);
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
        String rawText = PlainTextComponentSerializer.plainText().serialize(message);
        String mentionFormat = ConfigManager.getInstance().getMentioningFormat();

        Bukkit.getConsoleSender().sendMessage(YoChatAPI.getInstance().getChatManager().formatChannelMessage(channel, sender, message));
        Set<String> worlds = channel.getWorlds();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if(!channel.getMembers().contains(p)) continue;
            if (!channel.canJoin(p)) continue;
            if (channel.getRadius() > 0) {
                if (!p.getWorld().equals(sender.getWorld())) continue;
                if (p.getLocation().distance(sender.getLocation()) > channel.getRadius()) continue;
            }

            if(worlds != null && !worlds.isEmpty()) {

                if(channel.isStrictWorld() && !worlds.contains(sender.getWorld().getName())) return;

                if(!worlds.contains(p.getWorld().getName())) continue;
            }

            String finalContent = rawText;

            if (YoChatAPI.getInstance().getChatManager().containsName(p, rawText)) {

                String replacement = YoChatAPI.getInstance().getChatManager().formatMention(mentionFormat, p, sender);
                finalContent = rawText.replaceAll("(?i)" + Pattern.quote(p.getName()), replacement);

                if(ConfigManager.getInstance().isUseSound()) {
                    p.playSound(p.getLocation(), ConfigManager.getInstance().getSound(), ConfigManager.getInstance().getSoundVolume(), ConfigManager.getInstance().getSoundPitch());
                }
            }

            p.sendMessage(YoChatAPI.getInstance().getChatManager().formatChannelMessage(channel, sender, Component.text(finalContent)));
        }
    }

    public ChatChannel createChannel(String channelName, String permission, int radius, boolean strictWorld, @Nullable Set<String> worlds) {
        return new ChatChannel(channelName, permission, radius, strictWorld, worlds);
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
            section.set(key + ".worlds", channel.getWorlds());
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

        register(createChannel("global", null, -1, false, null));

        ConfigurationSection section = config.getConfigurationSection("channels");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            int radius = section.getInt(key + ".radius", -1);
            String permission = section.getString(key + ".permission", null);
            boolean strictWorld = section.getBoolean(key + ".strict-world", false);

            List<String> worldsList = section.getStringList(key + ".worlds");
            Set<String> worlds = new HashSet<>(worldsList);

            ChatChannel channel = createChannel(key, permission, radius, strictWorld, worlds);
            register(channel);
        }
    }

    public void joinChannel(Player player, ChatChannel channel) {
        ChatChannel channelByPlayer = getChannelByPlayer(player);
        if(channelByPlayer != null) {
            channelByPlayer.removeMember(player);
        }

        channel.addMember(player);
    }
}
