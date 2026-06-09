package com.interviewpilot.ai.service.chat;

import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.common.config.mimo.MimoCredentialResolver;
import com.interviewpilot.toolkit.ai.AIContentAccumulator;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import reactor.core.Disposable;
import reactor.core.publisher.FluxSink;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.LongConsumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicChatHandlerTest {

    @Test
    void streamToSink_ShouldFailWhenUpstreamReturnsUnauthorized() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/messages", exchange -> {
            byte[] body = "{\"error\":{\"message\":\"unauthorized\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            AiPropertiesDO properties = new AiPropertiesDO();
            properties.setApiUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.setApiKey("bad-key");
            properties.setModelName("mimo-v2.5-pro");
            properties.setMaxTokens(128);

            AnthropicChatHandler handler = new AnthropicChatHandler(
                    new MimoCredentialResolver(new MockEnvironment())
            );

            RuntimeException error = assertThrows(
                    RuntimeException.class,
                    () -> handler.streamToSink(
                            properties,
                            "hello",
                            List.of(),
                            new RecordingFluxSink(),
                            new AIContentAccumulator()
                    )
            );
            assertTrue(error.toString().contains("401")
                    || (error.getCause() != null && error.getCause().toString().contains("401")));
        } finally {
            server.stop(0);
        }
    }

    private static class RecordingFluxSink implements FluxSink<String> {

        private volatile boolean cancelled;

        @Override
        public FluxSink<String> next(String value) {
            return this;
        }

        @Override
        public void complete() {
            cancelled = true;
        }

        @Override
        public void error(Throwable throwable) {
            cancelled = true;
        }

        @Override
        public Context currentContext() {
            return Context.empty();
        }

        @Override
        public ContextView contextView() {
            return Context.empty();
        }

        @Override
        public long requestedFromDownstream() {
            return Long.MAX_VALUE;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public FluxSink<String> onRequest(LongConsumer consumer) {
            return this;
        }

        @Override
        public FluxSink<String> onCancel(Disposable disposable) {
            return this;
        }

        @Override
        public FluxSink<String> onDispose(Disposable disposable) {
            return this;
        }
    }
}
