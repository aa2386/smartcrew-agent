package com.smartcrew.agent.api.admin.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库与 Agent 绑定视图对象。
 */
@Data
public class KnowledgeBaseAgentBindingVo {

    /**
     * 知识库编码。
     */
    private String baseCode;

    /**
     * 已绑定的 Agent 列表。
     */
    private List<KnowledgeBaseAgentOptionVo> boundAgents = new ArrayList<>();

    /**
     * 可选绑定的 Agent 列表。
     */
    private List<KnowledgeBaseAgentOptionVo> availableAgents = new ArrayList<>();
}
