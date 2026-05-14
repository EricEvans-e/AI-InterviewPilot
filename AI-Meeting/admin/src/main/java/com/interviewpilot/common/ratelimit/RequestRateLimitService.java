package com.interviewpilot.common.ratelimit;

public interface RequestRateLimitService {

    boolean tryAcquire(String key, RequestRateLimitPolicy policy);
}
