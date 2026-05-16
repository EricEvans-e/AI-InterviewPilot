package com.interviewpilot.user.api.io.resp;

import lombok.Data;

/**
 * 管理后台统计数据
 */
@Data
public class AdminStatsRespDTO {
    private Integer totalUsers;
    private Integer todayActive;
    private Integer weekTrainingCount;
    private Double avgScore;
}
