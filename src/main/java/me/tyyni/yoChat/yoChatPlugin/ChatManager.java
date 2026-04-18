package me.tyyni.yoChat.yoChatPlugin;

import me.clip.placeholderapi.PlaceholderAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChatManager {

    private final YoChat plugin;
    public ChatManager(YoChat plugin) {
        this.plugin = plugin;
    }

    /**
    * Message formatting for non channel messages
     * @param sender Player who send the message
     * @param message The sent message to format
     * @return Component
    **/
    public Component formatMessage(Player sender, Component message) {
        ConfigManager config = ConfigManager.getInstance();

        // 1. Haetaan raakatekstit (String-muodossa)
        String prefix = "";
        String suffix = "";
        if (config.isUseLuckPerms()) {
            prefix = getLuckPermsPrefix(sender);
            suffix = getLuckPermsSuffix(sender);
        }


        String rawMsgText = PlainTextComponentSerializer.plainText().serialize(message);

        String format = config.getChannelFormat();

        String fullLine = format
                .replace("{prefix}", prefix)
                .replace("{player}", sender.getName())
                .replace("{suffix}", suffix)
                .replace("{message}", rawMsgText);

        if (config.isUsePlaceholderAPI() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            fullLine = PlaceholderAPI.setPlaceholders(sender, fullLine);
        }

        if (config.isDebug()) {
            plugin.getLogger().info("DEBUG: Full line before parse: " + fullLine);
        }

        return config.parse(fullLine);
    }

    /**
     * Help method for sending messages to all players on the server
     * @param message The message to send
     */
    public void broadcast(Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    /**
     * Message formatting for channel messages
     * @param channel The channel the message was sent to
     * @param sender Player who send the message
     * @param message The sent message to format
     * @return Component
     */
    public Component formatChannelMessage(ChatChannel channel, Player sender, Component message) {
        ConfigManager config = ConfigManager.getInstance();

        String prefix = "";
        String suffix = "";
        if (config.isUseLuckPerms()) {
          prefix = getLuckPermsPrefix(sender);
          suffix = getLuckPermsSuffix(sender);
        }

        String rawMsgText = PlainTextComponentSerializer.plainText().serialize(message);

        String format = config.getChannelFormat();

        String fullLine = format
                .replace("{prefix}", prefix)
                .replace("{player}", sender.getName())
                .replace("{suffix}", suffix)
                .replace("{channel}", channel.getName())
                .replace("{message}", rawMsgText);


        if (config.isUsePlaceholderAPI() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            fullLine = PlaceholderAPI.setPlaceholders(sender, fullLine);
        }

        if (config.isDebug()) {
            plugin.getLogger().info("DEBUG: Full line before parse: " + fullLine);
        }

        return config.parse(fullLine);
    }

    /**
     * Method for finding the LuckPerms prefix of a specific player
     * @param player The player whose prefix the method tries to find
     * @return String
     */
    private String getLuckPermsPrefix(Player player) {
        LuckPerms lp = LuckPermsProvider.get();
        User user = lp.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            return user.getCachedData().getMetaData().getPrefix() != null ? user.getCachedData().getMetaData().getPrefix() : "";
        }

        return "";
    }

    /**
     * Method for finding the LuckPerms suffix of a specific player
     * @param player The player whose suffix the method tries to find
     * @return String
     */
    private String getLuckPermsSuffix(Player player) {
        LuckPerms lp = LuckPermsProvider.get();
        User user = lp.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            return user.getCachedData().getMetaData().getSuffix() != null ? user.getCachedData().getMetaData().getSuffix() : "";
        }

        return "";
    }
}
