package com.interviewpilot.common.config.mimo;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "interview-pilot.security", name = "require-mimo-secrets", havingValue = "true")
public class MimoSecretStartupValidator {

    private final MimoProperties properties;

    @PostConstruct
    public void validate() {
        if (StrUtil.isBlank(properties.getApiKey())) {
            throw new IllegalStateException("Mimo API key is required: mimo.api-key or MIMO_API_KEY");
        }
    }
}
