package com.interviewpilot.ai.service.chat;

import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.common.config.mimo.MimoCredentialResolver;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniversalAiChatHandlerTest {

    @Test
    void callSyncWithImage_ShouldSendOpenAiCompatibleVisionPayload() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] body = """
                    {"choices":[{"message":{"content":"{\\"panicLevel\\":10,\\"seriousnessLevel\\":90,\\"emoticonHandling\\":85,\\"compositeScore\\":88}"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            AiPropertiesDO properties = new AiPropertiesDO();
            properties.setApiUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.setApiKey("test-key");
            properties.setModelName("mimo-v2.5");
            properties.setMaxTokens(128);

            UniversalAiChatHandler handler = new UniversalAiChatHandler(
                    new MimoCredentialResolver(new MockEnvironment())
            );

            String response = handler.callSyncWithImage(
                    properties,
                    "score this interview frame",
                    "fake-image".getBytes(StandardCharsets.UTF_8),
                    "image/png"
            );

            assertTrue(response.contains("\"compositeScore\":88"));
            assertEquals("Bearer test-key", authorization.get());
            assertTrue(requestBody.get().contains("\"model\":\"mimo-v2.5\""));
            assertTrue(requestBody.get().contains("\"type\":\"text\""));
            assertTrue(requestBody.get().contains("\"type\":\"image_url\""));
            assertTrue(requestBody.get().contains("data:image/png;base64,ZmFrZS1pbWFnZQ=="));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void callSyncWithImage_ShouldFallbackToReasoningContentWhenContentIsEmpty() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] body = """
                    {"choices":[{"message":{"content":"","reasoning_content":"{\\"panicLevel\\":20,\\"seriousnessLevel\\":80,\\"emoticonHandling\\":75,\\"compositeScore\\":82}"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            AiPropertiesDO properties = new AiPropertiesDO();
            properties.setApiUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.setApiKey("test-key");
            properties.setModelName("mimo-v2.5");
            properties.setMaxTokens(128);

            UniversalAiChatHandler handler = new UniversalAiChatHandler(
                    new MimoCredentialResolver(new MockEnvironment())
            );

            String response = handler.callSyncWithImage(
                    properties,
                    "score this interview frame",
                    "fake-image".getBytes(StandardCharsets.UTF_8),
                    "image/png"
            );

            assertTrue(response.contains("\"compositeScore\":82"));
        } finally {
            server.stop(0);
        }
    }
}
