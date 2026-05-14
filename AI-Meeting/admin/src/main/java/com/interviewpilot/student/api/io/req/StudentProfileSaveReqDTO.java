package com.interviewpilot.student.api.io.req;

import lombok.Data;

import java.util.List;

/**
 * 学生档案保存请求 DTO
 */
@Data
public class StudentProfileSaveReqDTO {

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
    private List<Long> collegeIds;

    /**
     * 目标专业ID列表
     */
    private List<Long> majorIds;
}
