package com.smartcrew.agent.api.admin.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库 Agent 绑定视图对象。
 */
@Data
public class KnowledgeBaseAgentBindingVo {

    private String baseCode;

    private List<KnowledgeBaseAgentOptionVo> boundAgents = new ArrayList<>();

    private List<KnowledgeBaseAgentOptionVo> availableAgents = new ArrayList<>();
}
