package com.interviewpilot.questionbank.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.questionbank.api.io.req.CollegeReqDTO;
import com.interviewpilot.questionbank.api.io.resp.CollegeRespDTO;
import com.interviewpilot.questionbank.dao.entity.CollegeDO;

import java.util.List;

/**
 * 院校服务接口
 */
public interface CollegeService extends IService<CollegeDO> {

    /**
     * 创建院校
     */
    void create(CollegeReqDTO requestParam);

    /**
     * 更新院校
     */
    void update(CollegeReqDTO requestParam);

    /**
     * 删除院校（逻辑删除）
     */
    void delete(Long id);

    /**
     * 根据ID获取院校详情
     */
    CollegeRespDTO getByIdResp(Long id);

    /**
     * 分页查询院校
     */
    PageInfo<CollegeRespDTO> getByPage(CollegeReqDTO requestParam);

    /**
     * 查询所有院校列表
     */
    List<CollegeRespDTO> listAll();

    /**
     * 根据省份查询院校列表
     */
    List<CollegeRespDTO> listByProvince(String province);
}
