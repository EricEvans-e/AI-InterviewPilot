package com.interviewpilot.interview.api;

import com.interviewpilot.common.config.storage.ApplicationStorageProperties;
import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.common.convention.context.UserContext;
import com.interviewpilot.common.web.GlobalExceptionHandler;
import com.interviewpilot.interview.api.io.resp.InterviewPlaybackItemRespDTO;
import com.interviewpilot.interview.api.io.resp.InterviewRecordRespDTO;
import com.interviewpilot.interview.service.InterviewRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class InterviewRecordControllerTest {

    private MockMvc mockMvc;
    private InterviewRecordService interviewRecordService;

    @BeforeEach
    void setUp() {
        interviewRecordService = mock(InterviewRecordService.class);
        ApplicationStorageProperties storageProperties = mock(ApplicationStorageProperties.class);
        InterviewRecordController controller = new InterviewRecordController(interviewRecordService, storageProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new MockCurrentUserResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldGenerateReferenceAnswersThroughController() throws Exception {
        InterviewPlaybackItemRespDTO item = new InterviewPlaybackItemRespDTO();
        item.setQuestionNumber("1-F1");
        item.setQuestionContent("追问内容");
        item.setReferenceAnswer("补生成的追问参考答案");

        InterviewRecordRespDTO respDTO = new InterviewRecordRespDTO();
        respDTO.setSessionId("session-controller-ref");
        respDTO.setPlaybackItems(List.of(item));

        when(interviewRecordService.generateReferenceAnswers("session-controller-ref", 7007L))
                .thenReturn(respDTO);

        mockMvc.perform(MockMvcRequestBuilders.post(
                        "/api/ip/v1/interview/interview/record/session-controller-ref/reference-answers"))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.sessionId").value("session-controller-ref"))
                .andExpect(jsonPath("$.data.playbackItems[0].questionNumber").value("1-F1"))
                .andExpect(jsonPath("$.data.playbackItems[0].referenceAnswer").value("补生成的追问参考答案"));

        verify(interviewRecordService).generateReferenceAnswers("session-controller-ref", 7007L);
    }

    private static final class MockCurrentUserResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUser.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            return new UserContext(7007L, "record-controller-user");
        }
    }
}
