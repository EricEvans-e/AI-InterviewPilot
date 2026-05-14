package com.interviewpilot.ai.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI配置Mapper接口
 */
@Mapper
public interface AiPropertiesMapper extends BaseMapper<AiPropertiesDO> {
}