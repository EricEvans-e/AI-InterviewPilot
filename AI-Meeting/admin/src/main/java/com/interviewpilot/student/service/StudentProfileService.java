package com.interviewpilot.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.student.api.io.req.StudentProfileSaveReqDTO;
import com.interviewpilot.student.api.io.resp.StudentProfileRespDTO;
import com.interviewpilot.student.dao.entity.StudentProfileDO;
import com.interviewpilot.student.dao.entity.StudentTargetCollegeDO;
import com.interviewpilot.student.dao.entity.StudentTargetMajorDO;
import com.interviewpilot.student.dao.mapper.StudentProfileMapper;
import com.interviewpilot.student.dao.mapper.StudentTargetCollegeMapper;
import com.interviewpilot.student.dao.mapper.StudentTargetMajorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 学生档案服务
 */
@Service
@RequiredArgsConstructor
public class StudentProfileService extends ServiceImpl<StudentProfileMapper, StudentProfileDO> {

    private final StudentTargetCollegeMapper targetCollegeMapper;
    private final StudentTargetMajorMapper targetMajorMapper;

    /**
     * 保存学生档案（upsert 模式）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveProfile(Long userId, StudentProfileSaveReqDTO req) {
        // upsert profile
        StudentProfileDO existing = this.getOne(new LambdaQueryWrapper<StudentProfileDO>()
                .eq(StudentProfileDO::getUserId, userId)
                .eq(StudentProfileDO::getDelFlag, 0));
        if (existing != null) {
            existing.setSchoolName(req.getSchoolName());
            existing.setGrade(req.getGrade());
            existing.setExamCategory(req.getExamCategory());
            existing.setTrainingStage(req.getTrainingStage());
            this.updateById(existing);
        } else {
            StudentProfileDO profile = new StudentProfileDO();
            profile.setUserId(userId);
            BeanUtils.copyProperties(req, profile);
            this.save(profile);
        }

        // 更新目标院校
        targetCollegeMapper.delete(new LambdaQueryWrapper<StudentTargetCollegeDO>()
                .eq(StudentTargetCollegeDO::getUserId, userId));
        if (req.getCollegeIds() != null) {
            for (Long collegeId : req.getCollegeIds()) {
                StudentTargetCollegeDO tc = new StudentTargetCollegeDO();
                tc.setUserId(userId);
                tc.setCollegeId(collegeId);
                targetCollegeMapper.insert(tc);
            }
        }

        // 更新目标专业
        targetMajorMapper.delete(new LambdaQueryWrapper<StudentTargetMajorDO>()
                .eq(StudentTargetMajorDO::getUserId, userId));
        if (req.getMajorIds() != null) {
            for (Long majorId : req.getMajorIds()) {
                StudentTargetMajorDO tm = new StudentTargetMajorDO();
                tm.setUserId(userId);
                tm.setMajorId(majorId);
                targetMajorMapper.insert(tm);
            }
        }
    }

    /**
     * 查询学生档案
     */
    public StudentProfileRespDTO getProfile(Long userId) {
        StudentProfileDO profile = this.getOne(new LambdaQueryWrapper<StudentProfileDO>()
                .eq(StudentProfileDO::getUserId, userId)
                .eq(StudentProfileDO::getDelFlag, 0));

        StudentProfileRespDTO resp = new StudentProfileRespDTO();
        if (profile == null) {
            resp.setUserId(userId);
            resp.setTargetColleges(List.of());
            resp.setTargetMajors(List.of());
            return resp;
        }

        BeanUtils.copyProperties(profile, resp);

        // 查询目标院校
        List<Long> collegeIds = targetCollegeMapper.selectList(
                new LambdaQueryWrapper<StudentTargetCollegeDO>()
                        .eq(StudentTargetCollegeDO::getUserId, userId))
                .stream()
                .map(StudentTargetCollegeDO::getCollegeId)
                .collect(Collectors.toList());
        resp.setTargetColleges(collegeIds);

        // 查询目标专业
        List<Long> majorIds = targetMajorMapper.selectList(
                new LambdaQueryWrapper<StudentTargetMajorDO>()
                        .eq(StudentTargetMajorDO::getUserId, userId))
                .stream()
                .map(StudentTargetMajorDO::getMajorId)
                .collect(Collectors.toList());
        resp.setTargetMajors(majorIds);

        return resp;
    }
}
