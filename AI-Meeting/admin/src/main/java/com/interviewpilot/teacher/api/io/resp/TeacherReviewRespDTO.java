package com.interviewpilot.teacher.api.io.resp;

import lombok.Data;

import java.util.Date;

/**
 * 教师点评响应参数
 */
@Data
public class TeacherReviewRespDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 面试会话ID
     */
    private String sessionId;

    /**
     * 教师用户ID
     */
    private Long teacherId;

    /**
     * 学生用户ID
     */
    private Long studentId;

    /**
     * 点评内容
     */
    private String content;

    /**
     * 调整后分数
     */
    private Integer adjustedScore;

    /**
     * 是否标记为优秀样例
     */
    private Boolean isExcellentSample;

    /**
     * 是否标记为模型误判
     */
    private Boolean isModelMisjudge;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
