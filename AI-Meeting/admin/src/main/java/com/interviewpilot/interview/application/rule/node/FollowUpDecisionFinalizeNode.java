package com.interviewpilot.interview.application.rule.node;

import com.interviewpilot.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * LiteFlow 规则链节点：追问决策最终化
 * 规则链最后一个节点，调用 finalizeDecision() 将上下文中的投票结果合并为最终决策。
 * 若没有任何节点触发，则默认不追问（NO_RULE_TRIGGER）。
 */
@LiteflowComponent("followUpDecisionFinalize")
public class FollowUpDecisionFinalizeNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        context.finalizeDecision(false);
    }
}
