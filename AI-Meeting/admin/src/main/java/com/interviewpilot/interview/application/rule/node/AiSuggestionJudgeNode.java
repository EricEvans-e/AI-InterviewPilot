package com.interviewpilot.interview.application.rule.node;

import com.interviewpilot.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * LiteFlow 规则链节点：AI 建议追问判断
 * 检查 AI 评分结果中的 followUpNeeded 标志，若 AI 建议追问则标记需要追问（AI_SUGGESTED）。
 * 注意：此节点只标记"建议追问"，不会终止规则链，后续节点可能因其他条件（如低分、遗漏点）追加建议。
 */
@LiteflowComponent("aiSuggestionJudge")
public class AiSuggestionJudgeNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        if (context.isTerminated()) {
            return;
        }
        if (context.isFollowUpNeededFromAi()) {
            context.markNeedFollowUp("AI_SUGGESTED", "follow-up suggested by ai result");
        }
    }
}
