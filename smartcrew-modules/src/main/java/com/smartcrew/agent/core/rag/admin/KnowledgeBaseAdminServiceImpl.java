package com.smartcrew.agent.core.rag.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcrew.agent.api.admin.domain.query.DocumentChunkQuery;
import com.smartcrew.agent.api.admin.domain.query.KnowledgeBaseQuery;
import com.smartcrew.agent.api.admin.domain.query.KnowledgeDocumentQuery;
import com.smartcrew.agent.api.admin.domain.request.KnowledgeBaseAgentBindingUpdateRequest;
import com.smartcrew.agent.api.admin.domain.request.KnowledgeBaseSaveRequest;
import com.smartcrew.agent.api.admin.domain.vo.DocumentChunkAdminVo;
import com.smartcrew.agent.api.admin.domain.vo.KnowledgeBaseAdminVo;
import com.smartcrew.agent.api.admin.domain.vo.KnowledgeBaseAgentBindingVo;
import com.smartcrew.agent.api.admin.domain.vo.KnowledgeBaseAgentOptionVo;
import com.smartcrew.agent.api.admin.domain.vo.KnowledgeDocumentAdminVo;
import com.smartcrew.agent.api.admin.service.KnowledgeBaseAdminService;
import com.smartcrew.agent.api.agent.domain.vo.AgentDefinitionVo;
import com.smartcrew.agent.api.agent.service.AgentDefinitionService;
import com.smartcrew.agent.api.rag.domain.entity.AgentKnowledgeBinding;
import com.smartcrew.agent.api.rag.domain.entity.DocumentChunk;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeBase;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeDocument;
import com.smartcrew.agent.api.rag.mapper.AgentKnowledgeBindingMapper;
import com.smartcrew.agent.api.rag.mapper.DocumentChunkMapper;
import com.smartcrew.agent.api.rag.mapper.KnowledgeBaseMapper;
import com.smartcrew.agent.api.rag.mapper.KnowledgeDocumentMapper;
import com.smartcrew.agent.api.rag.service.KnowledgeBaseService;
import com.smartcrew.agent.api.rag.service.KnowledgeDocumentService;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.page.TableDataInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库后台管理应用服务实现。
 */
