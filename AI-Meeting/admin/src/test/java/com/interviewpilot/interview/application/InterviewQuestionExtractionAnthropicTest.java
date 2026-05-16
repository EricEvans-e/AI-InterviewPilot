package com.interviewpilot.interview.application;

import com.interviewpilot.agent.application.BusinessAgentResolver;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.interview.api.io.req.InterviewQuestionReqDTO;
import com.interviewpilot.interview.api.io.resp.InterviewQuestionRespDTO;
import com.interviewpilot.interview.application.guard.core.InterviewAiGuardStage;
import com.interviewpilot.interview.application.guard.lock.InterviewAiSessionLockService;
import com.interviewpilot.interview.flow.extraction.InterviewQuestionExtractionService;
import com.interviewpilot.interview.service.InterviewQuestionCacheService;
import com.interviewpilot.interview.service.InterviewQuestionService;
import com.interviewpilot.interview.shared.InterviewAiInvoker;
import com.interviewpilot.interview.shared.InterviewResponseParser;
import com.interviewpilot.toolkit.iflytek.XunfeiWorkflowClient;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 Anthropic 模式下简历上传被跳过，直接走纯文本 prompt。
 */
class InterviewQuestionExtractionAnthropicTest {

    @Test
    void shouldSkipFileUploadWhenAiProviderIsAnthropic() throws Exception {
        BusinessAgentResolver businessAgentResolver = Mockito.mock(BusinessAgentResolver.class, Mockito.withSettings().mockMaker(MockMakers.SUBCLASS));
        XunfeiWorkflowClient xunfeiWorkflowClient = Mockito.mock(XunfeiWorkflowClient.class, Mockito.withSettings().mockMaker(MockMakers.SUBCLASS));
        InterviewAiInvoker interviewAiInvoker = Mockito.mock(InterviewAiInvoker.class, Mockito.withSettings().mockMaker(MockMakers.SUBCLASS));
        InterviewAiSessionLockService interviewAiSessionLockService = Mockito.mock(InterviewAiSessionLockService.class, Mockito.withSettings().mockMaker(MockMakers.SUBCLASS));
        InterviewQuestionService interviewQuestionService = Mockito.mock(InterviewQuestionService.class, Mockito.withSettings().mockMaker(MockMakers.SUBCLASS));
        InterviewQuestionCacheService interviewQuestionCacheService = Mockito.mock(InterviewQuestionCacheService.class, Mockito.withSettings().mockMaker(MockMakers.SUBCLASS));
        InterviewResponseParser interviewResponseParser = new InterviewResponseParser();

        InterviewQuestionExtractionService service = new InterviewQuestionExtractionService(
                businessAgentResolver,
                xunfeiWorkflowClient,
                interviewAiInvoker,
                interviewAiSessionLockService,
                interviewQuestionService,
                interviewQuestionCacheService,
                interviewResponseParser
        );

        // Anthropic Agent
        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setId(100L);
        agent.setAgentName("Mimo面试出题官");
        agent.setApiKey("tp-test-key");
        agent.setApiSecret("mimo-v2.5-pro");
        agent.setApiFlowId("https://token-plan-sgp.xiaomimimo.com/anthropic");
        agent.setAiProvider("anthropic");

        when(businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_QUESTION_EXTRACTION))
                .thenReturn(agent);

        RLock heavyLock = Mockito.mock(RLock.class, Mockito.withSettings().mockMaker(MockMakers.SUBCLASS));
        when(interviewAiSessionLockService.acquire("session-anthropic", InterviewAiGuardStage.INTERVIEW_EXTRACTION))
                .thenReturn(heavyLock);

        // Anthropic 模式下 callAiSyncWithFile 传入的 fileUrl 应为空字符串
        String anthropicResponse = """
                {"choices":[{"delta":{"role":"assistant","content":"{\\"questions\\":[\\"问题1\\",\\"问题2\\"],\\"sugest\\":[\\"建议1\\"],\\"type\\":\\"技术面试\\",\\"resumeScore\\":80}"}}]}
                """;
        when(interviewAiInvoker.callAiSyncWithFile(
                any(),
                eq("session-anthropic"),
                eq(agent),
                eq(""),  // fileUrl 为空（Anthropic 跳过上传）
                eq(InterviewAiGuardStage.INTERVIEW_EXTRACTION),
                any()
        )).thenReturn(anthropicResponse);

        InterviewQuestionReqDTO request = new InterviewQuestionReqDTO();
        request.setSessionId("session-anthropic");
        request.setUserName("tester");
        request.setResumePdf(new MockMultipartFile(
                "resumePdf",
                "resume.pdf",
                "application/pdf",
                "dummy".getBytes(StandardCharsets.UTF_8)
        ));

        InterviewQuestionRespDTO response = service.extractInterviewQuestions(request);

        // 关键验证：Anthropic 模式下不应调用 XunfeiWorkflowClient.uploadFile()
        verify(xunfeiWorkflowClient, never()).uploadFile(any(), any(), any());

        // 验证调用了 invoker（fileUrl=""）
        verify(interviewAiInvoker).callAiSyncWithFile(
                any(),
                eq("session-anthropic"),
                eq(agent),
                eq(""),
                eq(InterviewAiGuardStage.INTERVIEW_EXTRACTION),
                any()
        );
    }
}
