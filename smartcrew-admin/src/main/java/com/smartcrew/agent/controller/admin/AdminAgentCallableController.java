package com.smartcrew.agent.controller.admin;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.api.agent.service.AgentToolBindingService;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.core.page.TableDataInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台 Agent 可调用关系查询控制器。
 *
 * <p>基于 Agent 绑定的委托工具、目标白名单和 Agent 注册状态，
 * 实时解析当前 Agent 可调用的协作 Agent 列表，不依赖前端硬编码。</p>
 */
@RestController
@RequestMapping("/api/admin/agents")
@ConditionalOnProperty(prefix = "smartcrew.api.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminAgentCallableController {

    private static final String DELEGATION_TOOL_CODE = "agent-delegation";

    /** 委托工具允许的目标 Agent 白名单（与 AgentDelegationTools 保持一致） */
    private static final Set<String> ALLOWED_DELEGATION_TARGETS = Set.of(
            "life-tool-agent",
            "life-memory-agent"
    );

    private final AgentToolBindingService agentToolBindingService;
    private final AgentRegistry agentRegistry;

    public AdminAgentCallableController(AgentToolBindingService agentToolBindingService,
                                         AgentRegistry agentRegistry) {
        this.agentToolBindingService = agentToolBindingService;
        this.agentRegistry = agentRegistry;
    }

    /**
     * 获取指定 Agent 可调用的协作 Agent 列表。
     *
     * <p>结果基于真实委托工具绑定和 Agent 注册状态解析：</p>
     * <ul>
     *   <li>当前 Agent 是否绑定了 agent-delegation 工具</li>
     *   <li>委托工具允许的目标 Agent 白名单</li>
     *   <li>目标 Agent 是否在注册表中存在、是否启用、是否为 StubAgent</li>
     * </ul>
     *
     * @param code 来源 Agent 编码
     * @return 可调用协作 Agent 列表
     */
    @GetMapping("/{code}/callable-agents")
    public TableDataInfo<Map<String, Object>> listCallableAgents(@PathVariable("code") String code) {
        agentRegistry.getDefinition(code)
                .orElseThrow(() -> new ServiceException(404, "Agent 不存在: " + code));

        // 检查当前 Agent 是否绑定了委托工具
        Set<String> boundTools = agentToolBindingService.listBoundToolCodes(code);
        boolean hasDelegation = boundTools.contains(DELEGATION_TOOL_CODE);

        // 获取 Agent 定义信息
        AgentDefinition sourceDef = agentRegistry.getDefinition(code).orElse(null);
        boolean sourceEnabled = sourceDef != null && Boolean.TRUE.equals(sourceDef.getEnabled());

        List<Map<String, Object>> result = new ArrayList<>();

        for (String targetCode : ALLOWED_DELEGATION_TARGETS) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("sourceAgentCode", code);

            var targetAgent = agentRegistry.get(targetCode);
            AgentDefinition targetDef = agentRegistry.getDefinition(targetCode).orElse(null);

            entry.put("targetAgentCode", targetCode);
            entry.put("targetAgentName", targetDef != null ? targetDef.getAgentName() : targetCode);

            // Agent 类型：BUILTIN (真实 Bean) 或 DATABASE (StubAgent)
            String agentType = targetDef != null ? targetDef.getAgentType() : "UNKNOWN";
            entry.put("targetAgentType", agentType);

            // 是否可调用：需满足 ① 绑定了委托工具 ② 目标存在 ③ 目标启用 ④ 非 Stub
            boolean targetExists = targetAgent.isPresent() && targetDef != null;
            boolean targetEnabled = targetDef != null && Boolean.TRUE.equals(targetDef.getEnabled());
            boolean isStub = targetAgent.map(a -> a instanceof com.smartcrew.agent.core.agent.StubAgent).orElse(true);

            boolean callable = hasDelegation && targetExists && targetEnabled && !isStub && sourceEnabled;

            entry.put("callable", callable);
            entry.put("enabled", targetEnabled);
            entry.put("runtimeMode", isStub ? "STUB" : "BEAN");
            entry.put("sourceStatus", sourceEnabled ? "ENABLED" : "DISABLED");
            entry.put("viaToolCode", hasDelegation ? DELEGATION_TOOL_CODE : "");

            // 不可调用原因
            StringBuilder reason = new StringBuilder();
            if (callable) {
                reason.append("-");
            } else {
                if (!hasDelegation) {
                    reason.append("当前 Agent 未绑定委托工具 ").append(DELEGATION_TOOL_CODE).append("；");
                }
                if (!sourceEnabled) {
                    reason.append("当前 Agent 已禁用；");
                }
                if (!targetExists) {
                    reason.append("目标 Agent 未注册；");
                }
                if (!targetEnabled) {
                    reason.append("目标 Agent 已禁用；");
                }
                if (isStub) {
                    reason.append("目标 Agent 为 Stub 模式，非真实 Bean；");
                }
                if (reason.isEmpty()) {
                    reason.append("未知原因");
                }
            }
            entry.put("reason", reason.toString());

            result.add(entry);
        }

        return TableDataInfo.build(result);
    }
}
