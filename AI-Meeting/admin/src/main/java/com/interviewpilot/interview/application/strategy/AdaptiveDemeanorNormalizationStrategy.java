package com.interviewpilot.interview.application.strategy;

import org.springframework.stereotype.Component;

/**
 * 自适应神态评分归一化策略（默认实现）
 * 自动检测 AI 返回的评分是否为 10 分制，若是则乘以 10 转换为百分制。
 *
 * <p>检测逻辑：若所有输入分数都在 0-10 范围内，则判定为 10 分制并自动转换。</p>
 */
@Component
public class AdaptiveDemeanorNormalizationStrategy implements DemeanorNormalizationStrategy {

    @Override
    public boolean isLikelyTenScale(Integer... scores) {
        if (scores == null || scores.length == 0) {
            return false;
        }
        for (Integer score : scores) {
            if (score == null || score < 0 || score > 10) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int normalize(Integer score, boolean tenScaleDetected) {
        if (score == null) {
            return 0;
        }
        int normalized = tenScaleDetected ? score * 10 : score;
        return Math.max(0, Math.min(100, normalized));
    }
}

