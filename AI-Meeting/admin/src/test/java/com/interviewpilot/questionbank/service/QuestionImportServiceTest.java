package com.interviewpilot.questionbank.service;

import com.interviewpilot.questionbank.api.io.req.QuestionImportReqDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionImportRespDTO;
import com.interviewpilot.questionbank.service.impl.QuestionImportServiceImpl;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionImportServiceTest {

    @Test
    void shouldPreviewQuestionsFromWordTableWithoutSaving() throws Exception {
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        QuestionImportService service = new QuestionImportServiceImpl(questionBankService);

        QuestionImportRespDTO preview = service.preview(
                wordFileWithTable(),
                QuestionImportReqDTO.builder()
                        .importType("word_table")
                        .collegeId(11L)
                        .majorId(22L)
                        .defaultQuestionType("\u4e13\u4e1a\u9898")
                        .defaultDifficulty("medium")
                        .defaultYear(2026)
                        .statusAfterImport("pending_review")
                        .dryRun(true)
                        .build(),
                7L
        );

        assertEquals("PARTIAL_FAILED", preview.getStatus());
        assertEquals(2, preview.getTotalRows());
        assertEquals(1, preview.getValidCount());
        assertEquals(1, preview.getInvalidCount());
        assertEquals("\u8bf7\u8bf4\u660e RoBERTa \u7ee7\u7eed\u9884\u8bad\u7ec3\u7684\u8bbe\u8ba1\u601d\u8def",
                preview.getItems().get(0).getTitle());
        assertEquals("\u4e13\u4e1a\u9898", preview.getItems().get(0).getQuestionType());
        assertEquals(11L, preview.getItems().get(0).getCollegeId());
        assertEquals(22L, preview.getItems().get(0).getMajorId());
        assertFalse(preview.getItems().get(1).isValid());
        verify(questionBankService, never()).create(any(), any());
    }

    @Test
    void shouldPreviewQuestionsFromLegacyWordSections() throws Exception {
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        QuestionImportService service = new QuestionImportServiceImpl(questionBankService);

        QuestionImportRespDTO preview = service.preview(
                wordFileWithLegacySections(),
                QuestionImportReqDTO.builder()
                        .importType("word_section")
                        .collegeId(11L)
                        .majorId(22L)
                        .defaultQuestionType("\u4e13\u4e1a\u9898")
                        .defaultDifficulty("medium")
                        .defaultYear(2026)
                        .statusAfterImport("pending_review")
                        .dryRun(true)
                        .build(),
                7L
        );

        assertEquals(2, preview.getValidCount());
        assertEquals("\u8bf7\u8c08\u8c08\u4f60\u5bf9\u6587\u672c\u98ce\u63a7\u7cfb\u7edf\u591a\u6e90\u878d\u5408\u7684\u7406\u89e3\u3002",
                preview.getItems().get(0).getTitle());
        assertEquals("\u53ef\u4ee5\u4ece\u5ba3\u4f20\u8bed\u3001\u8bc4\u8bba\u3001\u4ef7\u683c\u4e09\u4e2a\u6765\u6e90\u89e3\u91ca\u3002",
                preview.getItems().get(0).getReferenceAnswer());
        assertTrue(preview.getItems().get(0).getScoringRule().contains("\u6280\u672f\u8def\u5f84"));
        assertTrue(preview.getItems().get(0).getFollowUpQuestions().contains("\u8bef\u5224"));
    }

    @Test
    void shouldPreviewQuestionsFromStrictWordSectionsWithoutTable() throws Exception {
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        QuestionImportService service = new QuestionImportServiceImpl(questionBankService);

        QuestionImportRespDTO preview = service.preview(
                wordFileWithStrictSections(),
                QuestionImportReqDTO.builder()
                        .importType("word_section")
                        .collegeId(11L)
                        .majorId(22L)
                        .defaultQuestionType("\u4e13\u4e1a\u9898")
                        .defaultDifficulty("medium")
                        .defaultYear(2026)
                        .statusAfterImport("pending_review")
                        .dryRun(true)
                        .build(),
                7L
        );

        assertEquals("PARSED", preview.getStatus());
        assertEquals(2, preview.getValidCount());
        assertEquals("\u4f60\u4e3a\u4ec0\u4e48\u62a5\u6211\u4eec\u5b66\u6821\u7684\u4eba\u5de5\u667a\u80fd\u6280\u672f\u5e94\u7528\u8fd9\u4e2a\u4e13\u4e1a\u5462\uff1f",
                preview.getItems().get(0).getTitle());
        assertEquals("\u7efc\u5408\u9898", preview.getItems().get(0).getQuestionType());
        assertEquals("\u4e13\u4e1a\u8ba4\u77e5", preview.getItems().get(0).getAbilityTag());
        assertEquals("medium", preview.getItems().get(0).getDifficulty());
        assertEquals(Integer.valueOf(2026), preview.getItems().get(0).getYear());
        assertTrue(preview.getItems().get(0).getReferenceAnswer().contains("\u4eba\u5de5\u667a\u80fd"));
        assertTrue(preview.getItems().get(0).getScoringRule().contains("40%"));
        assertTrue(preview.getItems().get(0).getFollowUpQuestions().contains("\u5b66\u4e60"));
        assertEquals("\u6821\u5185\u6574\u7406", preview.getItems().get(0).getSourceRef());
    }

    @Test
    void shouldPreviewQuestionsFromFlexibleWordSectionsWithOptionalMarkersAndEnglishColon() throws Exception {
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        QuestionImportService service = new QuestionImportServiceImpl(questionBankService);

        QuestionImportRespDTO preview = service.preview(
                wordFileWithFlexibleSections(),
                QuestionImportReqDTO.builder()
                        .importType("word_section")
                        .collegeId(11L)
                        .majorId(22L)
                        .defaultQuestionType("\u7efc\u5408\u9898")
                        .defaultDifficulty("medium")
                        .defaultYear(2026)
                        .statusAfterImport("pending_review")
                        .dryRun(true)
                        .build(),
                7L
        );

        assertEquals("PARSED", preview.getStatus());
        assertEquals(1, preview.getValidCount());
        assertEquals("\u8fd9\u4e2a\u4e13\u4e1a\u4ee5\u540e\u80fd\u505a\u4ec0\u4e48\uff1f", preview.getItems().get(0).getTitle());
        assertEquals("\u7efc\u5408\u9898", preview.getItems().get(0).getQuestionType());
        assertEquals("\u804c\u4e1a\u89c4\u5212", preview.getItems().get(0).getAbilityTag());
        assertEquals("medium", preview.getItems().get(0).getDifficulty());
        assertTrue(preview.getItems().get(0).getReferenceAnswer().contains("\u4eba\u5de5\u667a\u80fd\u5e94\u7528\u5f00\u53d1"));
        assertTrue(preview.getItems().get(0).getScoringRule().contains("40%"));
        assertEquals("\u6821\u5185\u6574\u7406", preview.getItems().get(0).getSourceRef());
    }

    @Test
    void shouldImportTitleOnlyWordSectionUsingDefaultQuestionType() throws Exception {
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        QuestionImportService service = new QuestionImportServiceImpl(questionBankService);

        QuestionImportRespDTO preview = service.preview(
                wordFileWithTitleOnlySection(),
                QuestionImportReqDTO.builder()
                        .importType("word_section")
                        .defaultQuestionType("\u7efc\u5408\u9898")
                        .defaultDifficulty("medium")
                        .defaultYear(2026)
                        .statusAfterImport("pending_review")
                        .dryRun(true)
                        .build(),
                7L
        );

        assertEquals("PARSED", preview.getStatus());
        assertEquals(1, preview.getValidCount());
        assertTrue(preview.getItems().get(0).isValid());
        assertEquals("\u4ecb\u7ecd\u4e00\u4e0b\u4f60\u5bf9\u8fd9\u4e2a\u4e13\u4e1a\u7684\u7406\u89e3", preview.getItems().get(0).getTitle());
        assertEquals("\u7efc\u5408\u9898", preview.getItems().get(0).getQuestionType());
        assertEquals("medium", preview.getItems().get(0).getDifficulty());
    }

    @Test
    void shouldConfirmPreviewBatchAndSaveValidQuestionsAsPendingReview() throws Exception {
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        when(questionBankService.create(any(), any())).thenReturn(1001L, 1002L);
        QuestionImportService service = new QuestionImportServiceImpl(questionBankService);

        QuestionImportRespDTO preview = service.preview(
                wordFileWithStrictSections(),
                QuestionImportReqDTO.builder()
                        .importType("word_section")
                        .collegeId(11L)
                        .majorId(22L)
                        .defaultQuestionType("\u4e13\u4e1a\u9898")
                        .defaultDifficulty("medium")
                        .defaultYear(2026)
                        .statusAfterImport("pending_review")
                        .dryRun(true)
                        .build(),
                7L
        );

        QuestionImportRespDTO imported = service.confirm(preview.getBatchId(), 7L);

        assertEquals("IMPORTED", imported.getStatus());
        assertEquals(2, imported.getImportedCount());
        verify(questionBankService, times(2)).create(any(), any());
    }

    @Test
    void shouldNotImportSameBatchTwiceWhenConfirmIsRepeated() throws Exception {
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        when(questionBankService.create(any(), any())).thenReturn(1001L, 1002L);
        QuestionImportService service = new QuestionImportServiceImpl(questionBankService);

        QuestionImportRespDTO preview = service.preview(
                wordFileWithStrictSections(),
                QuestionImportReqDTO.builder()
                        .importType("word_section")
                        .collegeId(11L)
                        .majorId(22L)
                        .defaultQuestionType("\u4e13\u4e1a\u9898")
                        .defaultDifficulty("medium")
                        .defaultYear(2026)
                        .statusAfterImport("pending_review")
                        .dryRun(true)
                        .build(),
                7L
        );

        service.confirm(preview.getBatchId(), 7L);
        QuestionImportRespDTO repeated = service.confirm(preview.getBatchId(), 7L);

        assertEquals("IMPORTED", repeated.getStatus());
        assertEquals(2, repeated.getImportedCount());
        verify(questionBankService, times(2)).create(any(), any());
    }

    @Test
    void shouldImportImmediatelyWhenDryRunIsFalse() throws Exception {
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        when(questionBankService.create(any(), any())).thenReturn(1001L, 1002L);
        QuestionImportService service = new QuestionImportServiceImpl(questionBankService);

        QuestionImportRespDTO imported = service.preview(
                wordFileWithStrictSections(),
                QuestionImportReqDTO.builder()
                        .importType("word_section")
                        .collegeId(11L)
                        .majorId(22L)
                        .defaultQuestionType("\u4e13\u4e1a\u9898")
                        .defaultDifficulty("medium")
                        .defaultYear(2026)
                        .statusAfterImport("pending_review")
                        .dryRun(false)
                        .build(),
                7L
        );

        assertEquals("IMPORTED", imported.getStatus());
        assertEquals(2, imported.getImportedCount());
        verify(questionBankService, times(2)).create(any(), any());
    }

    private MockMultipartFile wordFileWithTable() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable();
            XWPFTableRow header = table.getRow(0);
            header.getCell(0).setText("\u9898\u76ee");
            header.addNewTableCell().setText("\u9898\u578b");
            header.addNewTableCell().setText("\u80fd\u529b\u70b9");
            header.addNewTableCell().setText("\u96be\u5ea6");
            header.addNewTableCell().setText("\u53c2\u8003\u7b54\u6848");
            header.addNewTableCell().setText("\u8bc4\u5206\u89c4\u5219");
            header.addNewTableCell().setText("\u8ffd\u95ee\u9898");
            header.addNewTableCell().setText("\u6765\u6e90");

            XWPFTableRow row = table.createRow();
            row.getCell(0).setText("\u8bf7\u8bf4\u660e RoBERTa \u7ee7\u7eed\u9884\u8bad\u7ec3\u7684\u8bbe\u8ba1\u601d\u8def");
            row.getCell(1).setText("\u4e13\u4e1a\u9898");
            row.getCell(2).setText("\u4e13\u4e1a\u77e5\u8bc6");
            row.getCell(3).setText("medium");
            row.getCell(4).setText("\u8bf4\u660e MLM\u3001\u9886\u57df\u8bed\u6599\u548c\u5fae\u8c03\u76ee\u6807\u3002");
            row.getCell(5).setText("\u6280\u672f\u8def\u5f8440%\uff1b\u6311\u621830%\uff1b\u6548\u679c30%");
            row.getCell(6).setText("\u4e3a\u4ec0\u4e48\u9009\u62e9 MLM\uff1f");
            row.getCell(7).setText("\u8001\u5e08\u6574\u7406");

            XWPFTableRow invalidRow = table.createRow();
            invalidRow.getCell(0).setText("");
            invalidRow.getCell(1).setText("\u4e13\u4e1a\u9898");

            document.write(output);
            return new MockMultipartFile(
                    "file",
                    "questions.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    output.toByteArray()
            );
        }
    }

    private MockMultipartFile wordFileWithLegacySections() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("\u3010\u9898\u578b\u3011\u4e13\u4e1a\u9898");
            document.createParagraph().createRun().setText("\u3010\u80fd\u529b\u70b9\u3011\u4e13\u4e1a\u77e5\u8bc6\u3010\u96be\u5ea6\u3011medium");
            document.createParagraph().createRun().setText("1. \u8bf7\u8c08\u8c08\u4f60\u5bf9\u6587\u672c\u98ce\u63a7\u7cfb\u7edf\u591a\u6e90\u878d\u5408\u7684\u7406\u89e3\u3002");
            document.createParagraph().createRun().setText("\u53c2\u8003\u7b54\u6848\uff1a\u53ef\u4ee5\u4ece\u5ba3\u4f20\u8bed\u3001\u8bc4\u8bba\u3001\u4ef7\u683c\u4e09\u4e2a\u6765\u6e90\u89e3\u91ca\u3002");
            document.createParagraph().createRun().setText("\u8bc4\u5206\u89c4\u5219\uff1a\u6280\u672f\u8def\u5f8440%\uff1b\u98ce\u9669\u63a7\u523630%\uff1b\u6548\u679c\u6307\u680730%\u3002");
            document.createParagraph().createRun().setText("\u8ffd\u95ee\u9898\uff1a\u5982\u679c\u8bc4\u8bba\u8bed\u4e49\u4e0e\u5ba3\u4f20\u8bed\u51b2\u7a81\uff0c\u4f60\u5982\u4f55\u964d\u4f4e\u8bef\u5224\uff1f");
            document.createParagraph().createRun().setText("2\u3001\u5982\u679c\u6a21\u578b\u7ebf\u4e0a\u8bef\u5224\u7387\u4e0a\u5347\uff0c\u4f60\u4f1a\u5982\u4f55\u5b9a\u4f4d\uff1f");
            document.createParagraph().createRun().setText("\u53c2\u8003\u7b54\u6848\uff1a\u5148\u770b\u6570\u636e\u5206\u5e03\u3001\u9608\u503c\u3001\u6a21\u578b\u7248\u672c\u548c\u4eba\u5de5\u590d\u6838\u6837\u672c\u3002");
            document.write(output);
            return new MockMultipartFile(
                    "file",
                    "questions-legacy-section.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    output.toByteArray()
            );
        }
    }

    private MockMultipartFile wordFileWithStrictSections() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(
                    "\u9898\u76ee\uff1a\u4f60\u4e3a\u4ec0\u4e48\u62a5\u6211\u4eec\u5b66\u6821\u7684\u4eba\u5de5\u667a\u80fd\u6280\u672f\u5e94\u7528\u8fd9\u4e2a\u4e13\u4e1a\u5462\uff1f");
            document.createParagraph().createRun().setText("\u9898\u578b\uff1a\u7efc\u5408\u9898");
            document.createParagraph().createRun().setText("\u80fd\u529b\u70b9\uff1a\u4e13\u4e1a\u8ba4\u77e5");
            document.createParagraph().createRun().setText("\u96be\u5ea6\uff1amedium");
            document.createParagraph().createRun().setText("\u5e74\u4efd\uff1a2026");
            document.createParagraph().createRun().setText(
                    "\u53c2\u8003\u7b54\u6848\uff1a\u6211\u62a5\u8003\u8fd9\u4e2a\u4e13\u4e1a\uff0c\u4e3b\u8981\u662f\u56e0\u4e3a\u6211\u5bf9\u4eba\u5de5\u667a\u80fd\u6709\u6301\u7eed\u5174\u8da3\uff0c\u4e5f\u770b\u5230\u4e86\u5b83\u7684\u4ea7\u4e1a\u5e94\u7528\u524d\u666f\u3002");
            document.createParagraph().createRun().setText(
                    "\u8bc4\u5206\u89c4\u5219\uff1a\u4e13\u4e1a\u8ba4\u77e540%\uff1b\u8868\u8fbe\u903b\u8f9130%\uff1b\u804c\u4e1a\u89c4\u521230%");
            document.createParagraph().createRun().setText(
                    "\u8ffd\u95ee\u9898\uff1a\u4f60\u4e86\u89e3\u8fd9\u4e2a\u4e13\u4e1a\u672a\u6765\u4e3b\u8981\u4f1a\u5b66\u4e60\u54ea\u4e9b\u5185\u5bb9\u5417\uff1f");
            document.createParagraph().createRun().setText("\u6765\u6e90\uff1a\u6821\u5185\u6574\u7406");
            document.createParagraph().createRun().setText("");
            document.createParagraph().createRun().setText(
                    "\u9898\u76ee\uff1a\u8fd9\u4e2a\u4e13\u4e1a\u4ee5\u540e\u80fd\u505a\u4ec0\u4e48\uff1f");
            document.createParagraph().createRun().setText("\u9898\u578b\uff1a\u7efc\u5408\u9898");
            document.createParagraph().createRun().setText("\u80fd\u529b\u70b9\uff1a\u804c\u4e1a\u89c4\u5212");
            document.createParagraph().createRun().setText("\u96be\u5ea6\uff1amedium");
            document.createParagraph().createRun().setText(
                    "\u53c2\u8003\u7b54\u6848\uff1a\u6bd5\u4e1a\u540e\u53ef\u4ee5\u4ece\u4e8b\u4eba\u5de5\u667a\u80fd\u5e94\u7528\u5f00\u53d1\u3001\u6570\u636e\u5206\u6790\u3001\u667a\u80fd\u4ea7\u54c1\u5b9e\u65bd\u7b49\u5de5\u4f5c\u3002");
            document.createParagraph().createRun().setText(
                    "\u8bc4\u5206\u89c4\u5219\uff1a\u5c97\u4f4d\u7406\u89e340%\uff1b\u4e3e\u4f8b\u5177\u4f5330%\uff1b\u8868\u8fbe\u5b8c\u657430%");
            document.createParagraph().createRun().setText(
                    "\u8ffd\u95ee\u9898\uff1a\u5982\u679c\u4f60\u8fdb\u5165\u8fd9\u4e2a\u4e13\u4e1a\uff0c\u6700\u60f3\u5c1d\u8bd5\u54ea\u4e2a\u5c31\u4e1a\u65b9\u5411\uff1f");
            document.createParagraph().createRun().setText("\u6765\u6e90\uff1a\u6821\u5185\u6574\u7406");

            document.write(output);
            return new MockMultipartFile(
                    "file",
                    "questions-strict-section.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    output.toByteArray()
            );
        }
    }

    private MockMultipartFile wordFileWithFlexibleSections() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(
                    "\u9898\u76ee\uff1a\u8fd9\u4e2a\u4e13\u4e1a\u4ee5\u540e\u80fd\u505a\u4ec0\u4e48\uff1f");
            document.createParagraph().createRun().setText("\u9898\u578b*\uff1a\u7efc\u5408\u9898");
            document.createParagraph().createRun().setText("\u80fd\u529b\u70b9*: \u804c\u4e1a\u89c4\u5212");
            document.createParagraph().createRun().setText("\u96be\u5ea6*\uff1amedium");
            document.createParagraph().createRun().setText(
                    "\u53c2\u8003\u7b54\u6848*\uff1a\u8fd9\u4e2a\u4e13\u4e1a\u6bd5\u4e1a\u540e\u53ef\u4ee5\u4ece\u4e8b\u4eba\u5de5\u667a\u80fd\u5e94\u7528\u5f00\u53d1\u3001\u6570\u636e\u5904\u7406\u3001\u7b97\u6cd5\u8f85\u52a9\u5f00\u53d1\u3001\u667a\u80fd\u4ea7\u54c1\u6d4b\u8bd5\u4e0e\u5b9e\u65bd\u7b49\u5de5\u4f5c\uff0c\u4e5f\u53ef\u4ee5\u7ee7\u7eed\u6df1\u9020\uff0c\u5f80\u7b97\u6cd5\u3001\u6570\u636e\u5206\u6790\u6216\u8f6f\u4ef6\u5f00\u53d1\u65b9\u5411\u53d1\u5c55\u3002");
            document.createParagraph().createRun().setText(
                    "\u8bc4\u5206\u89c4\u5219*\uff1a\u5c97\u4f4d\u7406\u89e340%\uff1b\u4e3e\u4f8b\u5177\u4f5330%\uff1b\u8868\u8fbe\u5b8c\u657430%");
            document.createParagraph().createRun().setText("\u6765\u6e90*\uff1a\u6821\u5185\u6574\u7406");

            document.write(output);
            return new MockMultipartFile(
                    "file",
                    "questions-flexible-section.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    output.toByteArray()
            );
        }
    }

    private MockMultipartFile wordFileWithTitleOnlySection() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(
                    "\u9898\u76ee\uff1a\u4ecb\u7ecd\u4e00\u4e0b\u4f60\u5bf9\u8fd9\u4e2a\u4e13\u4e1a\u7684\u7406\u89e3");

            document.write(output);
            return new MockMultipartFile(
                    "file",
                    "questions-title-only.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    output.toByteArray()
            );
        }
    }
}
