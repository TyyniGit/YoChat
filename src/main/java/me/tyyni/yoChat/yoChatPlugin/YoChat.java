package me.tyyni.yoChat.yoChatPlugin;

import com.tchristofferson.configupdater.ConfigUpdater;
import lombok.Getter;
import lombok.Setter;
import me.tyyni.yoChat.yoChatPlugin.commands.YoChatCommand;
import me.tyyni.yoChat.yoChatPlugin.listeners.ChatListener;
import me.tyyni.yoChat.yoChatPlugin.listeners.PlayerJoinListener;
import me.tyyni.yoChat.yoChatPlugin.listeners.PlayerQuitListener;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatAPI.interfaces.YoChatProvider;
import me.tyyni.yoChat.yoChatPlugin.webhook.Discord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class YoChat extends JavaPlugin implements YoChatProvider {

    @Getter
    @Setter
    private Component YoChatPrefix;
    @Getter
    private final String alternativePrefix = "&#9863E7&lY&#9863E7&lo&#9863E7&lC&#9863E7&lh&#9863E7&la&#9863E7&lt #9863E7» ";
    @Getter
    private final TextColor mainColor = TextColor.fromHexString("#EBE7E4");
    @Getter
    private final TextColor highlightColor = TextColor.fromHexString("#9863E7");

    /*
    * private PrefixManager prefixManager;
    * private SuffixManager suffixManager;
    */
    private ChatManager chatManager;
    private ChannelManager channelManager;
    private ConfigManager configManager;
    private MuteManager muteManager;
    private MessageParseManager messageParseManager;
    @Getter
    private Discord discord;

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        YoChatAPI.setProvider(this);

        try {
            ConfigUpdater.update(this, "config.yml", new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            getLogger().severe("Failed to update config.yml" + e.getMessage());
        }

        reloadConfig();

        initializeManagers();

        messageParseManager.setupMM();
        channelManager.loadChannels();
        configManager.load();
        muteManager.load();
        muteManager.startMuteChecker();
        chatManager.reloadBlockedWords();

        registerEvents();
        registerCommands();

        sendHelloMessage();
    }

    @Override
    public void onDisable() {
        for(ChatChannel channel : channelManager.getChannels()) {
            for(Player player : channel.getMembers()) {
                channel.removeMember(player);
            }
        }

        channelManager.saveChannels();
        muteManager.save();
    }

    private void initializeManagers() {
        this.messageParseManager = new MessageParseManager();
        this.configManager = new ConfigManager(this);
        this.chatManager = new ChatManager(this);
        this.channelManager = new ChannelManager(this);
        this.muteManager = new MuteManager(this);
        this.discord = new Discord();
        /*
        * suffixManager = new SuffixManager();
        * prefixManager = new PrefixManager();
        */
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("yochat")).setExecutor(new YoChatCommand());
    }

    private void sendHelloMessage() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(YoChatPrefix.append(Component.text("Thanks for using my plugin! -Tyyni", getMainColor())));
        console.sendMessage(YoChatPrefix.append(
                Component.text("The plugin is still under development! ", NamedTextColor.YELLOW)
                        .append(Component.text("Report bugs on Discord: ", NamedTextColor.YELLOW))
                        .append(Component.text("@tyynilol", highlightColor))
        ));
    }

    @Override
    public YoChat getYoChat() {
        return this;
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
    public MuteManager getMuteManager() {return muteManager;}

    @Override
    public MessageParseManager getMessageParseManager() {return messageParseManager;}

    /* Prefix system might be implemented in the future.
     * @Override
     * public PrefixManager getPrefixManager() {
     *    return prefixManager;
     * }
     *
     * Suffix system might be implemented in the future.
     *  @Override
     *  public SuffixManager getSuffixManager() {
     *  return suffixManager;
     * }
     */
}
