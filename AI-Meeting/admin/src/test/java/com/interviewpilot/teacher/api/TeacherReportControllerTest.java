package com.interviewpilot.teacher.api;

import com.interviewpilot.interview.service.InterviewRecordService;
import com.interviewpilot.teacher.service.TeacherReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class TeacherReportControllerTest {

    private MockMvc mockMvc;
    private InterviewRecordService interviewRecordService;

    @BeforeEach
    void setUp() {
        TeacherReviewService teacherReviewService = mock(TeacherReviewService.class);
        interviewRecordService = mock(InterviewRecordService.class);
        TeacherReportController controller = new TeacherReportController(teacherReviewService, interviewRecordService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldDeleteInterviewRecordBySessionId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/ip/v1/teacher/sessions/session-delete-1/record"))
                .andExpect(jsonPath("$.code").value("0"));

        verify(interviewRecordService).deleteRecordBySessionIdForTeacher("session-delete-1");
    }
}
