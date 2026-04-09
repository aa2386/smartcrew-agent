package com.smartcrew.agent.api.prompt.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 与 Prompt 模板绑定关系实体。
 */
@Data
@TableName("agent_prompt_binding")
@EqualsAndHashCode(callSuper = true)
public class AgentPromptBinding extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Agent 编码。
     */
    private String agentCode;

    /**
     * Prompt 模板主键 ID。
     */
    private Long promptTemplateId;

    /**
     * 拼接顺序。
     */
    private Integer sortOrder;
}
