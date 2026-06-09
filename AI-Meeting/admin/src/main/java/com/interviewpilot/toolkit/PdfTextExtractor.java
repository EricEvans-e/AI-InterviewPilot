package com.interviewpilot.toolkit;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PDF 文本提取工具，用于从上传的 PDF 简历中提取纯文本内容。
 */
@Slf4j
public final class PdfTextExtractor {

    private static final float DEFAULT_RENDER_DPI = 144F;

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

    /**
     * Renders the first pages of a PDF to PNG images. This is used as a visual OCR
     * fallback for scanned resumes that do not contain a usable text layer.
     */
    public static List<byte[]> renderFirstPagesAsPng(byte[] pdfBytes, int maxPages) {
        if (pdfBytes == null || pdfBytes.length == 0 || maxPages <= 0) {
            return Collections.emptyList();
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = Math.min(maxPages, document.getNumberOfPages());
            List<byte[]> images = new ArrayList<>(pageCount);
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, DEFAULT_RENDER_DPI, ImageType.RGB);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                images.add(output.toByteArray());
            }
            log.info("PDF pages rendered for vision OCR, pages={}, imageCount={}", document.getNumberOfPages(), images.size());
            return images;
        } catch (IOException e) {
            log.error("Failed to render PDF pages for vision OCR: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<byte[]> renderFirstPagesAsPng(MultipartFile file, int maxPages) {
        if (file == null || file.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return renderFirstPagesAsPng(file.getBytes(), maxPages);
        } catch (IOException e) {
            log.error("Failed to read PDF bytes for vision OCR: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
