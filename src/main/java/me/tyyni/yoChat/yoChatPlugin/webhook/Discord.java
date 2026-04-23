package me.tyyni.yoChat.yoChatPlugin.webhook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import me.tyyni.yoChat.yoChatPlugin.ConfigManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Webhook system by SMCode (<a href="https://www.youtube.com/watch?v=vvDrp5jBsYQ">Link to the video</a>)
 */

@Slf4j
public class Discord {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void sendMessage(WebhookPayload payload, String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Webhook URL is null or empty. Message not sent.");
            return;
        }

        try (final HttpClient client = HttpClient.newHttpClient()) {
            final String json = gson.toJson(payload);
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

            future.thenAccept(response -> {
                if (ConfigManager.getInstance().isSendResponseCode()) {
                    log.info("[YoChat Discord Webhook] Response code: {}", response.statusCode());
                }

                if (response.statusCode() != 204) {
                    if (ConfigManager.getInstance().isSendResponseBody()) {
                        log.info("[YoChat Discord Webhook] Response body: {}", response.body());
                    }
                }

            }).exceptionally(ex -> {
                log.warn("Exeption: {}", String.valueOf(ex));
                return null;
            });
        }
    }
}
