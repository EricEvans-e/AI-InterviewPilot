package com.interviewpilot.interview.api.io.resp;

import lombok.Data;

/**
 * 雷达图数据传输对象
 */
@Data
public class RadarChartDTO {
    
    /**
     * 简历评估得分 (0-100)
     */
    private Integer resumeScore;
    
    /**
     * 面试表现得分 (0-100)
     */
    private Integer interviewPerformance;
    
    /**
     * 神态管理评分 (0-100)
     */
    private Integer demeanorEvaluation;
    
    /**
     * 用户潜力指数 (0-100)
     */
    private Integer potentialIndex;
    
    /**
     * 专业技能评分 (0-100)
     */
    private Integer professionalSkills;

    // ---- 7 维评分（新增） ----

    /**
     * 内容质量得分 (0-100)
     */
    private Integer contentScore;

    /**
     * 逻辑结构得分 (0-100)
     */
    private Integer logicScore;

    /**
     * 专业匹配得分 (0-100)
     */
    private Integer professionalMatchScore;

    /**
     * 语言表达得分 (0-100)
     */
    private Integer expressionScore;

    /**
     * 临场应变得分 (0-100)
     */
    private Integer adaptabilityScore;

    /**
     * 时间控制得分 (0-100)
     */
    private Integer timeControlScore;

    /**
     * 礼仪仪态得分 (0-100)
     */
    private Integer etiquetteScore;

    /**
     * 加权总分 (0-100)
     */
    private Integer compositeScore;
}