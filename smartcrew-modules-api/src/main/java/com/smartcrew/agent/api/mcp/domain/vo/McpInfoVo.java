package com.smartcrew.agent.api.mcp.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * MCP ???????????????????
 */
@Data
@Builder
public class McpInfoVo {

    /**
     * MCP 服务主键 ID。
     */
    private Long mcpId;
    /**
     * 服务端名称。
     */
    private String serverName;
    /**
     * 传输类型。
     */
    private String transportType;
    /**
     * 启动命令。
     */
    private String command;
    /**
     * 启动参数。
     */
    private String arguments;
    /**
     * 环境变量配置。
     */
    private String env;
    /**
     * 状态标记。
     */
    private Boolean status;
    /**
     * 描述信息。
     */
    private String description;
}
