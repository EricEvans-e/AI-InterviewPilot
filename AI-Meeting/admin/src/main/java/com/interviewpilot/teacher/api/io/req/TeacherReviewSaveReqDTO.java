package com.interviewpilot.teacher.api.io.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 教师点评保存请求参数
 */
@Data
public class TeacherReviewSaveReqDTO {

    /**
     * 点评内容
     */
    @NotBlank(message = "点评内容不能为空")
    @Size(max = 5000, message = "点评内容长度不能超过5000个字符")
    private String content;

    /**
     * 调整后分数（可选）
     */
    private Integer adjustedScore;

    /**
     * 是否标记为优秀样例
     */
    private Boolean isExcellentSample;

    /**
     * 是否标记为模型误判
     */
    private Boolean isModelMisjudge;
}
