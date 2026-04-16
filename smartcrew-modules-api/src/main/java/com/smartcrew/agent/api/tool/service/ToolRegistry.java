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

    /**
     * 刷新运行时注册数据。
     */
    void refresh();

    /**
     * 查询全部解析后的 Tool 定义。
     */
    List<ResolvedToolDefinition> listAll();

    /**
     * 按编码查询解析后的 Tool 定义。
     */
    Optional<ResolvedToolDefinition> getByCode(String toolCode);

    /**
     * 按工具编码与动作名称查询动作元数据。
     */
    Optional<ToolActionMetadata> getAction(String toolCode, String actionName);

    /**
     * 返回兼容旧接口的简化元数据视图。
     */
    default List<ToolMetadata> listLegacyMetadata() {
        return listAll().stream()
                .map(ToolMetadata::fromResolved)
                .toList();
    }

    /**
     * 设置目标 Tool 的启用状态。
     */
    void setEnabled(String toolCode, boolean enabled);
}
