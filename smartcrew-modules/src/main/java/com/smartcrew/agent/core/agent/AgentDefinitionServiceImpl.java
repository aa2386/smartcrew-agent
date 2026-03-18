package com.smartcrew.agent.core.agent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import com.smartcrew.agent.api.agent.domain.request.AgentRegisterRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDefinitionVo;
import com.smartcrew.agent.api.agent.mapper.AgentDefinitionMapper;
import com.smartcrew.agent.api.agent.service.AgentDefinitionService;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 代理定义服务实现，负责持久化代理定义并同步运行时注册表。
 */
@Service
public class AgentDefinitionServiceImpl implements AgentDefinitionService {

    /**
     * 代理定义数据访问对象。
     */
    private final AgentDefinitionMapper agentDefinitionMapper;
    /**
     * 代理注册表。
     */
    private final AgentRegistry agentRegistry;

    /**
     * 构造 AgentDefinitionServiceImpl 所需的依赖对象。
     */
    public AgentDefinitionServiceImpl(AgentDefinitionMapper agentDefinitionMapper, AgentRegistry agentRegistry) {
        this.agentDefinitionMapper = agentDefinitionMapper;
        this.agentRegistry = agentRegistry;
    }

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
        Optional<com.smartcrew.agent.api.agent.service.Agent> registeredAgent = agentRegistry.get(saved.getAgentCode());
        if (registeredAgent.isPresent()) {
            agentRegistry.register(registeredAgent.get(), saved);
        } else {
            agentRegistry.register(new StubAgent(saved), saved);
        }
        return saved;
    }

    /**
     * 查询全部数据。
     */
    @Override
    public List<AgentDefinitionVo> listAll() {
        return agentRegistry.listDefinitions().stream()
                .sorted(Comparator.comparing(AgentDefinition::getAgentCode))
                .map(this::toVo)
                .toList();
    }

    /**
     * 根据编码查询数据。
     */
    @Override
    public Optional<AgentDefinition> findByCode(String agentCode) {
        return Optional.ofNullable(agentDefinitionMapper.selectByAgentCode(agentCode));
    }

    /**
     * 查询数据库中已保存的全部代理定义。
     */
    public List<AgentDefinition> listDatabaseDefinitions() {
        return agentDefinitionMapper.selectList(Wrappers.emptyWrapper());
    }

    /**
     * 将代理定义实体转换为视图对象。
     */
    private AgentDefinitionVo toVo(AgentDefinition definition) {
        AgentDefinitionVo vo = new AgentDefinitionVo();
        BeanUtils.copyProperties(definition, vo);
        return vo;
    }
}
