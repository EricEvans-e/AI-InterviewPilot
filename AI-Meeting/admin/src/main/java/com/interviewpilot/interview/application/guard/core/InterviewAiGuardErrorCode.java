package com.interviewpilot.interview.application.guard.core;

/**
 * AI 调用保护层错误码
 * 前端根据这三种错误码展示不同的提示信息
 */
public enum InterviewAiGuardErrorCode {

    /** AI 调用超时（响应时间超过配置的阈值） */
    AI_TIMEOUT,

    /** AI 服务过载（被熔断器拒绝或限流器拦截） */
    AI_OVERLOADED,

    /** AI 服务不可用（网络异常、服务宕机等其他错误） */
    AI_UNAVAILABLE
}
