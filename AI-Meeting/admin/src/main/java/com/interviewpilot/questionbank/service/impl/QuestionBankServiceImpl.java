package com.interviewpilot.questionbank.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.questionbank.api.io.req.QuestionPageReqDTO;
import com.interviewpilot.questionbank.api.io.req.QuestionSaveReqDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionCoverageRespDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionRespDTO;
import com.interviewpilot.questionbank.dao.entity.MajorDO;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import com.interviewpilot.questionbank.dao.mapper.QuestionMapper;
import com.interviewpilot.questionbank.service.MajorService;
import com.interviewpilot.questionbank.service.QuestionBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题库服务实现
 */
@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl extends ServiceImpl<QuestionMapper, QuestionDO>
        implements QuestionBankService {

    private static final List<String> VALID_STATUSES = Arrays.asList(
            "draft", "pending_review", "approved", "rejected"
    );

    private final MajorService majorService;

    @Override
    @Transactional
    public Long create(QuestionSaveReqDTO requestParam, Long creatorId) {
        QuestionDO questionDO = new QuestionDO();
        BeanUtils.copyProperties(requestParam, questionDO);
        questionDO.setCreatorId(creatorId);

        boolean aiGenerated = Boolean.TRUE.equals(requestParam.getIsAiGenerated());
        questionDO.setIsAiGenerated(aiGenerated);
        if (StrUtil.isNotBlank(requestParam.getStatus())) {
            if (!VALID_STATUSES.contains(requestParam.getStatus())) {
                throw new ClientException("鏃犳晥鐨勭姸鎬佸€硷細" + requestParam.getStatus());
            }
            questionDO.setStatus(requestParam.getStatus());
        } else {
            questionDO.setStatus("draft");
        }

        questionDO.setCreateTime(new Date());
        questionDO.setUpdateTime(new Date());
        questionDO.setDelFlag(0);
        save(questionDO);
        return questionDO.getId();
    }

    @Override
    @Transactional
    public void update(Long id, QuestionSaveReqDTO requestParam) {
        QuestionDO existing = getById(id);
        if (existing == null || existing.getDelFlag() == 1) {
            throw new ClientException("题目不存在");
        }

        LambdaUpdateWrapper<QuestionDO> updateWrapper = Wrappers.lambdaUpdate(QuestionDO.class)
                .eq(QuestionDO::getId, id)
                .set(QuestionDO::getTitle, requestParam.getTitle())
                .set(QuestionDO::getContent, requestParam.getContent())
                .set(QuestionDO::getQuestionType, requestParam.getQuestionType())
                .set(QuestionDO::getCollegeId, requestParam.getCollegeId())
                .set(QuestionDO::getMajorId, requestParam.getMajorId())
                .set(QuestionDO::getAbilityTag, requestParam.getAbilityTag())
                .set(QuestionDO::getDifficulty, requestParam.getDifficulty())
                .set(QuestionDO::getAnswerTimeSeconds, requestParam.getAnswerTimeSeconds())
                .set(QuestionDO::getReferenceAnswer, requestParam.getReferenceAnswer())
                .set(QuestionDO::getScoringRule, requestParam.getScoringRule())
                .set(QuestionDO::getFollowUpRule, requestParam.getFollowUpRule())
                .set(QuestionDO::getFollowUpQuestions, requestParam.getFollowUpQuestions())
                .set(QuestionDO::getSourceRef, requestParam.getSourceRef())
                .set(QuestionDO::getYear, requestParam.getYear())
                .set(QuestionDO::getUpdateTime, new Date());
        update(updateWrapper);
    }

    @Override
    public QuestionRespDTO getDetail(Long id) {
        LambdaQueryWrapper<QuestionDO> queryWrapper = Wrappers.lambdaQuery(QuestionDO.class)
                .eq(QuestionDO::getId, id)
                .eq(QuestionDO::getDelFlag, 0);
        QuestionDO questionDO = baseMapper.selectOne(queryWrapper);
        if (questionDO == null) {
            throw new ClientException("题目不存在");
        }
        QuestionRespDTO result = new QuestionRespDTO();
        BeanUtils.copyProperties(questionDO, result);
        return result;
    }

    @Override
    public PageInfo<QuestionRespDTO> pageByFilter(QuestionPageReqDTO requestParam) {
        Page<QuestionDO> page = new Page<>(requestParam.getPageNum(), requestParam.getPageSize());
        LambdaQueryWrapper<QuestionDO> queryWrapper = Wrappers.lambdaQuery(QuestionDO.class)
                .eq(QuestionDO::getDelFlag, 0);

        if (requestParam.getCollegeId() != null) {
            queryWrapper.eq(QuestionDO::getCollegeId, requestParam.getCollegeId());
        }
        if (requestParam.getMajorId() != null) {
            queryWrapper.eq(QuestionDO::getMajorId, requestParam.getMajorId());
        }
        if (StrUtil.isNotBlank(requestParam.getQuestionType())) {
            queryWrapper.eq(QuestionDO::getQuestionType, requestParam.getQuestionType());
        }
        if (StrUtil.isNotBlank(requestParam.getAbilityTag())) {
            queryWrapper.eq(QuestionDO::getAbilityTag, requestParam.getAbilityTag());
        }
        if (StrUtil.isNotBlank(requestParam.getDifficulty())) {
            queryWrapper.eq(QuestionDO::getDifficulty, requestParam.getDifficulty());
        }
        if (StrUtil.isNotBlank(requestParam.getStatus())) {
            queryWrapper.eq(QuestionDO::getStatus, requestParam.getStatus());
        }

        queryWrapper.orderByDesc(QuestionDO::getCreateTime);

        Page<QuestionDO> questionDOPage = baseMapper.selectPage(page, queryWrapper);
        List<QuestionRespDTO> resultList = questionDOPage.getRecords().stream()
                .map(item -> {
                    QuestionRespDTO respDTO = new QuestionRespDTO();
                    BeanUtils.copyProperties(item, respDTO);
                    return respDTO;
                })
                .collect(Collectors.toList());

        PageInfo<QuestionRespDTO> pageInfo = new PageInfo<>();
        pageInfo.setRecords(resultList);
        pageInfo.setTotal(questionDOPage.getTotal());
        pageInfo.setCurrent(questionDOPage.getCurrent());
        pageInfo.setPages(questionDOPage.getPages());
        pageInfo.setSize(questionDOPage.getSize());
        return pageInfo;
    }

    @Override
    public List<QuestionDO> randomSelect(Long collegeId, Long majorId, String questionType, int count) {
        if (count <= 0) {
            return List.of();
        }

        List<QuestionDO> selected = new ArrayList<>();
        Set<Long> selectedIds = new LinkedHashSet<>();

        appendLayer(selected, selectedIds, exactQuery(collegeId, majorId, questionType), count);
        appendLayer(selected, selectedIds, collegeQuery(collegeId, questionType), count);
        appendLayer(selected, selectedIds, majorCategoryQuery(majorId, questionType), count);
        appendLayer(selected, selectedIds, generalQuery(questionType), count);

        return selected;
    }

    @Override
    public QuestionCoverageRespDTO coverage(Long collegeId, Long majorId, String interviewMode, int requiredCount) {
        String questionType = mapInterviewModeToQuestionType(interviewMode);
        long exactMatchCount = countQuestions(exactQuery(collegeId, majorId, questionType));

        Set<Long> countedIds = new LinkedHashSet<>();
        List<QuestionDO> available = new ArrayList<>();
        appendLayer(available, countedIds, exactQuery(collegeId, majorId, questionType), Integer.MAX_VALUE);
        appendLayer(available, countedIds, collegeQuery(collegeId, questionType), Integer.MAX_VALUE);
        appendLayer(available, countedIds, majorCategoryQuery(majorId, questionType), Integer.MAX_VALUE);
        appendLayer(available, countedIds, generalQuery(questionType), Integer.MAX_VALUE);

        long approvedCount = available.size();
        QuestionCoverageRespDTO response = new QuestionCoverageRespDTO();
        response.setCollegeId(collegeId);
        response.setMajorId(majorId);
        response.setInterviewMode(interviewMode);
        response.setExactMatchCount(exactMatchCount);
        response.setApprovedCount(approvedCount);
        response.setFallbackCount(Math.max(0, approvedCount - exactMatchCount));
        response.setCanStartImmediately(approvedCount > 0);
        int required = requiredCount <= 0 ? 5 : requiredCount;
        response.setMayNeedAiGeneration(approvedCount < required);
        return response;
    }

    @Override
    public List<QuestionDO> selectByAbility(String abilityTag, int count) {
        LambdaQueryWrapper<QuestionDO> queryWrapper = Wrappers.lambdaQuery(QuestionDO.class)
                .eq(QuestionDO::getDelFlag, 0)
                .eq(QuestionDO::getStatus, "approved")
                .eq(QuestionDO::getAbilityTag, abilityTag)
                .last("ORDER BY RAND() LIMIT " + count);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new ClientException("无效的状态值：" + status);
        }

        QuestionDO existing = getById(id);
        if (existing == null || existing.getDelFlag() == 1) {
            throw new ClientException("题目不存在");
        }

        LambdaUpdateWrapper<QuestionDO> updateWrapper = Wrappers.lambdaUpdate(QuestionDO.class)
                .eq(QuestionDO::getId, id)
                .set(QuestionDO::getStatus, status)
                .set(QuestionDO::getUpdateTime, new Date());
        update(updateWrapper);
    }

    private void appendLayer(List<QuestionDO> selected,
                             Set<Long> selectedIds,
                             QueryWrapper<QuestionDO> queryWrapper,
                             int targetCount) {
        if (selected.size() >= targetCount) {
            return;
        }
        int remaining = targetCount == Integer.MAX_VALUE ? Integer.MAX_VALUE : targetCount - selected.size();
        if (!selectedIds.isEmpty()) {
            queryWrapper.notIn("id", selectedIds);
        }
        if (remaining != Integer.MAX_VALUE) {
            queryWrapper.last("ORDER BY RAND() LIMIT " + remaining);
        }
        List<QuestionDO> layer = baseMapper.selectList(queryWrapper);
        if (layer == null || layer.isEmpty()) {
            return;
        }
        for (QuestionDO question : layer) {
            if (question.getId() != null && selectedIds.add(question.getId())) {
                selected.add(question);
            }
            if (selected.size() >= targetCount) {
                return;
            }
        }
    }

    private long countQuestions(Wrapper<QuestionDO> queryWrapper) {
        return baseMapper.selectCount(queryWrapper);
    }

    private QueryWrapper<QuestionDO> baseApprovedQuery(String questionType) {
        QueryWrapper<QuestionDO> queryWrapper = new QueryWrapper<QuestionDO>()
                .eq("del_flag", 0)
                .eq("status", "approved");
        if (StrUtil.isNotBlank(questionType)) {
            queryWrapper.eq("question_type", questionType);
        }
        return queryWrapper;
    }

    private QueryWrapper<QuestionDO> exactQuery(Long collegeId, Long majorId, String questionType) {
        QueryWrapper<QuestionDO> queryWrapper = baseApprovedQuery(questionType);
        if (collegeId != null) {
            queryWrapper.eq("college_id", collegeId);
        } else {
            queryWrapper.isNull("college_id");
        }
        if (majorId != null) {
            queryWrapper.eq("major_id", majorId);
        } else {
            queryWrapper.isNull("major_id");
        }
        return queryWrapper;
    }

    private QueryWrapper<QuestionDO> collegeQuery(Long collegeId, String questionType) {
        QueryWrapper<QuestionDO> queryWrapper = baseApprovedQuery(questionType);
        if (collegeId != null) {
            queryWrapper.eq("college_id", collegeId);
        } else {
            queryWrapper.isNull("college_id");
        }
        return queryWrapper;
    }

    private QueryWrapper<QuestionDO> majorCategoryQuery(Long majorId, String questionType) {
        QueryWrapper<QuestionDO> queryWrapper = baseApprovedQuery(questionType);
        List<Long> sameCategoryMajorIds = sameCategoryMajorIds(majorId);
        if (sameCategoryMajorIds.isEmpty()) {
            queryWrapper.apply("1 = 0");
            return queryWrapper;
        }
        return queryWrapper.in("major_id", sameCategoryMajorIds);
    }

    private QueryWrapper<QuestionDO> generalQuery(String questionType) {
        return baseApprovedQuery(questionType)
                .isNull("college_id")
                .isNull("major_id");
    }

    private List<Long> sameCategoryMajorIds(Long majorId) {
        if (majorId == null) {
            return List.of();
        }
        MajorDO currentMajor = majorService.getById(majorId);
        if (currentMajor == null || StrUtil.isBlank(currentMajor.getCategory())) {
            return List.of();
        }
        return majorService.list().stream()
                .filter(major -> major != null && major.getDelFlag() != null && major.getDelFlag() == 0)
                .filter(major -> StrUtil.equals(currentMajor.getCategory(), major.getCategory()))
                .map(MajorDO::getId)
                .filter(id -> id != null && !id.equals(majorId))
                .toList();
    }

    private String mapInterviewModeToQuestionType(String interviewMode) {
        if (StrUtil.isBlank(interviewMode)) {
            return interviewMode;
        }
        return switch (interviewMode) {
            case "结构化" -> "结构化";
            case "半结构化" -> "半结构化";
            case "专业认知" -> "专业题";
            case "综合素质" -> "开放题";
            default -> interviewMode;
        };
    }
}
