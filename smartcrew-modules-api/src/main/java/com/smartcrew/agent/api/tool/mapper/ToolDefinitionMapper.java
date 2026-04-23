package com.smartcrew.agent.api.tool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * ToolDefinitionMapper，负责 Tool 元数据持久化。
 */
@Mapper
public interface ToolDefinitionMapper extends BaseMapper<ToolDefinition> {

    @Select("select * from tool_definition where tool_code = #{toolCode} limit 1")
    ToolDefinition selectByToolCode(@Param("toolCode") String toolCode);
}
