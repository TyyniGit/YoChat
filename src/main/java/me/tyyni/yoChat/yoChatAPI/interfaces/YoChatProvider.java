package me.tyyni.yoChat.yoChatAPI.interfaces;

import me.tyyni.yoChat.yoChatPlugin.*;

public interface YoChatProvider {

    YoChat getYoChat();

    ChatManager getChatManager();

    ChannelManager getChannelManager();

    MuteManager getMuteManager();

    MessageParseManager getMessageParseManager();

    // Suffix system might be implemented in the future.
    // SuffixManager getSuffixManager();

    // Prefix system might be implemented in the future.
    // PrefixManager getPrefixManager();
}