package me.tyyni.yoChat.yoChatPlugin.webhook;

import lombok.*;

import java.util.List;

/**
 * Webhook system by SMCode (<a href="https://www.youtube.com/watch?v=vvDrp5jBsYQ">Link to the video</a>)
 */
@Builder
public class WebhookPayload {
    private String content;
    private String username;
    private List<Embed> embeds;

    @Builder
    public static class Embed {
        private String title;
        private String description;
        private List<Field> fields;
        private Author author;
        private int color;
    }

    @Builder
    public static class Field {
        private String name;
        private String value;
    }
    @AllArgsConstructor(staticName = "of")
    @Builder
    public static class Author {
        private String name;
    }
}
