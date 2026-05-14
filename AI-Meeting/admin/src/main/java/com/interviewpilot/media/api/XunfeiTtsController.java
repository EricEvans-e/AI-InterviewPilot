package com.interviewpilot.media.api;

import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.media.api.io.req.LongTextTtsReqDTO;
import com.interviewpilot.media.api.io.resp.LongTextTtsTaskRespDTO;
import com.interviewpilot.media.infrastructure.integration.XunfeiLongTextTtsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 讯飞长文本语音合成（TTS）控制器
 * 将面试报告、AI 回复等长文本转换为语音，支持异步任务模式
 */
@RestController
@RequestMapping("/api/ip/v1/xunfei/tts")
@RequiredArgsConstructor
public class XunfeiTtsController {

    private final XunfeiLongTextTtsService xunfeiLongTextTtsService;

    /**
     * 创建异步 TTS 任务
     * 提交文本后立即返回 taskId，后台异步合成语音
     *
     * @param requestParam 包含待合成的文本内容
     * @return 任务ID和初始状态
     */
    @PostMapping("/tasks")
    public Result<LongTextTtsTaskRespDTO> createTask(@RequestBody LongTextTtsReqDTO requestParam) {
        return Results.success(xunfeiLongTextTtsService.createTask(requestParam));
    }

    /**
     * 查询 TTS 任务状态（轮询用，检查合成是否完成）
     */
    @GetMapping("/tasks/{taskId}")
    public Result<LongTextTtsTaskRespDTO> queryTask(@PathVariable String taskId) {
        return Results.success(xunfeiLongTextTtsService.queryTask(taskId));
    }

    /**
     * 同步合成语音（创建任务并等待完成，适合短文本）
     * 阻塞直到合成完成或超时，返回完整的音频文件信息
     */
    @PostMapping("/synthesize")
    public Result<LongTextTtsTaskRespDTO> synthesizeAndWait(@RequestBody LongTextTtsReqDTO requestParam) {
        return Results.success(xunfeiLongTextTtsService.synthesizeAndWait(requestParam));
    }
}
