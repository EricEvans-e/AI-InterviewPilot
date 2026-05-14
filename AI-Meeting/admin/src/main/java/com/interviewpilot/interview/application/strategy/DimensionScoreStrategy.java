package com.interviewpilot.interview.application.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interviewpilot.interview.dao.entity.ScoringWeightConfigDO;
import com.interviewpilot.interview.dao.mapper.ScoringWeightConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 7 维评分引擎：基于 AI 原始分数和面试上下文计算 7 维评分 + 加权总分。
 * <p>
 * 维度：内容质量、逻辑结构、专业匹配、语言表达、临场应变、时间控制、礼仪仪态。
 * 权重按院校/专业可配置，未配置时使用默认权重。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DimensionScoreStrategy {

    private final ScoringWeightConfigMapper weightConfigMapper;

    /**
     * 计算 7 维分数 + 加权总分
     *
     * @param aiRawScore    AI 评估原始总分 (0-100)
     * @param aiFeedback    AI 评估反馈文本
     * @param demeanorScore 神态分析评分 (0-100)，可为 null
     * @param answerDuration 答题耗时（秒）
     * @param timeLimit     答题时限（秒），0 或 null 表示无限制
     * @param collegeId     院校 ID，可为 null
     * @param majorId       专业 ID，可为 null
     * @return 7 维评分结果
     */
    public DimensionScoreResult compute(int aiRawScore, String aiFeedback,
                                         Integer demeanorScore, int answerDuration,
                                         int timeLimit, Long collegeId, Long majorId) {
        DimensionScoreResult result = new DimensionScoreResult();

        // 基于 AI 分数和反馈进行维度拆分 (MVP: 简单规则)
        result.setContentScore(clamp(aiRawScore));
        result.setLogicScore(clamp((int) (aiRawScore * 0.9)));
        result.setProfessionalScore(clamp((int) (aiRawScore * 0.85)));
        result.setExpressionScore(clamp((int) (aiRawScore * 0.9)));
        result.setAdaptabilityScore(clamp((int) (aiRawScore * 0.8)));

        // 时间控制评分
        if (timeLimit > 0) {
            double ratio = (double) answerDuration / timeLimit;
            if (ratio >= 0.5 && ratio <= 1.0) {
                result.setTimeControlScore(100);
            } else if (ratio < 0.5) {
                result.setTimeControlScore((int) (ratio * 2 * 80));
            } else {
                result.setTimeControlScore(Math.max(0, (int) (100 - (ratio - 1.0) * 100)));
            }
        } else {
            result.setTimeControlScore(70);
        }

        // 礼仪评分 (来自神态分析)
        result.setEtiquetteScore(demeanorScore != null ? clamp(demeanorScore) : 70);

        // 加权总分
        ScoringWeightConfigDO weights = findWeightConfig(collegeId, majorId);
        result.setCompositeScore(computeWeighted(result, weights));

        return result;
    }

    private int computeWeighted(DimensionScoreResult scores, ScoringWeightConfigDO w) {
        double total = scores.getContentScore() * w.getWContent().doubleValue()
                + scores.getLogicScore() * w.getWLogic().doubleValue()
                + scores.getProfessionalScore() * w.getWProfessional().doubleValue()
                + scores.getExpressionScore() * w.getWExpression().doubleValue()
                + scores.getAdaptabilityScore() * w.getWAdaptability().doubleValue()
                + scores.getTimeControlScore() * w.getWTimeControl().doubleValue()
                + scores.getEtiquetteScore() * w.getWEtiquette().doubleValue();
        return clamp((int) Math.round(total / 100.0));
    }

    private ScoringWeightConfigDO findWeightConfig(Long collegeId, Long majorId) {
        // 优先精确匹配 collegeId+majorId, 再匹配 collegeId, 最后取默认
        if (collegeId != null && majorId != null) {
            ScoringWeightConfigDO config = weightConfigMapper.selectOne(
                    new LambdaQueryWrapper<ScoringWeightConfigDO>()
                            .eq(ScoringWeightConfigDO::getCollegeId, collegeId)
                            .eq(ScoringWeightConfigDO::getMajorId, majorId)
                            .eq(ScoringWeightConfigDO::getDelFlag, 0)
                            .last("LIMIT 1"));
            if (config != null) {
                return config;
            }
        }
        if (collegeId != null) {
            ScoringWeightConfigDO config = weightConfigMapper.selectOne(
                    new LambdaQueryWrapper<ScoringWeightConfigDO>()
                            .eq(ScoringWeightConfigDO::getCollegeId, collegeId)
                            .isNull(ScoringWeightConfigDO::getMajorId)
                            .eq(ScoringWeightConfigDO::getDelFlag, 0)
                            .last("LIMIT 1"));
            if (config != null) {
                return config;
            }
        }
        // 默认权重
        ScoringWeightConfigDO defaultConfig = weightConfigMapper.selectOne(
                new LambdaQueryWrapper<ScoringWeightConfigDO>()
                        .eq(ScoringWeightConfigDO::getIsDefault, true)
                        .eq(ScoringWeightConfigDO::getDelFlag, 0)
                        .last("LIMIT 1"));
        if (defaultConfig != null) {
            return defaultConfig;
        }
        // 兜底：数据库无任何配置时构造硬编码默认值 (30/15/15/15/10/5/10)
        return buildHardcodedDefault();
    }

    private ScoringWeightConfigDO buildHardcodedDefault() {
        ScoringWeightConfigDO fallback = new ScoringWeightConfigDO();
        fallback.setWContent(new java.math.BigDecimal("30"));
        fallback.setWLogic(new java.math.BigDecimal("15"));
        fallback.setWProfessional(new java.math.BigDecimal("15"));
        fallback.setWExpression(new java.math.BigDecimal("15"));
        fallback.setWAdaptability(new java.math.BigDecimal("10"));
        fallback.setWTimeControl(new java.math.BigDecimal("5"));
        fallback.setWEtiquette(new java.math.BigDecimal("10"));
        return fallback;
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
