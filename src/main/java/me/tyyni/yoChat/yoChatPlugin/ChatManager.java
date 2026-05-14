package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yoChatPlugin.objects.MutedPlayer;
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
import java.util.HashMap;
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
    private final ConfigManager config = ConfigManager.getInstance();
    private final MessageParseManager mpm = YoChatAPI.getPlugin().getMessageParseManager();
    private static final String PREFIX_TAG = "yochat_prefix";
    private static final String SUFFIX_TAG = "yochat_suffix";
    private static final String PLAYER_TAG = "yochat_player";
    private static final String CHANNEL_TAG = "yochat_channel";
    private static final String MESSAGE_TAG = "yochat_message";
    private static final String MENTIONER_PREFIX_TAG = "yochat_mentioner_prefix";
    private static final String MENTIONER_SUFFIX_TAG = "yochat_mentioner_suffix";
    private static final String MENTIONER_NAME_TAG = "yochat_mentioner_name";
    private static final String MESSAGE_PLACEHOLDER = "{message}";
    private static final List<String> FLOWING_HEADER_PLACEHOLDERS = List.of("{channel}", "{prefix}", "{player}", "{suffix}");

    public ChatManager(YoChat plugin) {
        if (setupChat()) {
            if (ConfigManager.getInstance().isDebug()) {
                plugin.getLogger().info("Vault chat hook enabled!");
            }
        }
    }

    private boolean setupChat() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            config.debug("Vault plugin not found, Vault chat hook disabled");
            return false;
        }

        RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp == null) {
            config.debug("Vault plugin found but no Chat service registration was available");
            return false;
        }

        setVaultChat(rsp.getProvider());
        config.debug("Vault chat provider hooked: %s", rsp.getProvider().getClass().getName());
        return true;
    }

    private PreparedFormat processFormat(String format, @Nullable Player player, Map<String, String> placeholders) {
        String finalLine = format;

        PlayerFormatValues playerValues = resolvePlayerFormatValues(player);
        String prefix = playerValues.prefix();
        String suffix = playerValues.suffix();
        String name = playerValues.name();

        finalLine = finalLine.replace("{prefix}", "<" + PREFIX_TAG + ">")
                .replace("{suffix}", "<" + SUFFIX_TAG + ">")
                .replace("{player}", "<" + PLAYER_TAG + ">");

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                finalLine = finalLine.replace(entry.getKey(), entry.getValue());
            }
        }

        if (player != null && config.isUsePlaceholderAPI() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            finalLine = PlaceholderAPI.setPlaceholders(player, finalLine);
            config.debug("Applied PlaceholderAPI placeholders for %s", player.getName());
        }

        config.debug("Processed format for player=%s template='%s' result='%s'",
                player != null ? player.getName() : "null", format, finalLine);

        TagResolver resolver = TagResolver.builder()
                .resolver(Placeholder.component(PREFIX_TAG, mpm.parseAdmin(prefix)))
                .resolver(Placeholder.component(SUFFIX_TAG, mpm.parseAdmin(suffix)))
                .resolver(Placeholder.component(PLAYER_TAG, Component.text(name)))
                .build();

        return new PreparedFormat(finalLine, resolver);
    }

    private boolean isValid(String str) {
        return str != null && !str.isEmpty();
    }

    public Component formatMessage(Player sender, Component message) {
        return formatStructuredChatMessage(config.getChatFormat(), sender, null, message);
    }

    public Component formatChannelFormat(ChatChannel channel) {
        return mpm.parseAdmin(channel.getFormat());
    }

    public Component formatChannelMessage(ChatChannel channel, Player sender, Component message) {
        String format = (channel.getFormat() != null && !channel.getFormat().isEmpty()) ? channel.getFormat() : config.getChannelFormat();
        return formatStructuredChatMessage(format, sender, channel.getName(), message);
    }

    public Component formatMessage(String format, Player sender, String blockedword) {
        PreparedFormat prepared = processFormat(format, sender, Map.of("{blockedword}", blockedword));
        return mpm.parseAdmin(prepared.template(), prepared.resolver());
    }

    public Component formatMention(String format, Player receiver, Player mentioner) {
        String mentionerprefix = "";
        String mentionersuffix = "";

        if (config.getPriorityOrder().contains("LuckPerms")) {
            mentionerprefix = getLuckPermsPrefix(mentioner);
            mentionersuffix = getLuckPermsSuffix(mentioner);
        }

        if (config.getPriorityOrder().contains("Vault") && (mentionerprefix.isEmpty() || mentionersuffix.isEmpty())) {
            if (vaultChat != null) {
                if (mentionerprefix.isEmpty()) mentionerprefix = vaultChat.getPlayerPrefix(mentioner);
                if (mentionersuffix.isEmpty()) mentionersuffix = vaultChat.getPlayerSuffix(mentioner);
            }
        }

        if (config.isUsePlaceholderAPI() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            mentionerprefix = PlaceholderAPI.setPlaceholders(mentioner, mentionerprefix);
            mentionersuffix = PlaceholderAPI.setPlaceholders(mentioner, mentionersuffix);
        }

        PreparedFormat prepared = processFormat(format, receiver, Map.of(
                "{name}", receiver.getName(),
                "{mentionername}", "<" + MENTIONER_NAME_TAG + ">",
                "{mentionersuffix}", "<" + MENTIONER_SUFFIX_TAG + ">",
                "{mentionerprefix}", "<" + MENTIONER_PREFIX_TAG + ">"
        ));

        return mpm.parseAdmin(prepared.template(),
                prepared.resolver(),
                Placeholder.component(MENTIONER_NAME_TAG, Component.text(mentioner.getName())),
                Placeholder.component(MENTIONER_PREFIX_TAG, mpm.parseAdmin(mentionerprefix)),
                Placeholder.component(MENTIONER_SUFFIX_TAG, mpm.parseAdmin(mentionersuffix)));
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
        PreparedFormat prepared = processFormat(format, sender, placeholders);
        return mpm.parseAdmin(prepared.template(), prepared.resolver());
    }

    public Component formatYouGotMutedMessage(String format, String punisher, OfflinePlayer target, String duration, @Nullable String reason) {
        Map<String, String> placeholders = Map.of(
                "{reason}", reason != null ? reason : "No reason",
                "{duration}", duration,
                "{punisher}", punisher
        );
        PreparedFormat prepared = processFormat(format, target.getPlayer(), placeholders);
        return mpm.parseAdmin(prepared.template(), prepared.resolver());
    }

    public Component formatYouGotUnmutedMessage(String format, String pardoner, OfflinePlayer target, @Nullable String reason) {
        Map<String, String> placeholders = Map.of(
                "{reason}", reason != null ? reason : "No reason",
                "{pardoner}", pardoner
        );
        PreparedFormat prepared = processFormat(format, target.getPlayer(), placeholders);
        return mpm.parseAdmin(prepared.template(), prepared.resolver());
    }

    public Component formatTimeEndedMessage(String format, Player player) {
        PreparedFormat prepared = processFormat(format, player, Map.of());
        return mpm.parseAdmin(prepared.template(), prepared.resolver());
    }

    private Component formatStructuredChatMessage(String format, Player sender, @Nullable String channelName, Component message) {
        StructuredMessageFormat structuredFormat = splitStructuredMessageFormat(format);
        if (structuredFormat == null) {
            Map<String, String> placeholders = channelName == null
                    ? Map.of(MESSAGE_PLACEHOLDER, "<" + MESSAGE_TAG + ">")
                    : Map.of(
                    "{channel}", "<" + CHANNEL_TAG + ">",
                    MESSAGE_PLACEHOLDER, "<" + MESSAGE_TAG + ">"
            );

            PreparedFormat prepared = processFormat(format, sender, placeholders);
            TagResolver.Builder resolverBuilder = TagResolver.builder()
                    .resolver(prepared.resolver())
                    .resolver(Placeholder.component(MESSAGE_TAG, message));

            if (channelName != null) {
                resolverBuilder.resolver(Placeholder.component(CHANNEL_TAG, Component.text(channelName)));
            }

            return mpm.parseAdmin(prepared.template(), resolverBuilder.build());
        }

        Map<String, String> rawValues = buildRawFormatValues(sender, channelName);
        Component rendered = Component.empty();

        if (!structuredFormat.headerTemplate().isEmpty()) {
            rendered = rendered.append(mpm.parseAdmin(applyRawFormatValues(structuredFormat.headerTemplate(), sender, rawValues)));
        }
        if (!structuredFormat.separatorTemplate().isEmpty()) {
            rendered = rendered.append(mpm.parseAdmin(applyRawFormatValues(structuredFormat.separatorTemplate(), sender, rawValues)));
        }

        rendered = rendered.append(message);

        if (!structuredFormat.tailTemplate().isEmpty()) {
            rendered = rendered.append(mpm.parseAdmin(applyRawFormatValues(structuredFormat.tailTemplate(), sender, rawValues)));
        }

        return rendered;
    }

    private @Nullable StructuredMessageFormat splitStructuredMessageFormat(String format) {
        int messageIndex = format.indexOf(MESSAGE_PLACEHOLDER);
        if (messageIndex < 0) {
            return null;
        }

        int lastPlaceholderIndex = -1;
        String lastPlaceholder = null;
        for (String candidate : FLOWING_HEADER_PLACEHOLDERS) {
            int candidateIndex = format.lastIndexOf(candidate, messageIndex - 1);
            if (candidateIndex > lastPlaceholderIndex) {
                lastPlaceholderIndex = candidateIndex;
                lastPlaceholder = candidate;
            }
        }

        if (lastPlaceholderIndex < 0) {
            return null;
        }

        int headerEnd = lastPlaceholderIndex + lastPlaceholder.length();
        return new StructuredMessageFormat(
                format.substring(0, headerEnd),
                format.substring(headerEnd, messageIndex),
                format.substring(messageIndex + MESSAGE_PLACEHOLDER.length())
        );
    }

    private Map<String, String> buildRawFormatValues(@Nullable Player player, @Nullable String channelName) {
        PlayerFormatValues playerValues = resolvePlayerFormatValues(player);
        Map<String, String> rawValues = new HashMap<>();
        rawValues.put("{prefix}", playerValues.prefix());
        rawValues.put("{suffix}", playerValues.suffix());
        rawValues.put("{player}", playerValues.name());
        if (channelName != null) {
            rawValues.put("{channel}", channelName);
        }
        return rawValues;
    }

    private String applyRawFormatValues(String input, @Nullable Player player, Map<String, String> rawValues) {
        String resolved = input;
        for (Map.Entry<String, String> entry : rawValues.entrySet()) {
            resolved = resolved.replace(entry.getKey(), entry.getValue());
        }

        if (player != null && config.isUsePlaceholderAPI() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            resolved = PlaceholderAPI.setPlaceholders(player, resolved);
        }

        return resolved;
    }

    private PlayerFormatValues resolvePlayerFormatValues(@Nullable Player player) {
        String prefix = "";
        String suffix = "";
        String name = (player != null) ? player.getName() : "Unknown";

        if (player != null) {
            for (String provider : config.getPriorityOrder()) {
                String foundPrefix = "";
                String foundSuffix = "";

                switch (provider.toUpperCase()) {
                    case "LUCKPERMS":
                        if (config.getPriorityOrder().contains("LuckPerms")) {
                            foundPrefix = getLuckPermsPrefix(player);
                            foundSuffix = getLuckPermsSuffix(player);
                        }
                        break;
                    case "VAULT":
                        if (config.getPriorityOrder().contains("Vault") && vaultChat != null) {
                            foundPrefix = vaultChat.getPlayerPrefix(player);
                            foundSuffix = vaultChat.getPlayerSuffix(player);
                        }
                        break;
                    case "YOCHAT":
                        foundPrefix = YoChatAPI.getPrefix(player);
                        foundSuffix = YoChatAPI.getSuffix(player);
                        break;
                }

                if (isValid(foundPrefix) || isValid(foundSuffix)) {
                    prefix = (foundPrefix != null) ? foundPrefix : "";
                    suffix = (foundSuffix != null) ? foundSuffix : "";
                    config.debug("Meta found via %s for %s", provider, player.getName());
                    break;
                }
            }
        }

        if (player != null && config.isUsePlaceholderAPI() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            prefix = PlaceholderAPI.setPlaceholders(player, prefix);
            suffix = PlaceholderAPI.setPlaceholders(player, suffix);
            name = PlaceholderAPI.setPlaceholders(player, name);
        }

        return new PlayerFormatValues(prefix, suffix, name);
    }

    public void broadcast(Component message, Player sender) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatMessage(sender, applyMentionFormatting(sender, player, message)));
        }

        Bukkit.getConsoleSender().sendMessage(formatMessage(sender, message));
    }

    public Component applyMentionFormatting(Player sender, @Nullable Player viewer, Component message) {
        String rawText = PlainTextComponentSerializer.plainText().serialize(message);

        if (viewer != null && config.isUseMentioning() && containsName(viewer, rawText)) {
            Component replacement = formatMention(config.getMentioningFormat(), viewer, sender);
            config.debug("Applied mention formatting: sender=%s viewer=%s message='%s'",
                    sender.getName(), viewer.getName(), rawText);
            return message.replaceText(TextReplacementConfig.builder()
                    .match("(?i)" + Pattern.quote(viewer.getName()))
                    .replacement(replacement)
                    .build());
        }

        return message;
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
            config.debug("Blocked words list is empty");
            return;
        }
        String regex = words.stream().filter(w -> w != null && !w.trim().isEmpty()).map(Pattern::quote).collect(Collectors.joining("|"));
        if (regex.isEmpty()) {
            this.blockedPattern = Pattern.compile("BLOCK_LIST_IS_EMPTY_DO_NOT_MATCH");
            config.debug("All blocked words were blank after filtering, pattern disabled");
            return;
        }
        blockedPattern = Pattern.compile("\\b(" + regex + ")\\b", Pattern.CASE_INSENSITIVE);
        config.debug("Reloaded blocked words pattern with %d entries", words.size());
    }

    public long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) return 0L;

        String unit = duration.replaceAll("\\d+", "").toLowerCase();
        String amountStr = duration.replaceAll("[a-zA-Z]+", "");

        if (amountStr.isEmpty()) return 0L;
        long amount = Long.parseLong(amountStr);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future;

        long parsed = switch (unit) {
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
        config.debug("Parsed duration input='%s' -> %d ms", duration, parsed);
        return parsed;
    }

    public boolean containsName(Player player, String text) {
        return text.toLowerCase(Locale.ROOT).contains(player.getName().toLowerCase(Locale.ROOT));
    }

    private record PreparedFormat(String template, TagResolver resolver) {
    }

    private record StructuredMessageFormat(String headerTemplate, String separatorTemplate, String tailTemplate) {
    }

    private record PlayerFormatValues(String prefix, String suffix, String name) {
    }

}
