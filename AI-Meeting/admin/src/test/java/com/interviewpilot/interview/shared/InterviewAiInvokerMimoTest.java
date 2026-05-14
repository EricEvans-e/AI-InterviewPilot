package com.interviewpilot.interview.shared;

import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.ai.service.chat.MimoChatHandler;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.interview.application.guard.core.AiCallGuardService;
import com.interviewpilot.interview.application.guard.singleflight.service.DistributedInterviewAiSingleFlightService;
import com.interviewpilot.toolkit.xunfei.XingChenAIClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * InterviewAiInvoker 的 Mimo 路由测试。
 * 通过反射直接测试 doChat() 私有方法，避免 guard/singleFlight 的类加载器隔离问题。
 */
class InterviewAiInvokerMimoTest {

    private final XingChenAIClient xingChenAIClient = Mockito.mock(XingChenAIClient.class, Mockito.withSettings().mockMaker(MockMakers.SUBCLASS));
    private final MimoChatHandler mimoChatHandler = Mockito.mock(MimoChatHandler.class, Mockito.withSettings().mockMaker(MockMakers.SUBCLASS));
    private final AiCallGuardService aiCallGuardService = Mockito.mock(AiCallGuardService.class, Mockito.withSettings().mockMaker(MockMakers.SUBCLASS));
    private final DistributedInterviewAiSingleFlightService singleFlightService = Mockito.mock(DistributedInterviewAiSingleFlightService.class, Mockito.withSettings().mockMaker(MockMakers.SUBCLASS));

    private String invokeDoChat(String input, String sessionId, AgentPropertiesDO agent, String fileUrl, Map<String, Object> params) throws Exception {
        InterviewAiInvoker invoker = new InterviewAiInvoker(xingChenAIClient, mimoChatHandler, aiCallGuardService, singleFlightService);
        Method doChat = InterviewAiInvoker.class.getDeclaredMethod("doChat", String.class, String.class, AgentPropertiesDO.class, String.class, Map.class);
        doChat.setAccessible(true);
        return (String) doChat.invoke(invoker, input, sessionId, agent, fileUrl, params);
    }

    @Test
    void shouldRouteToMimoWhenAiProviderIsMimo() throws Exception {
        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setAiProvider("mimo");
        agent.setApiKey("tp-test-key");
        agent.setApiSecret("mimo-v2.5-pro");
        agent.setApiFlowId("https://token-plan-sgp.xiaomimimo.com/anthropic");

        Map<String, Object> params = new HashMap<>();
        params.put("question", "什么是多态？");
        params.put("AGENT_USER_INPUT", "多态是面向对象的特性");
        params.put("resume_context", "3年Java经验");

        Mockito.when(mimoChatHandler.callSync(any(AiPropertiesDO.class), any(String.class)))
                .thenReturn("{\"score\":85,\"feedback\":\"回答良好\"}");

        String result = invokeDoChat("多态是面向对象的特性", "session-mimo", agent, null, params);

        assertEquals("{\"score\":85,\"feedback\":\"回答良好\"}", result);
        verify(mimoChatHandler).callSync(any(AiPropertiesDO.class), any(String.class));
    }

    @Test
    void shouldRouteToMimoAndBuildCorrectAiProperties() throws Exception {
        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setAiProvider("mimo");
        agent.setApiKey("tp-test-key");
        agent.setApiSecret("mimo-v2.5-pro");
        agent.setApiFlowId("https://token-plan-sgp.xiaomimimo.com/anthropic");

        Map<String, Object> params = new HashMap<>();
        params.put("question", "什么是微服务？");
        params.put("AGENT_USER_INPUT", "微服务是架构风格");
        params.put("mode", "FOLLOW_UP");
        params.put("follow_up_count", 1);
        params.put("max_follow_up", 3);

        Mockito.when(mimoChatHandler.callSync(any(AiPropertiesDO.class), any(String.class)))
                .thenReturn("请详细说说服务拆分策略");

        invokeDoChat("微服务是架构风格", "session-mimo2", agent, null, params);

        ArgumentCaptor<AiPropertiesDO> captor = ArgumentCaptor.forClass(AiPropertiesDO.class);
        verify(mimoChatHandler).callSync(captor.capture(), any(String.class));
        AiPropertiesDO capturedProps = captor.getValue();
        assertEquals("tp-test-key", capturedProps.getApiKey());
        assertEquals("mimo-v2.5-pro", capturedProps.getModelName());
        assertEquals("https://token-plan-sgp.xiaomimimo.com/anthropic", capturedProps.getApiUrl());
    }

    @Test
    void shouldBuildEvaluationPromptForMimo() throws Exception {
        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setAiProvider("mimo");
        agent.setApiKey("tp-key");
        agent.setApiSecret("mimo-v2.5-pro");
        agent.setApiFlowId("https://example.com");

        Map<String, Object> params = new HashMap<>();
        params.put("question", "描述一下你的项目");
        params.put("AGENT_USER_INPUT", "我做了个电商系统");
        params.put("resume_context", "5年经验");

        Mockito.when(mimoChatHandler.callSync(any(), any())).thenReturn("ok");

        invokeDoChat("我做了个电商系统", "s1", agent, null, params);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mimoChatHandler).callSync(any(), promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        // 验证 prompt 包含评分相关关键词
        assertEquals(true, prompt.contains("面试评分专家"), "prompt 应包含评分角色");
        assertEquals(true, prompt.contains("描述一下你的项目"), "prompt 应包含题目");
        assertEquals(true, prompt.contains("我做了个电商系统"), "prompt 应包含回答");
    }

    @Test
    void shouldBuildFollowUpPromptForMimo() throws Exception {
        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setAiProvider("mimo");
        agent.setApiKey("tp-key");
        agent.setApiSecret("mimo-v2.5-pro");
        agent.setApiFlowId("https://example.com");

        Map<String, Object> params = new HashMap<>();
        params.put("mode", "FOLLOW_UP");
        params.put("question", "什么是微服务？");
        params.put("AGENT_USER_INPUT", "微服务是一种架构");
        params.put("follow_up_count", 2);
        params.put("max_follow_up", 3);

        Mockito.when(mimoChatHandler.callSync(any(), any())).thenReturn("追问问题");

        invokeDoChat("微服务是一种架构", "s2", agent, null, params);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mimoChatHandler).callSync(any(), promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertEquals(true, prompt.contains("追问"), "prompt 应包含追问描述");
        assertEquals(true, prompt.contains("第 2/3 次追问"), "prompt 应包含追问次数");
    }
}
