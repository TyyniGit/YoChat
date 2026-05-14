package me.tyyni.yoChat.yoChatAPI.chatPipeline;

/**
 * Defines the ordered stages of YoChat's message processing pipeline.
 */
public enum Stage {
    /** Validation and early cancellation, such as mute checks. */
    PRE,          // mute, cancel, validation
    /** Raw message mutation, filtering and similar transformations. */
    PROCESS,      // filter, caps, edit message
    /** Placeholder resolution for message content. */
    PLACEHOLDER,  // PlaceholderAPI
    /** Channel selection and channel-related routing decisions. */
    CHANNEL,      // determine channel
    /** Viewer selection and recipient filtering. */
    VIEWERS,      // determine recipients
    /** Final message formatting and component creation. */
    FORMAT,       // prefix/suffix, MiniMessage
    /** Post-processing effects such as mentions or sounds. */
    POST          // mentions, sounds, extra effects
}
