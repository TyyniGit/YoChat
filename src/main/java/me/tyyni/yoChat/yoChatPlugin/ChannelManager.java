package me.tyyni.yoChat.yoChatPlugin;

import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChannelManager {
    private final Map<String, ChatChannel> channels = new HashMap<>();

    public ChannelManager() {
        createChannel("global", null, -1);
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
        Component msg = Component.text("[" + channel.getName() + "] ")
                .append(Component.text(sender.getName() + ": " + message));

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

    public void createChannel(String channelName, String permission, int radius) {
        ChatChannel channel = new ChatChannel(channelName, permission, radius);
        register(channel);

    }
    public void deleteChannel(String channelName) {
        channels.remove(channelName.toLowerCase(Locale.ROOT));
    }

    public ChatChannel getChannelByPlayer(Player player) {
        for(Map.Entry<String, ChatChannel> entry : channels.entrySet()) {
            return entry.getValue().getMembers().contains(player) ? entry.getValue() : null;
        }
        return null;
    }
}
