package com.interviewpilot.interview.flow.report;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.interviewpilot.agent.application.BusinessAgentResolver;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.interview.application.guard.core.InterviewAiGuardException;
import com.interviewpilot.interview.application.guard.core.InterviewAiGuardStage;
import com.interviewpilot.interview.dao.entity.InterviewSessionQuestionDO;
import com.interviewpilot.interview.dao.mapper.InterviewSessionQuestionMapper;
import com.interviewpilot.interview.service.model.InterviewTurnLog;
import com.interviewpilot.interview.shared.InterviewAiInvoker;
import com.interviewpilot.interview.shared.InterviewResponseParser;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import com.interviewpilot.questionbank.service.QuestionBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewReferenceAnswerService {

    private static final int MAX_TURNS_FOR_PROMPT = 8;
    private static final int MAX_TEXT_LEN = 900;

    private final BusinessAgentResolver businessAgentResolver;
    private final InterviewAiInvoker interviewAiInvoker;
    private final InterviewResponseParser interviewResponseParser;
    private final QuestionBankService questionBankService;
    private final InterviewSessionQuestionMapper sessionQuestionMapper;

    public List<InterviewTurnLog> attachAvailableReferenceAnswers(
            String sessionId,
            List<InterviewTurnLog> turns) {
        if (StrUtil.isBlank(sessionId) || turns == null || turns.isEmpty()) {
            return turns == null ? Collections.emptyList() : turns;
        }

        Map<String, String> presetAnswers = loadPresetReferenceAnswers(sessionId);
        for (InterviewTurnLog turn : turns) {
            if (turn == null || StrUtil.isNotBlank(turn.getReferenceAnswer())) {
                continue;
            }
            String preset = presetAnswers.get(normalizeQuestionNumber(turn.getQuestionNumber()));
            if (StrUtil.isNotBlank(preset)) {
                turn.setReferenceAnswer(clip(preset));
            }
        }
        return turns;
    }

    public List<InterviewTurnLog> generateMissingReferenceAnswers(
            String sessionId,
            String interviewDirection,
            List<InterviewTurnLog> turns) {
        turns = attachAvailableReferenceAnswers(sessionId, turns);
        if (StrUtil.isBlank(sessionId) || turns == null || turns.isEmpty()) {
            return turns == null ? Collections.emptyList() : turns;
        }
        List<InterviewTurnLog> missing = turns.stream()
                .filter(turn -> turn != null
                        && StrUtil.isNotBlank(turn.getQuestionContent())
                        && StrUtil.isBlank(turn.getReferenceAnswer()))
                .limit(MAX_TURNS_FOR_PROMPT)
                .toList();
        if (missing.isEmpty()) {
            return turns;
        }

        Map<String, String> generatedAnswers = generateReferenceAnswers(sessionId, interviewDirection, missing);
        for (InterviewTurnLog turn : missing) {
            String generated = generatedAnswers.get(normalizeQuestionNumber(turn.getQuestionNumber()));
            if (StrUtil.isBlank(generated)) {
                continue;
            }
            turn.setReferenceAnswer(clip(generated));
        }
        return turns;
    }

    private Map<String, String> loadPresetReferenceAnswers(String sessionId) {
        try {
            List<InterviewSessionQuestionDO> links = sessionQuestionMapper.selectList(
                    Wrappers.lambdaQuery(InterviewSessionQuestionDO.class)
                            .eq(InterviewSessionQuestionDO::getSessionId, sessionId)
                            .orderByAsc(InterviewSessionQuestionDO::getSeqIndex)
            );
            if (links == null || links.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<String, String> result = new LinkedHashMap<>();
            for (InterviewSessionQuestionDO link : links) {
                if (link == null || link.getQuestionId() == null || link.getSeqIndex() == null) {
                    continue;
                }
                QuestionDO question = questionBankService.getById(link.getQuestionId());
                if (question == null || StrUtil.isBlank(question.getReferenceAnswer())) {
                    continue;
                }
                result.put(String.valueOf(link.getSeqIndex() + 1), question.getReferenceAnswer().trim());
            }
            return result;
        } catch (Exception ex) {
            log.warn("Failed to load question-bank reference answers, sessionId={}", sessionId, ex);
            return Collections.emptyMap();
        }
    }

    private Map<String, String> generateReferenceAnswers(
            String sessionId,
            String interviewDirection,
            List<InterviewTurnLog> missingTurns) {
        if (missingTurns == null || missingTurns.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            AgentPropertiesDO agent = businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_ANSWER_EVALUATION);
            String prompt = buildPrompt(interviewDirection, missingTurns);
            String singleFlightKey = interviewAiInvoker.buildSingleFlightKey(
                    InterviewAiGuardStage.INTERVIEW_REFERENCE_ANSWER,
                    sessionId,
                    DigestUtil.sha256Hex(prompt).substring(0, 16)
            );
            String aiResponse = interviewAiInvoker.callAiSync(
                    prompt,
                    sessionId,
                    agent,
                    InterviewAiGuardStage.INTERVIEW_REFERENCE_ANSWER,
                    singleFlightKey
            );
            Map<String, Object> parsed = interviewResponseParser.extractStructuredResult(
                    aiResponse,
                    "referenceAnswers",
                    "referenceAnswer"
            );
            return parseReferenceAnswerMap(parsed);
        } catch (InterviewAiGuardException ex) {
            log.warn("Reference answer AI guarded call failed, sessionId={}, code={}",
                    sessionId, ex.getErrorCode(), ex);
            return Collections.emptyMap();
        } catch (Exception ex) {
            log.warn("Reference answer AI generation failed, sessionId={}", sessionId, ex);
            return Collections.emptyMap();
        }
    }

    private String buildPrompt(String interviewDirection, List<InterviewTurnLog> turns) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("interviewDirection", StrUtil.blankToDefault(interviewDirection, "未提供"));
        payload.put("qaTurns", turns.stream()
                .map(turn -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("questionNumber", normalizeQuestionNumber(turn.getQuestionNumber()));
                    row.put("isFollowUp", Boolean.TRUE.equals(turn.getIsFollowUp()));
                    row.put("question", clip(turn.getQuestionContent()));
                    row.put("candidateAnswer", clip(turn.getAnswerContent()));
                    row.put("score", turn.getScore());
                    row.put("feedback", clip(turn.getFeedback()));
                    return row;
                })
                .toList());

        return """
                你是一位严谨的中文 AI 面试官。请为面试报告生成“参考答案/参考回答”，供考生复盘学习。
                要求：
                1. 必须使用中文，回答应像优秀候选人在面试现场的作答，而不是评分点评。
                2. 必须紧扣题目和面试方向，结合候选人回答与反馈补足缺失点。
                3. 不要评价候选人，不要写“你回答得...”，不要复述得分。
                4. 每个 referenceAnswer 应包含可直接学习的答题结构、关键技术点、取舍、指标或落地细节。
                5. 如果是追问，参考回答要承接追问题本身，不要泛泛重复主问题。
                6. 只返回严格 JSON，不要 Markdown，不要输出思考过程。
                JSON schema:
                {"referenceAnswers":[{"questionNumber":"","referenceAnswer":""}]}

                输入数据：
                """ + JSON.toJSONString(payload);
    }

    private Map<String, String> parseReferenceAnswerMap(Map<String, Object> parsed) {
        if (parsed == null || parsed.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();

        if (parsed.containsKey("questionNumber") && parsed.containsKey("referenceAnswer")) {
            putReferenceAnswer(result, parsed);
            return result;
        }

        Object rawRows = parsed.get("referenceAnswers");
        if (rawRows == null) {
            return result;
        }

        try {
            List<LinkedHashMap<String, Object>> rows = JSON.parseObject(
                    JSON.toJSONString(rawRows),
                    new TypeReference<List<LinkedHashMap<String, Object>>>() {
                    }
            );
            if (rows == null) {
                return result;
            }
            for (Map<String, Object> row : rows) {
                putReferenceAnswer(result, row);
            }
            return result;
        } catch (Exception ex) {
            log.warn("Failed to parse reference answer rows", ex);
            return result;
        }
    }

    private void putReferenceAnswer(Map<String, String> result, Map<String, Object> row) {
        if (row == null) {
            return;
        }
        String questionNumber = normalizeQuestionNumber(interviewResponseParser.asString(row.get("questionNumber")));
        String referenceAnswer = interviewResponseParser.asString(row.get("referenceAnswer"));
        if (StrUtil.isBlank(questionNumber) || StrUtil.isBlank(referenceAnswer)) {
            return;
        }
        result.put(questionNumber, referenceAnswer.trim());
    }

    private String normalizeQuestionNumber(String questionNumber) {
        if (StrUtil.isBlank(questionNumber)) {
            return "";
        }
        return questionNumber.trim().toUpperCase();
    }

    private String clip(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        String cleaned = value.trim().replaceAll("\\s+", " ");
        if (cleaned.length() <= MAX_TEXT_LEN) {
            return cleaned;
        }
        return cleaned.substring(0, MAX_TEXT_LEN);
    }
}
