package com.interviewpilot.interview.config;

import com.interviewpilot.interview.application.guard.core.InterviewAiGuardStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterviewAiGuardConfigurationTest {

    @Test
    void shouldUseFiveMinuteTimeoutForReportReviewStage() {
        InterviewAiGuardConfiguration configuration = new InterviewAiGuardConfiguration();

        InterviewAiGuardConfiguration.StagePolicy policy =
                configuration.resolveStagePolicy(InterviewAiGuardStage.INTERVIEW_REPORT_REVIEW);

        assertEquals(300000L, policy.getTimeoutMillis());
        assertEquals(6, policy.getMaxConcurrentCalls());
        assertEquals(0, policy.getRetryCount());
        assertEquals(0L, policy.getRetryWaitMillis());
    }

    @Test
    void shouldUseFiveMinuteTimeoutForReferenceAnswerStage() {
        InterviewAiGuardConfiguration configuration = new InterviewAiGuardConfiguration();

        InterviewAiGuardConfiguration.StagePolicy policy =
                configuration.resolveStagePolicy(InterviewAiGuardStage.INTERVIEW_REFERENCE_ANSWER);

        assertEquals(300000L, policy.getTimeoutMillis());
        assertEquals(6, policy.getMaxConcurrentCalls());
        assertEquals(0, policy.getRetryCount());
        assertEquals(0L, policy.getRetryWaitMillis());
    }
}
