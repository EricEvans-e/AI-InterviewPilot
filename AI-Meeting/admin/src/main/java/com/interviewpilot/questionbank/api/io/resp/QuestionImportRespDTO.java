package com.interviewpilot.questionbank.api.io.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionImportRespDTO {

    private String batchId;

    private String status;

    private int totalRows;

    private int validCount;

    private int invalidCount;

    private int importedCount;

    private List<QuestionImportItemRespDTO> items = new ArrayList<>();

    private List<QuestionImportErrorRespDTO> errors = new ArrayList<>();
}
