package com.smartcrew.agent.api.agent.service;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import com.smartcrew.agent.api.agent.domain.request.AgentRegisterRequest;
import com.smartcrew.agent.api.agent.domain.vo.AgentDefinitionVo;

import java.util.List;
import java.util.Optional;

/**
 * AgentDefinitionService 接口，定义该领域的业务能力与操作约定。
 */
public interface AgentDefinitionService {

    /**
     * 注册或更新 Agent 定义。
     *
     * @param request 请求参数。
     * @return 注册或更新后的 Agent 定义。
     */
    AgentDefinition register(AgentRegisterRequest request);

    /**
     * 查询并返回全部记录。
     *
     * @return 结果列表。
     */
    List<AgentDefinitionVo> listAll();

    /**
     * 按编码查询 Agent 定义。
     *
     * @param agentCode Agent 编码。
     * @return 匹配结果；未找到时返回空 `Optional`。
     */
    Optional<AgentDefinition> findByCode(String agentCode);
}
