package com.interviewpilot.ai.service.chat;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import java.nio.charset.StandardCharsets;
import com.alibaba.fastjson2.JSONObject;
import com.interviewpilot.ai.api.io.resp.AiChatStreamRespDTO;
import com.interviewpilot.ai.api.io.resp.AiMessageHistoryRespDTO;
import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.ai.enums.AiPropritiesType;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.toolkit.xunfei.AIContentAccumulator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mimo (Anthropic 兼容) AI 聊天处理器
 * 使用 Anthropic Messages API 格式，支持流式输出和 thinking 模式
 */
@Slf4j
@Component
public class MimoChatHandler implements AiChatHandler {

    @Override
    public String getType() {
        return AiPropritiesType.MIMO.getType();
    }

    @Override
    public void streamToSink(AiPropertiesDO aiProperties, String userMessage,
                             List<AiMessageHistoryRespDTO> historyMessages,
                             FluxSink<String> sink, AIContentAccumulator accumulator) throws Exception {

        String baseUrl = aiProperties.getApiUrl();
        String apiKey = aiProperties.getApiKey();

        if (StrUtil.isBlank(baseUrl)) {
            baseUrl = AiPropritiesType.MIMO.getDefaultBaseUrl();
        }
        if (StrUtil.isBlank(apiKey)) {
            throw new ClientException("Mimo API Key 未配置");
        }

        JSONObject requestBody = buildRequestBody(aiProperties, userMessage, historyMessages, true);
        log.info("Mimo streamToSink: baseUrl={}, model={}", baseUrl, aiProperties.getModelName());

        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] streamError = new Throwable[1];

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        webClient.post()
                .uri("/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json; charset=UTF-8")
                .bodyValue(requestBody.toJSONString())
                .exchangeToFlux(response -> {
                    log.debug("Mimo response status: {}, contentType: {}", response.statusCode(), response.headers().contentType());
                    return response.bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                            .map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer);
                                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                            });
                })
                .subscribe(
                        chunk -> {
                            try {
                                log.debug("Mimo raw chunk: {}", chunk.length() > 200 ? chunk.substring(0, 200) : chunk);
                                processStreamChunk(chunk, sink, accumulator);
                            } catch (Exception e) {
                                log.error("Mimo 流式响应处理错误", e);
                                streamError[0] = e;
                                sink.error(e);
                                latch.countDown();
                            }
                        },
                        error -> {
                            log.error("Mimo 流式响应发生错误: {}", error.getMessage(), error);
                            streamError[0] = error;
                            sink.error(error);
                            latch.countDown();
                        },
                        () -> {
                            log.info("Mimo stream completed");
                            latch.countDown();
                        }
                );

        if (!latch.await(5, TimeUnit.MINUTES)) {
            throw new RuntimeException("Mimo AI 响应超时");
        }

        if (streamError[0] != null) {
            throw new RuntimeException(streamError[0]);
        }
    }

    /**
     * 同步调用 Mimo（非流式）
     */
    public String callSync(AiPropertiesDO aiProperties, String userMessage) {
        String baseUrl = aiProperties.getApiUrl();
        String apiKey = aiProperties.getApiKey();

        if (StrUtil.isBlank(baseUrl)) {
            baseUrl = AiPropritiesType.MIMO.getDefaultBaseUrl();
        }
        if (StrUtil.isBlank(apiKey)) {
            throw new ClientException("Mimo API Key 未配置");
        }

        JSONObject requestBody = buildRequestBody(aiProperties, userMessage, null, false);

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        String responseBody = webClient.post()
                .uri("/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json; charset=UTF-8")
                .bodyValue(requestBody.toJSONString())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(120))
                .block();

        if (responseBody == null) {
            return null;
        }

        JSONObject response = JSON.parseObject(responseBody);
        JSONArray content = response.getJSONArray("content");
        if (content == null || content.isEmpty()) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < content.size(); i++) {
            JSONObject block = content.getJSONObject(i);
            if ("text".equals(block.getString("type"))) {
                text.append(block.getString("text"));
            }
        }
        return text.isEmpty() ? null : text.toString();
    }

    private JSONObject buildRequestBody(AiPropertiesDO aiProperties, String userMessage,
                                         List<AiMessageHistoryRespDTO> historyMessages, boolean stream) {
        JSONObject body = new JSONObject();
        body.put("model", aiProperties.getModelName());
        body.put("stream", stream);

        if (aiProperties.getMaxTokens() != null) {
            body.put("max_tokens", aiProperties.getMaxTokens());
        } else {
            body.put("max_tokens", 8192);
        }

        if (aiProperties.getTemperature() != null) {
            body.put("temperature", aiProperties.getTemperature());
        }

        // System prompt
        if (StrUtil.isNotBlank(aiProperties.getSystemPrompt())) {
            body.put("system", aiProperties.getSystemPrompt());
        }

        // Messages
        JSONArray messages = new JSONArray();

        if (historyMessages != null) {
            for (AiMessageHistoryRespDTO history : historyMessages) {
                JSONObject msg = new JSONObject();
                msg.put("role", history.getMessageType() != null && history.getMessageType() == 1 ? "user" : "assistant");
                msg.put("content", history.getMessageContent());
                messages.add(msg);
            }
        }

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        body.put("messages", messages);

        // Enable thinking for mimo-v2.5-pro
        String modelName = aiProperties.getModelName();
        if (modelName != null && (modelName.contains("v2.5-pro") || modelName.contains("v2-pro"))) {
            JSONObject thinking = new JSONObject();
            thinking.put("type", "enabled");
            // thinking.budget_tokens is optional, let model decide
            body.put("thinking", thinking);
        }

        return body;
    }

    /**
     * 处理 Anthropic SSE 流式 chunk
     *
     * Anthropic SSE 格式:
     * event: message_start
     * data: {"type":"message_start","message":{...}}
     *
     * event: content_block_start
     * data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}
     *
     * event: content_block_delta
     * data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"..."}}
     *
     * event: content_block_delta (thinking)
     * data: {"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"..."}}
     *
     * event: content_block_stop
     * data: {"type":"content_block_stop","index":0}
     *
     * event: message_delta
     * data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{...}}
     *
     * event: message_stop
     * data: {"type":"message_stop"}
     */
    private void processStreamChunk(String chunk, FluxSink<String> sink, AIContentAccumulator accumulator) {
        if (chunk == null || chunk.isBlank()) {
            return;
        }

        // SSE format: data: {...}\n\n
        String[] lines = chunk.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("data: ")) {
                continue;
            }
            String jsonStr = line.substring(6).trim();
            if (jsonStr.isEmpty() || "[DONE]".equals(jsonStr)) {
                continue;
            }

            try {
                JSONObject event = JSON.parseObject(jsonStr);
                String type = event.getString("type");

                if ("content_block_delta".equals(type)) {
                    JSONObject delta = event.getJSONObject("delta");
                    if (delta == null) continue;

                    String deltaType = delta.getString("type");

                    if ("text_delta".equals(deltaType)) {
                        String text = delta.getString("text");
                        if (StrUtil.isNotEmpty(text)) {
                            AiChatStreamRespDTO resp = AiChatStreamRespDTO.builder()
                                    .type("content")
                                    .content(text)
                                    .build();
                            sink.next(JSON.toJSONString(resp));
                            accumulator.appendSimpleContent(text);
                        }
                    } else if ("thinking_delta".equals(deltaType)) {
                        String thinking = delta.getString("thinking");
                        if (StrUtil.isNotEmpty(thinking)) {
                            AiChatStreamRespDTO resp = AiChatStreamRespDTO.builder()
                                    .type("reasoning_content")
                                    .content(thinking)
                                    .build();
                            sink.next(JSON.toJSONString(resp));
                            accumulator.appendReasoningChunk(thinking.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Mimo SSE chunk 解析失败: {}", jsonStr, e);
            }
        }
    }
}
