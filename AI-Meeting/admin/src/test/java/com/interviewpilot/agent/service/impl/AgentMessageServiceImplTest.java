package com.interviewpilot.agent.service.impl;

import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.agent.application.AgentResolver;
import com.interviewpilot.agent.service.AgentConversationService;
import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.ai.service.chat.AnthropicChatHandler;
import com.interviewpilot.ai.service.chat.UniversalAiChatHandler;
import com.interviewpilot.conversation.application.ConversationMessageHistoryService;
import com.interviewpilot.conversation.application.ConversationMessagePersistenceService;
import com.interviewpilot.conversation.application.ConversationOwnershipService;
import com.interviewpilot.conversation.application.ConversationStreamingSupport;
import com.interviewpilot.toolkit.ai.AIContentAccumulator;
import com.interviewpilot.toolkit.ai.AgentPropertiesLoader;
import com.interviewpilot.toolkit.iflytek.XunfeiWorkflowClient;
import com.interviewpilot.user.api.io.req.UserMessageReqDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentMessageServiceImplTest {

    @Test
    void agentChatSse_ShouldRouteOpenAiProviderToMimoHandlerWithoutCallingLegacyXunfei() throws Exception {
        XunfeiWorkflowClient xunfeiWorkflowClient = mock(XunfeiWorkflowClient.class);
        UniversalAiChatHandler universalAiChatHandler = mock(UniversalAiChatHandler.class);
        AnthropicChatHandler anthropicChatHandler = mock(AnthropicChatHandler.class);
        AgentResolver agentResolver = mock(AgentResolver.class);
        AgentPropertiesLoader agentPropertiesLoader = mock(AgentPropertiesLoader.class);
        ConversationMessageHistoryService historyService = mock(ConversationMessageHistoryService.class);
        ConversationMessagePersistenceService persistenceService = mock(ConversationMessagePersistenceService.class);
        CountDownLatch routed = new CountDownLatch(1);

        AgentPropertiesDO mimoAgent = new AgentPropertiesDO();
        mimoAgent.setId(13L);
        mimoAgent.setAiProvider("openai");
        mimoAgent.setApiKey("tp-test-key");
        mimoAgent.setApiSecret("mimo-v2.5");
        mimoAgent.setApiFlowId("https://token-plan-cn.xiaomimimo.com/v1");

        when(agentResolver.resolveAgentId("session-mimo", null)).thenReturn(13L);
        when(agentPropertiesLoader.getByAgentId(13L)).thenReturn(mimoAgent);
        when(historyService.listAgentHistory("session-mimo")).thenReturn(List.of());
        when(persistenceService.saveAgentUserMessage(anyString(), anyString())).thenReturn(1);
        when(persistenceService.saveAgentAssistantMessage(anyString(), anyString(), any(Integer.class), any()))
                .thenReturn(2);

        doAnswer(invocation -> {
            AiPropertiesDO props = invocation.getArgument(0);
            AIContentAccumulator accumulator = invocation.getArgument(4);
            accumulator.appendSimpleContent("mimo-ok");
            routed.countDown();
            return null;
        }).when(universalAiChatHandler).streamToSink(
                any(AiPropertiesDO.class),
                anyString(),
                any(),
                any(FluxSink.class),
                any(AIContentAccumulator.class)
        );

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.initialize();
        try {
            AgentMessageServiceImpl service = new AgentMessageServiceImpl(
                    agentResolver,
                    providerOf(xunfeiWorkflowClient),
                    universalAiChatHandler,
                    anthropicChatHandler,
                    agentPropertiesLoader,
                    mock(AgentConversationService.class),
                    mock(ConversationOwnershipService.class),
                    historyService,
                    persistenceService,
                    new ConversationStreamingSupport(),
                    executor
            );

            UserMessageReqDTO request = new UserMessageReqDTO();
            request.setSessionId("session-mimo");
            request.setInputMessage("hello");
            service.agentChatSse(request, 1001L);

            assertTrue(routed.await(3, TimeUnit.SECONDS));
        } finally {
            executor.shutdown();
        }

        ArgumentCaptor<AiPropertiesDO> propsCaptor = ArgumentCaptor.forClass(AiPropertiesDO.class);
        verify(universalAiChatHandler).streamToSink(
                propsCaptor.capture(),
                anyString(),
                any(),
                any(FluxSink.class),
                any(AIContentAccumulator.class)
        );
        assertEquals("openai", propsCaptor.getValue().getAiType());
        assertEquals("mimo-v2.5", propsCaptor.getValue().getModelName());
        assertEquals("https://token-plan-cn.xiaomimimo.com/v1", propsCaptor.getValue().getApiUrl());
        verify(anthropicChatHandler, never()).streamToSink(any(), anyString(), any(), any(), any());
        verify(xunfeiWorkflowClient, never()).chat(
                anyString(),
                anyString(),
                anyString(),
                Mockito.anyBoolean(),
                any(),
                any(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    private ObjectProvider<XunfeiWorkflowClient> providerOf(XunfeiWorkflowClient client) {
        return new ObjectProvider<>() {
            @Override
            public XunfeiWorkflowClient getObject(Object... args) {
                return client;
            }

            @Override
            public XunfeiWorkflowClient getIfAvailable() {
                return client;
            }

            @Override
            public XunfeiWorkflowClient getIfUnique() {
                return client;
            }

            @Override
            public XunfeiWorkflowClient getObject() {
                return client;
            }
        };
    }
}
