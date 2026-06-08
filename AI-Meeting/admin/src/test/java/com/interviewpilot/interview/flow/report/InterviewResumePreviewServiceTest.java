package com.interviewpilot.interview.flow.report;

import com.interviewpilot.common.config.storage.ApplicationStorageProperties;
import com.interviewpilot.interview.dao.entity.InterviewQuestion;
import com.interviewpilot.interview.service.InterviewQuestionService;
import com.interviewpilot.interview.service.InterviewSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InterviewResumePreviewServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void loadResumePreview_ShouldReadLocalMimoAgentFileUrl() throws Exception {
        byte[] pdf = "%PDF-1.4\nlocal mimo resume\n".getBytes();
        Path agentFileDir = tempDir.resolve("agent-files");
        Files.createDirectories(agentFileDir);
        Files.write(agentFileDir.resolve("resume.pdf"), pdf);

        InterviewQuestion question = new InterviewQuestion();
        question.setSessionId("session-local");
        question.setResumeFileUrl("/agent-files/resume.pdf");

        InterviewQuestionService questionService = mock(InterviewQuestionService.class);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        when(questionService.getBySessionId("session-local")).thenReturn(question);

        ApplicationStorageProperties storageProperties = new ApplicationStorageProperties();
        storageProperties.setBaseDir(tempDir.toString());
        storageProperties.setAgentFileDir(agentFileDir.toString());

        InterviewResumePreviewService service = new InterviewResumePreviewService(
                questionService,
                sessionService,
                storageProperties
        );

        InterviewResumePreviewService.ResumePreviewResource resource = service.loadResumePreview("session-local");

        assertArrayEquals(pdf, resource.getContent());
        assertEquals("application/pdf", resource.getContentType());
        assertEquals("resume.pdf", resource.getFileName());
    }
}
