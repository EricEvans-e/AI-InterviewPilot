package com.interviewpilot.ai.enums;

import lombok.Getter;

@Getter
public enum AiPropritiesType {

    OPENAI(1, "openai", "Mimo OpenAI Compatible", "https://token-plan-cn.xiaomimimo.com/v1"),
    ANTHROPIC(5, "anthropic", "Mimo Anthropic Compatible", "https://token-plan-cn.xiaomimimo.com/anthropic"),
    OTHER(99, "other", "Other", "");

    private final Integer code;
    private final String type;
    private final String desc;
    private final String defaultBaseUrl;

    AiPropritiesType(Integer code, String type, String desc, String defaultBaseUrl) {
        this.code = code;
        this.type = type;
        this.desc = desc;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    public static AiPropritiesType getByCode(Integer code) {
        for (AiPropritiesType type : AiPropritiesType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return OTHER;
    }

    public static AiPropritiesType getByType(String type) {
        if (type == null) {
            return OTHER;
        }
        if ("generalv3.5".equalsIgnoreCase(type)
                || "spark".equalsIgnoreCase(type)
                || "doubao".equalsIgnoreCase(type)
                || "deepseek".equalsIgnoreCase(type)) {
            return OPENAI;
        }
        for (AiPropritiesType t : AiPropritiesType.values()) {
            if (t.getType().equalsIgnoreCase(type)) {
                return t;
            }
        }
        return OTHER;
    }

    public static boolean isSupported(String type) {
        return getByType(type) != OTHER;
    }
}
