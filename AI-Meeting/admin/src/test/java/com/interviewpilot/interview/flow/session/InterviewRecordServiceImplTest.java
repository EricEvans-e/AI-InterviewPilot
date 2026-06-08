package com.interviewpilot.interview.service.impl;

import com.interviewpilot.interview.application.InterviewSessionOwnershipService;
import com.interviewpilot.interview.application.finalize.InterviewFinalizeLockService;
import com.interviewpilot.interview.application.runtime.InterviewSessionRuntimeRehydrateService;
import com.interviewpilot.interview.application.runtime.InterviewSessionRuntimeSnapshotService;
import com.interviewpilot.interview.application.strategy.DimensionScoreResult;
import com.interviewpilot.interview.application.strategy.DimensionScoreStrategy;
import com.interviewpilot.interview.application.strategy.WeightedRadarComputationStrategy;
import com.interviewpilot.interview.dao.entity.InterviewRecordDO;
import com.interviewpilot.interview.dao.entity.InterviewSession;
import com.interviewpilot.interview.dao.mapper.InterviewRecordMapper;
import com.interviewpilot.interview.flow.report.InterviewRecordServiceImpl;
import com.interviewpilot.interview.service.InterviewQuestionCacheService;
import com.interviewpilot.interview.service.InterviewQuestionService;
import com.interviewpilot.interview.service.InterviewSessionService;
import com.interviewpilot.interview.service.model.InterviewTurnLog;
import com.interviewpilot.interview.service.model.InterviewSessionStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.redisson.api.RLock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewRecordServiceImplTest {

    @Test
    void shouldFinishInterviewSessionBeforePersistingRecordFromRedis() throws Exception {
        InterviewQuestionCacheService cacheService = mock(InterviewQuestionCacheService.class);
        InterviewSessionOwnershipService ownershipService = mock(InterviewSessionOwnershipService.class);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        InterviewQuestionService questionService = mock(InterviewQuestionService.class);
        InterviewFinalizeLockService finalizeLockService = mock(InterviewFinalizeLockService.class);
        InterviewSessionRuntimeSnapshotService runtimeSnapshotService = mock(InterviewSessionRuntimeSnapshotService.class);
        InterviewSessionRuntimeRehydrateService runtimeRehydrateService = mock(InterviewSessionRuntimeRehydrateService.class);
        DimensionScoreStrategy dimensionScoreStrategy = mock(DimensionScoreStrategy.class);
        WeightedRadarComputationStrategy weightedRadarComputationStrategy = mock(WeightedRadarComputationStrategy.class);
        InterviewRecordMapper mapper = mock(InterviewRecordMapper.class);
        InterviewRecordServiceImpl service = new InterviewRecordServiceImpl(
                cacheService,
                ownershipService,
                sessionService,
                questionService,
                finalizeLockService,
                runtimeSnapshotService,
                runtimeRehydrateService,
                dimensionScoreStrategy,
                weightedRadarComputationStrategy
        );
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        RLock finalizeLock = mock(RLock.class);
        when(finalizeLockService.acquire("interview-session-1")).thenReturn(finalizeLock);

        InterviewSession readySession = new InterviewSession();
        readySession.setSessionId("interview-session-1");
        readySession.setUserId(1001L);
        readySession.setStatus(InterviewSessionStatus.READY.name());
        readySession.setInterviewerAgentId(9L);
        readySession.setInterviewType("backend");
        readySession.setResumeFileUrl("https://example.com/resume.pdf");
        readySession.setCreateTime(new Date(System.currentTimeMillis() - 120_000));
        readySession.setStartTime(new Date(System.currentTimeMillis() - 60_000));
        readySession.setEndTime(new Date());

        InterviewSession finishedSession = new InterviewSession();
        finishedSession.setSessionId("interview-session-1");
        finishedSession.setUserId(1001L);
        finishedSession.setStatus(InterviewSessionStatus.FINISHED.name());
        finishedSession.setInterviewerAgentId(9L);
        finishedSession.setInterviewType("backend");
        finishedSession.setResumeFileUrl("https://example.com/resume.pdf");
        finishedSession.setCreateTime(readySession.getCreateTime());
        finishedSession.setStartTime(readySession.getStartTime());
        finishedSession.setEndTime(readySession.getEndTime());

        when(ownershipService.requireOwnedSession("interview-session-1", 1001L))
                .thenReturn(readySession, readySession, finishedSession);

        when(cacheService.getSessionTotalScore("interview-session-1")).thenReturn(92);
        when(cacheService.getSessionInterviewSuggestions("interview-session-1")).thenReturn(Map.of("1", "Structured answer"));
        when(cacheService.getSessionResumeScore("interview-session-1")).thenReturn(86);
        when(cacheService.getSessionInterviewQuestions("interview-session-1")).thenReturn(Map.of("1", "Describe JVM tuning"));
        when(cacheService.getSessionInterviewDirection("interview-session-1")).thenReturn("backend");
        when(questionService.getBySessionId("interview-session-1")).thenReturn(null);
        when(cacheService.getInterviewTurns("interview-session-1")).thenReturn(List.of(
                InterviewTurnLog.builder()
                        .questionNumber("1")
                        .score(88)
                        .feedback("Answer structure is clear and supported by a concrete project example.")
                        .build()
        ));
        when(runtimeSnapshotService.loadPersistedTurns("interview-session-1")).thenReturn(List.of(
                InterviewTurnLog.builder()
                        .questionNumber("1")
                        .score(88)
                        .feedback("Answer structure is clear and supported by a concrete project example.")
                        .build()
        ));
        DimensionScoreResult dimensionScore = new DimensionScoreResult();
        dimensionScore.setContentScore(92);
        dimensionScore.setLogicScore(83);
        dimensionScore.setProfessionalScore(78);
        dimensionScore.setExpressionScore(83);
        dimensionScore.setAdaptabilityScore(73);
        dimensionScore.setTimeControlScore(70);
        dimensionScore.setEtiquetteScore(70);
        dimensionScore.setCompositeScore(86);
        when(dimensionScoreStrategy.compute(anyInt(), any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(dimensionScore);
        InterviewRecordDO existingRecord = new InterviewRecordDO();
        existingRecord.setId(1L);
        existingRecord.setUserId(1001L);
        existingRecord.setSessionId("interview-session-1");
        existingRecord.setCreateTime(new Date(System.currentTimeMillis() - 30_000));
        when(mapper.selectOne(any())).thenReturn(null, existingRecord);
        when(mapper.insert(any(InterviewRecordDO.class))).thenReturn(1);
        when(mapper.updateById(any(InterviewRecordDO.class))).thenReturn(1);

        service.saveInterviewRecordFromRedis("interview-session-1", 1001L);

        InOrder inOrder = inOrder(mapper, sessionService);
        inOrder.verify(mapper).selectOne(any());
        inOrder.verify(mapper).insert(any(InterviewRecordDO.class));
        inOrder.verify(sessionService).finishSession("interview-session-1", 1001L);
        inOrder.verify(mapper).selectOne(any());
        inOrder.verify(mapper).updateById(any(InterviewRecordDO.class));
        verify(ownershipService, times(3)).requireOwnedSession("interview-session-1", 1001L);
        ArgumentCaptor<InterviewRecordDO> recordCaptor = ArgumentCaptor.forClass(InterviewRecordDO.class);
        verify(mapper).updateById(recordCaptor.capture());

        InterviewRecordDO record = recordCaptor.getValue();
        assertEquals("interview-session-1", record.getSessionId());
        assertEquals(1001L, record.getUserId());
        assertEquals(InterviewSessionStatus.FINISHED.name(), record.getInterviewStatus());
        assertEquals(9L, record.getInterviewerAgentId());
        assertEquals(92, record.getInterviewScore());
        assertEquals(86, record.getResumeScore());
        assertTrue(record.getSessionSnapshotJson().contains("\"sessionStatus\":\"FINISHED\""));
        assertTrue(record.getSessionSnapshotJson().contains("\"reviewFeedback\""));
    }
}
