package com.interviewpilot.questionbank.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.common.convention.context.UserContext;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.questionbank.api.io.req.QuestionGenerateReqDTO;
import com.interviewpilot.questionbank.api.io.req.QuestionPageReqDTO;
import com.interviewpilot.questionbank.api.io.req.QuestionSaveReqDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionRespDTO;
import com.interviewpilot.questionbank.service.QuestionAiGenerateService;
import com.interviewpilot.questionbank.service.QuestionBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题库管理控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ip/v1/questions")
public class QuestionBankController {

    private final QuestionBankService questionBankService;
    private final QuestionAiGenerateService aiGenerateService;

    /**
     * 创建题目
     */
    @PostMapping
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<Long> createQuestion(@RequestBody @Valid QuestionSaveReqDTO req,
                                       @CurrentUser UserContext currentUser) {
        return Results.success(questionBankService.create(req, currentUser.getUserId()));
    }

    /**
     * 更新题目
     */
    @PutMapping("/{id}")
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<Void> updateQuestion(@PathVariable Long id,
                                       @RequestBody @Valid QuestionSaveReqDTO req) {
        questionBankService.update(id, req);
        return Results.success();
    }

    /**
     * 删除题目（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        questionBankService.removeById(id);
        return Results.success();
    }

    /**
     * 获取题目详情
     */
    @GetMapping("/{id}")
    public Result<QuestionRespDTO> getQuestion(@PathVariable Long id) {
        return Results.success(questionBankService.getDetail(id));
    }

    /**
     * 分页查询题目
     */
    @GetMapping("/page")
    public Result<PageInfo<QuestionRespDTO>> pageQuestions(QuestionPageReqDTO req) {
        return Results.success(questionBankService.pageByFilter(req));
    }

    /**
     * AI 生成题目
     */
    @PostMapping("/ai-generate")
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<List<QuestionRespDTO>> aiGenerate(@RequestBody @Valid QuestionGenerateReqDTO req) {
        List<QuestionRespDTO> result = aiGenerateService.generateQuestions(req).stream()
                .map(questionDO -> {
                    QuestionRespDTO respDTO = new QuestionRespDTO();
                    org.springframework.beans.BeanUtils.copyProperties(questionDO, respDTO);
                    return respDTO;
                })
                .toList();
        return Results.success(result);
    }

    /**
     * 更新题目状态
     */
    @PutMapping("/{id}/status")
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        questionBankService.updateStatus(id, status);
        return Results.success();
    }
}
