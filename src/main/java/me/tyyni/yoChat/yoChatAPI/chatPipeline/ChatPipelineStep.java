package me.tyyni.yoChat.yoChatAPI.chatPipeline;

/**
 * Represents a single processing step inside YoChat's chat pipeline.
 *
 * <p>Implementations may inspect or mutate the provided {@link ChatContext},
 * cancel processing, or contribute additional formatting and delivery logic.</p>
 */
public interface ChatPipelineStep {
    /**
     * Processes the given chat context.
     *
     * @param context the mutable chat context for the current message
     */
    void process(ChatContext context);

    /**
     * Returns the default registration priority for this step.
     *
     * <p>Lower values run earlier inside the same {@link Stage}.</p>
     *
     * @return the default priority value
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Indicates whether this step may safely run off the main server thread.
     *
     * @return {@code true} if the step is async-safe, otherwise {@code false}
     */
    default boolean isAsyncSafe() {
        return false;
    }
}
