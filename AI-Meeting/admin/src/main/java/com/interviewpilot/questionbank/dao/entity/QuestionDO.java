package com.interviewpilot.questionbank.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.interviewpilot.common.database.BaseDO;
import lombok.Data;

/**
 * 题目持久层实体
 */
@Data
@TableName("question")
public class QuestionDO extends BaseDO {

    @TableId(type = IdType.AUTO)
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
     * 题型（情景题、专业题、开放题等）
     */
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
     * 是否AI生成
     */
    private Boolean isAiGenerated;

    /**
     * 状态（draft|pending_review|approved|rejected）
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
}
