package com.interviewpilot.interview.application.rule.node;

import com.interviewpilot.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * LiteFlow 规则链节点：追问次数上限守卫
 * 检查当前追问次数是否已达上限（followUpCount >= resolvedMaxFollowUp），
 * 若已达上限则标记不追问（FOLLOW_UP_LIMIT_REACHED）并终止规则链。
 */
@LiteflowComponent("followUpLimitGuard")
public class FollowUpLimitGuardNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        if (context.isTerminated()) {
            return;
        }
        if (context.getFollowUpCount() >= context.getResolvedMaxFollowUp()) {
            context.markNoFollowUp("FOLLOW_UP_LIMIT_REACHED", "follow-up limit reached");
        }
    }
}
