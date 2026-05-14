package com.interviewpilot.interview.application.strategy;

import com.interviewpilot.interview.api.io.resp.RadarChartDTO;
import org.springframework.stereotype.Component;

/**
 * 加权雷达图计算策略（默认实现）
 * 将简历评分、面试评分、神态评分按权重合成综合指数，并计算专业技能分。
 *
 * <p>权重配置：
 * <ul>
 *   <li>综合潜力指数 = 简历 25% + 面试 55% + 神态 20%</li>
 *   <li>专业技能 = 简历 30% + 面试 70%</li>
 * </ul>
 * </p>
 *
 * <p>同时支持通过 fillDimensionScores() 填充 7 维细粒度评分（内容、逻辑、专业、表达、应变、时间控制、礼仪）。</p>
 */
@Component
public class WeightedRadarComputationStrategy implements RadarComputationStrategy {

    private static final double RESUME_WEIGHT = 0.25D;
    private static final double INTERVIEW_WEIGHT = 0.55D;
    private static final double DEMEANOR_WEIGHT = 0.20D;
    private static final double PROFESSIONAL_SKILLS_INTERVIEW_WEIGHT = 0.70D;
    private static final double PROFESSIONAL_SKILLS_RESUME_WEIGHT = 0.30D;

    @Override
    public RadarChartDTO compute(Integer resumeScore, Integer interviewScore, Integer demeanorScore) {
        Integer resume = clampNullableScore(resumeScore);
        Integer interview = clampNullableScore(interviewScore);
        Integer demeanor = clampNullableScore(demeanorScore);

        RadarChartDTO radarChart = new RadarChartDTO();
        radarChart.setResumeScore(defaultScore(resume));
        radarChart.setInterviewPerformance(defaultScore(interview));
        radarChart.setDemeanorEvaluation(defaultScore(demeanor));
        radarChart.setProfessionalSkills(calculateProfessionalSkills(resume, interview));
        radarChart.setPotentialIndex(calculateWeightedComposite(resume, interview, demeanor));
        return radarChart;
    }

    /**
     * 在已有雷达图上填充 7 维评分字段（由 DimensionScoreStrategy 计算后传入）。
     */
    public void fillDimensionScores(RadarChartDTO radarChart, DimensionScoreResult dimensionResult) {
        if (radarChart == null || dimensionResult == null) {
            return;
        }
        radarChart.setContentScore(dimensionResult.getContentScore());
        radarChart.setLogicScore(dimensionResult.getLogicScore());
        radarChart.setProfessionalMatchScore(dimensionResult.getProfessionalScore());
        radarChart.setExpressionScore(dimensionResult.getExpressionScore());
        radarChart.setAdaptabilityScore(dimensionResult.getAdaptabilityScore());
        radarChart.setTimeControlScore(dimensionResult.getTimeControlScore());
        radarChart.setEtiquetteScore(dimensionResult.getEtiquetteScore());
        radarChart.setCompositeScore(dimensionResult.getCompositeScore());
    }

    private int calculateProfessionalSkills(Integer resumeScore, Integer interviewScore) {
        double weightedScore = 0D;
        double totalWeight = 0D;
        if (resumeScore != null) {
            weightedScore += clampScore(resumeScore) * PROFESSIONAL_SKILLS_RESUME_WEIGHT;
            totalWeight += PROFESSIONAL_SKILLS_RESUME_WEIGHT;
        }
        if (interviewScore != null) {
            weightedScore += clampScore(interviewScore) * PROFESSIONAL_SKILLS_INTERVIEW_WEIGHT;
            totalWeight += PROFESSIONAL_SKILLS_INTERVIEW_WEIGHT;
        }
        if (totalWeight <= 0D) {
            return 0;
        }
        return clampScore((int) Math.round(weightedScore / totalWeight));
    }

    private int calculateWeightedComposite(Integer resumeScore, Integer interviewScore, Integer demeanorScore) {
        double weightedScore = 0D;
        double totalWeight = 0D;
        if (resumeScore != null) {
            weightedScore += clampScore(resumeScore) * RESUME_WEIGHT;
            totalWeight += RESUME_WEIGHT;
        }
        if (interviewScore != null) {
            weightedScore += clampScore(interviewScore) * INTERVIEW_WEIGHT;
            totalWeight += INTERVIEW_WEIGHT;
        }
        if (demeanorScore != null) {
            weightedScore += clampScore(demeanorScore) * DEMEANOR_WEIGHT;
            totalWeight += DEMEANOR_WEIGHT;
        }
        if (totalWeight <= 0D) {
            return 0;
        }
        return clampScore((int) Math.round(weightedScore / totalWeight));
    }

    private int defaultScore(Integer score) {
        return score == null ? 0 : clampScore(score);
    }

    private Integer clampNullableScore(Integer score) {
        if (score == null) {
            return null;
        }
        return clampScore(score);
    }

    private int clampScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }
}

