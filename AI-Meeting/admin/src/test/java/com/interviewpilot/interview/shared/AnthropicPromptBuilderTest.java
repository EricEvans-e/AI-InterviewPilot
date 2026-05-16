package com.interviewpilot.interview.shared;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicPromptBuilderTest {

    @Test
    void shouldReturnInputWhenParametersIsNull() {
        String result = AnthropicPromptBuilder.build("hello", null);
        assertEquals("hello", result);
    }

    @Test
    void shouldReturnInputWhenParametersIsEmpty() {
        String result = AnthropicPromptBuilder.build("hello", new HashMap<>());
        assertEquals("hello", result);
    }

    @Test
    void shouldBuildEvaluationPromptWhenQuestionPresent() {
        Map<String, Object> params = new HashMap<>();
        params.put("question", "请介绍一下你的项目经验");
        params.put("AGENT_USER_INPUT", "我做过一个Spring Boot项目");
        params.put("resume_context", "3年Java开发经验");

        String prompt = AnthropicPromptBuilder.build("我做过一个Spring Boot项目", params);

        assertTrue(prompt.contains("面试评分专家"), "应包含评分角色描述");
        assertTrue(prompt.contains("请介绍一下你的项目经验"), "应包含面试题目");
        assertTrue(prompt.contains("我做过一个Spring Boot项目"), "应包含候选人回答");
        assertTrue(prompt.contains("3年Java开发经验"), "应包含简历背景");
        assertTrue(prompt.contains("\"score\""), "应包含JSON格式要求");
        assertTrue(prompt.contains("follow_up_needed"), "应包含追问判断字段");
    }

    @Test
    void shouldBuildEvaluationPromptWithoutResumeContext() {
        Map<String, Object> params = new HashMap<>();
        params.put("question", "什么是多态？");
        params.put("AGENT_USER_INPUT", "多态是面向对象的特性");

        String prompt = AnthropicPromptBuilder.build("多态是面向对象的特性", params);

        assertTrue(prompt.contains("什么是多态？"));
        assertTrue(prompt.contains("多态是面向对象的特性"));
        assertTrue(prompt.contains("面试评分专家"));
    }

    @Test
    void shouldBuildFollowUpPrompt() {
        Map<String, Object> params = new HashMap<>();
        params.put("mode", "FOLLOW_UP");
        params.put("question", "什么是微服务？");
        params.put("AGENT_USER_INPUT", "微服务是一种架构风格");
        params.put("follow_up_count", 2);
        params.put("max_follow_up", 3);
        params.put("resume_context", "有微服务项目经验");

        String prompt = AnthropicPromptBuilder.build("微服务是一种架构风格", params);

        assertTrue(prompt.contains("追问"), "应包含追问描述");
        assertTrue(prompt.contains("第 2/3 次追问"), "应包含追问次数");
        assertTrue(prompt.contains("什么是微服务？"), "应包含原题");
        assertTrue(prompt.contains("微服务是一种架构风格"), "应包含回答");
        assertTrue(prompt.contains("有微服务项目经验"), "应包含简历背景");
        assertTrue(prompt.contains("__FINISH__"), "应包含结束标记");
    }

    @Test
    void shouldBuildFollowUpPromptWithoutCount() {
        Map<String, Object> params = new HashMap<>();
        params.put("mode", "FOLLOW_UP");
        params.put("question", "什么是多态？");
        params.put("AGENT_USER_INPUT", "多态是...");

        String prompt = AnthropicPromptBuilder.build("多态是...", params);

        assertTrue(prompt.contains("追问"));
        assertTrue(prompt.contains("什么是多态？"));
        assertTrue(prompt.contains("__FINISH__"));
    }

    @Test
    void shouldFallbackToInputWhenNoQuestionAndNoMode() {
        Map<String, Object> params = new HashMap<>();
        params.put("some_key", "some_value");

        String result = AnthropicPromptBuilder.build("原始输入", params);
        assertEquals("原始输入", result);
    }
}
