package com.interviewpilot.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.questionbank.api.io.req.CollegeReqDTO;
import com.interviewpilot.questionbank.api.io.resp.CollegeRespDTO;
import com.interviewpilot.questionbank.dao.entity.CollegeDO;
import com.interviewpilot.questionbank.dao.mapper.CollegeMapper;
import com.interviewpilot.questionbank.service.CollegeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 院校服务实现
 */
@Service
@RequiredArgsConstructor
public class CollegeServiceImpl extends ServiceImpl<CollegeMapper, CollegeDO>
        implements CollegeService {

    @Override
    @Transactional
    public void create(CollegeReqDTO requestParam) {
        CollegeDO collegeDO = new CollegeDO();
        BeanUtils.copyProperties(requestParam, collegeDO);
        collegeDO.setCreateTime(new Date());
        collegeDO.setUpdateTime(new Date());
        collegeDO.setDelFlag(0);
        save(collegeDO);
    }

    @Override
    @Transactional
    public void update(CollegeReqDTO requestParam) {
        LambdaUpdateWrapper<CollegeDO> updateWrapper = Wrappers.lambdaUpdate(CollegeDO.class)
                .eq(CollegeDO::getId, requestParam.getId())
                .set(CollegeDO::getName, requestParam.getName())
                .set(CollegeDO::getCode, requestParam.getCode())
                .set(CollegeDO::getType, requestParam.getType())
                .set(CollegeDO::getProvince, requestParam.getProvince())
                .set(CollegeDO::getCity, requestParam.getCity())
                .set(CollegeDO::getLevel, requestParam.getLevel())
                .set(CollegeDO::getOfficialUrl, requestParam.getOfficialUrl())
                .set(CollegeDO::getRemark, requestParam.getRemark())
                .set(CollegeDO::getUpdateTime, new Date());
        update(updateWrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LambdaUpdateWrapper<CollegeDO> updateWrapper = Wrappers.lambdaUpdate(CollegeDO.class)
                .eq(CollegeDO::getId, id)
                .set(CollegeDO::getDelFlag, 1)
                .set(CollegeDO::getUpdateTime, new Date());
        update(null, updateWrapper);
    }

    @Override
    public CollegeRespDTO getByIdResp(Long id) {
        LambdaQueryWrapper<CollegeDO> queryWrapper = Wrappers.lambdaQuery(CollegeDO.class)
                .eq(CollegeDO::getId, id)
                .eq(CollegeDO::getDelFlag, 0);
        CollegeDO collegeDO = baseMapper.selectOne(queryWrapper);
        CollegeRespDTO result = new CollegeRespDTO();
        if (collegeDO != null) {
            BeanUtils.copyProperties(collegeDO, result);
        }
        return result;
    }

    @Override
    public PageInfo<CollegeRespDTO> getByPage(CollegeReqDTO requestParam) {
        Page<CollegeDO> page = new Page<>(requestParam.getPageNum(), requestParam.getPageSize());
        LambdaQueryWrapper<CollegeDO> queryWrapper = Wrappers.lambdaQuery(CollegeDO.class)
                .eq(CollegeDO::getDelFlag, 0)
                .orderByDesc(CollegeDO::getCreateTime);
        Page<CollegeDO> collegeDOPage = baseMapper.selectPage(page, queryWrapper);
        List<CollegeRespDTO> resultList = collegeDOPage.getRecords().stream()
                .map(item -> {
                    CollegeRespDTO respDTO = new CollegeRespDTO();
                    BeanUtils.copyProperties(item, respDTO);
                    return respDTO;
                })
                .collect(Collectors.toList());
        PageInfo<CollegeRespDTO> pageInfo = new PageInfo<>();
        pageInfo.setRecords(resultList);
        pageInfo.setTotal(collegeDOPage.getTotal());
        pageInfo.setCurrent(collegeDOPage.getCurrent());
        pageInfo.setPages(collegeDOPage.getPages());
        pageInfo.setSize(collegeDOPage.getSize());
        return pageInfo;
    }

    @Override
    public List<CollegeRespDTO> listAll() {
        LambdaQueryWrapper<CollegeDO> queryWrapper = Wrappers.lambdaQuery(CollegeDO.class)
                .eq(CollegeDO::getDelFlag, 0)
                .orderByDesc(CollegeDO::getCreateTime);
        List<CollegeDO> list = baseMapper.selectList(queryWrapper);
        return list.stream()
                .map(item -> {
                    CollegeRespDTO respDTO = new CollegeRespDTO();
                    BeanUtils.copyProperties(item, respDTO);
                    return respDTO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CollegeRespDTO> listByProvince(String province) {
        LambdaQueryWrapper<CollegeDO> queryWrapper = Wrappers.lambdaQuery(CollegeDO.class)
                .eq(CollegeDO::getDelFlag, 0)
                .eq(CollegeDO::getProvince, province)
                .orderByDesc(CollegeDO::getCreateTime);
        List<CollegeDO> list = baseMapper.selectList(queryWrapper);
        return list.stream()
                .map(item -> {
                    CollegeRespDTO respDTO = new CollegeRespDTO();
                    BeanUtils.copyProperties(item, respDTO);
                    return respDTO;
                })
                .collect(Collectors.toList());
    }
}
