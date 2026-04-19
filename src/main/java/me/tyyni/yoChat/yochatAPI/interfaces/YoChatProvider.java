package me.tyyni.yoChat.yochatAPI.interfaces;

import me.tyyni.yoChat.yoChatPlugin.*;

public interface YoChatProvider {

    YoChat getYoChat();
    PrefixManager getPrefixManager();

    ChatManager getChatManager();

    ChannelManager getChannelManager();

    SuffixManager getSuffixManager();

    MuteManager getMuteManager();
}