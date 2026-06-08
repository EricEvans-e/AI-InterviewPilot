package com.interviewpilot.interview.flow.report;

import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.interview.application.InterviewSessionOwnershipService;
import com.interviewpilot.interview.application.finalize.InterviewFinalizeLockService;
import com.interviewpilot.interview.application.runtime.InterviewSessionRuntimeRehydrateService;
import com.interviewpilot.interview.application.runtime.InterviewSessionRuntimeSnapshotService;
import com.interviewpilot.interview.application.strategy.DimensionScoreResult;
import com.interviewpilot.interview.application.strategy.DimensionScoreStrategy;
import com.interviewpilot.interview.application.strategy.WeightedRadarComputationStrategy;
import com.interviewpilot.interview.dao.entity.InterviewQuestion;
import com.interviewpilot.interview.dao.entity.InterviewRecordDO;
import com.interviewpilot.interview.dao.entity.InterviewSession;
import com.interviewpilot.interview.dao.mapper.InterviewRecordMapper;
import com.interviewpilot.interview.service.InterviewQuestionCacheService;
import com.interviewpilot.interview.service.InterviewQuestionService;
import com.interviewpilot.interview.service.InterviewSessionService;
import com.interviewpilot.interview.service.model.InterviewSessionStatus;
import com.interviewpilot.interview.testing.PressureTestReportUtil;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewRecordFinalizeConcurrencyTest {

    @Test
    void shouldAllowOnlySingleFinalizeWriterUnderConcurrentRequests() throws Exception {
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

        InterviewSession session = new InterviewSession();
        session.setSessionId("session-finalize-1");
        session.setUserId(3003L);
        session.setStatus(InterviewSessionStatus.FINISHED.name());
        session.setInterviewType("backend");
        session.setResumeFileUrl("https://mock/resume.pdf");
        session.setInterviewerAgentId(77L);
        session.setStartTime(new Date(System.currentTimeMillis() - 60_000));
        session.setCreateTime(new Date(System.currentTimeMillis() - 120_000));
        when(ownershipService.requireOwnedSession("session-finalize-1", 3003L))
                .thenAnswer(invocation -> {
                    Thread.sleep(80L);
                    return session;
                });

        InterviewQuestion question = new InterviewQuestion();
        question.setSessionId("session-finalize-1");
        question.setInterviewType("backend");
        question.setResumeScore(83);
        when(questionService.getBySessionId("session-finalize-1")).thenReturn(question);

        when(cacheService.getSessionTotalScore("session-finalize-1")).thenReturn(91);
        when(cacheService.getSessionInterviewSuggestions("session-finalize-1")).thenReturn(Map.of("1", "mock-tip"));
        when(cacheService.getSessionResumeScore("session-finalize-1")).thenReturn(83);
        when(cacheService.getSessionInterviewQuestions("session-finalize-1")).thenReturn(Map.of("1", "mock-question"));
        when(cacheService.getSessionInterviewDirection("session-finalize-1")).thenReturn("backend");
        when(cacheService.getInterviewTurns("session-finalize-1")).thenReturn(List.of());
        when(runtimeSnapshotService.loadPersistedTurns("session-finalize-1")).thenReturn(List.of());
        when(cacheService.getRadarChartData("session-finalize-1")).thenReturn(null);
        when(cacheService.getInterviewFlow("session-finalize-1")).thenReturn(null);
        DimensionScoreResult dimensionScore = new DimensionScoreResult();
        dimensionScore.setContentScore(91);
        dimensionScore.setLogicScore(82);
        dimensionScore.setProfessionalScore(77);
        dimensionScore.setExpressionScore(82);
        dimensionScore.setAdaptabilityScore(72);
        dimensionScore.setTimeControlScore(70);
        dimensionScore.setEtiquetteScore(70);
        dimensionScore.setCompositeScore(84);
        when(dimensionScoreStrategy.compute(anyInt(), any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(dimensionScore);

        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(InterviewRecordDO.class))).thenReturn(1);

        AtomicBoolean winnerChosen = new AtomicBoolean(false);
        RLock lock = mock(RLock.class);
        when(finalizeLockService.acquire("session-finalize-1"))
                .thenAnswer(invocation -> winnerChosen.compareAndSet(false, true) ? lock : null);

        int concurrency = 24;
        ExecutorService pool = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<TaskResult>> futures = new ArrayList<>();

        for (int i = 0; i < concurrency; i++) {
            futures.add(pool.submit(() -> {
                start.await(2, TimeUnit.SECONDS);
                long begin = System.nanoTime();
                try {
                    service.saveInterviewRecordFromRedis("session-finalize-1", 3003L);
                    return new TaskResult(true, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin));
                } catch (ClientException ex) {
                    return new TaskResult(false, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin));
                }
            }));
        }

        start.countDown();

        int successCount = 0;
        int rejectedCount = 0;
        List<Long> latencies = new ArrayList<>();
        for (Future<TaskResult> future : futures) {
            TaskResult result = future.get(8, TimeUnit.SECONDS);
            latencies.add(result.latencyMs());
            if (result.success()) {
                successCount++;
            } else {
                rejectedCount++;
            }
        }
        pool.shutdownNow();
        PressureTestReportUtil.printSummary(
                "record.finalize.concurrent-single-writer",
                concurrency,
                successCount,
                rejectedCount,
                latencies,
                Map.of("sessionId", "session-finalize-1")
        );

        assertEquals(1, successCount);
        assertEquals(concurrency - 1, rejectedCount);
        assertTrue(winnerChosen.get());
        verify(mapper, times(1)).insert(any(InterviewRecordDO.class));
        verify(finalizeLockService, times(1)).release(lock);
        verify(sessionService, times(0)).finishSession("session-finalize-1", 3003L);
    }

    private record TaskResult(boolean success, long latencyMs) {
    }
}
