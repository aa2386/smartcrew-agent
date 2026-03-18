package com.smartcrew.agent.api.mcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.mcp.domain.entity.McpInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MCP ??????????? MCP ????????
 */
@Mapper
public interface McpInfoMapper extends BaseMapper<McpInfo> {

    @Select("select * from mcp_info where server_name = #{serverName} limit 1")
    McpInfo selectByServerName(@Param("serverName") String serverName);

    @Update("update mcp_info set status = #{enabled} where server_name = #{serverName}")
    int updateStatusByServerName(@Param("serverName") String serverName, @Param("enabled") boolean enabled);
}
