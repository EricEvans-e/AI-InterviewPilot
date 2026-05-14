package com.interviewpilot.student.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.interviewpilot.common.database.BaseDO;
import lombok.Data;

/**
 * 学生档案持久层实体
 */
@Data
@TableName("student_profile")
public class StudentProfileDO extends BaseDO {

    @TableId(type = IdType.AUTO)
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
}
