package com.interviewpilot.interview.application.strategy;

import com.interviewpilot.interview.api.io.resp.RadarChartDTO;

/**
 * 雷达图计算策略接口
 * 根据简历评分、面试评分、神态评分三个维度计算雷达图数据。
 *
 * @see WeightedRadarComputationStrategy 加权实现（默认策略）
 */
public interface RadarComputationStrategy {

    RadarChartDTO compute(Integer resumeScore, Integer interviewScore, Integer demeanorScore);
}

