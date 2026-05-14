package com.interviewpilot.ai.service.chat;

import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.ai.api.io.resp.AiMessageHistoryRespDTO;
import com.interviewpilot.toolkit.xunfei.AIContentAccumulator;
import reactor.core.publisher.FluxSink;

import java.util.List;

public interface AiChatHandler {
    String getType();

    void streamToSink(AiPropertiesDO aiProperties, String userMessage, List<AiMessageHistoryRespDTO> historyMessages,
                      FluxSink<String> sink, AIContentAccumulator accumulator) throws Exception;

    /**
     * 同步调用 AI（非流式）
     */
    default String callSync(AiPropertiesDO aiProperties, String userMessage) {
        throw new UnsupportedOperationException("该 AI Handler 不支持同步调用");
    }
}

