package com.interviewpilot.ai.service.chat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.time.Duration;
import java.util.Base64;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import com.interviewpilot.ai.api.io.resp.AiChatStreamRespDTO;
import com.interviewpilot.ai.api.io.resp.AiMessageHistoryRespDTO;
import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.ai.enums.AiPropritiesType;
import com.interviewpilot.common.config.mimo.MimoCredentialResolver;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.toolkit.ai.AIContentAccumulator;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 通用 AI 聊天处理器，基于 Spring AI 实现
 * 支持 OpenAI、Doubao、Spark 等兼容 OpenAI 接口的模型
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UniversalAiChatHandler implements AiChatHandler {

    private final MimoCredentialResolver credentialResolver;

    @Override
    public String getType() {
        return "universal";
    }

    public boolean supports(String type) {
        return AiPropritiesType.isSupported(type);
    }

    @Override
    public void streamToSink(AiPropertiesDO aiProperties, String userMessage, List<AiMessageHistoryRespDTO> historyMessages,
                             FluxSink<String> sink, AIContentAccumulator accumulator) throws Exception {
        ChatClient chatClient = createChatClient(aiProperties);
        List<Message> messages = buildMessages(aiProperties, userMessage, historyMessages);

        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] streamError = new Throwable[1];

        chatClient.prompt()
                .messages(messages)
                .stream()
                .chatResponse()
                .subscribe(
                        chatResponse -> {
                            try {
                                Generation generation = chatResponse.getResult();
                                if (generation != null) {
                                    String content = generation.getOutput().getText();
                                    if (StrUtil.isNotEmpty(content)) {
                                        AiChatStreamRespDTO resp = AiChatStreamRespDTO.builder()
                                                .type("content")
                                                .content(content)
                                                .build();
                                        sink.next(JSON.toJSONString(resp));
                                        accumulator.appendSimpleContent(content);
                                    }

                                    String reasoning = null;
                                    try {
                                        java.lang.reflect.Method getReasoningContent = generation.getOutput().getClass().getMethod("getReasoningContent");
                                        Object reasoningVal = getReasoningContent.invoke(generation.getOutput());
                                        if (reasoningVal != null) {
                                            reasoning = reasoningVal.toString();
                                        }
                                    } catch (Exception ignore) {
                                    }

                                    if (reasoning == null) {
                                        Object reasoningObj = generation.getOutput().getMetadata().get("reasoningContent");
                                        if (reasoningObj != null) {
                                            reasoning = reasoningObj.toString();
                                        }
                                    }

                                    if (StrUtil.isNotEmpty(reasoning)) {
                                        AiChatStreamRespDTO resp = AiChatStreamRespDTO.builder()
                                                .type("reasoning_content")
                                                .content(reasoning)
                                                .build();
                                        sink.next(JSON.toJSONString(resp));
                                        accumulator.appendReasoningChunk(reasoning.getBytes());
                                    }
                                }
                            } catch (Exception e) {
                                log.error("流式响应处理错误", e);
                                streamError[0] = e;
                                sink.error(e);
                                latch.countDown();
                            }
                        },
                        error -> {
                            log.error("流式响应发生错误", error);
                            streamError[0] = error;
                            sink.error(error);
                            latch.countDown();
                        },
                        latch::countDown
                );

        if (!latch.await(5, TimeUnit.MINUTES)) {
            throw new RuntimeException("AI 响应超时");
        }

        if (streamError[0] != null) {
            throw new RuntimeException(streamError[0]);
        }
    }

    private ChatClient createChatClient(AiPropertiesDO aiProperties) {
        String baseUrl = aiProperties.getApiUrl();
        String apiKey = credentialResolver.resolveSecret(aiProperties.getApiKey());

        if (StrUtil.isBlank(baseUrl)) {
            baseUrl = AiPropritiesType.getByType(aiProperties.getAiType()).getDefaultBaseUrl();
        }
        if (StrUtil.isBlank(apiKey)) {
            throw new ClientException("AI API Key 未配置");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(30).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofMinutes(3).toMillis());

        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> {
                    if (StrUtil.isNotBlank(aiProperties.getProjectId())) {
                        headers.add("OpenAI-Project", aiProperties.getProjectId());
                    }
                    if (StrUtil.isNotBlank(aiProperties.getOrganizationId())) {
                        headers.add("OpenAI-Organization", aiProperties.getOrganizationId());
                    }
                });

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(3));
        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        OpenAiApi openAiApi = new OpenAiApi(
                baseUrl,
                new SimpleApiKey(apiKey),
                new LinkedMultiValueMap<>(),
                "/chat/completions",
                "/embeddings",
                restClientBuilder,
                webClientBuilder,
                new DefaultResponseErrorHandler()
        );

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(aiProperties.getModelName());

        if (aiProperties.getMaxTokens() != null) {
            optionsBuilder.maxTokens(aiProperties.getMaxTokens());
        }

        OpenAiChatOptions options = optionsBuilder.build();
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        OpenAiChatModel chatModel = new OpenAiChatModel(
                openAiApi,
                options,
                toolCallingManager,
                RetryTemplate.defaultInstance(),
                ObservationRegistry.NOOP
        );

        return ChatClient.builder(chatModel)
                .defaultOptions(options)
                .build();
    }

    private List<Message> buildMessages(AiPropertiesDO aiProperties, String userMessage, List<AiMessageHistoryRespDTO> historyMessages) {
        List<Message> messages = new ArrayList<>();

        if (StrUtil.isNotBlank(aiProperties.getSystemPrompt())) {
            messages.add(new SystemMessage(aiProperties.getSystemPrompt()));
        }

        if (CollUtil.isNotEmpty(historyMessages)) {
            for (AiMessageHistoryRespDTO history : historyMessages) {
                String content = history.getMessageContent();
                if (history.getMessageType() != null && history.getMessageType() == 1) {
                    messages.add(new UserMessage(content));
                } else {
                    messages.add(new AssistantMessage(content));
                }
            }
        }

        messages.add(new UserMessage(userMessage));
        return messages;
    }

    /**
     * 同步调用 AI（非流式）
     *
     * @param aiProperties AI 配置
     * @param userMessage  用户消息
     * @return AI 响应文本
     */
    public String callSync(AiPropertiesDO aiProperties, String userMessage) {
        log.info("[UniversalAiChatHandler] callSync start, url={}, model={}, inputPreview={}",
                aiProperties.getApiUrl(), aiProperties.getModelName(),
                userMessage.length() > 100 ? userMessage.substring(0, 100) : userMessage);
        try {
            ChatClient chatClient = createChatClient(aiProperties);
            List<Message> messages = new ArrayList<>();

            if (StrUtil.isNotBlank(aiProperties.getSystemPrompt())) {
                messages.add(new SystemMessage(aiProperties.getSystemPrompt()));
            }
            messages.add(new UserMessage(userMessage));

            var chatResponse = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .chatResponse();

            if (chatResponse != null && chatResponse.getResult() != null) {
                String result = extractGenerationText(chatResponse.getResult());
                log.info("[UniversalAiChatHandler] callSync success, resultPreview={}",
                        result != null && result.length() > 100 ? result.substring(0, 100) : result);
                return result;
            }
            return null;
        } catch (Exception e) {
            log.error("[UniversalAiChatHandler] callSync failed, url={}, model={}, error={}",
                    aiProperties.getApiUrl(), aiProperties.getModelName(), e.getMessage(), e);
            throw e;
        }
    }

    public String callSyncWithImage(
            AiPropertiesDO aiProperties,
            String userMessage,
            byte[] imageBytes,
            String imageMimeType) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new ClientException("image payload is required for vision chat");
        }

        String baseUrl = resolveBaseUrl(aiProperties);
        String apiKey = credentialResolver.resolveSecret(aiProperties.getApiKey());
        if (StrUtil.isBlank(apiKey)) {
            throw new ClientException("AI API Key 未配置");
        }

        String requestUrl = trimTrailingSlash(baseUrl) + "/chat/completions";
        JSONObject request = new JSONObject();
        request.put("model", aiProperties.getModelName());
        if (aiProperties.getMaxTokens() != null) {
            request.put("max_tokens", aiProperties.getMaxTokens());
        }

        JSONArray messages = new JSONArray();
        if (StrUtil.isNotBlank(aiProperties.getSystemPrompt())) {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", aiProperties.getSystemPrompt());
            messages.add(systemMessage);
        }

        JSONObject userMessageJson = new JSONObject();
        userMessageJson.put("role", "user");
        JSONArray content = new JSONArray();

        JSONObject textPart = new JSONObject();
        textPart.put("type", "text");
        textPart.put("text", userMessage);
        content.add(textPart);

        JSONObject imageUrl = new JSONObject();
        imageUrl.put(
                "url",
                "data:" + normalizeImageMimeType(imageMimeType) + ";base64,"
                        + Base64.getEncoder().encodeToString(imageBytes)
        );

        JSONObject imagePart = new JSONObject();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrl);
        content.add(imagePart);

        userMessageJson.put("content", content);
        messages.add(userMessageJson);
        request.put("messages", messages);

        log.info("[UniversalAiChatHandler] vision callSync start, url={}, model={}, imageBytes={}, inputPreview={}",
                requestUrl,
                aiProperties.getModelName(),
                imageBytes.length,
                userMessage != null && userMessage.length() > 100 ? userMessage.substring(0, 100) : userMessage);

        try {
            String responseBody = RestClient.builder()
                    .requestFactory(restRequestFactory())
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build()
                    .post()
                    .uri(requestUrl)
                    .body(request.toJSONString())
                    .retrieve()
                    .body(String.class);

            String result = extractChatContent(StrUtil.isBlank(responseBody) ? null : JSON.parseObject(responseBody));
            log.info("[UniversalAiChatHandler] vision callSync success, resultPreview={}",
                    result != null && result.length() > 100 ? result.substring(0, 100) : result);
            return result;
        } catch (RestClientException e) {
            log.error("[UniversalAiChatHandler] vision callSync failed, url={}, model={}, error={}",
                    requestUrl, aiProperties.getModelName(), e.getMessage(), e);
            throw e;
        }
    }

    private String resolveBaseUrl(AiPropertiesDO aiProperties) {
        String baseUrl = aiProperties.getApiUrl();
        if (StrUtil.isBlank(baseUrl)) {
            baseUrl = AiPropritiesType.getByType(aiProperties.getAiType()).getDefaultBaseUrl();
        }
        return baseUrl;
    }

    private SimpleClientHttpRequestFactory restRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(30).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofMinutes(3).toMillis());
        return requestFactory;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String normalizeImageMimeType(String imageMimeType) {
        return StrUtil.blankToDefault(imageMimeType, "image/jpeg");
    }

    private String extractChatContent(JSONObject response) {
        if (response == null) {
            return null;
        }
        JSONArray choices = response.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        JSONObject firstChoice = choices.getJSONObject(0);
        if (firstChoice == null) {
            return null;
        }
        JSONObject message = firstChoice.getJSONObject("message");
        return extractMessageText(message);
    }

    private String extractGenerationText(Generation generation) {
        if (generation == null || generation.getOutput() == null) {
            return null;
        }
        AssistantMessage output = generation.getOutput();
        String text = output.getText();
        if (StrUtil.isNotBlank(text)) {
            return text;
        }
        try {
            java.lang.reflect.Method getReasoningContent = output.getClass().getMethod("getReasoningContent");
            Object reasoningVal = getReasoningContent.invoke(output);
            if (reasoningVal != null && StrUtil.isNotBlank(reasoningVal.toString())) {
                return reasoningVal.toString();
            }
        } catch (Exception ignored) {
        }
        Object reasoningObj = output.getMetadata().get("reasoningContent");
        if (reasoningObj == null) {
            reasoningObj = output.getMetadata().get("reasoning_content");
        }
        return reasoningObj == null ? null : reasoningObj.toString();
    }

    private String extractMessageText(JSONObject message) {
        if (message == null) {
            return null;
        }
        String content = message.getString("content");
        if (StrUtil.isNotBlank(content)) {
            return content;
        }
        return message.getString("reasoning_content");
    }
}
