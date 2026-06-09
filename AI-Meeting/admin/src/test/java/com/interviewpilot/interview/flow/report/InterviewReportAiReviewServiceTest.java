package com.interviewpilot.interview.flow.report;

import com.interviewpilot.agent.application.BusinessAgentResolver;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.interview.api.io.resp.InterviewReviewFeedbackRespDTO;
import com.interviewpilot.interview.api.io.resp.RadarChartDTO;
import com.interviewpilot.interview.application.guard.core.InterviewAiGuardStage;
import com.interviewpilot.interview.service.model.InterviewTurnLog;
import com.interviewpilot.interview.shared.InterviewAiInvoker;
import com.interviewpilot.interview.shared.InterviewResponseParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewReportAiReviewServiceTest {

    @Test
    void shouldBuildStructuredReviewFeedbackFromAiResponse() throws Exception {
        BusinessAgentResolver resolver = mock(BusinessAgentResolver.class);
        InterviewAiInvoker invoker = mock(InterviewAiInvoker.class);
        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setId(9L);
        agent.setAiProvider("openai");
        agent.setApiKey("tp-test-key");
        agent.setApiSecret("mimo-v2.5");
        agent.setApiFlowId("https://token-plan-cn.xiaomimimo.com/v1");
        when(resolver.resolveRequired(BusinessAgentScene.INTERVIEW_ANSWER_EVALUATION)).thenReturn(agent);
        when(invoker.buildSingleFlightKey(
                eq(InterviewAiGuardStage.INTERVIEW_REPORT_REVIEW),
                eq("session-ai-report"),
                anyString()
        )).thenReturn("report-key");
        when(invoker.callAiSync(
                anyString(),
                eq("session-ai-report"),
                eq(agent),
                eq(InterviewAiGuardStage.INTERVIEW_REPORT_REVIEW),
                eq("report-key")
        )).thenReturn("""
                {"choices":[{"message":{"content":"{\\"overallComment\\":\\"AI总体评价：回答覆盖核心链路，但量化指标不足。\\",\\"highlights\\":[\\"能说明系统目标和基础流程。\\"],\\"improvementTips\\":[\\"补充模型评估指标和异常处理细节。\\"],\\"nextActions\\":[\\"下一次回答按背景-方法-指标-复盘组织。\\"]}"}}]}
                """);

        InterviewReportAiReviewService service = new InterviewReportAiReviewService(
                resolver,
                invoker,
                new InterviewResponseParser()
        );

        RadarChartDTO radar = new RadarChartDTO();
        radar.setResumeScore(92);
        radar.setInterviewPerformance(66);
        radar.setDemeanorEvaluation(82);
        radar.setProfessionalSkills(70);
        radar.setPotentialIndex(73);

        InterviewReviewFeedbackRespDTO feedback = service.generateReviewFeedback(
                "session-ai-report",
                "AI/机器学习工程师",
                List.of(InterviewTurnLog.builder()
                        .questionNumber("1")
                        .questionContent("请说明文本风控系统的算法设计。")
                        .answerContent("使用 TF-IDF 和语义匹配。")
                        .score(66)
                        .feedback("回答有方向，但缺少指标与实现细节。")
                        .build()),
                radar,
                "补充指标; 强化项目复盘"
        );

        assertNotNull(feedback);
        assertEquals("AI总体评价：回答覆盖核心链路，但量化指标不足。", feedback.getOverallComment());
        assertEquals(List.of("能说明系统目标和基础流程。"), feedback.getHighlights());
        assertEquals(List.of("补充模型评估指标和异常处理细节。"), feedback.getImprovementTips());
        assertEquals(List.of("下一次回答按背景-方法-指标-复盘组织。"), feedback.getNextActions());
        verify(resolver).resolveRequired(BusinessAgentScene.INTERVIEW_ANSWER_EVALUATION);
    }
}
