package me.tyyni.yoChat.yoChatPlugin.commands;

import com.tchristofferson.configupdater.ConfigUpdater;
import lombok.extern.slf4j.Slf4j;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;
import me.tyyni.yoChat.yoChatPlugin.MuteManager;
import me.tyyni.yoChat.yoChatPlugin.YoChat;
import me.tyyni.yoChat.yoChatPlugin.objects.MutedPlayer;
import me.tyyni.yoChat.yoChatPlugin.webhook.Discord;
import me.tyyni.yoChat.yoChatPlugin.webhook.WebhookPayload;
import me.tyyni.yoChat.yochatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
public class YoChatCommand implements TabExecutor {
    private final YoChat plugin = YoChatAPI.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender.hasPermission("yochat.commands")) {
            if (args.length == 0) {
                sender.sendMessage(plugin.getYoChatPrefix().append(
                        Component.text("YoChat version " + YoChatAPI.getInstance().getPluginMeta().getVersion(), plugin.getMainColor())));
                sender.sendMessage(Component.text("For help, use /yochat help", plugin.getMainColor()));

                return true;
            }

            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("help")) {
                    if (sender.hasPermission("yochat.commands.help")) {
                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("YoChat Commands:", plugin.getMainColor())));

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
                    } else {
                        sendNoPermissionMessage(sender);
                    }

                    return true;
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("yochat.commands.reload")) {
                        try {
                            ConfigUpdater.update(plugin, "config.yml", new File(plugin.getDataFolder(), "config.yml"));
                            ConfigManager.getInstance().load();
                        } catch (IOException e) {
                            log.error("Failed to update config.yml", e);
                        }
                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("YoChat config reloaded!", plugin.getMainColor())));
                    } else {
                        sendNoPermissionMessage(sender);

                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("channels")) {
                    if (sender.hasPermission("yochat.commands.channels")) {
                        if (ConfigManager.getInstance().isUseChannelSystem()) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Channel management commands:", plugin.getMainColor())));

                            sender.sendMessage(Component.text("/yochat channels create <name> ", plugin.getMainColor())
                                    .append(Component.text("- Creates a new chat channel", plugin.getHighlightColor())));

                            sender.sendMessage(Component.text("/yochat channels delete <name> ", plugin.getMainColor())
                                    .append(Component.text("- Deletes a chat channel", plugin.getHighlightColor())));

                            sender.sendMessage(Component.text("/yochat channels list ", plugin.getMainColor())
                                    .append(Component.text("- Lists all channels", plugin.getHighlightColor())));

                            sender.sendMessage(Component.text("/yochat channels join <name> ", plugin.getMainColor())
                                    .append(Component.text("- Makes you join a chat channel", plugin.getHighlightColor())));
                        } else {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("The channel system is not enabled", NamedTextColor.RED)
                            ));
                        }
                        return true;
                    } else {
                        sendNoPermissionMessage(sender);
                    }

                    return true;

                } else if (args[0].equalsIgnoreCase("mute")) {
                    if (sender.hasPermission("yochat.commands.mute")) {
                        if (ConfigManager.getInstance().isModerationEnabled()) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Mute commands:", plugin.getMainColor())));

                            sender.sendMessage(Component.text("/yochat mute <perm|temp> <player> <time if temp> [reason]", plugin.getMainColor())
                                    .append(Component.text(" - Mutes the player", plugin.getHighlightColor())));

                            sender.sendMessage(Component.text("/yochat unmute <player> [reason]", plugin.getMainColor())
                                    .append(Component.text(" - Unmutes the player", plugin.getHighlightColor())));
                        } else {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("The moderation system is not enabled!", NamedTextColor.RED)
                            ));
                        }
                        return true;

                    } else {
                        sendNoPermissionMessage(sender);
                    }

                    return true;
                } else if (args[0].equalsIgnoreCase("unmute")) {
                    if (sender.hasPermission("yochat.commands.unmute")) {
                        if (ConfigManager.getInstance().isModerationEnabled()) {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Mute commands:", plugin.getMainColor())));

                            sender.sendMessage(Component.text("/yochat mute <perm|temp> <player> <time if temp> [reason]", plugin.getMainColor())
                                    .append(Component.text(" - Mutes the player", plugin.getHighlightColor())));

                            sender.sendMessage(Component.text("/yochat unmute <player> [reason]", plugin.getMainColor())
                                    .append(Component.text(" - Unmutes the player", plugin.getHighlightColor())));
                        } else {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("The moderation system is not enabled!", NamedTextColor.RED)
                            ));
                        }

                        return true;
                    } else {
                        sendNoPermissionMessage(sender);
                    }
                    return true;
                } else {
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("Unknown subcommand. Use /yochat help for a list of commands.", NamedTextColor.RED)));
                    return true;
                }
            }

            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("channels")) {
                    if (sender.hasPermission("yochat.commands.channels")) {
                        if (ConfigManager.getInstance().isUseChannelSystem()) {
                            if (args[1].equalsIgnoreCase("list")) {
                                if (sender.hasPermission("yochat.commands.channels.list")) {
                                    List<String> channels = YoChatAPI.getInstance().getChannelManager().getChannels().stream()
                                            .map(ChatChannel::getName)
                                            .toList();
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("Channels:", plugin.getMainColor())));
                                    channels.forEach(channelName -> sender.sendMessage(Component.text("- ", plugin.getMainColor())
                                            .append(Component.text(channelName, plugin.getHighlightColor()))));
                                } else {
                                    sendNoPermissionMessage(sender);
                                }
                                return true;

                            } else if (args[1].equalsIgnoreCase("create")) {
                                if (sender.hasPermission("yochat.commands.channels.create")) {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("Usage: /yochat channels create <name> <permission> <radius>", NamedTextColor.RED)));
                                } else {
                                    sendNoPermissionMessage(sender);
                                }
                                return true;

                            } else if (args[1].equalsIgnoreCase("delete")) {
                                if (sender.hasPermission("yochat.commands.channels.delete")) {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("Usage: /yochat channels delete <name>", NamedTextColor.RED)));
                                } else {
                                    sendNoPermissionMessage(sender);
                                }
                                return true;

                            } else if (args[1].equalsIgnoreCase("join")) {
                                if (sender.hasPermission("yochat.commands.channels.join")) {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("Usage: /yochat channels join <name>", NamedTextColor.RED)));
                                } else {
                                    sendNoPermissionMessage(sender);
                                }
                                return true;

                            } else {
                                sender.sendMessage(plugin.getYoChatPrefix().append(
                                        Component.text("Unknown subcommand. Use /yochat help for a list of commands.", NamedTextColor.RED)));
                                return true;
                            }
                        } else {
                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("The channel system is not enabled", NamedTextColor.RED)
                            ));
                        }
                        return true;
                    } else {
                        sendNoPermissionMessage(sender);
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("unmute")) {
                    if (ConfigManager.getInstance().isModerationEnabled()) {
                        if (sender.hasPermission("yochat.commands.unmute")) {

                            String playerName = args[1];
                            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                            UUID uuid = offlinePlayer.getUniqueId();

                            if (MuteManager.isMuted(uuid.toString())) {
                                MutedPlayer mutedPlayer = YoChatAPI.getMutedPlayer(uuid);
                                YoChatAPI.removeMutedPlayer(mutedPlayer);

                                String name = offlinePlayer.getName();
                                if (name == null) {
                                    name = uuid.toString();
                                }

                                sender.sendMessage(plugin.getYoChatPrefix().append(
                                        Component.text("Unmuted player ", plugin.getMainColor())
                                                .append(Component.text("'" + name + "'", plugin.getHighlightColor())))
                                );

                                WebhookPayload.Embed embed = WebhookPayload.Embed.builder()
                                        .title("Player Unmuted")
                                        .color(11119017)
                                        .fields(List.of(
                                                WebhookPayload.Field.builder().name("Target").value(name).build(),
                                                WebhookPayload.Field.builder().name("Action by").value(sender.getName()).build()
                                        ))
                                        .build();

                                WebhookPayload payload = WebhookPayload.builder()
                                        .username("YoChat Logger")
                                        .embeds(List.of(embed))

                                        .build();

                                Discord discord = new Discord();
                                Discord.sendMessage(payload, discord.getUNMUTE_CHANNEL_URL());
                            }

                            MuteManager.getInstance().save();
                        } else {
                            sendNoPermissionMessage(sender);
                        }
                        return true;
                    } else {
                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("The moderation system is not enabled!", NamedTextColor.RED)
                        ));
                    }

                    return true;
                } else if (args[0].equalsIgnoreCase("mute")) {
                    if (ConfigManager.getInstance().isModerationEnabled()) {
                        if (sender.hasPermission("yochat.commands.mute")) {

                            sender.sendMessage(plugin.getYoChatPrefix().append(
                                    Component.text("Usage: /yochat mute <perm|temp> <player> <time if temp> [reason]", NamedTextColor.RED)));
                        } else {
                            sendNoPermissionMessage(sender);
                        }

                        return true;
                    } else {
                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("The moderation system is not enabled!", NamedTextColor.RED)
                        ));
                    }
                    return true;
                }
            }

            if (args.length > 2) {
                if (args[0].equalsIgnoreCase("unmute")) {
                    if (ConfigManager.getInstance().isModerationEnabled()) {
                        if (sender.hasPermission("yochat.commands.unmute")) {
                            String playerName = args[1];
                            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                            UUID uuid = offlinePlayer.getUniqueId();

                            if (MuteManager.isMuted(uuid.toString())) {
                                MutedPlayer mutedPlayer = YoChatAPI.getMutedPlayer(uuid);
                                YoChatAPI.removeMutedPlayer(mutedPlayer);

                                String name = offlinePlayer.getName();
                                if (name == null) {
                                    name = uuid.toString();
                                }

                                String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                                sender.sendMessage(plugin.getYoChatPrefix().append(
                                        Component.text("Unmuted player ", plugin.getMainColor())
                                                .append(Component.text("'" + name + "'", plugin.getHighlightColor())
                                                        .append(Component.text(" for the reason ", plugin.getMainColor())
                                                                .append(Component.text("'" + reason + "'", plugin.getHighlightColor())))))
                                );

                                WebhookPayload.Embed embed = WebhookPayload.Embed.builder()
                                        .title("Player Unmuted")
                                        .color(11119017)
                                        .fields(List.of(
                                                WebhookPayload.Field.builder().name("Target").value(name).build(),
                                                WebhookPayload.Field.builder().name("Reason").value(reason).build(),
                                                WebhookPayload.Field.builder().name("Action by").value(sender.getName()).build()
                                        ))
                                        .build();

                                WebhookPayload payload = WebhookPayload.builder()
                                        .username("YoChat Logger")
                                        .embeds(List.of(embed))

                                        .build();

                                Discord discord = new Discord();
                                Discord.sendMessage(payload, discord.getUNMUTE_CHANNEL_URL());
                            }

                            MuteManager.getInstance().save();
                        } else {
                            sendNoPermissionMessage(sender);
                        }

                        return true;
                    } else {
                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("The moderation system is not enabled!", NamedTextColor.RED)
                        ));
                    }
                    return true;
                }
            }

            if (args.length == 3) {
                if (args[0].equalsIgnoreCase("channels")) {
                    if (ConfigManager.getInstance().isUseChannelSystem()) {
                        if (sender.hasPermission("yochat.channels")) {

                            if (args[1].equalsIgnoreCase("delete")) {
                                if (sender.hasPermission("yochat.channels.delete")) {
                                    String channelName = args[2];
                                    if (YoChatAPI.getInstance().getChannelManager().getChannel(channelName) == null) {
                                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                                Component.text("No channel found with that name!", NamedTextColor.RED)));
                                        return true;
                                    }

                                    YoChatAPI.getInstance().getChannelManager().deleteChannel(channelName);
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("Channel ", plugin.getMainColor())
                                                    .append(Component.text("'" + channelName + "'", plugin.getHighlightColor())
                                                            .append(Component.text(" deleted!", plugin.getMainColor())))));

                                } else {
                                    sendNoPermissionMessage(sender);
                                }
                                return true;
                            } else if (args[1].equalsIgnoreCase("join")) {
                                if (sender.hasPermission("yochat.channels.join")) {
                                    String channelName = args[2];
                                    if (YoChatAPI.getInstance().getChannelManager().getChannel(channelName) == null) {
                                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                                Component.text("No channel found with that name!", NamedTextColor.RED)));
                                        return true;
                                    }

                                    if (sender instanceof Player player) {
                                        ChatChannel channel = YoChatAPI.getInstance().getChannelManager().getChannel(channelName);
                                        YoChatAPI.getInstance().getChannelManager().joinChannel(player, channel);
                                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                                Component.text("Joined channel ", plugin.getMainColor())
                                                        .append(Component.text("'" + channelName + "'", plugin.getHighlightColor())
                                                        )));
                                    } else {
                                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                                Component.text("Only players can use this command!", NamedTextColor.RED)));
                                    }

                                    return true;
                                } else {
                                    sendNoPermissionMessage(sender);
                                }

                                return true;
                            } else {
                                sender.sendMessage(plugin.getYoChatPrefix().append(
                                        Component.text("Unknown subcommand. Use /yochat help for a list of commands.", NamedTextColor.RED)));
                                return true;
                            }
                        } else {
                            sendNoPermissionMessage(sender);
                            return true;
                        }
                    } else {
                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("The channel system is not enabled", NamedTextColor.RED)
                        ));
                    }
                } else if (args[0].equalsIgnoreCase("mute")) {
                    if (ConfigManager.getInstance().isModerationEnabled()) {
                        if (sender.hasPermission("yochat.commands.mute")) {
                            if (args[1].equalsIgnoreCase("perm")) {
                                String playername = args[2];
                                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playername);

                                UUID uuid = offlinePlayer.getUniqueId();

                                if (!MuteManager.isMuted(uuid.toString())) {
                                    MutedPlayer mutedPlayer = new MutedPlayer(uuid, -1L, System.currentTimeMillis(), null, sender.getName());
                                    YoChatAPI.addMutedPlayer(mutedPlayer);

                                    String name = offlinePlayer.getName();
                                    if (name == null) {
                                        name = uuid.toString();
                                    }

                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("Muted player ", plugin.getMainColor())
                                                    .append(Component.text("'" + name + "'", plugin.getHighlightColor()))
                                    ));

                                    if (ConfigManager.getInstance().isWebhookEnabled()) {
                                        WebhookPayload.Embed embed = WebhookPayload.Embed.builder()
                                                .description("👤 **" + name + "** has been muted by **" + sender.getName() + "**")
                                                .color(15158332)
                                                .build();

                                        WebhookPayload payload = WebhookPayload.builder()
                                                .username("YoChat Logs")
                                                .embeds(List.of(embed))
                                                .build();

                                        Discord discord = new Discord();
                                        Discord.sendMessage(payload, discord.getMUTE_CHANNEL_URL());
                                    }

                                    MuteManager.getInstance().save();
                                } else {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("Player ", NamedTextColor.RED)
                                                    .append(Component.text("'" + playername + "'", plugin.getHighlightColor())
                                                            .append(Component.text("is muted already!", NamedTextColor.RED)))

                                    ));

                                }
                                return true;
                            }
                        } else {
                            sendNoPermissionMessage(sender);
                        }

                        return true;
                    } else {
                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("The moderation system is not enabled!", NamedTextColor.RED)
                        ));
                    }

                    return true;
                }
            }

            if (args.length == 5) {
                if (args[0].equalsIgnoreCase("channels")) {
                    if (sender.hasPermission("yochat.channels")) {
                        if (args[1].equalsIgnoreCase("create")) {
                            if (sender.hasPermission("yochat.channels.create")) {
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

                                ChatChannel channel = YoChatAPI.getInstance().getChannelManager().createChannel(channelName, permission, radius);
                                if (channel != null) {
                                    YoChatAPI.registerChannel(channel);

                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                                    Component.text("Channel ", plugin.getMainColor())
                                                            .append(Component.text("'" + channelName + "'", plugin.getHighlightColor())
                                                                    .append(Component.text(" created with permission ", plugin.getMainColor())
                                                                            .append(Component.text("'" + permission + "'", plugin.getHighlightColor())
                                                                                    .append(Component.text(" and radius ", plugin.getMainColor())
                                                                                            .append(Component.text("'" + radius + "'!", plugin.getHighlightColor())
                                                                                            )
                                                                                    )
                                                                            )
                                                                    )
                                                            )
                                            )
                                    );

                                    return true;
                                }

                            } else {
                                sendNoPermissionMessage(sender);
                            }
                            return true;
                        }
                    } else {
                        sendNoPermissionMessage(sender);
                    }
                }
            }

            if (args.length > 3) {
                if (ConfigManager.getInstance().isModerationEnabled()) {
                    if (sender.hasPermission("yochat.commands.mute")) {
                        if (args[0].equalsIgnoreCase("mute")) {
                            if (args[1].equalsIgnoreCase("perm")) {

                                String playername = args[2];
                                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playername);
                                UUID uuid = offlinePlayer.getUniqueId();

                                sender.sendMessage(Component.text("Yritetään mutettaa: " + playername + " (UUID: " + uuid + ")", NamedTextColor.RED));

                                if (!MuteManager.isMuted(uuid.toString())) {
                                    String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

                                    MutedPlayer mutedPlayer = new MutedPlayer(uuid, -1L, System.currentTimeMillis(), reason, sender.getName());
                                    YoChatAPI.addMutedPlayer(mutedPlayer);

                                    String name = offlinePlayer.getName();
                                    if (name == null) {
                                        name = uuid.toString();
                                    }

                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                                    Component.text("Muted player ", plugin.getMainColor())
                                                            .append(Component.text("'" + name + "'", plugin.getHighlightColor())
                                                                    .append(Component.text(" for the reason ", plugin.getHighlightColor())
                                                                            .append(Component.text("'" + reason + "'", plugin.getHighlightColor())
                                                                            )
                                                                    )
                                                            )
                                            )
                                    );

                                    if (ConfigManager.getInstance().isWebhookEnabled()) {
                                        WebhookPayload.Embed embed = WebhookPayload.Embed.builder()
                                                .title("Permanent Mute Issued")
                                                .color(15158332)
                                                .fields(List.of(
                                                        WebhookPayload.Field.builder().name("Player").value(name).build(),
                                                        WebhookPayload.Field.builder().name("Type").value("Permanent").build(),
                                                        WebhookPayload.Field.builder().name("Reason").value(reason).build(),
                                                        WebhookPayload.Field.builder().name("Punisher").value(sender.getName()).build()
                                                ))
                                                .build();

                                        WebhookPayload payload = WebhookPayload.builder()
                                                .username("YoChat Logger")
                                                .embeds(List.of(embed))

                                                .build();
                                        Discord discord = new Discord();
                                        Discord.sendMessage(payload, discord.getMUTE_CHANNEL_URL());
                                    }

                                    MuteManager.getInstance().save();
                                } else {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("Player ", NamedTextColor.RED)
                                                    .append(Component.text("'" + playername + "'", plugin.getHighlightColor())
                                                            .append(Component.text(" is muted already!", NamedTextColor.RED)))

                                    ));
                                }

                                return true;
                            }
                        }
                    } else {
                        sendNoPermissionMessage(sender);
                    }

                } else {
                    sender.sendMessage(plugin.getYoChatPrefix().append(
                            Component.text("The moderation system is not enabled!", NamedTextColor.RED)
                    ));
                }
            }


            /*
             * Merge this into the "arg.length > 3" above!!!
             */

            if (args.length > 4) {
                if (args[0].equalsIgnoreCase("mute")) {
                    if (ConfigManager.getInstance().isModerationEnabled()) {
                        if (sender.hasPermission("yochat.commands.mute")) {
                            if (args[1].equalsIgnoreCase("temp")) {
                                String playername = args[2];
                                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playername);

                                UUID uuid = offlinePlayer.getUniqueId();

                                if (!MuteManager.isMuted(uuid.toString())) {
                                    String duration = args[3];
                                    if (!duration.matches("\\d+(y|mo|w|d|h|m|s)")) {
                                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                                Component.text("Incorrect timeformat! Example: 10m, 2h, 7d",
                                                                NamedTextColor.RED)
                                                        .decoration(TextDecoration.BOLD, false)));
                                        return true;
                                    }

                                    String reason = String.join(" ",
                                            Arrays.copyOfRange(args, 4, args.length));

                                    long parsedDuration = YoChatAPI.getInstance().getChatManager().parseDuration(duration);

                                    MutedPlayer mutedPlayer = new MutedPlayer(uuid, parsedDuration, System.currentTimeMillis(), reason, sender.getName());
                                    YoChatAPI.addMutedPlayer(mutedPlayer);

                                    String name = offlinePlayer.getName();
                                    if (name == null) {
                                        name = uuid.toString();
                                    }

                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                                    Component.text("Muted player ", plugin.getMainColor())
                                                            .append(Component.text("'" + name + "'", plugin.getHighlightColor())
                                                                    .append(Component.text(" with the time ", plugin.getMainColor())
                                                                            .append(Component.text("'" + duration + "'", plugin.getHighlightColor())
                                                                                    .append(Component.text(" for the reason ", plugin.getMainColor())
                                                                                            .append(Component.text("'" + reason + "'", plugin.getHighlightColor())
                                                                                            )
                                                                                    )
                                                                            )
                                                                    )
                                                            )
                                            )
                                    );

                                    if (ConfigManager.getInstance().isWebhookEnabled()) {
                                        WebhookPayload.Field targetField = WebhookPayload.Field.builder()
                                                .name("Target")
                                                .value(name)
                                                .build();

                                        WebhookPayload.Field durationField = WebhookPayload.Field.builder()
                                                .name("Duration")
                                                .value(duration)
                                                .build();

                                        WebhookPayload.Field reasonField = WebhookPayload.Field.builder()
                                                .name("Reason")
                                                .value(reason)
                                                .build();

                                        WebhookPayload.Field executorField = WebhookPayload.Field.builder()
                                                .name("Punisher")
                                                .value(sender.getName())
                                                .build();

                                        WebhookPayload.Embed embed = WebhookPayload.Embed.builder()
                                                .title("Player Muted")
                                                .color(15158332)
                                                .description("A player has been muted in-game.")
                                                .fields(List.of(targetField, durationField, reasonField, executorField))
                                                .build();

                                        WebhookPayload payload = WebhookPayload.builder()
                                                .username("YoChat Logger")
                                                .embeds(List.of(embed))
                                                .build();

                                        Discord discord = new Discord();
                                        Discord.sendMessage(payload, discord.getMUTE_CHANNEL_URL());
                                    }

                                    MuteManager.getInstance().save();
                                } else {
                                    sender.sendMessage(plugin.getYoChatPrefix().append(
                                            Component.text("Player ", NamedTextColor.RED)
                                                    .append(Component.text("'" + playername + "'", plugin.getHighlightColor())
                                                            .append(Component.text(" is muted already!", NamedTextColor.RED)))

                                    ));
                                }

                                return true;
                            }
                        } else {
                            sendNoPermissionMessage(sender);
                            return true;
                        }

                    } else {
                        sender.sendMessage(plugin.getYoChatPrefix().append(
                                Component.text("The moderation system is not enabled!", NamedTextColor.RED)
                        ));
                    }

                    return true;
                }
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender.hasPermission("yochat.tabcomplete")) {
            if (args.length == 1) {
                return Stream.of("help", "channels", "reload", "mute", "unmute")
                        .filter(opt -> opt.startsWith(args[0]))
                        .toList();
            }

            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("channels")) {
                    return Stream.of("list", "create", "delete", "join")
                            .filter(opt -> opt.startsWith(args[1]))
                            .toList();
                } else if (args[0].equalsIgnoreCase("mute")) {
                    return Stream.of("temp", "perm")
                            .filter(opt -> opt.startsWith(args[1]))
                            .toList();
                } else if (args[0].equalsIgnoreCase("unmute")) {
                    return Arrays.stream(Bukkit.getOfflinePlayers())
                            .map(OfflinePlayer::getName)
                            .toList();
                }
            }

            if (args.length == 3) {
                if (args[0].equalsIgnoreCase("channels")) {
                    if (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("join")) {
                        return YoChatAPI.getInstance().getChannelManager().getChannels().stream()
                                .map(ChatChannel::getName)
                                .filter(opt -> opt.startsWith(args[2]))
                                .toList();
                    } else if (args[1].equalsIgnoreCase("create")) {
                        return Collections.singletonList("name");
                    }
                } else if (args[0].equalsIgnoreCase("mute")) {
                    return Arrays.stream(Bukkit.getOfflinePlayers())
                            .map(OfflinePlayer::getName)
                            .toList();
                }
            }

            if (args.length == 4) {
                if (args[0].equalsIgnoreCase("channels")) {
                    if (args[1].equalsIgnoreCase("create")) {
                        return Collections.singletonList("permission");
                    }
                } else if (args[0].equalsIgnoreCase("mute")) {
                    if (args[1].equalsIgnoreCase("perm")) {
                        return Collections.singletonList("reason");
                    } else if (args[1].equalsIgnoreCase("temp")) {
                        return Collections.singletonList("time");
                    }
                } else if (args[0].equalsIgnoreCase("unmute")) {
                    return Collections.singletonList("reason");
                }
            }

            if (args.length == 5) {
                if (args[0].equalsIgnoreCase("channels")) {
                    if (args[1].equalsIgnoreCase("create")) {
                        return Collections.singletonList("radius");
                    }
                } else if (args[0].equalsIgnoreCase("mute")) {
                    if (args[1].equalsIgnoreCase("temp")) {
                        return Collections.singletonList("reason");
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private void sendNoPermissionMessage(CommandSender sender) {
        sender.sendMessage(Component.text("You don't have the permission to execute this command!", plugin.getMainColor()));
    }
}