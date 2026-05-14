package com.interviewpilot.questionbank.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.interviewpilot.common.database.BaseDO;
import lombok.Data;

/**
 * 专业持久层实体
 */
@Data
@TableName("major")
public class MajorDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属院校ID
     */
    private Long collegeId;

    /**
     * 专业名称
     */
    private String name;

    /**
     * 专业代码
     */
    private String code;

    /**
     * 专业类别
     */
    private String category;

    /**
     * 招生对象类型
     */
    private String targetType;

    /**
     * 考试形式
     */
    private String testForm;

    /**
     * 考试内容
     */
    private String testContent;

    /**
     * 分数结构
     */
    private String scoreStructure;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 官方网址
     */
    private String officialUrl;
}
