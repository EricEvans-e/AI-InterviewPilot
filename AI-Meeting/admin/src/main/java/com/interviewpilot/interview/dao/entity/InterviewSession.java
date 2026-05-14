package com.interviewpilot.interview.dao.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "interview_session")
public class InterviewSession {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionId;

    @Indexed
    private Long userId;

    @Indexed
    private String status;

    private String conversationTitle;

    private Long interviewerAgentId;

    private String resumeFileUrl;

    private String interviewType;

    /**
     * 会话模式: resume(简历出题) | questionBank(题库抽题)
     */
    private String sessionMode;

    /**
     * 题库模式下的目标院校ID
     */
    private Long collegeId;

    /**
     * 题库模式下的目标专业ID
     */
    private Long majorId;

    /**
     * 题库模式下的面试类型: 结构化|半结构化|专业认知|综合素质
     */
    private String interviewMode;

    /**
     * 题库模式下的题目ID列表(JSON)
     */
    private String questionIds;

    private Date startTime;

    private Date endTime;

    @CreatedDate
    private Date createTime;

    @LastModifiedDate
    private Date updateTime;

    private Integer delFlag;
}
