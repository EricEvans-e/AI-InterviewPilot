package com.interviewpilot.student.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewpilot.student.dao.entity.StudentProfileDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生档案 Mapper
 */
@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfileDO> {
}
