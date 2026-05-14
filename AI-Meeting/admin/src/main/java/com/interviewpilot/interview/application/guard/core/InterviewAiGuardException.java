package com.interviewpilot.interview.application.guard.core;

import lombok.Getter;

/**
 * AI 调用保护层异常
 * 包装了错误码（errorCode）、阶段（stage）和原始异常，方便上层捕获和日志记录
 */
@Getter
public class InterviewAiGuardException extends RuntimeException {

    private final InterviewAiGuardErrorCode errorCode;
    private final String stage;

    public InterviewAiGuardException(
            InterviewAiGuardErrorCode errorCode,
            String stage,
            String message,
            Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.stage = stage;
    }
}
