package com.interviewpilot.interview.application.rule;

import lombok.Data;

import java.util.List;

/**
 * 追问规则引擎上下文（LiteFlow 规则链共享数据载体）
 * 在 LiteFlow 规则链执行过程中，各节点通过此上下文传递和读取数据，最终产出追问决策。
 *
 * <p>生命周期：由 InterviewFollowUpRuleService.decide() 创建并填充 → 规则链各节点读写 → 最终产出 InterviewFollowUpRuleDecision</p>
 *
 * <p>关键字段说明：
 * <ul>
 *   <li>followUpCount / maxFollowUp — 当前追问次数与上限</li>
 *   <li>score / lowScoreThreshold — 当前得分与低分阈值</li>
 *   <li>followUpNeededFromAi — AI 评分结果中是否建议追问</li>
 *   <li>missingPoints / followUpQuestionHint — AI 识别的回答遗漏点与追问提示</li>
 *   <li>terminated — 标记规则链是否已提前终止（不再执行后续节点）</li>
 *   <li>decision — 最终追问决策输出</li>
 * </ul>
 * </p>
 *
 * @see InterviewFollowUpRuleService 规则引擎入口
 * @see InterviewFollowUpRuleDecision 追问决策结果
 */
@Data
public class InterviewFollowUpRuleContext {

    private String sessionId;

    private String requestId;

    private String questionNumber;

    private String interviewType;

    private boolean followUpQuestion;

    private int followUpCount;

    private int maxFollowUp;

    private int resolvedMaxFollowUp;

    private Integer score;

    private int lowScoreThreshold;

    private boolean followUpNeededFromAi;

    private List<String> missingPoints;

    private String followUpQuestionHint;

    private boolean interviewCompleted;

    private String chainId;

    private String ruleVersion;

    private boolean terminated;

    private InterviewFollowUpRuleDecision decision;

    public void markNoFollowUp(String reasonCode, String reasonText) {
        ensureDecision();
        decision.setNeedFollowUp(false);
        decision.setReasonCode(reasonCode);
        decision.setReasonText(reasonText);
        terminated = true;
    }

    public void markNeedFollowUp(String reasonCode, String reasonText) {
        ensureDecision();
        if (!decision.isNeedFollowUp()) {
            decision.setNeedFollowUp(true);
            decision.setReasonCode(reasonCode);
            decision.setReasonText(reasonText);
        }
    }

    public void finalizeDecision(boolean fallback) {
        ensureDecision();
        if (decision.getReasonCode() == null || decision.getReasonCode().isBlank()) {
            decision.setReasonCode("NO_RULE_TRIGGER");
            decision.setReasonText("no follow-up trigger matched");
            decision.setNeedFollowUp(false);
        }
        decision.setResolvedMaxFollowUp(resolvedMaxFollowUp > 0 ? resolvedMaxFollowUp : Math.max(maxFollowUp, 1));
        decision.setChainId(chainId);
        decision.setRuleVersion(ruleVersion);
        decision.setFallback(fallback);
    }

    private void ensureDecision() {
        if (decision == null) {
            decision = new InterviewFollowUpRuleDecision();
        }
    }
}
