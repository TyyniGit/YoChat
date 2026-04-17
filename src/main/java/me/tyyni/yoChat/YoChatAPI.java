package me.tyyni.yoChat;

import lombok.Getter;
import me.tyyni.yoChat.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class YoChatAPI {

    @Getter
    private static YoChat instance;

    public static void init(YoChat plugin) {
        instance = plugin;

        if (instance == null) {
            throw new IllegalStateException("YoChat is not enabled!");
        }
    }

    public static void setPrefix(Player player, String prefix) {
        getInstance().getPrefixManager().setPrefix(player, prefix);
    }

    public static String getPrefix(Player player) {
        return getInstance().getPrefixManager().getPrefix(player);
    }

    public static void sendGlobalMessage(Component message) {
        getInstance().getChatManager().broadcast(message);
    }

    public static void registerChannel(ChatChannel channel) {
        getInstance().getChannelManager().register(channel);
    }
}
