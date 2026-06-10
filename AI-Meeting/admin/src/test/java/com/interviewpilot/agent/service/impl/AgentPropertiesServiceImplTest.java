package com.interviewpilot.agent.service.impl;

import com.interviewpilot.agent.api.io.resp.SceneBindingRespDTO;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.agent.dao.mapper.AgentPropertiesMapper;
import com.interviewpilot.agent.service.AgentTagService;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.toolkit.ai.AgentPropertiesLoader;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentPropertiesServiceImplTest {

    @Test
    void getSceneBindings_ShouldExposeProOnlyForTextOnlyScenes() {
        AgentPropertiesMapper mapper = mock(AgentPropertiesMapper.class);
        AgentPropertiesServiceImpl service = new AgentPropertiesServiceImpl(
                mock(AgentTagService.class),
                mock(AgentPropertiesLoader.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        when(mapper.selectList(any())).thenReturn(List.of(
                agent(8L, "Mimo 2.5 面试出题官", "interview-question-extraction", 1, "mimo-v2.5"),
                agent(9L, "Mimo 2.5 神态分析官", "interview-demeanor", 1, "mimo-v2.5"),
                agent(11L, "Mimo 2.5 答案评分官", "interview-answer-evaluation", 1, "mimo-v2.5"),
                agent(14L, "Mimo 2.5 Pro 答案评分官", "interview-answer-evaluation", 0, "mimo-v2.5-pro"),
                agent(12L, "Mimo 2.5 面试提问官", "interview-question-asking", 1, "mimo-v2.5"),
                agent(15L, "Mimo 2.5 Pro 面试提问官", "interview-question-asking", 0, "mimo-v2.5-pro"),
                agent(13L, "Mimo 2.5 通用智能体", "general-agent-chat", 1, "mimo-v2.5"),
                agent(16L, "Mimo 2.5 Pro 通用智能体", "general-agent-chat", 0, "mimo-v2.5-pro"),
                agent(21L, "误配置 Pro 出题官", "interview-question-extraction", 0, "mimo-v2.5-pro"),
                agent(22L, "误配置 Pro 神态分析官", "interview-demeanor", 0, "mimo-v2.5-pro")
        ));

        List<SceneBindingRespDTO> bindings = service.getSceneBindings();

        assertThat(candidateNames(bindings, "interview-question-extraction"))
                .containsExactly("Mimo 2.5 面试出题官");
        assertThat(candidateNames(bindings, "interview-answer-evaluation"))
                .containsExactly("Mimo 2.5 答案评分官", "Mimo 2.5 Pro 答案评分官");
        assertThat(candidateNames(bindings, "interview-question-asking"))
                .containsExactly("Mimo 2.5 面试提问官", "Mimo 2.5 Pro 面试提问官");
        assertThat(candidateNames(bindings, "general-agent-chat"))
                .containsExactly("Mimo 2.5 通用智能体", "Mimo 2.5 Pro 通用智能体");
        assertThat(candidateNames(bindings, "interview-demeanor"))
                .containsExactly("Mimo 2.5 神态分析官");
        assertThat(activeAgentName(bindings, "interview-demeanor"))
                .isEqualTo("Mimo 2.5 神态分析官");
    }

    @Test
    void activateAgent_ShouldRejectProModelForVisionScenes() {
        AgentPropertiesMapper mapper = mock(AgentPropertiesMapper.class);
        AgentPropertiesServiceImpl service = new AgentPropertiesServiceImpl(
                mock(AgentTagService.class),
                mock(AgentPropertiesLoader.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        AgentPropertiesDO proDemeanorAgent = agent(
                22L,
                "误配置 Pro 神态分析官",
                "interview-demeanor",
                0,
                "mimo-v2.5-pro"
        );
        when(mapper.selectById(22L)).thenReturn(proDemeanorAgent);

        assertThrows(
                ClientException.class,
                () -> service.activateAgent("interview-demeanor", 22L)
        );
        verify(mapper, never()).update(any(), any());
    }

    private static AgentPropertiesDO agent(
            Long id,
            String name,
            String sceneCode,
            Integer isActive,
            String model) {
        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setId(id);
        agent.setAgentName(name);
        agent.setSceneCode(sceneCode);
        agent.setIsActive(isActive);
        agent.setAiProvider("openai");
        agent.setApiSecret(model);
        agent.setDelFlag(0);
        return agent;
    }

    private static List<String> candidateNames(List<SceneBindingRespDTO> bindings, String sceneCode) {
        return binding(bindings, sceneCode).getCandidates().stream()
                .map(SceneBindingRespDTO.CandidateAgent::getAgentName)
                .toList();
    }

    private static String activeAgentName(List<SceneBindingRespDTO> bindings, String sceneCode) {
        return binding(bindings, sceneCode).getActiveAgentName();
    }

    private static SceneBindingRespDTO binding(List<SceneBindingRespDTO> bindings, String sceneCode) {
        return bindings.stream()
                .filter(item -> sceneCode.equals(item.getSceneCode()))
                .findFirst()
                .orElseThrow();
    }
}
