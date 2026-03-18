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

    /**
     * 保存或更新工具定义。
     *
     * @param request 请求参数。
     * @return 保存后的工具定义。
     */
    ToolDefinition saveOrUpdate(ToolDefinitionRequest request);

    /**
     * 查询全部工具定义。
     *
     * @return 结果列表。
     */
    List<ToolDefinitionVo> listAll();

    /**
     * 按工具编码查询记录。
     *
     * @param toolCode 工具编码。
     * @return 匹配结果；未找到时返回空 `Optional`。
     */
    Optional<ToolDefinition> findByToolCode(String toolCode);

    /**
     * 更新目标对象的启用状态。
     *
     * @param toolCode 工具编码。
     * @param enabled 是否启用，`true` 表示启用，`false` 表示禁用。
     */
    void updateEnabledStatus(String toolCode, boolean enabled);
}
