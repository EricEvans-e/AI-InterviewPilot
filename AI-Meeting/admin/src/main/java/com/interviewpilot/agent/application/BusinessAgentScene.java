package com.interviewpilot.agent.application;

import cn.hutool.core.util.StrUtil;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum BusinessAgentScene {

    GENERAL_AGENT_CHAT("general-agent-chat", "通用智能体", "Mimo 2.5 通用智能体", "Mimo 2.5 Pro 通用智能体"),
    INTERVIEW_QUESTION_EXTRACTION(
            "interview-question-extraction",
            "面试出题官",
            "面试题出题官",
            "Mimo 2.5 面试出题官"
    ),
    INTERVIEW_ANSWER_EVALUATION(
            "interview-answer-evaluation",
            "用户答案评分官",
            "面试答案评分官",
            "Mimo 2.5 答案评分官",
            "Mimo 2.5 Pro 答案评分官"
    ),
    INTERVIEW_DEMEANOR(
            "interview-demeanor",
            "神态分析官",
            "神态评分面试官",
            "表情分析面试官",
            "Mimo 2.5 神态分析官"
    ),
    INTERVIEW_QUESTION_ASKING(
            "interview-question-asking",
            "面试提问官",
            "Mimo 2.5 面试提问官",
            "Mimo 2.5 Pro 面试提问官"
    );

    private final String code;

    private final String defaultAgentName;

    private final List<String> candidateAgentNames;

    BusinessAgentScene(String code, String defaultAgentName, String... aliasAgentNames) {
        this.code = code;
        this.defaultAgentName = defaultAgentName;
        this.candidateAgentNames = Arrays.asList(buildCandidateNames(defaultAgentName, aliasAgentNames));
    }

    public String getCode() {
        return code;
    }

    public String getDefaultAgentName() {
        return defaultAgentName;
    }

    public List<String> getCandidateAgentNames() {
        return candidateAgentNames;
    }

    public boolean supportsAgent(AgentPropertiesDO agent) {
        if (agent == null) {
            return false;
        }
        if (requiresVisionCapableModel() && isMimoProModel(agent.getApiSecret())) {
            return false;
        }
        return true;
    }

    public static Optional<BusinessAgentScene> fromCode(String code) {
        return Arrays.stream(values())
                .filter(scene -> scene.getCode().equals(code))
                .findFirst();
    }

    private boolean requiresVisionCapableModel() {
        return this == INTERVIEW_QUESTION_EXTRACTION || this == INTERVIEW_DEMEANOR;
    }

    private static boolean isMimoProModel(String modelName) {
        String normalized = StrUtil.trimToEmpty(modelName).toLowerCase();
        return normalized.contains("mimo") && normalized.contains("pro");
    }

    private static String[] buildCandidateNames(String defaultAgentName, String... aliasAgentNames) {
        String[] names = new String[aliasAgentNames.length + 1];
        names[0] = defaultAgentName;
        System.arraycopy(aliasAgentNames, 0, names, 1, aliasAgentNames.length);
        return names;
    }
}
