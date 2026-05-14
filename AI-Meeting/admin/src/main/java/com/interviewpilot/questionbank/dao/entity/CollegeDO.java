package com.interviewpilot.questionbank.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.interviewpilot.common.database.BaseDO;
import lombok.Data;

/**
 * 院校持久层实体
 */
@Data
@TableName("college")
public class CollegeDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 院校名称
     */
    private String name;

    /**
     * 院校代码
     */
    private String code;

    /**
     * 院校类型（综合、理工、师范等）
     */
    private String type;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 办学层次（本科、专科等）
     */
    private String level;

    /**
     * 官方网址
     */
    private String officialUrl;

    /**
     * 备注
     */
    private String remark;
}
