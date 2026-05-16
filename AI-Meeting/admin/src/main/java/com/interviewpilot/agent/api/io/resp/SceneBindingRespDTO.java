package com.interviewpilot.agent.api.io.resp;

import lombok.Data;
import java.util.List;

@Data
public class SceneBindingRespDTO {
    private String sceneCode;
    private String sceneName;
    private Long activeAgentId;
    private String activeAgentName;
    private String activeProvider;
    private List<CandidateAgent> candidates;

    @Data
    public static class CandidateAgent {
        private Long id;
        private String agentName;
        private String aiProvider;
    }
}
