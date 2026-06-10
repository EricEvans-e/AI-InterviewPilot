package com.interviewpilot.questionbank.api;

import com.interviewpilot.questionbank.api.io.resp.QuestionCoverageRespDTO;
import com.interviewpilot.questionbank.service.QuestionAiGenerateService;
import com.interviewpilot.questionbank.service.QuestionBankService;
import com.interviewpilot.questionbank.service.QuestionImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class QuestionBankControllerTest {

    private MockMvc mockMvc;
    private QuestionBankService questionBankService;

    @BeforeEach
    void setUp() {
        questionBankService = mock(QuestionBankService.class);
        QuestionBankController controller = new QuestionBankController(
                questionBankService,
                mock(QuestionAiGenerateService.class),
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
        when(questionBankService.coverage(11L, 22L, "专业题", 5)).thenReturn(coverage);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/ip/v1/questions/coverage")
                        .param("collegeId", "11")
                        .param("majorId", "22")
                        .param("interviewMode", "专业题")
                        .param("requiredCount", "5"))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.approvedCount").value(3))
                .andExpect(jsonPath("$.data.canStartImmediately").value(true));

        verify(questionBankService).coverage(11L, 22L, "专业题", 5);
    }
}
