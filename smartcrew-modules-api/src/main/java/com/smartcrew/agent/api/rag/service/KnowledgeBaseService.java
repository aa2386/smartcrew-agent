package com.smartcrew.agent.api.rag.service;

import com.smartcrew.agent.api.rag.domain.entity.KnowledgeBase;

import java.util.List;
import java.util.Optional;

/**
 * 知识库服务接口。
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库。
     *
     * @param knowledgeBase 知识库实体。
     * @return 创建结果。
     */
    KnowledgeBase create(KnowledgeBase knowledgeBase);

    /**
     * 更新知识库。
     *
     * @param knowledgeBase 知识库实体。
     * @return 更新结果。
     */
    KnowledgeBase update(KnowledgeBase knowledgeBase);

    /**
     * 按编码删除知识库。
     *
     * @param baseCode 知识库编码。
     */
    void delete(String baseCode);

    /**
     * 按 ID 查询知识库。
     *
     * @param id 主键 ID。
     * @return 匹配结果。
     */
    Optional<KnowledgeBase> findById(Long id);

    /**
     * 按编码查询知识库。
     *
     * @param baseCode 知识库编码。
     * @return 匹配结果。
     */
    Optional<KnowledgeBase> findByCode(String baseCode);

    /**
     * 查询全部知识库。
     *
     * @return 知识库列表。
     */
    List<KnowledgeBase> findAll();

    /**
     * 按 Agent 编码查询已绑定的知识库。
     *
     * @param agentCode Agent 编码。
     * @return 知识库列表。
     */
    List<KnowledgeBase> findByAgentCode(String agentCode);
}
