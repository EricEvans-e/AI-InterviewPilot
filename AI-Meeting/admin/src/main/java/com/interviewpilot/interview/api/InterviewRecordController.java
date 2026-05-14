package com.interviewpilot.interview.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.common.convention.context.UserContext;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.interview.api.io.req.InterviewRecordPageReqDTO;
import com.interviewpilot.interview.api.io.req.InterviewRecordSaveReqDTO;
import com.interviewpilot.interview.api.io.resp.InterviewRecordRespDTO;
import com.interviewpilot.interview.service.InterviewRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面试记录控制器
 * 提供面试报告的保存、查询、分页等接口
 * 所有接口均需要登录（通过 Sa-Token 从请求头解析当前用户）
 */
@RestController
@RequestMapping("/api/ip/v1/interview")
@RequiredArgsConstructor
public class InterviewRecordController {

    private final InterviewRecordService interviewRecordService;

    /**
     * 保存面试记录
     * 面试结束后，前端调用此接口将评分、反馈等数据持久化到数据库
     *
     * @param requestParam 包含 sessionId 和评分数据
     * @param currentUser  当前登录用户（由 @CurrentUser 注解自动注入）
     */
    @PostMapping("/interview/record")
    public Result<Void> saveInterviewRecord(
            @Valid @RequestBody InterviewRecordSaveReqDTO requestParam,
            @CurrentUser UserContext currentUser) {
        interviewRecordService.saveInterviewRecord(requestParam.getSessionId(), currentUser.getUserId(), requestParam);
        return Results.success();
    }

    /**
     * 分页查询面试记录列表
     * 支持按时间排序、关键词搜索等条件筛选
     *
     * @param requestParam 分页参数（current、size、排序方式等）
     * @param currentUser  当前登录用户
     * @return 分页结果，包含总条数和当前页数据
     */
    @GetMapping("/interview/records")
    public Result<IPage<InterviewRecordRespDTO>> pageInterviewRecords(
            InterviewRecordPageReqDTO requestParam,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewRecordService.pageInterviewRecords(currentUser.getUserId(), requestParam));
    }

    /**
     * 根据会话ID查询单条面试记录
     * 用于查看某次面试的详细报告（含评分、反馈、追问记录等）
     *
     * @param sessionId   面试会话ID
     * @param currentUser 当前登录用户（用于校验数据归属，防止越权访问）
     */
    @GetMapping("/interview/record/{sessionId}")
    public Result<InterviewRecordRespDTO> getInterviewRecordBySessionId(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewRecordService.getBySessionId(sessionId, currentUser.getUserId()));
    }

    /**
     * 从 Redis 快照同步面试记录到数据库
     * 面试过程中的运行时数据存储在 Redis，此接口将其落盘持久化
     * 通常在面试结束或用户主动保存时调用
     *
     * @param sessionId   面试会话ID
     * @param currentUser 当前登录用户
     */
    @PostMapping("/interview/record/save-from-redis/{sessionId}")
    public Result<Void> saveInterviewRecordFromRedis(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        interviewRecordService.saveInterviewRecordFromRedis(sessionId, currentUser.getUserId());
        return Results.success();
    }
}
