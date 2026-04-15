package com.smartcrew.agent.controller.admin;

import com.smartcrew.agent.api.admin.domain.query.DocumentChunkQuery;
import com.smartcrew.agent.api.admin.domain.query.KnowledgeBaseQuery;
import com.smartcrew.agent.api.admin.domain.query.KnowledgeDocumentQuery;
import com.smartcrew.agent.api.admin.domain.request.KnowledgeBaseAgentBindingUpdateRequest;
import com.smartcrew.agent.api.admin.domain.request.KnowledgeBaseSaveRequest;
import com.smartcrew.agent.api.admin.domain.vo.DocumentChunkAdminVo;
import com.smartcrew.agent.api.admin.domain.vo.KnowledgeBaseAdminVo;
import com.smartcrew.agent.api.admin.domain.vo.KnowledgeBaseAgentBindingVo;
import com.smartcrew.agent.api.admin.domain.vo.KnowledgeDocumentAdminVo;
import com.smartcrew.agent.api.admin.service.KnowledgeBaseAdminService;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.core.page.TableDataInfo;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 后台知识库管理控制器。
 */
@RestController
@RequestMapping("/api/admin/knowledge-bases")
@ConditionalOnProperty(prefix = "smartcrew.rag", name = "enabled", havingValue = "true")
public class AdminKnowledgeBaseController {

    private final KnowledgeBaseAdminService knowledgeBaseAdminService;

    public AdminKnowledgeBaseController(KnowledgeBaseAdminService knowledgeBaseAdminService) {
        this.knowledgeBaseAdminService = knowledgeBaseAdminService;
    }

    /**
     * 查询知识库列表。
     */
    @GetMapping
    public TableDataInfo<KnowledgeBaseAdminVo> list(KnowledgeBaseQuery query) {
        return knowledgeBaseAdminService.listKnowledgeBases(query);
    }

    /**
     * 查询知识库详情。
     */
    @GetMapping("/{baseCode}")
    public R<KnowledgeBaseAdminVo> detail(@PathVariable("baseCode") String baseCode) {
        return R.ok(knowledgeBaseAdminService.getKnowledgeBase(baseCode));
    }

    /**
     * 创建知识库。
     */
    @PostMapping
    public R<KnowledgeBaseAdminVo> create(@Valid @RequestBody KnowledgeBaseSaveRequest request) {
        return R.ok(knowledgeBaseAdminService.createKnowledgeBase(request));
    }

    /**
     * 更新知识库。
     */
    @PutMapping("/{baseCode}")
    public R<KnowledgeBaseAdminVo> update(@PathVariable("baseCode") String baseCode,
                                          @Valid @RequestBody KnowledgeBaseSaveRequest request) {
        return R.ok(knowledgeBaseAdminService.updateKnowledgeBase(baseCode, request));
    }

    /**
     * 删除知识库。
     */
    @DeleteMapping("/{baseCode}")
    public R<Void> delete(@PathVariable("baseCode") String baseCode) {
        knowledgeBaseAdminService.deleteKnowledgeBase(baseCode);
        return R.ok("删除成功", null);
    }

    /**
     * 查询知识库下的文档列表。
     */
    @GetMapping("/{baseCode}/documents")
    public TableDataInfo<KnowledgeDocumentAdminVo> listDocuments(@PathVariable("baseCode") String baseCode,
                                                                 KnowledgeDocumentQuery query) {
        return knowledgeBaseAdminService.listDocuments(baseCode, query);
    }

    /**
     * 上传知识库文档。
     */
    @PostMapping("/{baseCode}/documents")
    public R<List<KnowledgeDocumentAdminVo>> uploadDocuments(@PathVariable("baseCode") String baseCode,
                                                             @RequestParam("files") MultipartFile[] files) {
        return R.ok(knowledgeBaseAdminService.uploadDocuments(baseCode, files));
    }

    /**
     * 重新处理知识文档。
     */
    @PostMapping("/{baseCode}/documents/{documentCode}/reprocess")
    public R<KnowledgeDocumentAdminVo> reprocessDocument(@PathVariable("baseCode") String baseCode,
                                                         @PathVariable("documentCode") String documentCode) {
        return R.ok(knowledgeBaseAdminService.reprocessDocument(baseCode, documentCode));
    }

    /**
     * 删除知识文档。
     */
    @DeleteMapping("/{baseCode}/documents/{documentCode}")
    public R<Void> deleteDocument(@PathVariable("baseCode") String baseCode,
                                  @PathVariable("documentCode") String documentCode) {
        knowledgeBaseAdminService.deleteDocument(baseCode, documentCode);
        return R.ok("删除成功", null);
    }

    /**
     * 查询文档切片列表。
     */
    @GetMapping("/{baseCode}/documents/{documentCode}/chunks")
    public TableDataInfo<DocumentChunkAdminVo> listChunks(@PathVariable("baseCode") String baseCode,
                                                          @PathVariable("documentCode") String documentCode,
                                                          DocumentChunkQuery query) {
        return knowledgeBaseAdminService.listChunks(baseCode, documentCode, query);
    }

    /**
     * 查询知识库接入的 Agent 绑定关系。
     */
    @GetMapping("/{baseCode}/agent-bindings")
    public R<KnowledgeBaseAgentBindingVo> getAgentBindings(@PathVariable("baseCode") String baseCode) {
        return R.ok(knowledgeBaseAdminService.getAgentBindings(baseCode));
    }

    /**
     * 替换知识库接入的 Agent 绑定关系。
     */
    @PutMapping("/{baseCode}/agent-bindings")
    public R<KnowledgeBaseAgentBindingVo> replaceAgentBindings(@PathVariable("baseCode") String baseCode,
                                                               @RequestBody KnowledgeBaseAgentBindingUpdateRequest request) {
        return R.ok(knowledgeBaseAdminService.replaceAgentBindings(baseCode, request));
    }
}
