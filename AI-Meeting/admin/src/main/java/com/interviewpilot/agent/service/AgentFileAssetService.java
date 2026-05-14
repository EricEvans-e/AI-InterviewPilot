package com.interviewpilot.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.interviewpilot.agent.api.io.resp.AgentFileUploadRespDTO;
import com.interviewpilot.agent.dao.entity.AgentFileAssetDO;
import org.springframework.web.multipart.MultipartFile;

public interface AgentFileAssetService extends IService<AgentFileAssetDO> {

    AgentFileUploadRespDTO uploadAndPersist(String sessionId, String bizType, String username, MultipartFile file);
}
