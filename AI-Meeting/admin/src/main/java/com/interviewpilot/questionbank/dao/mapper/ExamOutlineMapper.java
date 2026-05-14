package com.interviewpilot.questionbank.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewpilot.questionbank.dao.entity.ExamOutlineDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试大纲持久层
 */
@Mapper
public interface ExamOutlineMapper extends BaseMapper<ExamOutlineDO> {
}
