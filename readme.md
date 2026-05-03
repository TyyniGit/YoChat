![YoChat full logo](https://cdn.modrinth.com/data/cached_images/2a9a9e0792ed0bb46bad0764cdb6713a433c4c45.png)

**YoChat** is a Paper chat plugin focused on clean formatting, flexible channels and built-in moderation. It supports MiniMessage, legacy color codes, mentions and Discord webhook logging for mute actions.

---

## Features

* MiniMessage formatting with legacy `&` colors and hex color support
* Configurable chat channels with radius and world restrictions
* Temporary and permanent mutes
* Blocked words and mention highlighting
* Prefix and suffix support through LuckPerms or Vault
* Optional PlaceholderAPI support
* Small developer API for channels, mutes and chat pipeline hooks

---

## Requirements

* Java 21
* Paper 1.21.x
* LuckPerms or Vault
* PlaceholderAPI optional

---

## Installation

1. Put `YoChat.jar` into the `plugins` folder.
2. Make sure Java 21 is in use.
3. Install LuckPerms or Vault.
4. Start the server once.
5. Edit `config.yml` and `channels.yml` as needed.
6. Use `/yochat reload` after config changes.

---

## Commands

* `/yochat help`
* `/yochat reload`
* `/yochat mute <perm|temp> <player> <time if temp> [reason]`
* `/yochat unmute <player> [reason]`
* `/yochat channels`

Permissions are available in `plugin.yml`.

---

## For Developers

YoChat includes a small API with support for:

* channel lookup and management
* mute lookup and management
* message formatting and parsing
* custom chat pipeline steps

JitPack dependency:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>me.tyyni</groupId>
    <artifactId>YoChat</artifactId>
    <version>1.1.0</version>
    <scope>provided</scope>
</dependency>
```
