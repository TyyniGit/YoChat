package me.tyyni.yoChat.yoChatPlugin.listeners;

import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private static final int MAX_AUTO_JOIN_ATTEMPTS = 5;
    private static final long AUTO_JOIN_RETRY_DELAY_TICKS = 10L;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ConfigManager config = ConfigManager.getInstance();
        if (!config.isUseChannelSystem()) {
            return;
        }

        Bukkit.getScheduler().runTask(YoChatAPI.getPlugin(), () -> tryAutoJoin(player, 1));
    }

    private void tryAutoJoin(Player player, int attempt) {
        ConfigManager config = ConfigManager.getInstance();
        if (config == null || !config.isUseChannelSystem() || !player.isOnline()) {
            return;
        }

        if (YoChatAPI.getPlugin().getChannelManager().getChannelByPlayer(player) != null) {
            config.debug("Skipped auto-join for %s because they already belong to a channel", player.getName());
            return;
        }

        ChatChannel defaultChannel = config.resolveDefaultChannel();
        boolean canJoin = defaultChannel != null && defaultChannel.canJoin(player);
        if (canJoin) {
            YoChatAPI.getPlugin().getChannelManager().joinChannel(player, defaultChannel);
            config.debug("Auto-joined %s to default channel %s on attempt %d", player.getName(), defaultChannel.getName(), attempt);
            return;
        }

        if (attempt < MAX_AUTO_JOIN_ATTEMPTS) {
            config.debug("Retrying auto-join for %s (attempt %d/%d, channel=%s, canJoin=%s)",
                    player.getName(),
                    attempt,
                    MAX_AUTO_JOIN_ATTEMPTS,
                    defaultChannel != null ? defaultChannel.getName() : "null",
                false);
            Bukkit.getScheduler().runTaskLater(YoChatAPI.getPlugin(), () -> tryAutoJoin(player, attempt + 1), AUTO_JOIN_RETRY_DELAY_TICKS);
            return;
        }

        config.debug("Did not auto-join %s to default channel after %d attempts (channel=%s, canJoin=%s)",
                player.getName(),
                MAX_AUTO_JOIN_ATTEMPTS,
                defaultChannel != null ? defaultChannel.getName() : "null",
            false);
    }
}
