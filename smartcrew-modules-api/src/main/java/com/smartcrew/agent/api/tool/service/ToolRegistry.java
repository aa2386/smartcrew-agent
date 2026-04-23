package com.smartcrew.agent.api.tool.service;

import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolActionMetadata;
import com.smartcrew.agent.api.tool.domain.model.ToolMetadata;

import java.util.List;
import java.util.Optional;

/**
 * ToolRegistry 接口，负责运行时 Tool 注册、解析与查询。
 */
public interface ToolRegistry {

    void refresh();

    List<ResolvedToolDefinition> listAll();

    Optional<ResolvedToolDefinition> getByCode(String toolCode);

    Optional<ToolActionMetadata> getAction(String toolCode, String actionName);

    default List<ToolMetadata> listLegacyMetadata() {
        return listAll().stream()
                .map(ToolMetadata::fromResolved)
                .toList();
    }
}
