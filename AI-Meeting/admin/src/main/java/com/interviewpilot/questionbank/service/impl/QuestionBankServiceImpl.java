package com.interviewpilot.questionbank.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.questionbank.api.io.req.QuestionPageReqDTO;
import com.interviewpilot.questionbank.api.io.req.QuestionSaveReqDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionRespDTO;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import com.interviewpilot.questionbank.dao.mapper.QuestionMapper;
import com.interviewpilot.questionbank.service.QuestionBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
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

    @Override
    @Transactional
    public Long create(QuestionSaveReqDTO requestParam, Long creatorId) {
        QuestionDO questionDO = new QuestionDO();
        BeanUtils.copyProperties(requestParam, questionDO);
        questionDO.setCreatorId(creatorId);
        questionDO.setIsAiGenerated(false);
        questionDO.setStatus("draft");
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
        LambdaQueryWrapper<QuestionDO> queryWrapper = Wrappers.lambdaQuery(QuestionDO.class)
                .eq(QuestionDO::getDelFlag, 0)
                .eq(QuestionDO::getStatus, "approved");

        if (collegeId != null) {
            queryWrapper.eq(QuestionDO::getCollegeId, collegeId);
        }
        if (majorId != null) {
            queryWrapper.eq(QuestionDO::getMajorId, majorId);
        }
        if (StrUtil.isNotBlank(questionType)) {
            queryWrapper.eq(QuestionDO::getQuestionType, questionType);
        }

        queryWrapper.last("ORDER BY RAND() LIMIT " + count);
        return baseMapper.selectList(queryWrapper);
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
}
