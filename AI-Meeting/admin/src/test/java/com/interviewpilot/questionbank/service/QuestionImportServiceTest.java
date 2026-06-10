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
                        .defaultQuestionType("专业题")
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
        assertEquals("请说明 RoBERTa 继续预训练的设计思路", preview.getItems().get(0).getTitle());
        assertEquals("专业题", preview.getItems().get(0).getQuestionType());
        assertEquals(11L, preview.getItems().get(0).getCollegeId());
        assertEquals(22L, preview.getItems().get(0).getMajorId());
        assertFalse(preview.getItems().get(1).isValid());
        verify(questionBankService, never()).create(any(), any());
    }

    @Test
    void shouldPreviewQuestionsFromWordSections() throws Exception {
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        QuestionImportService service = new QuestionImportServiceImpl(questionBankService);

        QuestionImportRespDTO preview = service.preview(
                wordFileWithSections(),
                QuestionImportReqDTO.builder()
                        .importType("word_section")
                        .collegeId(11L)
                        .majorId(22L)
                        .defaultQuestionType("专业题")
                        .defaultDifficulty("medium")
                        .defaultYear(2026)
                        .statusAfterImport("pending_review")
                        .dryRun(true)
                        .build(),
                7L
        );

        assertEquals(2, preview.getValidCount());
        assertEquals("请谈谈你对文本风控系统多源融合的理解。", preview.getItems().get(0).getTitle());
        assertEquals("可以从宣传语、评论、价格三个来源解释。", preview.getItems().get(0).getReferenceAnswer());
        assertTrue(preview.getItems().get(0).getScoringRule().contains("技术路径"));
        assertTrue(preview.getItems().get(0).getFollowUpQuestions().contains("误判"));
    }

    @Test
    void shouldConfirmPreviewBatchAndSaveValidQuestionsAsPendingReview() throws Exception {
        QuestionBankService questionBankService = mock(QuestionBankService.class);
        when(questionBankService.create(any(), any())).thenReturn(1001L, 1002L);
        QuestionImportService service = new QuestionImportServiceImpl(questionBankService);

        QuestionImportRespDTO preview = service.preview(
                wordFileWithSections(),
                QuestionImportReqDTO.builder()
                        .importType("word_section")
                        .collegeId(11L)
                        .majorId(22L)
                        .defaultQuestionType("专业题")
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
                wordFileWithSections(),
                QuestionImportReqDTO.builder()
                        .importType("word_section")
                        .collegeId(11L)
                        .majorId(22L)
                        .defaultQuestionType("专业题")
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
                wordFileWithSections(),
                QuestionImportReqDTO.builder()
                        .importType("word_section")
                        .collegeId(11L)
                        .majorId(22L)
                        .defaultQuestionType("专业题")
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
            header.getCell(0).setText("题目");
            header.addNewTableCell().setText("题型");
            header.addNewTableCell().setText("能力点");
            header.addNewTableCell().setText("难度");
            header.addNewTableCell().setText("参考答案");
            header.addNewTableCell().setText("评分规则");
            header.addNewTableCell().setText("追问题");
            header.addNewTableCell().setText("来源");

            XWPFTableRow row = table.createRow();
            row.getCell(0).setText("请说明 RoBERTa 继续预训练的设计思路");
            row.getCell(1).setText("专业题");
            row.getCell(2).setText("专业知识");
            row.getCell(3).setText("medium");
            row.getCell(4).setText("说明 MLM、领域语料和微调目标。");
            row.getCell(5).setText("技术路径 40%；挑战 30%；效果 30%");
            row.getCell(6).setText("为什么选择 MLM？");
            row.getCell(7).setText("老师整理");

            XWPFTableRow invalidRow = table.createRow();
            invalidRow.getCell(0).setText("");
            invalidRow.getCell(1).setText("专业题");

            document.write(output);
            return new MockMultipartFile(
                    "file",
                    "questions.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    output.toByteArray()
            );
        }
    }

    private MockMultipartFile wordFileWithSections() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("【题型】专业题");
            document.createParagraph().createRun().setText("【能力点】专业知识【难度】medium");
            document.createParagraph().createRun().setText("1. 请谈谈你对文本风控系统多源融合的理解。");
            document.createParagraph().createRun().setText("参考答案：可以从宣传语、评论、价格三个来源解释。");
            document.createParagraph().createRun().setText("评分规则：技术路径 40%；风险控制 30%；效果指标 30%。");
            document.createParagraph().createRun().setText("追问题：如果评论语义与宣传语冲突，你如何降低误判？");
            document.createParagraph().createRun().setText("2、如果模型线上误判率上升，你会如何定位？");
            document.createParagraph().createRun().setText("参考答案：先看数据分布、阈值、模型版本和人工复核样本。");
            document.write(output);
            return new MockMultipartFile(
                    "file",
                    "questions-section.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    output.toByteArray()
            );
        }
    }
}
