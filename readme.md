![YoChat full logo](https://cdn.modrinth.com/data/cached_images/2a9a9e0792ed0bb46bad0764cdb6713a433c4c45.png)

**YoChat** is a modern, high-performance chat management plugin designed for Minecraft servers that demand flexibility and style. Powered by MiniMessage, it offers beautiful gradients, a robust channel system and built-in moderation tools. It also features full legacy color code support.

---

## Features

* **Modern Formatting:** Full support for Adventure MiniMessage (Gradients, Hover, Click events).
* **Channel System:** Create separate chat environments (e.g., Global, Staff, Local).
* **Integrated Moderation:** Built-in mute system and word blacklisting.
* **Smart Mentions:** Ping players in chat with custom sounds and visual highlights.
* **Discord Integration:** Log mutes and unmutes directly to your Discord staff channels via Webhooks.
* **Extensive Compatibility:** Native support for LuckPerms, Vault and PlaceholderAPI.

---

## Installation

1.  Place the `YoChat.jar` into your server's `plugins` folder.
2.  Ensure you have **LuckPerms** or **Vault** installed (required for prefixes/suffixes).
3.  Restart your server to generate the configuration.
4.  Configure your chat formats, moderation settings and more in `config.yml`.

---

## Commands & Permissions
You can find all the permissions in LuckPerms. Here is only a portion of the permissions.

| Command                                                     | Description                     | Permission                 |
|:------------------------------------------------------------|:--------------------------------|:---------------------------|
| `/yochat help`                                              | Lists all available commands.   | `yochat.commands.help`     |
| `/yochat reload`                                            | Reloads the configuration file. | `yochat.commands.reload`   |
| `/yochat mute <perm/temp> <player> <time if temp> [reason]` | Mutes a player.                 | `yochat.commands.mute`     |
| `/yochat unmute <player> [reason]`                          | Unmutes a player.               | `yochat.commands.unmute`   |
| `/yochat channels`                                          | Channel management commands.    | `yochat.commands.channels` |

---

## Configuration

YoChat uses a highly customizable `config.yml`. You can customize basically everything from there. Always remember to use `/yochat reload` after changing the config, otherwise the changes won't take effect.

---

## Requirements
* **Java 17** or higher.
* **Paper** (1.21.x recommended for the best compatibility).
* **LuckPerms** or **Vault** for prefix/suffix support.
* **PlaceholderAPI** (Optional) for additional variables.

---

## For Developers (API)
You can hook into YoChat using the YoChat API:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>me.tyyni</groupId>
    <artifactId>YoChat</artifactId>
    <version>{latest_version}</version>
    <scope>provided</scope>
</dependency>
```

```java
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.objects.ChatChannel;
import net.kyori.adventure.text.Component;

List<ChatChannel> channels = YoChatAPI.getChannels();
```
###### This project is developed with the **assistance** of AI to ensure code quality and efficiency.
