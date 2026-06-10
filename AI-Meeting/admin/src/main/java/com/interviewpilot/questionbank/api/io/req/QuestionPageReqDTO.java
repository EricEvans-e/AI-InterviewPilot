package com.interviewpilot.questionbank.api.io.req;

import lombok.Data;

/**
 * 题目分页查询请求参数
 */
@Data
public class QuestionPageReqDTO {

    /**
     * 目标院校ID
     */
    private Long collegeId;

    /**
     * 目标专业ID
     */
    private Long majorId;

    /**
     * 题型
     */
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
     * 状态（draft|pending_review|approved|rejected）
     */
    private String status;

    /**
     * 题目标题关键字
     */
    private String titleKeyword;

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
