package com.smartcrew.agent.api.tool.service;

import com.smartcrew.agent.api.tool.domain.model.ToolMetadata;

import java.util.List;
import java.util.Optional;

/**
 * ?????????????????????
 */
public interface ToolRegistry {

    void refresh();

    List<ToolMetadata> listAll();

    Optional<ToolMetadata> getByCode(String toolCode);

    void setEnabled(String toolCode, boolean enabled);
}
