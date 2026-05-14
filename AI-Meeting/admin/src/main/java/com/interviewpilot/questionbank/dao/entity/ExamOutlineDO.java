package com.interviewpilot.questionbank.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.interviewpilot.common.database.BaseDO;
import lombok.Data;

/**
 * 考试大纲持久层实体
 */
@Data
@TableName("exam_outline")
public class ExamOutlineDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属院校ID
     */
    private Long collegeId;

    /**
     * 所属专业ID
     */
    private Long majorId;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 大纲标题
     */
    private String title;

    /**
     * 文档类型
     */
    private String docType;

    /**
     * 大纲内容
     */
    private String content;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 来源URL
     */
    private String sourceUrl;

    /**
     * 状态
     */
    private String status;

    /**
     * 上传者ID
     */
    private Long uploaderId;
}
