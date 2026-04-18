package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.tyyni.yoChat.yoChatPlugin.commands.YoChatCommand;
import me.tyyni.yoChat.yoChatPlugin.listeners.ChatListener;
import me.tyyni.yoChat.yochatAPI.YoChatAPI;
import me.tyyni.yoChat.yochatAPI.interfaces.YoChatProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

@Slf4j
public final class YoChat extends JavaPlugin implements YoChatProvider {

    @Getter
    @Setter
    private Component YoChatPrefix;
    @Getter
    private final String alternativePrefix = "&#9863E7&lY&#9863E7&lo&#9863E7&lC&#9863E7&lh&#9863E7&la&#9863E7&lt&#9863E7&l:";
    @Getter
    public final TextColor mainColor = TextColor.fromHexString("#9863E7");

    private PrefixManager prefixManager;
    private ChatManager chatManager;
    private ChannelManager channelManager;
    private ConfigManager configManager;
    private SuffixManager suffixManager;

    @Override
    public void onEnable() {
        initializeManagers();

        registerEvents();

        registerCommands();

        sendHelloMessage();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                configManager.load();
                if (configManager.isDebug()) {
                    log.info("Managers loaded successfully.");
                }

                if(!configManager.isEnabled()) {
                    Bukkit.getPluginManager().disablePlugin(this);
                }
            } catch (Exception e) {
                log.error("Failed to load managers: ", e);
            }
        }, 10L);
    }

    @Override
    public void onDisable() {
        ConfigManager.getInstance().save();
    }

    private void initializeManagers() {
        prefixManager = new PrefixManager();
        chatManager = new ChatManager();
        channelManager = new ChannelManager();
        configManager = new ConfigManager(this);
        suffixManager = new SuffixManager();

    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("yochat")).setExecutor(new YoChatCommand());
    }

    private void sendHelloMessage() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(YoChatPrefix.append(Component.text("Thanks for using my plugin! -Tyyni")));
    }

    @Override
    public YoChat getYoChatAPI() {
        return YoChatAPI.getInstance();
    }

    @Override
    public PrefixManager getPrefixManager() {
        return prefixManager;
    }

    @Override
    public ChatManager getChatManager() {
        return chatManager;
    }

    @Override
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    @Override
    public SuffixManager getSuffixManager() {
        return suffixManager;
    }
}
