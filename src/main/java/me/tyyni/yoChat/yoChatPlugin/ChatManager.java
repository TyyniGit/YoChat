package me.tyyni.yoChat.yoChatPlugin;

import me.clip.placeholderapi.PlaceholderAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChatManager {

    public Component formatMessage(Player sender, Component message) {
        ConfigManager config = ConfigManager.getInstance();

        // 1. Haetaan formaatti ja varmistetaan tagit ( {tag} -> <tag> )
        String formatStr = config.getChannelFormat()
                .replace("{prefix}", "<prefix>")
                .replace("{player}", "<player>")
                .replace("{suffix}", "<suffix>")
                .replace("{message}", "<message>");

        // 2. PARSITAAN prefix ja suffix heti komponenteiksi
        // Tämä varmistaa, että &f tai &e toimii niiden sisällä
        String rawPrefix = "";
        String rawSuffix = "";
        if (config.isUseLuckPerms()) {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(sender.getUniqueId());
            if (user != null) {
                rawPrefix = user.getCachedData().getMetaData().getPrefix() != null ? user.getCachedData().getMetaData().getPrefix() : "";
                rawSuffix = user.getCachedData().getMetaData().getSuffix() != null ? user.getCachedData().getMetaData().getSuffix() : "";
            }
        }

        // 3. PAPI-käsittely RAUTATEKSTILLE (ennen komponentiksi muuttoa)
        if (config.isUsePlaceholderAPI()) {
            rawPrefix = PlaceholderAPI.setPlaceholders(sender, rawPrefix);
            rawSuffix = PlaceholderAPI.setPlaceholders(sender, rawSuffix);
            formatStr = PlaceholderAPI.setPlaceholders(sender, formatStr);
        }

        // 4. MUUTETAAN KOMPONENTEIKSI
        // Käytetään sun config.parse() -metodia, jotta & ja < > toimivat molemmat
        Component prefixComp = config.parse(rawPrefix);
        Component suffixComp = config.parse(rawSuffix);
        Component playerComp = Component.text(sender.getName()); // Pelaajan nimi

        // Viesti tekstinä (PAPI-tuki viestin sisällä)
        String rawMsgText = PlainTextComponentSerializer.plainText().serialize(message);
        if (config.isUsePlaceholderAPI()) {
            rawMsgText = PlaceholderAPI.setPlaceholders(sender, rawMsgText);
        }
        Component messageComp = Component.text(rawMsgText);

        MiniMessage flexibleMM = MiniMessage.builder()
                .tags(TagResolver.standard())
                .strict(false) // Tämä estää virheen, jos tekstissä on outoja merkkejä
                .build();

        return flexibleMM.deserialize(formatStr,
                Placeholder.component("prefix", prefixComp),
                Placeholder.component("player", playerComp),
                Placeholder.component("suffix", suffixComp),
                Placeholder.component("message", messageComp)
        );
    }

    public void broadcast(Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    public Component formatChannelMessage(ChatChannel channel, Player sender, Component message) {
        ConfigManager config = ConfigManager.getInstance();

        // 1. Haetaan formaatti ja varmistetaan tagit ( {tag} -> <tag> )
        String formatStr = config.getChannelFormat()
                .replace("{prefix}", "<prefix>")
                .replace("{player}", "<player>")
                .replace("{suffix}", "<suffix>")
                .replace("{channel}", "<channel>")
                .replace("{message}", "<message>");

        // 2. PARSITAAN prefix ja suffix heti komponenteiksi
        // Tämä varmistaa, että &f tai &e toimii niiden sisällä
        String rawPrefix = "";
        String rawSuffix = "";
        if (config.isUseLuckPerms()) {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(sender.getUniqueId());
            if (user != null) {
                rawPrefix = user.getCachedData().getMetaData().getPrefix() != null ? user.getCachedData().getMetaData().getPrefix() : "";
                rawSuffix = user.getCachedData().getMetaData().getSuffix() != null ? user.getCachedData().getMetaData().getSuffix() : "";
            }
        }

        // 3. PAPI-käsittely RAUTATEKSTILLE (ennen komponentiksi muuttoa)
        if (config.isUsePlaceholderAPI()) {
            rawPrefix = PlaceholderAPI.setPlaceholders(sender, rawPrefix);
            rawSuffix = PlaceholderAPI.setPlaceholders(sender, rawSuffix);
            formatStr = PlaceholderAPI.setPlaceholders(sender, formatStr);
        }

        // 4. MUUTETAAN KOMPONENTEIKSI
        // Käytetään sun config.parse() -metodia, jotta & ja < > toimivat molemmat
        Component prefixComp = config.parse(rawPrefix);
        Component suffixComp = config.parse(rawSuffix);
        Component playerComp = Component.text(sender.getName()); // Pelaajan nimi

        // Viesti tekstinä (PAPI-tuki viestin sisällä)
        String rawMsgText = PlainTextComponentSerializer.plainText().serialize(message);
        if (config.isUsePlaceholderAPI()) {
            rawMsgText = PlaceholderAPI.setPlaceholders(sender, rawMsgText);
        }
        Component messageComp = Component.text(rawMsgText);

        MiniMessage flexibleMM = MiniMessage.builder()
                .tags(TagResolver.standard())
                .strict(false) // Tämä estää virheen, jos tekstissä on outoja merkkejä
                .build();

        return flexibleMM.deserialize(formatStr,
                Placeholder.component("prefix", prefixComp),
                Placeholder.component("player", playerComp),
                Placeholder.component("suffix", suffixComp),
                Placeholder.component("message", messageComp),
                Placeholder.component("channel", Component.text(channel.getName()))

        );
    }
    private String getLuckPermsPrefix(Player player) {
        LuckPerms lp = LuckPermsProvider.get();
        User user = lp.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            return user.getCachedData().getMetaData().getPrefix() != null ? user.getCachedData().getMetaData().getPrefix() : "";
        }

        return "";
    }
}
