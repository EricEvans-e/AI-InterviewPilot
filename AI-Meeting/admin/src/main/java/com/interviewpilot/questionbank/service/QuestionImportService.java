package com.interviewpilot.questionbank.service;

import com.interviewpilot.questionbank.api.io.req.QuestionImportReqDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionImportRespDTO;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionImportService {

    QuestionImportRespDTO preview(MultipartFile file, QuestionImportReqDTO request, Long creatorId);

    QuestionImportRespDTO confirm(String batchId, Long creatorId);

    QuestionImportRespDTO getBatch(String batchId);
}
