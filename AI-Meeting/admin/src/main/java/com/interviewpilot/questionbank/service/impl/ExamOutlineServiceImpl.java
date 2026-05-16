package com.interviewpilot.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.questionbank.api.io.req.ExamOutlineReqDTO;
import com.interviewpilot.questionbank.api.io.resp.ExamOutlineRespDTO;
import com.interviewpilot.questionbank.dao.entity.ExamOutlineDO;
import com.interviewpilot.questionbank.dao.mapper.ExamOutlineMapper;
import com.interviewpilot.questionbank.service.ExamOutlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 考试大纲服务实现
 */
@Service
@RequiredArgsConstructor
public class ExamOutlineServiceImpl extends ServiceImpl<ExamOutlineMapper, ExamOutlineDO>
        implements ExamOutlineService {

    @Override
    @Transactional
    public void create(ExamOutlineReqDTO requestParam) {
        ExamOutlineDO examOutlineDO = new ExamOutlineDO();
        BeanUtils.copyProperties(requestParam, examOutlineDO);
        examOutlineDO.setCreateTime(new Date());
        examOutlineDO.setUpdateTime(new Date());
        examOutlineDO.setDelFlag(0);
        save(examOutlineDO);
    }

    @Override
    @Transactional
    public void update(ExamOutlineReqDTO requestParam) {
        LambdaUpdateWrapper<ExamOutlineDO> updateWrapper = Wrappers.lambdaUpdate(ExamOutlineDO.class)
                .eq(ExamOutlineDO::getId, requestParam.getId())
                .set(ExamOutlineDO::getCollegeId, requestParam.getCollegeId())
                .set(ExamOutlineDO::getMajorId, requestParam.getMajorId())
                .set(ExamOutlineDO::getYear, requestParam.getYear())
                .set(ExamOutlineDO::getTitle, requestParam.getTitle())
                .set(ExamOutlineDO::getDocType, requestParam.getDocType())
                .set(ExamOutlineDO::getContent, requestParam.getContent())
                .set(ExamOutlineDO::getFileUrl, requestParam.getFileUrl())
                .set(ExamOutlineDO::getSourceUrl, requestParam.getSourceUrl())
                .set(ExamOutlineDO::getStatus, requestParam.getStatus())
                .set(ExamOutlineDO::getUploaderId, requestParam.getUploaderId())
                .set(ExamOutlineDO::getUpdateTime, new Date());
        update(updateWrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LambdaUpdateWrapper<ExamOutlineDO> updateWrapper = Wrappers.lambdaUpdate(ExamOutlineDO.class)
                .eq(ExamOutlineDO::getId, id)
                .set(ExamOutlineDO::getDelFlag, 1)
                .set(ExamOutlineDO::getUpdateTime, new Date());
        update(null, updateWrapper);
    }

    @Override
    public ExamOutlineRespDTO getByIdResp(Long id) {
        LambdaQueryWrapper<ExamOutlineDO> queryWrapper = Wrappers.lambdaQuery(ExamOutlineDO.class)
                .eq(ExamOutlineDO::getId, id)
                .eq(ExamOutlineDO::getDelFlag, 0);
        ExamOutlineDO examOutlineDO = baseMapper.selectOne(queryWrapper);
        ExamOutlineRespDTO result = new ExamOutlineRespDTO();
        if (examOutlineDO != null) {
            BeanUtils.copyProperties(examOutlineDO, result);
        }
        return result;
    }

    @Override
    public PageInfo<ExamOutlineRespDTO> getByPage(ExamOutlineReqDTO requestParam) {
        Page<ExamOutlineDO> page = new Page<>(requestParam.getPageNum(), requestParam.getPageSize());
        LambdaQueryWrapper<ExamOutlineDO> queryWrapper = Wrappers.lambdaQuery(ExamOutlineDO.class)
                .eq(ExamOutlineDO::getDelFlag, 0)
                .eq(requestParam.getCollegeId() != null, ExamOutlineDO::getCollegeId, requestParam.getCollegeId())
                .eq(requestParam.getMajorId() != null, ExamOutlineDO::getMajorId, requestParam.getMajorId())
                .eq(requestParam.getYear() != null, ExamOutlineDO::getYear, requestParam.getYear())
                .orderByDesc(ExamOutlineDO::getCreateTime);
        Page<ExamOutlineDO> examOutlineDOPage = baseMapper.selectPage(page, queryWrapper);
        List<ExamOutlineRespDTO> resultList = examOutlineDOPage.getRecords().stream()
                .map(item -> {
                    ExamOutlineRespDTO respDTO = new ExamOutlineRespDTO();
                    BeanUtils.copyProperties(item, respDTO);
                    return respDTO;
                })
                .collect(Collectors.toList());
        PageInfo<ExamOutlineRespDTO> pageInfo = new PageInfo<>();
        pageInfo.setRecords(resultList);
        pageInfo.setTotal(examOutlineDOPage.getTotal());
        pageInfo.setCurrent(examOutlineDOPage.getCurrent());
        pageInfo.setPages(examOutlineDOPage.getPages());
        pageInfo.setSize(examOutlineDOPage.getSize());
        return pageInfo;
    }

    @Override
    public List<ExamOutlineRespDTO> listAll() {
        LambdaQueryWrapper<ExamOutlineDO> queryWrapper = Wrappers.lambdaQuery(ExamOutlineDO.class)
                .eq(ExamOutlineDO::getDelFlag, 0)
                .orderByDesc(ExamOutlineDO::getCreateTime);
        List<ExamOutlineDO> list = baseMapper.selectList(queryWrapper);
        return list.stream()
                .map(item -> {
                    ExamOutlineRespDTO respDTO = new ExamOutlineRespDTO();
                    BeanUtils.copyProperties(item, respDTO);
                    return respDTO;
                })
                .collect(Collectors.toList());
    }
}
