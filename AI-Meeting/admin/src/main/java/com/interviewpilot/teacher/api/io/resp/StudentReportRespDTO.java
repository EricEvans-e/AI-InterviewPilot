package com.interviewpilot.teacher.api.io.resp;

import lombok.Data;

import java.util.Date;

/**
 * 学生报告响应DTO（聚合多表数据）
 */
@Data
public class StudentReportRespDTO {

    private Long id;
    private String sessionId;
    private Long studentId;
    private String studentName;
    private String sessionTitle;
    private String collegeName;
    private String majorName;
    private Integer overallScore;
    private String status;
    private String content;
    private Integer adjustedScore;
    private Boolean isExcellentSample;
    private Boolean isModelMisjudge;
    private Date createTime;
}
