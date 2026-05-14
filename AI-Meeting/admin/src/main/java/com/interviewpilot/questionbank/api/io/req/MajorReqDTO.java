package com.interviewpilot.questionbank.api.io.req;

import lombok.Data;

/**
 * 专业请求参数
 */
@Data
public class MajorReqDTO {

    private Long id;

    /**
     * 所属院校ID
     */
    private Long collegeId;

    /**
     * 专业名称
     */
    private String name;

    /**
     * 专业代码
     */
    private String code;

    /**
     * 专业类别
     */
    private String category;

    /**
     * 招生对象类型
     */
    private String targetType;

    /**
     * 考试形式
     */
    private String testForm;

    /**
     * 考试内容
     */
    private String testContent;

    /**
     * 分数结构
     */
    private String scoreStructure;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 官方网址
     */
    private String officialUrl;

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
