package com.smartcrew.agent.core.experience;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.experience.domain.entity.AgentExperienceHitLog;
import com.smartcrew.agent.api.experience.domain.entity.AgentExperiencePool;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceScopes;
import com.smartcrew.agent.api.experience.domain.model.AgentExperienceToolCodes;
import com.smartcrew.agent.api.experience.domain.query.AgentExperienceHitLogQuery;
import com.smartcrew.agent.api.experience.domain.query.AgentExperiencePoolQuery;
import com.smartcrew.agent.api.experience.domain.vo.AgentExperienceHitLogVo;
import com.smartcrew.agent.api.experience.domain.vo.AgentExperienceRecallVo;
import com.smartcrew.agent.api.experience.mapper.AgentExperienceHitLogMapper;
import com.smartcrew.agent.api.experience.mapper.AgentExperiencePoolMapper;
import com.smartcrew.agent.api.experience.service.AgentExperienceService;
import com.smartcrew.agent.api.rag.service.EmbeddingService;
import com.smartcrew.agent.api.rag.service.VectorStoreService;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.page.TableDataInfo;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 经验服务实现。
 */
@Service
@RequiredArgsConstructor
public class AgentExperienceServiceImpl implements AgentExperienceService {

    private static final String VECTOR_NAMESPACE = "agent_experience_pool";
    private static final int DEFAULT_RECALL_LIMIT = 8;

    private final AgentExperiencePoolMapper agentExperiencePoolMapper;
    private final AgentExperienceHitLogMapper agentExperienceHitLogMapper;
    private final ObjectProvider<EmbeddingService> embeddingServiceProvider;
    private final ObjectProvider<VectorStoreService> vectorStoreServiceProvider;

    /**
     * 根据查询条件召回全局经验列表。
     * 默认会尝试使用向量服务对结果进行语义重排序，若不可用则回退到数据库默认排序。
     *
     * @param query 经验池查询条件
     * @return 经验召回分页数据
     */
    @Override
    public TableDataInfo<AgentExperienceRecallVo> recallGlobalExperiences(AgentExperiencePoolQuery query) {
        AgentExperiencePoolQuery safeQuery = query == null ? new AgentExperiencePoolQuery() : query;
        Page<AgentExperiencePool> page = agentExperiencePoolMapper.selectPage(
                new Page<>(resolvePageNum(safeQuery), resolvePageSize(safeQuery)),
                buildRecallWrapper(safeQuery)
        );
        List<AgentExperiencePool> orderedRecords = reorderByVectorIfAvailable(safeQuery.getKeyword(), page.getRecords());
        Page<AgentExperienceRecallVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(orderedRecords.stream().map(this::toRecallVo).toList());
        return TableDataInfo.build(result);
    }

    /**
     * 记录成功经验并同步向量索引。
     * 若经验代码已存在则合并更新（新增计数字段累加），否则新增一条经验记录。
     *
     * @param experiencePool 经验实体
     * @return 保存或更新后的经验
     */
    @Override
    @Transactional
    public AgentExperiencePool recordSuccessfulExperience(AgentExperiencePool experiencePool) {
        AgentExperiencePool safeExperience = normalizeExperience(experiencePool);
        AgentExperiencePool existing = agentExperiencePoolMapper.selectOne(Wrappers.lambdaQuery(AgentExperiencePool.class)
                .eq(AgentExperiencePool::getExperienceCode, safeExperience.getExperienceCode())
                .last("limit 1"));
        if (existing == null) {
            safeExperience.setHitCount(resolvePositiveCount(safeExperience.getHitCount(), 1));
            safeExperience.setSuccessCount(resolvePositiveCount(safeExperience.getSuccessCount(), 1));
            safeExperience.setLastUsedAt(LocalDateTime.now());
            agentExperiencePoolMapper.insert(safeExperience);
            syncVectorIndex(safeExperience);
            return safeExperience;
        }

        mergeExperience(existing, safeExperience);
        agentExperiencePoolMapper.updateById(existing);
        syncVectorIndex(existing);
        return existing;
    }

