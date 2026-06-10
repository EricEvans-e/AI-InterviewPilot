package com.interviewpilot.questionbank.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.ai.service.AiPropertiesService;
import com.interviewpilot.ai.service.chat.AiChatHandler;
import com.interviewpilot.ai.service.chat.AiChatHandlerFactory;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.questionbank.api.io.req.QuestionExpandReqDTO;
import com.interviewpilot.questionbank.api.io.req.QuestionGenerateReqDTO;
import com.interviewpilot.questionbank.dao.entity.CollegeDO;
import com.interviewpilot.questionbank.dao.entity.MajorDO;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目 AI 生成与拓题服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionAiGenerateService {

    private final AiChatHandlerFactory aiChatHandlerFactory;
    private final AiPropertiesService aiPropertiesService;
    private final CollegeService collegeService;
    private final MajorService majorService;
    private final QuestionBankService questionBankService;

    public List<QuestionDO> generateQuestions(QuestionGenerateReqDTO req) {
        String prompt = buildGeneratePrompt(req);
        String response = callAiSync(prompt, req.getAiType(), req.getAiPropertiesId());
        List<QuestionDO> questions = parseQuestionsFromAi(
                response,
                req.getCollegeId(),
                req.getMajorId(),
                req.getQuestionType(),
                req.getAbilityTag(),
                req.getDifficulty(),
                true,
                req.getGenerateScoringRule(),
                req.getGenerateFollowUp()
        );
        markGeneratedQuestions(questions);
        return questions;
    }

    public List<QuestionDO> expandQuestions(QuestionExpandReqDTO req) {
        List<QuestionDO> referenceQuestions = questionBankService.listByIds(req.getReferenceQuestionIds());
        if (CollUtil.isEmpty(referenceQuestions)) {
            throw new ClientException("未找到可用的参考题目");
        }

        String prompt = buildExpandPrompt(req, referenceQuestions);
        String response = callAiSync(prompt, req.getAiType(), req.getAiPropertiesId());
        List<QuestionDO> questions = parseQuestionsFromAi(
                response,
                req.getCollegeId(),
                req.getMajorId(),
                req.getQuestionType(),
                req.getAbilityTag(),
                req.getDifficulty(),
                req.getGenerateReferenceAnswer(),
                req.getGenerateScoringRule(),
                req.getGenerateFollowUp()
        );
        log.info("AI expand generated raw question count, requestedCount={}, actualCount={}",
                req.getCount(), questions.size());
        questions = limitExpandedQuestions(questions, req.getCount());
        markGeneratedQuestions(questions);
        return questions;
    }

    private void markGeneratedQuestions(List<QuestionDO> questions) {
        questions.forEach(question -> {
            question.setIsAiGenerated(true);
            question.setStatus("pending_review");
        });
    }

    private String buildGeneratePrompt(QuestionGenerateReqDTO req) {
        String collegeName = resolveCollegeName(req.getCollegeId());
        String majorName = resolveMajorName(req.getMajorId());

        return String.format("""
                你是一位高职院校面试题设计专家。请根据以下条件生成 %d 道新的面试题：
                目标院校：%s
                目标专业：%s
                题型：%s
                能力点：%s
                难度：%s
                生成追问：%s
                生成评分标准：%s

                输出要求：
                1. 只返回 JSON 数组，不要输出额外解释。
                2. 每个元素包含以下字段：
                   - title
                   - content
                   - questionType
                   - abilityTag
                   - difficulty
                   - answerTimeSeconds
                   - referenceAnswer
                   - scoringRule
                   - followUpQuestions
                   - sourceRef
                3. 若未要求生成追问，则 followUpQuestions 返回空字符串。
                4. 若未要求生成评分标准，则 scoringRule 返回空字符串。
                5. 题目内容必须适配目标院校和专业背景，避免重复和空泛表述。
                """,
                req.getCount(),
                collegeName,
                majorName,
                req.getQuestionType(),
                StrUtil.blankToDefault(req.getAbilityTag(), "综合能力"),
                StrUtil.blankToDefault(req.getDifficulty(), "medium"),
                Boolean.TRUE.equals(req.getGenerateFollowUp()) ? "是" : "否",
                Boolean.TRUE.equals(req.getGenerateScoringRule()) ? "是" : "否"
        );
    }

    private String buildExpandPrompt(QuestionExpandReqDTO req, List<QuestionDO> referenceQuestions) {
        String collegeName = resolveCollegeName(req.getCollegeId());
        String majorName = resolveMajorName(req.getMajorId());
        String referenceBlock = referenceQuestions.stream()
                .map(question -> String.format(
                        "- 参考题ID: %d%n  标题: %s%n  内容: %s",
                        question.getId(),
                        StrUtil.blankToDefault(question.getTitle(), "无标题"),
                        StrUtil.blankToDefault(question.getContent(), "无正文")
                ))
                .collect(Collectors.joining("%n"));

        return String.format("""
                你是一位高职院校面试题设计专家。现在请基于一批参考题做 AI 拓题。
                目标配置（由人工指定，不允许继承参考题元数据）：
                - 目标院校：%s
                - 目标专业：%s
                - 题型：%s
                - 能力点：%s
                - 难度：%s
                - 生成数量：%d
                - 生成参考答案：%s
                - 生成追问：%s
                - 生成评分标准：%s

                参考题如下：
                %s

                拓题要求：
                1. 学习参考题的考查风格、表达粒度和能力导向，但不要照抄标题、场景和表述。
                2. 生成的新题要优先做去重扩展，避免与参考题重复，也避免新题之间相互重复。
                3. 不要继承参考题的院校、专业或其他元数据，一律以本次目标配置为准。
                4. 如果目标院校或专业为空，则按通用面试场景生成。
                5. 如果未要求生成参考答案、追问、评分标准，对应字段返回空字符串。
                6. 只返回 JSON 数组，不要输出任何说明。
                JSON 每个元素必须包含：
                - title
                - content
                - questionType
                - abilityTag
                - difficulty
                - answerTimeSeconds
                - referenceAnswer
                - scoringRule
                - followUpQuestions
                - sourceRef
                """,
                collegeName,
                majorName,
                req.getQuestionType(),
                StrUtil.blankToDefault(req.getAbilityTag(), "综合能力"),
                StrUtil.blankToDefault(req.getDifficulty(), "medium"),
                req.getCount(),
                Boolean.TRUE.equals(req.getGenerateReferenceAnswer()) ? "是" : "否",
                Boolean.TRUE.equals(req.getGenerateFollowUp()) ? "是" : "否",
                Boolean.TRUE.equals(req.getGenerateScoringRule()) ? "是" : "否",
                referenceBlock
        );
    }

    private String resolveCollegeName(Long collegeId) {
        if (collegeId == null) {
            return "通用";
        }
        CollegeDO college = collegeService.getById(collegeId);
        return college == null ? "通用" : StrUtil.blankToDefault(college.getName(), "通用");
    }

    private String resolveMajorName(Long majorId) {
        if (majorId == null) {
            return "通用";
        }
        MajorDO major = majorService.getById(majorId);
        return major == null ? "通用" : StrUtil.blankToDefault(major.getName(), "通用");
    }

    private String callAiSync(String prompt, String aiType, Long aiPropertiesId) {
        AiPropertiesDO aiProperties;

        if (aiPropertiesId != null) {
            aiProperties = aiPropertiesService.getById(aiPropertiesId);
            if (aiProperties == null || Integer.valueOf(1).equals(aiProperties.getDelFlag()) || aiProperties.getIsEnabled() != 1) {
                throw new ClientException("指定的 AI 配置不存在或未启用");
            }
        } else if (StrUtil.isNotBlank(aiType)) {
            aiProperties = aiPropertiesService.getEnabledByAiType(aiType);
        } else {
            aiProperties = aiPropertiesService.getDefaultMimoConfig();
        }

        if (aiProperties == null) {
            throw new ClientException("未找到可用的 AI 配置，请先启用至少一个模型");
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
            throw new ClientException("AI 调用失败: " + e.getMessage());
        }
    }

    private List<QuestionDO> parseQuestionsFromAi(String response,
                                                  Long collegeId,
                                                  Long majorId,
                                                  String questionType,
                                                  String abilityTag,
                                                  String difficulty,
                                                  Boolean generateReferenceAnswer,
                                                  Boolean generateScoringRule,
                                                  Boolean generateFollowUp) {
        try {
            String jsonStr = extractJsonArray(response);
            JSONArray jsonArray = JSON.parseArray(jsonStr);
            List<QuestionDO> questions = new ArrayList<>();

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                QuestionDO question = new QuestionDO();
                question.setTitle(obj.getString("title"));
                question.setContent(obj.getString("content"));
                question.setQuestionType(StrUtil.blankToDefault(obj.getString("questionType"), questionType));
                question.setAbilityTag(StrUtil.blankToDefault(obj.getString("abilityTag"), abilityTag));
                question.setDifficulty(StrUtil.blankToDefault(obj.getString("difficulty"), difficulty));
                question.setAnswerTimeSeconds(obj.getInteger("answerTimeSeconds"));
                question.setReferenceAnswer(Boolean.TRUE.equals(generateReferenceAnswer) ? blankToNull(obj.getString("referenceAnswer")) : null);
                question.setScoringRule(Boolean.TRUE.equals(generateScoringRule) ? blankToNull(obj.getString("scoringRule")) : null);
                question.setFollowUpQuestions(Boolean.TRUE.equals(generateFollowUp) ? blankToNull(obj.getString("followUpQuestions")) : null);
                question.setSourceRef(blankToNull(obj.getString("sourceRef")));
                question.setCollegeId(collegeId);
                question.setMajorId(majorId);
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

    private String extractJsonArray(String response) {
        String jsonStr = response.trim();
        if (jsonStr.contains("```json")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
            jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("```"));
        } else if (jsonStr.contains("```")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
            jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("```"));
        }

        int start = jsonStr.indexOf('[');
        int end = jsonStr.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return jsonStr.substring(start, end + 1);
        }
        return jsonStr;
    }

    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : value;
    }

    private List<QuestionDO> limitExpandedQuestions(List<QuestionDO> questions, Integer requestedCount) {
        if (CollUtil.isEmpty(questions) || requestedCount == null || requestedCount <= 0) {
            return questions;
        }
        if (questions.size() <= requestedCount) {
            return questions;
        }
        log.warn("AI expand returned more questions than requested, requestedCount={}, actualCount={}, truncatedCount={}",
                requestedCount, questions.size(), requestedCount);
        return new ArrayList<>(questions.subList(0, requestedCount));
    }
}
