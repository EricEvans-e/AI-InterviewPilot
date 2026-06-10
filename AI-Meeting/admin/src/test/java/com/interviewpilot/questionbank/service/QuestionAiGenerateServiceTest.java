package com.interviewpilot.questionbank.service;

import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.ai.service.AiPropertiesService;
import com.interviewpilot.ai.service.chat.AiChatHandler;
import com.interviewpilot.ai.service.chat.AiChatHandlerFactory;
import com.interviewpilot.questionbank.api.io.req.QuestionExpandReqDTO;
import com.interviewpilot.questionbank.dao.entity.CollegeDO;
import com.interviewpilot.questionbank.dao.entity.MajorDO;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionAiGenerateServiceTest {

    @Test
    void shouldBuildExpandPromptFromReferenceQuestionsAndManualConfig() {
        AiChatHandlerFactory aiChatHandlerFactory = mock(AiChatHandlerFactory.class);
        AiPropertiesService aiPropertiesService = mock(AiPropertiesService.class);
        CollegeService collegeService = mock(CollegeService.class);
        MajorService majorService = mock(MajorService.class);
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        AiChatHandler handler = mock(AiChatHandler.class);

        QuestionAiGenerateService service = new QuestionAiGenerateService(
                aiChatHandlerFactory,
                aiPropertiesService,
                collegeService,
                majorService,
                questionBankService
        );

        QuestionExpandReqDTO req = new QuestionExpandReqDTO();
        req.setReferenceQuestionIds(List.of(11L, 12L));
        req.setCollegeId(101L);
        req.setMajorId(202L);
        req.setQuestionType("综合题");
        req.setAbilityTag("professional_knowledge");
        req.setDifficulty("hard");
        req.setCount(2);
        req.setGenerateReferenceAnswer(false);
        req.setGenerateFollowUp(false);
        req.setGenerateScoringRule(false);

        CollegeDO college = new CollegeDO();
        college.setId(101L);
        college.setName("浙江理工大学");
        MajorDO major = new MajorDO();
        major.setId(202L);
        major.setName("人工智能");

        QuestionDO referenceOne = new QuestionDO();
        referenceOne.setId(11L);
        referenceOne.setTitle("请介绍你做过的 AI 项目");
        referenceOne.setContent("重点说明目标、方案和结果");
        referenceOne.setCollegeId(1L);
        referenceOne.setMajorId(2L);

        QuestionDO referenceTwo = new QuestionDO();
        referenceTwo.setId(12L);
        referenceTwo.setTitle("你如何理解模型上线后的监控");
        referenceTwo.setContent("请举一个实际场景");
        referenceTwo.setCollegeId(3L);
        referenceTwo.setMajorId(4L);

        AiPropertiesDO aiProperties = new AiPropertiesDO();
        aiProperties.setAiType("openai");
        aiProperties.setIsEnabled(1);
        aiProperties.setDelFlag(0);

        when(collegeService.getById(101L)).thenReturn(college);
        when(majorService.getById(202L)).thenReturn(major);
        when(questionBankService.listByIds(List.of(11L, 12L))).thenReturn(List.of(referenceOne, referenceTwo));
        when(aiPropertiesService.getDefaultMimoConfig()).thenReturn(aiProperties);
        when(aiChatHandlerFactory.getHandler("openai")).thenReturn(handler);
        when(handler.callSync(eq(aiProperties), any())).thenReturn("""
                [
                  {
                    "title":"扩展题目A",
                    "content":"请结合项目落地经验回答",
                    "questionType":"综合题",
                    "abilityTag":"professional_knowledge",
                    "difficulty":"hard",
                    "referenceAnswer":"should be dropped",
                    "scoringRule":"should be dropped",
                    "followUpQuestions":"should be dropped"
                  },
                  {
                    "title":"扩展题目B",
                    "content":"第二道扩展题",
                    "questionType":"综合题",
                    "abilityTag":"professional_knowledge",
                    "difficulty":"hard"
                  },
                  {
                    "title":"扩展题目C",
                    "content":"不应超出请求数量",
                    "questionType":"综合题",
                    "abilityTag":"professional_knowledge",
                    "difficulty":"hard"
                  }
                ]
                """);

        List<QuestionDO> result = service.expandQuestions(req);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(handler).callSync(eq(aiProperties), promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertThat(prompt).contains("浙江理工大学");
        assertThat(prompt).contains("人工智能");
        assertThat(prompt).contains("请介绍你做过的 AI 项目");
        assertThat(prompt).contains("你如何理解模型上线后的监控");
        assertThat(prompt).contains("避免与参考题重复");
        assertThat(prompt).contains("不要继承参考题的院校、专业或其他元数据");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCollegeId()).isEqualTo(101L);
        assertThat(result.get(0).getMajorId()).isEqualTo(202L);
        assertThat(result.get(0).getQuestionType()).isEqualTo("综合题");
        assertThat(result.get(0).getDifficulty()).isEqualTo("hard");
        assertThat(result.get(0).getReferenceAnswer()).isNull();
        assertThat(result.get(0).getScoringRule()).isNull();
        assertThat(result.get(0).getFollowUpQuestions()).isNull();
        assertThat(result.get(0).getStatus()).isEqualTo("pending_review");
        assertThat(result.get(0).getIsAiGenerated()).isTrue();
        assertThat(result).extracting(QuestionDO::getTitle)
                .containsExactly("扩展题目A", "扩展题目B");
    }
}
