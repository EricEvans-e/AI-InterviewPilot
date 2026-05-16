package com.interviewpilot.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.agent.service.AgentPropertiesService;
import com.interviewpilot.agent.service.AgentTagService;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.agent.dao.entity.AgentPropertiesDO;
import com.interviewpilot.agent.dao.entity.AgentTag;
import com.interviewpilot.agent.dao.mapper.AgentPropertiesMapper;
import com.interviewpilot.agent.api.io.req.AgentPropertiesReqDTO;
import com.interviewpilot.agent.api.io.resp.AgentPropertiesRespDTO;
import com.interviewpilot.agent.api.io.resp.SceneBindingRespDTO;
import com.interviewpilot.agent.application.BusinessAgentScene;
import com.interviewpilot.common.enums.AgentTagType;
import com.interviewpilot.toolkit.xunfei.AgentPropertiesLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @description 针对表【agent_properties】的数据库操作Service实现
* @createDate 2025-05-27 10:08:58
*/
@Service
public class AgentPropertiesServiceImpl extends ServiceImpl<AgentPropertiesMapper, AgentPropertiesDO>
    implements AgentPropertiesService {

    private final AgentTagService agentTagService;
    private final AgentPropertiesLoader agentPropertiesLoader;

    public AgentPropertiesServiceImpl(AgentTagService agentTagService,
                                      @Lazy AgentPropertiesLoader agentPropertiesLoader) {
        this.agentTagService = agentTagService;
        this.agentPropertiesLoader = agentPropertiesLoader;
    }

    @Override
    @Transactional
    public void create(AgentPropertiesReqDTO requestParam) {
        AgentPropertiesDO agentPropertiesDO = new AgentPropertiesDO();
        BeanUtils.copyProperties(requestParam, agentPropertiesDO);
        agentPropertiesDO.setCreateTime(new Date());
        agentPropertiesDO.setUpdateTime(new Date());
        agentPropertiesDO.setDelFlag(0);
        save(agentPropertiesDO);
        
        // 处理标签数据
        if (requestParam.getTagCodes() != null && !requestParam.getTagCodes().isEmpty()) {
            createAgentTags(agentPropertiesDO.getId(), requestParam.getTagCodes());
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LambdaUpdateWrapper<AgentPropertiesDO> updateWrapper = Wrappers.lambdaUpdate(AgentPropertiesDO.class)
                .eq(AgentPropertiesDO::getId, id)
                .set(AgentPropertiesDO::getDelFlag, 1)
                .set(AgentPropertiesDO::getUpdateTime, new Date());
        baseMapper.update(null, updateWrapper);
        
        // 删除相关标签数据
        LambdaUpdateWrapper<AgentTag> tagUpdateWrapper = Wrappers.lambdaUpdate(AgentTag.class)
                .eq(AgentTag::getAgentId, id)
                .set(AgentTag::getDelFlag, 1);
        agentTagService.update(tagUpdateWrapper);
    }

    @Override
    @Transactional
    public void update(AgentPropertiesReqDTO requestParam) {
        LambdaUpdateWrapper<AgentPropertiesDO> updateWrapper = Wrappers.lambdaUpdate(AgentPropertiesDO.class)
                .eq(AgentPropertiesDO::getId, requestParam.getId())
                .set(AgentPropertiesDO::getAgentName, requestParam.getAgentName())
                .set(AgentPropertiesDO::getApiSecret, requestParam.getApiSecret())
                .set(AgentPropertiesDO::getApiKey, requestParam.getApiKey())
                .set(AgentPropertiesDO::getApiFlowId, requestParam.getApiFlowId())
                .set(AgentPropertiesDO::getUpdateTime, new Date());
        update(updateWrapper);
        
        // 更新标签数据
        if (requestParam.getTagCodes() != null) {
            updateAgentTags(requestParam.getId(), requestParam.getTagCodes());
        }
    }

    @Override
    public AgentPropertiesRespDTO getByName(String name) {
        LambdaQueryWrapper<AgentPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AgentPropertiesDO.class)
                .eq(AgentPropertiesDO::getAgentName, name)
                .eq(AgentPropertiesDO::getDelFlag, 0);
        AgentPropertiesDO agentPropertiesDO = baseMapper.selectOne(queryWrapper);
        AgentPropertiesRespDTO result = new AgentPropertiesRespDTO();
        if (agentPropertiesDO != null) {
            BeanUtils.copyProperties(agentPropertiesDO, result);
            // 填充标签数据
            result.setTags(getAgentTags(agentPropertiesDO.getId()));
        }
        return result;
    }

    @Override
    public PageInfo<AgentPropertiesRespDTO> getByPage(AgentPropertiesReqDTO requestParam) {
        Page<AgentPropertiesDO> page = new Page<>(requestParam.getPageNum(), requestParam.getPageSize());
        LambdaQueryWrapper<AgentPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AgentPropertiesDO.class)
                .eq(AgentPropertiesDO::getDelFlag, 0)
                .orderByDesc(AgentPropertiesDO::getCreateTime);
        Page<AgentPropertiesDO> agentPropertiesDOPage = baseMapper.selectPage(page, queryWrapper);
        List<AgentPropertiesRespDTO> resultList = agentPropertiesDOPage.getRecords().stream()
                .map(item -> {
                    AgentPropertiesRespDTO respDTO = new AgentPropertiesRespDTO();
                    BeanUtils.copyProperties(item, respDTO);
                    // 填充标签数据
                    respDTO.setTags(getAgentTags(item.getId()));
                    return respDTO;
                })
                .collect(Collectors.toList());
        PageInfo<AgentPropertiesRespDTO> pageInfo = new PageInfo<>();
        pageInfo.setRecords(resultList);
        pageInfo.setTotal(agentPropertiesDOPage.getTotal());
        pageInfo.setCurrent(agentPropertiesDOPage.getCurrent());
        pageInfo.setPages(agentPropertiesDOPage.getPages());
        pageInfo.setSize(agentPropertiesDOPage.getSize());
        return pageInfo;
    }

    /**
     * 创建智能体标签
     */
    private void createAgentTags(Long agentId, List<Integer> tagCodes) {
        List<AgentTag> agentTags = new ArrayList<>();
        for (Integer tagCode : tagCodes) {
            AgentTagType tagType = AgentTagType.getByCode(tagCode);
            if (tagType != null) {
                AgentTag agentTag = new AgentTag();
                agentTag.setAgentId(agentId);
                agentTag.setTagName(tagType.getName());
                agentTag.setDescription(tagType.getName() + "标签");
                agentTag.setCreateTime(new Date());
                agentTag.setUpdateTime(new Date());
                agentTag.setDelFlag(0);
                agentTags.add(agentTag);
            }
        }
        if (!agentTags.isEmpty()) {
            agentTagService.saveBatch(agentTags);
        }
    }

    /**
     * 更新智能体标签
     */
    private void updateAgentTags(Long agentId, List<Integer> tagCodes) {
        // 先删除原有标签
        LambdaUpdateWrapper<AgentTag> deleteWrapper = Wrappers.lambdaUpdate(AgentTag.class)
                .eq(AgentTag::getAgentId, agentId)
                .set(AgentTag::getDelFlag, 1);
        agentTagService.update(deleteWrapper);
        
        // 创建新标签
        if (!tagCodes.isEmpty()) {
            createAgentTags(agentId, tagCodes);
        }
    }

    /**
     * 获取智能体标签
     */
    private List<AgentPropertiesRespDTO.TagInfo> getAgentTags(Long agentId) {
        LambdaQueryWrapper<AgentTag> queryWrapper = Wrappers.lambdaQuery(AgentTag.class)
                .eq(AgentTag::getAgentId, agentId)
                .eq(AgentTag::getDelFlag, 0);
        List<AgentTag> agentTags = agentTagService.list(queryWrapper);
        
        return agentTags.stream().map(tag -> {
            AgentPropertiesRespDTO.TagInfo tagInfo = new AgentPropertiesRespDTO.TagInfo();
            // 根据标签名称获取对应的枚举
            AgentTagType tagType = AgentTagType.getByName(tag.getTagName());
            if (tagType != null) {
                tagInfo.setCode(tagType.getCode());
                tagInfo.setName(tagType.getName());
            } else {
                // 如果找不到对应枚举，使用默认值
                tagInfo.setCode(11); // 其他
                tagInfo.setName(tag.getTagName());
                tagInfo.setColor("#95a5a6");
            }
            return tagInfo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<AgentPropertiesDO> listTop10() {
        LambdaQueryWrapper<AgentPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AgentPropertiesDO.class)
                .eq(AgentPropertiesDO::getDelFlag, 0)
                .orderByDesc(AgentPropertiesDO::getCreateTime)
                .last("limit 10"); // Limit to top 10
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<AgentPropertiesDO> listActiveAgents() {
        LambdaQueryWrapper<AgentPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AgentPropertiesDO.class)
                .eq(AgentPropertiesDO::getDelFlag, 0)
                .orderByDesc(AgentPropertiesDO::getCreateTime);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<SceneBindingRespDTO> getSceneBindings() {
        // Load all non-deleted agents
        List<AgentPropertiesDO> allAgents = list(Wrappers.lambdaQuery(AgentPropertiesDO.class)
                .eq(AgentPropertiesDO::getDelFlag, 0));

        List<SceneBindingRespDTO> bindings = new ArrayList<>();
        for (BusinessAgentScene scene : BusinessAgentScene.values()) {
            SceneBindingRespDTO binding = new SceneBindingRespDTO();
            binding.setSceneCode(scene.getCode());
            binding.setSceneName(scene.getDefaultAgentName());

            // Find active agent for this scene
            allAgents.stream()
                    .filter(a -> scene.getCode().equals(a.getSceneCode()) && Integer.valueOf(1).equals(a.getIsActive()))
                    .findFirst()
                    .ifPresent(active -> {
                        binding.setActiveAgentId(active.getId());
                        binding.setActiveAgentName(active.getAgentName());
                        binding.setActiveProvider(active.getAiProvider());
                    });

            // Find candidate agents by scene code or matching agent names
            List<String> candidateNames = scene.getCandidateAgentNames();
            List<SceneBindingRespDTO.CandidateAgent> candidates = allAgents.stream()
                    .filter(a -> scene.getCode().equals(a.getSceneCode()) || candidateNames.contains(a.getAgentName()))
                    .map(a -> {
                        SceneBindingRespDTO.CandidateAgent ca = new SceneBindingRespDTO.CandidateAgent();
                        ca.setId(a.getId());
                        ca.setAgentName(a.getAgentName());
                        ca.setAiProvider(a.getAiProvider());
                        return ca;
                    })
                    .collect(Collectors.toList());
            binding.setCandidates(candidates);

            bindings.add(binding);
        }
        return bindings;
    }

    @Override
    @Transactional
    public void activateAgent(String sceneCode, Long agentId) {
        // Deactivate all agents currently active for this scene
        LambdaUpdateWrapper<AgentPropertiesDO> deactivateWrapper = Wrappers.lambdaUpdate(AgentPropertiesDO.class)
                .eq(AgentPropertiesDO::getSceneCode, sceneCode)
                .eq(AgentPropertiesDO::getIsActive, 1)
                .eq(AgentPropertiesDO::getDelFlag, 0)
                .set(AgentPropertiesDO::getIsActive, 0)
                .set(AgentPropertiesDO::getUpdateTime, new Date());
        update(deactivateWrapper);

        // Activate the target agent
        LambdaUpdateWrapper<AgentPropertiesDO> activateWrapper = Wrappers.lambdaUpdate(AgentPropertiesDO.class)
                .eq(AgentPropertiesDO::getId, agentId)
                .eq(AgentPropertiesDO::getDelFlag, 0)
                .set(AgentPropertiesDO::getIsActive, 1)
                .set(AgentPropertiesDO::getSceneCode, sceneCode)
                .set(AgentPropertiesDO::getUpdateTime, new Date());
        update(activateWrapper);

        // Refresh the startup cache
        agentPropertiesLoader.refreshActiveAgents();
    }
}





