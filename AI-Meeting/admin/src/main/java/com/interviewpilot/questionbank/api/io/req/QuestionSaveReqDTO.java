package com.interviewpilot.questionbank.api.io.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 题目创建/更新请求参数
 */
@Data
public class QuestionSaveReqDTO {

    /**
     * 题目标题
     */
    @NotBlank(message = "题目标题不能为空")
    private String title;

    /**
     * 题目正文
     */
    @NotBlank(message = "题目内容不能为空")
    private String content;

    /**
     * 题型（情景题、专业题、开放题等）
     */
    @NotBlank(message = "题型不能为空")
    private String questionType;

    /**
     * 目标院校ID
     */
    private Long collegeId;

    /**
     * 目标专业ID
     */
    private Long majorId;

    /**
     * 能力点标签
     */
    private String abilityTag;

    /**
     * 难度（easy/medium/hard）
     */
    private String difficulty;

    /**
     * 建议答题时间（秒）
     */
    private Integer answerTimeSeconds;

    /**
     * 参考答案/答题要点
     */
    private String referenceAnswer;

    /**
     * 评分规则（JSON字符串）
     */
    private String scoringRule;

    /**
     * 追问规则（JSON字符串）
     */
    private String followUpRule;

    /**
     * 预设追问题（JSON数组字符串）
     */
    private String followUpQuestions;

    /**
     * 来源依据说明
     */
    private String sourceRef;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 是否AI生成
     */
    private Boolean isAiGenerated;

    /**
     * 状态（draft/pending_review/approved/rejected）
     */
    private String status;
}
