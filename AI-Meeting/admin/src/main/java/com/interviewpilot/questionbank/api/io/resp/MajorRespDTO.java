package com.interviewpilot.questionbank.api.io.resp;

import lombok.Data;

import java.util.Date;

/**
 * 专业响应参数
 */
@Data
public class MajorRespDTO {

    private Long id;

    private Long collegeId;

    private String name;

    private String code;

    private String category;

    private String targetType;

    private String testForm;

    private String testContent;

    private String scoreStructure;

    private Integer year;

    private String officialUrl;

    private Date createTime;

    private Date updateTime;
}
