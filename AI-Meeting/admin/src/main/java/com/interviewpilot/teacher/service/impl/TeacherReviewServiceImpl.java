package com.interviewpilot.teacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.interview.dao.entity.InterviewSession;
import com.interviewpilot.interview.service.InterviewSessionService;
import com.interviewpilot.teacher.api.io.req.TeacherReviewSaveReqDTO;
import com.interviewpilot.teacher.dao.entity.TeacherReviewDO;
import com.interviewpilot.teacher.dao.mapper.TeacherReviewMapper;
import com.interviewpilot.teacher.service.TeacherReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 教师点评服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherReviewServiceImpl extends ServiceImpl<TeacherReviewMapper, TeacherReviewDO>
        implements TeacherReviewService {

    private final InterviewSessionService interviewSessionService;

    @Override
    @Transactional
    public Long createReview(Long teacherId, String sessionId, TeacherReviewSaveReqDTO req) {
        if (teacherId == null || teacherId <= 0) {
            throw new ClientException("教师ID无效");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new ClientException("会话ID不能为空");
        }

        // Resolve studentId from the interview session
        InterviewSession session = interviewSessionService.getBySessionId(sessionId);
        if (session == null) {
            throw new ClientException("面试会话不存在");
        }
        Long studentId = session.getUserId();

        TeacherReviewDO review = new TeacherReviewDO();
        review.setSessionId(sessionId);
        review.setTeacherId(teacherId);
        review.setStudentId(studentId);
        review.setContent(req.getContent());
        review.setAdjustedScore(req.getAdjustedScore());
        review.setIsExcellentSample(req.getIsExcellentSample() != null ? req.getIsExcellentSample() : false);
        review.setIsModelMisjudge(req.getIsModelMisjudge() != null ? req.getIsModelMisjudge() : false);
        review.setCreateTime(new Date());
        review.setUpdateTime(new Date());
        review.setDelFlag(0);

        save(review);
        log.info("Created teacher review, teacherId={}, sessionId={}, studentId={}, reviewId={}",
                teacherId, sessionId, studentId, review.getId());
        return review.getId();
    }

    @Override
    public List<TeacherReviewDO> getReviewsBySession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ClientException("会话ID不能为空");
        }
        LambdaQueryWrapper<TeacherReviewDO> queryWrapper = Wrappers.lambdaQuery(TeacherReviewDO.class)
                .eq(TeacherReviewDO::getSessionId, sessionId)
                .eq(TeacherReviewDO::getDelFlag, 0)
                .orderByDesc(TeacherReviewDO::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public IPage<TeacherReviewDO> getReviewsByStudent(Long studentId, Integer pageNum, Integer pageSize) {
        if (studentId == null || studentId <= 0) {
            throw new ClientException("学生ID无效");
        }
        Page<TeacherReviewDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TeacherReviewDO> queryWrapper = Wrappers.lambdaQuery(TeacherReviewDO.class)
                .eq(TeacherReviewDO::getStudentId, studentId)
                .eq(TeacherReviewDO::getDelFlag, 0)
                .orderByDesc(TeacherReviewDO::getCreateTime);
        return page(page, queryWrapper);
    }

    @Override
    public IPage<TeacherReviewDO> getStudentReportList(Long teacherId, Integer pageNum, Integer pageSize) {
        if (teacherId == null || teacherId <= 0) {
            throw new ClientException("教师ID无效");
        }
        Page<TeacherReviewDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TeacherReviewDO> queryWrapper = Wrappers.lambdaQuery(TeacherReviewDO.class)
                .eq(TeacherReviewDO::getTeacherId, teacherId)
                .eq(TeacherReviewDO::getDelFlag, 0)
                .orderByDesc(TeacherReviewDO::getCreateTime);
        return page(page, queryWrapper);
    }
}
