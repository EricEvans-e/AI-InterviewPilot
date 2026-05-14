package com.interviewpilot.interview.application.strategy;

import com.interviewpilot.interview.service.model.InterviewTurnLog;

import java.util.List;

/**
 * 面试评分聚合策略接口
 * 定义分数截断、从聚合数据计算平均分、从轮次记录计算平均分等操作。
 *
 * @see AverageInterviewScoreAggregatorStrategy 平均分实现（默认策略）
 */
public interface InterviewScoreAggregatorStrategy {

    int clampScore(Integer score);

    int averageFromAggregate(Long scoreSum, Long answerCount);

    Integer averageFromTurns(List<InterviewTurnLog> turns);
}

