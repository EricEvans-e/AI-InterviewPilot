package com.interviewpilot.interview.application.strategy;

import lombok.Data;

/**
 * 7 维评分结果
 */
@Data
public class DimensionScoreResult {

    /**
     * 内容质量 (0-100)
     */
    private Integer contentScore;

    /**
     * 逻辑结构 (0-100)
     */
    private Integer logicScore;

    /**
     * 专业匹配 (0-100)
     */
    private Integer professionalScore;

    /**
     * 语言表达 (0-100)
     */
    private Integer expressionScore;

    /**
     * 临场应变 (0-100)
     */
    private Integer adaptabilityScore;

    /**
     * 时间控制 (0-100)
     */
    private Integer timeControlScore;

    /**
     * 礼仪仪态 (0-100)
     */
    private Integer etiquetteScore;

    /**
     * 加权总分 (0-100)
     */
    private Integer compositeScore;
}
