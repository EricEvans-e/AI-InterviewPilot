package com.interviewpilot.common.config.iflytek;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * iFlytek config类
 * 从application.yml中读取xunfei配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "xunfei.lat-key")
public class XunfeiLatProperties {
    
    /**
     * iFlytek App ID
     */
    private String appId;
    
    /**
     * iFlytek API Key
     */
    private String apiKey;
    
    /**
     * iFlytek API Secret
     */
    private String apiSecret;
}