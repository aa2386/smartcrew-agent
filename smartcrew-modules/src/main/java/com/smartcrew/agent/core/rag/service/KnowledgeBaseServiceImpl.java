package com.smartcrew.agent.core.rag.service;

import com.smartcrew.agent.api.rag.domain.entity.KnowledgeBase;
import com.smartcrew.agent.api.rag.mapper.KnowledgeBaseMapper;
import com.smartcrew.agent.api.rag.service.KnowledgeBaseService;
import com.smartcrew.agent.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 知识库服务实现。
 */
@Service
@ConditionalOnProperty(prefix = "smartcrew.rag", name = "enabled", havingValue = "true")
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseServiceImpl.class);

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public KnowledgeBaseServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    @Override
    public KnowledgeBase create(KnowledgeBase knowledgeBase) {
        knowledgeBaseMapper.insert(knowledgeBase);
        log.info("创建知识库成功，baseCode: {}", knowledgeBase.getBaseCode());
        return knowledgeBase;
    }

    @Override
    public KnowledgeBase update(KnowledgeBase knowledgeBase) {
        if (knowledgeBase.getId() == null || knowledgeBaseMapper.selectById(knowledgeBase.getId()) == null) {
            throw new ServiceException(404, "知识库不存在");
        }
        knowledgeBaseMapper.updateById(knowledgeBase);
        log.info("更新知识库成功，baseCode: {}", knowledgeBase.getBaseCode());
        return knowledgeBase;
    }

    @Override
    public void delete(String baseCode) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectByBaseCode(baseCode);
        if (knowledgeBase == null) {
            throw new ServiceException(404, "知识库不存在");
        }
        knowledgeBaseMapper.deleteById(knowledgeBase.getId());
        log.info("删除知识库成功，baseCode: {}", baseCode);
    }

    @Override
    public Optional<KnowledgeBase> findById(Long id) {
        return Optional.ofNullable(knowledgeBaseMapper.selectById(id));
    }

    @Override
    public Optional<KnowledgeBase> findByCode(String baseCode) {
        return Optional.ofNullable(knowledgeBaseMapper.selectByBaseCode(baseCode));
    }

    @Override
    public List<KnowledgeBase> findAll() {
        return knowledgeBaseMapper.selectList(null);
    }

    @Override
    public List<KnowledgeBase> findByAgentCode(String agentCode) {
        return knowledgeBaseMapper.selectByAgentCode(agentCode);
    }
}
