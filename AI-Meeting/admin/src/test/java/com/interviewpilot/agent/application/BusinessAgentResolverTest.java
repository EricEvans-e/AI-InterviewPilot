package com.interviewpilot.agent.application;

import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.toolkit.ai.AgentPropertiesLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessAgentResolverTest {

    @Test
    void shouldResolveConfiguredAgentNameFirst() {
        BusinessAgentBindingProperties properties = new BusinessAgentBindingProperties();
        properties.setInterviewDemeanor("神态分析官");

        AgentPropertiesLoader agentPropertiesLoader = mock(AgentPropertiesLoader.class);
        AgentPropertiesDO agentProperties = new AgentPropertiesDO();
        agentProperties.setId(9L);
        agentProperties.setAgentName("神态分析官");
        when(agentPropertiesLoader.getByAgentName("神态分析官")).thenReturn(agentProperties);

        BusinessAgentResolver resolver = new BusinessAgentResolver(properties, agentPropertiesLoader);
        AgentPropertiesDO resolved = resolver.resolveRequired(BusinessAgentScene.INTERVIEW_DEMEANOR);

        assertEquals(9L, resolved.getId());
        assertEquals("神态分析官", resolved.getAgentName());
    }

    @Test
    void shouldFallbackToBuiltinAgentAliasWhenConfiguredNameIsInvalid() {
        BusinessAgentBindingProperties properties = new BusinessAgentBindingProperties();
        properties.setInterviewQuestionExtraction("不存在的出题官");

        AgentPropertiesLoader agentPropertiesLoader = mock(AgentPropertiesLoader.class);
        AgentPropertiesDO agentProperties = new AgentPropertiesDO();
        agentProperties.setId(8L);
        agentProperties.setAgentName("面试出题官");
        when(agentPropertiesLoader.getByAgentName("不存在的出题官")).thenReturn(null);
        when(agentPropertiesLoader.getByAgentName("面试出题官")).thenReturn(agentProperties);

        BusinessAgentResolver resolver = new BusinessAgentResolver(properties, agentPropertiesLoader);
        AgentPropertiesDO resolved = resolver.resolveRequired(BusinessAgentScene.INTERVIEW_QUESTION_EXTRACTION);

        assertEquals(8L, resolved.getId());
        assertEquals("面试出题官", resolved.getAgentName());
    }

    @Test
    void shouldIgnoreActiveProAgentForVisionSceneAndFallbackToCompatibleAgent() {
        BusinessAgentBindingProperties properties = new BusinessAgentBindingProperties();
        properties.setInterviewDemeanor("Mimo 2.5 神态分析官");

        AgentPropertiesLoader agentPropertiesLoader = mock(AgentPropertiesLoader.class);
        AgentPropertiesDO incompatibleActiveAgent = new AgentPropertiesDO();
        incompatibleActiveAgent.setId(22L);
        incompatibleActiveAgent.setAgentName("误配置 Pro 神态分析官");
        incompatibleActiveAgent.setAiProvider("openai");
        incompatibleActiveAgent.setApiSecret("mimo-v2.5-pro");
        when(agentPropertiesLoader.getActiveBySceneCode("interview-demeanor")).thenReturn(incompatibleActiveAgent);

        AgentPropertiesDO compatibleAgent = new AgentPropertiesDO();
        compatibleAgent.setId(9L);
        compatibleAgent.setAgentName("Mimo 2.5 神态分析官");
        compatibleAgent.setAiProvider("openai");
        compatibleAgent.setApiSecret("mimo-v2.5");
        when(agentPropertiesLoader.getByAgentName("Mimo 2.5 神态分析官")).thenReturn(compatibleAgent);

        BusinessAgentResolver resolver = new BusinessAgentResolver(properties, agentPropertiesLoader);
        AgentPropertiesDO resolved = resolver.resolveRequired(BusinessAgentScene.INTERVIEW_DEMEANOR);

        assertEquals(9L, resolved.getId());
        assertEquals("Mimo 2.5 神态分析官", resolved.getAgentName());
    }

    @Test
    void shouldFailWhenNoConfiguredOrBuiltinAgentExists() {
        BusinessAgentBindingProperties properties = new BusinessAgentBindingProperties();
        properties.setGeneralAgentChat("不存在的通用智能体");

        AgentPropertiesLoader agentPropertiesLoader = mock(AgentPropertiesLoader.class);
        when(agentPropertiesLoader.getByAgentName("不存在的通用智能体")).thenReturn(null);
        when(agentPropertiesLoader.getByAgentName("通用智能体")).thenReturn(null);

        BusinessAgentResolver resolver = new BusinessAgentResolver(properties, agentPropertiesLoader);

        assertThrows(
                ClientException.class,
                () -> resolver.resolveRequired(BusinessAgentScene.GENERAL_AGENT_CHAT)
        );
    }
}
