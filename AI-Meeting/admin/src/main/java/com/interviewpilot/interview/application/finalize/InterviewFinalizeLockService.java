package com.interviewpilot.interview.application.finalize;

import cn.hutool.core.util.StrUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 面试终结锁服务
 * 面试结束时（finishSession）加锁，防止重复触发报告生成
 * 锁 key：interview:finalize:lock:{sessionId}
 * 等待时间：0ms（拿不到直接失败）| 过期时间：120秒（自动释放防死锁）
 */
@Service
public class InterviewFinalizeLockService {

    private static final String LOCK_KEY_PREFIX = "interview:finalize:lock:";
    private static final long LOCK_WAIT_MILLIS = 0L;
    private static final long LOCK_LEASE_SECONDS = 120L;

    private final RedissonClient redissonClient;

    public InterviewFinalizeLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public RLock acquire(String sessionId) throws InterruptedException {
        if (StrUtil.isBlank(sessionId)) {
            return null;
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + sessionId);
        boolean acquired = lock.tryLock(LOCK_WAIT_MILLIS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        return acquired ? lock : null;
    }

    public void release(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
