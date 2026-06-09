package com.interviewpilot.interview.service.impl;

import com.interviewpilot.interview.api.io.req.DemeanorScoreDTO;
import com.interviewpilot.interview.application.strategy.AdaptiveDemeanorNormalizationStrategy;
import com.interviewpilot.interview.application.strategy.AverageInterviewScoreAggregatorStrategy;
import com.interviewpilot.interview.service.cache.InterviewCacheStore;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InterviewScoreServiceImplTest {

    @Test
    void shouldNotFabricateZeroDemeanorDetailsWhenCacheIsMissing() {
        InterviewCacheStore cacheStore = mock(InterviewCacheStore.class);
        when(cacheStore.getValue(anyString())).thenReturn(null);

        InterviewScoreServiceImpl service = new InterviewScoreServiceImpl(
                cacheStore,
                mock(StringRedisTemplate.class),
                new AverageInterviewScoreAggregatorStrategy(),
                new AdaptiveDemeanorNormalizationStrategy()
        );

        DemeanorScoreDTO detail = service.getSessionDemeanorScoreDetails("session-no-demeanor");

        assertNotNull(detail);
        assertNull(detail.getPanicLevel());
        assertNull(detail.getSeriousnessLevel());
        assertNull(detail.getEmoticonHandling());
        assertNull(detail.getCompositeScore());
    }
}
