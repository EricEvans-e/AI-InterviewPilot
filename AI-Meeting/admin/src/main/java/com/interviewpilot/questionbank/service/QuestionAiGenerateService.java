package com.interviewpilot.questionbank.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.ai.service.AiPropertiesService;
import com.interviewpilot.ai.service.chat.AiChatHandler;
import com.interviewpilot.ai.service.chat.AiChatHandlerFactory;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.questionbank.api.io.req.QuestionGenerateReqDTO;
import com.interviewpilot.questionbank.dao.entity.CollegeDO;
import com.interviewpilot.questionbank.dao.entity.MajorDO;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import com.interviewpilot.questionbank.service.CollegeService;
import com.interviewpilot.questionbank.service.MajorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 题目AI生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionAiGenerateService {

    private final AiChatHandlerFactory aiChatHandlerFactory;
    private final AiPropertiesService aiPropertiesService;
    private final CollegeService collegeService;
    private final MajorService majorService;

    /**
     * 基于条件 AI 生成题目
     *
     * @param req 生成请求参数
     * @return 生成的题目列表
     */
    public List<QuestionDO> generateQuestions(QuestionGenerateReqDTO req) {
        // 1. Build prompt
        String prompt = buildGeneratePrompt(req);

        // 2. Call AI (sync mode, non-streaming)
        String response = callAiSync(prompt, req.getAiType(), req.getAiPropertiesId());

        // 3. Parse JSON response to QuestionDO list
        List<QuestionDO> questions = parseQuestionsFromAi(response, req);

        // 4. Mark as AI generated + pending review
        questions.forEach(q -> {
            q.setIsAiGenerated(true);
            q.setStatus("pending_review");
        });

        return questions;
    }

    private String buildGeneratePrompt(QuestionGenerateReqDTO req) {
        String collegeName = "通用";
        if (req.getCollegeId() != null) {
            CollegeDO college = collegeService.getById(req.getCollegeId());
            if (college != null) {
                collegeName = college.getName();
            }
        }
        String majorName = "通用";
        if (req.getMajorId() != null) {
            MajorDO major = majorService.getById(req.getMajorId());
            if (major != null) {
                majorName = major.getName();
            }
        }

        return String.format("""
            你是一位浙江高职提前招生面试出题专家。请根据以下条件生成 %d 道面试题：

            目标院校：%s
            目标专业：%s
            题型：%s
            能力点：%s
            难度：%s
            是否生成追问题：%s
            是否生成评分标准：%s

            请以 JSON 数组格式返回，每道题包含：
            - title: 题目标题
            - content: 题目正文
            - questionType: 题型
            - abilityTag: 能力点
            - difficulty: 难度
            - answerTimeSeconds: 建议答题时间(秒)
            - referenceAnswer: 参考答案/答题要点
            - scoringRule: 评分规则(JSON字符串)
            - followUpQuestions: 预设追问题(JSON数组字符串)
            - sourceRef: 来源依据说明

            只返回 JSON 数组，不要其他文字。
            """,
                req.getCount(),
                collegeName,
                majorName,
                req.getQuestionType(),
                req.getAbilityTag() != null ? req.getAbilityTag() : "综合",
                req.getDifficulty() != null ? req.getDifficulty() : "medium",
                req.getGenerateFollowUp() ? "是" : "否",
                req.getGenerateScoringRule() ? "是" : "否"
        );
    }

    private String callAiSync(String prompt, String aiType, Long aiPropertiesId) {
        AiPropertiesDO aiProperties;

        if (aiPropertiesId != null) {
            aiProperties = aiPropertiesService.getById(aiPropertiesId);
            if (aiProperties == null || aiProperties.getDelFlag() == 1 || aiProperties.getIsEnabled() != 1) {
                throw new ClientException("指定的AI配置不存在或未启用");
            }
        } else if (StrUtil.isNotBlank(aiType)) {
            aiProperties = aiPropertiesService.getEnabledByAiType(aiType);
        } else {
            // 使用默认AI配置（优先 DeepSeek）
            aiProperties = aiPropertiesService.getEnabledByAiType("deepseek");
            if (aiProperties == null) {
                aiProperties = aiPropertiesService.getDefaultDoubaoConfig();
            }
        }

        if (aiProperties == null) {
            throw new ClientException("未找到可用的AI配置，请先在AI配置中添加并启用至少一个AI模型");
        }

        try {
            AiChatHandler handler = aiChatHandlerFactory.getHandler(aiProperties.getAiType());
            if (handler == null) {
                throw new ClientException("不支持的 AI 类型: " + aiProperties.getAiType());
            }
            String response = handler.callSync(aiProperties, prompt);
            if (StrUtil.isBlank(response)) {
                throw new ClientException("AI 返回为空，请稍后重试");
            }
            return response;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 调用失败", e);
            throw new ClientException("AI 调用失败：" + e.getMessage());
        }
    }

    private List<QuestionDO> parseQuestionsFromAi(String response, QuestionGenerateReqDTO req) {
        try {
            // 尝试提取 JSON 数组（AI 可能返回带有 markdown 代码块）
            String jsonStr = response.trim();
            if (jsonStr.contains("```json")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("```"));
            } else if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
                jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("```"));
            }

            // 尝试找到 JSON 数组
            int start = jsonStr.indexOf('[');
            int end = jsonStr.lastIndexOf(']');
            if (start >= 0 && end > start) {
                jsonStr = jsonStr.substring(start, end + 1);
            }

            JSONArray jsonArray = JSON.parseArray(jsonStr);
            List<QuestionDO> questions = new ArrayList<>();

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                QuestionDO question = new QuestionDO();
                question.setTitle(obj.getString("title"));
                question.setContent(obj.getString("content"));
                question.setQuestionType(obj.getString("questionType"));
                question.setAbilityTag(obj.getString("abilityTag"));
                question.setDifficulty(obj.getString("difficulty"));
                question.setAnswerTimeSeconds(obj.getInteger("answerTimeSeconds"));
                question.setReferenceAnswer(obj.getString("referenceAnswer"));
                question.setScoringRule(obj.getString("scoringRule"));
                question.setFollowUpQuestions(obj.getString("followUpQuestions"));
                question.setSourceRef(obj.getString("sourceRef"));
                questions.add(question);
            }

            if (questions.isEmpty()) {
                throw new ClientException("AI 未能生成任何题目，请调整条件后重试");
            }

            return questions;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析 AI 返回结果失败，原始响应：{}", response, e);
            throw new ClientException("解析 AI 返回结果失败，请稍后重试");
        }
    }
}
