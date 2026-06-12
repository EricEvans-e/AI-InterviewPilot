package com.interviewpilot.interview.shared;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Shared helpers for the mandatory self-introduction opening question.
 */
public final class InterviewOpeningQuestionSupport {

    public static final String SELF_INTRODUCTION_QUESTION =
            "请先做一个简短的自我介绍，重点介绍你的教育背景、相关经历，以及你与应聘方向的匹配度。";

    private static final List<Pattern> SELF_INTRODUCTION_PATTERNS = List.of(
            Pattern.compile("自我介绍"),
            Pattern.compile("介绍一下(你自己|自己)"),
            Pattern.compile("简单介绍一下(你自己|自己)"),
            Pattern.compile("先做.{0,8}自我介绍")
    );

    private InterviewOpeningQuestionSupport() {
    }

    public static List<String> prependSelfIntroduction(List<String> questions) {
        List<String> normalizedQuestions = new ArrayList<>();
        if (questions != null) {
            for (String question : questions) {
                String normalized = normalizeQuestion(question);
                if (normalized != null) {
                    normalizedQuestions.add(normalized);
                }
            }
        }

        String openingQuestion = null;
        List<String> remainingQuestions = new ArrayList<>();
        for (String question : normalizedQuestions) {
            if (isSelfIntroductionQuestion(question)) {
                if (openingQuestion == null) {
                    openingQuestion = question;
                }
                continue;
            }
            remainingQuestions.add(question);
        }

        List<String> result = new ArrayList<>();
        result.add(openingQuestion == null ? SELF_INTRODUCTION_QUESTION : openingQuestion);
        result.addAll(remainingQuestions);
        return result;
    }

    public static LinkedHashMap<String, String> toQuestionMap(List<String> questions) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        List<String> normalizedQuestions = prependSelfIntroduction(questions);
        for (int i = 0; i < normalizedQuestions.size(); i++) {
            result.put(String.valueOf(i + 1), normalizedQuestions.get(i));
        }
        return result;
    }

    public static LinkedHashMap<String, String> prependSelfIntroduction(Map<String, String> questions) {
        return toQuestionMap(extractOrderedValues(questions));
    }

    public static boolean isSyntheticSelfIntroduction(Map<String, String> questions) {
        if (questions == null || questions.isEmpty()) {
            return false;
        }
        for (String question : extractOrderedValues(questions)) {
            return isSelfIntroductionQuestion(question);
        }
        return false;
    }

    public static boolean isSelfIntroductionQuestion(String question) {
        String normalized = normalizeQuestion(question);
        if (normalized == null) {
            return false;
        }
        for (Pattern pattern : SELF_INTRODUCTION_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return true;
            }
        }
        return false;
    }

    public static int resolveQuestionBankSeqIndex(Map<String, String> questions, String questionNumber) {
        Integer mainQuestionNumber = parseMainQuestionNumber(questionNumber);
        if (mainQuestionNumber == null) {
            return -1;
        }
        int offset = isSyntheticSelfIntroduction(questions) ? 1 : 0;
        int seqIndex = mainQuestionNumber - 1 - offset;
        return Math.max(seqIndex, -1);
    }

    private static List<String> extractOrderedValues(Map<String, String> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }
        return questions.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry<String, String>::getKey, InterviewOpeningQuestionSupport::compareQuestionNumber))
                .map(Map.Entry::getValue)
                .map(InterviewOpeningQuestionSupport::normalizeQuestion)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private static Integer parseMainQuestionNumber(String questionNumber) {
        if (StrUtil.isBlank(questionNumber)) {
            return null;
        }
        String normalized = questionNumber.trim();
        int followUpSeparator = normalized.indexOf("-F");
        if (followUpSeparator > 0) {
            normalized = normalized.substring(0, followUpSeparator);
        }
        try {
            int parsed = Integer.parseInt(normalized);
            return parsed > 0 ? parsed : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static int compareQuestionNumber(String left, String right) {
        Integer leftNumber = parseMainQuestionNumber(left);
        Integer rightNumber = parseMainQuestionNumber(right);
        if (leftNumber != null && rightNumber != null) {
            return Integer.compare(leftNumber, rightNumber);
        }
        return StrUtil.nullToEmpty(left).compareTo(StrUtil.nullToEmpty(right));
    }

    private static String normalizeQuestion(String question) {
        String normalized = StrUtil.trimToNull(question);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }
}
