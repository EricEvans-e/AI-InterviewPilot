package com.interviewpilot.ai.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.interviewpilot.ai.service.AiConversationService;
import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.ai.api.io.req.AiConversationPageReqDTO;
import com.interviewpilot.ai.api.io.req.AiSessionCreateReqDTO;
import com.interviewpilot.ai.api.io.resp.AiConversationRespDTO;
import com.interviewpilot.ai.api.io.resp.AiSessionCreateRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI 对话管理控制器（通用多模型聊天）
 * 管理 AI 对话的生命周期：创建、查询、更新、删除、结束
 * 与 Agent 对话不同，这里支持多种 AI 模型（DeepSeek、Mimo、Doubao 等）
 */
@RestController
@RequestMapping("/api/ip/v1/ai/conversations")
@RequiredArgsConstructor
public class AiConversationController {

    private final AiConversationService aiConversationService;

    /**
     * 创建 AI 对话
     * 自动生成对话标题（基于首条消息），关联指定的 AI 模型
     *
     * @param requestParam 包含 aiId（AI模型ID）和 firstMessage（首条消息）
     * @param username     当前登录用户名
     * @return 对话 sessionId 和自动生成的标题
     */
    @PostMapping
    public Result<AiSessionCreateRespDTO> createConversation(@RequestBody AiSessionCreateReqDTO requestParam, @CurrentUser String username) {
        AiSessionCreateRespDTO result = aiConversationService.createConversationWithTitle(
                username,
                requestParam.getAiId(),
                requestParam.getFirstMessage()
        );
        return Results.success(result);
    }

    /**
     * 分页查询当前用户的 AI 对话列表
     */
    @GetMapping
    public Result<IPage<AiConversationRespDTO>> pageConversations(
            AiConversationPageReqDTO requestParam,
            @CurrentUser String username) {
        IPage<AiConversationRespDTO> result = aiConversationService.pageConversations(username, requestParam);
        return Results.success(result);
    }

    /**
     * 更新对话信息（消息数量、标题等）
     */
    @PutMapping("/{sessionId}")
    public Result<Void> updateConversation(@PathVariable String sessionId,
                                           @RequestParam(required = false) Integer messageCount,
                                           @RequestParam(required = false) String title,
                                           @CurrentUser String username) {
        aiConversationService.updateConversation(sessionId, messageCount, title, username);
        return Results.success();
    }

    /**
     * 结束对话（标记为已结束，不再接受新消息）
     */
    @PutMapping("/{sessionId}/end")
    public Result<Void> endConversation(@PathVariable String sessionId,
                                        @CurrentUser String username) {
        aiConversationService.endConversation(sessionId, username);
        return Results.success();
    }

    /**
     * 删除对话（逻辑删除）
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteConversation(@PathVariable String sessionId,
                                           @CurrentUser String username) {
        aiConversationService.deleteConversation(sessionId, username);
        return Results.success();
    }

    /**
     * 根据 sessionId 查询单个对话详情
     */
    @GetMapping("/{sessionId}")
    public Result<AiConversationRespDTO> getConversationById(@PathVariable String sessionId,
                                                             @CurrentUser String username) {
        AiConversationRespDTO result = aiConversationService.getConversationBySessionId(sessionId, username);
        return Results.success(result);
    }
}
