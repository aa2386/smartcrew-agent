package com.smartcrew.agent.api.admin.domain.vo;

import lombok.Data;

/**
 * 知识库可绑定 Agent 选项。
 */
@Data
public class KnowledgeBaseAgentOptionVo {

    /**
     * Agent 编码。
     */
    private String agentCode;

    /**
     * Agent 名称。
     */
    private String agentName;

    /**
     * Agent 类型。
     */
    private String agentType;

    /**
     * 是否启用。
     */
    private Boolean enabled;
}
