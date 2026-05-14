package com.interviewpilot.interview.application.rule.node;

import com.interviewpilot.interview.application.rule.InterviewFollowUpRuleContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * LiteFlow 规则链节点：低分追问判断
 * 检查当前得分是否低于阈值（score < lowScoreThreshold），
 * 若低于阈值则标记需要追问（LOW_SCORE），触发原因包含具体分数对比。
 */
@LiteflowComponent("lowScoreJudge")
public class LowScoreJudgeNode extends NodeComponent {

    @Override
    public void process() {
        InterviewFollowUpRuleContext context = getContextBean(InterviewFollowUpRuleContext.class);
        if (context.isTerminated()) {
            return;
        }
        Integer score = context.getScore();
        if (score != null && score < context.getLowScoreThreshold()) {
            context.markNeedFollowUp(
                    "LOW_SCORE",
                    "score below threshold: " + score + " < " + context.getLowScoreThreshold()
            );
        }
    }
}
