package me.tyyni.yoChat.yoChatPlugin;

import com.tchristofferson.configupdater.ConfigUpdater;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.tyyni.yoChat.yoChatPlugin.commands.YoChatCommand;
import me.tyyni.yoChat.yoChatPlugin.listeners.ChatListener;
import me.tyyni.yoChat.yoChatPlugin.listeners.PlayerJoinListener;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yochatAPI.YoChatAPI;
import me.tyyni.yoChat.yochatAPI.interfaces.YoChatProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Slf4j
public final class YoChat extends JavaPlugin implements YoChatProvider {

    @Getter
    @Setter
    private Component YoChatPrefix;
    @Getter
    private final String alternativePrefix = "&#9863E7&lY&#9863E7&lo&#9863E7&lC&#9863E7&lh&#9863E7&la&#9863E7&lt&#9863E7&l: ";
    @Getter
    public final TextColor mainColor = TextColor.fromHexString("#9863E7");

    private PrefixManager prefixManager;
    private ChatManager chatManager;
    private ChannelManager channelManager;
    private ConfigManager configManager;
    private SuffixManager suffixManager;

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            ConfigUpdater.update(this, "config.yml", new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            log.error("Failed to update config.yml", e);
        }

        reloadConfig();

        initializeManagers();

        YoChatAPI.setProvider(this);

        channelManager.loadChannels();
        configManager.load();

        registerEvents();
        registerCommands();

        sendHelloMessage();
    }

    @Override
    public void onDisable() {
        ConfigManager.getInstance().save();
        for(ChatChannel channel : channelManager.getChannels()) {
            for(Player player : channel.getMembers()) {
                channel.removeMember(player);
            }
        }

        channelManager.saveChannels();
    }

    private void initializeManagers() {
        prefixManager = new PrefixManager();
        chatManager = new ChatManager();
        channelManager = new ChannelManager(this);
        configManager = new ConfigManager(this);
        suffixManager = new SuffixManager();
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("yochat")).setExecutor(new YoChatCommand());
    }

    private void sendHelloMessage() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(YoChatPrefix.append(Component.text("Thanks for using my plugin! -Tyyni")));
    }

    @Override
    public YoChat getYoChat() {
        return this;
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
