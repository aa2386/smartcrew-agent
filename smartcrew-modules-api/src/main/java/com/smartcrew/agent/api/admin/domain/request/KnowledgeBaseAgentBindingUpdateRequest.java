package com.smartcrew.agent.api.admin.domain.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库 Agent 绑定替换请求。
 */
@Data
public class KnowledgeBaseAgentBindingUpdateRequest {

    /**
     * 目标 Agent 编码列表。
     */
    private List<String> agentCodes = new ArrayList<>();
}
