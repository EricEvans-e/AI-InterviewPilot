package com.interviewpilot.interview.flow.demeanor;

import com.interviewpilot.agent.application.BusinessAgentResolver;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.interview.api.io.req.DemeanorEvaluationReqDTO;
import com.interviewpilot.interview.application.guard.core.InterviewAiGuardStage;
import com.interviewpilot.interview.application.guard.lock.InterviewAiSessionLockService;
import com.interviewpilot.interview.application.runtime.InterviewSessionRuntimeSnapshotService;
import com.interviewpilot.interview.application.strategy.AdaptiveDemeanorNormalizationStrategy;
import com.interviewpilot.interview.shared.InterviewAiInvoker;
import com.interviewpilot.interview.shared.InterviewResponseParser;
import com.interviewpilot.interview.service.InterviewQuestionCacheService;
import com.interviewpilot.toolkit.iflytek.XunfeiWorkflowClient;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewDemeanorServiceTest {

    @Test
    void shouldRouteOpenAiProviderToMimoWithoutLegacyXunfeiUpload() throws Exception {
        XunfeiWorkflowClient xunfeiWorkflowClient = mock(XunfeiWorkflowClient.class);
        InterviewAiInvoker interviewAiInvoker = mock(InterviewAiInvoker.class);
        InterviewQuestionCacheService cacheService = mock(InterviewQuestionCacheService.class);
        InterviewSessionRuntimeSnapshotService runtimeSnapshotService = mock(InterviewSessionRuntimeSnapshotService.class);
        InterviewAiSessionLockService lockService = mock(InterviewAiSessionLockService.class);
        BusinessAgentResolver businessAgentResolver = mock(BusinessAgentResolver.class);
        RLock lock = mock(RLock.class);

        AgentPropertiesDO agent = mimoAgent();
        when(businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_DEMEANOR)).thenReturn(agent);
        when(lockService.acquire("session-mimo", InterviewAiGuardStage.INTERVIEW_DEMEANOR)).thenReturn(lock);
        when(interviewAiInvoker.callAiSyncWithFile(
                argThat(prompt -> prompt != null && prompt.contains("panicLevel")),
                eq("session-mimo"),
                eq(agent),
                eq(null),
                eq(InterviewAiGuardStage.INTERVIEW_DEMEANOR),
                any()
        )).thenReturn("""
                {"choices":[{"message":{"content":"{\\"panicLevel\\":20,\\"seriousnessLevel\\":80,\\"emoticonHandling\\":75,\\"compositeScore\\":82}"}}]}
                """);

        InterviewDemeanorService service = newService(
                xunfeiWorkflowClient,
                interviewAiInvoker,
                cacheService,
                runtimeSnapshotService,
                lockService,
                businessAgentResolver
        );

        DemeanorEvaluationReqDTO request = new DemeanorEvaluationReqDTO();
        request.setSessionId("session-mimo");
        request.setUserPhoto(new MockMultipartFile(
                "userPhoto",
                "face.jpg",
                "image/jpeg",
                "fake-image".getBytes(StandardCharsets.UTF_8)
        ));

        String result = service.evaluateDemeanor(request);

        assertEquals("Demeanor evaluation completed", result);
        verify(xunfeiWorkflowClient, never()).uploadFile(any(), any(), any());
        verify(cacheService).cacheDemeanorScoreDetails("session-mimo", 20, 80, 75, 82);
        verify(cacheService).cacheDemeanorScore("session-mimo", 82);
        verify(runtimeSnapshotService).refreshAfterDemeanorEvaluated("session-mimo");
        verify(lockService).release(lock);
    }

    @Test
    void shouldFailClearlyWhenLegacyXingchenProviderIsConfiguredButClientDisabled() throws Exception {
        InterviewAiInvoker interviewAiInvoker = mock(InterviewAiInvoker.class);
        InterviewQuestionCacheService cacheService = mock(InterviewQuestionCacheService.class);
        InterviewSessionRuntimeSnapshotService runtimeSnapshotService = mock(InterviewSessionRuntimeSnapshotService.class);
        InterviewAiSessionLockService lockService = mock(InterviewAiSessionLockService.class);
        BusinessAgentResolver businessAgentResolver = mock(BusinessAgentResolver.class);
        RLock lock = mock(RLock.class);

        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setId(9L);
        agent.setAiProvider("xingchen");
        agent.setApiKey("legacy-key");
        agent.setApiSecret("legacy-secret");
        when(businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_DEMEANOR)).thenReturn(agent);
        when(lockService.acquire("session-legacy", InterviewAiGuardStage.INTERVIEW_DEMEANOR)).thenReturn(lock);

        InterviewDemeanorService service = newService(
                null,
                interviewAiInvoker,
                cacheService,
                runtimeSnapshotService,
                lockService,
                businessAgentResolver
        );

        DemeanorEvaluationReqDTO request = new DemeanorEvaluationReqDTO();
        request.setSessionId("session-legacy");
        request.setUserPhoto(new MockMultipartFile(
                "userPhoto",
                "face.jpg",
                "image/jpeg",
                "fake-image".getBytes(StandardCharsets.UTF_8)
        ));

        ClientException ex = assertThrows(ClientException.class, () -> service.evaluateDemeanor(request));

        assertEquals(true, ex.getMessage().contains("LEGACY_XUNFEI_ENABLED=true"));
        verify(lockService).release(lock);
    }

    private InterviewDemeanorService newService(
            XunfeiWorkflowClient xunfeiWorkflowClient,
            InterviewAiInvoker interviewAiInvoker,
            InterviewQuestionCacheService cacheService,
            InterviewSessionRuntimeSnapshotService runtimeSnapshotService,
            InterviewAiSessionLockService lockService,
            BusinessAgentResolver businessAgentResolver) {
        ObjectProvider<XunfeiWorkflowClient> provider = new ObjectProvider<>() {
            @Override
            public XunfeiWorkflowClient getObject(Object... args) {
                return xunfeiWorkflowClient;
            }

            @Override
            public XunfeiWorkflowClient getIfAvailable() {
                return xunfeiWorkflowClient;
            }

            @Override
            public XunfeiWorkflowClient getIfUnique() {
                return xunfeiWorkflowClient;
            }

            @Override
            public XunfeiWorkflowClient getObject() {
                return xunfeiWorkflowClient;
            }
        };
        return new InterviewDemeanorService(
                provider,
                businessAgentResolver,
                cacheService,
                interviewAiInvoker,
                lockService,
                new InterviewResponseParser(),
                new AdaptiveDemeanorNormalizationStrategy(),
                runtimeSnapshotService
        );
    }

    private AgentPropertiesDO mimoAgent() {
        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setId(9L);
        agent.setAiProvider("openai");
        agent.setApiKey("tp-test-key");
        agent.setApiSecret("mimo-v2.5");
        agent.setApiFlowId("https://token-plan-cn.xiaomimimo.com/v1");
        return agent;
    }
}
