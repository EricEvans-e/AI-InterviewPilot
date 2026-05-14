package com.interviewpilot.questionbank.api.io.req;

import lombok.Data;

/**
 * 考试大纲请求参数
 */
@Data
public class ExamOutlineReqDTO {

    private Long id;

    /**
     * 所属院校ID
     */
    private Long collegeId;

    /**
     * 所属专业ID
     */
    private Long majorId;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 大纲标题
     */
    private String title;

    /**
     * 文档类型
     */
    private String docType;

    /**
     * 大纲内容
     */
    private String content;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 来源URL
     */
    private String sourceUrl;

    /**
     * 状态
     */
    private String status;

    /**
     * 上传者ID
     */
    private Long uploaderId;

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
