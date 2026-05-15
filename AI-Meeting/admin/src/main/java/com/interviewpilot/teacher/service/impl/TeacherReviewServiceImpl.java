package com.interviewpilot.teacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.interview.dao.entity.InterviewRecordDO;
import com.interviewpilot.interview.dao.entity.InterviewSession;
import com.interviewpilot.interview.service.InterviewRecordService;
import com.interviewpilot.interview.service.InterviewSessionService;
import com.interviewpilot.questionbank.dao.entity.CollegeDO;
import com.interviewpilot.questionbank.dao.entity.MajorDO;
import com.interviewpilot.questionbank.service.CollegeService;
import com.interviewpilot.questionbank.service.MajorService;
import com.interviewpilot.teacher.api.io.req.TeacherReviewSaveReqDTO;
import com.interviewpilot.teacher.api.io.resp.StudentReportRespDTO;
import com.interviewpilot.teacher.dao.entity.TeacherReviewDO;
import com.interviewpilot.teacher.dao.mapper.TeacherReviewMapper;
import com.interviewpilot.teacher.service.TeacherReviewService;
import com.interviewpilot.user.dao.entity.UserDO;
import com.interviewpilot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 教师点评服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherReviewServiceImpl extends ServiceImpl<TeacherReviewMapper, TeacherReviewDO>
        implements TeacherReviewService {

    private final InterviewSessionService interviewSessionService;
    private final UserService userService;
    private final CollegeService collegeService;
    private final MajorService majorService;
    private final InterviewRecordService interviewRecordService;

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
    public IPage<StudentReportRespDTO> getStudentReportList(Long teacherId, Integer pageNum, Integer pageSize) {
        if (teacherId == null || teacherId <= 0) {
            throw new ClientException("教师ID无效");
        }
        Page<TeacherReviewDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TeacherReviewDO> queryWrapper = Wrappers.lambdaQuery(TeacherReviewDO.class)
                .eq(TeacherReviewDO::getTeacherId, teacherId)
                .eq(TeacherReviewDO::getDelFlag, 0)
                .orderByDesc(TeacherReviewDO::getCreateTime);
        IPage<TeacherReviewDO> reviewPage = page(page, queryWrapper);

        List<StudentReportRespDTO> records = reviewPage.getRecords().stream()
                .map(this::toStudentReportDTO)
                .collect(Collectors.toList());

        Page<StudentReportRespDTO> result = new Page<>(pageNum, pageSize, reviewPage.getTotal());
        result.setRecords(records);
        return result;
    }

    private StudentReportRespDTO toStudentReportDTO(TeacherReviewDO review) {
        StudentReportRespDTO dto = new StudentReportRespDTO();
        dto.setId(review.getId());
        dto.setSessionId(review.getSessionId());
        dto.setStudentId(review.getStudentId());
        dto.setContent(review.getContent());
        dto.setAdjustedScore(review.getAdjustedScore());
        dto.setIsExcellentSample(review.getIsExcellentSample());
        dto.setIsModelMisjudge(review.getIsModelMisjudge());
        dto.setCreateTime(review.getCreateTime());

        // Student name
        if (review.getStudentId() != null) {
            try {
                UserDO user = userService.getById(review.getStudentId());
                if (user != null) {
                    dto.setStudentName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                }
            } catch (Exception e) {
                log.warn("Failed to load user for studentId={}", review.getStudentId(), e);
            }
        }

        // Session title, collegeId, majorId from MongoDB
        if (review.getSessionId() != null) {
            try {
                InterviewSession session = interviewSessionService.getBySessionId(review.getSessionId());
                if (session != null) {
                    dto.setSessionTitle(session.getConversationTitle());
                    if (session.getCollegeId() != null) {
                        CollegeDO college = collegeService.getById(session.getCollegeId());
                        if (college != null) {
                            dto.setCollegeName(college.getName());
                        }
                    }
                    if (session.getMajorId() != null) {
                        MajorDO major = majorService.getById(session.getMajorId());
                        if (major != null) {
                            dto.setMajorName(major.getName());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to load session for sessionId={}", review.getSessionId(), e);
            }
        }

        // Interview score and status from interview_record
        if (review.getSessionId() != null) {
            try {
                LambdaQueryWrapper<InterviewRecordDO> recordWrapper = Wrappers.lambdaQuery(InterviewRecordDO.class)
                        .eq(InterviewRecordDO::getSessionId, review.getSessionId())
                        .eq(InterviewRecordDO::getDelFlag, 0);
                InterviewRecordDO record = interviewRecordService.getOne(recordWrapper);
                if (record != null) {
                    dto.setOverallScore(record.getInterviewScore());
                    dto.setStatus(record.getInterviewStatus());
                }
            } catch (Exception e) {
                log.warn("Failed to load interview record for sessionId={}", review.getSessionId(), e);
            }
        }

        return dto;
    }
}
