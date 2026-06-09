package com.interviewpilot.interview.flow.report;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.interviewpilot.agent.application.BusinessAgentResolver;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.interview.api.io.resp.InterviewReviewFeedbackRespDTO;
import com.interviewpilot.interview.api.io.resp.RadarChartDTO;
import com.interviewpilot.interview.application.guard.core.InterviewAiGuardException;
import com.interviewpilot.interview.application.guard.core.InterviewAiGuardStage;
import com.interviewpilot.interview.service.model.InterviewTurnLog;
import com.interviewpilot.interview.shared.InterviewAiInvoker;
import com.interviewpilot.interview.shared.InterviewResponseParser;
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
public class InterviewReportAiReviewService {

    private static final int MAX_TURNS_FOR_PROMPT = 8;
    private static final int MAX_TEXT_LEN = 700;

    private final BusinessAgentResolver businessAgentResolver;
    private final InterviewAiInvoker interviewAiInvoker;
    private final InterviewResponseParser interviewResponseParser;

    public InterviewReviewFeedbackRespDTO generateReviewFeedback(
            String sessionId,
            String interviewDirection,
            List<InterviewTurnLog> turns,
            RadarChartDTO radarChart,
            String interviewSuggestions) {
        if (StrUtil.isBlank(sessionId) || turns == null || turns.isEmpty()) {
            return null;
        }

        try {
            AgentPropertiesDO agent = businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_ANSWER_EVALUATION);
            String prompt = buildPrompt(interviewDirection, turns, radarChart, interviewSuggestions);
            String singleFlightKey = interviewAiInvoker.buildSingleFlightKey(
                    InterviewAiGuardStage.INTERVIEW_REPORT_REVIEW,
                    sessionId,
                    DigestUtil.sha256Hex(prompt).substring(0, 16)
            );
            String aiResponse = interviewAiInvoker.callAiSync(
                    prompt,
                    sessionId,
                    agent,
                    InterviewAiGuardStage.INTERVIEW_REPORT_REVIEW,
                    singleFlightKey
            );
            Map<String, Object> parsed = interviewResponseParser.extractStructuredResult(
                    aiResponse,
                    "overallComment",
                    "highlights",
                    "improvementTips",
                    "nextActions"
            );
            InterviewReviewFeedbackRespDTO feedback = normalizeReviewFeedback(parsed);
            if (!hasContent(feedback)) {
                return null;
            }
            return feedback;
        } catch (InterviewAiGuardException ex) {
            log.warn("Report AI review guarded call failed, sessionId={}, code={}",
                    sessionId, ex.getErrorCode(), ex);
            return null;
        } catch (Exception ex) {
            log.warn("Report AI review failed, sessionId={}", sessionId, ex);
            return null;
        }
    }

    private String buildPrompt(
            String interviewDirection,
            List<InterviewTurnLog> turns,
            RadarChartDTO radarChart,
            String interviewSuggestions) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("interviewDirection", StrUtil.blankToDefault(interviewDirection, "未提供"));
        payload.put("scores", buildScorePayload(radarChart));
        payload.put("interviewSuggestions", StrUtil.blankToDefault(interviewSuggestions, ""));
        payload.put("qaTurns", buildTurnPayload(turns));

        return """
                你是一位严谨的 AI 面试复盘教练。请基于候选人的真实问答、每轮 AI 评分反馈、雷达分和神态分，生成本场面试报告总结。
                要求：
                1. 必须使用中文，避免英文模板句。
                2. 所有结论必须来自输入数据，不要编造不存在的经历。
                3. overallComment 给出整场表现判断，点明主要优势和最重要短板。
                4. highlights、improvementTips、nextActions 各返回 1 到 3 条，句子具体可执行。
                5. 只返回严格 JSON，不要 Markdown，不要输出思考过程或解释。
                6. 每条内容不超过 40 个汉字。
                JSON schema:
                {"overallComment":"","highlights":[""],"improvementTips":[""],"nextActions":[""]}

                输入数据：
                """ + JSON.toJSONString(payload);
    }

    private Map<String, Object> buildScorePayload(RadarChartDTO radarChart) {
        if (radarChart == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> scores = new LinkedHashMap<>();
        scores.put("resumeScore", radarChart.getResumeScore());
        scores.put("interviewPerformance", radarChart.getInterviewPerformance());
        scores.put("demeanorEvaluation", radarChart.getDemeanorEvaluation());
        scores.put("professionalSkills", radarChart.getProfessionalSkills());
        scores.put("potentialIndex", radarChart.getPotentialIndex());
        scores.put("contentScore", radarChart.getContentScore());
        scores.put("logicScore", radarChart.getLogicScore());
        scores.put("professionalScore", radarChart.getProfessionalScore());
        scores.put("expressionScore", radarChart.getExpressionScore());
        scores.put("adaptabilityScore", radarChart.getAdaptabilityScore());
        scores.put("timeControlScore", radarChart.getTimeControlScore());
        scores.put("etiquetteScore", radarChart.getEtiquetteScore());
        return scores;
    }

    private List<Map<String, Object>> buildTurnPayload(List<InterviewTurnLog> turns) {
        return turns.stream()
                .filter(turn -> turn != null && !Boolean.TRUE.equals(turn.getIsFollowUp()))
                .limit(MAX_TURNS_FOR_PROMPT)
                .map(turn -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("questionNumber", turn.getQuestionNumber());
                    row.put("question", clip(turn.getQuestionContent()));
                    row.put("answer", clip(turn.getAnswerContent()));
                    row.put("score", turn.getScore());
                    row.put("feedback", clip(turn.getFeedback()));
                    row.put("followUpNeeded", turn.getFollowUpNeeded());
                    return row;
                })
                .toList();
    }

    private InterviewReviewFeedbackRespDTO normalizeReviewFeedback(Map<String, Object> parsed) {
        if (parsed == null || parsed.isEmpty()) {
            return null;
        }
        InterviewReviewFeedbackRespDTO feedback = new InterviewReviewFeedbackRespDTO();
        feedback.setOverallComment(clip(interviewResponseParser.asString(parsed.get("overallComment"))));
        feedback.setHighlights(limitStrings(interviewResponseParser.asStringList(parsed.get("highlights"))));
        feedback.setImprovementTips(limitStrings(interviewResponseParser.asStringList(parsed.get("improvementTips"))));
        feedback.setNextActions(limitStrings(interviewResponseParser.asStringList(parsed.get("nextActions"))));
        return feedback;
    }

    private List<String> limitStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(StrUtil::isNotBlank)
                .map(this::clip)
                .filter(StrUtil::isNotBlank)
                .limit(3)
                .toList();
    }

    private boolean hasContent(InterviewReviewFeedbackRespDTO feedback) {
        if (feedback == null) {
            return false;
        }
        return StrUtil.isNotBlank(feedback.getOverallComment())
                || (feedback.getHighlights() != null && !feedback.getHighlights().isEmpty())
                || (feedback.getImprovementTips() != null && !feedback.getImprovementTips().isEmpty())
                || (feedback.getNextActions() != null && !feedback.getNextActions().isEmpty());
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
