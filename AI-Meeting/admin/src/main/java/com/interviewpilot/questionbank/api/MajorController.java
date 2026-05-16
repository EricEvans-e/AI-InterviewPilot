package com.interviewpilot.questionbank.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.questionbank.api.io.req.MajorReqDTO;
import com.interviewpilot.questionbank.api.io.resp.MajorRespDTO;
import com.interviewpilot.questionbank.service.MajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专业管理控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ip/v1/majors")
public class MajorController {

    private final MajorService majorService;

    /**
     * 创建专业
     */
    @PostMapping
    @SaCheckRole("admin")
    public Result<Void> create(@RequestBody MajorReqDTO requestParam) {
        majorService.create(requestParam);
        return Results.success();
    }

    /**
     * 更新专业
     */
    @PutMapping("/{id}")
    @SaCheckRole("admin")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody MajorReqDTO requestParam) {
        requestParam.setId(id);
        majorService.update(requestParam);
        return Results.success();
    }

    /**
     * 删除专业（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @SaCheckRole("admin")
    public Result<Void> delete(@PathVariable("id") Long id) {
        majorService.delete(id);
        return Results.success();
    }

    /**
     * 获取专业详情
     */
    @GetMapping("/{id}")
    public Result<MajorRespDTO> getById(@PathVariable("id") Long id) {
        return Results.success(majorService.getByIdResp(id));
    }

    /**
     * 分页查询专业
     */
    @GetMapping("/page")
    public Result<PageInfo<MajorRespDTO>> page(MajorReqDTO requestParam) {
        return Results.success(majorService.getByPage(requestParam));
    }

    /**
     * 查询所有专业列表（可按院校ID筛选）
     */
    @GetMapping("/list")
    public Result<List<MajorRespDTO>> list(@RequestParam(value = "collegeId", required = false) Long collegeId) {
        if (collegeId != null) {
            return Results.success(majorService.listByCollegeId(collegeId));
        }
        return Results.success(majorService.listAll());
    }
}
