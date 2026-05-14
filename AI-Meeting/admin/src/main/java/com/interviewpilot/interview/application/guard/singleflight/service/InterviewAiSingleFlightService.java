package com.interviewpilot.interview.application.guard.singleflight.service;

import cn.hutool.core.util.StrUtil;
import com.interviewpilot.interview.config.InterviewAiSingleFlightConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 本地 AI 请求 SingleFlight 去重服务（JVM 内级别）
 * 同一个 key 的并发请求只允许一个 leader 执行真实 AI 调用，其余 follower 复用结果。
 * 适用于单机部署或作为分布式模式的降级回退。
 *
 * <p>核心机制：ConcurrentHashMap + CompletableFuture，leader 写入 future，follower 等待 future 完成。
 * TTL 过期自动清理，防止 future 泄漏。</p>
 *
 * @see DistributedInterviewAiSingleFlightService 分布式版本（跨 JVM 协调）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewAiSingleFlightService {

    private final InterviewAiSingleFlightConfiguration configuration;
    private final MeterRegistry meterRegistry;

    private final ConcurrentMap<String, FlightEntry> flights = new ConcurrentHashMap<>();

    public <T> T execute(String key, Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        if (!Boolean.TRUE.equals(configuration.getEnable()) || StrUtil.isBlank(key)) {
            meterRegistry.counter("ai_singleflight_miss_total").increment();
            return supplier.get();
        }

        long now = System.currentTimeMillis();
        long ttlMillis = resolveTtlMillis();
        AtomicBoolean newFlight = new AtomicBoolean(false);
        // compute 保证同 key 下“创建 flight + 复用 flight”原子化，避免瞬时并发下出现多个 leader。
        FlightEntry entry = flights.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expireAtMillis <= now) {
                newFlight.set(true);
                return new FlightEntry(new CompletableFuture<>(), now + ttlMillis);
            }
            return existing;
        });

        if (newFlight.get()) {
            meterRegistry.counter("ai_singleflight_miss_total").increment();
            try {
                // leader 执行真实调用，并把结果广播给同 key 的等待者。
                T value = supplier.get();
                entry.resultFuture.complete(value);
                return value;
            } catch (Throwable ex) {
                entry.resultFuture.completeExceptionally(ex);
                flights.remove(key, entry);
                throw ex;
            } finally {
                cleanupExpired(now);
            }
        }

        meterRegistry.counter("ai_singleflight_hit_total").increment();
        try {
            @SuppressWarnings("unchecked")
            T reused = (T) entry.resultFuture.get(resolveWaitTimeoutMillis(), TimeUnit.MILLISECONDS);
            return reused;
        } catch (TimeoutException ex) {
            // waiter 超时后主动剔除旧 flight，避免后续请求持续等待一个可能已失活的 future。
            flights.remove(key, entry);
            throw new CompletionException(new RejectedExecutionException("single-flight wait timeout", ex));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CompletionException(ex);
        } catch (ExecutionException ex) {
            throw rethrow(ex.getCause());
        }
    }

    private RuntimeException rethrow(Throwable cause) {
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new CompletionException(cause);
    }

    private long resolveTtlMillis() {
        Long configured = configuration.getTtlMillis();
        return configured != null && configured > 0 ? configured : 4000L;
    }

    private long resolveWaitTimeoutMillis() {
        Long configured = configuration.getWaitTimeoutMillis();
        return configured != null && configured > 0 ? configured : 5000L;
    }

    private void cleanupExpired(long nowMillis) {
        Integer configured = configuration.getCleanupThreshold();
        int threshold = configured != null && configured > 0 ? configured : 256;
        if (flights.size() < threshold) {
            return;
        }
        flights.entrySet().removeIf(entry -> entry.getValue().expireAtMillis <= nowMillis);
    }

    private record FlightEntry(CompletableFuture<Object> resultFuture, long expireAtMillis) {
    }
}
