package com.interviewpilot.interview.application.strategy;

public interface DemeanorNormalizationStrategy {

    boolean isLikelyTenScale(Integer... scores);

    int normalize(Integer score, boolean tenScaleDetected);
}

