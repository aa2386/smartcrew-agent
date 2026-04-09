package com.smartcrew.agent.api.admin.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Agent Prompt 绑定项请求。
 */
@Data
public class AgentPromptBindingItemRequest {

    /**
     * Prompt 模板主键 ID。
     */
    @NotNull
    private Long promptTemplateId;
}
