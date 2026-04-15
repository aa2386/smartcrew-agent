package com.smartcrew.agent.api.admin.domain.vo;

import lombok.Data;

/**
 * 知识库可绑定 Agent 选项。
 */
@Data
public class KnowledgeBaseAgentOptionVo {

    private String agentCode;

    private String agentName;

    private String agentType;

    private Boolean enabled;
}
