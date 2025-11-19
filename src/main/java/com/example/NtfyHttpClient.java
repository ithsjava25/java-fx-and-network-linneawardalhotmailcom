package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class NtfyHttpClient implements ChatNetworkClient {
    private final HttpClient http;
    private final ObjectMapper mapper;

    public NtfyHttpClient() {
        this.http = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    @Override
    public void send(String baseUrl, NtfyMessage message) {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(message.message()))
                .header("Content-Type", "text/plain")
                .uri(URI.create(baseUrl + "/" + message.topic()))
                .build();

        System.out.println("Skickar till: " + baseUrl + "/" + message.topic());
        System.out.println("Meddelande: " + message.message());

        http.sendAsync(
                        request,
                        HttpResponse.BodyHandlers.discarding()
                ).thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    if (statusCode >= 400) {
                        Platform.runLater(() ->
                                System.err.println("Fel vid sändning: HTTP " + statusCode)
                        );
                    } else {
                        System.out.println("Meddelandet skickat! Statuskod: " + statusCode);
                    }
                })
                .exceptionally(error -> {
                    Platform.runLater(() ->
                            System.err.println("Nätverksfel: " + error.getMessage())
                    );
                    return null;
                });
    }

    @Override
    public com.example.Subscription subscribe(String baseUrl, String topic, Consumer<NtfyMessage> messageHandler) {
        AtomicBoolean isOpen = new AtomicBoolean(true);
        CompletableFuture<?> subscriptionFuture = http.sendAsync(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(baseUrl + "/" + topic + "/json"))
                        .build(),
                HttpResponse.BodyHandlers.ofLines()
        ).thenAccept(response -> {
            System.out.println("Prenumeration startad för topic: " + topic);
            response.body()
                    .peek(line -> System.out.println("Mottagen rad från servern: " + line))
                    .takeWhile(line -> isOpen.get())
                    .forEach(line -> {
                        try {
                            NtfyMessage message = mapper.readValue(line, NtfyMessage.class);
                            System.out.println("Parsat meddelande: " + message);
                            if ("message".equals(message.event())) {
                                Platform.runLater(() -> messageHandler.accept(message));
                            }
                        } catch (IOException e) {
                            System.err.println("Kunde inte tolka rad som JSON: " + line + ". Fel: " + e.getMessage());
                        }
                    });
        }).exceptionally(error -> {
            System.err.println("Fel vid prenumeration: " + error.getMessage());
            return null;
        });

        return new com.example.Subscription() {
            @Override
            public void close() throws IOException {
                isOpen.set(false);
                subscriptionFuture.cancel(true);
            }

            @Override
            public boolean isOpen() {
                return isOpen.get();
            }
        };
    }

}
