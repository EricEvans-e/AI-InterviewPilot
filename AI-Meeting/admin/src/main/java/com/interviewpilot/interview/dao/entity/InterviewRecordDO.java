package com.interviewpilot.interview.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 面试记录实体
 */
@Data
@TableName("interview_record")
public class InterviewRecordDO {

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 面试得分
     */
    @TableField("interview_score")
    private Integer interviewScore;

    /**
     * 简历得分
     */
    @TableField("resume_score")
    private Integer resumeScore;

    /**
     * 内容质量得分
     */
    @TableField("content_score")
    private Integer contentScore;

    /**
     * 逻辑结构得分
     */
    @TableField("logic_score")
    private Integer logicScore;

    /**
     * 专业匹配得分
     */
    @TableField("professional_score")
    private Integer professionalScore;

    /**
     * 语言表达得分
     */
    @TableField("expression_score")
    private Integer expressionScore;

    /**
     * 临场应变得分
     */
    @TableField("adaptability_score")
    private Integer adaptabilityScore;

    /**
     * 时间控制得分
     */
    @TableField("time_control_score")
    private Integer timeControlScore;

    /**
     * 礼仪仪态得分
     */
    @TableField("etiquette_score")
    private Integer etiquetteScore;

    /**
     * 综合得分
     */
    @TableField("composite_score")
    private Integer compositeScore;

    /**
     * 目标院校ID
     */
    @TableField("college_id")
    private Long collegeId;

    /**
     * 目标专业ID
     */
    @TableField("major_id")
    private Long majorId;

    /**
     * 会话模式: resume|questionBank
     */
    @TableField("session_mode")
    private String sessionMode;

    /**
     * 面试状态（INIT/IN_PROGRESS/FINISHED/EVALUATED）
     */
    @TableField("interview_status")
    private String interviewStatus;

    /**
     * 面试问题总数
     */
    @TableField("question_count")
    private Integer questionCount;

    /**
     * 面试会话智能体ID
     */
    @TableField("interviewer_agent_id")
    private Long interviewerAgentId;

    /**
     * 面试建议
     */
    @TableField("interview_suggestions")
    private String interviewSuggestions;

    /**
     * 面试方向
     */
    @TableField("interview_direction")
    private String interviewDirection;

    /**
     * 会话开始时间
     */
    @TableField("start_time")
    private Date startTime;

    /**
     * 会话结束时间
     */
    @TableField("end_time")
    private Date endTime;

    /**
     * 会话持续时长（秒）
     */
    @TableField("duration_seconds")
    private Integer durationSeconds;

    /**
     * 会话快照（JSON）
     */
    @TableField("session_snapshot_json")
    private String sessionSnapshotJson;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField("update_time")
    private Date updateTime;

    /**
     * 删除标识 0：未删除 1：已删除
     */
    @TableField("del_flag")
    private Integer delFlag;
}
