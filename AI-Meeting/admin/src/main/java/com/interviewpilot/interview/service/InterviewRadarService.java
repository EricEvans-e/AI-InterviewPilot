package com.interviewpilot.interview.service;

import com.interviewpilot.interview.api.io.resp.RadarChartDTO;

public interface InterviewRadarService {

    RadarChartDTO buildRadarChart(Integer resumeScore, Integer interviewScore, Integer demeanorScore);
}

