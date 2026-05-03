package me.tyyni.yoChat.yoChatPlugin.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.events.YoChatPipelineEvent;
import me.tyyni.yoChat.yoChatAPI.events.YoChatSendEvent;
import me.tyyni.yoChat.yoChatPlugin.ChatManager;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import me.tyyni.yoChat.yoChatAPI.events.YoChatMessageEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatManager chatManager = YoChatAPI.getPlugin().getChatManager();
        ConfigManager config = ConfigManager.getInstance();
        String plainText = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        ChatChannel channel = config.isUseChannelSystem()
                ? YoChatAPI.getPlugin().getChannelManager().getChannelByPlayer(player)
                : null;

        config.debug("Incoming chat from %s in world=%s: '%s'", player.getName(), player.getWorld().getName(), plainText);

        ChatContext context = new ChatContext();
        context.setSender(player);
        context.setSenderDisplayName(player.displayName());
        context.setRawMessage(plainText);
        context.setProcessedMessage(plainText);
        context.setChannel(channel);
        context.setViewers(new LinkedHashSet<>(event.viewers()));

        YoChatAPI.getPlugin().getChatPipelineManager().execute(context);
        if (context.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        YoChatPipelineEvent pipelineEvent = new YoChatPipelineEvent(player, context.getComponent(), context.getChannel(), context);
        Bukkit.getPluginManager().callEvent(pipelineEvent);
        if (pipelineEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        YoChatMessageEvent yoChatEvent = new YoChatMessageEvent(player, context.getComponent(), context.getChannel());
        Bukkit.getPluginManager().callEvent(yoChatEvent);
        if (yoChatEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        context.setComponent(yoChatEvent.getMessage());
        context.setChannel(yoChatEvent.getChannel());

        YoChatSendEvent sendEvent = new YoChatSendEvent(player, context.getComponent(), context.getChannel());
        Bukkit.getPluginManager().callEvent(sendEvent);
        if (sendEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        context.setComponent(sendEvent.getMessage());
        context.setChannel(sendEvent.getChannel());

        LinkedHashSet<Audience> resolvedViewers = new LinkedHashSet<>(context.getViewers());
        RenderPlan renderPlan;
        try {
            renderPlan = buildRenderPlan(player, context.getComponent(), context.getChannel(), resolvedViewers, chatManager, config);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            event.setCancelled(true);
            YoChatAPI.getPlugin().getLogger().warning("Chat render plan was interrupted for " + player.getName());
            return;
        } catch (ExecutionException ex) {
            event.setCancelled(true);
            YoChatAPI.getPlugin().getLogger().severe("Failed to build chat render plan for " + player.getName() + ": " + ex.getCause());
            return;
        }

        event.viewers().clear();
        event.viewers().addAll(resolvedViewers);
        event.message(context.getComponent());
        event.renderer((source, sourceDisplayName, message, viewer) -> renderPlan.resolve(viewer));
    }

    private RenderPlan buildRenderPlan(
            Player sender,
            Component baseMessage,
            ChatChannel channel,
            Set<Audience> viewers,
            ChatManager chatManager,
            ConfigManager config
    ) throws ExecutionException, InterruptedException {
        return callSync(() -> createRenderPlan(sender, baseMessage, channel, viewers, chatManager, config));
    }

    private RenderPlan createRenderPlan(
            Player sender,
            Component baseMessage,
            ChatChannel channel,
            Set<Audience> viewers,
            ChatManager chatManager,
            ConfigManager config
    ) {
        Map<UUID, Component> playerMessages = new HashMap<>();
        Component fallbackMessage = renderMessage(chatManager, config, channel, sender, baseMessage);

        for (Audience viewer : viewers) {
            if (!(viewer instanceof Player viewerPlayer)) {
                continue;
            }

            Component viewerMessage = chatManager.applyMentionFormatting(sender, viewerPlayer, baseMessage);
            playerMessages.put(viewerPlayer.getUniqueId(), renderMessage(chatManager, config, channel, sender, viewerMessage));
        }

        return new RenderPlan(playerMessages, fallbackMessage);
    }

    private Component renderMessage(
            ChatManager chatManager,
            ConfigManager config,
            ChatChannel channel,
            Player sender,
            Component message
    ) {
        if (config.isUseChannelSystem() && channel != null) {
            return chatManager.formatChannelMessage(channel, sender, message);
        }

        return chatManager.formatMessage(sender, message);
    }

    private <T> T callSync(Callable<T> task) throws ExecutionException, InterruptedException {
        if (Bukkit.isPrimaryThread()) {
            try {
                return task.call();
            } catch (ExecutionException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ExecutionException(ex);
            }
        }

        return Bukkit.getScheduler().callSyncMethod(YoChatAPI.getPlugin(), task).get();
    }

    private record RenderPlan(Map<UUID, Component> playerMessages, Component fallbackMessage) {
        private Component resolve(Audience viewer) {
            if (viewer instanceof Player player) {
                return playerMessages.getOrDefault(player.getUniqueId(), fallbackMessage);
            }

            return fallbackMessage;
        }
    }
}
