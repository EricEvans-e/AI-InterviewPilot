package com.interviewpilot.teacher.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.interviewpilot.common.database.BaseDO;
import lombok.Data;

/**
 * 教师点评实体
 */
@Data
@TableName("teacher_review")
public class TeacherReviewDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 面试会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 教师用户ID
     */
    @TableField("teacher_id")
    private Long teacherId;

    /**
     * 学生用户ID
     */
    @TableField("student_id")
    private Long studentId;

    /**
     * 点评内容
     */
    @TableField("content")
    private String content;

    /**
     * 调整后分数
     */
    @TableField("adjusted_score")
    private Integer adjustedScore;

    /**
     * 是否标记为优秀样例
     */
    @TableField("is_excellent_sample")
    private Boolean isExcellentSample;

    /**
     * 是否标记为模型误判
     */
    @TableField("is_model_misjudge")
    private Boolean isModelMisjudge;
}
