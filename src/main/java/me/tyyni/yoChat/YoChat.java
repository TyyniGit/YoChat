package me.tyyni.yoChat;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

@Slf4j
public final class YoChat extends JavaPlugin {

    @Getter
    Component YoChatPrefix = MiniMessage.miniMessage().deserialize("<b><gradient:#9863E7:#9863E7>YoChat</gradient></b> ");
    @Getter
    String alternativePrefix = "&#9863E7&lY&#9863E7&lo&#9863E7&lC&#9863E7&lh&#9863E7&la&#9863E7&lt";

    @Getter
    private PrefixManager prefixManager;
    @Getter
    private ChatManager chatManager;
    @Getter
    private ChannelManager channelManager;

    @Override
    public void onEnable() {
        initializeManagers();

        registerEvents();

        sendHelloMessage();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void initializeManagers() {
        prefixManager = new PrefixManager();
        chatManager = new ChatManager();
        channelManager = new ChannelManager();

        YoChatAPI.init(this);
    }

    private void registerEvents() {
        // Register event listeners here
    }

    private void sendHelloMessage() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(YoChatPrefix.append(Component.text("Thanks for using my plugin! -Tyyni")));
    }
}
