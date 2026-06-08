package com.interviewpilot.ai.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiPropritiesTypeTest {

    @Test
    void openAiDefaults_ShouldPointToMimoChinaOpenAiCompatibleEndpoint() {
        assertEquals("https://token-plan-cn.xiaomimimo.com/v1", AiPropritiesType.OPENAI.getDefaultBaseUrl());
    }

    @Test
    void anthropicDefaults_ShouldPointToMimoChinaAnthropicCompatibleEndpoint() {
        assertEquals("https://token-plan-cn.xiaomimimo.com/anthropic", AiPropritiesType.ANTHROPIC.getDefaultBaseUrl());
    }

    @Test
    void legacySparkType_ShouldResolveToMimoOpenAiCompatibleProvider() {
        assertEquals(AiPropritiesType.OPENAI, AiPropritiesType.getByType("generalv3.5"));
        assertEquals(AiPropritiesType.OPENAI, AiPropritiesType.getByType("spark"));
        assertEquals(AiPropritiesType.OPENAI, AiPropritiesType.getByType("doubao"));
        assertEquals(AiPropritiesType.OPENAI, AiPropritiesType.getByType("deepseek"));
    }
}
