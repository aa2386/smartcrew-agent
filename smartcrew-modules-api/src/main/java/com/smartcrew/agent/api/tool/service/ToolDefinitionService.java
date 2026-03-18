package com.smartcrew.agent.api.tool.service;

import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.domain.request.ToolDefinitionRequest;
import com.smartcrew.agent.api.tool.domain.vo.ToolDefinitionVo;

import java.util.List;
import java.util.Optional;

/**
 * ToolDefinitionService 接口，定义该领域的业务能力与操作约定。
 */
public interface ToolDefinitionService {

    ToolDefinition saveOrUpdate(ToolDefinitionRequest request);

    List<ToolDefinitionVo> listAll();

    Optional<ToolDefinition> findByToolCode(String toolCode);

    void updateEnabledStatus(String toolCode, boolean enabled);
}
