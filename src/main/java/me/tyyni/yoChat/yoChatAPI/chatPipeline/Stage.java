package me.tyyni.yoChat.yoChatAPI.chatPipeline;

/**
 * Defines the ordered stages of YoChat's message processing pipeline.
 */
public enum Stage {
    /** Validation and early cancellation, such as mute checks. */
    PRE,
    /** Raw message mutation, filtering and similar transformations. */
    PROCESS,
    /** Placeholder resolution for message content. */
    PLACEHOLDER,
    /** Channel selection and channel-related routing decisions. */
    CHANNEL,
    /** Viewer selection and recipient filtering. */
    VIEWERS,
    /** Final message formatting and component creation. */
    FORMAT,
    /** Post-processing effects such as mentions or sounds. */
    POST
}
