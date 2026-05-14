package com.interviewpilot.interview.application.rule.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.interviewpilot.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * LiteFlow 规则链节点：遗漏点追问判断
 * 检查 AI 是否识别到回答中的遗漏关键点（missingPoints）或追问提示（followUpQuestionHint），
 * 若存在则标记需要追问（MISSING_POINTS）。
 */
@LiteflowComponent("missingPointsJudge")
public class MissingPointsJudgeNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        if (context.isTerminated()) {
            return;
        }
        boolean hasMissingPoints = CollUtil.isNotEmpty(context.getMissingPoints());
        boolean hasFollowUpHint = StrUtil.isNotBlank(context.getFollowUpQuestionHint());
        if (hasMissingPoints || hasFollowUpHint) {
            context.markNeedFollowUp("MISSING_POINTS", "missing points or follow-up hint detected");
        }
    }
}
