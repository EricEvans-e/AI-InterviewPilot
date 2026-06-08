package com.interviewpilot.common.config.mimo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mimo")
public class MimoProperties {

    public static final String DEFAULT_OPENAI_BASE_URL = "https://token-plan-cn.xiaomimimo.com/v1";
    public static final String DEFAULT_ANTHROPIC_BASE_URL = "https://token-plan-cn.xiaomimimo.com/anthropic";

    private String apiKey;

    private String openaiBaseUrl = DEFAULT_OPENAI_BASE_URL;

    private String anthropicBaseUrl = DEFAULT_ANTHROPIC_BASE_URL;

    private String chatModel = "mimo-v2.5";

    private String proModel = "mimo-v2.5-pro";

    private String asrModel = "mimo-v2.5-asr";

    private String ttsModel = "mimo-v2.5-tts";

    private String ttsVoice = "Chloe";

    private String ttsFormat = "wav";

    private String asrLanguage = "auto";

    private Integer pcmSampleRate = 16000;

    private Integer pcmChannels = 1;

    private Integer pcmBitsPerSample = 16;
}
