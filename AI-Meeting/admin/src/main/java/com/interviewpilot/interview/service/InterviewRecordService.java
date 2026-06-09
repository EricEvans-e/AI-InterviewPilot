package com.interviewpilot.interview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.interviewpilot.interview.dao.entity.InterviewRecordDO;
import com.interviewpilot.interview.api.io.req.InterviewRecordPageReqDTO;
import com.interviewpilot.interview.api.io.req.InterviewRecordSaveReqDTO;
import com.interviewpilot.interview.api.io.resp.InterviewRecordRespDTO;

import java.util.Map;

/**
 * 面试记录服务接口
 */
public interface InterviewRecordService extends IService<InterviewRecordDO> {

    /**
     * 保存面试记录
     * @param sessionId 会话ID
     * @param requestParam 保存请求参数
     */
    void saveInterviewRecord(String sessionId, Long userId, InterviewRecordSaveReqDTO requestParam);

    /**
     * 分页查询用户面试记录
     * @param username 用户名
     * @param requestParam 分页查询参数
     * @return 分页结果
     */
    IPage<InterviewRecordRespDTO> pageInterviewRecords(Long userId, InterviewRecordPageReqDTO requestParam);

    /**
     * 根据会话ID获取面试记录
     * @param sessionId 会话ID
     * @param username 当前登录用户名
     * @return 面试记录
     */
    InterviewRecordRespDTO getBySessionId(String sessionId, Long userId);

    /**
     * 手动生成报告参考答案。该操作可能调用 AI，避免在报告首屏自动执行。
     * @param sessionId 会话ID
     * @param userId 当前登录用户ID
     * @return 更新后的面试报告
     */
    InterviewRecordRespDTO generateReferenceAnswers(String sessionId, Long userId);

    /**
     * 手动生成 AI 面试结论，覆盖报告中的 reviewFeedback。
     * @param sessionId 会话ID
     * @param userId 当前登录用户ID
     * @return 更新后的面试报告
     */
    InterviewRecordRespDTO generateAiReviewFeedback(String sessionId, Long userId);
    
    /**
     * 从Redis保存面试记录
     * @param sessionId 会话ID
     * @param username 当前登录用户名
     */
    void saveInterviewRecordFromRedis(String sessionId, Long userId);
    
    /**
     * 解析面试建议字符串为Map格式
     * @param suggestionsString 面试建议字符串（分号分隔）
     * @return 解析后的建议Map，key为编号，value为建议内容
     */
    Map<String, String> parseInterviewSuggestions(String suggestionsString);

    /**
     * 按学生ID分页查询面试记录
     * @param studentId 学生用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    IPage<InterviewRecordDO> pageByStudent(Long studentId, Integer pageNum, Integer pageSize);

    /**
     * 分页查询所有面试记录（教师使用）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    IPage<InterviewRecordDO> pageAllRecords(Integer pageNum, Integer pageSize);

    /**
     * 根据会话ID获取完整报告（教师使用，不校验用户所有权）
     * @param sessionId 会话ID
     * @return 完整面试报告
     */
    InterviewRecordRespDTO getReportBySessionId(String sessionId);
}
