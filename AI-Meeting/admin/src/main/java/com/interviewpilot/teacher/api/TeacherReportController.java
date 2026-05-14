package com.interviewpilot.teacher.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.common.convention.context.UserContext;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.interview.dao.entity.InterviewRecordDO;
import com.interviewpilot.interview.service.InterviewRecordService;
import com.interviewpilot.teacher.api.io.req.TeacherReviewSaveReqDTO;
import com.interviewpilot.teacher.dao.entity.TeacherReviewDO;
import com.interviewpilot.teacher.service.TeacherReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师点评控制器
 */
@RestController
@RequestMapping("/api/ip/v1/teacher")
@RequiredArgsConstructor
@SaCheckRole("teacher")
public class TeacherReportController {

    private final TeacherReviewService teacherReviewService;
    private final InterviewRecordService interviewRecordService;

    /**
     * 获取学生的面试记录列表
     */
    @GetMapping("/students/{studentId}/records")
    public Result<IPage<InterviewRecordDO>> getStudentRecords(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Results.success(interviewRecordService.pageByStudent(studentId, pageNum, pageSize));
    }

    /**
     * 创建教师点评
     */
    @PostMapping("/sessions/{sessionId}/review")
    public Result<Long> createReview(@PathVariable String sessionId,
                                     @RequestBody @Valid TeacherReviewSaveReqDTO req,
                                     @CurrentUser UserContext currentUser) {
        return Results.success(teacherReviewService.createReview(
                currentUser.getUserId(), sessionId, req));
    }

    /**
     * 获取指定会话的所有点评
     */
    @GetMapping("/sessions/{sessionId}/reviews")
    public Result<List<TeacherReviewDO>> getSessionReviews(@PathVariable String sessionId) {
        return Results.success(teacherReviewService.getReviewsBySession(sessionId));
    }

    /**
     * 分页获取指定学生的点评
     */
    @GetMapping("/students/{studentId}/reviews")
    public Result<IPage<TeacherReviewDO>> getStudentReviews(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Results.success(teacherReviewService.getReviewsByStudent(studentId, pageNum, pageSize));
    }

    /**
     * 教师查看自己点评过的学生报告列表
     */
    @GetMapping("/reports")
    public Result<IPage<TeacherReviewDO>> getStudentReportList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @CurrentUser UserContext currentUser) {
        return Results.success(teacherReviewService.getStudentReportList(
                currentUser.getUserId(), pageNum, pageSize));
    }
}
