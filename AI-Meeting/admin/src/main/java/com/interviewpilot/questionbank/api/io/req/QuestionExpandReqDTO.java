package com.interviewpilot.questionbank.api.io.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * AI 拓题请求参数
 */
@Data
public class QuestionExpandReqDTO {

    /**
     * 参考题目 ID 列表
     */
    @NotEmpty(message = "参考题目不能为空")
    private List<Long> referenceQuestionIds;

    /**
     * 生成数量
     */
    @Min(value = 1, message = "生成数量至少为 1")
    private Integer count = 5;

    /**
     * 目标院校 ID
     */
    private Long collegeId;

    /**
     * 目标专业 ID
     */
    private Long majorId;

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
     * 难度
     */
    private String difficulty;

    /**
     * 是否生成参考答案
     */
    private Boolean generateReferenceAnswer = true;

    /**
     * 是否生成追问
     */
    private Boolean generateFollowUp = false;

    /**
     * 是否生成评分标准
     */
    private Boolean generateScoringRule = false;

    /**
     * AI 类型
     */
    private String aiType;

    /**
     * AI 配置 ID
     */
    private Long aiPropertiesId;
}
