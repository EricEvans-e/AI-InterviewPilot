package com.interviewpilot.interview.application.strategy;

import com.interviewpilot.interview.api.io.resp.RadarChartDTO;

public interface RadarComputationStrategy {

    RadarChartDTO compute(Integer resumeScore, Integer interviewScore, Integer demeanorScore);
}

