package com.interviewpilot.interview.flow.session;

import com.interviewpilot.interview.api.io.resp.InterviewAnswerRespDTO;
import com.interviewpilot.interview.application.flow.InterviewFlowStateMachine;
import com.interviewpilot.interview.application.runtime.InterviewSessionRuntimeRehydrateService;
import com.interviewpilot.interview.application.runtime.InterviewRuntimeRehydrateScope;
import com.interviewpilot.interview.flow.answer.InterviewAnswerPipeline;
import com.interviewpilot.interview.flow.answer.InterviewQuestionLockService;
import com.interviewpilot.interview.flow.demeanor.InterviewDemeanorService;
import com.interviewpilot.interview.flow.extraction.InterviewQuestionExtractionService;
import com.interviewpilot.interview.service.InterviewQuestionCacheService;
import com.interviewpilot.interview.service.model.InterviewFlowState;
import com.interviewpilot.interview.service.model.InterviewRuntimeLoadMode;
import com.interviewpilot.interview.shared.InterviewOpeningQuestionSupport;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewAgentOrchestrationSelfIntroductionTest {

    @Test
    void shouldReturnSelfIntroductionAsCurrentQuestionFromSharedFlow() {
        InterviewQuestionCacheService cacheService = mock(InterviewQuestionCacheService.class);
        InterviewQuestionExtractionService extractionService = mock(InterviewQuestionExtractionService.class);
        InterviewDemeanorService demeanorService = mock(InterviewDemeanorService.class);
        InterviewAnswerPipeline answerPipeline = mock(InterviewAnswerPipeline.class);
        InterviewFlowStateMachine flowStateMachine = mock(InterviewFlowStateMachine.class);
        InterviewQuestionLockService questionLockService = mock(InterviewQuestionLockService.class);
        InterviewSessionRuntimeRehydrateService runtimeRehydrateService = mock(InterviewSessionRuntimeRehydrateService.class);

        InterviewAgentOrchestrationService service = new InterviewAgentOrchestrationService(
                cacheService,
                extractionService,
                demeanorService,
                answerPipeline,
                flowStateMachine,
                questionLockService,
                runtimeRehydrateService
        );

        Map<String, String> questions = new LinkedHashMap<>();
        questions.put("1", InterviewOpeningQuestionSupport.SELF_INTRODUCTION_QUESTION);
        questions.put("2", "请介绍一下你做过的项目。");
        when(cacheService.getSessionInterviewQuestions("session-1")).thenReturn(questions);

        InterviewFlowState flowState = new InterviewFlowState();
        flowState.setStatus("ASKING");
        flowState.setCurrentIndex(0);
        flowState.setCurrentQuestionNumber("1");
        flowState.setTotalQuestions(2);
        when(cacheService.getInterviewFlow("session-1")).thenReturn(flowState);
        when(flowStateMachine.isOutOfRange(flowState)).thenReturn(false);
        when(flowStateMachine.currentQuestionNumber(flowState)).thenReturn("1");

        InterviewAnswerRespDTO response = service.getCurrentQuestion("session-1");

        assertTrue(Boolean.TRUE.equals(response.getIsSuccess()));
        assertEquals("1", response.getQuestionNumber());
        assertEquals(InterviewOpeningQuestionSupport.SELF_INTRODUCTION_QUESTION, response.getQuestionContent());
        assertEquals("1", response.getNextQuestionNumber());
        assertEquals(InterviewOpeningQuestionSupport.SELF_INTRODUCTION_QUESTION, response.getNextQuestion());
        verify(runtimeRehydrateService).ensureRuntime(
                eq("session-1"),
                eq(InterviewRuntimeLoadMode.READ_WRITE_REQUIRED),
                eq(InterviewRuntimeRehydrateScope.FLOW_ONLY)
        );
        verify(cacheService, never()).loadInterviewQuestionsFromDatabase(any());
    }
}
