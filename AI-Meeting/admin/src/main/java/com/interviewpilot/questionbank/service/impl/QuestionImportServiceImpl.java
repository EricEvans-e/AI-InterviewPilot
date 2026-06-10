package com.interviewpilot.questionbank.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.questionbank.api.io.req.QuestionImportReqDTO;
import com.interviewpilot.questionbank.api.io.req.QuestionSaveReqDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionImportErrorRespDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionImportItemRespDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionImportRespDTO;
import com.interviewpilot.questionbank.service.QuestionBankService;
import com.interviewpilot.questionbank.service.QuestionImportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionImportServiceImpl implements QuestionImportService {

    private static final String IMPORT_TYPE_WORD_TABLE = "word_table";
    private static final String IMPORT_TYPE_WORD_SECTION = "word_section";

    private static final String FIELD_TITLE = "\u9898\u76ee";
    private static final String FIELD_QUESTION_TYPE = "\u9898\u578b";
    private static final String FIELD_ABILITY_TAG = "\u80fd\u529b\u70b9";
    private static final String FIELD_DIFFICULTY = "\u96be\u5ea6";
    private static final String FIELD_REFERENCE_ANSWER = "\u53c2\u8003\u7b54\u6848";
    private static final String FIELD_SCORING_RULE = "\u8bc4\u5206\u89c4\u5219";
    private static final String FIELD_FOLLOW_UP = "\u8ffd\u95ee\u9898";
    private static final String FIELD_SOURCE = "\u6765\u6e90";
    private static final String FIELD_YEAR = "\u5e74\u4efd";

    private static final String STATUS_IMPORTED = "IMPORTED";
    private static final List<String> VALID_STATUSES = List.of("draft", "pending_review", "approved", "rejected");

    private static final Pattern LEGACY_META_PATTERN = Pattern.compile("\u3010([^\\u3011]+)\u3011([^\\u3010]+)");
    private static final Pattern LEGACY_QUESTION_PATTERN = Pattern.compile("^\\s*\\d+\\s*[.\\u3001]\\s*(.+)$");

    private final QuestionBankService questionBankService;
    private final Map<String, QuestionImportRespDTO> batches = new ConcurrentHashMap<>();

    @Override
    public QuestionImportRespDTO preview(MultipartFile file, QuestionImportReqDTO request, Long creatorId) {
        if (file == null || file.isEmpty()) {
            throw new ClientException("\u5bfc\u5165\u6587\u4ef6\u4e0d\u80fd\u4e3a\u7a7a");
        }

        QuestionImportReqDTO normalized = normalizeRequest(request);
        try (InputStream inputStream = file.getInputStream(); XWPFDocument document = new XWPFDocument(inputStream)) {
            List<QuestionImportItemRespDTO> items = switch (normalized.getImportType()) {
                case IMPORT_TYPE_WORD_TABLE -> parseTables(document, normalized);
                case IMPORT_TYPE_WORD_SECTION -> parseSections(document, normalized);
                default -> throw new ClientException("\u6682\u4e0d\u652f\u6301\u7684\u5bfc\u5165\u7c7b\u578b\uff1a" + normalized.getImportType());
            };

            QuestionImportRespDTO response = buildResponse(items);
            response.setBatchId("imp-" + IdUtil.fastSimpleUUID());
            response.setStatus(response.getInvalidCount() > 0 ? "PARTIAL_FAILED" : "PARSED");
            batches.put(response.getBatchId(), response);
            if (Boolean.FALSE.equals(normalized.getDryRun())) {
                return confirm(response.getBatchId(), creatorId);
            }
            return response;
        } catch (ClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ClientException("Word \u6587\u4ef6\u89e3\u6790\u5931\u8d25\uff1a" + ex.getMessage());
        }
    }

    @Override
    public QuestionImportRespDTO confirm(String batchId, Long creatorId) {
        QuestionImportRespDTO batch = batches.get(batchId);
        if (batch == null) {
            throw new ClientException("\u5bfc\u5165\u6279\u6b21\u4e0d\u5b58\u5728\u6216\u5df2\u8fc7\u671f");
        }
        if (STATUS_IMPORTED.equals(batch.getStatus())) {
            return batch;
        }
        int imported = 0;
        for (QuestionImportItemRespDTO item : batch.getItems()) {
            if (!item.isValid()) {
                continue;
            }
            questionBankService.create(toSaveReq(item), creatorId);
            imported++;
        }
        batch.setImportedCount(imported);
        batch.setStatus(STATUS_IMPORTED);
        return batch;
    }

    @Override
    public QuestionImportRespDTO getBatch(String batchId) {
        QuestionImportRespDTO batch = batches.get(batchId);
        if (batch == null) {
            throw new ClientException("\u5bfc\u5165\u6279\u6b21\u4e0d\u5b58\u5728\u6216\u5df2\u8fc7\u671f");
        }
        return batch;
    }

    private QuestionImportReqDTO normalizeRequest(QuestionImportReqDTO request) {
        QuestionImportReqDTO normalized = request == null ? new QuestionImportReqDTO() : request;
        if (StrUtil.isBlank(normalized.getImportType())) {
            normalized.setImportType(IMPORT_TYPE_WORD_TABLE);
        }
        if (StrUtil.isBlank(normalized.getDefaultQuestionType())) {
            normalized.setDefaultQuestionType("综合题");
        }
        if (StrUtil.isBlank(normalized.getDefaultDifficulty())) {
            normalized.setDefaultDifficulty("medium");
        }
        if (normalized.getDefaultYear() == null) {
            normalized.setDefaultYear(2026);
        }
        if (StrUtil.isBlank(normalized.getStatusAfterImport())) {
            normalized.setStatusAfterImport("pending_review");
        }
        if (!VALID_STATUSES.contains(normalized.getStatusAfterImport())) {
            throw new ClientException("\u65e0\u6548\u7684\u5bfc\u5165\u540e\u72b6\u6001\uff1a" + normalized.getStatusAfterImport());
        }
        return normalized;
    }

    private List<QuestionImportItemRespDTO> parseTables(XWPFDocument document, QuestionImportReqDTO request) {
        List<QuestionImportItemRespDTO> items = new ArrayList<>();
        for (XWPFTable table : document.getTables()) {
            List<XWPFTableRow> rows = table.getRows();
            if (rows.size() <= 1) {
                continue;
            }
            Map<String, Integer> header = buildHeader(rows.get(0));
            for (int i = 1; i < rows.size(); i++) {
                XWPFTableRow row = rows.get(i);
                QuestionImportItemRespDTO item = newBaseItem(i, request);
                item.setTitle(cell(row, header, FIELD_TITLE));
                item.setContent(item.getTitle());
                item.setQuestionType(firstNotBlank(cell(row, header, FIELD_QUESTION_TYPE), request.getDefaultQuestionType()));
                item.setAbilityTag(cell(row, header, FIELD_ABILITY_TAG));
                item.setDifficulty(firstNotBlank(cell(row, header, FIELD_DIFFICULTY), request.getDefaultDifficulty()));
                item.setReferenceAnswer(cell(row, header, FIELD_REFERENCE_ANSWER));
                item.setScoringRule(cell(row, header, FIELD_SCORING_RULE));
                item.setFollowUpQuestions(cell(row, header, FIELD_FOLLOW_UP));
                item.setSourceRef(cell(row, header, FIELD_SOURCE));
                item.setYear(firstNotNull(parseInt(cell(row, header, FIELD_YEAR)), request.getDefaultYear()));
                item.setRawText(row.getTableCells().stream().map(XWPFTableCell::getText).collect(Collectors.joining(" | ")));
                validate(item);
                items.add(item);
            }
        }
        return items;
    }

    private List<QuestionImportItemRespDTO> parseSections(XWPFDocument document, QuestionImportReqDTO request) {
        List<QuestionImportItemRespDTO> items = new ArrayList<>();
        QuestionImportItemRespDTO current = null;
        Map<String, String> legacyMetadata = new LinkedHashMap<>();
        int rowIndex = 0;

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = normalize(paragraph.getText());
            if (StrUtil.isBlank(text)) {
                continue;
            }

            Map<String, String> strictFields = parseStrictFields(text);
            if (!strictFields.isEmpty()) {
                if (strictFields.containsKey(FIELD_TITLE)) {
                    if (current != null) {
                        validate(current);
                        items.add(current);
                    }
                    rowIndex++;
                    current = newBaseItem(rowIndex, request);
                    applyLegacyMetadata(current, legacyMetadata, request);
                } else if (current == null) {
                    continue;
                }

                applyStrictFields(current, strictFields, request);
                appendRawText(current, text);
                continue;
            }

            Matcher legacyMetaMatcher = LEGACY_META_PATTERN.matcher(text);
            boolean foundMeta = false;
            while (legacyMetaMatcher.find()) {
                foundMeta = true;
                legacyMetadata.put(normalizeFieldName(legacyMetaMatcher.group(1)), legacyMetaMatcher.group(2).trim());
            }
            if (foundMeta) {
                continue;
            }

            Matcher legacyQuestionMatcher = LEGACY_QUESTION_PATTERN.matcher(text);
            if (legacyQuestionMatcher.matches()) {
                if (current != null) {
                    validate(current);
                    items.add(current);
                }
                rowIndex++;
                current = newBaseItem(rowIndex, request);
                applyLegacyMetadata(current, legacyMetadata, request);
                current.setTitle(legacyQuestionMatcher.group(1).trim());
                current.setContent(current.getTitle());
                appendRawText(current, text);
                continue;
            }

            if (current == null) {
                continue;
            }

            appendRawText(current, text);
            String reference = extractValue(text, FIELD_REFERENCE_ANSWER);
            if (reference != null) {
                current.setReferenceAnswer(reference);
                continue;
            }
            String scoringRule = extractValue(text, FIELD_SCORING_RULE);
            if (scoringRule != null) {
                current.setScoringRule(scoringRule);
                continue;
            }
            String followUp = extractValue(text, FIELD_FOLLOW_UP);
            if (followUp != null) {
                current.setFollowUpQuestions(followUp);
                continue;
            }
            String source = extractValue(text, FIELD_SOURCE);
            if (source != null) {
                current.setSourceRef(source);
            }
        }

        if (current != null) {
            validate(current);
            items.add(current);
        }
        return items;
    }

    private QuestionImportItemRespDTO newBaseItem(int rowIndex, QuestionImportReqDTO request) {
        QuestionImportItemRespDTO item = new QuestionImportItemRespDTO();
        item.setRowIndex(rowIndex);
        item.setCollegeId(request.getCollegeId());
        item.setMajorId(request.getMajorId());
        item.setQuestionType(request.getDefaultQuestionType());
        item.setDifficulty(request.getDefaultDifficulty());
        item.setYear(request.getDefaultYear());
        item.setAnswerTimeSeconds(120);
        item.setStatusAfterImport(request.getStatusAfterImport());
        return item;
    }

    private void applyLegacyMetadata(QuestionImportItemRespDTO item, Map<String, String> metadata, QuestionImportReqDTO request) {
        item.setQuestionType(firstNotBlank(metadata.get(FIELD_QUESTION_TYPE), request.getDefaultQuestionType()));
        item.setAbilityTag(metadata.get(FIELD_ABILITY_TAG));
        item.setDifficulty(firstNotBlank(metadata.get(FIELD_DIFFICULTY), request.getDefaultDifficulty()));
        item.setYear(firstNotNull(parseInt(metadata.get(FIELD_YEAR)), request.getDefaultYear()));
    }

    private void applyStrictFields(QuestionImportItemRespDTO item, Map<String, String> strictFields, QuestionImportReqDTO request) {
        String title = strictFields.get(FIELD_TITLE);
        if (StrUtil.isNotBlank(title)) {
            item.setTitle(title);
            item.setContent(title);
        }
        String questionType = strictFields.get(FIELD_QUESTION_TYPE);
        if (StrUtil.isNotBlank(questionType)) {
            item.setQuestionType(questionType);
        } else if (StrUtil.isBlank(item.getQuestionType())) {
            item.setQuestionType(request.getDefaultQuestionType());
        }
        if (strictFields.containsKey(FIELD_ABILITY_TAG)) {
            item.setAbilityTag(strictFields.get(FIELD_ABILITY_TAG));
        }
        String difficulty = strictFields.get(FIELD_DIFFICULTY);
        if (StrUtil.isNotBlank(difficulty)) {
            item.setDifficulty(difficulty);
        } else if (StrUtil.isBlank(item.getDifficulty())) {
            item.setDifficulty(request.getDefaultDifficulty());
        }
        if (strictFields.containsKey(FIELD_REFERENCE_ANSWER)) {
            item.setReferenceAnswer(strictFields.get(FIELD_REFERENCE_ANSWER));
        }
        if (strictFields.containsKey(FIELD_SCORING_RULE)) {
            item.setScoringRule(strictFields.get(FIELD_SCORING_RULE));
        }
        if (strictFields.containsKey(FIELD_FOLLOW_UP)) {
            item.setFollowUpQuestions(strictFields.get(FIELD_FOLLOW_UP));
        }
        if (strictFields.containsKey(FIELD_SOURCE)) {
            item.setSourceRef(strictFields.get(FIELD_SOURCE));
        }
        if (strictFields.containsKey(FIELD_YEAR)) {
            item.setYear(firstNotNull(parseInt(strictFields.get(FIELD_YEAR)), request.getDefaultYear()));
        }
    }

    private Map<String, Integer> buildHeader(XWPFTableRow row) {
        Map<String, Integer> header = new HashMap<>();
        List<XWPFTableCell> cells = row.getTableCells();
        for (int i = 0; i < cells.size(); i++) {
            header.put(normalize(cells.get(i).getText()), i);
        }
        return header;
    }

    private String cell(XWPFTableRow row, Map<String, Integer> header, String key) {
        Integer index = header.get(key);
        if (index == null || index >= row.getTableCells().size()) {
            return null;
        }
        return normalize(row.getCell(index).getText());
    }

    private Map<String, String> parseStrictFields(String text) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String fieldName : List.of(
                FIELD_TITLE,
                FIELD_QUESTION_TYPE,
                FIELD_ABILITY_TAG,
                FIELD_DIFFICULTY,
                FIELD_REFERENCE_ANSWER,
                FIELD_SCORING_RULE,
                FIELD_FOLLOW_UP,
                FIELD_SOURCE,
                FIELD_YEAR)) {
            String value = extractValue(text, fieldName);
            if (value != null) {
                fields.put(fieldName, value);
            }
        }
        return fields;
    }

    private String extractValue(String text, String fieldName) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String normalizedField = normalizeFieldName(fieldName);
        String normalizedText = normalize(text);
        if (!normalizedText.startsWith(normalizedField)) {
            return null;
        }
        String remainder = normalizedText.substring(normalizedField.length()).trim();
        if (remainder.startsWith("*") || remainder.startsWith("＊")) {
            remainder = remainder.substring(1).trim();
        }
        if (remainder.startsWith("\uff1a") || remainder.startsWith(":")) {
            remainder = remainder.substring(1).trim();
            return remainder;
        }
        return null;
    }

    private String normalizeFieldName(String fieldName) {
        return fieldName == null ? "" : fieldName.replace(" ", "").replace("*", "").replace("＊", "").trim();
    }

    private void appendRawText(QuestionImportItemRespDTO item, String text) {
        if (item == null || StrUtil.isBlank(text)) {
            return;
        }
        item.setRawText(StrUtil.isBlank(item.getRawText()) ? text : item.getRawText() + "\n" + text);
    }

    private void validate(QuestionImportItemRespDTO item) {
        if (StrUtil.isBlank(item.getTitle())) {
            item.setValid(false);
            item.setErrorMessage("\u9898\u76ee\u5185\u5bb9\u4e3a\u7a7a");
            return;
        }
        item.setValid(true);
        item.setErrorMessage(null);
    }

    private QuestionImportRespDTO buildResponse(List<QuestionImportItemRespDTO> items) {
        QuestionImportRespDTO response = new QuestionImportRespDTO();
        response.setItems(items);
        response.setTotalRows(items.size());
        response.setValidCount((int) items.stream().filter(QuestionImportItemRespDTO::isValid).count());
        response.setInvalidCount(response.getTotalRows() - response.getValidCount());
        response.setErrors(items.stream()
                .filter(item -> !item.isValid())
                .map(item -> new QuestionImportErrorRespDTO(item.getRowIndex(), item.getErrorMessage()))
                .toList());
        return response;
    }

    private QuestionSaveReqDTO toSaveReq(QuestionImportItemRespDTO item) {
        QuestionSaveReqDTO request = new QuestionSaveReqDTO();
        request.setTitle(item.getTitle());
        request.setContent(firstNotBlank(item.getContent(), item.getTitle()));
        request.setQuestionType(item.getQuestionType());
        request.setCollegeId(item.getCollegeId());
        request.setMajorId(item.getMajorId());
        request.setAbilityTag(item.getAbilityTag());
        request.setDifficulty(item.getDifficulty());
        request.setAnswerTimeSeconds(item.getAnswerTimeSeconds());
        request.setReferenceAnswer(item.getReferenceAnswer());
        request.setScoringRule(item.getScoringRule());
        request.setFollowUpQuestions(item.getFollowUpQuestions());
        request.setSourceRef(item.getSourceRef());
        request.setYear(item.getYear());
        request.setStatus(firstNotBlank(item.getStatusAfterImport(), "pending_review"));
        request.setIsAiGenerated(false);
        return request;
    }

    private Integer parseInt(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer firstNotNull(Integer first, Integer second) {
        return first != null ? first : second;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace('\u00a0', ' ').trim();
    }

    private String firstNotBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : second;
    }
}
