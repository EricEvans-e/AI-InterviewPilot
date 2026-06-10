package com.interviewpilot.questionbank.api.io.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionImportErrorRespDTO {

    private int rowIndex;

    private String message;
}
