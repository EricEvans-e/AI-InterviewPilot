package com.interviewpilot.interview.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 面试会话-题目关联实体 (从题库抽取的题目序列)
 */
@Data
@TableName("interview_session_question")
public class InterviewSessionQuestionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 题目ID
     */
    @TableField("question_id")
    private Long questionId;

    /**
     * 题目序号(0-based)
     */
    @TableField("seq_index")
    private Integer seqIndex;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
}
