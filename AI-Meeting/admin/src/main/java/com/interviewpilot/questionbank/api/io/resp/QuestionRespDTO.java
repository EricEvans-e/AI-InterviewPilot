package com.interviewpilot.questionbank.api.io.resp;

import lombok.Data;

import java.util.Date;

/**
 * 题目响应参数
 */
@Data
public class QuestionRespDTO {

    private Long id;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 题目正文
     */
    private String content;

    /**
     * 题型
     */
    private String questionType;

    /**
     * 目标院校ID
     */
    private Long collegeId;

    private String collegeName;

    /**
     * 目标专业ID
     */
    private Long majorId;

    private String majorName;

    /**
     * 能力点标签
     */
    private String abilityTag;

    /**
     * 难度
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
     * 是否AI生成
     */
    private Boolean isAiGenerated;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 年份
     */
    private Integer year;

    private Date createTime;

    private Date updateTime;
}
