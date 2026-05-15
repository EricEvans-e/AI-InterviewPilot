package com.interviewpilot.teacher.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.interviewpilot.teacher.api.io.req.TeacherReviewSaveReqDTO;
import com.interviewpilot.teacher.api.io.resp.StudentReportRespDTO;
import com.interviewpilot.teacher.dao.entity.TeacherReviewDO;

import java.util.List;

/**
 * 教师点评服务接口
 */
public interface TeacherReviewService extends IService<TeacherReviewDO> {

    /**
     * 创建教师点评
     * @param teacherId 教师用户ID
     * @param sessionId 面试会话ID
     * @param req 点评请求参数
     * @return 点评记录ID
     */
    Long createReview(Long teacherId, String sessionId, TeacherReviewSaveReqDTO req);

    /**
     * 获取指定会话的所有点评
     * @param sessionId 面试会话ID
     * @return 点评列表
     */
    List<TeacherReviewDO> getReviewsBySession(String sessionId);

    /**
     * 分页获取指定学生的点评
     * @param studentId 学生用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    IPage<TeacherReviewDO> getReviewsByStudent(Long studentId, Integer pageNum, Integer pageSize);

    /**
     * 教师查看已授权的学生报告列表
     * @param teacherId 教师用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    IPage<StudentReportRespDTO> getStudentReportList(Long teacherId, Integer pageNum, Integer pageSize);
}
