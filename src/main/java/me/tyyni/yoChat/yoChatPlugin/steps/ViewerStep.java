package me.tyyni.yoChat.yoChatPlugin.steps;

import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class ViewerStep implements ChatPipelineStep {

    @Override
    public void process(ChatContext context) {
        if (context.isCancelled()) return;

        ChatChannel channel = context.getChannel();
        if (channel == null) return;

        Player player = context.getSender();
        int initialViewerCount = context.getViewers().size();

        Set<Audience> filtered = new HashSet<>();

        context.getViewers().forEach(viewer -> {
            if (!(viewer instanceof Player viewerPlayer)) {
                filtered.add(viewer);
                return;
            }

            if (!channel.getMembers().contains(viewerPlayer)) return;
            if (!channel.canJoin(viewerPlayer)) return;

            Set<String> worlds = channel.getWorlds();
            if (worlds != null && !worlds.isEmpty()) {
                if (channel.isStrictWorld() && !worlds.contains(player.getWorld().getName())) return;
                if (!worlds.contains(viewerPlayer.getWorld().getName())) return;
            }

            if (channel.getRadius() > 0) {
                if (!viewerPlayer.getWorld().equals(player.getWorld())) return;
                if (viewerPlayer.getLocation().distance(player.getLocation()) > channel.getRadius()) return;
            }

            filtered.add(viewerPlayer);
        });

        context.setViewers(filtered);
        ConfigManager.getInstance().debug("Viewer step: %d -> %d viewers for channel %s", initialViewerCount, filtered.size(), channel.getName());
    }
}
