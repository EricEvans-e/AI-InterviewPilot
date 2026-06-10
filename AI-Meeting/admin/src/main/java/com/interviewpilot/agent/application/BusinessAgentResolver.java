package com.interviewpilot.agent.application;

import cn.hutool.core.util.StrUtil;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.common.enums.InterviewErrorCodeEnum;
import com.interviewpilot.toolkit.ai.AgentPropertiesLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessAgentResolver {

    private final BusinessAgentBindingProperties bindingProperties;
    private final AgentPropertiesLoader agentPropertiesLoader;

    public AgentPropertiesDO resolveRequired(BusinessAgentScene scene) {
        // 1. 优先查 DB 中 is_active=1 + scene_code 匹配的 agent
        AgentPropertiesDO activeAgent = agentPropertiesLoader.getActiveBySceneCode(scene.getCode());
        if (activeAgent != null && scene.supportsAgent(activeAgent)) {
            log.info("Resolved business agent from scene binding, scene={}, agentName={}, agentId={}, provider={}",
                    scene.getCode(), activeAgent.getAgentName(), activeAgent.getId(), activeAgent.getAiProvider());
            return activeAgent;
        }
        if (activeAgent != null) {
            log.warn("Ignored incompatible active business agent, scene={}, agentName={}, agentId={}, provider={}, model={}",
                    scene.getCode(),
                    activeAgent.getAgentName(),
                    activeAgent.getId(),
                    activeAgent.getAiProvider(),
                    activeAgent.getApiSecret());
        }

        // 2. Fallback: YAML 配置的 agent name → enum 候选名（保留原有逻辑）
        Set<String> candidateAgentNames = new LinkedHashSet<>();
        String configuredAgentName = bindingProperties.resolveAgentName(scene);
        if (StrUtil.isNotBlank(configuredAgentName)) {
            candidateAgentNames.add(configuredAgentName.trim());
        }
        candidateAgentNames.addAll(scene.getCandidateAgentNames());

        for (String candidateAgentName : candidateAgentNames) {
            AgentPropertiesDO agentProperties = agentPropertiesLoader.getByAgentName(candidateAgentName);
            if (agentProperties != null && scene.supportsAgent(agentProperties)) {
                if (StrUtil.isNotBlank(configuredAgentName) && !configuredAgentName.trim().equals(candidateAgentName)) {
                    log.warn(
                            "Configured agent not found, fallback matched scene={}, configuredName={}, matchedName={}, agentId={}",
                            scene.getCode(),
                            configuredAgentName,
                            candidateAgentName,
                            agentProperties.getId()
                    );
                } else {
                    log.info("Resolved business agent scene={}, agentName={}, agentId={}",
                            scene.getCode(), candidateAgentName, agentProperties.getId());
                }
                return agentProperties;
            }
            if (agentProperties != null) {
                log.warn("Ignored incompatible fallback business agent, scene={}, candidateName={}, agentId={}, provider={}, model={}",
                        scene.getCode(),
                        candidateAgentName,
                        agentProperties.getId(),
                        agentProperties.getAiProvider(),
                        agentProperties.getApiSecret());
            }
        }

        log.error("No agent configuration found for scene={}, candidateNames={}", scene.getCode(), candidateAgentNames);
        throw new ClientException(
                "agent binding not found for scene=" + scene.getCode() + ", candidateNames=" + candidateAgentNames,
                InterviewErrorCodeEnum.AGENT_CONFIG_NOT_FOUND
        );
    }
}
