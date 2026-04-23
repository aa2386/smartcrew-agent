package com.smartcrew.agent.core.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcrew.agent.api.admin.domain.request.AgentToolBindingUpdateRequest;
import com.smartcrew.agent.api.agent.domain.entity.AgentToolBinding;
import com.smartcrew.agent.api.agent.domain.vo.AgentDefinitionVo;
import com.smartcrew.agent.api.agent.domain.vo.AgentToolBindingVo;
import com.smartcrew.agent.api.agent.mapper.AgentToolBindingMapper;
import com.smartcrew.agent.api.agent.service.AgentDefinitionService;
import com.smartcrew.agent.api.agent.service.AgentToolBindingService;
import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.service.ToolRegistry;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent Tool 绑定服务实现。
 */
@Service
@RequiredArgsConstructor
public class AgentToolBindingServiceImpl implements AgentToolBindingService {

    private final AgentToolBindingMapper agentToolBindingMapper;
    private final AgentDefinitionService agentDefinitionService;
    private final ToolRegistry toolRegistry;

    /**
     * 查询 Agent 的 Tool 绑定关系视图。
     *
     * @param agentCode Agent 编码
     * @return 绑定视图，包含已绑定和可用的工具列表
     */
    @Override
    public AgentToolBindingVo getBindings(String agentCode) {
        ensureAgentExists(agentCode);
        Set<String> boundCodes = listBoundToolCodes(agentCode);// 获取agent绑定的tool编码
        AgentToolBindingVo result = new AgentToolBindingVo();
        result.setAgentCode(agentCode);

        List<ResolvedToolDefinition> tools = toolRegistry.listAll().stream()
                .sorted(Comparator.comparing(ResolvedToolDefinition::getToolCode))
                .toList();
        for (ResolvedToolDefinition tool : tools) {
            if (boundCodes.contains(tool.getToolCode())) {
                result.getBoundTools().add(tool.toVo());
            } else {
                result.getAvailableTools().add(tool.toVo());
            }
        }
        return result;
    }

    /**
     * 替换 Agent 的 Tool 绑定关系。
     *
     * @param agentCode Agent 编码
     * @param request   绑定更新请求
     * @return 更新后的绑定视图
     */
    @Override
    @Transactional
    public AgentToolBindingVo replaceBindings(String agentCode, AgentToolBindingUpdateRequest request) {
        ensureAgentExists(agentCode);
        List<String> targetCodes = request == null || request.getToolCodes() == null
                ? Collections.emptyList()
                : request.getToolCodes().stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();

        Set<String> validCodes = toolRegistry.listAll().stream()
                .map(ResolvedToolDefinition::getToolCode)
                .collect(Collectors.toSet());
        for (String toolCode : targetCodes) {
            if (!validCodes.contains(toolCode)) {
                throw new ServiceException(400, "存在无效的 Tool 编码: " + toolCode);
            }
        }

        agentToolBindingMapper.delete(new LambdaQueryWrapper<AgentToolBinding>()
                .eq(AgentToolBinding::getAgentCode, agentCode));
        for (String toolCode : targetCodes) {
            AgentToolBinding binding = new AgentToolBinding();
            binding.setAgentCode(agentCode);
            binding.setToolCode(toolCode);
            agentToolBindingMapper.insert(binding);
        }
        return getBindings(agentCode);
    }

    /**
     * 查询 Agent 已绑定的 Tool 编码。
     *
     * @param agentCode Agent 编码
     * @return 已绑定的工具编码集合
     */
    @Override
    public Set<String> listBoundToolCodes(String agentCode) {
        ensureAgentExists(agentCode);
        return agentToolBindingMapper.selectList(new LambdaQueryWrapper<AgentToolBinding>()
                        .eq(AgentToolBinding::getAgentCode, agentCode))
                .stream()
                .map(AgentToolBinding::getToolCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 查询 Agent 已绑定的运行时 Tool 定义。
     *
     * @param agentCode Agent 编码
     * @return 已绑定的工具定义列表
     */
    @Override
    public List<ResolvedToolDefinition> listResolvedToolsByAgentCode(String agentCode) {
        Set<String> boundCodes = listBoundToolCodes(agentCode);
        if (boundCodes.isEmpty()) {
            return List.of();
        }
        return toolRegistry.listAll().stream()
                .filter(item -> boundCodes.contains(item.getToolCode()))
                .sorted(Comparator.comparing(ResolvedToolDefinition::getToolCode))
                .toList();
    }

    /**
     * 查询 Agent 已绑定且启用的运行时 Tool 定义。
     *
     * @param agentCode Agent 编码
     * @return 已绑定且可执行的工具定义列表
     */
    @Override
    public List<ResolvedToolDefinition> listEnabledResolvedToolsByAgentCode(String agentCode) {
        return listResolvedToolsByAgentCode(agentCode).stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .filter(item -> Boolean.TRUE.equals(item.getExecutable()))
                .toList();
    }

    /* 校验 Agent 是否存在，不存在则抛出异常。 */
    private void ensureAgentExists(String agentCode) {
        AgentDefinitionVo ignored = agentDefinitionService.findViewByCode(agentCode)
                .orElseThrow(() -> new ServiceException(404, "Agent 不存在"));
    }
}
