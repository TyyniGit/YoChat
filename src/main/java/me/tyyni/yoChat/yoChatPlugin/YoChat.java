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

/**
 * Main plugin entry point and runtime service provider for YoChat.
 *
 * <p>This type is exposed through {@link YoChatAPI#getPlugin()} for integrations
 * that need direct access to the running plugin instance.</p>
 */
public final class YoChat extends JavaPlugin implements YoChatProvider {

    /** Parsed prefix component used in plugin-originated messages. */
    @Getter
    @Setter
    private Component YoChatPrefix;
    /** Fallback legacy-format prefix used when the configured prefix is missing. */
    @Getter
    private final String alternativePrefix = "&#9863E7&lY&#9863E7&lo&#9863E7&lC&#9863E7&lh&#9863E7&la&#9863E7&lt #9863E7» ";
    /** Primary neutral text color used by plugin messages. */
    @Getter
    private final TextColor mainColor = TextColor.fromHexString("#EBE7E4");
    /** Accent color used by plugin messages. */
    @Getter
    private final TextColor highlightColor = TextColor.fromHexString("#9863E7");

    private ChatManager chatManager;
    private ChannelManager channelManager;
    private ConfigManager configManager;
    private MuteManager muteManager;
    private MessageParseManager messageParseManager;
    private ChatPipelineManager chatPipelineManager;
    private PrefixManager prefixManager;
    private SuffixManager suffixManager;
    private ReplyManager replyManager;

    @Getter
    private Discord discord;

    @Override
    public void onLoad() {
        ensureDefaultResource("config.yml");
        ensureDefaultResource("channels.yml");
        ensureDefaultResource("mutedplayers.yml");
    }

    @Override
    public void onEnable() {
        try {
            ConfigUpdater.update(this, "config.yml", new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            getLogger().severe("Failed to update config.yml" + e.getMessage());
        }

        reloadConfig();

        configManager = new ConfigManager(this);
        channelManager = new ChannelManager(this);
        messageParseManager = new MessageParseManager();

        messageParseManager.setupMM();

        YoChatAPI.setProvider(this);

        channelManager.loadChannels();
        configManager.load();

        if (!configManager.isEnabled()) {
            getLogger().info("YoChat is disabled in config. Plugin loaded but not active.");
            return;
        }

        chatManager = new ChatManager(this);
        muteManager = new MuteManager(this);
        discord = new Discord();
        chatPipelineManager = new ChatPipelineManager();
        prefixManager = new PrefixManager(this);
        suffixManager = new SuffixManager(this);
        replyManager = new ReplyManager();

        muteManager.load();
        muteManager.startMuteChecker();
        chatManager.reloadBlockedWords();
        prefixManager.load();
        suffixManager.load();
        configManager.debug("Core managers initialized and runtime state loaded");

        registerEvents();
        registerCommands();
        configManager.debug("Events and commands registered");

        sendHelloMessage();
        configManager.debug("Plugin enable sequence completed");
    }

    @Override
    public void onDisable() {
        debug("Starting plugin shutdown");
        if (channelManager != null) {
            for(ChatChannel channel : channelManager.getChannels()) {
                for(Player player : channel.getMembers()) {
                    channel.removeMember(player);
                }
            }

            channelManager.saveChannels();
        }

        if (muteManager != null) {
            muteManager.stopMuteChecker();
            muteManager.save();
        }

        if (prefixManager != null) {
            prefixManager.save();
        }

        if (suffixManager != null) {
            suffixManager.save();
        }

        if (muteManager != null) {
            muteManager.save();
        }

        debug("Plugin shutdown completed");
    }


    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        debug("Registered chat, join and quit listeners");
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("yochat")).setExecutor(new YoChatCommand());
        debug("Registered /yochat command executor");
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

    /**
     * Returns the active chat manager.
     *
     * @return the chat manager
     */
    @Override
    public ChatManager getChatManager() {
        return chatManager;
    }

    /**
     * Returns the active channel manager.
     *
     * @return the channel manager
     */
    @Override
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    /**
     * Returns the active mute manager.
     *
     * @return the mute manager
     */
    @Override
    public MuteManager getMuteManager() {return muteManager;}

    /**
     * Returns the active message parse manager.
     *
     * @return the message parse manager
     */
    @Override
    public MessageParseManager getMessageParseManager() {return messageParseManager;}

    /**
     * Returns the active chat pipeline manager.
     *
     * @return the chat pipeline manager
     */
    @Override
    public ChatPipelineManager getChatPipelineManager() {
        return chatPipelineManager;
    }

    /**
     * Returns the active suffix manager.
     *
     * @return the suffix manager
     */
    @Override
    public SuffixManager getSuffixManager() {
        return suffixManager;
    }

    /**
     * Returns the active prefix manager.
     *
     * @return the prefix manager
     */
    @Override
    public PrefixManager getPrefixManager() {
        return prefixManager;
    }

    /**
     * Returns the active reply manager.
     * @return the reply manager
     */
    @Override
    public ReplyManager getReplyManager() {
        return replyManager;
    }

    private void debug(String message) {
        if (configManager != null) {
            configManager.debug(message);
        }
    }

    private void ensureDefaultResource(String resourceName) {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Failed to create plugin data folder for " + resourceName);
            return;
        }

        File target = new File(getDataFolder(), resourceName);
        if (target.exists()) {
            return;
        }

        saveResource(resourceName, false);
        getLogger().info("Generated default " + resourceName);
    }
}
