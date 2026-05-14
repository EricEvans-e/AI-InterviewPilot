package com.interviewpilot.questionbank.api.io.req;

import lombok.Data;

/**
 * 院校请求参数
 */
@Data
public class CollegeReqDTO {

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
     * 院校类型
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
     * 办学层次
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

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
