package com.interviewpilot.agent.api;


import com.interviewpilot.common.convention.result.PageInfo;
import com.interviewpilot.common.convention.result.Result;
import com.interviewpilot.common.convention.result.Results;
import com.interviewpilot.agent.api.io.req.AgentPropertiesReqDTO;
import com.interviewpilot.agent.api.io.resp.AgentPropertiesRespDTO;
import com.interviewpilot.agent.api.io.resp.SceneBindingRespDTO;
import com.interviewpilot.agent.service.AgentPropertiesService;

import cn.dev33.satoken.annotation.SaCheckRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * agent配置管理层
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ip/v1/agent-properties")
@SaCheckRole("admin")
public class AgentPropertiesController {

    private final AgentPropertiesService agentPropertiesService;

    /**
     * 创建agent配置
     * @param requestParam
     * @return
     */
    @PostMapping
    public Result<Void> create(@RequestBody AgentPropertiesReqDTO requestParam) {
        agentPropertiesService.create(requestParam);
        return Results.success();
    }

    /**
     * 根据id删除agent配置
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        agentPropertiesService.delete(id);
        return Results.success();
    }

    /**
     * 更新agent配置
     * @param requestParam
     * @return
     */
    @PutMapping
    public Result<Void> update(@RequestBody AgentPropertiesReqDTO requestParam) {
        agentPropertiesService.update(requestParam);
        return Results.success();
    }

    /**
     * 根据名称查询agent配置
     * @param name
     * @return
     */
    @GetMapping("/byName")
    public Result<AgentPropertiesRespDTO> getByName(@RequestParam("name") String name) {
        return Results.success(agentPropertiesService.getByName(name));
    }

    /**
     * 分页查询agent配置
     * @param requestParam
     * @return
     */
    @GetMapping
    public Result<PageInfo<AgentPropertiesRespDTO>> getByPage(AgentPropertiesReqDTO requestParam) {
        return Results.success(agentPropertiesService.getByPage(requestParam));
    }

    /**
     * 获取所有业务场景的agent绑定信息
     */
    @GetMapping("/scene-bindings")
    public Result<List<SceneBindingRespDTO>> getSceneBindings() {
        return Results.success(agentPropertiesService.getSceneBindings());
    }

    /**
     * 激活指定场景的agent
     */
    @PutMapping("/scene-bindings/{sceneCode}/active/{agentId}")
    public Result<Void> activateAgent(@PathVariable String sceneCode, @PathVariable Long agentId) {
        agentPropertiesService.activateAgent(sceneCode, agentId);
        return Results.success();
    }
}