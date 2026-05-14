package com.interviewpilot.interview.api.io.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 面试答题请求 DTO
 * 用户在面试页面提交答案时，前端将数据打包成此格式发送给后端
 *
 * 示例 JSON：
 * {
 *   "questionNumber": "1",
 *   "answerContent": "我使用了并行计算框架...",
 *   "requestId": "6f3e4d6a-ade3-4571-839b-868cee6fa265"
 * }
 */
@Data
public class InterviewAnswerReqDTO {

    /**
     * 题目编号
     * 主题为 "1"、"2"、"3"...，追问为 "1-F1"、"1-F2"（表示第1题的第1/2次追问）
     * 必填，最大32字符
     */
    @NotBlank(message = "questionNumber cannot be blank")
    @Size(max = 32, message = "questionNumber length must be less than or equal to 32")
    private String questionNumber;

    /**
     * 用户的回答内容
     * 支持文字输入或语音转文字后的内容，最大5000字符
     */
    @NotBlank(message = "answerContent cannot be blank")
    @Size(max = 5000, message = "answerContent length must be less than or equal to 5000")
    private String answerContent;

    /**
     * 面试会话ID
     * 通常从 URL 路径中获取（如 /sessions/{sessionId}/answer），此字段可选
     */
    private String sessionId;

    /**
     * 请求幂等ID（UUID）
     * 用于防止重复提交：同样的 requestId 只会处理一次，后续重试直接返回上次结果
     * 前端生成一次 UUID，网络超时重试时带上同一个 ID 即可安全重试
     */
    @Size(max = 64, message = "requestId length must be less than or equal to 64")
    private String requestId;
}
