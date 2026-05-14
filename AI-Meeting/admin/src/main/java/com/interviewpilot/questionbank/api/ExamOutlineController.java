package com.interviewpilot.questionbank.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.questionbank.api.io.req.ExamOutlineReqDTO;
import com.interviewpilot.questionbank.api.io.resp.ExamOutlineRespDTO;
import com.interviewpilot.questionbank.service.ExamOutlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考试大纲管理控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ip/v1/exam-outlines")
public class ExamOutlineController {

    private final ExamOutlineService examOutlineService;

    /**
     * 创建考试大纲
     */
    @PostMapping
    @SaCheckRole("admin")
    public Result<Void> create(@RequestBody ExamOutlineReqDTO requestParam) {
        examOutlineService.create(requestParam);
        return Results.success();
    }

    /**
     * 更新考试大纲
     */
    @PutMapping("/{id}")
    @SaCheckRole("admin")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody ExamOutlineReqDTO requestParam) {
        requestParam.setId(id);
        examOutlineService.update(requestParam);
        return Results.success();
    }

    /**
     * 删除考试大纲（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @SaCheckRole("admin")
    public Result<Void> delete(@PathVariable("id") Long id) {
        examOutlineService.delete(id);
        return Results.success();
    }

    /**
     * 获取考试大纲详情
     */
    @GetMapping("/{id}")
    public Result<ExamOutlineRespDTO> getById(@PathVariable("id") Long id) {
        return Results.success(examOutlineService.getByIdResp(id));
    }

    /**
     * 分页查询考试大纲
     */
    @GetMapping("/page")
    public Result<PageInfo<ExamOutlineRespDTO>> page(ExamOutlineReqDTO requestParam) {
        return Results.success(examOutlineService.getByPage(requestParam));
    }

    /**
     * 查询所有考试大纲列表
     */
    @GetMapping("/list")
    public Result<List<ExamOutlineRespDTO>> list() {
        return Results.success(examOutlineService.listAll());
    }
}
