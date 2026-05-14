package com.interviewpilot.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.questionbank.api.io.req.MajorReqDTO;
import com.interviewpilot.questionbank.api.io.resp.MajorRespDTO;
import com.interviewpilot.questionbank.dao.entity.MajorDO;
import com.interviewpilot.questionbank.dao.mapper.MajorMapper;
import com.interviewpilot.questionbank.service.MajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 专业服务实现
 */
@Service
@RequiredArgsConstructor
public class MajorServiceImpl extends ServiceImpl<MajorMapper, MajorDO>
        implements MajorService {

    @Override
    @Transactional
    public void create(MajorReqDTO requestParam) {
        MajorDO majorDO = new MajorDO();
        BeanUtils.copyProperties(requestParam, majorDO);
        majorDO.setCreateTime(new Date());
        majorDO.setUpdateTime(new Date());
        majorDO.setDelFlag(0);
        save(majorDO);
    }

    @Override
    @Transactional
    public void update(MajorReqDTO requestParam) {
        LambdaUpdateWrapper<MajorDO> updateWrapper = Wrappers.lambdaUpdate(MajorDO.class)
                .eq(MajorDO::getId, requestParam.getId())
                .set(MajorDO::getCollegeId, requestParam.getCollegeId())
                .set(MajorDO::getName, requestParam.getName())
                .set(MajorDO::getCode, requestParam.getCode())
                .set(MajorDO::getCategory, requestParam.getCategory())
                .set(MajorDO::getTargetType, requestParam.getTargetType())
                .set(MajorDO::getTestForm, requestParam.getTestForm())
                .set(MajorDO::getTestContent, requestParam.getTestContent())
                .set(MajorDO::getScoreStructure, requestParam.getScoreStructure())
                .set(MajorDO::getYear, requestParam.getYear())
                .set(MajorDO::getOfficialUrl, requestParam.getOfficialUrl())
                .set(MajorDO::getUpdateTime, new Date());
        update(updateWrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LambdaUpdateWrapper<MajorDO> updateWrapper = Wrappers.lambdaUpdate(MajorDO.class)
                .eq(MajorDO::getId, id)
                .set(MajorDO::getDelFlag, 1)
                .set(MajorDO::getUpdateTime, new Date());
        update(null, updateWrapper);
    }

    @Override
    public MajorRespDTO getByIdResp(Long id) {
        LambdaQueryWrapper<MajorDO> queryWrapper = Wrappers.lambdaQuery(MajorDO.class)
                .eq(MajorDO::getId, id)
                .eq(MajorDO::getDelFlag, 0);
        MajorDO majorDO = baseMapper.selectOne(queryWrapper);
        MajorRespDTO result = new MajorRespDTO();
        if (majorDO != null) {
            BeanUtils.copyProperties(majorDO, result);
        }
        return result;
    }

    @Override
    public PageInfo<MajorRespDTO> getByPage(MajorReqDTO requestParam) {
        Page<MajorDO> page = new Page<>(requestParam.getPageNum(), requestParam.getPageSize());
        LambdaQueryWrapper<MajorDO> queryWrapper = Wrappers.lambdaQuery(MajorDO.class)
                .eq(MajorDO::getDelFlag, 0)
                .orderByDesc(MajorDO::getCreateTime);
        Page<MajorDO> majorDOPage = baseMapper.selectPage(page, queryWrapper);
        List<MajorRespDTO> resultList = majorDOPage.getRecords().stream()
                .map(item -> {
                    MajorRespDTO respDTO = new MajorRespDTO();
                    BeanUtils.copyProperties(item, respDTO);
                    return respDTO;
                })
                .collect(Collectors.toList());
        PageInfo<MajorRespDTO> pageInfo = new PageInfo<>();
        pageInfo.setRecords(resultList);
        pageInfo.setTotal(majorDOPage.getTotal());
        pageInfo.setCurrent(majorDOPage.getCurrent());
        pageInfo.setPages(majorDOPage.getPages());
        pageInfo.setSize(majorDOPage.getSize());
        return pageInfo;
    }

    @Override
    public List<MajorRespDTO> listAll() {
        LambdaQueryWrapper<MajorDO> queryWrapper = Wrappers.lambdaQuery(MajorDO.class)
                .eq(MajorDO::getDelFlag, 0)
                .orderByDesc(MajorDO::getCreateTime);
        List<MajorDO> list = baseMapper.selectList(queryWrapper);
        return list.stream()
                .map(item -> {
                    MajorRespDTO respDTO = new MajorRespDTO();
                    BeanUtils.copyProperties(item, respDTO);
                    return respDTO;
                })
                .collect(Collectors.toList());
    }
}
