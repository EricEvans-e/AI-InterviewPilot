package com.interviewpilot.interview.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.interviewpilot.common.database.BaseDO;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 评分权重配置实体
 */
@Data
@TableName("scoring_weight_config")
public class ScoringWeightConfigDO extends BaseDO {

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置名称
     */
    @TableField("config_name")
    private String configName;

    /**
     * 院校ID
     */
    @TableField("college_id")
    private Long collegeId;

    /**
     * 专业ID
     */
    @TableField("major_id")
    private Long majorId;

    /**
     * 内容质量权重 (百分比, 如 30 表示 30%)
     */
    @TableField("w_content")
    private BigDecimal wContent;

    /**
     * 逻辑结构权重
     */
    @TableField("w_logic")
    private BigDecimal wLogic;

    /**
     * 专业匹配权重
     */
    @TableField("w_professional")
    private BigDecimal wProfessional;

    /**
     * 语言表达权重
     */
    @TableField("w_expression")
    private BigDecimal wExpression;

    /**
     * 临场应变权重
     */
    @TableField("w_adaptability")
    private BigDecimal wAdaptability;

    /**
     * 时间控制权重
     */
    @TableField("w_time_control")
    private BigDecimal wTimeControl;

    /**
     * 礼仪仪态权重
     */
    @TableField("w_etiquette")
    private BigDecimal wEtiquette;

    /**
     * 是否默认配置
     */
    @TableField("is_default")
    private Boolean isDefault;
}
