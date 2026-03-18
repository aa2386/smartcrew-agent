package com.smartcrew.agent.api.agent.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AgentToolBinding 实体，表示持久化层的业务数据结构。
 */
@Data
@TableName("agent_tool_binding")
@EqualsAndHashCode(callSuper = true)
public class AgentToolBinding extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 代理编码。
     */
    private String agentCode;
    /**
     * 工具编码。
     */
    private String toolCode;
}
