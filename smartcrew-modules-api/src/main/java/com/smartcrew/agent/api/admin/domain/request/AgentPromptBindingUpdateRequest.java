package com.smartcrew.agent.api.admin.domain.request;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent Prompt 绑定整表替换请求。
 */
@Data
public class AgentPromptBindingUpdateRequest {

    /**
     * 绑定列表，顺序即最终拼接顺序。
     */
    @Valid
    private List<AgentPromptBindingItemRequest> bindings = new ArrayList<>();
}
