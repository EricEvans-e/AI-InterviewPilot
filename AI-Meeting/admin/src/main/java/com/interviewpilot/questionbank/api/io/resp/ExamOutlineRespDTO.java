package com.interviewpilot.questionbank.api.io.resp;

import lombok.Data;

import java.util.Date;

/**
 * 考试大纲响应参数
 */
@Data
public class ExamOutlineRespDTO {

    private Long id;

    private Long collegeId;

    private Long majorId;

    private Integer year;

    private String title;

    private String docType;

    private String content;

    private String fileUrl;

    private String sourceUrl;

    private String status;

    private Long uploaderId;

    private Date createTime;

    private Date updateTime;
}
