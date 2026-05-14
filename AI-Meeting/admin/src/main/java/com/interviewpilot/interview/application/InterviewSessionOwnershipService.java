package com.interviewpilot.interview.application;

import cn.hutool.core.util.StrUtil;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.common.enums.InterviewErrorCodeEnum;
import com.interviewpilot.interview.dao.entity.InterviewSession;
import com.interviewpilot.interview.dao.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 面试会话归属校验服务
 * 防止用户 A 访问用户 B 的面试会话（防越权）
 * 所有面试相关接口在执行业务逻辑前都会调用此服务校验
 */
@Component
@RequiredArgsConstructor
public class InterviewSessionOwnershipService {

    private final InterviewSessionRepository interviewSessionRepository;

    /**
     * 校验会话归属，返回会话对象
     *
     * @param sessionId 面试会话ID
     * @param userId    当前登录用户ID
     * @return 校验通过的会话对象
     * @throws ClientException SESSION_ID_EMPTY / INVALID_USER_ID / SESSION_NOT_FOUND / ACCESS_DENIED
     */
    public InterviewSession requireOwnedSession(String sessionId, Long userId) {
        if (StrUtil.isBlank(sessionId)) {
            throw new ClientException(InterviewErrorCodeEnum.SESSION_ID_EMPTY);
        }
        if (userId == null || userId <= 0) {
            throw new ClientException(InterviewErrorCodeEnum.INVALID_USER_ID);
        }
        InterviewSession session = interviewSessionRepository.findBySessionIdAndDelFlag(sessionId, 0)
                .orElseThrow(() -> new ClientException(InterviewErrorCodeEnum.INTERVIEW_SESSION_NOT_FOUND));
        if (!userId.equals(session.getUserId())) {
            throw new ClientException(InterviewErrorCodeEnum.INTERVIEW_SESSION_ACCESS_DENIED);
        }
        return session;
    }
}
