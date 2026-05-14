package com.interviewpilot.interview.application.rule.node;

import com.interviewpilot.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * LiteFlow 规则链节点：加载追问上下文
 * 规则链第一个节点，负责初始化 resolvedMaxFollowUp（最终追问上限）。
 * 若上下文未预设值，则取 maxFollowUp 和 1 中的较大值作为默认值。
 */
@LiteflowComponent("loadFollowUpContext")
public class LoadFollowUpContextNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        int resolvedMax = context.getResolvedMaxFollowUp() > 0 ? context.getResolvedMaxFollowUp() : Math.max(context.getMaxFollowUp(), 1);
        context.setResolvedMaxFollowUp(resolvedMax);
    }
}
