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

    /**
     * 按工具编码查询工具定义。
     *
     * @param toolCode 工具编码。
     * @return 匹配到的工具定义；未命中时返回 `null`。
     */
    @Select("select * from tool_definition where tool_code = #{toolCode} limit 1")
    ToolDefinition selectByToolCode(@Param("toolCode") String toolCode);

    /**
     * 更新目标对象的启用状态。
     *
     * @param toolCode 工具编码。
     * @param enabled 是否启用，`true` 表示启用，`false` 表示禁用。
     * @return 受影响的行数。
     */
    @Update("update tool_definition set enabled = #{enabled} where tool_code = #{toolCode}")
    int updateEnabledStatus(@Param("toolCode") String toolCode, @Param("enabled") boolean enabled);
}
