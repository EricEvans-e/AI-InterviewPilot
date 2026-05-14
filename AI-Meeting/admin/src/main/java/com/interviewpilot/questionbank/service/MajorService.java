package com.interviewpilot.questionbank.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.questionbank.api.io.req.MajorReqDTO;
import com.interviewpilot.questionbank.api.io.resp.MajorRespDTO;
import com.interviewpilot.questionbank.dao.entity.MajorDO;

import java.util.List;

/**
 * 专业服务接口
 */
public interface MajorService extends IService<MajorDO> {

    /**
     * 创建专业
     */
    void create(MajorReqDTO requestParam);

    /**
     * 更新专业
     */
    void update(MajorReqDTO requestParam);

    /**
     * 删除专业（逻辑删除）
     */
    void delete(Long id);

    /**
     * 根据ID获取专业详情
     */
    MajorRespDTO getByIdResp(Long id);

    /**
     * 分页查询专业
     */
    PageInfo<MajorRespDTO> getByPage(MajorReqDTO requestParam);

    /**
     * 查询所有专业列表
     */
    List<MajorRespDTO> listAll();
}
