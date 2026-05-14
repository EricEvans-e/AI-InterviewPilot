package com.interviewpilot.interview.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.interviewpilot.agent.api.io.resp.AgentMessageHistoryRespDTO;
import com.interviewpilot.common.convention.annotation.CurrentUser;
import com.interviewpilot.common.convention.context.UserContext;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.interview.api.io.req.InterviewAnswerReqDTO;
import com.interviewpilot.interview.api.io.req.InterviewConversationPageReqDTO;
import com.interviewpilot.interview.api.io.req.InterviewFromBankReqDTO;
import com.interviewpilot.interview.api.io.resp.InterviewAnswerRespDTO;
import com.interviewpilot.interview.api.io.resp.InterviewConversationRespDTO;
import com.interviewpilot.interview.api.io.resp.InterviewQuestionRespDTO;
import com.interviewpilot.interview.api.io.resp.InterviewSessionCreateRespDTO;
import com.interviewpilot.interview.api.io.resp.InterviewSessionRestoreRespDTO;
import com.interviewpilot.interview.api.io.resp.RadarChartDTO;
import com.interviewpilot.interview.flow.session.InterviewSessionFacade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 面试会话控制器
 * 面试流程的核心入口，涵盖：创建会话、上传简历出题、答题、评分、追问、神态分析、获取报告等全流程
 */
@Validated
@RestController
@RequestMapping("/api/ip/v1/interview")
@RequiredArgsConstructor
public class InterviewSessionController {

    private final InterviewSessionFacade interviewSessionFacade;

