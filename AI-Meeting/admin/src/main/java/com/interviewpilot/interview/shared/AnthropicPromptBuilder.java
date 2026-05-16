package com.interviewpilot.interview.shared;

import java.util.Map;

/**
 * 将 XingChen workflow 结构化参数转换为 Anthropic 纯文本 prompt。
 * 供 InterviewAiInvoker 在 aiProvider=anthropic 时使用。
 */
public final class AnthropicPromptBuilder {

    private AnthropicPromptBuilder() {
    }

    /**
     * 根据 parameters 内容自动判断场景并生成 prompt。
     */
    public static String build(String input, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return input;
        }

        String mode = getStr(parameters, "mode");

        // 追问场景
        if ("FOLLOW_UP".equalsIgnoreCase(mode)) {
            return buildFollowUpPrompt(parameters, input);
        }

        // 评分场景（含 question 和 AGENT_USER_INPUT）
        String question = getStr(parameters, "question");
        if (!question.isEmpty()) {
            return buildEvaluationPrompt(parameters, input, question);
        }

        // 简历出题场景（含 resume_text）
        String resumeText = getStr(parameters, "resume_text");
        if (!resumeText.isEmpty()) {
            return buildExtractionPrompt(resumeText);
        }

        // 通用 fallback
        return input;
    }

    private static String buildEvaluationPrompt(Map<String, Object> parameters, String answer, String question) {
        String resumeContext = getStr(parameters, "resume_context");

        StringBuilder sb = new StringBuilder();
        sb.append("你是一位面试评分专家。请对以下面试回答进行评分。\n\n");
        sb.append("【面试题目】\n").append(question).append("\n\n");
        sb.append("【候选人回答】\n").append(answer).append("\n\n");
        if (!resumeContext.isEmpty()) {
            sb.append("【简历背景】\n").append(resumeContext).append("\n\n");
        }
        sb.append("请以 JSON 格式返回：\n");
        sb.append("{\"score\":0-100,\"feedback\":\"评分理由\",\"follow_up_needed\":true或false,\"follow_up_question\":\"追问问题（如需要）\",\"missing_points\":[\"遗漏要点\"]}\n");
        sb.append("只返回 JSON，不要其他内容。");
        return sb.toString();
    }

    private static String buildFollowUpPrompt(Map<String, Object> parameters, String answer) {
        String question = getStr(parameters, "question");
        String resumeContext = getStr(parameters, "resume_context");
        int followUpCount = getInt(parameters, "follow_up_count");
        int maxFollowUp = getInt(parameters, "max_follow_up");

        StringBuilder sb = new StringBuilder();
        sb.append("你是面试官，正在进行追问。");
        if (maxFollowUp > 0) {
            sb.append("这是第 ").append(followUpCount).append("/").append(maxFollowUp).append(" 次追问。\n\n");
        } else {
            sb.append("\n\n");
        }
        if (!question.isEmpty()) {
            sb.append("【原题】\n").append(question).append("\n\n");
        }
        sb.append("【候选人回答】\n").append(answer).append("\n\n");
        if (!resumeContext.isEmpty()) {
            sb.append("【简历背景】\n").append(resumeContext).append("\n\n");
        }
        sb.append("请生成一个有针对性的追问问题，帮助深入了解候选人的能力和经验。\n");
        sb.append("如果回答已经非常完整，不需要追问，请返回 __FINISH__。\n");
        sb.append("只返回追问问题或 __FINISH__，不要其他内容。");
        return sb.toString();
    }

    private static String buildExtractionPrompt(String resumeText) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的面试出题官。请根据以下简历内容，生成有针对性的面试题目。\n\n");
        sb.append("【简历内容】\n").append(resumeText).append("\n\n");
        sb.append("请以 JSON 格式返回，包含以下字段：\n");
        sb.append("- questions: 面试题目数组，包含 3-5 个针对性问题\n");
        sb.append("- sugest: 面试建议数组，包含 2-3 条针对该简历的面试建议\n");
        sb.append("- type: 面试方向/类型（如：Java开发、产品经理等）\n");
        sb.append("- resumeScore: 简历评分（0-100 的整数）\n\n");
        sb.append("只返回 JSON，不要其他内容。");
        return sb.toString();
    }

    private static String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? "" : val.toString().trim();
    }

    private static int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.parseInt(val.toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
