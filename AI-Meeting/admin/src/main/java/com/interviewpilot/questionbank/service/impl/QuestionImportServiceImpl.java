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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionImportServiceImpl implements QuestionImportService {

    private static final Pattern SECTION_META_PATTERN = Pattern.compile("【([^】]+)】([^【]+)");
    private static final Pattern SECTION_QUESTION_PATTERN = Pattern.compile("^\\s*\\d+\\s*[.、]\\s*(.+)$");
    private static final List<String> VALID_STATUSES = List.of("draft", "pending_review", "approved", "rejected");

    private final QuestionBankService questionBankService;
    private final Map<String, QuestionImportRespDTO> batches = new ConcurrentHashMap<>();

    @Override
    public QuestionImportRespDTO preview(MultipartFile file, QuestionImportReqDTO request, Long creatorId) {
        if (file == null || file.isEmpty()) {
            throw new ClientException("导入文件不能为空");
        }
        QuestionImportReqDTO normalized = normalizeRequest(request);
        try (InputStream inputStream = file.getInputStream(); XWPFDocument document = new XWPFDocument(inputStream)) {
            List<QuestionImportItemRespDTO> items = switch (normalized.getImportType()) {
                case "word_table" -> parseTables(document, normalized);
                case "word_section" -> parseSections(document, normalized);
                default -> throw new ClientException("暂不支持的导入类型：" + normalized.getImportType());
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
            throw new ClientException("Word 文件解析失败：" + ex.getMessage());
        }
    }

    @Override
    public QuestionImportRespDTO confirm(String batchId, Long creatorId) {
        QuestionImportRespDTO batch = batches.get(batchId);
        if (batch == null) {
            throw new ClientException("导入批次不存在或已过期");
        }
        if ("IMPORTED".equals(batch.getStatus())) {
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
        batch.setStatus("IMPORTED");
        return batch;
    }

    @Override
    public QuestionImportRespDTO getBatch(String batchId) {
        QuestionImportRespDTO batch = batches.get(batchId);
        if (batch == null) {
            throw new ClientException("导入批次不存在或已过期");
        }
        return batch;
    }

    private QuestionImportReqDTO normalizeRequest(QuestionImportReqDTO request) {
        QuestionImportReqDTO normalized = request == null ? new QuestionImportReqDTO() : request;
        if (StrUtil.isBlank(normalized.getImportType())) {
            normalized.setImportType("word_table");
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
            throw new ClientException("无效的导入后状态：" + normalized.getStatusAfterImport());
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
                item.setTitle(cell(row, header, "题目"));
                item.setContent(item.getTitle());
                item.setQuestionType(firstNotBlank(cell(row, header, "题型"), request.getDefaultQuestionType()));
                item.setAbilityTag(cell(row, header, "能力点"));
                item.setDifficulty(firstNotBlank(cell(row, header, "难度"), request.getDefaultDifficulty()));
                item.setReferenceAnswer(cell(row, header, "参考答案"));
                item.setScoringRule(cell(row, header, "评分规则"));
                item.setFollowUpQuestions(cell(row, header, "追问题"));
                item.setSourceRef(cell(row, header, "来源"));
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
        int rowIndex = 0;
        Map<String, String> metadata = new HashMap<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = normalize(paragraph.getText());
            if (StrUtil.isBlank(text)) {
                continue;
            }
            Matcher metaMatcher = SECTION_META_PATTERN.matcher(text);
            while (metaMatcher.find()) {
                metadata.put(metaMatcher.group(1).trim(), metaMatcher.group(2).trim());
            }

            Matcher questionMatcher = SECTION_QUESTION_PATTERN.matcher(text);
            if (questionMatcher.matches()) {
                if (current != null) {
                    validate(current);
                    items.add(current);
                }
                rowIndex++;
                current = newBaseItem(rowIndex, request);
                applyMetadata(current, metadata, request);
                current.setTitle(questionMatcher.group(1).trim());
                current.setContent(current.getTitle());
                current.setRawText(text);
                continue;
            }

            if (current == null) {
                continue;
            }
            current.setRawText(firstNotBlank(current.getRawText(), "") + "\n" + text);
            if (startsWithAny(text, "参考答案：", "参考答案:")) {
                current.setReferenceAnswer(afterColon(text));
            } else if (startsWithAny(text, "评分规则：", "评分规则:")) {
                current.setScoringRule(afterColon(text));
            } else if (startsWithAny(text, "追问题：", "追问题:", "追问：", "追问:")) {
                current.setFollowUpQuestions(afterColon(text));
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

    private void applyMetadata(QuestionImportItemRespDTO item, Map<String, String> metadata, QuestionImportReqDTO request) {
        item.setQuestionType(firstNotBlank(metadata.get("题型"), request.getDefaultQuestionType()));
        item.setAbilityTag(metadata.get("能力点"));
        item.setDifficulty(firstNotBlank(metadata.get("难度"), request.getDefaultDifficulty()));
        item.setYear(parseInt(firstNotBlank(metadata.get("年份"), String.valueOf(request.getDefaultYear()))));
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

    private void validate(QuestionImportItemRespDTO item) {
        if (StrUtil.isBlank(item.getTitle())) {
            item.setValid(false);
            item.setErrorMessage("题目内容为空");
            return;
        }
        if (StrUtil.isBlank(item.getQuestionType())) {
            item.setValid(false);
            item.setErrorMessage("题型为空");
            return;
        }
        item.setValid(true);
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

    private boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String afterColon(String value) {
        int chinese = value.indexOf('：');
        int english = value.indexOf(':');
        int index;
        if (chinese < 0) {
            index = english;
        } else if (english < 0) {
            index = chinese;
        } else {
            index = Math.min(chinese, english);
        }
        return index >= 0 ? value.substring(index + 1).trim() : value.trim();
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
