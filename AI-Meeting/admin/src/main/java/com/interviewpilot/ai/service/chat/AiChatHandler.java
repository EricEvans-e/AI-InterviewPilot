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
}

