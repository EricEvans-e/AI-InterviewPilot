package com.interviewpilot.interview.application.guard.lock;

import cn.hutool.core.util.StrUtil;
import com.interviewpilot.interview.config.InterviewAiSingleFlightConfiguration;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 面试会话级 AI 操作分布式锁
 * 同一个面试会话的重操作（如出题、生成报告）需要加锁，防止并发
 * 锁 key：interview:ai:heavy:lock:{stage}:{sessionId}
 * 等待时间：0ms（拿不到直接失败）| 过期时间：45秒（自动释放防死锁）
 */
@Service
@RequiredArgsConstructor
public class InterviewAiSessionLockService {

    private static final String LOCK_KEY_PREFIX = "interview:ai:heavy:lock:";

    private final RedissonClient redissonClient;
    private final InterviewAiSingleFlightConfiguration configuration;

    public RLock acquire(String sessionId, String stage) throws InterruptedException {
        if (StrUtil.isBlank(sessionId) || StrUtil.isBlank(stage)) {
            return null;
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + stage + ":" + sessionId);
        boolean acquired = lock.tryLock(
                resolveWaitMillis(),
                resolveExpireSeconds(),
                TimeUnit.SECONDS
        );
        return acquired ? lock : null;
    }

    public void release(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private long resolveExpireSeconds() {
        Long configured = configuration.getHeavyLockExpireSeconds();
        return configured != null && configured > 0 ? configured : 45L;
    }

    private long resolveWaitMillis() {
        Long configured = configuration.getHeavyLockWaitMillis();
        return configured != null && configured >= 0 ? configured : 0L;
    }
}
