package me.tyyni.yoChat.yoChatPlugin.steps;

import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import org.bukkit.entity.Player;

public class ChannelStep implements ChatPipelineStep {
    @Override
    public void process(ChatContext context) {
        if (context.isCancelled()) return;

        Player player = context.getSender();
        ConfigManager config = ConfigManager.getInstance();

        if (!config.isUseChannelSystem()) return;

        ChatChannel channel = YoChatAPI.getPlugin()
            .getChannelManager()
            .getChannelByPlayer(player);

        context.setChannel(channel);

        config.debug("Channel for %s is %s", player.getName(), channel != null ? channel.getName() : "null");

        if (channel == null && config.isUseChannelSystem()) {
            context.setCancelled(true);
            ConfigManager.getInstance().debug("Cancelled chat because %s does not belong to a channel", player.getName());
            player.sendMessage(YoChatAPI.getPlugin().getChatManager().formatMessage(
                    ConfigManager.getInstance().getNoChannelMessage(),
                    player,
                    ""
            ));
        }
    }
}
