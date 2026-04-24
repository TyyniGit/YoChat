package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yoChatPlugin.objects.MutedPlayer;
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatManager {
    @Getter
    @Setter
    private Chat vaultChat = null;

    @Getter
    private Pattern blockedPattern;
    private final YoChat plugin;
    private final ConfigManager config = ConfigManager.getInstance();
    private final MessageParseManager mpm = YoChatAPI.getPlugin().getMessageParseManager();

    public ChatManager(YoChat plugin) {
        this.plugin = plugin;

        if (setupChat()) {
            if (ConfigManager.getInstance().isDebug()) {
                plugin.getLogger().info("Vault chat hook enabled!");
            }
        }
    }

    private boolean setupChat() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp == null) {
            return false;
        }

        setVaultChat(rsp.getProvider());
        return true;
    }

    private String processFormat(String format, @Nullable Player player, Map<String, String> placeholders) {
        String finalLine = format;

        String prefix = "";
        String suffix = "";
        String name = (player != null) ? player.getName() : "Unknown";

        if (player != null) {
            if (config.isUseLuckPerms()) {
                prefix = getLuckPermsPrefix(player);
                suffix = getLuckPermsSuffix(player);
            }

            if (config.isUseVault() && (prefix.isEmpty() || suffix.isEmpty())) {
                if (vaultChat != null) {
                    if (prefix.isEmpty()) prefix = vaultChat.getPlayerPrefix(player);
                    if (suffix.isEmpty()) suffix = vaultChat.getPlayerSuffix(player);
                }
            }

            if (prefix.isEmpty() && suffix.isEmpty()) {
                prefix = "";
                suffix = "";

                plugin.getLogger().warning("No prefix or suffix provided!");
            }
        }

        finalLine = finalLine.replace("{prefix}", prefix)
                .replace("{suffix}", suffix)
                .replace("{player}", name);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                finalLine = finalLine.replace(entry.getKey(), entry.getValue());
            }
        }

        if (config.isUsePlaceholderAPI() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            finalLine = PlaceholderAPI.setPlaceholders(player, finalLine);
        }

        if (config.isDebug()) {
            plugin.getLogger().info("DEBUG: Full line: " + finalLine);
        }

        return finalLine;
    }

    public Component formatMessage(Player sender, Component message) {
        String rawText = PlainTextComponentSerializer.plainText().serialize(message);

        Component parsedUserMessage = mpm.parse(sender, rawText);

        String formatted = processFormat(config.getChatFormat(), sender, Map.of("{message}", "<msg>"));

        return mpm.parseAdmin(formatted, Placeholder.component("msg", parsedUserMessage));
    }

    public Component formatChannelMessage(ChatChannel channel, Player sender, Component message) {
        String rawText = PlainTextComponentSerializer.plainText().serialize(message);

        Component parsedUserMessage = mpm.parse(sender, rawText);

        String format = (channel.getFormat() != null && !channel.getFormat().isEmpty()) ? channel.getFormat() : config.getChannelFormat();

        Map<String, String> placeholders = Map.of(
                "{channel}", channel.getName(),
                "{message}", "<msg>"
        );

        String processedFormat = processFormat(format, sender, placeholders);

        return mpm.parseAdmin(processedFormat, Placeholder.component("msg", parsedUserMessage));
    }

    public Component formatMessage(String format, Player sender, String blockedword) {
        return mpm.parseAdmin(processFormat(format, sender, Map.of("{blockedword}", blockedword)));
    }

    public String formatMention(String format, Player receiver, Player mentioner) {
        String mentionerprefix = "";
        String mentionersuffix = "";

        if (config.isUseLuckPerms()) {
            mentionerprefix = getLuckPermsPrefix(mentioner);
            mentionersuffix = getLuckPermsSuffix(mentioner);
        }

        if (config.isUseVault() && (mentionerprefix.isEmpty() || mentionersuffix.isEmpty())) {
            if (vaultChat != null) {
                if (mentionerprefix.isEmpty()) mentionerprefix = vaultChat.getPlayerPrefix(mentioner);
                if (mentionersuffix.isEmpty()) mentionersuffix = vaultChat.getPlayerSuffix(mentioner);
            }
        }

        Map<String, String> placeholders = Map.of(
                "{name}", receiver.getName(),
                "{mentionername}", mentioner.getName(),
                "{mentionersuffix}", mentionersuffix,
                "{mentionerprefix}", mentionerprefix
        );

        return processFormat(format, receiver, placeholders);
    }

    public Component formatMuteMessage(String format, Player sender) {
        MutedPlayer mp = YoChatAPI.getMutedPlayer(sender.getUniqueId());
        Map<String, String> placeholders = null;
        if (mp != null) {
            placeholders = Map.of(
                    "{reason}", mp.getReason() != null ? mp.getReason() : "No reason",
                    "{duration}", parseLong(mp.getDuration()),
                    "{punisher}", mp.getPunisher(),
                    "{whenstarted}", getFormattedDate(mp.whenStarted()),
                    "{timeleft}", getRemainingTime(mp.whenStarted(), mp.getDuration())
            );
        }
        return mpm.parseAdmin(processFormat(format, sender, placeholders));
    }

    public Component formatYouGotMutedMessage(String format, String punisher, OfflinePlayer target, String duration, @Nullable String reason) {
        Map<String, String> placeholders = Map.of(
                "{reason}", reason != null ? reason : "No reason",
                "{duration}", duration,
                "{punisher}", punisher
        );
        return mpm.parseAdmin(processFormat(format, target.getPlayer(), placeholders));
    }

    public Component formatYouGotUnmutedMessage(String format, String pardoner, OfflinePlayer target, @Nullable String reason) {
        Map<String, String> placeholders = Map.of(
                "{reason}", reason != null ? reason : "No reason",
                "{pardoner}", pardoner
        );
        return mpm.parseAdmin(processFormat(format, target.getPlayer(), placeholders));
    }

    public Component formatTimeEndedMessage(String format, Player player) {
        return mpm.parseAdmin(processFormat(format, player, Map.of()));
    }

    public void broadcast(Component message, Player sender) {
        String rawText = PlainTextComponentSerializer.plainText().serialize(message);
        String mentionFormat = ConfigManager.getInstance().getMentioningFormat();

        for (Player player : Bukkit.getOnlinePlayers()) {

            String finalContent = rawText;

            if (containsName(player, rawText)) {
                String replacement = formatMention(mentionFormat, player, sender);
                finalContent = rawText.replaceAll("(?i)" + Pattern.quote(player.getName()), replacement);

                if(ConfigManager.getInstance().isUseSound()) {
                    player.playSound(player.getLocation(), ConfigManager.getInstance().getSound(), ConfigManager.getInstance().getSoundVolume(), ConfigManager.getInstance().getSoundPitch());
                }
            }

            player.sendMessage(formatMessage(sender, Component.text(finalContent)));
        }

        Bukkit.getConsoleSender().sendMessage(formatMessage(sender, message));
    }

    private String getLuckPermsPrefix(Player player) {
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        return (user != null && user.getCachedData().getMetaData().getPrefix() != null) ? user.getCachedData().getMetaData().getPrefix() : "";
    }

    private String getLuckPermsSuffix(Player player) {
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        return (user != null && user.getCachedData().getMetaData().getSuffix() != null) ? user.getCachedData().getMetaData().getSuffix() : "";
    }

    public String parseLong(long duration) {
        if (duration == -1L) return "permanent";
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
        return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(whenStarted));
    }

    public String getRemainingTime(long whenStarted, long durationMillis) {
        if (durationMillis <= -1) return "Permanent";
        long remaining = (whenStarted + durationMillis) - System.currentTimeMillis();
        return remaining <= 0 ? "0s" : parseLong(remaining);
    }

    public void reloadBlockedWords() {
        List<String> words = ConfigManager.getInstance().getBlockedwords();
        if (words == null || words.isEmpty()) {
            this.blockedPattern = Pattern.compile("BLOCK_LIST_IS_EMPTY_DO_NOT_MATCH");
            return;
        }
        String regex = words.stream().filter(w -> w != null && !w.trim().isEmpty()).map(Pattern::quote).collect(Collectors.joining("|"));
        blockedPattern = Pattern.compile("\\b(" + regex + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    public long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) return 0L;

        String unit = duration.replaceAll("\\d+", "").toLowerCase();
        String amountStr = duration.replaceAll("[a-zA-Z]+", "");

        if (amountStr.isEmpty()) return 0L;
        long amount = Long.parseLong(amountStr);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future;

        return switch (unit) {
            case "s" -> amount * 1000L;
            case "m" -> amount * 60000L;
            case "h" -> amount * 3600000L;
            case "d" -> amount * 86400000L;
            case "w" -> amount * 604800000L;

            case "mo" -> {
                future = now.plusMonths(amount);
                yield ChronoUnit.MILLIS.between(now, future);
            }
            case "y" -> {
                future = now.plusYears(amount);
                yield ChronoUnit.MILLIS.between(now, future);
            }
            default -> 0L;
        };
    }

    public boolean containsName(Player player, String text) {
        return text.toLowerCase(Locale.ROOT).contains(player.getName().toLowerCase(Locale.ROOT));
    }
}