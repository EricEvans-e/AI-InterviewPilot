package com.interviewpilot.interview.application.strategy;

/**
 * 神态评分归一化策略接口
 * 处理不同 AI 模型返回的评分尺度差异（如 10 分制 vs 100 分制），统一归一化到 0-100 分。
 *
 * @see AdaptiveDemeanorNormalizationStrategy 自适应实现（默认策略）
 */
public interface DemeanorNormalizationStrategy {

    boolean isLikelyTenScale(Integer... scores);

    int normalize(Integer score, boolean tenScaleDetected);
}

