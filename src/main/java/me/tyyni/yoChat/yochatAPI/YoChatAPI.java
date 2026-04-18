package me.tyyni.yoChat.yochatAPI;

import lombok.Getter;
import lombok.Setter;
import me.tyyni.yoChat.yoChatPlugin.YoChat;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yochatAPI.interfaces.YoChatProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class YoChatAPI {

    @Setter
    private static YoChatProvider provider;

    @Getter
    private static YoChat instance;

    public static void setPrefix(Player player, String prefix) {
       provider.getPrefixManager().setPrefix(player, prefix);
    }

    public static String getPrefix(Player player) {
        return provider.getPrefixManager().getPrefix(player);
    }

    public static void sendGlobalMessage(Component message) {
        provider.getChatManager().broadcast(message);
    }

    public static void registerChannel(ChatChannel channel) {
       provider.getChannelManager().register(channel);
    }

    public static String getSuffix(Player player) {
        return provider.getSuffixManager().getSuffix(player);
    }

    public static void setSuffix(Player player, String suffix) {
        provider.getSuffixManager().setSuffix(player, suffix);
    }

}
