package com.interviewpilot.interview.application.strategy;

import com.interviewpilot.interview.service.model.InterviewTurnLog;

import java.util.List;

public interface InterviewScoreAggregatorStrategy {

    int clampScore(Integer score);

    int averageFromAggregate(Long scoreSum, Long answerCount);

    Integer averageFromTurns(List<InterviewTurnLog> turns);
}

