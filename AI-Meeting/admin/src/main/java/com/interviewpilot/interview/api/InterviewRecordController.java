package com.interviewpilot.interview.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.interviewpilot.common.config.storage.ApplicationStorageProperties;
import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.common.convention.context.UserContext;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.interview.api.io.req.InterviewRecordPageReqDTO;
import com.interviewpilot.interview.api.io.req.InterviewRecordSaveReqDTO;
import com.interviewpilot.interview.api.io.resp.InterviewRecordRespDTO;
import com.interviewpilot.interview.service.InterviewRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * 面试记录控制器
 * 提供面试报告的保存、查询、分页等接口
 * 所有接口均需要登录（通过 Sa-Token 从请求头解析当前用户）
 */
@Slf4j
@RestController
@RequestMapping("/api/ip/v1/interview")
@RequiredArgsConstructor
public class InterviewRecordController {

    private static final Set<String> ALLOWED_RECORDING_EXTENSIONS = Set.of(
            ".webm", ".mp4", ".mkv", ".mp3", ".wav", ".m4a"
    );

    private final InterviewRecordService interviewRecordService;
    private final ApplicationStorageProperties storageProperties;

    /**
     * 保存面试记录
     * 面试结束后，前端调用此接口将评分、反馈等数据持久化到数据库
     *
     * @param requestParam 包含 sessionId 和评分数据
     * @param currentUser  当前登录用户（由 @CurrentUser 注解自动注入）
     */
    @PostMapping({"/interview/record", "/record"})
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
    @GetMapping({"/interview/records", "/records"})
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
    @GetMapping({"/interview/record/{sessionId}", "/record/{sessionId}"})
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
    @PostMapping({"/interview/record/save-from-redis/{sessionId}", "/record/save-from-redis/{sessionId}"})
    public Result<Void> saveInterviewRecordFromRedis(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        interviewRecordService.saveInterviewRecordFromRedis(sessionId, currentUser.getUserId());
        return Results.success();
    }

    /**
     * 上传面试录像/录音文件
     * 前端在面试结束后调用此接口上传录制的音视频文件
     *
     * @param sessionId   面试会话ID
     * @param file        录像/录音文件（支持 .webm, .mp4, .mkv, .mp3, .wav, .m4a）
     * @param currentUser 当前登录用户
     * @return 文件的相对访问路径
     */
    @PostMapping({"/interview/record/{sessionId}/recording", "/record/{sessionId}/recording"})
    public Result<String> uploadRecording(
            @PathVariable String sessionId,
            @RequestParam("file") MultipartFile file,
            @CurrentUser UserContext currentUser) {
        validateRecordingFile(file);
        String relativeUrl = saveRecordingFile(sessionId, file);
        return Results.success(relativeUrl);
    }

    private void validateRecordingFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ClientException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("文件名不合法");
        }
        String lowerName = originalFilename.toLowerCase();
        boolean allowed = ALLOWED_RECORDING_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!allowed) {
            throw new ClientException("不支持的文件格式，允许的格式: " + ALLOWED_RECORDING_EXTENSIONS);
        }
    }

    private String saveRecordingFile(String sessionId, MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String filename = sessionId + "_" + System.currentTimeMillis() + ext;

            Path recordingDir = storageProperties.getRecordingPath();
            Files.createDirectories(recordingDir);

            Path targetPath = recordingDir.resolve(filename);
            file.transferTo(targetPath.toFile());

            log.info("Saved recording file, sessionId={}, filename={}, size={}", sessionId, filename, file.getSize());
            return "/recordings/" + filename;
        } catch (IOException e) {
            log.error("Failed to save recording file, sessionId={}", sessionId, e);
            throw new ClientException("文件保存失败");
        }
    }
}
