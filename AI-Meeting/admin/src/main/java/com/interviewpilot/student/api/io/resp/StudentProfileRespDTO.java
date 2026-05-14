package com.interviewpilot.student.api.io.resp;

import lombok.Data;

import java.util.List;

/**
 * 学生档案响应 DTO
 */
@Data
public class StudentProfileRespDTO {

    /**
     * 档案ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 年级
     */
    private String grade;

    /**
     * 考试类别
     */
    private String examCategory;

    /**
     * 培训阶段
     */
    private String trainingStage;

    /**
     * 目标院校ID列表
     */
    private List<Long> targetColleges;

    /**
     * 目标专业ID列表
     */
    private List<Long> targetMajors;
}
