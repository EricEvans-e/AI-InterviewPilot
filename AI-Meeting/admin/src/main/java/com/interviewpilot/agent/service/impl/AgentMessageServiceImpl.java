package com.interviewpilot.agent.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interviewpilot.agent.api.io.resp.AgentMessageHistoryRespDTO;
import com.interviewpilot.agent.application.AgentResolver;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.agent.service.AgentConversationService;
import com.interviewpilot.agent.service.AgentMessageService;
import com.interviewpilot.ai.api.io.resp.AiMessageHistoryRespDTO;
import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.ai.service.chat.AnthropicChatHandler;
import com.interviewpilot.ai.service.chat.UniversalAiChatHandler;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.common.enums.AgentErrorCodeEnum;
import com.interviewpilot.conversation.application.ConversationMessageHistoryService;
import com.interviewpilot.conversation.application.ConversationMessagePersistenceService;
import com.interviewpilot.conversation.application.ConversationOwnershipService;
import com.interviewpilot.conversation.application.ConversationStreamingSupport;
import com.interviewpilot.toolkit.ai.AIContentAccumulator;
import com.interviewpilot.toolkit.ai.AgentPropertiesLoader;
import com.interviewpilot.toolkit.iflytek.XunfeiWorkflowClient;
import com.interviewpilot.user.api.io.req.UserMessageReqDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.FluxSink;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentMessageServiceImpl implements AgentMessageService {

    private static final String DEFAULT_ERROR_CONTENT = "Sorry, an error occurred while processing your request.";
    private static final String PROVIDER_ANTHROPIC = "anthropic";
    private static final String PROVIDER_XINGCHEN = "xingchen";
    private static final String PROVIDER_OPENAI = "openai";

    private final AgentResolver agentResolver;
    private final ObjectProvider<XunfeiWorkflowClient> xunfeiWorkflowClientProvider;
    private final UniversalAiChatHandler universalAiChatHandler;
    private final AnthropicChatHandler anthropicChatHandler;
    private final AgentPropertiesLoader agentPropertiesLoader;
    private final AgentConversationService agentConversationService;
    private final ConversationOwnershipService conversationOwnershipService;
    private final ConversationMessageHistoryService conversationMessageHistoryService;
    private final ConversationMessagePersistenceService conversationMessagePersistenceService;
    private final ConversationStreamingSupport conversationStreamingSupport;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public List<AgentMessageHistoryRespDTO> getConversationHistory(String sessionId) {
        return conversationMessageHistoryService.listAgentHistory(sessionId);
    }

    @Override
    public List<AgentMessageHistoryRespDTO> getConversationHistory(String sessionId, Long userId) {
        conversationOwnershipService.requireOwnedConversation(sessionId, userId);
        return getConversationHistory(sessionId);
    }

    @Override
    public IPage<AgentMessageHistoryRespDTO> pageHistoryMessages(String sessionId, Integer current, Integer size) {
        if (StrUtil.isNotBlank(sessionId)) {
            return conversationMessageHistoryService.pageAgentHistory(sessionId, current, size);
        }
        return conversationMessageHistoryService.pageAllAgentHistory(current, size);
    }

    @Override
    public IPage<AgentMessageHistoryRespDTO> pageHistoryMessages(
            String sessionId,
            Integer current,
            Integer size,
            Long userId) {
        if (StrUtil.isNotBlank(sessionId)) {
            conversationOwnershipService.requireOwnedConversation(sessionId, userId);
            return pageHistoryMessages(sessionId, current, size);
        }

        List<String> sessionIds = conversationOwnershipService.listOwnedSessionIds(userId);
        if (CollUtil.isEmpty(sessionIds)) {
            Page<AgentMessageHistoryRespDTO> emptyPage = new Page<>(current, size);
            emptyPage.setTotal(0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }
        return conversationMessageHistoryService.pageAgentHistory(sessionIds, current, size);
    }

    @Override
    public SseEmitter agentChatSse(UserMessageReqDTO requestParam) {
        String sessionId = requestParam.getSessionId();
        if (StrUtil.isBlank(sessionId)) {
            throw new ClientException("sessionId cannot be empty");
        }

        SseEmitter emitter = new SseEmitter(18000L);
        Long agentId = agentResolver.resolveAgentId(sessionId, null);
        if (agentId == null) {
            throw new ClientException(AgentErrorCodeEnum.Agent_NULL);
        }
        String userMessage = requestParam.getInputMessage() == null ? "No input" : requestParam.getInputMessage();
        AIContentAccumulator accumulator = new AIContentAccumulator();

        threadPoolTaskExecutor.submit(() -> processChat(sessionId, agentId, userMessage, emitter, accumulator));
        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    @Override
    public SseEmitter agentChatSse(UserMessageReqDTO requestParam, Long userId) {
        String sessionId = requestParam != null ? requestParam.getSessionId() : null;
        conversationOwnershipService.requireOwnedConversation(sessionId, userId);
        return agentChatSse(requestParam);
    }

    private void processChat(
            String sessionId,
            Long agentId,
            String userMessage,
            SseEmitter emitter,
            AIContentAccumulator accumulator) {
        conversationStreamingSupport.execute(ConversationStreamingSupport.ConversationStreamRequest
                .<AgentMessageHistoryRespDTO>builder()
                .sessionId(sessionId)
                .defaultErrorContent(DEFAULT_ERROR_CONTENT)
                .accumulator(accumulator)
                .historySupplier(() -> conversationMessageHistoryService.listAgentHistory(sessionId))
                .userMessageSaver(() -> conversationMessagePersistenceService.saveAgentUserMessage(sessionId, userMessage))
                .streamExecutor((historyMessages, contentAccumulator) -> {
                    AgentPropertiesDO agentProperties = agentPropertiesLoader.getByAgentId(agentId);
                    if (agentProperties == null) {
                        throw new ClientException(AgentErrorCodeEnum.Agent_NULL);
                    }
                    if (!PROVIDER_XINGCHEN.equalsIgnoreCase(StrUtil.trimToEmpty(agentProperties.getAiProvider()))) {
                        streamMimoAgentChat(agentProperties, userMessage, historyMessages, emitter, contentAccumulator);
                        return;
                    }
                    legacyXunfeiWorkflowClient().chat(
                            userMessage,
                            sessionId,
                            buildHistoryJson(historyMessages),
                            true,
                            new OutputStream() {
                                @Override
                                public void write(int b) {
                                }

                                @Override
                                public void write(byte[] b, int off, int len) throws IOException {
                                    String jsonChunk = new String(b, off, len);
                                    emitter.send(SseEmitter.event().data(jsonChunk));
                                    contentAccumulator.appendChunk(b);
                                }

                                @Override
                                public void flush() {
                                }
                            },
                            data -> {
                            },
                            agentProperties.getApiKey(),
                            agentProperties.getApiSecret(),
                            agentProperties.getApiFlowId()
                    );
                })
                .assistantMessageSaver(payload -> conversationMessagePersistenceService.saveAgentAssistantMessage(
                        sessionId,
                        payload.content(),
                        payload.responseTime(),
                        payload.errorMessage()))
                .conversationUpdater(messageSeq -> agentConversationService.updateConversation(sessionId, messageSeq, null))
                .successHandler(() -> {
                    try {
                        emitter.send(SseEmitter.event().name("end").data("[DONE]"));
                        emitter.complete();
                    } catch (IOException ex) {
                        log.warn("Failed to send agent stream completion event, sessionId={}", sessionId, ex);
                        emitter.completeWithError(ex);
                    }
                })
                .errorHandler(emitter::completeWithError)
                .build());
    }

    private XunfeiWorkflowClient legacyXunfeiWorkflowClient() {
        XunfeiWorkflowClient client = xunfeiWorkflowClientProvider.getIfAvailable();
        if (client == null) {
            throw new ClientException("legacy xingchen provider requires LEGACY_XUNFEI_ENABLED=true",
                    AgentErrorCodeEnum.Agent_NULL);
        }
        return client;
    }

    private void streamMimoAgentChat(
            AgentPropertiesDO agentProperties,
            String userMessage,
            List<AgentMessageHistoryRespDTO> historyMessages,
            SseEmitter emitter,
            AIContentAccumulator accumulator) throws Exception {
        String provider = StrUtil.blankToDefault(agentProperties.getAiProvider(), PROVIDER_OPENAI);
        AiPropertiesDO aiProperties = toAiProperties(agentProperties, provider);
        FluxSink<String> sink = new SseEmitterFluxSink(emitter);

        if (PROVIDER_ANTHROPIC.equalsIgnoreCase(provider)) {
            anthropicChatHandler.streamToSink(
                    aiProperties,
                    userMessage,
                    toAiHistory(historyMessages),
                    sink,
                    accumulator
            );
            return;
        }

        universalAiChatHandler.streamToSink(
                aiProperties,
                userMessage,
                toAiHistory(historyMessages),
                sink,
                accumulator
        );
    }

    private AiPropertiesDO toAiProperties(AgentPropertiesDO agentProperties, String provider) {
        AiPropertiesDO aiProperties = new AiPropertiesDO();
        aiProperties.setAiType(PROVIDER_ANTHROPIC.equalsIgnoreCase(provider) ? PROVIDER_ANTHROPIC : PROVIDER_OPENAI);
        aiProperties.setApiKey(agentProperties.getApiKey());
        aiProperties.setApiUrl(agentProperties.getApiFlowId());
        aiProperties.setModelName(StrUtil.blankToDefault(
                agentProperties.getApiSecret(),
                PROVIDER_ANTHROPIC.equalsIgnoreCase(provider) ? "mimo-v2.5-pro" : "mimo-v2.5"
        ));
        aiProperties.setMaxTokens(8192);
        return aiProperties;
    }

    private List<AiMessageHistoryRespDTO> toAiHistory(List<AgentMessageHistoryRespDTO> historyMessages) {
        if (CollUtil.isEmpty(historyMessages)) {
            return Collections.emptyList();
        }
        return historyMessages.stream().map(history -> {
            AiMessageHistoryRespDTO aiHistory = new AiMessageHistoryRespDTO();
            aiHistory.setId(history.getId());
            aiHistory.setSessionId(history.getSessionId());
            aiHistory.setMessageType(history.getMessageType());
            aiHistory.setMessageContent(history.getMessageContent());
            aiHistory.setMessageSeq(history.getMessageSeq());
            aiHistory.setTokenCount(history.getTokenCount());
            aiHistory.setResponseTime(history.getResponseTime());
            aiHistory.setCreateTime(history.getCreateTime());
            return aiHistory;
        }).collect(Collectors.toList());
    }

    private String buildHistoryJson(List<AgentMessageHistoryRespDTO> historyMessages) {
        List<HashMap<String, String>> historyList = new ArrayList<>();
        if (CollUtil.isNotEmpty(historyMessages)) {
            historyList = historyMessages.stream().map(history -> {
                HashMap<String, String> item = new HashMap<>();
                item.put("role", history.getMessageType() == 1 ? "user" : "assistant");
                item.put("content_type", "text");
                item.put("content", history.getMessageContent());
                return item;
            }).collect(Collectors.toList());
        }
        return JSON.toJSONString(historyList);
    }

    private static class SseEmitterFluxSink implements FluxSink<String> {

        private final SseEmitter emitter;
        private volatile boolean cancelled;

        private SseEmitterFluxSink(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public FluxSink<String> next(String value) {
            if (cancelled) {
                return this;
            }
            try {
                emitter.send(SseEmitter.event().data(value));
            } catch (IOException ex) {
                cancelled = true;
                throw new RuntimeException(ex);
            }
            return this;
        }

        @Override
        public void complete() {
            cancelled = true;
        }

        @Override
        public void error(Throwable throwable) {
            cancelled = true;
            emitter.completeWithError(throwable);
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
