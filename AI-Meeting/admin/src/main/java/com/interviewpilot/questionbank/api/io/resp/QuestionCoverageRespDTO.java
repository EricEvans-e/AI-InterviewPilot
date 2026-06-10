package com.interviewpilot.questionbank.api.io.resp;

import lombok.Data;

@Data
public class QuestionCoverageRespDTO {

    private Long collegeId;

    private Long majorId;

    private String interviewMode;

    private long approvedCount;

    private long exactMatchCount;

    private long fallbackCount;

    private boolean canStartImmediately;

    private boolean mayNeedAiGeneration;
}
