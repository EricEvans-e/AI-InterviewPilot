package com.interviewpilot.interview.application;

import com.interviewpilot.agent.application.BusinessAgentResolver;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.common.config.storage.ApplicationStorageProperties;
import com.interviewpilot.interview.api.io.req.InterviewQuestionReqDTO;
import com.interviewpilot.interview.api.io.resp.InterviewQuestionRespDTO;
import com.interviewpilot.interview.application.guard.core.InterviewAiGuardStage;
import com.interviewpilot.interview.application.guard.lock.InterviewAiSessionLockService;
import com.interviewpilot.interview.flow.extraction.InterviewQuestionExtractionService;
import com.interviewpilot.interview.service.InterviewQuestionCacheService;
import com.interviewpilot.interview.service.InterviewQuestionService;
import com.interviewpilot.interview.shared.InterviewAiInvoker;
import com.interviewpilot.interview.shared.InterviewResponseParser;
import com.interviewpilot.toolkit.iflytek.XunfeiWorkflowClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RLock;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewQuestionExtractionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldFailWhenWorkflowFallsBackToSmallTalkInsteadOfQuestions() throws Exception {
        BusinessAgentResolver businessAgentResolver = mock(BusinessAgentResolver.class);
        XunfeiWorkflowClient xunfeiWorkflowClient = mock(XunfeiWorkflowClient.class);
        InterviewAiInvoker interviewAiInvoker = mock(InterviewAiInvoker.class);
        InterviewAiSessionLockService interviewAiSessionLockService = mock(InterviewAiSessionLockService.class);
        InterviewQuestionService interviewQuestionService = mock(InterviewQuestionService.class);
        InterviewQuestionCacheService interviewQuestionCacheService = mock(InterviewQuestionCacheService.class);
        InterviewResponseParser interviewResponseParser = new InterviewResponseParser();
        InterviewQuestionExtractionService service = new InterviewQuestionExtractionService(
                businessAgentResolver,
                providerOf(xunfeiWorkflowClient),
                interviewAiInvoker,
                interviewAiSessionLockService,
                interviewQuestionService,
                interviewQuestionCacheService,
                interviewResponseParser,
                storageProperties()
        );

        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setId(8L);
        agent.setApiKey("key");
        agent.setApiSecret("secret");
        agent.setApiFlowId("flow-id");
        when(businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_QUESTION_EXTRACTION))
                .thenReturn(agent);
        when(xunfeiWorkflowClient.uploadFile(any(), eq("key"), eq("secret")))
                .thenReturn("https://example.com/resume.pdf");

        RLock heavyLock = mock(RLock.class);
        when(interviewAiSessionLockService.acquire("session-1", InterviewAiGuardStage.INTERVIEW_EXTRACTION)).thenReturn(heavyLock);
        String workflowResponse = """
                {"choices":[{"delta":{"role":"assistant","content":"{\\"questions\\":[],\\"sugest\\":[],\\"type\\":\\"\\",\\"smallTalk\\":\\"fallback smalltalk\\",\\"resumeScore\\":0}"}}]}
                """;
        when(interviewAiInvoker.callAiSyncWithFile(
                any(),
                eq("session-1"),
                eq(agent),
                eq("https://example.com/resume.pdf"),
                eq(InterviewAiGuardStage.INTERVIEW_EXTRACTION),
                any(),
                any()
        )).thenReturn(workflowResponse);

        InterviewQuestionReqDTO request = new InterviewQuestionReqDTO();
        request.setSessionId("session-1");
        request.setUserName("tester");
        request.setResumePdf(new MockMultipartFile(
                "resumePdf",
                "resume.pdf",
                "application/pdf",
                "dummy".getBytes(StandardCharsets.UTF_8)
        ));

        InterviewQuestionRespDTO response = service.extractInterviewQuestions(request);

        assertEquals(0, response.getIsSuccess());
        assertTrue(response.getErrorMessage().contains("smallTalk"));
        verify(interviewQuestionService).createFromAIResponse(
                eq(request),
                any(),
                any(),
                eq(null)
        );
        verify(interviewQuestionCacheService, never()).cacheInterviewQuestions(eq("session-1"), any());
        verify(interviewQuestionCacheService, never()).initInterviewFlow(eq("session-1"), any());
        verify(interviewAiSessionLockService).release(heavyLock);
    }

    @Test
    void shouldUseVisionOcrFallbackWhenOpenAiResumePdfHasNoTextLayer() throws Exception {
        BusinessAgentResolver businessAgentResolver = mock(BusinessAgentResolver.class);
        XunfeiWorkflowClient xunfeiWorkflowClient = mock(XunfeiWorkflowClient.class);
        InterviewAiInvoker interviewAiInvoker = mock(InterviewAiInvoker.class);
        InterviewAiSessionLockService interviewAiSessionLockService = mock(InterviewAiSessionLockService.class);
        InterviewQuestionService interviewQuestionService = mock(InterviewQuestionService.class);
        InterviewQuestionCacheService interviewQuestionCacheService = mock(InterviewQuestionCacheService.class);
        InterviewResponseParser interviewResponseParser = new InterviewResponseParser();
        InterviewQuestionExtractionService service = new InterviewQuestionExtractionService(
                businessAgentResolver,
                providerOf(xunfeiWorkflowClient),
                interviewAiInvoker,
                interviewAiSessionLockService,
                interviewQuestionService,
                interviewQuestionCacheService,
                interviewResponseParser,
                storageProperties()
        );

        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setId(8L);
        agent.setApiKey("key");
        agent.setApiSecret("mimo-v2.5");
        agent.setApiFlowId("https://token-plan-cn.xiaomimimo.com/v1");
        agent.setAiProvider("openai");
        when(businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_QUESTION_EXTRACTION))
                .thenReturn(agent);

        RLock heavyLock = mock(RLock.class);
        when(interviewAiSessionLockService.acquire("session-scan", InterviewAiGuardStage.INTERVIEW_EXTRACTION))
                .thenReturn(heavyLock);

        when(interviewAiInvoker.callAiSyncWithImage(
                argThat(prompt -> prompt != null && prompt.contains("OCR")),
                eq("session-scan"),
                eq(agent),
                argThat(bytes -> bytes != null && bytes.length > 0),
                eq("image/png"),
                eq(InterviewAiGuardStage.INTERVIEW_EXTRACTION),
                any()
        )).thenReturn("Candidate: Eric. Skills: Java, Spring Boot, OCR and AI interview systems.");

        String extractionResponse = """
                {"choices":[{"message":{"content":"{\\"questions\\":[\\"Explain your OCR pipeline\\"],\\"sugest\\":[\\"Ask about scanned PDF fallback\\"],\\"type\\":\\"AI backend engineer\\",\\"resumeScore\\":86}"}}]}
                """;
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        when(interviewAiInvoker.callAiSyncWithFile(
                any(),
                eq("session-scan"),
                eq(agent),
                argThat(url -> url != null && url.startsWith("/agent-files/")),
                eq(InterviewAiGuardStage.INTERVIEW_EXTRACTION),
                any(),
                paramsCaptor.capture()
        )).thenReturn(extractionResponse);

        when(interviewQuestionCacheService.getSessionInterviewQuestions("session-scan"))
                .thenReturn(Map.of("1", "Explain your OCR pipeline"));
        when(interviewQuestionCacheService.getSessionInterviewSuggestions("session-scan"))
                .thenReturn(Map.of("1", "Ask about scanned PDF fallback"));

        InterviewQuestionReqDTO request = new InterviewQuestionReqDTO();
        request.setSessionId("session-scan");
        request.setUserName("tester");
        request.setResumePdf(new MockMultipartFile(
                "resumePdf",
                "scanned-resume.pdf",
                "application/pdf",
                scannedResumePdf()
        ));

        InterviewQuestionRespDTO response = service.extractInterviewQuestions(request);

        assertEquals(1, response.getIsSuccess());
        assertEquals(86, response.getResumeScore());
        verify(xunfeiWorkflowClient, never()).uploadFile(any(), any(), any());
        verify(interviewAiInvoker).callAiSyncWithImage(
                any(),
                eq("session-scan"),
                eq(agent),
                any(),
                eq("image/png"),
                eq(InterviewAiGuardStage.INTERVIEW_EXTRACTION),
                any()
        );
        Map<String, Object> params = paramsCaptor.getValue();
        assertTrue(String.valueOf(params.get("resume_text")).contains("OCR and AI interview systems"));
        assertEquals("pdf_image_vision_ocr", params.get("resume_extraction_mode"));
        verify(interviewAiSessionLockService).release(heavyLock);
    }

    @Test
    void shouldPreferPdfTextAndSkipVisionOcrWhenOpenAiResumePdfHasUsableTextLayer() throws Exception {
        BusinessAgentResolver businessAgentResolver = mock(BusinessAgentResolver.class);
        XunfeiWorkflowClient xunfeiWorkflowClient = mock(XunfeiWorkflowClient.class);
        InterviewAiInvoker interviewAiInvoker = mock(InterviewAiInvoker.class);
        InterviewAiSessionLockService interviewAiSessionLockService = mock(InterviewAiSessionLockService.class);
        InterviewQuestionService interviewQuestionService = mock(InterviewQuestionService.class);
        InterviewQuestionCacheService interviewQuestionCacheService = mock(InterviewQuestionCacheService.class);
        InterviewResponseParser interviewResponseParser = new InterviewResponseParser();
        InterviewQuestionExtractionService service = new InterviewQuestionExtractionService(
                businessAgentResolver,
                providerOf(xunfeiWorkflowClient),
                interviewAiInvoker,
                interviewAiSessionLockService,
                interviewQuestionService,
                interviewQuestionCacheService,
                interviewResponseParser,
                storageProperties()
        );

        AgentPropertiesDO agent = new AgentPropertiesDO();
        agent.setId(8L);
        agent.setApiKey("key");
        agent.setApiSecret("mimo-v2.5");
        agent.setApiFlowId("https://token-plan-cn.xiaomimimo.com/v1");
        agent.setAiProvider("openai");
        when(businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_QUESTION_EXTRACTION))
                .thenReturn(agent);

        RLock heavyLock = mock(RLock.class);
        when(interviewAiSessionLockService.acquire("session-text", InterviewAiGuardStage.INTERVIEW_EXTRACTION))
                .thenReturn(heavyLock);

        String extractionResponse = """
                {"choices":[{"message":{"content":"{\\"questions\\":[\\"Explain your Spring Boot project\\"],\\"sugest\\":[\\"Ask about architecture\\"],\\"type\\":\\"Java backend engineer\\",\\"resumeScore\\":90}"}}]}
                """;
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        when(interviewAiInvoker.callAiSyncWithFile(
                any(),
                eq("session-text"),
                eq(agent),
                argThat(url -> url != null && url.startsWith("/agent-files/")),
                eq(InterviewAiGuardStage.INTERVIEW_EXTRACTION),
                any(),
                paramsCaptor.capture()
        )).thenReturn(extractionResponse);

        when(interviewQuestionCacheService.getSessionInterviewQuestions("session-text"))
                .thenReturn(Map.of("1", "Explain your Spring Boot project"));
        when(interviewQuestionCacheService.getSessionInterviewSuggestions("session-text"))
                .thenReturn(Map.of("1", "Ask about architecture"));

        InterviewQuestionReqDTO request = new InterviewQuestionReqDTO();
        request.setSessionId("session-text");
        request.setUserName("tester");
        request.setResumePdf(new MockMultipartFile(
                "resumePdf",
                "resume.pdf",
                "application/pdf",
                textResumePdf()
        ));

        InterviewQuestionRespDTO response = service.extractInterviewQuestions(request);

        assertEquals(1, response.getIsSuccess());
        assertEquals(90, response.getResumeScore());
        verify(interviewAiInvoker, never()).callAiSyncWithImage(any(), any(), any(), any(), any(), any(), any());
        Map<String, Object> params = paramsCaptor.getValue();
        assertTrue(String.valueOf(params.get("resume_text")).contains("Spring Boot"));
        assertEquals("pdf_text", params.get("resume_extraction_mode"));
        verify(interviewAiSessionLockService).release(heavyLock);
    }

    private ApplicationStorageProperties storageProperties() {
        ApplicationStorageProperties properties = new ApplicationStorageProperties();
        properties.setBaseDir(tempDir.toString());
        properties.setAgentFileDir(tempDir.resolve("agent-files").toString());
        return properties;
    }

    private ObjectProvider<XunfeiWorkflowClient> providerOf(XunfeiWorkflowClient client) {
        return new ObjectProvider<>() {
            @Override
            public XunfeiWorkflowClient getObject(Object... args) {
                return client;
            }

            @Override
            public XunfeiWorkflowClient getIfAvailable() {
                return client;
            }

            @Override
            public XunfeiWorkflowClient getIfUnique() {
                return client;
            }

            @Override
            public XunfeiWorkflowClient getObject() {
                return client;
            }
        };
    }

    private byte[] scannedResumePdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            BufferedImage image = new BufferedImage(900, 1200, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 36));
            graphics.drawString("Candidate: Eric", 80, 140);
            graphics.drawString("Skills: Java, Spring Boot, OCR", 80, 220);
            graphics.drawString("Project: AI interview systems", 80, 300);
            graphics.dispose();

            PDImageXObject pageImage = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pageImage, 40, 80, 520, 690);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] textResumePdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(40, 760);
                contentStream.showText("Eric Resume: Java backend engineer with Spring Boot, Redis, MySQL, OCR fallback,");
                contentStream.newLineAtOffset(0, -18);
                contentStream.showText("AI interview systems, PDF processing, Mimo model integration, and report reliability work.");
                contentStream.endText();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }
}
