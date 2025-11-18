package com.example;

import java.io.IOException;
import java.util.function.Consumer;

public interface ChatNetworkClient {
    void send(String baseUrl, NtfyMessage message) throws IOException;
    com.example.Subscription subscribe(String baseUrl, String topic, Consumer<NtfyMessage> messageHandler);

}
