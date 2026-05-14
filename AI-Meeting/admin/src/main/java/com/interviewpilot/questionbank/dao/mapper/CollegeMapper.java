package com.interviewpilot.questionbank.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewpilot.questionbank.dao.entity.CollegeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 院校持久层
 */
@Mapper
public interface CollegeMapper extends BaseMapper<CollegeDO> {
}
