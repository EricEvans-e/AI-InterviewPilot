package com.interviewpilot.common.config.web;

import com.interviewpilot.common.config.storage.ApplicationStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ApplicationStorageProperties storageProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // 允许所有域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 明确允许的HTTP方法
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(true) // 允许携带凭证
                .maxAge(3600); // 预检请求缓存时间
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String recordingDir = storageProperties.getRecordingPath().toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/recordings/**")
                .addResourceLocations("file:" + recordingDir);

        String agentFileDir = storageProperties.getAgentFilePath().toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/agent-files/**")
                .addResourceLocations("file:" + agentFileDir);
    }
}
