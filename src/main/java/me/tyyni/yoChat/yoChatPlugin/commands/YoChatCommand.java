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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YoChatCommand implements TabExecutor {
    private final YoChat plugin = YoChatAPI.getPlugin();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player && !sender.hasPermission("yochat.commands")) {
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
            sendNoPermissionMessage(sender);
            return;
        }

        YoChat api = YoChatAPI.getPlugin();
        ConfigManager configManager = ConfigManager.getInstance();
        try {
            ConfigUpdater.update(plugin, "config.yml", new File(plugin.getDataFolder(), "config.yml"));
            configManager.load();
            api.getChatManager().reloadBlockedWords();
            api.getMessageParseManager().setupMM();
            api.getMuteManager().setInterval(api.getChatManager().parseDuration(configManager.getMuteCheckerInterval()) * 20);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update config.yml: " + e.getMessage());
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
                    if(joinedWorlds != null) {
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
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("No channel found with that name!", NamedTextColor.RED)));
                    return;
                }
                YoChatAPI.getPlugin().getChannelManager().deleteChannel(channelName);
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
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("No channel found with that name!", NamedTextColor.RED)));
                    return;
                }

                if(channel.isMember(player)) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("You are already in this channel!", NamedTextColor.RED)
                    ));
                    return;
                }

                YoChatAPI.getPlugin().getChannelManager().joinChannel(player, channel);
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Joined channel ", plugin.getMainColor())
                                .append(Component.text("'" + channelName + "'", plugin.getHighlightColor()))));
            } case "edit" -> {
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
                        if(edit.equalsIgnoreCase("-")) {
                            channel.setPermission(null);

                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Permission removed", plugin.getMainColor())));
                            return;
                        }

                        channel.setPermission(edit);

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Permission set to ", plugin.getMainColor())
                                        .append(Component.text("'"+ edit + "'", plugin.getHighlightColor()))
                        ));
                    } case "name" -> {
                        String edit = args[4];
                        channel.setName(edit);

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Name changed to ", plugin.getMainColor())
                                        .append(Component.text("'"+ edit + "'", plugin.getHighlightColor()))
                        ));
                    } case "strict" -> {
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

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Strict world set to ", plugin.getMainColor())
                                        .append(Component.text("'"+ strict + "'", plugin.getHighlightColor()))
                        ));
                    } case "radius" -> {
                        String radiusString = args[4];
                        int radius;
                        try {
                            radius = Integer.parseInt(radiusString);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Radius has to be a number!", NamedTextColor.RED)
                            ));
                            return;
                        }

                        channel.setRadius(radius);

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Radius set to ", plugin.getMainColor())
                                        .append(Component.text("'"+ radius + "'", plugin.getHighlightColor()))
                        ));
                    } case "worlds" -> {
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
                                 if(Bukkit.getWorld(worldName) == null) {
                                     sender.sendMessage(plugin.getYoChatPrefix().append(
                                             Component.text("World ", NamedTextColor.RED))
                                                     .append(Component.text("'"+ worldName +"'", plugin.getHighlightColor())
                                                             .append(Component.text(" doesn't exist! ", NamedTextColor.RED)
                                     )));

                                     return;
                                 }

                                 channel.addWorld(worldName);

                                 sender.sendMessage(plugin.getYoChatPrefix().append(
                                         Component.text("Added world ", plugin.getMainColor())
                                                 .append(Component.text("'"+ worldName +"'", plugin.getHighlightColor())
                                 )));
                            } case "remove" -> {
                                if (args.length < 6) {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("Usage: /yochat channels edit <channel> worlds remove <world>", NamedTextColor.RED)
                                    ));
                                    return;
                                }
                                String worldName = args[5];

                                if(!channel.hasWorld(worldName)) {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("This channel doesn't contain the world ", plugin.getMainColor())
                                                    .append(Component.text("'"+ worldName +"'", plugin.getHighlightColor()))

                                    ));

                                    return;
                                }

                                channel.removeWorld(worldName);

                                sender.sendMessage(plugin.getYoChatPrefix().append(
                                        Component.text("Removed world ", plugin.getMainColor())
                                                .append(Component.text("'"+ worldName +"'", plugin.getHighlightColor()))
                                ));
                            } case "list" -> {
                                Set<String> worlds = channel.getWorlds();

                                List<Component> worldComponents = worlds != null ? worlds.stream()
                                                                                   .map(name -> Component.text(name, plugin.getHighlightColor()))
                                                                                   .collect(Collectors.toList()) : null;
                                Component joinedWorlds;
                                if (worldComponents != null) {
                                    joinedWorlds = Component.join(
                                            JoinConfiguration.separator(Component.text(", ", plugin.getMainColor())),
                                            worldComponents
                                    );
                                } else {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("No worlds in this channel", NamedTextColor.RED)
                                    ));

                                    return;
                                }

                                sender.sendMessage(plugin.getYoChatPrefix().append(
                                        Component.text("List of worlds in this channel: ", plugin.getMainColor())
                                                .append(joinedWorlds)
                                ));
                            } default -> sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Invalid operation!", NamedTextColor.RED)
                            ));
                        }
                    }
                    default -> sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("Invalid type of edit!", NamedTextColor.RED)
                    ));
                }
            } case "leave" -> {
                if (!sender.hasPermission("yochat.commands.channels.leave")) {
                    sendNoPermissionMessage(sender);
                    return;
                }

                if (args.length < 3) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("Usage: /yochat channels leave <channel>", NamedTextColor.RED)));
                    return;
                }

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("Only players can use this command!", NamedTextColor.RED)));
                    return;
                }

                String channelName = args[2];
                ChatChannel channel = YoChatAPI.getPlugin().getChannelManager().getChannel(channelName);
                if (channel == null) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("No channel found with that name!", NamedTextColor.RED)));
                    return;
                }

                if(!channel.isMember(player)) {
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("You don't belong to this channel!", NamedTextColor.RED)
                    ));
                    return;
                }

                channel.removeMember(player);

                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Left from channel ", plugin.getMainColor())
                                .append(Component.text("'"+ channelName + "'", plugin.getHighlightColor()))
                ));
            } case "members" -> {
                if (!sender.hasPermission("yochat.commands.channels.members")) {
                    sendNoPermissionMessage(sender);
                    return;
                }

                if(args.length < 4) {
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
                        if(args.length < 5) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Usage: /yochat channels members <channel> add <player>!", NamedTextColor.RED)
                            ));

                            return;
                        }

                        String membername = args[4];
                        Player player = Bukkit.getPlayer(membername);
                        if(player == null) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Player not found!", NamedTextColor.RED)
                            ));

                            return;
                        }

                        channel.addMember(player);

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Added member ", plugin.getMainColor())
                                        .append(Component.text("'" + membername + "'", plugin.getHighlightColor()))
                        ));
                    } case "remove" -> {
                        if(args.length < 5) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Usage: /yochat channels members <channel> remove <player>!", NamedTextColor.RED)
                            ));

                            return;
                        }

                        String membername = args[4];
                        Player player = Bukkit.getPlayer(membername);
                        if(player == null) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Player not found!", NamedTextColor.RED)
                            ));

                            return;
                        }

                        channel.removeMember(player);

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Removed member ", plugin.getMainColor())
                                        .append(Component.text("'" + membername + "'", plugin.getHighlightColor()))
                        ));
                    } case "list" -> {
                        List<Component> membersList = channel.getMembers().stream()
                                        .map(p -> Component.text(p.getName(), plugin.getHighlightColor()))
                                        .collect(Collectors.toList());

                        Component members;
                        if(!membersList.isEmpty()) {
                            members = Component.join(
                                    JoinConfiguration.separator(Component.text(", ", plugin.getMainColor())),
                                    membersList
                            );
                        } else {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("No members in this channel", NamedTextColor.RED)
                            ));

                            return;
                        }

                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("Channel's members: ", plugin.getMainColor())
                                        .append(members)
                        ));
                    } default -> sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("Invalid operation!", NamedTextColor.RED)
                    ));
                }
            }
            default -> sender.sendMessage(plugin.getYoChatPrefix().append(
                    Component.text("Unknown subcommand. Use /yochat help for a list of commands.", NamedTextColor.RED)));
        }
    }

    private void handleMute(CommandSender sender, String[] args) {
        if (!ConfigManager.getInstance().isModerationEnabled()) {
            sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("The moderation system is not enabled!", NamedTextColor.RED)));
            return;
        }

        if (sender instanceof Player && !sender.hasPermission("yochat.commands.mute")) {
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

        if (MuteManager.isMuted(uuid.toString())) {
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
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("Incorrect timeformat! Example: 10m, 2h, 7d", NamedTextColor.RED).decoration(TextDecoration.BOLD, false)));
                return;
            }
            if (args.length > 4) reason = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
            duration = YoChatAPI.getPlugin().getChatManager().parseDuration(timeStr);

            MutedPlayer mutedPlayer = new MutedPlayer(uuid, duration, System.currentTimeMillis(), reason, sender.getName());
            YoChatAPI.addMutedPlayer(mutedPlayer);

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
        }

        MuteManager.getInstance().save();
    }

    private void handleUnmute(CommandSender sender, String[] args) {
        if (!ConfigManager.getInstance().isModerationEnabled()) {
            sender.sendMessage(plugin.getYoChatPrefix().append(Component.text("The moderation system is not enabled!", NamedTextColor.RED)));
            return;
        }

        if (sender instanceof Player && !sender.hasPermission("yochat.commands.unmute")) {
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

        if (MuteManager.isMuted(uuid.toString())) {
            MutedPlayer mutedPlayer = YoChatAPI.getMutedPlayer(uuid);
            YoChatAPI.removeMutedPlayer(mutedPlayer);

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

        } else {
            sender.sendMessage(plugin.getYoChatPrefix().append(
                    Component.text("Player ", NamedTextColor.RED)
                            .append(Component.text("'" + playerName + "'", plugin.getHighlightColor())
                                    .append(Component.text(" is not muted!", NamedTextColor.RED)))));
        }

        MuteManager.getInstance().save();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("yochat.tabcomplete")) return Collections.emptyList();

        if (args.length == 1) {
            return Stream.of("help", "channels", "reload", "mute", "unmute")
                    .filter(opt -> opt.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .toList();
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("channels")) {
                return Stream.of("list", "create", "delete", "join", "leave", "members", "edit")
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
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("channels")) {
                if (args[1].equalsIgnoreCase("delete")
                        || args[1].equalsIgnoreCase("join")
                        || args[1].equalsIgnoreCase("leave")
                        || args[1].equalsIgnoreCase("members")
                        || args[1].equalsIgnoreCase("edit")) {
                    return YoChatAPI.getPlugin().getChannelManager().getChannels().stream()
                            .map(ChatChannel::getName)
                            .filter(opt -> opt.toLowerCase().startsWith(args[2].toLowerCase()))
                            .sorted()
                            .toList();
                } else if (args[1].equalsIgnoreCase("create")) {
                    return Collections.singletonList("name");
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
                }
            } else if (args[0].equalsIgnoreCase("mute")) {
                if (args[1].equalsIgnoreCase("perm")) return Collections.singletonList("reason");
                if (args[1].equalsIgnoreCase("temp")) return Collections.singletonList("time");
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
                        return Stream.of("add", "remove", "list")
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
                    }
                }
            } else if (args[0].equalsIgnoreCase("mute") && args[1].equalsIgnoreCase("temp")) {
                return Collections.singletonList("reason");
            }
        }

        if (args.length == 6 && args[0].equalsIgnoreCase("channels") && args[1].equalsIgnoreCase("create")) {
            return Collections.singletonList("worlds");
        }

        return Collections.emptyList();
    }

    private void sendNoPermissionMessage(CommandSender sender) {
        sender.sendMessage(Component.text("You don't have permission to execute this command!", NamedTextColor.RED));
    }

    private void sendUnmuteWebhook(String targetname, @Nullable String reason, String senderName) {
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
}
