package com.interviewpilot.interview.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewpilot.interview.dao.entity.InterviewRecordDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

/**
 * 面试记录Mapper
 */
public interface InterviewRecordMapper extends BaseMapper<InterviewRecordDO> {

    @Select("SELECT COUNT(DISTINCT user_id) FROM interview_record WHERE del_flag = 0 AND create_time >= #{todayStart}")
    int countTodayActiveUsers(@Param("todayStart") Date todayStart);

    @Select("SELECT COUNT(*) FROM interview_record WHERE del_flag = 0 AND create_time >= #{weekStart}")
    int countWeekTraining(@Param("weekStart") Date weekStart);

    @Select("SELECT AVG(interview_score) FROM interview_record WHERE del_flag = 0 AND interview_score IS NOT NULL")
    Double avgInterviewScore();
}