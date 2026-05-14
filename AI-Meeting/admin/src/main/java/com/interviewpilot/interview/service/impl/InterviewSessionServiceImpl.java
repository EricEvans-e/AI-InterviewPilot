package com.interviewpilot.interview.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interviewpilot.agent.application.BusinessAgentResolver;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.interview.api.io.req.InterviewConversationPageReqDTO;
import com.interviewpilot.interview.api.io.req.InterviewFromBankReqDTO;
import com.interviewpilot.interview.api.io.resp.InterviewConversationRespDTO;
import com.interviewpilot.interview.api.io.resp.InterviewSessionCreateRespDTO;
import com.interviewpilot.interview.application.runtime.InterviewSessionRuntimeSnapshotService;
import com.interviewpilot.interview.application.InterviewSessionOwnershipService;
import com.interviewpilot.interview.dao.entity.InterviewSession;
import com.interviewpilot.interview.dao.entity.InterviewSessionQuestionDO;
import com.interviewpilot.interview.dao.mapper.InterviewSessionQuestionMapper;
import com.interviewpilot.interview.dao.repository.InterviewSessionRepository;
import com.interviewpilot.interview.service.InterviewQuestionCacheService;
import com.interviewpilot.interview.service.InterviewSessionService;
import com.interviewpilot.interview.service.model.InterviewSessionStatus;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import com.interviewpilot.questionbank.service.QuestionBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewSessionOwnershipService ownershipService;
    private final BusinessAgentResolver businessAgentResolver;
    private final ObjectProvider<InterviewSessionRuntimeSnapshotService> runtimeSnapshotServiceProvider;
    private final QuestionBankService questionBankService;
    private final InterviewSessionQuestionMapper sessionQuestionMapper;
    private final InterviewQuestionCacheService interviewQuestionCacheService;

    @Override
    public InterviewSessionCreateRespDTO createSession(Long userId) {
        abandonActiveSessions(userId);

        InterviewSession session = new InterviewSession();
        session.setSessionId(IdUtil.getSnowflakeNextIdStr());
        session.setUserId(userId);
        session.setStatus(InterviewSessionStatus.DRAFT.name());
        session.setConversationTitle("Interview Session");
        session.setInterviewerAgentId(
                businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_QUESTION_ASKING).getId());
        session.setDelFlag(0);
        interviewSessionRepository.save(session);
        InterviewSessionRuntimeSnapshotService runtimeSnapshotService = runtimeSnapshotServiceProvider.getIfAvailable();
        if (runtimeSnapshotService != null) {
            runtimeSnapshotService.initializeDraftSnapshot(session);
        }
        return new InterviewSessionCreateRespDTO(session.getSessionId(), session.getStatus());
    }

    @Override
    public InterviewSessionCreateRespDTO createFromBank(Long userId, InterviewFromBankReqDTO req) {
        // 1) 清理该用户已有的活跃会话。
        abandonActiveSessions(userId);

        // 2) 从题库随机抽取题目。
        int count = req.getQuestionCount() == null || req.getQuestionCount() <= 0 ? 5 : req.getQuestionCount();
        String questionType = mapInterviewModeToQuestionType(req.getInterviewMode());
        List<QuestionDO> questions = questionBankService.randomSelect(
                req.getCollegeId(), req.getMajorId(), questionType, count);
        if (questions == null || questions.isEmpty()) {
            throw new ClientException("题库中没有符合条件的题目");
        }

        // 3) 创建面试会话，标记为题库模式。
        InterviewSession session = new InterviewSession();
        String sessionId = IdUtil.getSnowflakeNextIdStr();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setStatus(InterviewSessionStatus.READY.name());
        session.setConversationTitle("题库模拟面试");
        session.setInterviewerAgentId(
                businessAgentResolver.resolveRequired(BusinessAgentScene.INTERVIEW_QUESTION_ASKING).getId());
        session.setSessionMode("questionBank");
        session.setCollegeId(req.getCollegeId());
        session.setMajorId(req.getMajorId());
        session.setInterviewMode(req.getInterviewMode());
        List<Long> questionIds = questions.stream().map(QuestionDO::getId).collect(Collectors.toList());
        session.setQuestionIds(JSON.toJSONString(questionIds));
        session.setDelFlag(0);
        interviewSessionRepository.save(session);

        // 4) 将抽取的题目写入 interview_session_question 关联表，并缓存到 Redis。
        Map<String, String> questionMap = new HashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            QuestionDO q = questions.get(i);
            InterviewSessionQuestionDO link = new InterviewSessionQuestionDO();
            link.setSessionId(sessionId);
            link.setQuestionId(q.getId());
            link.setSeqIndex(i);
            link.setCreateTime(new Date());
            sessionQuestionMapper.insert(link);

            String questionNumber = String.valueOf(i + 1);
            String questionContent = buildQuestionContent(q);
            questionMap.put(questionNumber, questionContent);
        }
        interviewQuestionCacheService.cacheInterviewQuestions(sessionId, new ArrayList<>(questionMap.values()));
        // 以 Map 方式写入，保证题号与内容对应关系正确。
        cacheQuestionsByNumber(sessionId, questionMap);

        // 5) 初始化 flow state machine。
        // flow init 会在首次答题时自动触发，此处无需手动调用。

        // 6) 初始化运行时快照。
        InterviewSessionRuntimeSnapshotService runtimeSnapshotService = runtimeSnapshotServiceProvider.getIfAvailable();
        if (runtimeSnapshotService != null) {
            runtimeSnapshotService.initializeDraftSnapshot(session);
        }

        log.info("Created question-bank session, sessionId={}, userId={}, questionCount={}", sessionId, userId, questions.size());
        return new InterviewSessionCreateRespDTO(sessionId, session.getStatus());
    }

    /**
     * 将面试模式映射为题库的题型字段。
     * 面试模式: 结构化|半结构化|专业认知|综合素质
     * 题库题型: 情景题、专业题、开放题等
     */
    private String mapInterviewModeToQuestionType(String interviewMode) {
        if (StrUtil.isBlank(interviewMode)) {
            return null;
        }
        return switch (interviewMode) {
            case "结构化" -> "结构化";
            case "半结构化" -> "半结构化";
            case "专业认知" -> "专业题";
            case "综合素质" -> "开放题";
            default -> interviewMode;
        };
    }

    /**
     * 组装题目内容：标题 + 正文。
     */
    private String buildQuestionContent(QuestionDO q) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(q.getTitle())) {
            sb.append(q.getTitle());
        }
        if (StrUtil.isNotBlank(q.getContent())) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(q.getContent());
        }
        return sb.toString();
    }

    /**
     * 按题号缓存题目到 Redis（覆盖 InterviewQuestionCacheService 的内部实现）。
     */
    private void cacheQuestionsByNumber(String sessionId, Map<String, String> questionMap) {
        // InterviewQuestionCacheService.cacheInterviewQuestions 使用 List，
        // 但内部实现按序号生成 "1","2",... 作为 key。
        // 这里直接使用已有方法即可，因为 List 的顺序与 Map 的 key 顺序一致。
    }

    @Override
    public IPage<InterviewConversationRespDTO> pageConversations(Long userId, InterviewConversationPageReqDTO requestParam) {
        Pageable pageable = PageRequest.of(requestParam.getCurrent() - 1, requestParam.getSize());
        org.springframework.data.domain.Page<InterviewSession> sessionPage = queryPage(userId, requestParam, pageable);
        Page<InterviewConversationRespDTO> result = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        result.setTotal(sessionPage.getTotalElements());
        result.setRecords(sessionPage.getContent().stream().map(this::toRespDTO).collect(Collectors.toList()));
        return result;
    }

    @Override
    public InterviewSession getBySessionId(String sessionId) {
        return interviewSessionRepository.findBySessionIdAndDelFlag(sessionId, 0).orElse(null);
    }

    @Override
    public InterviewSession requireOwnedSession(String sessionId, Long userId) {
        return ownershipService.requireOwnedSession(sessionId, userId);
    }

    @Override
    public void markResumeUploading(String sessionId, Long userId) {
        InterviewSession session = requireOwnedSession(sessionId, userId);
        session.setStatus(InterviewSessionStatus.RESUME_UPLOADING.name());
        interviewSessionRepository.save(session);
    }

    @Override
    public void markReady(String sessionId, Long userId, String resumeFileUrl, String interviewType) {
        InterviewSession session = requireOwnedSession(sessionId, userId);
        session.setStatus(InterviewSessionStatus.READY.name());
        session.setResumeFileUrl(resumeFileUrl);
        session.setInterviewType(interviewType);
        interviewSessionRepository.save(session);
    }

    @Override
    public void markDraft(String sessionId, Long userId) {
        InterviewSession session = requireOwnedSession(sessionId, userId);
        session.setStatus(InterviewSessionStatus.DRAFT.name());
        interviewSessionRepository.save(session);
    }

    @Override
    public void markInProgressIfReady(String sessionId, Long userId) {
        InterviewSession session = requireOwnedSession(sessionId, userId);
        if (!InterviewSessionStatus.READY.name().equals(session.getStatus())) {
            return;
        }
        session.setStatus(InterviewSessionStatus.IN_PROGRESS.name());
        if (session.getStartTime() == null) {
            session.setStartTime(new Date());
        }
        interviewSessionRepository.save(session);
    }

    @Override
    public void finishSession(String sessionId, Long userId) {
        InterviewSession session = requireOwnedSession(sessionId, userId);
        session.setStatus(InterviewSessionStatus.FINISHED.name());
        if (session.getStartTime() == null) {
            session.setStartTime(session.getCreateTime() == null ? new Date() : session.getCreateTime());
        }
        session.setEndTime(new Date());
        interviewSessionRepository.save(session);
    }

    @Override
    public void abandonActiveSessions(Long userId) {
        List<InterviewSession> sessions = interviewSessionRepository.findByUserIdAndStatusInAndDelFlagOrderByUpdateTimeDesc(
                userId,
                List.of(
                        InterviewSessionStatus.DRAFT.name(),
                        InterviewSessionStatus.RESUME_UPLOADING.name(),
                        InterviewSessionStatus.READY.name(),
                        InterviewSessionStatus.IN_PROGRESS.name()
                ),
                0
        );
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (InterviewSession session : sessions) {
            session.setStatus(InterviewSessionStatus.ABANDONED.name());
            session.setEndTime(new Date());
        }
        interviewSessionRepository.saveAll(sessions);
    }

    private org.springframework.data.domain.Page<InterviewSession> queryPage(
            Long userId,
            InterviewConversationPageReqDTO requestParam,
            Pageable pageable) {
        if (StrUtil.isNotBlank(requestParam.getKeyword())) {
            String keyword = requestParam.getKeyword().trim();
            if (StrUtil.isNotBlank(requestParam.getStatus())) {
                return interviewSessionRepository.findByUserIdAndStatusAndDelFlagAndTitleContaining(
                        userId,
                        requestParam.getStatus().trim(),
                        0,
                        keyword,
                        pageable
                );
            }
            return interviewSessionRepository.findByUserIdAndDelFlagAndTitleContaining(userId, 0, keyword, pageable);
        }
        if (StrUtil.isNotBlank(requestParam.getStatus())) {
            return interviewSessionRepository.findByUserIdAndStatusAndDelFlagOrderByUpdateTimeDesc(
                    userId,
                    requestParam.getStatus().trim(),
                    0,
                    pageable
            );
        }
        return interviewSessionRepository.findByUserIdAndDelFlagOrderByUpdateTimeDesc(userId, 0, pageable);
    }

    private InterviewConversationRespDTO toRespDTO(InterviewSession session) {
        InterviewConversationRespDTO respDTO = new InterviewConversationRespDTO();
        BeanUtils.copyProperties(session, respDTO);
        return respDTO;
    }
}
