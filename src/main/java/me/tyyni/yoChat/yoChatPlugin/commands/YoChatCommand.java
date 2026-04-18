package me.tyyni.yoChat.yoChatPlugin.commands;

import me.tyyni.yoChat.yoChatPlugin.YoChat;
import me.tyyni.yoChat.yochatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class YoChatCommand implements TabExecutor {
    private final YoChat plugin = YoChatAPI.getInstance();
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender.hasPermission("yochat")) {
            if (args.length == 0) {
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("YoChat version + " + YoChatAPI.getInstance().getPluginMeta().getVersion(), plugin.getMainColor())));
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("For help, use /yochat help", plugin.getMainColor())));
            }

            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("YoChat Commands:", plugin.getMainColor())));
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("/yochat - Displays plugin information", plugin.getMainColor())));
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("/yochat help - Displays this help message", plugin.getMainColor())));
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("/yochat reload - Reloads the config", plugin.getMainColor())));
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("/yochat channels - Channel management commands", plugin.getMainColor())));
                } else if (args[0].equalsIgnoreCase("reload")) {
                    YoChatAPI.getInstance().reloadConfig();
                    sender.sendMessage(Component.text("YoChat config reloaded!", plugin.getMainColor()));
                } else if (args[0].equalsIgnoreCase("channels")) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("Channel management commands:", plugin.getMainColor())));
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("/yochat channels create <name> - Creates a new channel", plugin.getMainColor())));
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("/yochat channels delete <name> - Deletes a channel", plugin.getMainColor())));
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("/yochat channels list - Lists all channels", plugin.getMainColor())));
                } else {
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("Unknown subcommand. Use /yochat help for a list of commands.", NamedTextColor.RED)));
                }
            }

            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("channels")) {
                    if (args[1].equalsIgnoreCase("list")) {
                        List<String> channels = YoChatAPI.getInstance().getChannelManager().getChannels().stream()
                                .map(ChatChannel::getName)
                                .toList();
                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Channels:", plugin.getMainColor())));
                        channels.forEach(channelName -> sender.sendMessage(Component.text("- " + channelName, plugin.getMainColor())));
                    } else if (args[1].equalsIgnoreCase("create")) {
                        sender.sendMessage(Component.text("Usage: /yochat channels create <name>", NamedTextColor.RED));
                    } else if (args[1].equalsIgnoreCase("delete")) {
                        sender.sendMessage(Component.text("Usage: /yochat channels delete <name>", NamedTextColor.RED));
                    } else {
                        sender.sendMessage(Component.text("Unknown subcommand. Use /yochat help for a list of commands.", NamedTextColor.RED));
                    }
                }
            }

            if (args.length == 3) {
                if (args[0].equalsIgnoreCase("channels")) {
                    if (args[1].equalsIgnoreCase("delete")) {
                        String channelName = args[2];
                        if (YoChatAPI.getInstance().getChannelManager().getChannel(channelName) == null) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("No channel found with that name!", NamedTextColor.RED)));
                        } else {
                            YoChatAPI.getInstance().getChannelManager().deleteChannel(channelName);
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Channel '" + channelName + "' deleted!", plugin.getMainColor())));
                        }
                    } else {
                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Unknown subcommand. Use /yochat help for a list of commands.", NamedTextColor.RED)));
                    }
                }
            }

            if(args.length == 5) {
                if (args[0].equalsIgnoreCase("channels")) {
                    if(args[1].equalsIgnoreCase("create")) {
                        String channelName = args[2];
                        String permission = args[3];
                        int radius;
                        try {
                            radius = Integer.parseInt(args[4]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Radius must be a number!", NamedTextColor.RED)));
                            return true;
                        }

                        ChatChannel channel = new ChatChannel(channelName, permission, radius);
                        YoChatAPI.registerChannel(channel);
                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Channel '" + channelName + "' created with permission '" + permission + "' and radius " + radius + "!", plugin.getMainColor())));
                    }
                }
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return Collections.emptyList();
    }
}
