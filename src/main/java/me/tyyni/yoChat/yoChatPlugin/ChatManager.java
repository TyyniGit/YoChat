package me.tyyni.yoChat.yoChatPlugin;

import me.clip.placeholderapi.PlaceholderAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yoChatPlugin.objects.MutedPlayer;
import me.tyyni.yoChat.yochatAPI.YoChatAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ChatManager {

    private final YoChat plugin;
    public ChatManager(YoChat plugin) {
        this.plugin = plugin;
    }

    /**
    * Message formatting for non channel messages
     * @param sender Player who sent the message
     * @param message The sent message to format
     * @return Component
    **/
    public Component formatMessage(Player sender, Component message) {
        ConfigManager config = ConfigManager.getInstance();

        String prefix = "";
        String suffix = "";
        if (config.isUseLuckPerms()) {
            prefix = getLuckPermsPrefix(sender);
            suffix = getLuckPermsSuffix(sender);
        }

        if(prefix.isEmpty()) {
            prefix = YoChatAPI.getPrefix(sender);
            suffix = YoChatAPI.getSuffix(sender);
        }

        String rawMsgText = PlainTextComponentSerializer.plainText().serialize(message);

        String format = config.getChatFormat();

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

        Bukkit.getConsoleSender().sendMessage(message);
    }

    /**
     * Message formatting for channel messages
     * @param channel The channel the message was sent to
     * @param sender Player who sent the message
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

    /**
     * Message formatting for non channel messages
     * @param format The sent message to format
     * @param sender Player who sent the message
     * @param blockedword The word that got the message blocked
     * @return Component
     **/

    public Component formatMessage(String format, Player sender, String blockedword) {
        ConfigManager config = ConfigManager.getInstance();

        String prefix = "";
        String suffix = "";
        if (config.isUseLuckPerms()) {
            prefix = getLuckPermsPrefix(sender);
            suffix = getLuckPermsSuffix(sender);
        }


        String fullLine = format
                .replace("{prefix}", prefix)
                .replace("{player}", sender.getName())
                .replace("{suffix}", suffix)
                .replace("{blockedword}", blockedword);

        if (config.isUsePlaceholderAPI() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            fullLine = PlaceholderAPI.setPlaceholders(sender, fullLine);
        }

        if (config.isDebug()) {
            plugin.getLogger().info("DEBUG: Full line before parse: " + fullLine);
        }

        return config.parse(fullLine);
    }

    /**
     * Message formatting for the mute message
     * @param format The sent message to format
     * @param sender Player who sent the message
     * @return Component
     */
    public Component formatMuteMessage(String format, Player sender) {
        ConfigManager config = ConfigManager.getInstance();
        MutedPlayer mutedPlayer = YoChatAPI.getMutedPlayer(sender.getUniqueId());

        String prefix = "";
        String suffix = "";
        if (config.isUseLuckPerms()) {
            prefix = getLuckPermsPrefix(sender);
            suffix = getLuckPermsSuffix(sender);
        }


        String fullLine = format
                .replace("{prefix}", prefix)
                .replace("{player}", sender.getName())
                .replace("{suffix}", suffix)
                .replace("{reason}", mutedPlayer.getReason() != null ? mutedPlayer.getReason() : "No reason")
                .replace("{duration}", parseLong(mutedPlayer.getDuration()))
                .replace("{punisher}", mutedPlayer.getPunisher())
                .replace("{whenstarted}", getFormattedDate(mutedPlayer.whenStarted()))
                .replace("{timeleft}", getRemainingTime(mutedPlayer.whenStarted(), mutedPlayer.getDuration()));

        if (config.isUsePlaceholderAPI() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            fullLine = PlaceholderAPI.setPlaceholders(sender, fullLine);
        }

        if (config.isDebug()) {
            plugin.getLogger().info("DEBUG: Full line before parse: " + fullLine);
        }

        return config.parse(fullLine);
    }

    public long parseDuration(String duration) {
        String unit = duration.replaceAll("\\d+", "");
        long amount = Long.parseLong(duration.replaceAll("[a-zA-Z]+", ""));

        return switch (unit) {
            case "s" -> amount * 1000L;
            case "m" -> amount * 1000L * 60L;
            case "h" -> amount * 1000L * 3600L;
            case "d" -> amount * 1000L * 3600L * 24L;
            case "w" -> amount * 1000L * 3600L * 24L * 7L;
            case "mo" -> amount * 1000L * 3600L * 24L * 30L;
            case "y" -> amount * 1000L * 3600L * 24L * 365L;
            default -> 0L;
        };
    }

    public String parseLong(long duration) {
        if(duration == -1L) {
            return "permanent";
        }

        if (duration <= 0) return "0s";

        long seconds = (duration / 1000) % 60;
        long minutes = (duration / (1000 * 60)) % 60;
        long hours = (duration / (1000 * 60 * 60)) % 24;
        long days = (duration / (1000 * 60 * 60 * 24));

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    public String getFormattedDate(long whenStarted) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.systemDefault());

        return formatter.format(Instant.ofEpochMilli(whenStarted));
    }

    public String getRemainingTime(long whenStarted, long durationMillis) {
        if (durationMillis <= -1) {
            return "Permanent";
        }

        long expiryTime = whenStarted + durationMillis;
        long remainingMillis = expiryTime - System.currentTimeMillis();

        if (remainingMillis <= 0) {
            return "0s";
        }

        return parseLong(remainingMillis);
    }
}
