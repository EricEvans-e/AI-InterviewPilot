package com.interviewpilot.questionbank.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.questionbank.api.io.req.ExamOutlineReqDTO;
import com.interviewpilot.questionbank.api.io.resp.ExamOutlineRespDTO;
import com.interviewpilot.questionbank.dao.entity.ExamOutlineDO;

import java.util.List;

/**
 * 考试大纲服务接口
 */
public interface ExamOutlineService extends IService<ExamOutlineDO> {

    /**
     * 创建考试大纲
     */
    void create(ExamOutlineReqDTO requestParam);

    /**
     * 更新考试大纲
     */
    void update(ExamOutlineReqDTO requestParam);

    /**
     * 删除考试大纲（逻辑删除）
     */
    void delete(Long id);

    /**
     * 根据ID获取考试大纲详情
     */
    ExamOutlineRespDTO getByIdResp(Long id);

    /**
     * 分页查询考试大纲
     */
    PageInfo<ExamOutlineRespDTO> getByPage(ExamOutlineReqDTO requestParam);

    /**
     * 查询所有考试大纲列表
     */
    List<ExamOutlineRespDTO> listAll();
}
