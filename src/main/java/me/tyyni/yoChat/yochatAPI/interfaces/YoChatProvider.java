package me.tyyni.yoChat.yochatAPI.interfaces;

import me.tyyni.yoChat.yoChatPlugin.*;
import me.tyyni.yoChat.yochatAPI.YoChatAPI;

public interface YoChatProvider {

    YoChat getYoChatAPI();
    PrefixManager getPrefixManager();

    ChatManager getChatManager();

    ChannelManager getChannelManager();

    SuffixManager getSuffixManager();
}