    /**
     * 创建面试会话（简历模式）
     * 创建一个空会话，后续需上传简历提取面试题
     */
    @PostMapping("/sessions")
    public Result<InterviewSessionCreateRespDTO> createSession(@CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.createSession(currentUser.getUserId()));
    }

    /**
     * 从题库创建面试会话 (题库模式)
     */
    @PostMapping("/sessions/from-bank")
    public Result<InterviewSessionCreateRespDTO> createSessionFromBank(
            @RequestBody @Valid InterviewFromBankReqDTO req,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.createFromBank(currentUser.getUserId(), req));
    }

    /**
     * 分页查询面试会话列表
     */
    @GetMapping("/conversations")
    public Result<IPage<InterviewConversationRespDTO>> pageConversations(
            InterviewConversationPageReqDTO requestParam,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.pageConversations(currentUser.getUserId(), requestParam));
    }

    /**
     * 查询面试会话的完整对话历史（一问一答的完整记录）
     */
    @GetMapping("/conversations/{sessionId}/messages")
    public Result<List<AgentMessageHistoryRespDTO>> getConversationHistory(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.getConversationHistory(sessionId, currentUser.getUserId()));
    }

    /**
     * 分页查询历史消息
     */
    @GetMapping("/messages/history")
    public Result<IPage<AgentMessageHistoryRespDTO>> pageHistoryMessages(
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @CurrentUser UserContext currentUser) {
        return Results.success(
                interviewSessionFacade.pageHistoryMessages(sessionId, current, size, currentUser.getUserId()));
    }

    /**
     * 结束面试会话（标记为已完成，触发最终报告生成）
     */
    @PutMapping("/sessions/{sessionId}/finish")
    public Result<Void> finishSession(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        interviewSessionFacade.finishSession(sessionId, currentUser.getUserId());
        return Results.success();
    }

    /**
     * 结束对话（通用，非面试专用）
     */
    @PutMapping("/conversations/{sessionId}/end")
    public Result<Void> endConversation(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        interviewSessionFacade.endConversation(sessionId, currentUser.getUserId());
        return Results.success();
    }

    /**
     * 上传简历并提取面试题
     * 解析 PDF 简历内容，AI 生成针对性面试题
     */
    @PostMapping("/sessions/{sessionId}/interview-questions")
    public Result<InterviewQuestionRespDTO> extractInterviewQuestions(
            @PathVariable String sessionId,
            @RequestParam("resumePdf") MultipartFile resumePdf,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.extractInterviewQuestions(
                sessionId, resumePdf, currentUser.getUserId(), currentUser.getUsername()));
    }

    /**
     * 提交面试答案（表单参数方式）
     * 提交后 AI 自动评分、决定是否追问、返回下一题
     */
    @PostMapping("/sessions/{sessionId}/interview/answer")
    public Result<InterviewAnswerRespDTO> answerInterviewQuestion(
            @PathVariable String sessionId,
            @NotBlank(message = "questionNumber cannot be blank")
            @Size(max = 32, message = "questionNumber length must be less than or equal to 32")
            @RequestParam("questionNumber") String questionNumber,
            @NotBlank(message = "answerContent cannot be blank")
            @Size(max = 5000, message = "answerContent length must be less than or equal to 5000")
            @RequestParam("answerContent") String answerContent,
            @RequestParam(value = "requestId", required = false) String requestId,
            @CurrentUser UserContext currentUser) {
        InterviewAnswerReqDTO requestParam = new InterviewAnswerReqDTO();
        requestParam.setQuestionNumber(questionNumber);
        requestParam.setAnswerContent(answerContent);
        requestParam.setRequestId(requestId);
        return Results.success(
                interviewSessionFacade.answerInterviewQuestion(sessionId, requestParam, currentUser.getUserId()));
    }

    /**
     * 提交面试答案（JSON 方式，功能与上面相同）
     */
    @PostMapping(value = "/sessions/{sessionId}/interview/answer-json", consumes = "application/json")
    public Result<InterviewAnswerRespDTO> answerInterviewQuestionJson(
            @PathVariable String sessionId,
            @Valid @RequestBody InterviewAnswerReqDTO requestParam,
            @CurrentUser UserContext currentUser) {
        return Results.success(
                interviewSessionFacade.answerInterviewQuestion(sessionId, requestParam, currentUser.getUserId()));
    }

    /**
     * 获取下一题（跳过当前题或追问结束后调用）
     */
    @GetMapping("/sessions/{sessionId}/next-question")
    public Result<InterviewAnswerRespDTO> getNextQuestion(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.getNextQuestion(sessionId, currentUser.getUserId()));
    }

    /**
     * 获取当前题目（页面刷新或恢复会话时调用）
     */
    @GetMapping("/sessions/{sessionId}/current-question")
    public Result<InterviewAnswerRespDTO> getCurrentQuestion(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.getCurrentQuestion(sessionId, currentUser.getUserId()));
    }

    /**
     * 恢复面试会话（断线重连或刷新页面时，恢复到上次答题进度）
     */
    @GetMapping("/sessions/{sessionId}/restore")
    public Result<InterviewSessionRestoreRespDTO> restoreInterviewSession(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.restoreInterviewSession(sessionId, currentUser.getUserId()));
    }

    /**
     * 获取面试会话的所有题目（题目编号→题目内容的映射）
     */
    @GetMapping("/sessions/{sessionId}/interview/questions")
    public Result<Map<String, String>> getSessionInterviewQuestions(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.getSessionInterviewQuestions(sessionId, currentUser.getUserId()));
    }

    /**
     * 获取面试总分（所有已答题目的累计得分）
     */
    @GetMapping("/sessions/{sessionId}/interview/score")
    public Result<Integer> getSessionTotalScore(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.getSessionTotalScore(sessionId, currentUser.getUserId()));
    }

    /**
     * 获取简历改进建议（AI 根据简历内容生成的优化建议）
     */
    @GetMapping("/sessions/{sessionId}/interview/suggestions")
    public Result<Map<String, String>> getSessionInterviewSuggestions(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(
                interviewSessionFacade.getSessionInterviewSuggestions(sessionId, currentUser.getUserId()));
    }

    /**
     * 获取简历评分（AI 对简历质量的打分）
     */
    @GetMapping("/sessions/{sessionId}/resume/score")
    public Result<Integer> getSessionResumeScore(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.getSessionResumeScore(sessionId, currentUser.getUserId()));
    }

    /**
     * 获取雷达图数据（多维度能力评估：沟通、逻辑、专业、抗压、仪表）
     */
    @GetMapping("/sessions/{sessionId}/radar-chart")
    public Result<RadarChartDTO> getRadarChartData(
            @PathVariable String sessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.getRadarChartData(sessionId, currentUser.getUserId()));
    }

    /**
     * 提交神态分析（上传面试截图，AI 分析坐姿、表情、着装等）
     */
    @PostMapping("/sessions/{sessionId}/demeanor-evaluation")
    public Result<String> evaluateDemeanor(
            @PathVariable String sessionId,
            @RequestPart("userPhoto") MultipartFile userPhoto,
            @RequestParam(value = "sessionId", required = false) String requestSessionId,
            @CurrentUser UserContext currentUser) {
        return Results.success(interviewSessionFacade.evaluateDemeanor(
                sessionId,
                userPhoto,
                requestSessionId,
                currentUser.getUserId(),
                currentUser.getUsername()));
    }
}
