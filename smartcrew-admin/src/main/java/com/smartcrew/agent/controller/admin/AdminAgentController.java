package com.smartcrew.agent.controller.admin;

import com.smartcrew.agent.api.admin.domain.request.AgentConfigUpdateRequest;
import com.smartcrew.agent.api.admin.domain.request.AgentPromptBindingUpdateRequest;
import com.smartcrew.agent.api.agent.domain.request.AgentRegisterRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDefinitionVo;
import com.smartcrew.agent.api.agent.service.AgentDefinitionService;
import com.smartcrew.agent.api.prompt.domain.vo.AgentPromptBindingVo;
import com.smartcrew.agent.api.prompt.service.AgentPromptBindingService;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.core.page.TableDataInfo;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台 Agent 管理控制器。
 */
@RestController
@RequestMapping("/api/admin/agents")
@ConditionalOnProperty(prefix = "smartcrew.api.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminAgentController {

    /**
     * Agent 定义服务。
     */
    private final AgentDefinitionService agentDefinitionService;

    /**
     * Agent Prompt 绑定服务。
     */
    private final AgentPromptBindingService agentPromptBindingService;

    public AdminAgentController(AgentDefinitionService agentDefinitionService,
                                AgentPromptBindingService agentPromptBindingService) {
        this.agentDefinitionService = agentDefinitionService;
        this.agentPromptBindingService = agentPromptBindingService;
    }

    /**
     * 查询 Agent 列表。
     */
    @GetMapping
    public TableDataInfo<AgentDefinitionVo> list() {
        return TableDataInfo.build(agentDefinitionService.listAll());
    }

    /**
     * 查询指定 Agent 详情。
     */
    @GetMapping("/{code}")
    public R<AgentDefinitionVo> detail(@PathVariable("code") String code) {
        AgentDefinitionVo vo = agentDefinitionService.findViewByCode(code)
                .orElseThrow(() -> new ServiceException(404, "Agent 不存在"));
        return R.ok(vo);
    }

    /**
     * 查询 Agent 的 Prompt 绑定关系。
     */
    @GetMapping("/{code}/prompt-bindings")
    public R<List<AgentPromptBindingVo>> listPromptBindings(@PathVariable("code") String code) {
        ensureAgentExists(code);
        return R.ok(agentPromptBindingService.listByAgentCode(code));
    }

    /**
     * 创建 Agent 配置。
     */
    @PostMapping
    public R<AgentDefinitionVo> create(@Valid @RequestBody AgentConfigUpdateRequest request) {
        if (agentDefinitionService.findByCode(request.getAgentCode()).isPresent()) {
            throw new ServiceException(400, "Agent 数据库配置已存在");
        }
        AgentRegisterRequest registerRequest = new AgentRegisterRequest();
        BeanUtils.copyProperties(request, registerRequest);
        agentDefinitionService.register(registerRequest);
        AgentDefinitionVo vo = agentDefinitionService.findViewByCode(request.getAgentCode())
                .orElseThrow(() -> new ServiceException(500, "Agent 创建后查询失败"));
        return R.ok(vo);
    }

    /**
     * 更新 Agent 配置。
     */
    @PutMapping("/{code}")
    public R<AgentDefinitionVo> update(@PathVariable("code") String code,
                                       @Valid @RequestBody AgentConfigUpdateRequest request) {
        agentDefinitionService.findByCode(code)
                .orElseThrow(() -> new ServiceException(404, "Agent 数据库配置不存在"));
        AgentRegisterRequest registerRequest = new AgentRegisterRequest();
        BeanUtils.copyProperties(request, registerRequest);
        registerRequest.setAgentCode(code);
        agentDefinitionService.register(registerRequest);
        AgentDefinitionVo vo = agentDefinitionService.findViewByCode(code)
                .orElseThrow(() -> new ServiceException(500, "Agent 更新后查询失败"));
        return R.ok(vo);
    }

    /**
     * 替换 Agent 的 Prompt 绑定关系。
     */
    @PutMapping("/{code}/prompt-bindings")
    public R<List<AgentPromptBindingVo>> replacePromptBindings(@PathVariable("code") String code,
                                                               @Valid @RequestBody AgentPromptBindingUpdateRequest request) {
        ensureAgentExists(code);
        return R.ok(agentPromptBindingService.replaceBindings(code, request));
    }

    /**
     * 校验 Agent 是否存在于当前统一视图中。
     */
    private void ensureAgentExists(String code) {
        agentDefinitionService.findViewByCode(code)
                .orElseThrow(() -> new ServiceException(404, "Agent 不存在"));
    }
}
