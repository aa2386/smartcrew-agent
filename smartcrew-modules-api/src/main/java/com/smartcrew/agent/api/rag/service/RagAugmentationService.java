package com.smartcrew.agent.api.rag.service;

import com.smartcrew.agent.api.rag.domain.vo.RagAugmentationResult;

/**
 * Agent 运行时检索增强服务接口。
 */
public interface RagAugmentationService {

    /**
     * 按 Agent 与用户问题构建运行时知识增强结果。
     *
     * @param agentCode Agent 编码
     * @param query 用户问题
     * @param traceId 链路追踪 ID
     * @return 检索增强结果
     */
    RagAugmentationResult augment(String agentCode, String query, String traceId);
}
