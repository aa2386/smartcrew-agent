package com.smartcrew.agent.api.mcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.mcp.domain.entity.McpInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * McpInfoMapper 接口，负责对应领域对象的数据访问操作。
 */
@Mapper
public interface McpInfoMapper extends BaseMapper<McpInfo> {

    /**
     * 按服务名称查询 MCP 信息。
     *
     * @param serverName MCP 服务名称。
     * @return 匹配到的 MCP 信息；未命中时返回 `null`。
     */
    @Select("select * from mcp_info where server_name = #{serverName} limit 1")
    McpInfo selectByServerName(@Param("serverName") String serverName);

    /**
     * 按服务名称更新启用状态。
     *
     * @param serverName MCP 服务名称。
     * @param enabled 是否启用，`true` 表示启用，`false` 表示禁用。
     * @return 受影响的行数。
     */
    @Update("update mcp_info set status = #{enabled} where server_name = #{serverName}")
    int updateStatusByServerName(@Param("serverName") String serverName, @Param("enabled") boolean enabled);
}