@Service
@ConditionalOnProperty(prefix = "smartcrew.rag", name = "enabled", havingValue = "true")
public class KnowledgeBaseAdminServiceImpl implements KnowledgeBaseAdminService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseAdminServiceImpl.class);

    private static final Set<String> ACTIVE_STATUSES = Set.of("pending", "processing");
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PROCESSING = "processing";

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final AgentKnowledgeBindingMapper agentKnowledgeBindingMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final AgentDefinitionService agentDefinitionService;
    private final SmartCrewProperties properties;
    private final TaskExecutor ragDocumentTaskExecutor;

    public KnowledgeBaseAdminServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper,
                                         KnowledgeDocumentMapper knowledgeDocumentMapper,
                                         DocumentChunkMapper documentChunkMapper,
                                         AgentKnowledgeBindingMapper agentKnowledgeBindingMapper,
                                         KnowledgeBaseService knowledgeBaseService,
                                         KnowledgeDocumentService knowledgeDocumentService,
                                         AgentDefinitionService agentDefinitionService,
                                         SmartCrewProperties properties,
                                         @Qualifier("ragDocumentTaskExecutor") TaskExecutor ragDocumentTaskExecutor) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.documentChunkMapper = documentChunkMapper;
        this.agentKnowledgeBindingMapper = agentKnowledgeBindingMapper;
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.agentDefinitionService = agentDefinitionService;
        this.properties = properties;
        this.ragDocumentTaskExecutor = ragDocumentTaskExecutor;
    }

    @Override
    public TableDataInfo<KnowledgeBaseAdminVo> listKnowledgeBases(KnowledgeBaseQuery query) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(KnowledgeBase::getBaseCode, keyword)
                    .or()
                    .like(KnowledgeBase::getBaseName, keyword)
                    .or()
                    .like(KnowledgeBase::getDescription, keyword));
        }
        if (query.getEnabled() != null) {
            wrapper.eq(KnowledgeBase::getEnabled, query.getEnabled());
        }
        wrapper.orderByDesc(KnowledgeBase::getUpdateTime)
                .orderByDesc(KnowledgeBase::getId);

        if (query.hasPaging()) {
            Page<KnowledgeBase> page = knowledgeBaseMapper.selectPage(query.build(), wrapper);
            Page<KnowledgeBaseAdminVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            result.setRecords(enrichKnowledgeBases(page.getRecords()));
            return TableDataInfo.build(result);
        }
        return TableDataInfo.build(enrichKnowledgeBases(knowledgeBaseMapper.selectList(wrapper)));
    }

    @Override
    public KnowledgeBaseAdminVo getKnowledgeBase(String baseCode) {
        KnowledgeBase knowledgeBase = requireBase(baseCode);
        return enrichKnowledgeBases(List.of(knowledgeBase)).get(0);
    }

    @Override
    @Transactional
    public KnowledgeBaseAdminVo createKnowledgeBase(KnowledgeBaseSaveRequest request) {
        String normalizedBaseCode = normalizeBaseCode(request.getBaseCode());
        if (knowledgeBaseService.findByCode(normalizedBaseCode).isPresent()) {
            throw new ServiceException(400, "知识库编码已存在");
        }

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setBaseCode(normalizedBaseCode);
        knowledgeBase.setBaseName(request.getBaseName().trim());
        knowledgeBase.setDescription(trimToEmpty(request.getDescription()));
        knowledgeBase.setEmbeddingModel(resolveEmbeddingModel(request.getEmbeddingModel()));
        knowledgeBase.setCollectionName(resolveCollectionName(request.getCollectionName(), normalizedBaseCode));
        knowledgeBase.setEnabled(request.getEnabled() == null || request.getEnabled());
        knowledgeBaseService.create(knowledgeBase);
        return getKnowledgeBase(normalizedBaseCode);
    }

    @Override
    @Transactional
    public KnowledgeBaseAdminVo updateKnowledgeBase(String baseCode, KnowledgeBaseSaveRequest request) {
        KnowledgeBase existing = requireBase(baseCode);
        String newBaseCode = normalizeBaseCode(request.getBaseCode());
        boolean baseCodeChanged = !Objects.equals(existing.getBaseCode(), newBaseCode);
        boolean hasDocuments = countDocuments(existing.getId()) > 0;
        boolean hasCompletedDocuments = countCompletedDocuments(existing.getId()) > 0;

        if (baseCodeChanged && hasDocuments) {
            throw new ServiceException(400, "知识库已有文档，暂不允许修改编码");
        }
        if (baseCodeChanged && knowledgeBaseService.findByCode(newBaseCode)
                .filter(item -> !Objects.equals(item.getId(), existing.getId()))
                .isPresent()) {
            throw new ServiceException(400, "知识库编码已存在");
        }

        String targetCollectionName = resolveCollectionName(request.getCollectionName(), newBaseCode);
        if (hasDocuments && !Objects.equals(existing.getCollectionName(), targetCollectionName)) {
            throw new ServiceException(400, "知识库已有文档，暂不允许修改向量命名空间");
        }

        String targetEmbeddingModel = resolveEmbeddingModel(request.getEmbeddingModel());
        if (hasCompletedDocuments && !Objects.equals(existing.getEmbeddingModel(), targetEmbeddingModel)) {
            throw new ServiceException(400, "知识库已有已处理文档，暂不允许修改嵌入模型");
        }

        existing.setBaseCode(newBaseCode);
        existing.setBaseName(request.getBaseName().trim());
        existing.setDescription(trimToEmpty(request.getDescription()));
        existing.setEmbeddingModel(targetEmbeddingModel);
        existing.setCollectionName(targetCollectionName);
        existing.setEnabled(request.getEnabled() == null || request.getEnabled());
        knowledgeBaseService.update(existing);

        if (baseCodeChanged) {
            LambdaUpdateWrapper<AgentKnowledgeBinding> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(AgentKnowledgeBinding::getBaseCode, baseCode)
                    .set(AgentKnowledgeBinding::getBaseCode, newBaseCode);
            agentKnowledgeBindingMapper.update(null, updateWrapper);
        }
        return getKnowledgeBase(newBaseCode);
    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(String baseCode) {
        KnowledgeBase knowledgeBase = requireBase(baseCode);
        if (countDocuments(knowledgeBase.getId()) > 0) {
            throw new ServiceException(400, "知识库下仍存在文档，无法删除");
        }
        agentKnowledgeBindingMapper.delete(new LambdaQueryWrapper<AgentKnowledgeBinding>()
                .eq(AgentKnowledgeBinding::getBaseCode, knowledgeBase.getBaseCode()));
        knowledgeBaseService.delete(knowledgeBase.getBaseCode());
    }

    @Override
    public TableDataInfo<KnowledgeDocumentAdminVo> listDocuments(String baseCode, KnowledgeDocumentQuery query) {
        KnowledgeBase knowledgeBase = requireBase(baseCode);
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDocument::getBaseId, knowledgeBase.getId());
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(KnowledgeDocument::getStatus, query.getStatus().trim());
        }
        if (StringUtils.isNotBlank(query.getFileType())) {
            wrapper.eq(KnowledgeDocument::getFileType, query.getFileType().trim().toLowerCase(Locale.ROOT));
        }
        if (StringUtils.isNotBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(KnowledgeDocument::getDocumentCode, keyword)
                    .or()
                    .like(KnowledgeDocument::getDocumentName, keyword));
        }
        wrapper.orderByDesc(KnowledgeDocument::getUpdateTime)
                .orderByDesc(KnowledgeDocument::getId);

        if (query.hasPaging()) {
            Page<KnowledgeDocument> page = knowledgeDocumentMapper.selectPage(query.build(), wrapper);
            Page<KnowledgeDocumentAdminVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            result.setRecords(page.getRecords().stream().map(item -> toDocumentVo(knowledgeBase, item)).toList());
            return TableDataInfo.build(result);
        }

        return TableDataInfo.build(knowledgeDocumentMapper.selectList(wrapper).stream()
                .map(item -> toDocumentVo(knowledgeBase, item))
                .toList());
    }

    @Override
    public List<KnowledgeDocumentAdminVo> uploadDocuments(String baseCode, MultipartFile[] files) {
        KnowledgeBase knowledgeBase = requireBase(baseCode);
        if (files == null || files.length == 0) {
            throw new ServiceException(400, "请至少选择一个文档文件");
        }

        List<KnowledgeDocumentAdminVo> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String originalFilename = StringUtils.isBlank(file.getOriginalFilename()) ? file.getName() : file.getOriginalFilename();
            try (InputStream inputStream = file.getInputStream()) {
                KnowledgeDocument document = knowledgeDocumentService.upload(
                        knowledgeBase.getId(),
                        originalFilename,
                        inputStream,
                        file.getSize()
                );
                dispatchDocumentProcessing(document.getId());
                result.add(toDocumentVo(knowledgeBase, requireDocument(document.getId())));
            } catch (IOException exception) {
                throw new ServiceException(500, "读取上传文件失败");
            }
        }

        if (result.isEmpty()) {
            throw new ServiceException(400, "未检测到可上传的有效文件");
        }
        return result;
    }

    @Override
    public KnowledgeDocumentAdminVo reprocessDocument(String baseCode, String documentCode) {
        KnowledgeBase knowledgeBase = requireBase(baseCode);
        KnowledgeDocument document = requireDocument(baseCode, documentCode);
        if (STATUS_PROCESSING.equals(document.getStatus())) {
            throw new ServiceException(400, "文档正在处理中，请稍后再试");
        }
        document.setStatus(STATUS_PENDING);
        document.setErrorMessage(null);
        document.setChunkCount(0);
        knowledgeDocumentMapper.updateById(document);
        dispatchDocumentProcessing(document.getId());
        return toDocumentVo(knowledgeBase, requireDocument(document.getId()));
    }

    @Override
    public void deleteDocument(String baseCode, String documentCode) {
        requireBase(baseCode);
        KnowledgeDocument document = requireDocument(baseCode, documentCode);
        if (STATUS_PROCESSING.equals(document.getStatus())) {
            throw new ServiceException(400, "文档正在处理中，暂不允许删除");
        }
        knowledgeDocumentService.deleteDocument(document.getId());
    }

    @Override
    public TableDataInfo<DocumentChunkAdminVo> listChunks(String baseCode, String documentCode, DocumentChunkQuery query) {
        requireBase(baseCode);
        KnowledgeDocument document = requireDocument(baseCode, documentCode);
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentChunk::getDocumentId, document.getId());
        if (StringUtils.isNotBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(DocumentChunk::getContent, keyword)
                    .or()
                    .like(DocumentChunk::getVectorId, keyword)
                    .or()
                    .like(DocumentChunk::getMetadata, keyword));
        }
        wrapper.orderByAsc(DocumentChunk::getChunkIndex)
                .orderByAsc(DocumentChunk::getId);

        if (query.hasPaging()) {
            Page<DocumentChunk> page = documentChunkMapper.selectPage(query.build(), wrapper);
            Page<DocumentChunkAdminVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            result.setRecords(page.getRecords().stream().map(item -> toChunkVo(document, item)).toList());
            return TableDataInfo.build(result);
        }

        return TableDataInfo.build(documentChunkMapper.selectList(wrapper).stream()
                .map(item -> toChunkVo(document, item))
                .toList());
    }

    @Override
    public KnowledgeBaseAgentBindingVo getAgentBindings(String baseCode) {
        KnowledgeBase knowledgeBase = requireBase(baseCode);
        Set<String> boundCodes = agentKnowledgeBindingMapper.selectList(new LambdaQueryWrapper<AgentKnowledgeBinding>()
                        .eq(AgentKnowledgeBinding::getBaseCode, knowledgeBase.getBaseCode()))
                .stream()
                .map(AgentKnowledgeBinding::getAgentCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        KnowledgeBaseAgentBindingVo result = new KnowledgeBaseAgentBindingVo();
        result.setBaseCode(knowledgeBase.getBaseCode());
        List<AgentDefinitionVo> agents = agentDefinitionService.listAll().stream()
                .sorted(Comparator.comparing(AgentDefinitionVo::getAgentCode))
                .toList();
        for (AgentDefinitionVo agent : agents) {
            KnowledgeBaseAgentOptionVo option = toAgentOption(agent);
            if (boundCodes.contains(agent.getAgentCode())) {
                result.getBoundAgents().add(option);
            } else {
                result.getAvailableAgents().add(option);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public KnowledgeBaseAgentBindingVo replaceAgentBindings(String baseCode, KnowledgeBaseAgentBindingUpdateRequest request) {
        KnowledgeBase knowledgeBase = requireBase(baseCode);
        Set<String> validAgentCodes = agentDefinitionService.listAll().stream()
                .map(AgentDefinitionVo::getAgentCode)
                .collect(Collectors.toSet());
        List<String> targetCodes = request.getAgentCodes() == null
                ? Collections.emptyList()
                : request.getAgentCodes().stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        for (String agentCode : targetCodes) {
            if (!validAgentCodes.contains(agentCode)) {
                throw new ServiceException(400, "存在无效的 Agent 编码: " + agentCode);
            }
        }

        agentKnowledgeBindingMapper.delete(new LambdaQueryWrapper<AgentKnowledgeBinding>()
                .eq(AgentKnowledgeBinding::getBaseCode, knowledgeBase.getBaseCode()));
        for (String agentCode : targetCodes) {
            AgentKnowledgeBinding binding = new AgentKnowledgeBinding();
            binding.setAgentCode(agentCode);
            binding.setBaseCode(knowledgeBase.getBaseCode());
            agentKnowledgeBindingMapper.insert(binding);
        }
        return getAgentBindings(knowledgeBase.getBaseCode());
    }

    /* 提交文档异步处理任务。 */
    private void dispatchDocumentProcessing(Long documentId) {
        ragDocumentTaskExecutor.execute(() -> {
            try {
                knowledgeDocumentService.processDocument(documentId);
            } catch (Exception ex) {
                log.warn("异步处理知识文档失败，documentId: {}", documentId, ex);
            }
        });
    }

    /* 为知识库列表补充统计信息。 */
    private List<KnowledgeBaseAdminVo> enrichKnowledgeBases(List<KnowledgeBase> bases) {
        if (bases.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, KnowledgeBaseStats> statsMap = buildStats(bases);
        return bases.stream()
                .map(base -> toKnowledgeBaseVo(base, statsMap.getOrDefault(base.getId(), new KnowledgeBaseStats())))
                .toList();
    }

    /* 构建知识库统计结果。 */
    private Map<Long, KnowledgeBaseStats> buildStats(List<KnowledgeBase> bases) {
        List<Long> baseIds = bases.stream().map(KnowledgeBase::getId).toList();
        List<String> baseCodes = bases.stream().map(KnowledgeBase::getBaseCode).toList();
        List<KnowledgeDocument> documents = knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                .in(KnowledgeDocument::getBaseId, baseIds));
        Map<String, Long> agentCounts = agentKnowledgeBindingMapper.selectList(new LambdaQueryWrapper<AgentKnowledgeBinding>()
                        .in(AgentKnowledgeBinding::getBaseCode, baseCodes))
                .stream()
                .collect(Collectors.groupingBy(AgentKnowledgeBinding::getBaseCode, LinkedHashMap::new, Collectors.counting()));

        Map<Long, KnowledgeBaseStats> result = new LinkedHashMap<>();
        for (KnowledgeBase base : bases) {
            KnowledgeBaseStats stats = new KnowledgeBaseStats();
            stats.agentCount = agentCounts.getOrDefault(base.getBaseCode(), 0L);
            result.put(base.getId(), stats);
        }
        for (KnowledgeDocument document : documents) {
            KnowledgeBaseStats stats = result.computeIfAbsent(document.getBaseId(), key -> new KnowledgeBaseStats());
            stats.documentCount++;
            stats.chunkCount += document.getChunkCount() == null ? 0 : document.getChunkCount();
            if (ACTIVE_STATUSES.contains(document.getStatus())) {
                stats.processingDocumentCount++;
            }
            if (STATUS_COMPLETED.equals(document.getStatus())) {
                stats.completedDocumentCount++;
            }
        }
        return result;
    }

    /* 转换知识库后台视图对象。 */
    private KnowledgeBaseAdminVo toKnowledgeBaseVo(KnowledgeBase base, KnowledgeBaseStats stats) {
        KnowledgeBaseAdminVo vo = new KnowledgeBaseAdminVo();
        vo.setId(base.getId());
        vo.setBaseCode(base.getBaseCode());
        vo.setBaseName(base.getBaseName());
        vo.setDescription(base.getDescription());
        vo.setEmbeddingModel(base.getEmbeddingModel());
        vo.setCollectionName(base.getCollectionName());
        vo.setEnabled(base.getEnabled());
        vo.setDocumentCount(stats.documentCount);
        vo.setChunkCount(stats.chunkCount);
        vo.setAgentCount(stats.agentCount);
        vo.setProcessingDocumentCount(stats.processingDocumentCount);
        vo.setHasDocuments(stats.documentCount > 0);
        vo.setCollectionNameEditable(stats.documentCount == 0);
        vo.setEmbeddingModelEditable(stats.completedDocumentCount == 0);
        vo.setCreateTime(base.getCreateTime());
        vo.setUpdateTime(base.getUpdateTime());
        return vo;
    }

    /* 转换知识文档后台视图对象。 */
    private KnowledgeDocumentAdminVo toDocumentVo(KnowledgeBase base, KnowledgeDocument document) {
        KnowledgeDocumentAdminVo vo = new KnowledgeDocumentAdminVo();
        vo.setId(document.getId());
        vo.setBaseId(document.getBaseId());
        vo.setBaseCode(base.getBaseCode());
        vo.setDocumentCode(document.getDocumentCode());
        vo.setDocumentName(document.getDocumentName());
        vo.setFilePath(document.getFilePath());
        vo.setFileType(document.getFileType());
        vo.setFileSize(document.getFileSize());
        vo.setStatus(document.getStatus());
        vo.setChunkCount(document.getChunkCount());
        vo.setErrorMessage(document.getErrorMessage());
        vo.setCreateTime(document.getCreateTime());
        vo.setUpdateTime(document.getUpdateTime());
        return vo;
    }

    /* 转换文档切片后台视图对象。 */
    private DocumentChunkAdminVo toChunkVo(KnowledgeDocument document, DocumentChunk chunk) {
        DocumentChunkAdminVo vo = new DocumentChunkAdminVo();
        vo.setId(chunk.getId());
        vo.setDocumentId(chunk.getDocumentId());
        vo.setDocumentCode(document.getDocumentCode());
        vo.setDocumentName(document.getDocumentName());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setContent(chunk.getContent());
        vo.setContentPreview(buildContentPreview(chunk.getContent()));
        vo.setVectorId(chunk.getVectorId());
        vo.setTokenCount(chunk.getTokenCount());
        vo.setMetadata(chunk.getMetadata());
        vo.setCreateTime(chunk.getCreateTime());
        vo.setUpdateTime(chunk.getUpdateTime());
        return vo;
    }

    /* 转换 Agent 选项视图对象。 */
    private KnowledgeBaseAgentOptionVo toAgentOption(AgentDefinitionVo agent) {
        KnowledgeBaseAgentOptionVo vo = new KnowledgeBaseAgentOptionVo();
        vo.setAgentCode(agent.getAgentCode());
        vo.setAgentName(agent.getAgentName());
        vo.setAgentType(agent.getAgentType());
        vo.setEnabled(agent.getEnabled());
        return vo;
    }

    /* 校验并返回知识库。 */
    private KnowledgeBase requireBase(String baseCode) {
        return knowledgeBaseService.findByCode(baseCode)
                .orElseThrow(() -> new ServiceException(404, "知识库不存在"));
    }

    /* 校验并返回知识文档。 */
    private KnowledgeDocument requireDocument(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentMapper.selectById(documentId);
        if (document == null) {
            throw new ServiceException(404, "知识文档不存在");
        }
        return document;
    }

    /* 按知识库编码和文档编码校验文档。 */
    private KnowledgeDocument requireDocument(String baseCode, String documentCode) {
        KnowledgeBase base = requireBase(baseCode);
        KnowledgeDocument document = knowledgeDocumentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getBaseId, base.getId())
                .eq(KnowledgeDocument::getDocumentCode, documentCode)
                .last("limit 1"));
        if (document == null) {
            throw new ServiceException(404, "知识文档不存在");
        }
        return document;
    }

    /* 统计知识库下文档数量。 */
    private long countDocuments(Long baseId) {
        return knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getBaseId, baseId));
    }

    /* 统计知识库下已完成文档数量。 */
    private long countCompletedDocuments(Long baseId) {
        return knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getBaseId, baseId)
                .eq(KnowledgeDocument::getStatus, STATUS_COMPLETED));
    }

    /* 解析知识库使用的嵌入模型。 */
    private String resolveEmbeddingModel(String embeddingModel) {
        if (StringUtils.isNotBlank(embeddingModel)) {
            return embeddingModel.trim();
        }
        String configured = properties.getRag().getEmbedding().getModel();
        return StringUtils.isNotBlank(configured) ? configured.trim() : "text-embedding-v3";
    }

    /* 解析知识库向量命名空间。 */
    private String resolveCollectionName(String collectionName, String baseCode) {
        if (StringUtils.isNotBlank(collectionName)) {
            return collectionName.trim();
        }
        return "kb_" + baseCode.replace('-', '_');
    }

    /* 规范化知识库编码。 */
    private String normalizeBaseCode(String baseCode) {
        if (StringUtils.isBlank(baseCode)) {
            throw new ServiceException(400, "知识库编码不能为空");
        }
        String normalized = baseCode.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-_]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (StringUtils.isBlank(normalized)) {
            throw new ServiceException(400, "知识库编码格式不合法");
        }
        return normalized;
    }

    /* 将空白字符串转换为默认空字符串。 */
    private String trimToEmpty(String value) {
        return StringUtils.isBlank(value) ? "" : value.trim();
    }

    /* 生成切片内容预览。 */
    private String buildContentPreview(String content) {
        if (StringUtils.isBlank(content)) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }

    private static class KnowledgeBaseStats {
        private long documentCount;
        private long chunkCount;
        private long agentCount;
        private long processingDocumentCount;
        private long completedDocumentCount;
    }
}
