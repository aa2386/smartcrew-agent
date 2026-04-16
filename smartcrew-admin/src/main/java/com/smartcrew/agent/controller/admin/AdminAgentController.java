package com.smartcrew.agent.controller.admin;

import com.smartcrew.agent.api.admin.domain.request.AgentConfigUpdateRequest;
import com.smartcrew.agent.api.admin.domain.request.AgentPromptBindingUpdateRequest;
import com.smartcrew.agent.api.admin.domain.request.AgentToolBindingUpdateRequest;
import com.smartcrew.agent.api.agent.domain.request.AgentRegisterRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDefinitionVo;
import com.smartcrew.agent.api.agent.domain.vo.AgentToolBindingVo;
import com.smartcrew.agent.api.agent.service.AgentDefinitionService;
import com.smartcrew.agent.api.agent.service.AgentToolBindingService;
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

    private final AgentDefinitionService agentDefinitionService;
    private final AgentPromptBindingService agentPromptBindingService;
    private final AgentToolBindingService agentToolBindingService;

    public AdminAgentController(AgentDefinitionService agentDefinitionService,
                                AgentPromptBindingService agentPromptBindingService,
                                AgentToolBindingService agentToolBindingService) {
        this.agentDefinitionService = agentDefinitionService;
        this.agentPromptBindingService = agentPromptBindingService;
        this.agentToolBindingService = agentToolBindingService;
    }

    /**
     * 获取所有 Agent 列表。
     *
     * @return Agent 定义列表
     */
    @GetMapping
    public TableDataInfo<AgentDefinitionVo> list() {
        return TableDataInfo.build(agentDefinitionService.listAll());
    }

    /**
     * 获取指定 Agent 详情。
     *
     * @param code Agent 编码
     * @return Agent 定义详情
     */
    @GetMapping("/{code}")
    public R<AgentDefinitionVo> detail(@PathVariable("code") String code) {
        AgentDefinitionVo vo = agentDefinitionService.findViewByCode(code)
                .orElseThrow(() -> new ServiceException(404, "Agent 不存在"));
        return R.ok(vo);
    }

    /**
     * 获取指定 Agent 的提示词绑定列表。
     *
     * @param code Agent 编码
     * @return 提示词绑定列表
     */
    @GetMapping("/{code}/prompt-bindings")
    public R<List<AgentPromptBindingVo>> listPromptBindings(@PathVariable("code") String code) {
        ensureAgentExists(code);
        return R.ok(agentPromptBindingService.listByAgentCode(code));
    }

    /**
     * 获取指定 Agent 的工具绑定配置。
     *
     * @param code Agent 编码
     * @return 工具绑定配置
     */
    @GetMapping("/{code}/tool-bindings")
    public R<AgentToolBindingVo> listToolBindings(@PathVariable("code") String code) {
        ensureAgentExists(code);
        return R.ok(agentToolBindingService.getBindings(code));
    }

    /**
     * 创建 Agent 配置。
     *
     * @param request Agent 配置请求
     * @return 创建后的 Agent 定义
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
     *
     * @param code    Agent 编码
     * @param request Agent 配置请求
     * @return 更新后的 Agent 定义
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
     * 替换 Agent 的提示词绑定。
     *
     * @param code    Agent 编码
     * @param request 提示词绑定更新请求
     * @return 更新后的提示词绑定列表
     */
    @PutMapping("/{code}/prompt-bindings")
    public R<List<AgentPromptBindingVo>> replacePromptBindings(@PathVariable("code") String code,
                                                               @Valid @RequestBody AgentPromptBindingUpdateRequest request) {
        ensureAgentExists(code);
        return R.ok(agentPromptBindingService.replaceBindings(code, request));
    }

    /**
     * 替换 Agent 的工具绑定。
     *
     * @param code    Agent 编码
     * @param request 工具绑定更新请求
     * @return 更新后的工具绑定配置
     */
    @PutMapping("/{code}/tool-bindings")
    public R<AgentToolBindingVo> replaceToolBindings(@PathVariable("code") String code,
                                                     @RequestBody(required = false) AgentToolBindingUpdateRequest request) {
        ensureAgentExists(code);
        AgentToolBindingUpdateRequest safeRequest = request == null ? new AgentToolBindingUpdateRequest() : request;
        return R.ok(agentToolBindingService.replaceBindings(code, safeRequest));
    }

    /* 校验 Agent 是否存在，不存在则抛出异常。 */
    private void ensureAgentExists(String code) {
        agentDefinitionService.findViewByCode(code)
                .orElseThrow(() -> new ServiceException(404, "Agent 不存在"));
    }
}
