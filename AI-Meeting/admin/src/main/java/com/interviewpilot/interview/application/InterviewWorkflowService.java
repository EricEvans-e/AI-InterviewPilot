package com.interviewpilot.interview.application;

import com.interviewpilot.interview.api.io.req.DemeanorEvaluationReqDTO;
import com.interviewpilot.interview.api.io.req.InterviewAnswerReqDTO;
import com.interviewpilot.interview.api.io.req.InterviewQuestionReqDTO;
import com.interviewpilot.interview.api.io.resp.InterviewAnswerRespDTO;
import com.interviewpilot.interview.api.io.resp.InterviewQuestionRespDTO;

public interface InterviewWorkflowService {

    InterviewQuestionRespDTO extractInterviewQuestions(InterviewQuestionReqDTO requestParam);

    InterviewAnswerRespDTO answerInterviewQuestion(String sessionId, InterviewAnswerReqDTO requestParam);

    InterviewAnswerRespDTO getNextQuestion(String sessionId);

    InterviewAnswerRespDTO getCurrentQuestion(String sessionId);

    String evaluateDemeanor(DemeanorEvaluationReqDTO requestParam);
}
