package com.interviewpilot.interview.api.io.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InterviewSessionCreateRespDTO {

    private String sessionId;

    private String status;

    /**
     * 会话模式: resume(简历出题) | questionBank(题库抽题)
     */
    private String sessionMode;
}
