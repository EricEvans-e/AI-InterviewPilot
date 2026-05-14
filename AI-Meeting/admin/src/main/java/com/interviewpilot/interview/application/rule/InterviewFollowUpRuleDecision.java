package com.interviewpilot.interview.application.rule;

import lombok.Data;

/**
 * 追问规则引擎决策结果
 * 由 LiteFlow 规则链各节点投票后产出，包含是否需要追问、原因码、最终追问上限等信息。
 *
 * <p>reasonCode 取值（按优先级从高到低）：
 * <ul>
 *   <li>INTERVIEW_COMPLETED — 面试已结束，不追问</li>
 *   <li>FOLLOW_UP_LIMIT_REACHED — 追问次数已达上限</li>
 *   <li>AI_SUGGESTED — AI 评分结果建议追问</li>
 *   <li>LOW_SCORE — 得分低于阈值，需要追问</li>
 *   <li>MISSING_POINTS — AI 识别到回答遗漏关键点</li>
 *   <li>NO_RULE_TRIGGER — 无规则触发，不追问</li>
 *   <li>RULE_ENGINE_FALLBACK — 规则引擎异常，降级决策</li>
 * </ul>
 * </p>
 */
@Data
public class InterviewFollowUpRuleDecision {

    private boolean needFollowUp;

    private int resolvedMaxFollowUp;

    private String reasonCode;

    private String reasonText;

    private String chainId;

    private String ruleVersion;

    private boolean fallback;

    public static InterviewFollowUpRuleDecision noFollowUp(
            int resolvedMaxFollowUp,
            String reasonCode,
            String reasonText,
            String chainId,
            String ruleVersion,
            boolean fallback) {
        InterviewFollowUpRuleDecision decision = new InterviewFollowUpRuleDecision();
        decision.setNeedFollowUp(false);
        decision.setResolvedMaxFollowUp(resolvedMaxFollowUp);
        decision.setReasonCode(reasonCode);
        decision.setReasonText(reasonText);
        decision.setChainId(chainId);
        decision.setRuleVersion(ruleVersion);
        decision.setFallback(fallback);
        return decision;
    }
}
