package com.interviewpilot.interview.application.strategy;

import com.interviewpilot.interview.service.model.InterviewTurnLog;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 平均分面试评分聚合策略（默认实现）
 * 从面试轮次记录中计算主问题（非追问）的平均得分。
 *
 * <p>计算逻辑：
 * <ul>
 *   <li>过滤掉追问轮次（isFollowUp=true）和无分数轮次</li>
 *   <li>对剩余主问题的得分取算术平均值</li>
 *   <li>所有分数截断到 0-100 范围</li>
 * </ul>
 * </p>
 */
@Component
public class AverageInterviewScoreAggregatorStrategy implements InterviewScoreAggregatorStrategy {

    @Override
    public int clampScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }

    @Override
    public int averageFromAggregate(Long scoreSum, Long answerCount) {
        if (scoreSum == null || answerCount == null || answerCount <= 0) {
            return 0;
        }
        return clampScore((int) Math.round((double) scoreSum / answerCount));
    }

    @Override
    public Integer averageFromTurns(List<InterviewTurnLog> turns) {
        if (turns == null || turns.isEmpty()) {
            return null;
        }

        int scoreSum = 0;
        int scoredTurns = 0;
        for (InterviewTurnLog turn : turns) {
            if (turn == null || turn.getScore() == null || Boolean.TRUE.equals(turn.getIsFollowUp())) {
                continue;
            }
            scoreSum += clampScore(turn.getScore());
            scoredTurns++;
        }

        if (scoredTurns <= 0) {
            return null;
        }
        return clampScore((int) Math.round((double) scoreSum / scoredTurns));
    }
}

