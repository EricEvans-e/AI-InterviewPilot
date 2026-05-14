package com.interviewpilot.agent.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewpilot.agent.dao.entity.AgentFileAssetDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件上传记录 Mapper
 */
@Mapper
public interface AgentFileAssetMapper extends BaseMapper<AgentFileAssetDO> {
}

