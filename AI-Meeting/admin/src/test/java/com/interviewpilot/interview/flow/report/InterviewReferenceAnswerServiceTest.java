package com.interviewpilot.interview.flow.report;

import com.interviewpilot.agent.application.BusinessAgentResolver;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.interview.application.guard.core.InterviewAiGuardStage;
import com.interviewpilot.interview.dao.entity.InterviewSessionQuestionDO;
import com.interviewpilot.interview.dao.mapper.InterviewSessionQuestionMapper;
import com.interviewpilot.interview.service.model.InterviewTurnLog;
import com.interviewpilot.interview.shared.InterviewAiInvoker;
import com.interviewpilot.interview.shared.InterviewResponseParser;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import com.interviewpilot.questionbank.service.QuestionBankService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewReferenceAnswerServiceTest {

    @Test
    void shouldUsePresetQuestionBankReferenceAnswerBeforeAiGeneration() throws Exception {
        BusinessAgentResolver resolver = mock(BusinessAgentResolver.class);
        InterviewAiInvoker invoker = mock(InterviewAiInvoker.class);
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        InterviewSessionQuestionMapper sessionQuestionMapper = mock(InterviewSessionQuestionMapper.class);

        InterviewSessionQuestionDO link = new InterviewSessionQuestionDO();
        link.setSeqIndex(0);
        link.setQuestionId(101L);
        when(sessionQuestionMapper.selectList(any())).thenReturn(List.of(link));
        QuestionDO question = new QuestionDO();
        question.setId(101L);
        question.setReferenceAnswer("题库标准答案：先给结论，再说明关键步骤、评价指标和风险控制。");
        when(questionBankService.getById(101L)).thenReturn(question);

        InterviewReferenceAnswerService service = new InterviewReferenceAnswerService(
                resolver,
                invoker,
                new InterviewResponseParser(),
                questionBankService,
                sessionQuestionMapper
        );

        List<InterviewTurnLog> turns = service.attachReferenceAnswers(
                "session-bank",
                "AI/机器学习工程师",
                List.of(InterviewTurnLog.builder()
                        .questionNumber("1")
                        .questionContent("请说明文本风控系统的算法设计。")
                        .answerContent("使用 TF-IDF 和语义匹配。")
                        .feedback("回答有方向，但缺少细节。")
                        .score(66)
                        .build())
        );

        assertEquals("题库标准答案：先给结论，再说明关键步骤、评价指标和风险控制。",
                turns.get(0).getReferenceAnswer());
        verify(invoker, never()).callAiSync(anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void shouldGenerateMissingReferenceAnswerWithInterviewContext() throws Exception {
        BusinessAgentResolver resolver = mock(BusinessAgentResolver.class);
        InterviewAiInvoker invoker = mock(InterviewAiInvoker.class);
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        InterviewSessionQuestionMapper sessionQuestionMapper = mock(InterviewSessionQuestionMapper.class);
        when(sessionQuestionMapper.selectList(any())).thenReturn(List.of());

        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setId(9L);
        agent.setAiProvider("openai");
        agent.setApiKey("tp-test-key");
        agent.setApiSecret("mimo-v2.5");
        agent.setApiFlowId("https://token-plan-cn.xiaomimimo.com/v1");
        when(resolver.resolveRequired(BusinessAgentScene.INTERVIEW_ANSWER_EVALUATION)).thenReturn(agent);
        when(invoker.buildSingleFlightKey(
                eq(InterviewAiGuardStage.INTERVIEW_REFERENCE_ANSWER),
                eq("session-resume"),
                anyString()
        )).thenReturn("reference-answer-key");
        when(invoker.callAiSync(
                anyString(),
                eq("session-resume"),
                eq(agent),
                eq(InterviewAiGuardStage.INTERVIEW_REFERENCE_ANSWER),
                eq("reference-answer-key")
        )).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0, String.class);
            assertTrue(prompt.contains("AI/机器学习工程师"));
            assertTrue(prompt.contains("TF-IDF"));
            assertTrue(prompt.contains("回答有方向，但缺少实现细节"));
            return """
                    {"choices":[{"message":{"content":"{\\"referenceAnswers\\":[{\\"questionNumber\\":\\"1\\",\\"referenceAnswer\\":\\"参考回答：可以先说明 TF-IDF 用于关键词召回和稀疏相似度计算，再说明 Sentence-BERT 用于语义一致性判断，最后用加权融合、阈值校准和人工复核闭环降低误判。\\"}]}"}}]}
                    """;
        });

        InterviewReferenceAnswerService service = new InterviewReferenceAnswerService(
                resolver,
                invoker,
                new InterviewResponseParser(),
                questionBankService,
                sessionQuestionMapper
        );

        List<InterviewTurnLog> turns = service.attachReferenceAnswers(
                "session-resume",
                "AI/机器学习工程师",
                List.of(InterviewTurnLog.builder()
                        .questionNumber("1")
                        .questionContent("请说明 TF-IDF 与语义匹配如何协同工作。")
                        .answerContent("我用了 TF-IDF 和 Sentence-BERT。")
                        .feedback("回答有方向，但缺少实现细节。")
                        .score(68)
                        .build())
        );

        assertEquals("参考回答：可以先说明 TF-IDF 用于关键词召回和稀疏相似度计算，再说明 Sentence-BERT 用于语义一致性判断，最后用加权融合、阈值校准和人工复核闭环降低误判。",
                turns.get(0).getReferenceAnswer());
    }
}
