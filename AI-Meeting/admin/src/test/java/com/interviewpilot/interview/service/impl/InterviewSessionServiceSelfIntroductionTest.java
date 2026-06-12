package com.interviewpilot.interview.service.impl;

import com.interviewpilot.agent.application.BusinessAgentResolver;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.interview.api.io.req.InterviewFromBankReqDTO;
import com.interviewpilot.interview.application.InterviewSessionOwnershipService;
import com.interviewpilot.interview.application.runtime.InterviewSessionRuntimeSnapshotService;
import com.interviewpilot.interview.dao.entity.InterviewSession;
import com.interviewpilot.interview.dao.entity.InterviewSessionQuestionDO;
import com.interviewpilot.interview.dao.mapper.InterviewSessionQuestionMapper;
import com.interviewpilot.interview.dao.repository.InterviewSessionRepository;
import com.interviewpilot.interview.service.InterviewQuestionCacheService;
import com.interviewpilot.interview.shared.InterviewOpeningQuestionSupport;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import com.interviewpilot.questionbank.service.QuestionBankService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewSessionServiceSelfIntroductionTest {

    @Test
    void shouldPrependSelfIntroductionWhenCreatingBankSession() {
        InterviewSessionRepository repository = mock(InterviewSessionRepository.class);
        when(repository.findByUserIdAndStatusInAndDelFlagOrderByUpdateTimeDesc(any(), any(), any()))
                .thenReturn(List.of());
        when(repository.save(any(InterviewSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewSessionOwnershipService ownershipService = mock(InterviewSessionOwnershipService.class);
        BusinessAgentResolver businessAgentResolver = mock(BusinessAgentResolver.class);
        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setId(9L);
        when(businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_QUESTION_ASKING)).thenReturn(agent);

        @SuppressWarnings("unchecked")
        ObjectProvider<InterviewSessionRuntimeSnapshotService> runtimeProvider = mock(ObjectProvider.class);
        when(runtimeProvider.getIfAvailable()).thenReturn(null);

        QuestionBankService questionBankService = mock(QuestionBankService.class);
        QuestionDO firstQuestion = new QuestionDO();
        firstQuestion.setId(1001L);
        firstQuestion.setTitle("请介绍一下你做过的项目。");
        QuestionDO secondQuestion = new QuestionDO();
        secondQuestion.setId(1002L);
        secondQuestion.setTitle("你如何处理线上故障？");
        when(questionBankService.randomSelect(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(firstQuestion, secondQuestion));

        InterviewSessionQuestionMapper sessionQuestionMapper = mock(InterviewSessionQuestionMapper.class);
        InterviewQuestionCacheService cacheService = mock(InterviewQuestionCacheService.class);

        InterviewSessionServiceImpl service = new InterviewSessionServiceImpl(
                repository,
                ownershipService,
                businessAgentResolver,
                runtimeProvider,
                questionBankService,
                sessionQuestionMapper,
                cacheService
        );

        InterviewFromBankReqDTO request = new InterviewFromBankReqDTO();
        request.setQuestionCount(2);

        service.createFromBank(7L, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> cachedQuestionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(cacheService).cacheInterviewQuestions(any(), cachedQuestionsCaptor.capture());
        List<String> cachedQuestions = cachedQuestionsCaptor.getValue();
        assertEquals(3, cachedQuestions.size());
        assertEquals(InterviewOpeningQuestionSupport.SELF_INTRODUCTION_QUESTION, cachedQuestions.get(0));
        assertEquals("请介绍一下你做过的项目。", cachedQuestions.get(1));
        assertEquals("你如何处理线上故障？", cachedQuestions.get(2));

        ArgumentCaptor<InterviewSessionQuestionDO> sessionQuestionCaptor =
                ArgumentCaptor.forClass(InterviewSessionQuestionDO.class);
        verify(sessionQuestionMapper, times(2)).insert(sessionQuestionCaptor.capture());
        List<InterviewSessionQuestionDO> links = sessionQuestionCaptor.getAllValues();
        assertEquals(0, links.get(0).getSeqIndex());
        assertEquals(1, links.get(1).getSeqIndex());
    }
}
