package com.interviewpilot.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.agent.api.io.resp.AgentFileUploadRespDTO;
import com.interviewpilot.agent.application.AgentResolver;
import com.interviewpilot.agent.application.BusinessAgentResolver;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.agent.dao.entity.AgentFileAssetDO;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.agent.dao.mapper.AgentFileAssetMapper;
import com.interviewpilot.agent.service.AgentFileAssetService;
import com.interviewpilot.common.config.storage.ApplicationStorageProperties;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.common.enums.AgentErrorCodeEnum;
import com.interviewpilot.toolkit.iflytek.XunfeiWorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentFileAssetServiceImpl extends ServiceImpl<AgentFileAssetMapper, AgentFileAssetDO>
        implements AgentFileAssetService {

    private static final String DEFAULT_BIZ_TYPE = "general";
    private static final String SOURCE_PLATFORM_MIMO_LOCAL = "mimo-local";
    private static final String SOURCE_PLATFORM_XUNFEI = "xingchen";

    private final ObjectProvider<XunfeiWorkflowClient> xunfeiWorkflowClientProvider;
    private final AgentResolver agentResolver;
    private final BusinessAgentResolver businessAgentResolver;
    private final ApplicationStorageProperties storageProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentFileUploadRespDTO uploadAndPersist(
            String sessionId,
            String bizType,
            String username,
            MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ClientException("uploaded file cannot be empty", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
        }

        AgentPropertiesDO agentProperties = resolveAgentProperties(sessionId);
        String originalFileName = normalizeFileName(file.getOriginalFilename());
        String sourcePlatform;
        String fileUrl;
        if (SOURCE_PLATFORM_XUNFEI.equalsIgnoreCase(agentProperties.getAiProvider())) {
            sourcePlatform = SOURCE_PLATFORM_XUNFEI;
            fileUrl = uploadToLegacyXunfei(file, agentProperties);
        } else {
            sourcePlatform = SOURCE_PLATFORM_MIMO_LOCAL;
            fileUrl = saveLocalAgentFile(file, originalFileName);
        }

        Date now = new Date();
        AgentFileAssetDO fileAssetDO = new AgentFileAssetDO();
        fileAssetDO.setAgentId(agentProperties.getId());
        fileAssetDO.setSessionId(StrUtil.isBlank(sessionId) ? null : sessionId.trim());
        fileAssetDO.setUserName(StrUtil.blankToDefault(username, "unknown"));
        fileAssetDO.setBizType(StrUtil.blankToDefault(bizType, DEFAULT_BIZ_TYPE));
        fileAssetDO.setSourcePlatform(sourcePlatform);
        fileAssetDO.setFileName(originalFileName);
        fileAssetDO.setFileExt(extractFileExt(originalFileName));
        fileAssetDO.setContentType(file.getContentType());
        fileAssetDO.setFileSize(file.getSize());
        fileAssetDO.setFileUrl(fileUrl);
        fileAssetDO.setUploadStatus(1);
        fileAssetDO.setCreateTime(now);
        fileAssetDO.setUpdateTime(now);
        fileAssetDO.setDelFlag(0);

        boolean saved = save(fileAssetDO);
        if (!saved) {
            throw new ClientException("persist uploaded file url failed", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
        }

        AgentFileUploadRespDTO respDTO = new AgentFileUploadRespDTO();
        respDTO.setId(fileAssetDO.getId());
        respDTO.setSessionId(fileAssetDO.getSessionId());
        respDTO.setBizType(fileAssetDO.getBizType());
        respDTO.setFileName(fileAssetDO.getFileName());
        respDTO.setFileSize(fileAssetDO.getFileSize());
        respDTO.setContentType(fileAssetDO.getContentType());
        respDTO.setFileUrl(fileAssetDO.getFileUrl());
        respDTO.setCreateTime(fileAssetDO.getCreateTime());
        return respDTO;
    }

    private String uploadToLegacyXunfei(MultipartFile file, AgentPropertiesDO agentProperties) {
        if (StrUtil.isBlank(agentProperties.getApiKey()) || StrUtil.isBlank(agentProperties.getApiSecret())) {
            throw new ClientException("legacy xunfei agent api credentials are missing", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
        }

        try {
            return legacyXunfeiWorkflowClient().uploadFile(file, agentProperties.getApiKey(), agentProperties.getApiSecret());
        } catch (Exception ex) {
            log.error("File upload to legacy xunfei workflow failed, agentId={}, fileName={}",
                    agentProperties.getId(), file.getOriginalFilename(), ex);
            throw new ClientException(
                    "upload to legacy xunfei workflow failed: " + ex.getMessage(),
                    ex,
                    AgentErrorCodeEnum.AGENT_SAVE_ERROR
            );
        }
    }

    private XunfeiWorkflowClient legacyXunfeiWorkflowClient() {
        XunfeiWorkflowClient client = xunfeiWorkflowClientProvider.getIfAvailable();
        if (client == null) {
            throw new ClientException("legacy xingchen provider requires LEGACY_XUNFEI_ENABLED=true",
                    AgentErrorCodeEnum.AGENT_SAVE_ERROR);
        }
        return client;
    }

    private String saveLocalAgentFile(MultipartFile file, String originalFileName) {
        try {
            Path storageDir = storageProperties.getAgentFilePath();
            Files.createDirectories(storageDir);
            String extension = extractFileExt(originalFileName);
            String storedFileName = UUID.randomUUID()
                    + (StrUtil.isBlank(extension) ? "" : "." + extension);
            Path target = storageDir.resolve(storedFileName).normalize();
            if (!target.startsWith(storageDir)) {
                throw new ClientException("invalid file path", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/agent-files/" + storedFileName;
        } catch (ClientException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Local agent file save failed, fileName={}", originalFileName, ex);
            throw new ClientException(
                    "save local agent file failed: " + ex.getMessage(),
                    ex,
                    AgentErrorCodeEnum.AGENT_SAVE_ERROR
            );
        }
    }

    private AgentPropertiesDO resolveAgentProperties(String sessionId) {
        if (StrUtil.isNotBlank(sessionId)) {
            AgentPropertiesDO boundAgent = agentResolver.resolveAgent(sessionId, null);
            if (boundAgent != null) {
                return boundAgent;
            }
        }
        return businessAgentResolver.resolveRequired(BusinessAgentScene.GENERAL_AGENT_CHAT);
    }

    private String normalizeFileName(String originalFileName) {
        if (StrUtil.isBlank(originalFileName)) {
            return "unknown_" + System.currentTimeMillis();
        }
        return originalFileName.trim();
    }

    private String extractFileExt(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return null;
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(idx + 1).toLowerCase();
    }
}
