package com.interviewpilot.interview.shared;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.ai.service.chat.MimoChatHandler;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.interview.application.guard.core.AiCallGuardService;
import com.interviewpilot.interview.application.guard.core.InterviewAiGuardStage;
import com.interviewpilot.interview.application.guard.singleflight.service.DistributedInterviewAiSingleFlightService;
import com.interviewpilot.toolkit.xunfei.XingChenAIClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 面试 AI 的统一调用入口，负责生成请求指纹、串联限流熔断保护、
 * 分布式 single-flight 复用以及最终的模型调用过程。
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewAiInvoker {

    private final XingChenAIClient xingChenAIClient;
    private final MimoChatHandler mimoChatHandler;
    private final AiCallGuardService aiCallGuardService;
    private final DistributedInterviewAiSingleFlightService distributedInterviewAiSingleFlightService;

    public String callAiSync(String prompt, String sessionId, AgentPropertiesDO agentProperties) throws Exception {
        String key = buildSingleFlightKey(InterviewAiGuardStage.INTERVIEW_EVALUATION, sessionId, null, prompt);
        return callAiSync(prompt, sessionId, agentProperties, InterviewAiGuardStage.INTERVIEW_EVALUATION, key);
    }

    public String callAiSync(
            String prompt,
            String sessionId,
            AgentPropertiesDO agentProperties,
            String stage,
            String singleFlightKey) throws Exception {
        return guardedCall(stage, singleFlightKey, () -> doChat(prompt, sessionId, agentProperties, null, null));
    }

    public String callAiSyncWithFile(
            String prompt,
            String sessionId,
            AgentPropertiesDO agentProperties,
            String fileUrl) throws Exception {
        String key = buildSingleFlightKey(InterviewAiGuardStage.INTERVIEW_DEMEANOR, sessionId, fileUrl);
        return callAiSyncWithFile(prompt, sessionId, agentProperties, fileUrl, InterviewAiGuardStage.INTERVIEW_DEMEANOR, key);
    }

    public String callAiSyncWithFile(
            String prompt,
            String sessionId,
            AgentPropertiesDO agentProperties,
            String fileUrl,
            String stage,
            String singleFlightKey) throws Exception {
        return guardedCall(stage, singleFlightKey, () -> doChat(prompt, sessionId, agentProperties, fileUrl, null));
    }

    public String callAiSyncWithParameters(
            String sessionId,
            AgentPropertiesDO agentProperties,
            Map<String, Object> parameters) throws Exception {
        Object rawInput = parameters == null ? null : parameters.get("AGENT_USER_INPUT");
        String input = rawInput == null ? "" : rawInput.toString().trim();
        String key = buildSingleFlightKey(InterviewAiGuardStage.INTERVIEW_EVALUATION, sessionId, null, input);
        return callAiSyncWithParameters(
                sessionId,
                agentProperties,
                parameters,
                InterviewAiGuardStage.INTERVIEW_EVALUATION,
                key
        );
    }

    public String callAiSyncWithParameters(
            String sessionId,
            AgentPropertiesDO agentProperties,
            Map<String, Object> parameters,
            String stage,
            String singleFlightKey) throws Exception {
        Object rawInput = parameters == null ? null : parameters.get("AGENT_USER_INPUT");
        String input = rawInput == null ? "" : rawInput.toString().trim();
        return guardedCall(
                stage,
                singleFlightKey,
                () -> doChat(StrUtil.blankToDefault(input, ""), sessionId, agentProperties, null, parameters)
        );
    }

    public String buildSingleFlightKey(
            String stage,
            String sessionId,
            String questionNumber,
            String answerContent) {
        String safeStage = StrUtil.blankToDefault(stage, "interview-default");
        String safeSessionId = StrUtil.blankToDefault(StrUtil.trimToEmpty(sessionId), "no-session");
        String safeQuestionNumber = StrUtil.blankToDefault(StrUtil.trimToEmpty(questionNumber), "-");
        String safeAnswerHash = StrUtil.isBlank(answerContent)
                ? "-"
                : DigestUtil.sha256Hex(answerContent.trim()).substring(0, 16);
        return safeStage + "|" + safeSessionId + "|" + safeQuestionNumber + "|" + safeAnswerHash;
    }

    public String buildSingleFlightKey(String stage, String sessionId, String businessKey) {
        String safeStage = StrUtil.blankToDefault(stage, "interview-default");
        String safeSessionId = StrUtil.blankToDefault(StrUtil.trimToEmpty(sessionId), "no-session");
        String safeBusinessKey = StrUtil.blankToDefault(StrUtil.trimToEmpty(businessKey), "-");
        return safeStage + "|" + safeSessionId + "|" + safeBusinessKey;
    }

    private String guardedCall(String stage, String singleFlightKey, Callable<String> callable) throws Exception {
        String safeStage = StrUtil.blankToDefault(stage, "interview-default");
        String key = StrUtil.blankToDefault(singleFlightKey, safeStage + "|no-key");
        return distributedInterviewAiSingleFlightService.execute(
                safeStage,
                key,
                () -> aiCallGuardService.execute(safeStage, key, callable)
        );
    }

    private String doChat(
            String input,
            String sessionId,
            AgentPropertiesDO agentProperties,
            String fileUrl,
            Map<String, Object> parameters) throws Exception {
        String aiProvider = agentProperties.getAiProvider();
        if ("mimo".equalsIgnoreCase(aiProvider)) {
            return doChatMimo(input, sessionId, agentProperties, parameters);
        }
        return doChatXingChen(input, sessionId, agentProperties, fileUrl, parameters);
    }

    private String doChatXingChen(
            String input,
            String sessionId,
            AgentPropertiesDO agentProperties,
            String fileUrl,
            Map<String, Object> parameters) throws Exception {
        StringBuilder aiResponse = new StringBuilder();
        xingChenAIClient.chat(
                input,
                StrUtil.isNotBlank(sessionId) ? sessionId : "evaluation_" + System.currentTimeMillis(),
                "{}",
                false,
                new OutputStream() {
                    @Override
                    public void write(int b) {
                    }

                    @Override
                    public void write(byte[] b, int off, int len) {
                        aiResponse.append(new String(b, off, len, StandardCharsets.UTF_8));
                    }
                },
                data -> {
                },
                agentProperties.getApiKey(),
                agentProperties.getApiSecret(),
                agentProperties.getApiFlowId(),
                fileUrl,
                parameters
        );
        return aiResponse.toString();
    }

    /**
     * Mimo 调用：将 XingChen 结构化参数转换为纯文本 prompt，通过 MimoChatHandler.callSync 调用。
     * field reuse: apiKey=Mimo API Key, apiSecret=模型名, apiFlowId=API URL
     */
    private String doChatMimo(
            String input,
            String sessionId,
            AgentPropertiesDO agentProperties,
            Map<String, Object> parameters) {
        String prompt = MimoPromptBuilder.build(input, parameters);
        log.info("[InterviewAiInvoker] Mimo 调用 sessionId={}, prompt前100字={}", sessionId,
                prompt.length() > 100 ? prompt.substring(0, 100) : prompt);

        AiPropertiesDO aiProps = new AiPropertiesDO();
        aiProps.setApiKey(agentProperties.getApiKey());
        aiProps.setModelName(agentProperties.getApiSecret());
        aiProps.setApiUrl(agentProperties.getApiFlowId());
        aiProps.setMaxTokens(8192);

        return mimoChatHandler.callSync(aiProps, prompt);
    }
}
