package me.tyyni.yoChat.yoChatAPI.chatPipeline;

public interface ChatPipelineStep {
    void process(ChatContext context);

    default int getPriority() {
        return 0;
    }

    default boolean isAsyncSafe() {
        return false;
    }
}
