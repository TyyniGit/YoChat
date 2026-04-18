package me.tyyni.yoChat.yoChatPlugin;

import me.clip.placeholderapi.PlaceholderAPI;
import me.tyyni.yoChat.yochatAPI.YoChatAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChatManager {

    public Component formatMessage(Player player, Component message) {

        MiniMessage mm = MiniMessage.miniMessage();

        String format = ConfigManager.getInstance().getChatFormat();

        String prefix = "";
        String suffix = "";

        // 🔹 LuckPerms
        if (ConfigManager.getInstance().isUseLuckPerms()) {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());

            if (user != null) {
                String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                String lpSuffix = user.getCachedData().getMetaData().getSuffix();

                prefix = lpPrefix != null ? lpPrefix : YoChatAPI.getPrefix(player);
                suffix = lpSuffix != null ? lpSuffix : YoChatAPI.getSuffix(player);
            }
        }

        // 🔥 TÄRKEIN MUUTOS:
        // Component → MiniMessage string (EI plain text!)
        String msg = mm.serialize(message);

        // 🔹 PlaceholderAPI
        if (ConfigManager.getInstance().isUsePlaceholderAPI()) {
            msg = PlaceholderAPI.setPlaceholders(player, msg);
            prefix = PlaceholderAPI.setPlaceholders(player, prefix);
            suffix = PlaceholderAPI.setPlaceholders(player, suffix);
            format = PlaceholderAPI.setPlaceholders(player, format);
        }

        // 🔹 Build string
        String finalMessage = format
                .replace("{player}", player.getName())
                .replace("{message}", msg)
                .replace("{prefix}", prefix)
                .replace("{suffix}", suffix);

        // 🔥 takaisin Componentiksi
        return mm.deserialize(finalMessage);
    }

    public void broadcast(Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }
}