    /**
     * 记录经验命中日志。
     *
     * @param hitLog 经验命中日志实体
     */
    @Override
    @Transactional
    public void recordExperienceHit(AgentExperienceHitLog hitLog) {
        if (hitLog == null) {
            throw new ServiceException(400, "经验命中日志不能为空");
        }
        if (StringUtils.isBlank(hitLog.getTraceId()) || StringUtils.isBlank(hitLog.getExperienceCode())) {
            throw new ServiceException(400, "经验命中日志缺少 traceId 或 experienceCode");
        }
        if (StringUtils.isBlank(hitLog.getAppliedStage())) {
            hitLog.setAppliedStage("RECALL");
        }
        if (hitLog.getSuccessFlag() == null) {
            hitLog.setSuccessFlag(Boolean.FALSE);
        }
        agentExperienceHitLogMapper.insert(hitLog);
    }

    /**
     * 分页查询经验命中日志列表。
     *
     * @param query 查询条件
     * @return 命中日志分页数据
     */
    @Override
    public TableDataInfo<AgentExperienceHitLogVo> listExperienceHits(AgentExperienceHitLogQuery query) {
        AgentExperienceHitLogQuery safeQuery = query == null ? new AgentExperienceHitLogQuery() : query;
        LambdaQueryWrapper<AgentExperienceHitLog> wrapper = Wrappers.lambdaQuery(AgentExperienceHitLog.class);
        if (StringUtils.isNotBlank(safeQuery.getTraceId())) {
            wrapper.eq(AgentExperienceHitLog::getTraceId, safeQuery.getTraceId().trim());
        }
        if (StringUtils.isNotBlank(safeQuery.getExperienceCode())) {
            wrapper.eq(AgentExperienceHitLog::getExperienceCode, safeQuery.getExperienceCode().trim());
        }
        if (StringUtils.isNotBlank(safeQuery.getAgentCode())) {
            wrapper.eq(AgentExperienceHitLog::getAgentCode, safeQuery.getAgentCode().trim());
        }
        if (safeQuery.getSuccessFlag() != null) {
            wrapper.eq(AgentExperienceHitLog::getSuccessFlag, safeQuery.getSuccessFlag());
        }
        wrapper.orderByDesc(AgentExperienceHitLog::getCreateTime)
                .orderByDesc(AgentExperienceHitLog::getId);
        Page<AgentExperienceHitLog> page = agentExperienceHitLogMapper.selectPage(
                safeQuery.hasPaging() ? safeQuery.build() : new Page<>(1, DEFAULT_RECALL_LIMIT),
                wrapper
        );
        Page<AgentExperienceHitLogVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toHitVo).toList());
        return TableDataInfo.build(result);
    }

    /**
     * 根据ID查询经验池。
     *
     * @param id 经验ID
     * @return 经验实体（可选）
     */
    @Override
    public Optional<AgentExperiencePool> findExperienceById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentExperiencePoolMapper.selectById(id));
    }

    /* 构建召回查询条件。 */
    private LambdaQueryWrapper<AgentExperiencePool> buildRecallWrapper(AgentExperiencePoolQuery query) {
        LambdaQueryWrapper<AgentExperiencePool> wrapper = Wrappers.lambdaQuery(AgentExperiencePool.class);
        String scopeType = StringUtils.isNotBlank(query.getScopeType()) ? query.getScopeType().trim() : AgentExperienceScopes.GLOBAL;
        wrapper.eq(AgentExperiencePool::getScopeType, scopeType);
        if (StringUtils.isNotBlank(query.getExperienceType())) {
            wrapper.eq(AgentExperiencePool::getExperienceType, query.getExperienceType().trim());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(AgentExperiencePool::getEnabled, query.getEnabled());
        }
        if (StringUtils.isNotBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(AgentExperiencePool::getExperienceCode, keyword)
                    .or()
                    .like(AgentExperiencePool::getTitle, keyword)
                    .or()
                    .like(AgentExperiencePool::getTriggerPattern, keyword)
                    .or()
                    .like(AgentExperiencePool::getStrategySummary, keyword)
                    .or()
                    .like(AgentExperiencePool::getSuccessSample, keyword)
                    .or()
                    .like(AgentExperiencePool::getFailureAvoidance, keyword)
                    .or()
                    .like(AgentExperiencePool::getRecommendedAgentCode, keyword)
                    .or()
                    .like(AgentExperiencePool::getRecommendedToolCodesJson, keyword)
                    .or()
                    .like(AgentExperiencePool::getSourceTraceId, keyword));
        }
        wrapper.orderByDesc(AgentExperiencePool::getQualityScore)
                .orderByDesc(AgentExperiencePool::getSuccessCount)
                .orderByDesc(AgentExperiencePool::getHitCount)
                .orderByDesc(AgentExperiencePool::getLastUsedAt)
                .orderByDesc(AgentExperiencePool::getId);
        return wrapper;
    }

    /* 利用向量服务对数据库召回结果进行语义重排序。 */
    private List<AgentExperiencePool> reorderByVectorIfAvailable(String keyword, List<AgentExperiencePool> records) {
        if (records == null || records.size() <= 1 || StringUtils.isBlank(keyword)) {
            return records == null ? List.of() : records;
        }
        EmbeddingService embeddingService = embeddingServiceProvider.getIfAvailable();
        VectorStoreService vectorStoreService = vectorStoreServiceProvider.getIfAvailable();
        if (embeddingService == null || vectorStoreService == null) {
            return records;
        }
        try {
            Embedding queryEmbedding = embeddingService.embed(keyword.trim());
            List<EmbeddingMatch<TextSegment>> matches = vectorStoreService.search(VECTOR_NAMESPACE, queryEmbedding, records.size());
            Map<String, AgentExperiencePool> poolMap = records.stream().collect(Collectors.toMap(
                    AgentExperiencePool::getExperienceCode,
                    Function.identity(),
                    (left, right) -> left,
                    LinkedHashMap::new
            ));
            List<AgentExperiencePool> reordered = new ArrayList<>(records.size());
            for (EmbeddingMatch<TextSegment> match : matches) {
                String experienceCode = extractExperienceCode(match);
                if (StringUtils.isBlank(experienceCode)) {
                    continue;
                }
                AgentExperiencePool matched = poolMap.remove(experienceCode);
                if (matched != null) {
                    reordered.add(matched);
                }
            }
            reordered.addAll(poolMap.values());
            return reordered;
        } catch (Exception ignored) {
            return records;
        }
    }

    /* 从向量匹配结果的元数据中提取 experienceCode。 */
    private String extractExperienceCode(EmbeddingMatch<TextSegment> match) {
        if (match == null || match.embedded() == null || match.embedded().metadata() == null) {
            return "";
        }
        Object experienceCode = match.embedded().metadata().toMap().get("experienceCode");
        return experienceCode == null ? "" : String.valueOf(experienceCode);
    }

    /* 同步经验数据到向量索引。 */
    private void syncVectorIndex(AgentExperiencePool experiencePool) {
        EmbeddingService embeddingService = embeddingServiceProvider.getIfAvailable();
        VectorStoreService vectorStoreService = vectorStoreServiceProvider.getIfAvailable();
        if (embeddingService == null || vectorStoreService == null || experiencePool == null
                || StringUtils.isBlank(experiencePool.getExperienceCode())) {
            return;
        }
        String document = buildExperienceDocument(experiencePool);
        if (StringUtils.isBlank(document)) {
            return;
        }
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("experienceCode", experiencePool.getExperienceCode());
            metadata.put("scopeType", experiencePool.getScopeType());
            metadata.put("experienceType", experiencePool.getExperienceType());
            metadata.put("title", experiencePool.getTitle());
            metadata.put("sourceTraceId", experiencePool.getSourceTraceId());
            vectorStoreService.add(VECTOR_NAMESPACE, embeddingService.embed(document), TextSegment.from(document, Metadata.from(metadata)));
        } catch (Exception ignored) {
            // 向量索引失败不影响 MySQL 权威存储。
        }
    }

    /* 构建用于向量嵌入的经验文本。 */
    private String buildExperienceDocument(AgentExperiencePool experiencePool) {
        List<String> sections = new ArrayList<>();
        appendSection(sections, experiencePool.getExperienceCode());
        appendSection(sections, experiencePool.getTitle());
        appendSection(sections, experiencePool.getTriggerPattern());
        appendSection(sections, experiencePool.getStrategySummary());
        appendSection(sections, experiencePool.getSuccessSample());
        appendSection(sections, experiencePool.getFailureAvoidance());
        appendSection(sections, experiencePool.getRecommendedAgentCode());
        appendSection(sections, experiencePool.getRecommendedToolCodesJson());
        return String.join("\n", sections);
    }

    /* 向文档列表中追加非空字段。 */
    private void appendSection(List<String> sections, String value) {
        if (StringUtils.isNotBlank(value)) {
            sections.add(value.trim());
        }
    }

    /* 将源经验数据合并到目标经验。 */
    private void mergeExperience(AgentExperiencePool target, AgentExperiencePool source) {
        if (StringUtils.isNotBlank(source.getScopeType())) {
            target.setScopeType(source.getScopeType().trim());
        }
        if (StringUtils.isNotBlank(source.getExperienceType())) {
            target.setExperienceType(source.getExperienceType().trim());
        }
        if (StringUtils.isNotBlank(source.getTitle())) {
            target.setTitle(source.getTitle().trim());
        }
        if (StringUtils.isNotBlank(source.getTriggerPattern())) {
            target.setTriggerPattern(source.getTriggerPattern().trim());
        }
        if (StringUtils.isNotBlank(source.getStrategySummary())) {
            target.setStrategySummary(source.getStrategySummary().trim());
        }
        if (StringUtils.isNotBlank(source.getRecommendedAgentCode())) {
            target.setRecommendedAgentCode(source.getRecommendedAgentCode().trim());
        }
        if (StringUtils.isNotBlank(source.getRecommendedToolCodesJson())) {
            target.setRecommendedToolCodesJson(source.getRecommendedToolCodesJson().trim());
        }
        if (StringUtils.isNotBlank(source.getSuccessSample())) {
            target.setSuccessSample(source.getSuccessSample().trim());
        }
        if (StringUtils.isNotBlank(source.getFailureAvoidance())) {
            target.setFailureAvoidance(source.getFailureAvoidance().trim());
        }
        if (source.getQualityScore() != null) {
            target.setQualityScore(source.getQualityScore());
        }
        target.setHitCount(resolvePositiveCount(target.getHitCount(), 0) + resolvePositiveCount(source.getHitCount(), 1));
        target.setSuccessCount(resolvePositiveCount(target.getSuccessCount(), 0) + resolvePositiveCount(source.getSuccessCount(), 1));
        target.setLastUsedAt(LocalDateTime.now());
        if (source.getEnabled() != null) {
            target.setEnabled(source.getEnabled());
        }
        if (StringUtils.isNotBlank(source.getSourceTraceId())) {
            target.setSourceTraceId(source.getSourceTraceId().trim());
        }
    }

    /* 规范化经验实体，确保必填字段有默认值。 */
    private AgentExperiencePool normalizeExperience(AgentExperiencePool experiencePool) {
        if (experiencePool == null) {
            throw new ServiceException(400, "经验不能为空");
        }
        if (StringUtils.isBlank(experiencePool.getExperienceCode())) {
            throw new ServiceException(400, "经验代码不能为空");
        }
        AgentExperiencePool normalized = new AgentExperiencePool();
        normalized.setExperienceCode(experiencePool.getExperienceCode().trim());
        normalized.setScopeType(StringUtils.isNotBlank(experiencePool.getScopeType())
                ? experiencePool.getScopeType().trim()
                : AgentExperienceScopes.GLOBAL);
        normalized.setExperienceType(StringUtils.isNotBlank(experiencePool.getExperienceType())
                ? experiencePool.getExperienceType().trim()
                : "COLLABORATION_STRATEGY");
        normalized.setTitle(StringUtils.isNotBlank(experiencePool.getTitle())
                ? experiencePool.getTitle().trim()
                : experiencePool.getExperienceCode().trim());
        normalized.setTriggerPattern(defaultString(experiencePool.getTriggerPattern()));
        normalized.setStrategySummary(defaultString(experiencePool.getStrategySummary()));
        normalized.setRecommendedAgentCode(defaultString(experiencePool.getRecommendedAgentCode()));
        normalized.setRecommendedToolCodesJson(StringUtils.isNotBlank(experiencePool.getRecommendedToolCodesJson())
                ? experiencePool.getRecommendedToolCodesJson().trim()
                : AgentExperienceToolCodes.toJson(List.of()));
        normalized.setSuccessSample(defaultString(experiencePool.getSuccessSample()));
        normalized.setFailureAvoidance(defaultString(experiencePool.getFailureAvoidance()));
        normalized.setQualityScore(experiencePool.getQualityScore() == null ? BigDecimal.ZERO : experiencePool.getQualityScore());
        normalized.setHitCount(experiencePool.getHitCount());
        normalized.setSuccessCount(experiencePool.getSuccessCount());
        normalized.setLastUsedAt(experiencePool.getLastUsedAt());
        normalized.setEnabled(experiencePool.getEnabled() == null ? Boolean.TRUE : experiencePool.getEnabled());
        normalized.setSourceTraceId(defaultString(experiencePool.getSourceTraceId()));
        return normalized;
    }

    /* 转换为经验召回视图。 */
    private AgentExperienceRecallVo toRecallVo(AgentExperiencePool experiencePool) {
        AgentExperienceRecallVo vo = new AgentExperienceRecallVo();
        vo.setExperienceCode(experiencePool.getExperienceCode());
        vo.setScopeType(experiencePool.getScopeType());
        vo.setExperienceType(experiencePool.getExperienceType());
        vo.setTitle(experiencePool.getTitle());
        vo.setTriggerPattern(experiencePool.getTriggerPattern());
        vo.setStrategySummary(experiencePool.getStrategySummary());
        vo.setRecommendedAgentCode(experiencePool.getRecommendedAgentCode());
        vo.setRecommendedToolCodes(parseToolCodes(experiencePool.getRecommendedToolCodesJson()));
        vo.setSuccessSample(experiencePool.getSuccessSample());
        vo.setFailureAvoidance(experiencePool.getFailureAvoidance());
        vo.setQualityScore(experiencePool.getQualityScore());
        vo.setHitCount(experiencePool.getHitCount());
        vo.setSuccessCount(experiencePool.getSuccessCount());
        vo.setLastUsedAt(experiencePool.getLastUsedAt());
        vo.setEnabled(experiencePool.getEnabled());
        return vo;
    }

    /* 转换为命中日志视图。 */
    private AgentExperienceHitLogVo toHitVo(AgentExperienceHitLog hitLog) {
        AgentExperienceHitLogVo vo = new AgentExperienceHitLogVo();
        vo.setTraceId(hitLog.getTraceId());
        vo.setExperienceCode(hitLog.getExperienceCode());
        vo.setAgentCode(hitLog.getAgentCode());
        vo.setAppliedStage(hitLog.getAppliedStage());
        vo.setAppliedSnapshot(hitLog.getAppliedSnapshot());
        vo.setSuccessFlag(hitLog.getSuccessFlag());
        vo.setCreateTime(hitLog.getCreateTime());
        return vo;
    }

    /* 解析工具编码 JSON 为列表。 */
    private List<String> parseToolCodes(String json) {
        try {
            return AgentExperienceToolCodes.fromJson(json);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private int resolvePageNum(AgentExperiencePoolQuery query) {
        return query.getPageNum() == null ? 1 : Math.max(query.getPageNum(), 1);
    }

    private int resolvePageSize(AgentExperiencePoolQuery query) {
        return query.getPageSize() == null ? DEFAULT_RECALL_LIMIT : Math.max(query.getPageSize(), 1);
    }

    private int resolvePositiveCount(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value.trim();
    }
}
