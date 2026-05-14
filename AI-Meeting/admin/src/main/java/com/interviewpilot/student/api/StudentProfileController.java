package com.interviewpilot.student.api;

import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.common.convention.context.UserContext;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.student.api.io.req.StudentProfileSaveReqDTO;
import com.interviewpilot.student.api.io.resp.StudentProfileRespDTO;
import com.interviewpilot.student.service.StudentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生档案控制器
 */
@RestController
@RequestMapping("/api/ip/v1/student/profile")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    /**
     * 获取当前学生档案
     */
    @GetMapping
    public Result<StudentProfileRespDTO> getProfile(@CurrentUser UserContext currentUser) {
        return Results.success(studentProfileService.getProfile(currentUser.getUserId()));
    }

    /**
     * 保存/更新学生档案
     */
    @PutMapping
    public Result<Void> saveProfile(@CurrentUser UserContext currentUser,
                                    @RequestBody @Valid StudentProfileSaveReqDTO req) {
        studentProfileService.saveProfile(currentUser.getUserId(), req);
        return Results.success();
    }
}
