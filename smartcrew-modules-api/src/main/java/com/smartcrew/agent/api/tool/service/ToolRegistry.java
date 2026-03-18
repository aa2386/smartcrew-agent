package com.smartcrew.agent.api.tool.service;

import com.smartcrew.agent.api.tool.domain.model.ToolMetadata;

import java.util.List;
import java.util.Optional;

/**
 * ToolRegistry 接口，负责相关组件的注册、查询与管理。
 */
public interface ToolRegistry {

    /**
     * 刷新运行时注册数据。
     */
    void refresh();

    /**
     * 查询并返回全部记录。
     *
     * @return 结果列表。
     */
    List<ToolMetadata> listAll();

    /**
     * 按编码查询元数据信息。
     *
     * @param toolCode 工具编码。
     * @return 匹配结果；未找到时返回空 `Optional`。
     */
    Optional<ToolMetadata> getByCode(String toolCode);

    /**
     * 设置工具启用状态。
     *
     * @param toolCode 工具编码。
     * @param enabled 是否启用，`true` 表示启用，`false` 表示禁用。
     */
    void setEnabled(String toolCode, boolean enabled);
}
