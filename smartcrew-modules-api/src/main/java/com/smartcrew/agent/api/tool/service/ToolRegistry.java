package com.smartcrew.agent.api.tool.service;

import com.smartcrew.agent.api.tool.domain.model.ToolMetadata;

import java.util.List;
import java.util.Optional;

/**
 * ToolRegistry 接口，负责相关组件的注册、查询与管理。
 */
public interface ToolRegistry {

    void refresh();

    List<ToolMetadata> listAll();

    Optional<ToolMetadata> getByCode(String toolCode);

    void setEnabled(String toolCode, boolean enabled);
}
