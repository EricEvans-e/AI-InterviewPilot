package com.interviewpilot.agent.api;

import com.interviewpilot.agent.api.io.resp.AgentFileUploadRespDTO;
import com.interviewpilot.agent.service.AgentFileAssetService;
import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Agent 文件上传控制器
 * 用于上传简历、图片等文件到讯飞 Agent 平台
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ip/v1/agents/files")
public class AgentFileController {

    private final AgentFileAssetService agentFileAssetService;

    /**
     * 上传文件（简历 PDF、图片等）
     * 文件会同时保存到本地存储和讯飞 Agent 平台
     *
     * @param sessionId 关联的会话ID（可选）
     * @param bizType   业务类型（可选，如 resume、photo）
     * @param file      上传的文件
     * @param username  当前登录用户名
     * @return 文件上传结果（含文件URL等信息）
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AgentFileUploadRespDTO> upload(
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "bizType", required = false) String bizType,
            @RequestPart("file") MultipartFile file,
            @CurrentUser String username) {
        return Results.success(agentFileAssetService.uploadAndPersist(sessionId, bizType, username, file));
    }
}
