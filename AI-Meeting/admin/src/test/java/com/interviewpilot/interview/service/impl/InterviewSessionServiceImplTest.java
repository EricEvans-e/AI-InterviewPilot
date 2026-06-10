package com.interviewpilot.interview.service.impl;

import com.interviewpilot.agent.application.BusinessAgentResolver;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.interview.api.io.req.InterviewFromBankReqDTO;
import com.interviewpilot.interview.dao.entity.InterviewSession;
import com.interviewpilot.interview.dao.mapper.InterviewSessionQuestionMapper;
import com.interviewpilot.interview.dao.repository.InterviewSessionRepository;
import com.interviewpilot.interview.application.InterviewSessionOwnershipService;
import com.interviewpilot.interview.application.runtime.InterviewSessionRuntimeSnapshotService;
import com.interviewpilot.interview.service.InterviewQuestionCacheService;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import com.interviewpilot.questionbank.service.QuestionBankService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewSessionServiceImplTest {

    @Test
    void shouldPassAbilityTagAndDifficultyWhenCreatingBankSession() {
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
        QuestionDO question = new QuestionDO();
        question.setId(1001L);
        question.setTitle("题目 1");
        when(questionBankService.randomSelect(11L, 22L, "专业题", "communication", "hard", 5))
                .thenReturn(List.of(question));

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
        request.setCollegeId(11L);
        request.setMajorId(22L);
        request.setInterviewMode("专业题");
        request.setQuestionCount(5);
        request.setAbilityTag("communication");
        request.setDifficulty("hard");

        service.createFromBank(7L, request);

        verify(questionBankService).randomSelect(11L, 22L, "专业题", "communication", "hard", 5);
    }
}
