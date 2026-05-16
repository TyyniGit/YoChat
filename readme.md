![YoChat full logo](https://cdn.modrinth.com/data/cached_images/2a9a9e0792ed0bb46bad0764cdb6713a433c4c45.png)

**YoChat** is a Paper chat plugin for formatted chat, channel-based messaging and built-in moderation. It supports MiniMessage, legacy `&` colors, mentions, custom player prefixes/suffixes and Discord webhook logging for mute actions.

---

## Features

* MiniMessage chat with legacy colors and hex support
* Channels with radius, world restrictions and per-channel formatting
* Temporary and permanent mutes with optional webhook logging
* Blocked words, mention highlighting and mention sounds
* Prefix/suffix priority support for LuckPerms, Vault and YoChat
* Optional PlaceholderAPI support
* Developer API for channels, mutes, formatting and pipeline hooks

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
4. Start the server at once.
5. Edit `config.yml` as needed.
6. Use `/yochat reload` after config changes.

---

## Commands

* `/yochat help`
* `/yochat reload`
* `/yochat mute <perm|temp> <player> <time if temp> [reason]`
* `/yochat unmute <player> [reason]`
* `/yochat channels ...`
* `/yochat player <player> <info|setprefix|removeprefix|setsuffix|removesuffix>`
* `/yochat debug <pipeline>`

---

## For Developers

YoChat includes an API for:

* channel lookup and management
* mute lookup and management
* message parsing and formatting
* custom chat pipeline steps and execution

JitPack dependency:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>me.tyyni</groupId>
    <artifactId>YoChat</artifactId>
    <version>1.2.1</version>
    <scope>provided</scope>
</dependency>
```
###### This project is made with the help of AI for the best performance.
