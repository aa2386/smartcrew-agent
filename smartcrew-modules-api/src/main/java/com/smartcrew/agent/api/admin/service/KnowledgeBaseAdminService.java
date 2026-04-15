package com.smartcrew.agent.api.admin.service;

import com.smartcrew.agent.api.admin.domain.query.DocumentChunkQuery;
import com.smartcrew.agent.api.admin.domain.query.KnowledgeBaseQuery;
import com.smartcrew.agent.api.admin.domain.query.KnowledgeDocumentQuery;
import com.smartcrew.agent.api.admin.domain.request.KnowledgeBaseAgentBindingUpdateRequest;
import com.smartcrew.agent.api.admin.domain.request.KnowledgeBaseSaveRequest;
import com.smartcrew.agent.api.admin.domain.vo.DocumentChunkAdminVo;
import com.smartcrew.agent.api.admin.domain.vo.KnowledgeBaseAdminVo;
import com.smartcrew.agent.api.admin.domain.vo.KnowledgeBaseAgentBindingVo;
import com.smartcrew.agent.api.admin.domain.vo.KnowledgeDocumentAdminVo;
import com.smartcrew.agent.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库后台管理应用服务。
 */
public interface KnowledgeBaseAdminService {

    /**
     * 分页查询知识库。
     */
    TableDataInfo<KnowledgeBaseAdminVo> listKnowledgeBases(KnowledgeBaseQuery query);

    /**
     * 查询知识库详情。
     */
    KnowledgeBaseAdminVo getKnowledgeBase(String baseCode);

    /**
     * 创建知识库。
     */
    KnowledgeBaseAdminVo createKnowledgeBase(KnowledgeBaseSaveRequest request);

    /**
     * 更新知识库。
     */
    KnowledgeBaseAdminVo updateKnowledgeBase(String baseCode, KnowledgeBaseSaveRequest request);

    /**
     * 删除知识库。
     */
    void deleteKnowledgeBase(String baseCode);

    /**
     * 分页查询知识库文档。
     */
    TableDataInfo<KnowledgeDocumentAdminVo> listDocuments(String baseCode, KnowledgeDocumentQuery query);

    /**
     * 上传知识库文档。
     */
    List<KnowledgeDocumentAdminVo> uploadDocuments(String baseCode, MultipartFile[] files);

    /**
     * 重新处理文档。
     */
    KnowledgeDocumentAdminVo reprocessDocument(String baseCode, String documentCode);

    /**
     * 删除文档。
     */
    void deleteDocument(String baseCode, String documentCode);

    /**
     * 分页查询文档切片。
     */
    TableDataInfo<DocumentChunkAdminVo> listChunks(String baseCode, String documentCode, DocumentChunkQuery query);

    /**
     * 查询知识库 Agent 绑定视图。
     */
    KnowledgeBaseAgentBindingVo getAgentBindings(String baseCode);

    /**
     * 替换知识库 Agent 绑定。
     */
    KnowledgeBaseAgentBindingVo replaceAgentBindings(String baseCode, KnowledgeBaseAgentBindingUpdateRequest request);
}
