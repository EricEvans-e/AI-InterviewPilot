package com.interviewpilot.ai.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.interviewpilot.ai.service.AiPropertiesService;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.ai.api.io.req.AiPropertiesCreateReqDTO;
import com.interviewpilot.ai.api.io.req.AiPropertiesPageReqDTO;
import com.interviewpilot.ai.api.io.req.AiPropertiesUpdateReqDTO;
import com.interviewpilot.ai.api.io.resp.AiModelOptionRespDTO;
import com.interviewpilot.ai.api.io.resp.AiPropertiesRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 模型配置管理控制器
 * 管理通用对话使用的 AI 模型（DeepSeek、Mimo、Doubao 等）的增删改查
 * 对应数据库 ai_properties 表，控制 AI 对话页可用的模型列表
 */
@RestController
@RequestMapping("/api/ip/v1/ai-properties")
@RequiredArgsConstructor
public class AiPropertiesController {

    private final AiPropertiesService aiPropertiesService;

    /**
     * 获取所有可用AI模型（用于前端下拉列表）
     */
    @GetMapping("/options")
    public Result<List<AiModelOptionRespDTO>> getAvailableAiModels() {
        return Results.success(aiPropertiesService.getAvailableAiModels());
    }

    /**
     * 新增 AI 模型配置
     */
    @PostMapping
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<Void> createAiProperties(@RequestBody AiPropertiesCreateReqDTO requestParam) {
        aiPropertiesService.createAiProperties(requestParam);
        return Results.success();
    }

    /**
     * 更新 AI 模型配置
     */
    @PutMapping
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<Void> updateAiProperties(@RequestBody AiPropertiesUpdateReqDTO requestParam) {
        aiPropertiesService.updateAiProperties(requestParam);
        return Results.success();
    }

    /**
     * 删除 AI 模型配置
     */
    @DeleteMapping("/{id}")
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<Void> deleteAiProperties(@PathVariable Long id) {
        aiPropertiesService.deleteAiProperties(id);
        return Results.success();
    }

    /**
     * 根据 ID 查询 AI 模型配置详情
     */
    @GetMapping("/{id}")
    public Result<AiPropertiesRespDTO> getAiPropertiesById(@PathVariable Long id) {
        AiPropertiesRespDTO result = aiPropertiesService.getAiPropertiesById(id);
        return Results.success(result);
    }

    /**
     * 分页查询 AI 模型配置列表
     */
    @GetMapping
    public Result<IPage<AiPropertiesRespDTO>> pageAiProperties(AiPropertiesPageReqDTO requestParam) {
        IPage<AiPropertiesRespDTO> result = aiPropertiesService.pageAiProperties(requestParam);
        return Results.success(result);
    }

    /**
     * 获取所有已启用的 AI 模型（前端下拉列表用）
     */
    @GetMapping("/enabled")
    public Result<List<AiPropertiesRespDTO>> getAllEnabledAiProperties() {
        List<AiPropertiesRespDTO> result = aiPropertiesService.getAllEnabledAiProperties();
        return Results.success(result);
    }

    /**
     * 切换 AI 模型启用/禁用状态
     */
    @PutMapping("/{id}/status")
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<Void> toggleAiPropertiesStatus(@PathVariable Long id, @RequestParam Integer isEnabled) {
        aiPropertiesService.toggleAiPropertiesStatus(id, isEnabled);
        return Results.success();
    }

    /**
     * 设为同类型默认模型
     */
    @PutMapping("/{id}/default")
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<Void> setDefaultAiProperties(@PathVariable Long id) {
        aiPropertiesService.setDefaultAiProperties(id);
        return Results.success();
    }
}
