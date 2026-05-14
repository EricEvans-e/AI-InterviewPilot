package com.interviewpilot.ai.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.interviewpilot.ai.api.io.req.AiMessageReqDTO;
import com.interviewpilot.ai.api.io.resp.AiMessageHistoryRespDTO;
import com.interviewpilot.ai.service.AiMessageService;
import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 消息控制器（通用多模型聊天）
 * 提供 AI 对话的流式聊天（SSE）和历史消息查询
 * 支持 DeepSeek、Mimo、Doubao 等多种模型，通过 sessionId 关联到对应的 AI 模型配置
 */
@RestController
@RequestMapping("/api/ip/v1/ai")
@RequiredArgsConstructor
public class AiMessageController {

    private final AiMessageService aiMessageService;

    /**
     * AI 流式聊天（SSE）
     * 使用 Server-Sent Events 实现流式输出，前端可逐字展示 AI 回复
     * 支持 thinking 模型的推理过程展示（reasoning_content）
     *
     * @param sessionId    对话sessionId
     * @param requestParam 用户消息内容
     * @param username     当前登录用户名
     * @return SSE 流，每个事件是一个 JSON 字符串（包含 type 和 content）
     */
    @PostMapping(value = "/sessions/{sessionId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@PathVariable String sessionId,
                             @RequestBody AiMessageReqDTO requestParam,
                             @CurrentUser String username,
                             HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Cache-Control");
        requestParam.setSessionId(sessionId);
        return aiMessageService.aiChatFlux(requestParam, username);
    }

    /**
     * 查询对话的完整消息历史
     */
    @GetMapping("/history/{sessionId}")
    public Result<List<AiMessageHistoryRespDTO>> getConversationHistory(@PathVariable String sessionId,
                                                                        @CurrentUser String username) {
        List<AiMessageHistoryRespDTO> result = aiMessageService.getConversationHistory(sessionId, username);
        return Results.success(result);
    }

    /**
     * 分页查询历史消息
     */
    @GetMapping("/history/page")
    public Result<IPage<AiMessageHistoryRespDTO>> pageHistoryMessages(
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @CurrentUser String username) {
        IPage<AiMessageHistoryRespDTO> result = aiMessageService.pageHistoryMessages(sessionId, current, size, username);
        return Results.success(result);
    }
}
