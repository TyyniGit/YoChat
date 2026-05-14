package me.tyyni.yoChat.yoChatAPI.interfaces;

import me.tyyni.yoChat.yoChatPlugin.*;

/**
 * Internal provider contract used to expose YoChat runtime services through the public API.
 */
public interface YoChatProvider {

    /**
     * Returns the active YoChat plugin instance.
     *
     * @return the plugin instance
     */
    YoChat getYoChat();

    /**
     * Returns the chat manager implementation.
     *
     * @return the chat manager
     */
    ChatManager getChatManager();

    /**
     * Returns the channel manager implementation.
     *
     * @return the channel manager
     */
    ChannelManager getChannelManager();

    /**
     * Returns the mute manager implementation.
     *
     * @return the mute manager
     */
    MuteManager getMuteManager();

    /**
     * Returns the message parsing manager implementation.
     *
     * @return the message parse manager
     */
    MessageParseManager getMessageParseManager();

    /**
     * Returns the chat pipeline manager implementation.
     *
     * @return the chat pipeline manager
     */
    ChatPipelineManager getChatPipelineManager();

    /**
     * Returns the suffix manager implementation.
     *
     * @return the suffix manager
     */
    SuffixManager getSuffixManager();

    /**
     * Returns the prefix manager implementation.
     *
     * @return the prefix manager
     */
    PrefixManager getPrefixManager();
}
