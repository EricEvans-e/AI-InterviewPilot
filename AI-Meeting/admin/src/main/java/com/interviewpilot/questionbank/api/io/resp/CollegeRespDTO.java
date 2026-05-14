package com.interviewpilot.questionbank.api.io.resp;

import lombok.Data;

import java.util.Date;

/**
 * 院校响应参数
 */
@Data
public class CollegeRespDTO {

    private Long id;

    private String name;

    private String code;

    private String type;

    private String province;

    private String city;

    private String level;

    private String officialUrl;

    private String remark;

    private Date createTime;

    private Date updateTime;
}
