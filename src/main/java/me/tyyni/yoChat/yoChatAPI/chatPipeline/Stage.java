package me.tyyni.yoChat.yoChatAPI.chatPipeline;

public enum Stage {
    PRE,          // mute, cancel, validation
    PROCESS,      // filter, caps, edit message
    PLACEHOLDER,  // PlaceholderAPI
    CHANNEL,      // determine channel
    VIEWERS,      // determine recipients
    FORMAT,       // prefix/suffix, MiniMessage
    POST          // mentions, sounds, extra effects
}
