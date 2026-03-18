package com.smartcrew.agent.api.mcp.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * McpInfo 实体，表示持久化层的业务数据结构。
 */
@Data
@TableName("mcp_info")
@EqualsAndHashCode(callSuper = true)
public class McpInfo extends BaseEntity {

    /**
     * MCP 服务主键 ID。
     */
    @TableId(value = "mcp_id", type = IdType.AUTO)
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
