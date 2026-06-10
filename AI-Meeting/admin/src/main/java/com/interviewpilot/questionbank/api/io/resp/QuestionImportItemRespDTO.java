package com.interviewpilot.questionbank.api.io.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionImportItemRespDTO {

    private int rowIndex;

    private boolean valid;

    private String errorMessage;

    private String rawText;

    private String title;

    private String content;

    private String questionType;

    private Long collegeId;

    private Long majorId;

    private String abilityTag;

    private String difficulty;

    private Integer answerTimeSeconds;

    private String referenceAnswer;

    private String scoringRule;

    private String followUpQuestions;

    private String sourceRef;

    private Integer year;

    private String statusAfterImport;

    private List<String> warnings = new ArrayList<>();
}
