package com.interviewpilot.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.ai.api.io.req.AiPropertiesCreateReqDTO;
import com.interviewpilot.ai.api.io.req.AiPropertiesPageReqDTO;
import com.interviewpilot.ai.api.io.req.AiPropertiesUpdateReqDTO;
import com.interviewpilot.ai.api.io.resp.AiModelOptionRespDTO;
import com.interviewpilot.ai.api.io.resp.AiPropertiesRespDTO;
import com.interviewpilot.ai.dao.entity.AiPropertiesDO;
import com.interviewpilot.ai.dao.mapper.AiPropertiesMapper;
import com.interviewpilot.ai.service.AiPropertiesService;
import com.interviewpilot.common.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiPropertiesServiceImpl extends ServiceImpl<AiPropertiesMapper, AiPropertiesDO>
        implements AiPropertiesService {

    @Override
    public List<AiModelOptionRespDTO> getAvailableAiModels() {
        return getAllEnabledAiProperties().stream()
                .map(prop -> AiModelOptionRespDTO.builder()
                        .id(prop.getId())
                        .aiName(prop.getAiName())
                        .aiType(Integer.valueOf(prop.getAiType()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void createAiProperties(AiPropertiesCreateReqDTO requestParam) {
        LambdaQueryWrapper<AiPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getAiName, requestParam.getAiName())
                .eq(AiPropertiesDO::getDelFlag, 0);

        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ClientException("AI name already exists");
        }

        AiPropertiesDO aiPropertiesDO = new AiPropertiesDO();
        BeanUtil.copyProperties(requestParam, aiPropertiesDO);
        aiPropertiesDO.setCreateTime(new Date());
        aiPropertiesDO.setUpdateTime(new Date());
        aiPropertiesDO.setDelFlag(0);

        if (aiPropertiesDO.getIsEnabled() == null) {
            aiPropertiesDO.setIsEnabled(1);
        }

        baseMapper.insert(aiPropertiesDO);
    }

    @Override
    public void updateAiProperties(AiPropertiesUpdateReqDTO requestParam) {
        AiPropertiesDO existingRecord = baseMapper.selectById(requestParam.getId());
        if (existingRecord == null || existingRecord.getDelFlag() == 1) {
            throw new ClientException("AI config does not exist");
        }

        if (StrUtil.isNotBlank(requestParam.getAiName())
                && !requestParam.getAiName().equals(existingRecord.getAiName())) {
            LambdaQueryWrapper<AiPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AiPropertiesDO.class)
                    .eq(AiPropertiesDO::getAiName, requestParam.getAiName())
                    .eq(AiPropertiesDO::getDelFlag, 0)
                    .ne(AiPropertiesDO::getId, requestParam.getId());

            if (baseMapper.selectCount(queryWrapper) > 0) {
                throw new ClientException("AI name already exists");
            }
        }

        AiPropertiesDO aiPropertiesDO = new AiPropertiesDO();
        BeanUtil.copyProperties(requestParam, aiPropertiesDO);
        aiPropertiesDO.setUpdateTime(new Date());

        baseMapper.updateById(aiPropertiesDO);
    }

    @Override
    public void deleteAiProperties(Long id) {
        AiPropertiesDO existingRecord = baseMapper.selectById(id);
        if (existingRecord == null || existingRecord.getDelFlag() == 1) {
            throw new ClientException("AI config does not exist");
        }

        LambdaUpdateWrapper<AiPropertiesDO> updateWrapper = Wrappers.lambdaUpdate(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getId, id)
                .set(AiPropertiesDO::getDelFlag, 1)
                .set(AiPropertiesDO::getUpdateTime, new Date());

        baseMapper.update(null, updateWrapper);
    }

    @Override
    public AiPropertiesRespDTO getAiPropertiesById(Long id) {
        AiPropertiesDO aiPropertiesDO = baseMapper.selectById(id);
        if (aiPropertiesDO == null || aiPropertiesDO.getDelFlag() == 1) {
            throw new ClientException("AI config does not exist");
        }

        AiPropertiesRespDTO respDTO = new AiPropertiesRespDTO();
        BeanUtil.copyProperties(aiPropertiesDO, respDTO);
        if (StrUtil.isNotBlank(respDTO.getApiKey())) {
            respDTO.setApiKey(maskApiKey(respDTO.getApiKey()));
        }
        return respDTO;
    }

    @Override
    public IPage<AiPropertiesRespDTO> pageAiProperties(AiPropertiesPageReqDTO requestParam) {
        LambdaQueryWrapper<AiPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getDelFlag, 0)
                .like(StrUtil.isNotBlank(requestParam.getAiName()), AiPropertiesDO::getAiName, requestParam.getAiName())
                .eq(StrUtil.isNotBlank(requestParam.getAiType()), AiPropertiesDO::getAiType, requestParam.getAiType())
                .eq(requestParam.getIsEnabled() != null, AiPropertiesDO::getIsEnabled, requestParam.getIsEnabled())
                .orderByDesc(AiPropertiesDO::getCreateTime);

        IPage<AiPropertiesDO> page = baseMapper.selectPage(requestParam, queryWrapper);

        IPage<AiPropertiesRespDTO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(page.getRecords().stream()
                .map(this::toMaskedResponse)
                .collect(Collectors.toList()));
        return resultPage;
    }

    @Override
    public List<AiPropertiesRespDTO> listEnabledAiProperties() {
        LambdaQueryWrapper<AiPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getDelFlag, 0)
                .eq(AiPropertiesDO::getIsEnabled, 1)
                .orderByDesc(AiPropertiesDO::getCreateTime);

        return baseMapper.selectList(queryWrapper).stream()
                .map(this::toMaskedResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void toggleAiPropertiesStatus(Long id, Integer isEnabled) {
        AiPropertiesDO existingRecord = baseMapper.selectById(id);
        if (existingRecord == null || existingRecord.getDelFlag() == 1) {
            throw new ClientException("AI config does not exist");
        }

        LambdaUpdateWrapper<AiPropertiesDO> updateWrapper = Wrappers.lambdaUpdate(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getId, id)
                .set(AiPropertiesDO::getIsEnabled, isEnabled)
                .set(AiPropertiesDO::getUpdateTime, new Date());

        baseMapper.update(null, updateWrapper);
    }

    @Override
    public void setDefaultAiProperties(Long id) {
        AiPropertiesDO target = baseMapper.selectById(id);
        if (target == null || target.getDelFlag() == 1) {
            throw new ClientException("AI config does not exist");
        }

        LambdaUpdateWrapper<AiPropertiesDO> unsetWrapper = Wrappers.lambdaUpdate(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getAiType, target.getAiType())
                .eq(AiPropertiesDO::getDelFlag, 0)
                .ne(AiPropertiesDO::getId, id)
                .set(AiPropertiesDO::getIsDefault, 0)
                .set(AiPropertiesDO::getUpdateTime, new Date());
        baseMapper.update(null, unsetWrapper);

        LambdaUpdateWrapper<AiPropertiesDO> setWrapper = Wrappers.lambdaUpdate(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getId, id)
                .set(AiPropertiesDO::getIsDefault, 1)
                .set(AiPropertiesDO::getUpdateTime, new Date());
        baseMapper.update(null, setWrapper);
    }

    @Override
    public List<AiPropertiesRespDTO> getAllEnabledAiProperties() {
        return listEnabledAiProperties();
    }

    @Override
    public AiPropertiesDO getEnabledByAiType(String aiType) {
        LambdaQueryWrapper<AiPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getDelFlag, 0)
                .eq(AiPropertiesDO::getIsEnabled, 1)
                .eq(AiPropertiesDO::getAiType, aiType)
                .orderByDesc(AiPropertiesDO::getIsDefault)
                .orderByDesc(AiPropertiesDO::getCreateTime)
                .last("LIMIT 1");

        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public AiPropertiesDO getDefaultMimoConfig() {
        AiPropertiesDO mimoConfig = getEnabledByAiType("openai");
        if (mimoConfig == null) {
            throw new ClientException("Mimo AI config does not exist or is disabled");
        }
        return mimoConfig;
    }

    private AiPropertiesRespDTO toMaskedResponse(AiPropertiesDO record) {
        AiPropertiesRespDTO respDTO = new AiPropertiesRespDTO();
        BeanUtil.copyProperties(record, respDTO);
        if (StrUtil.isNotBlank(respDTO.getApiKey())) {
            respDTO.setApiKey(maskApiKey(respDTO.getApiKey()));
        }
        return respDTO;
    }

    private String maskApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey) || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
