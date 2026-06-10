package com.interviewpilot.questionbank.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.questionbank.api.io.req.QuestionPageReqDTO;
import com.interviewpilot.questionbank.api.io.req.QuestionSaveReqDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionCoverageRespDTO;
import com.interviewpilot.questionbank.api.io.resp.QuestionRespDTO;
import com.interviewpilot.questionbank.dao.entity.QuestionDO;

import java.util.List;

/**
 * 题库服务接口
 */
public interface QuestionBankService extends IService<QuestionDO> {

    /**
     * 创建题目
     *
     * @param requestParam 题目参数
     * @param creatorId    创建者ID
     * @return 题目ID
     */
    Long create(QuestionSaveReqDTO requestParam, Long creatorId);

    /**
     * 更新题目
     *
     * @param id           题目ID
     * @param requestParam 题目参数
     */
    void update(Long id, QuestionSaveReqDTO requestParam);

    /**
     * 获取题目详情
     *
     * @param id 题目ID
     * @return 题目详情
     */
    QuestionRespDTO getDetail(Long id);

    /**
     * 多条件分页查询题目
     *
     * @param requestParam 查询条件
     * @return 分页结果
     */
    PageInfo<QuestionRespDTO> pageByFilter(QuestionPageReqDTO requestParam);

    /**
     * 随机选取题目
     *
     * @param collegeId 院校ID（可选）
     * @param majorId   专业ID（可选）
     * @param questionType 题型（可选）
     * @param count     数量
     * @return 题目列表
     */
    List<QuestionDO> randomSelect(Long collegeId, Long majorId, String questionType, int count);

    /**
     * 随机选取题目，带能力点和难度筛选。
     *
     * @param collegeId    院校ID（可选）
     * @param majorId      专业ID（可选）
     * @param questionType 题型（可选）
     * @param abilityTag   能力点标签（可选）
     * @param difficulty   难度（可选）
     * @param count        数量
     * @return 题目列表
     */
    List<QuestionDO> randomSelect(Long collegeId,
                                  Long majorId,
                                  String questionType,
                                  String abilityTag,
                                  String difficulty,
                                  int count);

    /**
     * 棰樺簱瑕嗙洊搴︾粺璁?
     *
     * @param collegeId 闄㈡牎ID
     * @param majorId 涓撲笟ID
     * @param interviewMode 闈㈣瘯妯″紡鎴栭鍨?
     * @param requiredCount 鏈闈㈣瘯鏈熸湜棰樻暟
     * @return 瑕嗙洊搴︾粺璁?
     */
    QuestionCoverageRespDTO coverage(Long collegeId,
                                     Long majorId,
                                     String interviewMode,
                                     String abilityTag,
                                     String difficulty,
                                     int requiredCount);

    /**
     * 按能力标签选取题目
     *
     * @param abilityTag 能力标签
     * @param count      数量
     * @return 题目列表
     */
    List<QuestionDO> selectByAbility(String abilityTag, int count);

    /**
     * 更新题目状态
     *
     * @param id     题目ID
     * @param status 新状态
     */
    void updateStatus(Long id, String status);
}
