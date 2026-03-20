package com.smartcrew.agent.controller.agent;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import com.smartcrew.agent.api.agent.domain.request.AgentDispatchRequest;
import com.smartcrew.agent.api.agent.domain.request.AgentRegisterRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDefinitionVo;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.AgentCoordinator;
import com.smartcrew.agent.api.agent.service.AgentDefinitionService;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.core.page.TableDataInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 代理管理控制器，提供代理注册、查询和派发相关 REST 接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    /**
     * 代理定义服务。
     */
    private final AgentDefinitionService agentDefinitionService;
    /**
     * 代理协调器。
     */
    private final AgentCoordinator agentCoordinator;

    /**
     * 注册或更新目标对象。
     */
    @PostMapping("/register")
    public R<AgentDefinitionVo> register(@Valid @RequestBody AgentRegisterRequest request) {
        AgentDefinition definition = agentDefinitionService.register(request);
        AgentDefinitionVo vo = new AgentDefinitionVo();
        BeanUtils.copyProperties(definition, vo);
        return R.ok("agent registered", vo);
    }

    /**
     * 查询列表数据。
     */
    @GetMapping
    public TableDataInfo<AgentDefinitionVo> list() {
        return TableDataInfo.build(agentDefinitionService.listAll());
    }

    /**
     * 按代理编码派发请求。
     */
    @PostMapping("/{code}/dispatch")
    public R<AgentDispatchResponse> dispatch(@PathVariable("code") String code,
                                             @Valid @RequestBody AgentDispatchRequest request) {
        return R.ok(agentCoordinator.dispatch(code, request));
    }
}
