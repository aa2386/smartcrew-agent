package com.smartcrew.agent.core.agent.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import com.smartcrew.agent.api.agent.domain.request.AgentRegisterRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDefinitionVo;
import com.smartcrew.agent.api.agent.mapper.AgentDefinitionMapper;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.agent.service.AgentDefinitionService;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.core.agent.StubAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Agent 定义服务实现，负责持久化 Agent 配置并同步运行时注册表。
 */
@RequiredArgsConstructor
@Service
public class AgentDefinitionServiceImpl implements AgentDefinitionService {

    /**
     * 仅代码存在时的来源状态。
     */
    private static final String SOURCE_CODE_ONLY = "CODE_ONLY";

    /**
     * 仅数据库存在时的来源状态。
     */
    private static final String SOURCE_DB_ONLY = "DB_ONLY";

    /**
     * 代码与数据库均存在时的来源状态。
     */
    private static final String SOURCE_LINKED = "LINKED";

    /**
     * Agent 定义数据访问对象。
     */
    private final AgentDefinitionMapper agentDefinitionMapper;

    /**
     * Agent 运行时注册表。
     */
    private final AgentRegistry agentRegistry;

    /**
     * 注册或更新目标对象。
     */
    @Override
    public AgentDefinition register(AgentRegisterRequest request) {
        AgentDefinition entity = agentDefinitionMapper.selectByAgentCode(request.getAgentCode());
        if (entity == null) {
            entity = new AgentDefinition();
        }
        BeanUtils.copyProperties(request, entity);
        if (entity.getId() == null) {
            agentDefinitionMapper.insert(entity);
        } else {
            agentDefinitionMapper.updateById(entity);
        }
        AgentDefinition saved = entity;
        Optional<Agent> registeredAgent = agentRegistry.get(saved.getAgentCode());
        if (registeredAgent.isPresent()) {
            agentRegistry.register(registeredAgent.get(), saved);
        } else {
            agentRegistry.register(new StubAgent(saved), saved);
        }
        return saved;
    }

    /**
     * 查询全部 Agent 视图数据。
     */
    @Override
    public List<AgentDefinitionVo> listAll() {
        return buildMergedDefinitions();
    }

    /**
     * 按编码查询 Agent 视图数据。
     */
    @Override
    public Optional<AgentDefinitionVo> findViewByCode(String agentCode) {
        return buildMergedDefinitionMap().values().stream()
                .filter(item -> item.getAgentCode().equals(agentCode))
                .findFirst();
    }

    /**
     * 根据编码查询数据库配置。
     */
    @Override
    public Optional<AgentDefinition> findByCode(String agentCode) {
        return Optional.ofNullable(agentDefinitionMapper.selectByAgentCode(agentCode));
    }

    /**
     * 查询数据库中已保存的全部 Agent 定义。
     */
    public List<AgentDefinition> listDatabaseDefinitions() {
        return agentDefinitionMapper.selectList(Wrappers.emptyWrapper());
    }

    /**
     * 构建统一 Agent 视图列表。
     */
    private List<AgentDefinitionVo> buildMergedDefinitions() {
        return buildMergedDefinitionMap().values().stream()
                .sorted(Comparator.comparing(AgentDefinitionVo::getAgentCode))
                .toList();
    }

    /**
     * 构建统一 Agent 视图映射。
     */
    private Map<String, AgentDefinitionVo> buildMergedDefinitionMap() {
        Map<String, AgentDefinition> databaseDefinitions = listDatabaseDefinitions().stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getAgentCode(), item), LinkedHashMap::putAll);
        Map<String, AgentDefinition> runtimeDefinitions = agentRegistry.listDefinitions().stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getAgentCode(), item), LinkedHashMap::putAll);

        TreeSet<String> codes = new TreeSet<>();
        codes.addAll(databaseDefinitions.keySet());
        codes.addAll(runtimeDefinitions.keySet());

        Map<String, AgentDefinitionVo> result = new LinkedHashMap<>();
        for (String code : codes) {
            AgentDefinition dbDefinition = databaseDefinitions.get(code);
            AgentDefinition runtimeDefinition = runtimeDefinitions.get(code);
            AgentDefinition preferredDefinition = dbDefinition != null ? dbDefinition : runtimeDefinition;
            if (preferredDefinition == null) {
                continue;
            }
            AgentDefinitionVo vo = new AgentDefinitionVo();
            BeanUtils.copyProperties(preferredDefinition, vo);
            fillSourceInfo(vo, dbDefinition != null);
            agentRegistry.get(code).ifPresent(agent -> fillRuntimeInfo(vo, agent));
            result.put(code, vo);
        }
        return result;
    }

    /**
     * 填充来源状态信息。
     */
    private void fillSourceInfo(AgentDefinitionVo vo, boolean hasDatabaseConfig) {
        Optional<Agent> runtimeAgent = agentRegistry.get(vo.getAgentCode());
        boolean hasCodeBean = runtimeAgent.isPresent() && !(runtimeAgent.get() instanceof StubAgent);
        vo.setHasCodeBean(hasCodeBean);
        vo.setHasDatabaseConfig(hasDatabaseConfig);
        if (hasCodeBean && hasDatabaseConfig) {
            vo.setSourceStatus(SOURCE_LINKED);
            return;
        }
        if (hasCodeBean) {
            vo.setSourceStatus(SOURCE_CODE_ONLY);
            return;
        }
        vo.setSourceStatus(SOURCE_DB_ONLY);
    }

    /**
     * 补充运行时信息。
     */
    private void fillRuntimeInfo(AgentDefinitionVo vo, Agent agent) {
        vo.setBeanClassName(agent.getClass().getName());
        vo.setRuntimeMode(agent instanceof StubAgent ? "STUB" : "BEAN");
    }
}
