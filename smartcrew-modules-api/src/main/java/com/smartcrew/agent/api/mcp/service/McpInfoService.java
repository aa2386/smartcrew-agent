package com.smartcrew.agent.api.mcp.service;

import com.smartcrew.agent.api.mcp.domain.request.McpInfoRequest;
import com.smartcrew.agent.api.mcp.domain.vo.McpInfoVo;

import java.util.List;
import java.util.Optional;

/**
 * McpInfoService 接口，定义该领域的业务能力与操作约定。
 */
public interface McpInfoService {

    /**
     * 保存或更新目标记录。
     *
     * @param request 请求参数。
     * @return 保存后的 MCP 信息。
     */
    McpInfoVo saveOrUpdate(McpInfoRequest request);

    /**
     * 查询并返回全部记录。
     *
     * @return 结果列表。
     */
    List<McpInfoVo> listAll();

    /**
     * 按服务名称查询 MCP 信息。
     *
     * @param serverName MCP 服务名称。
     * @return 匹配结果；未找到时返回空 `Optional`。
     */
    Optional<McpInfoVo> findByServerName(String serverName);
}
