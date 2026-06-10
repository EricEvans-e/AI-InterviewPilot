package com.interviewpilot.questionbank.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.common.convention.context.UserContext;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.questionbank.api.io.req.QuestionGenerateReqDTO;
import com.interviewpilot.questionbank.api.io.req.QuestionImportReqDTO;
import com.interviewpilot.questionbank.api.io.req.QuestionPageReqDTO;
import com.interviewpilot.questionbank.api.io.req.QuestionSaveReqDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionCoverageRespDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionImportRespDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionRespDTO;
import com.interviewpilot.questionbank.service.QuestionAiGenerateService;
import com.interviewpilot.questionbank.service.QuestionBankService;
import com.interviewpilot.questionbank.service.QuestionImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final QuestionImportService questionImportService;

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

    @PostMapping("/import")
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<QuestionImportRespDTO> importQuestions(@RequestParam("file") MultipartFile file,
                                                        @RequestParam(required = false, defaultValue = "word_table") String importType,
                                                        @RequestParam(required = false) Long collegeId,
                                                        @RequestParam(required = false) Long majorId,
                                                        @RequestParam(required = false) String defaultQuestionType,
                                                        @RequestParam(required = false, defaultValue = "medium") String defaultDifficulty,
                                                        @RequestParam(required = false, defaultValue = "2026") Integer defaultYear,
                                                        @RequestParam(required = false, defaultValue = "pending_review") String statusAfterImport,
                                                        @RequestParam(required = false, defaultValue = "true") Boolean dryRun,
                                                        @CurrentUser UserContext currentUser) {
        QuestionImportReqDTO request = QuestionImportReqDTO.builder()
                .importType(importType)
                .collegeId(collegeId)
                .majorId(majorId)
                .defaultQuestionType(defaultQuestionType)
                .defaultDifficulty(defaultDifficulty)
                .defaultYear(defaultYear)
                .statusAfterImport(statusAfterImport)
                .dryRun(dryRun)
                .build();
        return Results.success(questionImportService.preview(file, request, currentUser.getUserId()));
    }

    @PostMapping("/import/{batchId}/confirm")
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<QuestionImportRespDTO> confirmImport(@PathVariable String batchId,
                                                      @CurrentUser UserContext currentUser) {
        return Results.success(questionImportService.confirm(batchId, currentUser.getUserId()));
    }

    @GetMapping("/import/{batchId}")
    @SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)
    public Result<QuestionImportRespDTO> getImportBatch(@PathVariable String batchId) {
        return Results.success(questionImportService.getBatch(batchId));
    }

    @GetMapping("/coverage")
    public Result<QuestionCoverageRespDTO> coverage(@RequestParam(required = false) Long collegeId,
                                                    @RequestParam(required = false) Long majorId,
                                                    @RequestParam(required = false) String interviewMode,
                                                    @RequestParam(required = false, defaultValue = "5") Integer requiredCount) {
        return Results.success(questionBankService.coverage(collegeId, majorId, interviewMode, requiredCount));
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
