package com.interviewpilot.interview.service.impl;

import com.interviewpilot.interview.application.InterviewSessionOwnershipService;
import com.interviewpilot.interview.application.finalize.InterviewFinalizeLockService;
import com.interviewpilot.interview.application.runtime.InterviewSessionRuntimeRehydrateService;
import com.interviewpilot.interview.application.runtime.InterviewSessionRuntimeSnapshotService;
import com.interviewpilot.interview.application.strategy.DimensionScoreResult;
import com.interviewpilot.interview.application.strategy.DimensionScoreStrategy;
import com.interviewpilot.interview.application.strategy.WeightedRadarComputationStrategy;
import com.interviewpilot.common.config.storage.ApplicationStorageProperties;
import com.interviewpilot.interview.api.io.req.DemeanorScoreDTO;
import com.interviewpilot.interview.api.io.resp.InterviewRecordRespDTO;
import com.interviewpilot.interview.api.io.resp.InterviewReviewFeedbackRespDTO;
import com.interviewpilot.interview.dao.entity.InterviewRecordDO;
import com.interviewpilot.interview.dao.entity.InterviewSession;
import com.interviewpilot.interview.dao.mapper.InterviewRecordMapper;
import com.interviewpilot.interview.flow.report.InterviewReportAiReviewService;
import com.interviewpilot.interview.flow.report.InterviewRecordServiceImpl;
import com.interviewpilot.interview.flow.report.InterviewReferenceAnswerService;
import com.interviewpilot.interview.service.InterviewQuestionCacheService;
import com.interviewpilot.interview.service.InterviewQuestionService;
import com.interviewpilot.interview.service.InterviewSessionService;
import com.interviewpilot.interview.service.model.InterviewTurnLog;
import com.interviewpilot.interview.service.model.InterviewSessionStatus;
import com.interviewpilot.teacher.dao.mapper.TeacherReviewMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.redisson.api.RLock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        InterviewReportAiReviewService reportAiReviewService = mock(InterviewReportAiReviewService.class);
        InterviewReferenceAnswerService referenceAnswerService = mock(InterviewReferenceAnswerService.class);
        InterviewRecordMapper mapper = mock(InterviewRecordMapper.class);
        when(referenceAnswerService.attachAvailableReferenceAnswers(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        InterviewRecordServiceImpl service = new InterviewRecordServiceImpl(
                cacheService,
                ownershipService,
                sessionService,
                questionService,
                finalizeLockService,
                runtimeSnapshotService,
                runtimeRehydrateService,
                dimensionScoreStrategy,
                weightedRadarComputationStrategy,
                reportAiReviewService,
                referenceAnswerService,
                storageProperties(),
                mock(TeacherReviewMapper.class)
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
        when(cacheService.getSessionDemeanorScore("interview-session-1")).thenReturn(82);
        DemeanorScoreDTO demeanorDetails = new DemeanorScoreDTO();
        demeanorDetails.setPanicLevel(20);
        demeanorDetails.setSeriousnessLevel(80);
        demeanorDetails.setEmoticonHandling(75);
        demeanorDetails.setCompositeScore(82);
        when(cacheService.getSessionDemeanorScoreDetails("interview-session-1")).thenReturn(demeanorDetails);
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
        verify(reportAiReviewService, never()).generateReviewFeedback(any(), any(), any(), any(), any());
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
        assertTrue(record.getSessionSnapshotJson().contains("\"demeanorScore\":82"));
        assertTrue(record.getSessionSnapshotJson().contains("\"demeanorDetails\""));
        assertTrue(record.getSessionSnapshotJson().contains("\"panicLevel\":20"));

        when(mapper.selectOne(any())).thenReturn(record);
        InterviewRecordRespDTO report = service.getBySessionId("interview-session-1", 1001L);
        assertNotNull(report);
        assertNotNull(report.getReviewFeedback());
        assertNotNull(report.getReviewFeedback().getOverallComment());
        assertTrue(report.getReviewFeedback().getNextActions().contains("Structured answer"));
        assertNotNull(report.getRadarChart());
        assertEquals(92, report.getRadarChart().getContentScore());
        assertEquals(70, report.getRadarChart().getEtiquetteScore());
        assertEquals("Answer structure is clear and supported by a concrete project example.",
                report.getPlaybackItems().get(0).getFeedback());
    }

    @Test
    void shouldUseChineseRuleBasedReviewWhenAiReviewIsMissing() {
        InterviewQuestionCacheService cacheService = mock(InterviewQuestionCacheService.class);
        InterviewSessionOwnershipService ownershipService = mock(InterviewSessionOwnershipService.class);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        InterviewQuestionService questionService = mock(InterviewQuestionService.class);
        InterviewFinalizeLockService finalizeLockService = mock(InterviewFinalizeLockService.class);
        InterviewSessionRuntimeSnapshotService runtimeSnapshotService = mock(InterviewSessionRuntimeSnapshotService.class);
        InterviewSessionRuntimeRehydrateService runtimeRehydrateService = mock(InterviewSessionRuntimeRehydrateService.class);
        DimensionScoreStrategy dimensionScoreStrategy = mock(DimensionScoreStrategy.class);
        WeightedRadarComputationStrategy weightedRadarComputationStrategy = mock(WeightedRadarComputationStrategy.class);
        InterviewReportAiReviewService reportAiReviewService = mock(InterviewReportAiReviewService.class);
        InterviewReferenceAnswerService referenceAnswerService = mock(InterviewReferenceAnswerService.class);
        InterviewRecordMapper mapper = mock(InterviewRecordMapper.class);
        when(referenceAnswerService.attachAvailableReferenceAnswers(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        InterviewRecordServiceImpl service = new InterviewRecordServiceImpl(
                cacheService,
                ownershipService,
                sessionService,
                questionService,
                finalizeLockService,
                runtimeSnapshotService,
                runtimeRehydrateService,
                dimensionScoreStrategy,
                weightedRadarComputationStrategy,
                reportAiReviewService,
                referenceAnswerService,
                storageProperties(),
                mock(TeacherReviewMapper.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        InterviewRecordDO record = new InterviewRecordDO();
        record.setId(2L);
        record.setUserId(1002L);
        record.setSessionId("session-rule-review");
        record.setInterviewSuggestions("补充量化指标; 复盘关键技术取舍");
        record.setContentScore(58);
        record.setLogicScore(52);
        record.setProfessionalScore(50);
        record.setExpressionScore(54);
        record.setAdaptabilityScore(48);
        record.setTimeControlScore(70);
        record.setEtiquetteScore(82);
        record.setCompositeScore(60);
        record.setSessionSnapshotJson("""
                {
                  "sessionId":"session-rule-review",
                  "radar":{
                    "resumeScore":92,
                    "interviewPerformance":42,
                    "demeanorEvaluation":82,
                    "professionalSkills":50,
                    "potentialIndex":56
                  },
                  "turns":[
                    {
                      "questionNumber":"1",
                      "questionContent":"请说明文本风控系统的算法设计。",
                      "answerContent":"你好",
                      "score":10,
                      "feedback":"回答没有覆盖题目要求，需要补充算法路径、评价指标和工程挑战。"
                    }
                  ]
                }
                """);
        when(mapper.selectOne(any())).thenReturn(record);

        InterviewRecordRespDTO report = service.getBySessionId("session-rule-review", 1002L);

        assertNotNull(report.getReviewFeedback());
        assertTrue(report.getReviewFeedback().getOverallComment().contains("整体表现"));
        assertFalse(report.getReviewFeedback().getOverallComment().contains("Overall performance"));
        assertTrue(report.getReviewFeedback().getHighlights().stream()
                .allMatch(line -> !line.contains("Your resume")));
        assertTrue(report.getReviewFeedback().getImprovementTips().stream()
                .anyMatch(line -> line.contains("算法路径") || line.contains("量化")));
        assertEquals(List.of("补充量化指标", "复盘关键技术取舍",
                        "回答没有覆盖题目要求，需要补充算法路径、评价指标和工程挑战."),
                report.getReviewFeedback().getNextActions());
        verify(referenceAnswerService, never()).attachAvailableReferenceAnswers(any(), any());
        verify(referenceAnswerService, never()).generateMissingReferenceAnswers(any(), any(), any());
    }

    @Test
    void shouldGenerateReferenceAnswersOnlyWhenRequested() {
        InterviewQuestionCacheService cacheService = mock(InterviewQuestionCacheService.class);
        InterviewSessionOwnershipService ownershipService = mock(InterviewSessionOwnershipService.class);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        InterviewQuestionService questionService = mock(InterviewQuestionService.class);
        InterviewFinalizeLockService finalizeLockService = mock(InterviewFinalizeLockService.class);
        InterviewSessionRuntimeSnapshotService runtimeSnapshotService = mock(InterviewSessionRuntimeSnapshotService.class);
        InterviewSessionRuntimeRehydrateService runtimeRehydrateService = mock(InterviewSessionRuntimeRehydrateService.class);
        DimensionScoreStrategy dimensionScoreStrategy = mock(DimensionScoreStrategy.class);
        WeightedRadarComputationStrategy weightedRadarComputationStrategy = mock(WeightedRadarComputationStrategy.class);
        InterviewReportAiReviewService reportAiReviewService = mock(InterviewReportAiReviewService.class);
        InterviewReferenceAnswerService referenceAnswerService = mock(InterviewReferenceAnswerService.class);
        InterviewRecordMapper mapper = mock(InterviewRecordMapper.class);
        when(referenceAnswerService.attachAvailableReferenceAnswers(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(referenceAnswerService.generateMissingReferenceAnswers(any(), any(), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<InterviewTurnLog> turns = invocation.getArgument(2, List.class);
                    turns.get(0).setReferenceAnswer("参考回答：先说明方案，再说明指标和取舍。");
                    return turns;
                });
        InterviewRecordServiceImpl service = new InterviewRecordServiceImpl(
                cacheService,
                ownershipService,
                sessionService,
                questionService,
                finalizeLockService,
                runtimeSnapshotService,
                runtimeRehydrateService,
                dimensionScoreStrategy,
                weightedRadarComputationStrategy,
                reportAiReviewService,
                referenceAnswerService,
                storageProperties(),
                mock(TeacherReviewMapper.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        InterviewRecordDO record = new InterviewRecordDO();
        record.setId(3L);
        record.setUserId(1003L);
        record.setSessionId("session-reference-manual");
        record.setInterviewDirection("AI/机器学习工程师");
        record.setSessionSnapshotJson("""
                {
                  "sessionId":"session-reference-manual",
                  "turns":[
                    {
                      "questionNumber":"1",
                      "questionContent":"请说明 TF-IDF 与语义匹配如何协同。",
                      "answerContent":"我用了 TF-IDF 和语义模型。",
                      "score":70,
                      "feedback":"需要补充融合细节。"
                    }
                  ]
                }
                """);
        when(mapper.selectOne(any())).thenReturn(record);
        when(mapper.updateById(any(InterviewRecordDO.class))).thenReturn(1);

        InterviewRecordRespDTO report = service.generateReferenceAnswers("session-reference-manual", 1003L);

        assertNotNull(report);
        assertEquals("参考回答：先说明方案，再说明指标和取舍。",
                report.getPlaybackItems().get(0).getReferenceAnswer());
        verify(referenceAnswerService).generateMissingReferenceAnswers(
                eq("session-reference-manual"),
                eq("AI/机器学习工程师"),
                any()
        );
        verify(mapper).updateById(any(InterviewRecordDO.class));
    }

    @Test
    void shouldExposePresetReferenceAnswersOnInitialReportLoad() {
        InterviewQuestionCacheService cacheService = mock(InterviewQuestionCacheService.class);
        InterviewSessionOwnershipService ownershipService = mock(InterviewSessionOwnershipService.class);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        InterviewQuestionService questionService = mock(InterviewQuestionService.class);
        InterviewFinalizeLockService finalizeLockService = mock(InterviewFinalizeLockService.class);
        InterviewSessionRuntimeSnapshotService runtimeSnapshotService = mock(InterviewSessionRuntimeSnapshotService.class);
        InterviewSessionRuntimeRehydrateService runtimeRehydrateService = mock(InterviewSessionRuntimeRehydrateService.class);
        DimensionScoreStrategy dimensionScoreStrategy = mock(DimensionScoreStrategy.class);
        WeightedRadarComputationStrategy weightedRadarComputationStrategy = mock(WeightedRadarComputationStrategy.class);
        InterviewReportAiReviewService reportAiReviewService = mock(InterviewReportAiReviewService.class);
        InterviewReferenceAnswerService referenceAnswerService = mock(InterviewReferenceAnswerService.class);
        InterviewRecordMapper mapper = mock(InterviewRecordMapper.class);
        when(referenceAnswerService.attachAvailableReferenceAnswers(any(), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<InterviewTurnLog> turns = invocation.getArgument(1, List.class);
                    turns.get(0).setReferenceAnswer("题库预设参考答案");
                    return turns;
                });

        InterviewRecordServiceImpl service = new InterviewRecordServiceImpl(
                cacheService,
                ownershipService,
                sessionService,
                questionService,
                finalizeLockService,
                runtimeSnapshotService,
                runtimeRehydrateService,
                dimensionScoreStrategy,
                weightedRadarComputationStrategy,
                reportAiReviewService,
                referenceAnswerService,
                storageProperties(),
                mock(TeacherReviewMapper.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        InterviewRecordDO record = new InterviewRecordDO();
        record.setId(31L);
        record.setUserId(1031L);
        record.setSessionId("session-preset-reference");
        record.setSessionSnapshotJson("""
                {
                  "sessionId":"session-preset-reference",
                  "turns":[
                    {
                      "questionNumber":"1",
                      "questionContent":"为什么选择这个专业？",
                      "answerContent":"因为前景不错",
                      "score":60,
                      "feedback":"回答可以更具体"
                    }
                  ]
                }
                """);
        when(mapper.selectOne(any())).thenReturn(record);

        InterviewRecordRespDTO report = service.getBySessionId("session-preset-reference", 1031L);

        assertNotNull(report);
        assertEquals("题库预设参考答案", report.getPlaybackItems().get(0).getReferenceAnswer());
        verify(referenceAnswerService).attachAvailableReferenceAnswers(eq("session-preset-reference"), any());
        verify(referenceAnswerService, never()).generateMissingReferenceAnswers(any(), any(), any());
    }

    @Test
    void shouldGenerateAiReviewFeedbackOnlyWhenRequested() {
        InterviewQuestionCacheService cacheService = mock(InterviewQuestionCacheService.class);
        InterviewSessionOwnershipService ownershipService = mock(InterviewSessionOwnershipService.class);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        InterviewQuestionService questionService = mock(InterviewQuestionService.class);
        InterviewFinalizeLockService finalizeLockService = mock(InterviewFinalizeLockService.class);
        InterviewSessionRuntimeSnapshotService runtimeSnapshotService = mock(InterviewSessionRuntimeSnapshotService.class);
        InterviewSessionRuntimeRehydrateService runtimeRehydrateService = mock(InterviewSessionRuntimeRehydrateService.class);
        DimensionScoreStrategy dimensionScoreStrategy = mock(DimensionScoreStrategy.class);
        WeightedRadarComputationStrategy weightedRadarComputationStrategy = mock(WeightedRadarComputationStrategy.class);
        InterviewReportAiReviewService reportAiReviewService = mock(InterviewReportAiReviewService.class);
        InterviewReferenceAnswerService referenceAnswerService = mock(InterviewReferenceAnswerService.class);
        InterviewRecordMapper mapper = mock(InterviewRecordMapper.class);
        when(referenceAnswerService.attachAvailableReferenceAnswers(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        InterviewReviewFeedbackRespDTO aiFeedback = new InterviewReviewFeedbackRespDTO();
        aiFeedback.setOverallComment("AI 总结：回答覆盖了系统主链路，但量化指标解释仍不充分。");
        aiFeedback.setHighlights(List.of("能够说明系统流程和核心模块。"));
        aiFeedback.setImprovementTips(List.of("补充评估指标、异常处理和性能权衡。"));
        aiFeedback.setNextActions(List.of("下次按背景-方案-指标-结果组织回答。"));
        when(reportAiReviewService.generateReviewFeedback(any(), any(), any(), any(), any()))
                .thenReturn(aiFeedback);

        InterviewRecordServiceImpl service = new InterviewRecordServiceImpl(
                cacheService,
                ownershipService,
                sessionService,
                questionService,
                finalizeLockService,
                runtimeSnapshotService,
                runtimeRehydrateService,
                dimensionScoreStrategy,
                weightedRadarComputationStrategy,
                reportAiReviewService,
                referenceAnswerService,
                storageProperties(),
                mock(TeacherReviewMapper.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        InterviewRecordDO record = new InterviewRecordDO();
        record.setId(4L);
        record.setUserId(1004L);
        record.setSessionId("session-manual-review");
        record.setInterviewDirection("NLP 算法工程师");
        record.setInterviewSuggestions("补充量化指标; 说明技术取舍");
        record.setResumeScore(85);
        record.setInterviewScore(43);
        record.setContentScore(0);
        record.setLogicScore(0);
        record.setProfessionalScore(0);
        record.setExpressionScore(0);
        record.setAdaptabilityScore(0);
        record.setTimeControlScore(70);
        record.setEtiquetteScore(0);
        record.setCompositeScore(43);
        record.setSessionSnapshotJson("""
                {
                  "sessionId":"session-manual-review",
                  "radar":{
                    "resumeScore":85,
                    "interviewPerformance":0,
                    "demeanorEvaluation":0,
                    "professionalSkills":26,
                    "potentialIndex":43
                  },
                  "turns":[
                    {
                      "questionNumber":"1",
                      "questionContent":"请说明文本风控系统的实现路径。",
                      "answerContent":"你好",
                      "score":0,
                      "feedback":"回答没有覆盖题目要求，需要补充算法路径和评估指标。"
                    }
                  ]
                }
                """);
        when(mapper.selectOne(any())).thenReturn(record);
        when(mapper.updateById(any(InterviewRecordDO.class))).thenReturn(1);

        InterviewRecordRespDTO report = service.generateAiReviewFeedback("session-manual-review", 1004L);

        assertNotNull(report);
        assertNotNull(report.getReviewFeedback());
        assertEquals("AI 总结：回答覆盖了系统主链路，但量化指标解释仍不充分。",
                report.getReviewFeedback().getOverallComment());
        assertEquals(List.of("能够说明系统流程和核心模块。"),
                report.getReviewFeedback().getHighlights());
        assertEquals(List.of("补充评估指标、异常处理和性能权衡。"),
                report.getReviewFeedback().getImprovementTips());
        assertEquals(List.of("下次按背景-方案-指标-结果组织回答。"),
                report.getReviewFeedback().getNextActions());
        verify(reportAiReviewService).generateReviewFeedback(
                eq("session-manual-review"),
                eq("NLP 算法工程师"),
                any(),
                any(),
                eq("补充量化指标; 说明技术取舍")
        );
        verify(mapper).updateById(any(InterviewRecordDO.class));
    }

    @Test
    void shouldDeleteInterviewRecordAndSessionScopedDataForTeacher() {
        InterviewQuestionCacheService cacheService = mock(InterviewQuestionCacheService.class);
        InterviewSessionOwnershipService ownershipService = mock(InterviewSessionOwnershipService.class);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        InterviewQuestionService questionService = mock(InterviewQuestionService.class);
        InterviewFinalizeLockService finalizeLockService = mock(InterviewFinalizeLockService.class);
        InterviewSessionRuntimeSnapshotService runtimeSnapshotService = mock(InterviewSessionRuntimeSnapshotService.class);
        InterviewSessionRuntimeRehydrateService runtimeRehydrateService = mock(InterviewSessionRuntimeRehydrateService.class);
        DimensionScoreStrategy dimensionScoreStrategy = mock(DimensionScoreStrategy.class);
        WeightedRadarComputationStrategy weightedRadarComputationStrategy = mock(WeightedRadarComputationStrategy.class);
        InterviewReportAiReviewService reportAiReviewService = mock(InterviewReportAiReviewService.class);
        InterviewReferenceAnswerService referenceAnswerService = mock(InterviewReferenceAnswerService.class);
        InterviewRecordMapper mapper = mock(InterviewRecordMapper.class);
        TeacherReviewMapper teacherReviewMapper = mock(TeacherReviewMapper.class);

        InterviewRecordServiceImpl service = new InterviewRecordServiceImpl(
                cacheService,
                ownershipService,
                sessionService,
                questionService,
                finalizeLockService,
                runtimeSnapshotService,
                runtimeRehydrateService,
                dimensionScoreStrategy,
                weightedRadarComputationStrategy,
                reportAiReviewService,
                referenceAnswerService,
                storageProperties(),
                teacherReviewMapper
        );
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        InterviewRecordDO record = new InterviewRecordDO();
        record.setId(5L);
        record.setSessionId("session-delete-teacher");
        record.setUserId(1005L);
        record.setRecordingUrl("/recordings/session-delete-teacher.webm");
        when(mapper.selectOne(any())).thenReturn(record);
        when(mapper.delete(any())).thenReturn(1);

        service.deleteRecordBySessionIdForTeacher("session-delete-teacher");

        verify(mapper).delete(any());
        verify(questionService).deleteBySessionId("session-delete-teacher");
        verify(sessionService).deleteBySessionId("session-delete-teacher");
        verify(runtimeSnapshotService).deleteBySessionId("session-delete-teacher");
        verify(cacheService).clearSessionRuntime("session-delete-teacher");
        verify(teacherReviewMapper).delete(any());
    }

    private ApplicationStorageProperties storageProperties() {
        ApplicationStorageProperties properties = new ApplicationStorageProperties();
        String tmpDir = System.getProperty("java.io.tmpdir");
        properties.setBaseDir(tmpDir);
        properties.setRecordingDir(tmpDir);
        return properties;
    }
}
