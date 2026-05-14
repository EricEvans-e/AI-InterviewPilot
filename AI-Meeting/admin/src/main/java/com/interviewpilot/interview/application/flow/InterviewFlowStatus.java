package com.interviewpilot.interview.application.flow;

/**
 * 面试流程状态枚举
 * 状态转换：INIT → ASKING → EVALUATING → FOLLOW_UP → ASKING → ... → COMPLETED
 */
public enum InterviewFlowStatus {

    /** 初始态（刚出完题，还没开始答） */
    INIT,
    /** 出题中（正在展示题目给用户） */
    ASKING,
    /** 评分中（用户提交了答案，AI 正在评分） */
    EVALUATING,
    /** 追问中（AI 决定追问，展示追问问题） */
    FOLLOW_UP,
    /** 已完成（所有题目答完） */
    COMPLETED;

    public static InterviewFlowStatus from(String rawStatus) {
        if (rawStatus == null) {
            return INIT;
        }
        try {
            return InterviewFlowStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (Exception ex) {
            return INIT;
        }
    }
}
