package com.smartcrew.agent.api.mcp.service;

import com.smartcrew.agent.api.mcp.domain.request.McpInfoRequest;
import com.smartcrew.agent.api.mcp.domain.vo.McpInfoVo;

import java.util.List;
import java.util.Optional;

/**
 * McpInfoService 接口，定义该领域的业务能力与操作约定。
 */
public interface McpInfoService {

    McpInfoVo saveOrUpdate(McpInfoRequest request);

    List<McpInfoVo> listAll();

    Optional<McpInfoVo> findByServerName(String serverName);
}
