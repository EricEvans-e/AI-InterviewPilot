package com.interviewpilot.media.infrastructure.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MimoStreamingTranscriptAccumulatorTest {

    @Test
    void merge_ShouldPreferExpandedSnapshotWhenProviderReturnsFullText() {
        assertEquals("你好杭州",
                MimoStreamingTranscriptAccumulator.merge("", "你好杭州"));
        assertEquals("你好杭州科技",
                MimoStreamingTranscriptAccumulator.merge("你好杭州", "你好杭州科技"));
    }

    @Test
    void merge_ShouldAppendDeltaWhenProviderReturnsIncrementalSuffix() {
        assertEquals("你好杭州科技",
                MimoStreamingTranscriptAccumulator.merge("你好杭州", "科技"));
    }

    @Test
    void merge_ShouldIgnoreRegressiveChunksThatContainLessText() {
        assertEquals("你好杭州科技",
                MimoStreamingTranscriptAccumulator.merge("你好杭州科技", "你好杭州"));
    }

    @Test
    void merge_ShouldUseOverlapToAvoidDuplicateText() {
        assertEquals("我想报考人工智能专业",
                MimoStreamingTranscriptAccumulator.merge("我想报考人工智能", "人工智能专业"));
    }
}
