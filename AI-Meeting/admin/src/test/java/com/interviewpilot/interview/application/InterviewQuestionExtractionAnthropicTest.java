package com.interviewpilot.interview.application;

import com.interviewpilot.agent.application.BusinessAgentResolver;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.common.config.storage.ApplicationStorageProperties;
import com.interviewpilot.interview.api.io.req.InterviewQuestionReqDTO;
import com.interviewpilot.interview.api.io.resp.InterviewQuestionRespDTO;
import com.interviewpilot.interview.application.guard.core.InterviewAiGuardStage;
import com.interviewpilot.interview.application.guard.lock.InterviewAiSessionLockService;
import com.interviewpilot.interview.flow.extraction.InterviewQuestionExtractionService;
import com.interviewpilot.interview.service.InterviewQuestionCacheService;
import com.interviewpilot.interview.service.InterviewQuestionService;
import com.interviewpilot.interview.shared.InterviewAiInvoker;
import com.interviewpilot.interview.shared.InterviewOpeningQuestionSupport;
import com.interviewpilot.interview.shared.InterviewResponseParser;
import com.interviewpilot.toolkit.iflytek.XunfeiWorkflowClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RLock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewQuestionExtractionAnthropicTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreResumeLocallyAndSkipLegacyXunfeiUploadWhenProviderIsAnthropic() throws Exception {
        BusinessAgentResolver businessAgentResolver = mock(BusinessAgentResolver.class);
        XunfeiWorkflowClient xunfeiWorkflowClient = mock(XunfeiWorkflowClient.class);
        InterviewAiInvoker interviewAiInvoker = mock(InterviewAiInvoker.class);
        InterviewAiSessionLockService interviewAiSessionLockService = mock(InterviewAiSessionLockService.class);
        InterviewQuestionService interviewQuestionService = mock(InterviewQuestionService.class);
        InterviewQuestionCacheService interviewQuestionCacheService = mock(InterviewQuestionCacheService.class);

        InterviewQuestionExtractionService service = new InterviewQuestionExtractionService(
                businessAgentResolver,
                providerOf(xunfeiWorkflowClient),
                interviewAiInvoker,
                interviewAiSessionLockService,
                interviewQuestionService,
                interviewQuestionCacheService,
                new InterviewResponseParser(),
                storageProperties()
        );

        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setId(100L);
        agent.setAgentName("Mimo Interview Question Extractor");
        agent.setApiKey("tp-test-key");
        agent.setApiSecret("mimo-v2.5-pro");
        agent.setApiFlowId("https://token-plan-cn.xiaomimimo.com/anthropic");
        agent.setAiProvider("anthropic");

        when(businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_QUESTION_EXTRACTION))
                .thenReturn(agent);

        RLock heavyLock = mock(RLock.class);
        when(interviewAiSessionLockService.acquire("session-anthropic", InterviewAiGuardStage.INTERVIEW_EXTRACTION))
                .thenReturn(heavyLock);

        String anthropicResponse = """
                {"choices":[{"delta":{"role":"assistant","content":"{\\"questions\\":[\\"Question 1\\",\\"Question 2\\"],\\"sugest\\":[\\"Suggestion 1\\"],\\"type\\":\\"technical\\",\\"resumeScore\\":80}"}}]}
                """;
        when(interviewAiInvoker.callAiSyncWithFile(
                any(),
                eq("session-anthropic"),
                eq(agent),
                argThat(url -> url != null && url.startsWith("/agent-files/")),
                eq(InterviewAiGuardStage.INTERVIEW_EXTRACTION),
                any(),
                any()
        )).thenReturn(anthropicResponse);

        InterviewQuestionReqDTO request = new InterviewQuestionReqDTO();
        request.setSessionId("session-anthropic");
        request.setUserName("tester");
        request.setResumePdf(new MockMultipartFile(
                "resumePdf",
                "resume.pdf",
                "application/pdf",
                "%PDF-1.4\nresume".getBytes(StandardCharsets.UTF_8)
        ));

        service.extractInterviewQuestions(request);

        verify(xunfeiWorkflowClient, never()).uploadFile(any(), any(), any());
        verify(interviewAiInvoker).callAiSyncWithFile(
                any(),
                eq("session-anthropic"),
                eq(agent),
                argThat(url -> url != null && url.startsWith("/agent-files/")),
                eq(InterviewAiGuardStage.INTERVIEW_EXTRACTION),
                any(),
                any()
        );
    }

    @Test
    void shouldPrependSelfIntroductionToExtractedResumeQuestions() throws Exception {
        BusinessAgentResolver businessAgentResolver = mock(BusinessAgentResolver.class);
        XunfeiWorkflowClient xunfeiWorkflowClient = mock(XunfeiWorkflowClient.class);
        InterviewAiInvoker interviewAiInvoker = mock(InterviewAiInvoker.class);
        InterviewAiSessionLockService interviewAiSessionLockService = mock(InterviewAiSessionLockService.class);
        InterviewQuestionService interviewQuestionService = mock(InterviewQuestionService.class);
        InterviewQuestionCacheService interviewQuestionCacheService = mock(InterviewQuestionCacheService.class);

        InterviewQuestionExtractionService service = new InterviewQuestionExtractionService(
                businessAgentResolver,
                providerOf(xunfeiWorkflowClient),
                interviewAiInvoker,
                interviewAiSessionLockService,
                interviewQuestionService,
                interviewQuestionCacheService,
                new InterviewResponseParser(),
                storageProperties()
        );

        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setId(101L);
        agent.setApiKey("tp-test-key");
        agent.setApiSecret("mimo-v2.5-pro");
        agent.setApiFlowId("https://token-plan-cn.xiaomimimo.com/anthropic");
        agent.setAiProvider("anthropic");

        when(businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_QUESTION_EXTRACTION))
                .thenReturn(agent);

        RLock heavyLock = mock(RLock.class);
        when(interviewAiSessionLockService.acquire("session-resume-self-intro", InterviewAiGuardStage.INTERVIEW_EXTRACTION))
                .thenReturn(heavyLock);

        String anthropicResponse = """
                {"choices":[{"delta":{"role":"assistant","content":"{\\"questions\\":[\\"请介绍一下你最有代表性的项目经历。\\",\\"你如何处理线上故障？\\"],\\"sugest\\":[\\"回答时突出结果\\"],\\"type\\":\\"technical\\",\\"resumeScore\\":80}"}}]}
                """;
        when(interviewAiInvoker.callAiSyncWithFile(
                any(),
                eq("session-resume-self-intro"),
                eq(agent),
                argThat(url -> url != null && url.startsWith("/agent-files/")),
                eq(InterviewAiGuardStage.INTERVIEW_EXTRACTION),
                any(),
                any()
        )).thenReturn(anthropicResponse);

        when(interviewQuestionCacheService.getSessionInterviewQuestions("session-resume-self-intro"))
                .thenReturn(Map.of(
                        "1", InterviewOpeningQuestionSupport.SELF_INTRODUCTION_QUESTION,
                        "2", "请介绍一下你最有代表性的项目经历。",
                        "3", "你如何处理线上故障？"
                ));
        when(interviewQuestionCacheService.getSessionInterviewSuggestions("session-resume-self-intro"))
                .thenReturn(Map.of("1", "回答时突出结果"));

        InterviewQuestionReqDTO request = new InterviewQuestionReqDTO();
        request.setSessionId("session-resume-self-intro");
        request.setUserName("tester");
        request.setResumePdf(new MockMultipartFile(
                "resumePdf",
                "resume.pdf",
                "application/pdf",
                "%PDF-1.4\nresume".getBytes(StandardCharsets.UTF_8)
        ));

        InterviewQuestionRespDTO response = service.extractInterviewQuestions(request);

        assertEquals(1, response.getIsSuccess());
        assertEquals(3, response.getQuestionCount());
        assertEquals(InterviewOpeningQuestionSupport.SELF_INTRODUCTION_QUESTION, response.getQuestions().get("1"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<String>> questionsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(interviewQuestionCacheService).cacheInterviewQuestions(eq("session-resume-self-intro"), questionsCaptor.capture());
        assertEquals(InterviewOpeningQuestionSupport.SELF_INTRODUCTION_QUESTION, questionsCaptor.getValue().get(0));
        assertEquals(3, questionsCaptor.getValue().size());
    }

    private ApplicationStorageProperties storageProperties() {
        ApplicationStorageProperties properties = new ApplicationStorageProperties();
        properties.setBaseDir(tempDir.toString());
        properties.setAgentFileDir(tempDir.resolve("agent-files").toString());
        return properties;
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
