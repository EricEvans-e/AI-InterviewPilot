package com.interviewpilot.questionbank.api.io.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI生成题目请求参数
 */
@Data
public class QuestionGenerateReqDTO {

    /**
     * 生成题目数量
     */
    @Min(value = 1, message = "生成数量至少为1")
    private Integer count = 5;

    /**
     * 目标院校名称
     */
    private String collegeName;

    /**
     * 目标专业名称
     */
    private String majorName;

    /**
     * 题型
     */
    @NotBlank(message = "题型不能为空")
    private String questionType;

    /**
     * 能力点标签
     */
    private String abilityTag;

    /**
     * 难度（easy/medium/hard）
     */
    private String difficulty;

    /**
     * 是否生成追问题
     */
    private Boolean generateFollowUp = false;

    /**
     * 是否生成评分标准
     */
    private Boolean generateScoringRule = true;

    /**
     * AI类型（可选，默认使用系统默认AI）
     */
    private String aiType;
}
