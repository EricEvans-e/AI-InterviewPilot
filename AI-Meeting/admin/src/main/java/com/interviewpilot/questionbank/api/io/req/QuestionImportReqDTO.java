package com.interviewpilot.questionbank.api.io.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionImportReqDTO {

    private String importType;

    private Long collegeId;

    private Long majorId;

    private String defaultQuestionType;

    private String defaultDifficulty;

    private Integer defaultYear;

    private String statusAfterImport;

    private Boolean dryRun;
}
