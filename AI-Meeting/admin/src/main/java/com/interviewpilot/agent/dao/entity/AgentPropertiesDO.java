package com.interviewpilot.agent.dao.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName agent_properties
 */
@Data
@TableName("agent_properties")
public class AgentPropertiesDO {
    /**
     * ID
     */
    private Long id;

    /**
     * 智能体名称
     */
    private String agentName;

    /**
     * 鉴权密钥
     */
    private String apiSecret;

    /**
     * 鉴权key
     */
    private String apiKey;

    /**
     * 工作流id（xingchen 模式）或 API URL（mimo 模式）
     */
    private String apiFlowId;

    /**
     * AI 提供商标识: xingchen（默认）, mimo
     */
    private String aiProvider;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 删除标识 0：未删除 1：已删除
     */
    private Integer delFlag;

}