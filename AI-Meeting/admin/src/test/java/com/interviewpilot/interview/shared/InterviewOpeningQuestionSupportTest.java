package com.interviewpilot.interview.shared;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewOpeningQuestionSupportTest {

    @Test
    void shouldPrependCanonicalSelfIntroductionWhenMissing() {
        List<String> normalized = InterviewOpeningQuestionSupport.prependSelfIntroduction(List.of(
                "请介绍一下你做过的项目。",
                "你如何处理线上故障？"
        ));

        assertEquals(3, normalized.size());
        assertEquals(InterviewOpeningQuestionSupport.SELF_INTRODUCTION_QUESTION, normalized.get(0));
        assertEquals("请介绍一下你做过的项目。", normalized.get(1));
    }

    @Test
    void shouldMoveExistingSelfIntroductionToTheFrontWithoutDuplicatingIt() {
        List<String> normalized = InterviewOpeningQuestionSupport.prependSelfIntroduction(List.of(
                "请介绍一下你做过的项目。",
                "请先做一个自我介绍。",
                "你如何处理线上故障？"
        ));

        assertEquals(List.of(
                "请先做一个自我介绍。",
                "请介绍一下你做过的项目。",
                "你如何处理线上故障？"
        ), normalized);
    }

    @Test
    void shouldBuildOrderedQuestionMapWithSelfIntroduction() {
        Map<String, String> normalized = InterviewOpeningQuestionSupport.prependSelfIntroduction(new LinkedHashMap<>(Map.of(
                "2", "你如何处理线上故障？",
                "1", "请介绍一下你做过的项目。"
        )));

        assertEquals(InterviewOpeningQuestionSupport.SELF_INTRODUCTION_QUESTION, normalized.get("1"));
        assertEquals("请介绍一下你做过的项目。", normalized.get("2"));
        assertEquals("你如何处理线上故障？", normalized.get("3"));
    }

    @Test
    void shouldResolveQuestionBankSeqIndexWithSyntheticOpeningQuestion() {
        Map<String, String> questions = InterviewOpeningQuestionSupport.toQuestionMap(List.of(
                "请介绍一下你做过的项目。",
                "你如何处理线上故障？"
        ));

        assertEquals(-1, InterviewOpeningQuestionSupport.resolveQuestionBankSeqIndex(questions, "1"));
        assertEquals(0, InterviewOpeningQuestionSupport.resolveQuestionBankSeqIndex(questions, "2"));
        assertEquals(1, InterviewOpeningQuestionSupport.resolveQuestionBankSeqIndex(questions, "3"));
        assertTrue(InterviewOpeningQuestionSupport.isSyntheticSelfIntroduction(questions));
    }

    @Test
    void shouldKeepLegacyQuestionBankSeqIndexWhenOpeningQuestionIsNotSynthetic() {
        Map<String, String> legacyQuestions = new LinkedHashMap<>();
        legacyQuestions.put("1", "请介绍一下你做过的项目。");
        legacyQuestions.put("2", "你如何处理线上故障？");

        assertFalse(InterviewOpeningQuestionSupport.isSyntheticSelfIntroduction(legacyQuestions));
        assertEquals(0, InterviewOpeningQuestionSupport.resolveQuestionBankSeqIndex(legacyQuestions, "1"));
        assertEquals(1, InterviewOpeningQuestionSupport.resolveQuestionBankSeqIndex(legacyQuestions, "2"));
    }
}
