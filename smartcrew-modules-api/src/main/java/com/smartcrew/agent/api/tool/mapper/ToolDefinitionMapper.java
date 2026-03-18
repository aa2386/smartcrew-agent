package com.smartcrew.agent.api.tool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * ToolDefinitionMapper 接口，负责对应领域对象的数据访问操作。
 */
@Mapper
public interface ToolDefinitionMapper extends BaseMapper<ToolDefinition> {

    @Select("select * from tool_definition where tool_code = #{toolCode} limit 1")
    ToolDefinition selectByToolCode(@Param("toolCode") String toolCode);

    @Update("update tool_definition set enabled = #{enabled} where tool_code = #{toolCode}")
    int updateEnabledStatus(@Param("toolCode") String toolCode, @Param("enabled") boolean enabled);
}
