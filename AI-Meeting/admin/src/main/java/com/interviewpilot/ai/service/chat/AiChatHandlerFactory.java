package com.interviewpilot.ai.service.chat;

import cn.hutool.core.util.StrUtil;
import com.interviewpilot.ai.enums.AiPropritiesType;
import org.springframework.stereotype.Component;

/**
 * AI聊天处理器工厂
 * 根据 aiType 路由到对应的 Handler
 */
@Component
public class AiChatHandlerFactory {

    private final UniversalAiChatHandler universalAiChatHandler;
    private final AnthropicChatHandler anthropicChatHandler;

    public AiChatHandlerFactory(UniversalAiChatHandler universalAiChatHandler, AnthropicChatHandler anthropicChatHandler) {
        this.universalAiChatHandler = universalAiChatHandler;
        this.anthropicChatHandler = anthropicChatHandler;
    }

    public AiChatHandler getHandler(String aiType) {
        if (StrUtil.isBlank(aiType)) {
            return null;
        }
        // Anthropic 协议，走专用 handler
        if (AiPropritiesType.ANTHROPIC.getType().equalsIgnoreCase(aiType)) {
            return anthropicChatHandler;
        }
        // 通用处理器支持所有兼容OpenAI协议的模型
        if (universalAiChatHandler.supports(aiType)) {
            return universalAiChatHandler;
        }
        return null;
    }
}
