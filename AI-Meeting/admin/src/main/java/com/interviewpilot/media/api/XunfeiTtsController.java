package com.interviewpilot.media.api;

import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.media.api.io.req.LongTextTtsReqDTO;
import com.interviewpilot.media.api.io.resp.LongTextTtsTaskRespDTO;
import com.interviewpilot.media.infrastructure.integration.MimoAudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/ip/v1/mimo/tts", "/api/ip/v1/xunfei/tts"})
@RequiredArgsConstructor
public class XunfeiTtsController {

    private final MimoAudioService mimoAudioService;

    @PostMapping("/tasks")
    public Result<LongTextTtsTaskRespDTO> createTask(@RequestBody LongTextTtsReqDTO requestParam) {
        return Results.success(mimoAudioService.createTask(requestParam));
    }

    @GetMapping("/tasks/{taskId}")
    public Result<LongTextTtsTaskRespDTO> queryTask(@PathVariable String taskId) {
        return Results.success(mimoAudioService.queryTask(taskId));
    }

    @PostMapping("/synthesize")
    public Result<LongTextTtsTaskRespDTO> synthesizeAndWait(@RequestBody LongTextTtsReqDTO requestParam) {
        return Results.success(mimoAudioService.synthesizeAndWait(requestParam));
    }
}
