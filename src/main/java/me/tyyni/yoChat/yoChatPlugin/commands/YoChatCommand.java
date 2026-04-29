package me.tyyni.yoChat.yoChatPlugin.commands;

import com.tchristofferson.configupdater.ConfigUpdater;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yoChatPlugin.MuteManager;
import me.tyyni.yoChat.yoChatPlugin.YoChat;
import me.tyyni.yoChat.yoChatPlugin.objects.MutedPlayer;
import me.tyyni.yoChat.yoChatPlugin.webhook.WebhookPayload;
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YoChatCommand implements TabExecutor {
    private final YoChat plugin = YoChatAPI.getPlugin();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        debug("Command invoked by %s: /%s %s", sender.getName(), label, String.join(" ", args));
        if (sender instanceof Player && !sender.hasPermission("yochat.commands")) {
            debug("Denied root command access for %s", sender.getName());
            sendNoPermissionMessage(sender);
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getYoChatPrefix().append(
                    Component.text("YoChat version " + YoChatAPI.getPlugin().getPluginMeta().getVersion(), plugin.getMainColor())));
            sender.sendMessage(Component.text("For help, use /yochat help", plugin.getMainColor()));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                handleHelp(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "channels":
                handleChannels(sender, args);
                break;
            case "mute":
                handleMute(sender, args);
                break;
            case "unmute":
                handleUnmute(sender, args);
                break;
            default:
                debug("Unknown subcommand from %s: %s", sender.getName(), subCommand);
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Unknown subcommand. Use /yochat help for a list of commands.", NamedTextColor.RED)));
                break;
        }

        return true;
    }

    private void handleHelp(CommandSender sender) {
        if (sender instanceof Player && !sender.hasPermission("yochat.commands.help")) {
            sendNoPermissionMessage(sender);
            return;
        }

        sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("YoChat Commands:", plugin.getMainColor())));

        sender.sendMessage(Component.text("/yochat ", plugin.getMainColor())
                .append(Component.text("- Displays plugin information", plugin.getHighlightColor())));

        sender.sendMessage(Component.text("/yochat help ", plugin.getMainColor())
                .append(Component.text("- Displays this help message", plugin.getHighlightColor())));

        sender.sendMessage(Component.text("/yochat reload ", plugin.getMainColor())
                .append(Component.text("- Reloads the config", plugin.getHighlightColor())));

        sender.sendMessage(Component.text("/yochat channels ", plugin.getMainColor())
                .append(Component.text("- Channel management commands", plugin.getHighlightColor())));

        sender.sendMessage(Component.text("/yochat mute ", plugin.getMainColor())
                .append(Component.text("- Mute command", plugin.getHighlightColor())));

        sender.sendMessage(Component.text("/yochat unmute ", plugin.getMainColor())
                .append(Component.text("- Unmute command", plugin.getHighlightColor())));
    }

    private void handleReload(CommandSender sender) {
        if (sender instanceof Player && !sender.hasPermission("yochat.commands.reload")) {
            debug("Denied reload command for %s", sender.getName());
            sendNoPermissionMessage(sender);
            return;
        }

        YoChat api = YoChatAPI.getPlugin();
        ConfigManager configManager = ConfigManager.getInstance();
        debug("Reload requested by %s", sender.getName());
        try {
            ConfigUpdater.update(plugin, "config.yml", new File(plugin.getDataFolder(), "config.yml"));
            configManager.load();
            api.getChatManager().reloadBlockedWords();
            api.getMessageParseManager().setupMM();
            api.getMuteManager().setInterval(api.getChatManager().parseDuration(configManager.getMuteCheckerInterval()) * 20);
            debug("Reload completed: muteIntervalTicks=%d", api.getMuteManager().getInterval());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update config.yml: " + e.getMessage());
            debug("Reload failed: %s", e.getMessage());
        }
        sender.sendMessage(plugin.getYoChatPrefix().append(
                Component.text("YoChat config reloaded!", plugin.getMainColor())));
    }

    private void handleChannels(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("yochat.commands.channels")) {
            sendNoPermissionMessage(sender);
            return;
        }

        if (!ConfigManager.getInstance().isUseChannelSystem()) {
            debug("Channel command rejected because channel system is disabled");
            sender.sendMessage(plugin.getYoChatPrefix().append(
                Component.text("The channel system is not enabled", NamedTextColor.RED)
            ));
            return;
        }

        if (args.length == 1) {
            sender.sendMessage(plugin.getYoChatPrefix().append(
                Component.text("Channel management commands:", plugin.getMainColor())));
            sender.sendMessage(Component.text("/yochat channels create <name> <permission> <radius> [worlds] ", plugin.getMainColor())
                .append(Component.text("- Creates a new chat channel", plugin.getHighlightColor())));
            sender.sendMessage(Component.text("/yochat channels delete <name> ", plugin.getMainColor())
                .append(Component.text("- Deletes a chat channel", plugin.getHighlightColor())));
            sender.sendMessage(Component.text("/yochat channels list ", plugin.getMainColor())
                .append(Component.text("- Lists all channels", plugin.getHighlightColor())));
            sender.sendMessage(Component.text("/yochat channels join <name> ", plugin.getMainColor())
                .append(Component.text("- Makes you join a chat channel", plugin.getHighlightColor())));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "list" -> {
                if (!sender.hasPermission("yochat.commands.channels.list")) {
                    sendNoPermissionMessage(sender);
                    return;
                }
                List<String> channels = YoChatAPI.getPlugin().getChannelManager().getChannels().stream().map(ChatChannel::getName).toList();
                debug("Listing %d channels for %s", channels.size(), sender.getName());
                if (channels.isEmpty()) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("There are no channels", plugin.getMainColor())));

                    return;
                }

                sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("Channels:", plugin.getMainColor())));
                channels.forEach(channelName -> sender.sendMessage(Component.text("- ", plugin.getMainColor())
                    .append(Component.text(channelName, plugin.getHighlightColor()))));
            }
            case "create" -> {
                if (!sender.hasPermission("yochat.commands.channels.create")) {
                    sendNoPermissionMessage(sender);
                    return;
                }
                if (args.length < 5) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Usage: /yochat channels create <name> <radius> <strict> [worlds]", NamedTextColor.RED)));
                    return;
                }

                String channelName = args[2];
                int radius;
                try {
                    radius = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    debug("Invalid channel radius '%s' from %s", args[3], sender.getName());
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Radius must be a number!", NamedTextColor.RED)));
                    return;
                }

                String strict = args[4];
                boolean strictWorld;
                if (strict.equalsIgnoreCase("true")) {
                    strictWorld = true;
                } else if (strict.equalsIgnoreCase("false")) {
                    strictWorld = false;
                } else {
                    debug("Invalid strict boolean '%s' for channel create by %s", strict, sender.getName());
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Incorrect boolean usage!", NamedTextColor.RED)
                            .append(Component.text("Use "))
                            .append(Component.text("true", plugin.getHighlightColor())
                                .append(Component.text(" or ", NamedTextColor.RED)
                                    .append(Component.text("false", plugin.getHighlightColor()))))));
                    return;
                }

                Set<String> worlds = new HashSet<>();
                if (args.length > 5) {
                    String worldString = String.join(" ", Arrays.copyOfRange(args, 5, args.length));
                    for (String world : worldString.split(",")) {
                        World isWorld = Bukkit.getWorld(world);
                        if (isWorld == null) {
                            debug("Unknown world '%s' while creating channel %s", world, channelName);
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("World ", NamedTextColor.RED)
                                    .append(Component.text("'" + world + "'", plugin.getHighlightColor())
                                        .append(Component.text(" doesn't exist!", NamedTextColor.RED)))));
                            continue;
                        }
                        worlds.add(isWorld.getName());
                    }
                }

                if (worlds.isEmpty()) worlds = null;

                ChatChannel channel = YoChatAPI.getPlugin().getChannelManager().createChannel(channelName, radius, strictWorld, worlds);
                if (channel != null) {
                    YoChatAPI.registerChannel(channel);
                    debug("Created channel %s radius=%d strict=%s worlds=%s", channelName, radius, strictWorld, worlds);

                    List<Component> worldComponents = worlds != null ? worlds.stream()
                                                                       .map(name -> Component.text(name, plugin.getHighlightColor()))
                                                                       .collect(Collectors.toList()) : null;

                    Component joinedWorlds = null;
                    if (worldComponents != null) {
                        joinedWorlds = Component.join(
                            JoinConfiguration.separator(Component.text(", ", plugin.getMainColor())),
                            worldComponents
                        );
                    }

                    Component message = plugin.getYoChatPrefix().append(
                        Component.text("Channel ", plugin.getMainColor())
                            .append(Component.text("'" + channelName + "'", plugin.getHighlightColor())
                                .append(Component.text(" created: ", plugin.getMainColor())
                                    .appendNewline()
                                    .append(Component.text("Name: ", plugin.getMainColor())
                                        .append(Component.text(channelName, plugin.getHighlightColor())))

                                    .appendNewline()
                                    .append(Component.text("Radius: ", plugin.getMainColor())
                                        .append(Component.text(radius, plugin.getHighlightColor())))

                                    .appendNewline()
                                    .append(Component.text("Strict: ", plugin.getMainColor())
                                        .append(Component.text(strictWorld, plugin.getHighlightColor()))))));
                    if (joinedWorlds != null) {
                        message = message.appendNewline()
                            .append(Component.text("Worlds: ", plugin.getMainColor())
                                .append(joinedWorlds));
                    }

                    sender.sendMessage(message);

                    sender.sendMessage(Component.text("Tweak settings and add a permission by using /yochat channels edit <channel>", plugin.getMainColor()));
                }
            }
            case "delete" -> {
                if (!sender.hasPermission("yochat.commands.channels.delete")) {
                    sendNoPermissionMessage(sender);
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("Usage: /yochat channels delete <name>", NamedTextColor.RED)));
                    return;
                }
                String channelName = args[2];
                if (YoChatAPI.getPlugin().getChannelManager().getChannel(channelName) == null) {
                    debug("Delete requested for missing channel %s", channelName);
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("No channel found with that name!", NamedTextColor.RED)));
                    return;
                }
                YoChatAPI.getPlugin().getChannelManager().deleteChannel(channelName);
                debug("Deleted channel %s by %s", channelName, sender.getName());
                sender.sendMessage(plugin.getYoChatPrefix().append(
                    Component.text("Channel ", plugin.getMainColor())
                        .append(Component.text("'" + channelName + "'", plugin.getHighlightColor())
                            .append(Component.text(" deleted!", plugin.getMainColor())))));
            }
            case "join" -> {
                if (!sender.hasPermission("yochat.commands.channels.join")) {
                    sendNoPermissionMessage(sender);
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("Usage: /yochat channels join <name>", NamedTextColor.RED)));
                    return;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("Only players can use this command!", NamedTextColor.RED)));
                    return;
                }
                String channelName = args[2];
                ChatChannel channel = YoChatAPI.getPlugin().getChannelManager().getChannel(channelName);
                if (channel == null) {
                    debug("Join requested for missing channel %s", channelName);
                    sender.sendMessage(plugin.getYoChatPrefix()
                        .append(Component.text("No channel found with that name!", NamedTextColor.RED)));
                    return;
                }

                if (channel.isMember(player)) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("You are already in channel !", NamedTextColor.RED)
                            .append(Component.text("'" + channelName + "'", plugin.getHighlightColor()))
                    ));
                    return;
                }

                YoChatAPI.joinChannel(player, channel);
                debug("%s joined channel %s via command", player.getName(), channelName);
                sender.sendMessage(plugin.getYoChatPrefix().append(
                    Component.text("Joined channel ", plugin.getMainColor())
                        .append(Component.text("'" + channelName + "'", plugin.getHighlightColor()))));
            }
            case "edit" -> {
                if (!sender.hasPermission("yochat.commands.channels.edit")) {
                    sendNoPermissionMessage(sender);
                    return;
                }

                if (args.length < 5) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("Usage: /yochat channels edit <channel> <type> <value>", NamedTextColor.RED)));
                    return;
                }

                String channelName = args[2];
                ChatChannel channel = YoChatAPI.getPlugin().getChannelManager().getChannel(channelName);
                if (channel == null) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("No channel found with that name!", NamedTextColor.RED)));
                    return;
                }

                String editType = args[3];
                switch (editType) {
                    case "permission" -> {
                        String edit = args[4];
                        if (edit.equalsIgnoreCase("-")) {
                            channel.setPermission(null);
                            debug("Removed permission from channel %s", channelName);

                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Permission removed for channel", plugin.getMainColor())
                                    .append(Component.text("'" + channelName + "'", plugin.getHighlightColor()))));
                            return;
                        }

                        channel.setPermission(edit);
                        debug("Set permission for channel %s to %s", channelName, edit);

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("Permission set to ", plugin.getMainColor())
                                .append(Component.text("'" + edit + "'", plugin.getHighlightColor())
                                    .append(Component.text(" for channel ", plugin.getMainColor())
                                        .append(Component.text("'" + channelName + "'", plugin.getHighlightColor())))))
                        );
                    }
                    case "name" -> {
                        String edit = args[4];
                        channel.setName(edit);
                        debug("Renamed channel %s to %s", channelName, edit);

                        YoChatAPI.getPlugin().getChannelManager().getChannelsList().remove(channelName);
                        ChatChannel newChannel = new ChatChannel(channel.getName(), channel.getRadius(), channel.isStrictWorld(), channel.getWorlds());
                        YoChatAPI.registerChannel(newChannel);

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("Name changed to ", plugin.getMainColor())
                                .append(Component.text("'" + edit + "'", plugin.getHighlightColor())
                                    .append(Component.text(" for channel ", plugin.getMainColor())
                                        .append(Component.text("'" + channelName + "'", plugin.getHighlightColor()))))
                        ));
                    }
                    case "strict" -> {
                        String strict = args[4];
                        boolean strictWorld;
                        if (strict.equalsIgnoreCase("true")) {
                            strictWorld = true;
                        } else if (strict.equalsIgnoreCase("false")) {
                            strictWorld = false;
                        } else {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Incorrect boolean usage!", NamedTextColor.RED)
                                        .append(Component.text("Use "))
                                        .append(Component.text("true", plugin.getHighlightColor())
                                            .append(Component.text(" or ", NamedTextColor.RED)
                                                .append(Component.text("false", plugin.getHighlightColor())
                                                )
                                            )
                                        )
                                )
                            );
                            return;
                        }

                        channel.setStrictWorld(strictWorld);
                        debug("Set strictWorld for channel %s to %s", channelName, strictWorld);

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("Strict world set to ", plugin.getMainColor())
                                .append(Component.text("'" + strict + "'", plugin.getHighlightColor())
                                    .append(Component.text(" for channel ", plugin.getMainColor())
                                        .append(Component.text("'" + channelName + "'", plugin.getHighlightColor())))
                                )));
                    }
                    case "radius" -> {
                        String radiusString = args[4];
                        int radius;
                        try {
                            radius = Integer.parseInt(radiusString);
                        } catch (NumberFormatException e) {
                            debug("Invalid radius edit '%s' for channel %s", radiusString, channelName);
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Radius has to be a number!", NamedTextColor.RED)
                            ));
                            return;
                        }

                        channel.setRadius(radius);
                        debug("Set radius for channel %s to %d", channelName, radius);

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("Radius set to ", plugin.getMainColor())
                                .append(Component.text("'" + radius + "'", plugin.getHighlightColor())
                                    .append(Component.text(" for channel ", plugin.getMainColor())
                                        .append(Component.text("'" + channelName + "'", plugin.getHighlightColor()))))
                        ));
                    }
                    case "worlds" -> {
                        String operation = args[4];
                        switch (operation) {
                            case "add" -> {
                                if (args.length < 6) {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                        Component.text("Usage: /yochat channels edit <channel> worlds add <world>", NamedTextColor.RED)
                                    ));
                                    return;
                                }
                                String worldName = args[5];
                                if (Bukkit.getWorld(worldName) == null) {
                                    debug("Attempted to add unknown world %s to channel %s", worldName, channelName);
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("World ", NamedTextColor.RED))
                                        .append(Component.text("'" + worldName + "'", plugin.getHighlightColor())
                                            .append(Component.text(" doesn't exist! ", NamedTextColor.RED)
                                            )));

                                    return;
                                }

                                if(channel.hasWorld(worldName)) {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                        Component.text("Channel ", plugin.getMainColor())
                                            .append(Component.text("'" + channelName + "'", plugin.getHighlightColor())
                                                .append(Component.text(" already contains world ", NamedTextColor.RED)
                                                    .append(Component.text("'" + worldName +  "'", plugin.getHighlightColor())
                                    )))));

                                    return;
                                }

                                channel.addWorld(worldName);
                                debug("Added world %s to channel %s", worldName, channelName);

                                sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Added world ", plugin.getMainColor())
                                        .append(Component.text("'" + worldName + "'", plugin.getHighlightColor())
                                            .append(Component.text(" to channel ", plugin.getMainColor())
                                                .append(Component.text("'" + channelName + "'", plugin.getHighlightColor()))))));
                            }
                            case "remove" -> {
                                if (args.length < 6) {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                        Component.text("Usage: /yochat channels edit <channel> worlds remove <world>", NamedTextColor.RED)
                                    ));
                                    return;
                                }
                                String worldName = args[5];

                                if (!channel.hasWorld(worldName)) {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                        Component.text("Channel ", plugin.getMainColor())
                                            .append(Component.text("'" + channelName + "'", plugin.getHighlightColor())
                                                .append(Component.text(" doesn't contain world ", NamedTextColor.RED)
                                                    .append(Component.text("'" + worldName + "'", plugin.getHighlightColor()))

                                    ))));

                                    return;
                                }

                                channel.removeWorld(worldName);
                                debug("Removed world %s from channel %s", worldName, channelName);

                                sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Removed world ", plugin.getMainColor())
                                        .append(Component.text("'" + worldName + "'", plugin.getHighlightColor())
                                            .append(Component.text(" for channel ", plugin.getMainColor())
                                                .append(Component.text("'" + channelName + "'", plugin.getHighlightColor())))))
                                );
                            }
                            default -> sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Invalid operation!", NamedTextColor.RED)
                            ));
                        }
                    }
                    default -> sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Invalid type of edit!", NamedTextColor.RED)
                    ));
                }
            }
            case "leave" -> {
                if (!sender.hasPermission("yochat.commands.channels.leave")) {
                    sendNoPermissionMessage(sender);
                    return;
                }

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("Only players can use this command!", NamedTextColor.RED)));
                    return;
                }

                ChatChannel channel = YoChatAPI.getChannelByPlayer(player);
                if(channel == null) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("You don't belong to any channels!", NamedTextColor.RED)
                    ));

                    return;
                }

                channel.removeMember(player);
                debug("%s left channel %s via command", player.getName(), channel.getName());

                sender.sendMessage(plugin.getYoChatPrefix().append(
                    Component.text("Left from channel ", plugin.getMainColor())
                        .append(Component.text("'" + channel.getName() + "'", plugin.getHighlightColor()))
                ));
            }
            case "members" -> {
                if (!sender.hasPermission("yochat.commands.channels.members")) {
                    sendNoPermissionMessage(sender);
                    return;
                }

                if (args.length < 4) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Usage: /yochat channels members <channel> <list|add|remove> <player if add|remove>", NamedTextColor.RED)
                    ));

                    return;
                }

                String channelName = args[2];
                ChatChannel channel = YoChatAPI.getPlugin().getChannelManager().getChannel(channelName);
                if (channel == null) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("No channel found with that name!", NamedTextColor.RED)));
                    return;
                }

                String operation = args[3];
                switch (operation) {
                    case "add" -> {
                        if (args.length < 5) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Usage: /yochat channels members <channel> add <player>!", NamedTextColor.RED)
                            ));

                            return;
                        }

                        String membername = args[4];
                        Player player = Bukkit.getPlayer(membername);
                        if (player == null) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Player not found!", NamedTextColor.RED)
                            ));

                            return;
                        }

                        if (channel.isMember(player)) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Player ", NamedTextColor.RED)
                                    .append(Component.text("'" + player.getName() + "'", plugin.getHighlightColor())
                                        .append(Component.text(" is already a member of this channel!", NamedTextColor.RED))
                                    )));

                            return;
                        }

                        channel.addMember(player);
                        debug("Added member %s to channel %s", membername, channelName);

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("Added member ", plugin.getMainColor())
                                .append(Component.text("'" + membername + "'", plugin.getHighlightColor())
                                    .append(Component.text(" to channel ", plugin.getMainColor())
                                        .append(Component.text("'" + channelName + "'", plugin.getHighlightColor()))
                        ))));
                    }
                    case "remove" -> {
                        if (args.length < 5) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Usage: /yochat channels members <channel> remove <player>!", NamedTextColor.RED)
                            ));

                            return;
                        }

                        String membername = args[4];
                        Player player = Bukkit.getPlayer(membername);
                        if (player == null) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Player not found!", NamedTextColor.RED)
                            ));

                            return;
                        }

                        if (!channel.isMember(player)) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Player ", NamedTextColor.RED)
                                    .append(Component.text("'" + player.getName() + "'", plugin.getHighlightColor())
                                        .append(Component.text(" is not a member of this channel!", NamedTextColor.RED))
                                    )));

                            return;
                        }

                        channel.removeMember(player);
                        debug("Removed member %s from channel %s", membername, channelName);

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("Removed member ", plugin.getMainColor())
                                .append(Component.text("'" + membername + "'", plugin.getHighlightColor())
                                    .append(Component.text(" from channel ", plugin.getMainColor())
                                        .append(Component.text("'" + channelName + "'", plugin.getHighlightColor()))
                        ))));
                    }
                    case "list" -> {
                        List<Component> membersList = channel.getMembers().stream()
                            .map(p -> Component.text(p.getName(), plugin.getHighlightColor()))
                            .collect(Collectors.toList());

                        Component members;
                        if (!membersList.isEmpty()) {
                            members = Component.join(
                                JoinConfiguration.separator(Component.text(", ", plugin.getMainColor())),
                                membersList
                            );
                        } else {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("No members in channel", plugin.getMainColor())
                                    .append(Component.text("'" + channelName + "'", plugin.getHighlightColor()))
                            ));

                            return;
                        }

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("Channel's members: ", plugin.getMainColor())
                                .append(members)
                        ));
                        debug("Listed %d members for channel %s", membersList.size(), channelName);
                    }
                    default -> sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Invalid operation!", NamedTextColor.RED)
                    ));
                }
            }
            case "info" -> {
                if (!sender.hasPermission("yochat.commands.channels.info")) {
                    sendNoPermissionMessage(sender);
                    return;
                }

                if (args.length < 3) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("Usage: /yochat channels info <channel>", NamedTextColor.RED)));
                    return;
                }

                String channelName = args[2];
                ChatChannel channel = YoChatAPI.getPlugin().getChannelManager().getChannel(channelName);
                if (channel == null) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("No channel found with that name!", NamedTextColor.RED)));
                    return;
                }

                @Nullable Set<String> worlds = channel.getWorlds();
                List<Component> worldComponents = worlds != null ? worlds.stream()
                                                                   .map(name -> Component.text(name, plugin.getHighlightColor()))
                                                                   .collect(Collectors.toList()) : null;

                Component joinedWorlds = null;
                if (worldComponents != null) {
                    if(!worldComponents.isEmpty()) {
                        joinedWorlds = Component.join(
                            JoinConfiguration.separator(Component.text(", ", plugin.getMainColor())),
                            worldComponents
                        );
                    }
                }

                sender.sendMessage(plugin.getYoChatPrefix().append(
                    Component.text("Channel ", plugin.getMainColor())
                        .append(Component.text("'" + channelName + "'", plugin.getHighlightColor())
                            .append(Component.text(" info:", plugin.getMainColor())

                                .appendNewline()
                                .append(Component.text("Permission: ", plugin.getMainColor())
                                    .append(Component.text(channel.getPermission() != null ? channel.getPermission() : "No permission", plugin.getHighlightColor())))

                                .appendNewline()
                                .append(Component.text("Radius: ", plugin.getMainColor())
                                    .append(Component.text(channel.getRadius(), plugin.getHighlightColor())))

                                .appendNewline()
                                .append(Component.text("Strict: ", plugin.getMainColor())
                                    .append(Component.text(channel.isStrictWorld(), plugin.getHighlightColor())))

                                .appendNewline()
                                .append(Component.text("Format: ", plugin.getMainColor())
                                    .append(channel.getFormat() != null ? YoChatAPI.getPlugin().getChatManager().formatChannelFormat(channel) : Component.text("Default channel format", plugin.getHighlightColor())))

                                .appendNewline()
                                .append(Component.text("Worlds: ")
                                    .append(joinedWorlds != null ? joinedWorlds : Component.text("No worlds", plugin.getHighlightColor())))
                            ))));
                debug("Displayed info for channel %s", channelName);
            }
            default -> sender.sendMessage(plugin.getYoChatPrefix().append(
                Component.text("Unknown subcommand. Use /yochat help for a list of commands.", NamedTextColor.RED)));
        }
    }

    private void handleMute(CommandSender sender, String[] args) {
        if (!ConfigManager.getInstance().isModerationEnabled()) {
            debug("Mute command rejected because moderation is disabled");
            sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("The moderation system is not enabled!", NamedTextColor.RED)));
            return;
        }

        if (sender instanceof Player && !sender.hasPermission("yochat.commands.mute")) {
            debug("Denied mute command for %s", sender.getName());
            sendNoPermissionMessage(sender);
            return;
        }

        if (args.length == 1) {
            sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("Mute commands:", plugin.getMainColor())));
            sender.sendMessage(Component.text("/yochat mute <perm|temp> <player> <time if temp> [reason]", plugin.getMainColor())
                    .append(Component.text(" - Mutes the player", plugin.getHighlightColor())));
            sender.sendMessage(Component.text("/yochat unmute <player> [reason]", plugin.getMainColor())
                    .append(Component.text(" - Unmutes the player", plugin.getHighlightColor())));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getYoChatPrefix().append(
                    Component.text("Usage: /yochat mute <perm|temp> <player> <time if temp> [reason]", NamedTextColor.RED)));
            return;
        }

        String type = args[1].toLowerCase();
        String playername = args[2];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playername);
        UUID uuid = offlinePlayer.getUniqueId();
        debug("Mute requested by %s type=%s target=%s", sender.getName(), type, playername);

        if (MuteManager.isMuted(uuid.toString())) {
            debug("Mute skipped because %s is already muted", playername);
            sender.sendMessage(plugin.getYoChatPrefix().append(
                    Component.text("Player ", NamedTextColor.RED)
                            .append(Component.text("'" + playername + "'", plugin.getHighlightColor())
                                    .append(Component.text(" is muted already!", NamedTextColor.RED)))));
            return;
        }

        String reason = null;
        String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
        long duration;

        if (type.equals("perm")) {
            if (args.length > 3) reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            MutedPlayer mutedPlayer = new MutedPlayer(uuid, -1L, System.currentTimeMillis(), reason, sender.getName());
            YoChatAPI.addMutedPlayer(mutedPlayer);
            debug("Permanent mute added for %s reason=%s", name, reason);

            if (reason != null && !reason.isEmpty()) {
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Muted player ", plugin.getMainColor())
                                .append(Component.text("'" + name + "'", plugin.getHighlightColor())
                                        .append(Component.text(" for the reason ", plugin.getMainColor())
                                                .append(Component.text("'" + reason + "'", plugin.getHighlightColor()))))));
            } else {
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Muted player ", plugin.getMainColor())
                                .append(Component.text("'" + name + "'", plugin.getHighlightColor()))));
            }

            if (ConfigManager.getInstance().isWebhookEnabled()) sendMuteWebhook(name, "permanent", reason, sender.getName());
            if (ConfigManager.getInstance().isUseYouGotMutedMessage() && offlinePlayer.isOnline()) {
                Component formatted = YoChatAPI.getPlugin().getChatManager().formatYouGotMutedMessage(ConfigManager.getInstance().getYouGotMutedMessage(), sender.getName(), offlinePlayer, "permanent", reason);
                if (offlinePlayer.getPlayer() != null) offlinePlayer.getPlayer().sendMessage(formatted);
            }

        } else if (type.equals("temp")) {
            if (args.length < 4) {
                sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("Usage: /yochat mute temp <player> <time> [reason]", NamedTextColor.RED)));
                return;
            }
            String timeStr = args[3];
            if (!timeStr.matches("\\d+(y|mo|w|d|h|m|s)")) {
                debug("Mute rejected due to invalid time format '%s' for target %s", timeStr, playername);
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Incorrect timeformat! Example: 10m, 2h, 7d", NamedTextColor.RED).decoration(TextDecoration.BOLD, false)));
                return;
            }
            if (args.length > 4) reason = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
            duration = YoChatAPI.getPlugin().getChatManager().parseDuration(timeStr);

            MutedPlayer mutedPlayer = new MutedPlayer(uuid, duration, System.currentTimeMillis(), reason, sender.getName());
            YoChatAPI.addMutedPlayer(mutedPlayer);
            debug("Temporary mute added for %s duration=%s reason=%s", name, timeStr, reason);

            if (reason != null && !reason.isEmpty()) {
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Muted player ", plugin.getMainColor())
                                .append(Component.text("'" + name + "'", plugin.getHighlightColor())
                                        .append(Component.text(" with the time ", plugin.getMainColor())
                                                .append(Component.text("'" + timeStr + "'", plugin.getHighlightColor())
                                                        .append(Component.text(" for the reason ", plugin.getMainColor())
                                                                .append(Component.text("'" + reason + "'", plugin.getHighlightColor()))))))));
            } else {
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Muted player ", plugin.getMainColor())
                                .append(Component.text("'" + name + "'", plugin.getHighlightColor())
                                        .append(Component.text(" with the time ", plugin.getMainColor())
                                                .append(Component.text("'" + timeStr + "'", plugin.getHighlightColor()))))));
            }

            if (ConfigManager.getInstance().isWebhookEnabled()) sendMuteWebhook(name, timeStr, reason, sender.getName());
            if (ConfigManager.getInstance().isUseYouGotMutedMessage() && offlinePlayer.isOnline()) {
                Component formatted = YoChatAPI.getPlugin().getChatManager().formatYouGotMutedMessage(ConfigManager.getInstance().getYouGotMutedMessage(), sender.getName(), offlinePlayer, timeStr, reason);
                if (offlinePlayer.getPlayer() != null) offlinePlayer.getPlayer().sendMessage(formatted);
            }
        } else {
            debug("Unknown mute type '%s' from %s", type, sender.getName());
        }

        MuteManager.getInstance().save();
        debug("Mute command finished for target=%s", playername);
    }

    private void handleUnmute(CommandSender sender, String[] args) {
        if (!ConfigManager.getInstance().isModerationEnabled()) {
            debug("Unmute command rejected because moderation is disabled");
            sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("The moderation system is not enabled!", NamedTextColor.RED)));
            return;
        }

        if (sender instanceof Player && !sender.hasPermission("yochat.commands.unmute")) {
            debug("Denied unmute command for %s", sender.getName());
            sendNoPermissionMessage(sender);
            return;
        }

        if (args.length == 1) {
            sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("Mute commands:", plugin.getMainColor())));
            sender.sendMessage(Component.text("/yochat mute <perm|temp> <player> <time if temp> [reason]", plugin.getMainColor())
                    .append(Component.text(" - Mutes the player", plugin.getHighlightColor())));
            sender.sendMessage(Component.text("/yochat unmute <player> [reason]", plugin.getMainColor())
                    .append(Component.text(" - Unmutes the player", plugin.getHighlightColor())));
            return;
        }

        String playerName = args[1];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = offlinePlayer.getUniqueId();
        debug("Unmute requested by %s target=%s", sender.getName(), playerName);

        if (MuteManager.isMuted(uuid.toString())) {
            YoChatAPI.removeMutedPlayer(uuid);

            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
            String reason = (args.length > 2) ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;

            if (reason != null && !reason.isEmpty()) {
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Unmuted player ", plugin.getMainColor())
                                .append(Component.text("'" + name + "'", plugin.getHighlightColor())
                                        .append(Component.text(" for the reason ", plugin.getMainColor())
                                                .append(Component.text("'" + reason + "'", plugin.getHighlightColor()))))));
            } else {
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Unmuted player ", plugin.getMainColor())
                                .append(Component.text("'" + name + "'", plugin.getHighlightColor()))));
            }

            if (ConfigManager.getInstance().isWebhookEnabled()) sendUnmuteWebhook(name, reason, sender.getName());
            if (ConfigManager.getInstance().isUseYouGotUnmutedMessage() && offlinePlayer.isOnline()) {
                Component formatted = YoChatAPI.getPlugin().getChatManager().formatYouGotUnmutedMessage(ConfigManager.getInstance().getYouGotUnmutedMessage(), sender.getName(), offlinePlayer, reason);
                if (offlinePlayer.getPlayer() != null) offlinePlayer.getPlayer().sendMessage(formatted);
            }
            debug("Unmuted %s reason=%s", name, reason);

        } else {
            debug("Unmute skipped because %s is not muted", playerName);
            sender.sendMessage(plugin.getYoChatPrefix().append(
                    Component.text("Player ", NamedTextColor.RED)
                            .append(Component.text("'" + playerName + "'", plugin.getHighlightColor())
                                    .append(Component.text(" is not muted!", NamedTextColor.RED)))));
        }

        MuteManager.getInstance().save();
        debug("Unmute command finished for target=%s", playerName);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("yochat.tabcomplete")) return Collections.emptyList();
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            return Stream.of("help", "channels", "reload", "mute", "unmute")
                .filter(opt -> opt.startsWith(args[0].toLowerCase()))
                .sorted()
                .toList();
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("channels")) {
                return Stream.of("list", "create", "delete", "join", "leave", "members", "edit", "info")
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .sorted()
                    .toList();
            } else if (args[0].equalsIgnoreCase("mute")) {
                return Stream.of("temp", "perm")
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .sorted()
                    .toList();
            } else if (args[0].equalsIgnoreCase("unmute")) {
                return MuteManager.getMutedPlayerNames().stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .sorted()
                    .toList();
            } else if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("reload")) {
                return Collections.emptyList();
            } else {
                return Collections.emptyList();
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("channels")) {
                if (args[1].equalsIgnoreCase("delete")
                    || args[1].equalsIgnoreCase("members")
                    || args[1].equalsIgnoreCase("edit")
                    || args[1].equalsIgnoreCase("info")) {
                    return YoChatAPI.getPlugin().getChannelManager().getChannels().stream()
                        .map(ChatChannel::getName)
                        .filter(opt -> opt.toLowerCase().startsWith(args[2].toLowerCase()))
                        .sorted()
                        .toList();
                } else if (args[1].equalsIgnoreCase("create")) {
                    return Collections.singletonList("name");
                } else if(args[1].equalsIgnoreCase("join")) {
                    return YoChatAPI.getPlugin().getChannelManager().getChannels().stream()
                        .filter(channel -> !channel.isMember(player))
                        .map(ChatChannel::getName)
                        .filter(opt -> opt.toLowerCase().startsWith(args[2].toLowerCase()))
                        .sorted()
                        .toList();
                } else {
                    return Collections.emptyList();
                }

            } else if (args[0].equalsIgnoreCase("mute")) {
                return Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(Objects::nonNull)
                    .filter(opt -> opt.toLowerCase().startsWith(args[2].toLowerCase()))
                    .sorted()
                    .toList();
            } else if (args[0].equalsIgnoreCase("unmute")) {
                return Collections.singletonList("reason");
            } else {
                return Collections.emptyList();
            }
        }

        ChatChannel channel = YoChatAPI.getChannel(args[2]);

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("channels")) {
                if (args[1].equalsIgnoreCase("create")) {
                    return Collections.singletonList("radius");
                } else if (args[1].equalsIgnoreCase("members")) {
                    if (channel == null) {
                        return Collections.emptyList();
                    }

                    return Stream.of("add", "remove", "list")
                        .filter(operation -> operation.startsWith(args[3].toLowerCase()))
                        .sorted()
                        .toList();
                } else if (args[1].equalsIgnoreCase("edit")) {
                    if (channel == null) {
                        return Collections.emptyList();
                    }

                    return Stream.of("radius", "worlds", "strict", "permission", "name")
                        .filter(opt -> opt.startsWith(args[3].toLowerCase()))
                        .sorted()
                        .toList();
                } else {
                    return Collections.emptyList();
                }
            } else if (args[0].equalsIgnoreCase("mute")) {
                if (args[1].equalsIgnoreCase("perm")) return Collections.singletonList("reason");
                if (args[1].equalsIgnoreCase("temp")) return Collections.singletonList("time");
            } else {
                return Collections.emptyList();
            }
        }

        if (args.length == 5) {
            if (args[0].equalsIgnoreCase("channels")) {
                if (args[1].equalsIgnoreCase("create")) {
                    return Stream.of("true", "false")
                        .filter(opt -> opt.startsWith(args[4].toLowerCase()))
                        .sorted()
                        .toList();
                } else if (args[1].equalsIgnoreCase("edit")) {
                    if (channel == null) {
                        return Collections.emptyList();
                    }

                    if (args[3].equalsIgnoreCase("permission")) {
                        return List.of("permission", "-");

                    } else if (args[3].equalsIgnoreCase("strict")) {
                        return Stream.of("true", "false")
                            .filter(opt -> opt.startsWith(args[4].toLowerCase()))
                            .sorted()
                            .toList();

                    } else if (args[3].equalsIgnoreCase("radius")) {
                        return Collections.singletonList("radius");

                    } else if (args[3].equalsIgnoreCase("worlds")) {
                        return Stream.of("add", "remove")
                            .filter(operation -> operation.startsWith(args[4].toLowerCase()))
                            .sorted()
                            .toList();
                    } else if (args[3].equalsIgnoreCase("name")) {
                        return Collections.singletonList("name");
                    }
                } else if (args[1].equalsIgnoreCase("members")) {
                    if (channel == null) {
                        return Collections.emptyList();
                    }

                    if (args[3].equalsIgnoreCase("add")) {
                        return Bukkit.getOnlinePlayers().stream()
                            .filter(Predicate.not(channel::isMember))
                            .map(Player::getName)
                            .filter(name -> name.startsWith(args[4].toLowerCase()))
                            .sorted()
                            .toList();

                    } else if (args[3].equalsIgnoreCase("remove")) {
                        return channel.getMembers().stream()
                            .map(Player::getName)
                            .filter(name -> name.startsWith(args[4].toLowerCase()))
                            .sorted()
                            .toList();
                    } else {
                        return Collections.emptyList();
                    }
                } else {
                    return Collections.emptyList();
                }
            } else if (args[0].equalsIgnoreCase("mute") && args[1].equalsIgnoreCase("temp")) {
                return Collections.singletonList("reason");
            } else {
                return Collections.emptyList();
            }
        }

        if (args.length == 6 && args[0].equalsIgnoreCase("channels")) {
            if (args[1].equalsIgnoreCase("create"))
                return Collections.singletonList("worlds");

            if (args[1].equalsIgnoreCase("edit")) {
                if (channel == null) {
                    return Collections.emptyList();
                }

                if (args[3].equalsIgnoreCase("worlds")) {
                    if (args[4].equalsIgnoreCase("add")) {

                        return Bukkit.getWorlds().stream()
                            .map(WorldInfo::getName)
                            .filter(name -> !channel.hasWorld(name))
                            .filter(name -> name.startsWith(args[5].toLowerCase()))
                            .sorted()
                            .toList();

                    } else if (args[4].equalsIgnoreCase("remove")) {
                        if (channel.getWorlds() == null) {
                            return Collections.emptyList();
                        }

                        return channel.getWorlds().stream()
                            .filter(name -> name.startsWith(args[5].toLowerCase()))
                            .toList();

                    } else {
                        return Collections.emptyList();
                    }
                } else {
                    return Collections.emptyList();
                }
            } else {
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }

    private void sendNoPermissionMessage(CommandSender sender) {
        Component msg = YoChatAPI.getPlugin().getMessageParseManager().parseAdmin(ConfigManager.getInstance().getNoPermissionMessage());
        sender.sendMessage(msg);
    }

    private void sendUnmuteWebhook(String targetname, @Nullable String reason, String senderName) {
        debug("Queueing unmute webhook for target=%s pardoner=%s", targetname, senderName);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            WebhookPayload.Field targetField = WebhookPayload.Field.builder()
                    .name("Target")
                    .value(targetname)
                    .build();

            WebhookPayload.Field reasonField = WebhookPayload.Field.builder()
                    .name("Reason")
                    .value(Objects.requireNonNullElse(reason, "No reason provided"))
                    .build();

            WebhookPayload.Field senderField = WebhookPayload.Field.builder()
                    .name("Pardoner")
                    .value(senderName)
                    .build();

            List<WebhookPayload.Field> fieldList = List.of(targetField, reasonField, senderField);

            WebhookPayload.Embed embed = WebhookPayload.Embed.builder()
                    .title("Player Unmuted")
                    .color(11119017)
                    .description("A player has been unmuted in-game.")
                    .fields(fieldList)
                    .build();

            WebhookPayload payload = WebhookPayload.builder()
                    .embeds(List.of(embed))
                    .build();

            plugin.getDiscord().sendMessage(payload, ConfigManager.getInstance().getUnmuteWebhookUrl());
        });
    }

    private void sendMuteWebhook(String targetname, String duration, @Nullable String reason, String sendername) {
        debug("Queueing mute webhook for target=%s duration=%s punisher=%s", targetname, duration, sendername);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            WebhookPayload.Field targetField = WebhookPayload.Field.builder()
                    .name("Target")
                    .value(targetname)
                    .build();

            WebhookPayload.Field durationField = WebhookPayload.Field.builder()
                    .name("Duration")
                    .value(duration)
                    .build();

            WebhookPayload.Field reasonField = WebhookPayload.Field.builder()
                    .name("Reason")
                    .value(Objects.requireNonNullElse(reason, "No reason provided"))
                    .build();

            WebhookPayload.Field executorField = WebhookPayload.Field.builder()
                    .name("Punisher")
                    .value(sendername)
                    .build();

            List<WebhookPayload.Field> fieldList = List.of(targetField, durationField, reasonField, executorField);

            WebhookPayload.Embed embed = WebhookPayload.Embed.builder()
                    .title("Player Muted")
                    .color(15158332)
                    .description("A player has been muted in-game.")
                    .fields(fieldList)
                    .build();

            WebhookPayload payload = WebhookPayload.builder()
                    .embeds(List.of(embed))
                    .build();

            plugin.getDiscord().sendMessage(payload, ConfigManager.getInstance().getMuteWebhookUrl());
        });
    }

    private void debug(String format, Object... args) {
        ConfigManager configManager = config();
        if (configManager != null) {
            configManager.debug(format, args);
        }
    }

    private ConfigManager config() {
        return ConfigManager.getInstance();
    }
}
