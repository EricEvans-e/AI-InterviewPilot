package com.interviewpilot.interview.api.io.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 从题库创建面试会话请求DTO
 */
@Data
public class InterviewFromBankReqDTO {

    /**
     * 目标院校ID（可选）
     */
    private Long collegeId;

    /**
     * 目标专业ID（可选）
     */
    private Long majorId;

    /**
     * 面试类型: 结构化|半结构化|专业认知|综合素质
     */
    @NotBlank(message = "interviewMode cannot be blank")
    private String interviewMode;

    /**
     * 抽题数量，默认5
     */
    private Integer questionCount = 5;

    /**
     * 难度: easy|medium|hard（可选）
     */
    private String difficulty;
}
