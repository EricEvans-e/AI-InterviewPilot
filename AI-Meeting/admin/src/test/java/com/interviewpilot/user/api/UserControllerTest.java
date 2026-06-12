package com.interviewpilot.user.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.user.service.SmsCodeService;
import com.interviewpilot.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private com.interviewpilot.auth.application.LoginSessionService loginSessionService;

    @Mock
    private SmsCodeService smsCodeService;

    @InjectMocks
    private UserController controller;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new MockCurrentUserResolver())
                .build();
    }

    @Test
    void changePassword_ShouldUseCurrentUserAndReturnSuccess() throws Exception {
        mockMvc.perform(put("/api/ip/v1/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "oldPassword", "old-pass",
                                "newPassword", "new-pass-123",
                                "confirmPassword", "new-pass-123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> oldPasswordCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> newPasswordCaptor = ArgumentCaptor.forClass(String.class);
        verify(userService).changePassword(
                usernameCaptor.capture(),
                oldPasswordCaptor.capture(),
                newPasswordCaptor.capture()
        );

        assertThat(usernameCaptor.getValue()).isEqualTo("student-user");
        assertThat(oldPasswordCaptor.getValue()).isEqualTo("old-pass");
        assertThat(newPasswordCaptor.getValue()).isEqualTo("new-pass-123");
    }

    private static final class MockCurrentUserResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUser.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            return "student-user";
        }
    }
}
