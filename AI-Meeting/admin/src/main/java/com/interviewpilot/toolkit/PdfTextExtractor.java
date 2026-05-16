package com.interviewpilot.toolkit;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * PDF 文本提取工具，用于从上传的 PDF 简历中提取纯文本内容。
 */
@Slf4j
public final class PdfTextExtractor {

    private PdfTextExtractor() {
    }

    /**
     * 从 MultipartFile 中提取 PDF 文本内容。
     * 提取失败时返回空字符串而非抛出异常，保证主流程不中断。
     */
    public static String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("PDF text extracted, pages={}, textLength={}", document.getNumberOfPages(), text.length());
            return text == null ? "" : text.trim();
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 从字节数组中提取 PDF 文本内容。
     */
    public static String extractText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return "";
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("PDF text extracted, pages={}, textLength={}", document.getNumberOfPages(), text.length());
            return text == null ? "" : text.trim();
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage());
            return "";
        }
    }
}
