package com.interviewpilot.interview.application.rule.node;

import com.interviewpilot.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * LiteFlow 规则链节点：面试完成状态守卫
 * 检查面试是否已结束（interviewCompleted），若已结束则标记不追问并终止规则链。
 * 防止在面试已完成后继续生成追问问题。
 */
@LiteflowComponent("completedStateGuard")
public class CompletedStateGuardNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        if (context.isInterviewCompleted()) {
            context.markNoFollowUp("INTERVIEW_COMPLETED", "interview already completed");
        }
    }
}
