package com.interviewpilot.questionbank.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目持久层
 */
@Mapper
public interface QuestionMapper extends BaseMapper<QuestionDO> {
}
