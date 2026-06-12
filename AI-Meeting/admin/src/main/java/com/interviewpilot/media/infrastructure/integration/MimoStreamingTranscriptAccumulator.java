package com.interviewpilot.media.infrastructure.integration;

import cn.hutool.core.util.StrUtil;

final class MimoStreamingTranscriptAccumulator {

    private MimoStreamingTranscriptAccumulator() {
    }

    static String merge(String currentText, String incomingText) {
        String current = normalize(currentText);
        String incoming = normalize(incomingText);

        if (StrUtil.isBlank(current)) {
            return incoming;
        }
        if (StrUtil.isBlank(incoming)) {
            return current;
        }
        if (incoming.equals(current)) {
            return current;
        }
        if (incoming.startsWith(current)) {
            return incoming;
        }
        if (current.startsWith(incoming)) {
            return current;
        }

        int overlap = longestSuffixPrefixOverlap(current, incoming);
        if (overlap > 0) {
            return current + incoming.substring(overlap);
        }

        if (incoming.length() <= 3 && !current.contains(incoming)) {
            return current + incoming;
        }

        return current.length() >= incoming.length() ? current : incoming;
    }

    private static int longestSuffixPrefixOverlap(String current, String incoming) {
        int max = Math.min(current.length(), incoming.length());
        for (int size = max; size > 0; size--) {
            if (current.regionMatches(current.length() - size, incoming, 0, size)) {
                return size;
            }
        }
        return 0;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
