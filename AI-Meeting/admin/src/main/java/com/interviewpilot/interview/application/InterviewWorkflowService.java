package com.interviewpilot.interview.application;

import com.interviewpilot.interview.api.io.req.DemeanorEvaluationReqDTO;
import com.interviewpilot.interview.api.io.req.InterviewAnswerReqDTO;
import com.interviewpilot.interview.api.io.req.InterviewQuestionReqDTO;
import com.interviewpilot.interview.api.io.resp.InterviewAnswerRespDTO;
import com.interviewpilot.interview.api.io.resp.InterviewQuestionRespDTO;

/**
 * 面试工作流接口
 * 定义面试核心操作，由 InterviewSessionFacade 调用，实现在 service/impl/ 中
 */
public interface InterviewWorkflowService {

    /** 上传简历，AI 生成面试题 */
    InterviewQuestionRespDTO extractInterviewQuestions(InterviewQuestionReqDTO requestParam);

    /** 提交答案，AI 评分并返回下一题 */
    InterviewAnswerRespDTO answerInterviewQuestion(String sessionId, InterviewAnswerReqDTO requestParam);

    /** 获取下一题（跳过当前题或追问结束后） */
    InterviewAnswerRespDTO getNextQuestion(String sessionId);

    /** 获取当前题（页面刷新恢复用） */
    InterviewAnswerRespDTO getCurrentQuestion(String sessionId);

    /** 神态分析（上传截图，AI 分析仪态） */
    String evaluateDemeanor(DemeanorEvaluationReqDTO requestParam);
}
