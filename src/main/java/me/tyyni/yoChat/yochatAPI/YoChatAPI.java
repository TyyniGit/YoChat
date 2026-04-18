package me.tyyni.yoChat.yochatAPI;

import lombok.NoArgsConstructor;
import lombok.Setter;
import me.tyyni.yoChat.yoChatPlugin.YoChat;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yochatAPI.interfaces.YoChatProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class YoChatAPI {

    @Setter
    private static YoChatProvider provider;

    public static YoChat getInstance() {
        checkProvider();
        return provider.getYoChat();
    }

    public static void setPrefix(Player player, String prefix) {
        checkProvider();
        provider.getPrefixManager().setPrefix(player, prefix);
    }

    public static String getPrefix(Player player) {
        checkProvider();
        return provider.getPrefixManager().getPrefix(player);
    }

    public static void sendGlobalMessage(Component message) {
        checkProvider();
        provider.getChatManager().broadcast(message);
    }

    public static void registerChannel(ChatChannel channel) {
        checkProvider();
        provider.getChannelManager().register(channel);
    }

    public static String getSuffix(Player player) {
        checkProvider();
        return provider.getSuffixManager().getSuffix(player);
    }

    public static void setSuffix(Player player, String suffix) {
        checkProvider();
        provider.getSuffixManager().setSuffix(player, suffix);
    }

    private static void checkProvider() {
        if (provider == null) {
            throw new IllegalStateException("YoChatProvider is not set! Please set it before using the API.");
        }
    }
}
