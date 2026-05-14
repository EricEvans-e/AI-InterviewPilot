package com.interviewpilot.questionbank.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.questionbank.api.io.req.CollegeReqDTO;
import com.interviewpilot.questionbank.api.io.resp.CollegeRespDTO;
import com.interviewpilot.questionbank.service.CollegeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 院校管理控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ip/v1/colleges")
public class CollegeController {

    private final CollegeService collegeService;

    /**
     * 创建院校
     */
    @PostMapping
    @SaCheckRole("admin")
    public Result<Void> create(@RequestBody CollegeReqDTO requestParam) {
        collegeService.create(requestParam);
        return Results.success();
    }

    /**
     * 更新院校
     */
    @PutMapping("/{id}")
    @SaCheckRole("admin")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody CollegeReqDTO requestParam) {
        requestParam.setId(id);
        collegeService.update(requestParam);
        return Results.success();
    }

    /**
     * 删除院校（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @SaCheckRole("admin")
    public Result<Void> delete(@PathVariable("id") Long id) {
        collegeService.delete(id);
        return Results.success();
    }

    /**
     * 获取院校详情
     */
    @GetMapping("/{id}")
    public Result<CollegeRespDTO> getById(@PathVariable("id") Long id) {
        return Results.success(collegeService.getByIdResp(id));
    }

    /**
     * 分页查询院校
     */
    @GetMapping("/page")
    public Result<PageInfo<CollegeRespDTO>> page(CollegeReqDTO requestParam) {
        return Results.success(collegeService.getByPage(requestParam));
    }

    /**
     * 查询所有院校列表
     */
    @GetMapping("/list")
    public Result<List<CollegeRespDTO>> list() {
        return Results.success(collegeService.listAll());
    }

    /**
     * 根据省份查询院校列表
     */
    @GetMapping("/list/province")
    public Result<List<CollegeRespDTO>> listByProvince(@RequestParam("province") String province) {
        return Results.success(collegeService.listByProvince(province));
    }
}
