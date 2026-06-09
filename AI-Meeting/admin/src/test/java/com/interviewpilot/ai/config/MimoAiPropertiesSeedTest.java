package com.interviewpilot.ai.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MimoAiPropertiesSeedTest {

    @Test
    void mimoProChatSeed_ShouldUseOpenAiCompatibleTextRoute() throws Exception {
        String seedSql = Files.readString(
                Path.of("src/main/resources/sql/ai_properties.sql"),
                StandardCharsets.UTF_8
        );

        assertTrue(seedSql.contains(
                "(2, 'Mimo V2.5 Pro', 'openai', 'MIMO_API_KEY', null, 'https://token-plan-cn.xiaomimimo.com/v1', 'mimo-v2.5-pro'"
        ));
    }

    @Test
    void demeanorAgentSeed_ShouldUseVisionCapableMimoV25Model() throws Exception {
        String seedSql = Files.readString(
                Path.of("src/main/resources/sql/agent_properties.sql"),
                StandardCharsets.UTF_8
        );

        assertTrue(seedSql.contains(
                "(9, 'Mimo Demeanor Analyst', 'mimo-v2.5', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai'"
        ));
    }
}
