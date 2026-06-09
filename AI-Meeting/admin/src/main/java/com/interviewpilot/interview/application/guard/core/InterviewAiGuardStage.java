package com.interviewpilot.interview.application.guard.core;

/**
 * AI 调用保护阶段常量
 * 每个阶段有独立的熔断器、限流器、超时配置
 */
public final class InterviewAiGuardStage {

    /** 评分阶段（用户提交答案后 AI 打分） */
    public static final String INTERVIEW_EVALUATION = "interview-evaluation";
    /** 追问阶段（AI 决定是否追问并生成追问问题） */
    public static final String INTERVIEW_FOLLOWUP = "interview-followup";
    /** 出题阶段（AI 解析简历生成面试题） */
    public static final String INTERVIEW_EXTRACTION = "interview-extraction";
    /** 神态分析阶段（AI 分析用户摄像头截图） */
    public static final String INTERVIEW_DEMEANOR = "interview-demeanor";
    /** 报告总结阶段（AI 生成整场面试分析与建议） */
    public static final String INTERVIEW_REPORT_REVIEW = "interview-report-review";
    /** 参考答案生成阶段（报告复盘用，不参与候选人评分） */
    public static final String INTERVIEW_REFERENCE_ANSWER = "interview-reference-answer";

    private InterviewAiGuardStage() {
    }
}
