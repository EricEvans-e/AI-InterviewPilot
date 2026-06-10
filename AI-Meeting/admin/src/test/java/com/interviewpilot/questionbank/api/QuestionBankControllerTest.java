package com.interviewpilot.questionbank.api;

import com.interviewpilot.questionbank.api.io.resp.QuestionCoverageRespDTO;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import com.interviewpilot.questionbank.service.QuestionAiGenerateService;
import com.interviewpilot.questionbank.service.QuestionBankService;
import com.interviewpilot.questionbank.service.QuestionImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class QuestionBankControllerTest {

    private MockMvc mockMvc;
    private QuestionBankService questionBankService;
    private QuestionAiGenerateService questionAiGenerateService;

    @BeforeEach
    void setUp() {
        questionBankService = mock(QuestionBankService.class);
        questionAiGenerateService = mock(QuestionAiGenerateService.class);
        QuestionBankController controller = new QuestionBankController(
                questionBankService,
                questionAiGenerateService,
                mock(QuestionImportService.class)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReturnQuestionCoverage() throws Exception {
        QuestionCoverageRespDTO coverage = new QuestionCoverageRespDTO();
        coverage.setCollegeId(11L);
        coverage.setMajorId(22L);
        coverage.setInterviewMode("专业题");
        coverage.setApprovedCount(3);
        coverage.setExactMatchCount(1);
        coverage.setFallbackCount(2);
        coverage.setCanStartImmediately(true);
        coverage.setMayNeedAiGeneration(true);
        when(questionBankService.coverage(11L, 22L, "专业题", "沟通表达", "hard", 5)).thenReturn(coverage);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/ip/v1/questions/coverage")
                        .param("collegeId", "11")
                        .param("majorId", "22")
                        .param("interviewMode", "专业题")
                        .param("abilityTag", "沟通表达")
                        .param("difficulty", "hard")
                        .param("requiredCount", "5"))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.approvedCount").value(3))
                .andExpect(jsonPath("$.data.canStartImmediately").value(true));

        verify(questionBankService).coverage(11L, 22L, "专业题", "沟通表达", "hard", 5);
    }

    @Test
    void shouldReturnExpandedQuestions() throws Exception {
        QuestionDO question = new QuestionDO();
        question.setId(99L);
        question.setTitle("AI 扩展题");
        question.setQuestionType("综合题");
        question.setCollegeId(10L);
        question.setMajorId(20L);

        when(questionAiGenerateService.expandQuestions(any())).thenReturn(List.of(question));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/ip/v1/questions/ai-expand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "referenceQuestionIds":[11,12],
                                  "collegeId":10,
                                  "majorId":20,
                                  "questionType":"综合题",
                                  "count":2
                                }
                                """))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data[0].title").value("AI 扩展题"))
                .andExpect(jsonPath("$.data[0].questionType").value("综合题"))
                .andExpect(jsonPath("$.data[0].collegeId").value(10))
                .andExpect(jsonPath("$.data[0].majorId").value(20));

        verify(questionAiGenerateService).expandQuestions(any());
    }
}
