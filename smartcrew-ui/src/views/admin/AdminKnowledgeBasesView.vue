<template>
  <div class="knowledge-grid">
    <GlassPanel panel-class="admin-card knowledge-list-card">
      <div class="card-head">
        <div>
          <h3>知识库列表</h3>
          <p class="muted">统一管理知识库、文档、切片与 Agent 绑定关系。</p>
        </div>
        <el-button type="primary" @click="startCreateBase">新建知识库</el-button>
      </div>

      <div class="filter-bar">
        <el-input
          v-model="baseFilters.keyword"
          clearable
          placeholder="按名称、编码或描述搜索"
          @keyup.enter="handleBaseQuery"
        />
        <el-select v-model="baseFilters.enabled" placeholder="启用状态">
          <el-option label="全部状态" value="all" />
          <el-option label="仅启用" value="true" />
          <el-option label="仅停用" value="false" />
        </el-select>
        <el-button @click="handleBaseQuery">查询</el-button>
        <el-button plain @click="resetBaseFilters">重置</el-button>
      </div>

      <div class="table-shell">
        <el-table
          :data="knowledgeBases"
          stripe
          highlight-current-row
          row-key="baseCode"
          height="100%"
          @current-change="handleBaseCurrentChange"
        >
          <el-table-column prop="baseName" label="知识库名称" min-width="170" />
          <el-table-column prop="baseCode" label="编码" min-width="160" />
          <el-table-column label="文档 / 切片" width="140">
            <template #default="{ row }">
              <span>{{ row.documentCount }} / {{ row.chunkCount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Agent" width="90">
            <template #default="{ row }">{{ row.agentCount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'">
                {{ row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination
        :current-page="basePager.pageNum"
        :page-size="basePager.pageSize"
        :page-sizes="pageSizeOptions"
        :total="basePager.total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handleBasePageChange"
        @size-change="handleBaseSizeChange"
      />
    </GlassPanel>

    <GlassPanel panel-class="admin-card knowledge-detail-card">
      <div class="card-head">
        <div>
          <h3>{{ detailTitle }}</h3>
          <p class="muted">{{ detailDescription }}</p>
        </div>
        <div class="detail-actions">
          <el-button plain :disabled="pageMode === 'empty'" @click="refreshCurrentBase">刷新</el-button>
          <el-button
            v-if="pageMode === 'edit'"
            type="danger"
            plain
            :loading="deletingBase"
            @click="removeCurrentBase"
          >
            删除知识库
          </el-button>
        </div>
      </div>

      <div v-if="pageMode === 'empty'" class="empty-state">
        <h4>请选择一个知识库，或创建新的知识库</h4>
        <p class="muted">上传文档后，系统会自动完成切分、嵌入和向量入库流程。</p>
      </div>

      <template v-else>
        <el-tabs v-model="activeTab" class="knowledge-tabs">
          <el-tab-pane label="基础信息" name="basic">
            <div class="detail-scroll">
              <div class="stats-grid">
                <div class="stat-card glass-panel">
                  <span class="muted">文档数</span>
                  <strong>{{ selectedBase?.documentCount ?? 0 }}</strong>
                </div>
                <div class="stat-card glass-panel">
                  <span class="muted">切片数</span>
                  <strong>{{ selectedBase?.chunkCount ?? 0 }}</strong>
                </div>
                <div class="stat-card glass-panel">
                  <span class="muted">已绑定 Agent</span>
                  <strong>{{ selectedBase?.agentCount ?? 0 }}</strong>
                </div>
                <div class="stat-card glass-panel">
                  <span class="muted">处理中</span>
                  <strong>{{ selectedBase?.processingDocumentCount ?? 0 }}</strong>
                </div>
              </div>

              <el-form label-position="top">
                <div class="form-row">
                  <el-form-item label="知识库编码" required>
                    <el-input
                      v-model="baseForm.baseCode"
                      :disabled="pageMode === 'edit' && Boolean(selectedBase?.hasDocuments)"
                      placeholder="例如：product-manuals"
                    />
                  </el-form-item>
                  <el-form-item label="知识库名称" required>
                    <el-input v-model="baseForm.baseName" placeholder="请输入知识库名称" />
                  </el-form-item>
                </div>

                <el-form-item label="描述">
                  <el-input
                    v-model="baseForm.description"
                    type="textarea"
                    :rows="3"
                    placeholder="简要说明知识库的适用范围与维护对象"
                  />
                </el-form-item>

                <div class="form-row">
                  <el-form-item label="嵌入模型">
                    <el-input
                      v-model="baseForm.embeddingModel"
                      :disabled="pageMode === 'edit' && !selectedBase?.embeddingModelEditable"
                      placeholder="默认继承系统 RAG 配置"
                    />
                  </el-form-item>
                  <el-form-item label="向量命名空间">
                    <el-input
                      v-model="baseForm.collectionName"
                      :disabled="pageMode === 'edit' && !selectedBase?.collectionNameEditable"
                      placeholder="默认根据知识库编码自动生成"
                    />
                  </el-form-item>
                </div>

                <div class="form-row form-row--compact">
                  <el-form-item label="启用状态">
                    <el-switch
                      v-model="baseForm.enabled"
                      inline-prompt
                      active-text="启用"
                      inactive-text="停用"
                    />
                  </el-form-item>
                  <el-form-item label="当前状态">
                    <el-input :model-value="selectedBaseStatusText" disabled />
                  </el-form-item>
                </div>

                <div class="hint-list">
                  <el-alert
                    type="info"
                    :closable="false"
                    show-icon
                    title="上传文档后会自动完成切分、向量化和 Chroma 入库。"
                  />
                  <el-alert
                    v-if="pageMode === 'edit' && !selectedBase?.collectionNameEditable"
                    type="warning"
                    :closable="false"
                    show-icon
                    title="当前知识库已有文档，向量命名空间已锁定。"
                  />
                  <el-alert
                    v-if="pageMode === 'edit' && !selectedBase?.embeddingModelEditable"
                    type="warning"
                    :closable="false"
                    show-icon
                    title="当前知识库已有已处理文档，嵌入模型已锁定。"
                  />
                </div>

                <div class="action-row">
                  <el-button type="primary" :loading="savingBase" @click="submitBase">
                    {{ pageMode === 'create' ? '创建知识库' : '保存知识库' }}
                  </el-button>
                  <el-button plain @click="resetBaseForm">
                    {{ pageMode === 'create' ? '清空表单' : '恢复当前数据' }}
                  </el-button>
                </div>
              </el-form>
            </div>
          </el-tab-pane>

          <el-tab-pane label="文档管理" name="documents">
            <div class="detail-scroll">
              <el-alert
                class="document-alert"
                type="info"
                :closable="false"
                show-icon
                title="上传文档后会自动异步处理，页面可通过“刷新”按钮手动查看最新状态。"
              />

              <div class="upload-shell">
                <el-upload
                  ref="uploadRef"
                  class="upload-panel"
                  drag
                  multiple
                  :auto-upload="false"
                  :show-file-list="true"
                  :on-change="handleUploadFileChange"
                  :on-remove="handleUploadFileRemove"
                >
                  <el-icon class="upload-icon"><UploadFilled /></el-icon>
                  <div class="el-upload__text">拖拽文档到这里，或点击选择文件</div>
                  <template #tip>
                    <div class="el-upload__tip">支持批量上传；上传后会自动切分并写入向量库。</div>
                  </template>
                </el-upload>

                <div class="upload-actions">
                  <el-button type="primary" :loading="uploading" @click="submitUploadFiles">开始上传</el-button>
                  <el-button plain @click="clearUploadFiles">清空待上传</el-button>
                </div>
              </div>

              <div class="filter-bar filter-bar--documents">
                <el-input
                  v-model="documentFilters.keyword"
                  clearable
                  placeholder="按文档名称或编码搜索"
                  @keyup.enter="handleDocumentQuery"
                />
                <el-select v-model="documentFilters.status" placeholder="处理状态">
                  <el-option label="全部状态" value="" />
                  <el-option label="待处理" value="pending" />
                  <el-option label="处理中" value="processing" />
                  <el-option label="已完成" value="completed" />
                  <el-option label="失败" value="failed" />
                </el-select>
                <el-input
                  v-model="documentFilters.fileType"
                  clearable
                  placeholder="文件类型，如 md / pdf"
                  @keyup.enter="handleDocumentQuery"
                />
                <el-button @click="handleDocumentQuery">查询</el-button>
                <el-button plain @click="resetDocumentFilters">重置</el-button>
              </div>

              <div class="table-shell">
                <el-table :data="documents" stripe height="100%">
                  <el-table-column prop="documentName" label="文档名称" min-width="220" show-overflow-tooltip />
                  <el-table-column prop="fileType" label="类型" width="100" />
                  <el-table-column label="大小" width="110">
                    <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
                  </el-table-column>
                  <el-table-column label="状态" width="120">
                    <template #default="{ row }">
                      <el-tag :type="documentStatusTagType(row.status)">
                        {{ documentStatusText(row.status) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="chunkCount" label="切片数" width="90" />
                  <el-table-column label="更新时间" min-width="170">
                    <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="250" fixed="right">
                    <template #default="{ row }">
                      <div class="row-actions">
                        <el-button link type="primary" @click="viewDocumentChunks(row)">查看切片</el-button>
                        <el-button link type="primary" @click="reprocessDocument(row)">重新处理</el-button>
                        <el-button link type="danger" @click="removeDocument(row)">删除</el-button>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <div class="document-foot">
                <el-pagination
                  :current-page="documentPager.pageNum"
                  :page-size="documentPager.pageSize"
                  :page-sizes="pageSizeOptions"
                  :total="documentPager.total"
                  layout="total, sizes, prev, pager, next, jumper"
                  @current-change="handleDocumentPageChange"
                  @size-change="handleDocumentSizeChange"
                />
                <p v-if="currentDocumentError" class="error-text">最近失败原因：{{ currentDocumentError }}</p>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="切片查看" name="chunks">
            <div class="detail-scroll">
              <div class="chunk-toolbar">
                <el-select
                  v-model="selectedDocumentCode"
                  filterable
                  placeholder="请选择文档"
                  @change="handleChunkDocumentChange"
                >
                  <el-option
                    v-for="item in documents"
                    :key="item.documentCode"
                    :label="`${item.documentName} / ${item.documentCode}`"
                    :value="item.documentCode"
                  />
                </el-select>
                <el-input
                  v-model="chunkFilters.keyword"
                  clearable
                  placeholder="按内容、向量 ID 或 metadata 搜索"
                  @keyup.enter="handleChunkQuery"
                />
                <el-button :disabled="!selectedDocumentCode" @click="handleChunkQuery">查询</el-button>
                <el-button plain :disabled="!selectedDocumentCode" @click="resetChunkFilters">重置</el-button>
              </div>

              <div class="table-shell">
                <el-table :data="chunks" stripe height="100%">
                  <el-table-column prop="chunkIndex" label="序号" width="90" />
                  <el-table-column prop="tokenCount" label="Token" width="100" />
                  <el-table-column prop="vectorId" label="向量 ID" min-width="180" show-overflow-tooltip />
                  <el-table-column prop="contentPreview" label="内容摘要" min-width="320" show-overflow-tooltip />
                  <el-table-column label="操作" width="120" fixed="right">
                    <template #default="{ row }">
                      <el-button link type="primary" @click="openChunkDialog(row)">查看详情</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <el-pagination
                :current-page="chunkPager.pageNum"
                :page-size="chunkPager.pageSize"
                :page-sizes="pageSizeOptions"
                :total="chunkPager.total"
                layout="total, sizes, prev, pager, next, jumper"
                @current-change="handleChunkPageChange"
                @size-change="handleChunkSizeChange"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="接入 Agent" name="agents">
            <div class="detail-scroll">
              <div class="binding-head">
                <div>
                  <h4>管理当前知识库可接入的 Agent</h4>
                  <p class="muted">右侧为已绑定 Agent，保存后会整体替换绑定关系。</p>
                </div>
                <el-button type="primary" :loading="savingBindings" @click="saveAgentBindings">保存绑定</el-button>
              </div>

              <el-transfer
                v-model="bindingTargetKeys"
                class="agent-transfer"
                filterable
                :data="transferData"
                :titles="['可选 Agent', '已绑定 Agent']"
                :props="{ key: 'key', label: 'label', disabled: 'disabled' }"
                filter-placeholder="搜索 Agent"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </GlassPanel>

    <el-dialog v-model="chunkDialogVisible" title="切片详情" width="860px">
      <template v-if="activeChunk">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="文档">{{ activeChunk.documentName }}</el-descriptions-item>
          <el-descriptions-item label="切片序号">{{ activeChunk.chunkIndex }}</el-descriptions-item>
          <el-descriptions-item label="Token 数">{{ activeChunk.tokenCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="向量 ID">{{ activeChunk.vectorId || '暂无' }}</el-descriptions-item>
        </el-descriptions>

        <div class="chunk-detail">
          <h4>切片内容</h4>
          <pre>{{ activeChunk.content }}</pre>
        </div>

        <div class="chunk-detail">
          <h4>Metadata</h4>
          <pre>{{ activeChunk.metadata || '暂无' }}</pre>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import type { UploadInstance, UploadRawFile, UploadUserFile } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type {
  DocumentChunkRecord,
  KnowledgeBaseAgentBindingRecord,
  KnowledgeBaseRecord,
  KnowledgeDocumentRecord
} from '../../types'

type KnowledgePageMode = 'empty' | 'create' | 'edit'

const pageSizeOptions = [10, 30, 50, 100, 250]

const authStore = useAuthStore()
const uploadRef = ref<UploadInstance>()
const pageMode = ref<KnowledgePageMode>('empty')
const activeTab = ref('basic')
const knowledgeBases = ref<KnowledgeBaseRecord[]>([])
const selectedBase = ref<KnowledgeBaseRecord>()
const documents = ref<KnowledgeDocumentRecord[]>([])
const chunks = ref<DocumentChunkRecord[]>([])
const agentBindings = ref<KnowledgeBaseAgentBindingRecord>()
const bindingTargetKeys = ref<string[]>([])
const uploadFiles = ref<UploadRawFile[]>([])
const savingBase = ref(false)
const deletingBase = ref(false)
const uploading = ref(false)
const savingBindings = ref(false)
const chunkDialogVisible = ref(false)
const activeChunk = ref<DocumentChunkRecord>()
const selectedDocumentCode = ref('')
const pollingTimer = ref<number>()
const pollingInFlight = ref(false)

const baseFilters = reactive({
  keyword: '',
  enabled: 'all'
})

const basePager = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const baseForm = reactive(createEmptyBaseForm())

const documentFilters = reactive({
  keyword: '',
  status: '',
  fileType: ''
})

const documentPager = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const chunkFilters = reactive({
  keyword: ''
})

const chunkPager = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const detailTitle = computed(() => {
  if (pageMode.value === 'create') return '新建知识库'
  if (pageMode.value === 'edit') return selectedBase.value?.baseName || '知识库详情'
  return '知识库详情'
})

const detailDescription = computed(() => {
  if (pageMode.value === 'create') {
    return '先建立知识库基础信息，随后即可上传文档并自动完成 RAG 入库。'
  }
  if (pageMode.value === 'edit') {
    return `${selectedBase.value?.baseCode || ''} · 管理文档、切片和 Agent 接入范围。`
  }
  return '请选择左侧知识库进行管理。'
})

const selectedBaseStatusText = computed(() => {
  if (pageMode.value === 'create') return '新建中'
  if (!selectedBase.value) return '未选择'
  return selectedBase.value.enabled ? '已启用' : '已停用'
})

const transferData = computed(() => {
  if (!agentBindings.value) {
    return []
  }
  return [...agentBindings.value.availableAgents, ...agentBindings.value.boundAgents].map((item) => ({
    key: item.agentCode,
    label: `${item.agentName} / ${item.agentCode} / ${item.agentType}`,
    disabled: !item.enabled
  }))
})

const currentDocumentError = computed(() => {
  const failed = documents.value.find((item) => item.status === 'failed' && item.errorMessage)
  return failed?.errorMessage || ''
})

onMounted(async () => {
  await loadKnowledgeBases()
})

onBeforeUnmount(() => {
  stopDocumentPolling()
})

watch(activeTab, async (value) => {
  if (value === 'chunks' && selectedDocumentCode.value) {
    await loadChunks()
  }
  if (value === 'agents' && selectedBase.value?.baseCode && !agentBindings.value) {
    await loadAgentBindings()
  }
})

function createEmptyBaseForm() {
  return {
    baseCode: '',
    baseName: '',
    description: '',
    embeddingModel: '',
    collectionName: '',
    enabled: true
  }
}

function applyBaseForm(payload?: Partial<KnowledgeBaseRecord>) {
  const empty = createEmptyBaseForm()
  baseForm.baseCode = payload?.baseCode ?? empty.baseCode
  baseForm.baseName = payload?.baseName ?? empty.baseName
  baseForm.description = payload?.description ?? empty.description
  baseForm.embeddingModel = payload?.embeddingModel ?? empty.embeddingModel
  baseForm.collectionName = payload?.collectionName ?? empty.collectionName
  baseForm.enabled = payload?.enabled ?? empty.enabled
}

function resetBaseForm() {
  if (pageMode.value === 'edit' && selectedBase.value) {
    applyBaseForm(selectedBase.value)
    return
  }
  applyBaseForm()
}

async function loadKnowledgeBases(preferredCode?: string) {
  try {
    const response = await adminPortalApi.listKnowledgeBases(authStore.adminToken, {
      keyword: baseFilters.keyword || undefined,
      enabled: baseFilters.enabled === 'all' ? undefined : baseFilters.enabled === 'true',
      pageNum: basePager.pageNum,
      pageSize: basePager.pageSize
    })
    knowledgeBases.value = response.rows
    basePager.total = response.total

    if (pageMode.value === 'create' && !preferredCode) {
      return
    }

    const targetCode = preferredCode || selectedBase.value?.baseCode || knowledgeBases.value[0]?.baseCode
    if (!targetCode) {
      resetDetailState()
      return
    }
    await loadKnowledgeBaseDetail(targetCode)
  } catch (error) {
    handleError(error)
  }
}

async function loadKnowledgeBaseDetail(baseCode: string) {
  try {
    const detail = await adminPortalApi.getKnowledgeBase(authStore.adminToken, baseCode)
    selectedBase.value = detail
    pageMode.value = 'edit'
    applyBaseForm(detail)
    await Promise.all([loadDocuments(), loadAgentBindings()])
  } catch (error) {
    handleError(error)
  }
}

async function refreshCurrentBase() {
  if (!selectedBase.value?.baseCode) return
  await Promise.all([loadKnowledgeBases(selectedBase.value.baseCode), loadChunks()])
}

function startCreateBase() {
  stopDocumentPolling()
  pageMode.value = 'create'
  activeTab.value = 'basic'
  selectedBase.value = undefined
  documents.value = []
  chunks.value = []
  agentBindings.value = undefined
  bindingTargetKeys.value = []
  selectedDocumentCode.value = ''
  documentPager.total = 0
  chunkPager.total = 0
  clearUploadFiles()
  applyBaseForm()
}

async function submitBase() {
  if (!baseForm.baseCode.trim()) {
    ElMessage.warning('请输入知识库编码')
    return
  }
  if (!baseForm.baseName.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }

  savingBase.value = true
  try {
    const payload = {
      baseCode: baseForm.baseCode.trim(),
      baseName: baseForm.baseName.trim(),
      description: baseForm.description?.trim() || '',
      embeddingModel: baseForm.embeddingModel?.trim() || '',
      collectionName: baseForm.collectionName?.trim() || '',
      enabled: baseForm.enabled
    }

    const result = pageMode.value === 'create'
      ? await adminPortalApi.createKnowledgeBase(authStore.adminToken, payload)
      : await adminPortalApi.updateKnowledgeBase(authStore.adminToken, selectedBase.value!.baseCode, payload)

    ElMessage.success(pageMode.value === 'create' ? '知识库已创建' : '知识库已保存')
    pageMode.value = 'edit'
    activeTab.value = 'basic'
    await loadKnowledgeBases(result.baseCode)
  } catch (error) {
    handleError(error)
  } finally {
    savingBase.value = false
  }
}

async function removeCurrentBase() {
  if (!selectedBase.value) return
  try {
    await ElMessageBox.confirm(
      `确认删除知识库「${selectedBase.value.baseName}」吗？仅当该知识库下没有文档时才允许删除。`,
      '删除确认',
      { type: 'warning' }
    )
    deletingBase.value = true
    await adminPortalApi.deleteKnowledgeBase(authStore.adminToken, selectedBase.value.baseCode)
    ElMessage.success('知识库已删除')
    basePager.pageNum = 1
    await loadKnowledgeBases()
  } catch (error) {
    if (error instanceof Error && error.message !== 'cancel') {
      handleError(error)
    }
  } finally {
    deletingBase.value = false
  }
}

async function loadDocuments() {
  if (!selectedBase.value?.baseCode) {
    documents.value = []
    documentPager.total = 0
    stopDocumentPolling()
    return
  }
  try {
    const response = await adminPortalApi.listKnowledgeDocuments(authStore.adminToken, selectedBase.value.baseCode, {
      keyword: documentFilters.keyword || undefined,
      status: documentFilters.status || undefined,
      fileType: documentFilters.fileType || undefined,
      pageNum: documentPager.pageNum,
      pageSize: documentPager.pageSize
    })
    documents.value = response.rows
    documentPager.total = response.total
    syncSelectedDocument()
    syncDocumentPolling()
  } catch (error) {
    stopDocumentPolling()
    handleError(error)
  }
}

function syncSelectedDocument() {
  if (!documents.value.length) {
    selectedDocumentCode.value = ''
    chunks.value = []
    chunkPager.total = 0
    return
  }
  if (!selectedDocumentCode.value || !documents.value.some((item) => item.documentCode === selectedDocumentCode.value)) {
    selectedDocumentCode.value = documents.value[0].documentCode
  }
}

function syncDocumentPolling() {
  // const hasActive = documents.value.some((item) => item.status === 'pending' || item.status === 'processing')
  // if (!hasActive) {
  //   stopDocumentPolling()
  //   return
  // }
  // if (pollingTimer.value) {
  //   return
  // }
  // pollingTimer.value = window.setInterval(async () => {
  //   if (pollingInFlight.value || !selectedBase.value?.baseCode) {
  //     return
  //   }
  //   pollingInFlight.value = true
  //   try {
  //     await loadDocuments()
  //     if (activeTab.value === 'chunks' && selectedDocumentCode.value) {
  //       await loadChunks()
  //     }
  //     await loadKnowledgeBases(selectedBase.value.baseCode)
  //   } finally {
  //     pollingInFlight.value = false
  //   }
  // }, 3000)
  // 暂时关闭自动轮询，保留方法供后续恢复。
  stopDocumentPolling()
}

function stopDocumentPolling() {
  if (pollingTimer.value) {
    window.clearInterval(pollingTimer.value)
    pollingTimer.value = undefined
  }
  pollingInFlight.value = false
}

function handleUploadFileChange(_file: UploadUserFile, fileList: UploadUserFile[]) {
  uploadFiles.value = fileList.map((item) => item.raw).filter((item): item is UploadRawFile => Boolean(item))
}

function handleUploadFileRemove(_file: UploadUserFile, fileList: UploadUserFile[]) {
  uploadFiles.value = fileList.map((item) => item.raw).filter((item): item is UploadRawFile => Boolean(item))
}

function clearUploadFiles() {
  uploadRef.value?.clearFiles()
  uploadFiles.value = []
}

async function submitUploadFiles() {
  if (!selectedBase.value?.baseCode) {
    ElMessage.warning('请先创建或选择知识库')
    return
  }
  if (!uploadFiles.value.length) {
    ElMessage.warning('请先选择要上传的文档')
    return
  }
  uploading.value = true
  try {
    const result = await adminPortalApi.uploadKnowledgeDocuments(
      authStore.adminToken,
      selectedBase.value.baseCode,
      uploadFiles.value
    )
    clearUploadFiles()
    ElMessage.success(`已提交 ${result.length} 份文档，系统正在后台处理`)
    documentPager.pageNum = 1
    await Promise.all([loadDocuments(), loadKnowledgeBases(selectedBase.value.baseCode)])
  } catch (error) {
    handleError(error)
  } finally {
    uploading.value = false
  }
}

async function reprocessDocument(row: KnowledgeDocumentRecord) {
  if (!selectedBase.value?.baseCode) return
  try {
    await adminPortalApi.reprocessKnowledgeDocument(authStore.adminToken, selectedBase.value.baseCode, row.documentCode)
    ElMessage.success('文档已重新加入处理队列')
    await Promise.all([loadDocuments(), loadKnowledgeBases(selectedBase.value.baseCode)])
  } catch (error) {
    handleError(error)
  }
}

async function removeDocument(row: KnowledgeDocumentRecord) {
  if (!selectedBase.value?.baseCode) return
  try {
    await ElMessageBox.confirm(`确认删除文档「${row.documentName}」吗？该操作会同时删除切片与向量记录。`, '删除确认', {
      type: 'warning'
    })
    await adminPortalApi.deleteKnowledgeDocument(authStore.adminToken, selectedBase.value.baseCode, row.documentCode)
    ElMessage.success('文档已删除')
    await Promise.all([loadDocuments(), loadChunks(), loadKnowledgeBases(selectedBase.value.baseCode)])
  } catch (error) {
    if (error instanceof Error && error.message !== 'cancel') {
      handleError(error)
    }
  }
}

function viewDocumentChunks(row: KnowledgeDocumentRecord) {
  selectedDocumentCode.value = row.documentCode
  activeTab.value = 'chunks'
  chunkPager.pageNum = 1
  loadChunks()
}

async function loadChunks() {
  if (!selectedBase.value?.baseCode || !selectedDocumentCode.value) {
    chunks.value = []
    chunkPager.total = 0
    return
  }
  try {
    const response = await adminPortalApi.listDocumentChunks(
      authStore.adminToken,
      selectedBase.value.baseCode,
      selectedDocumentCode.value,
      {
        keyword: chunkFilters.keyword || undefined,
        pageNum: chunkPager.pageNum,
        pageSize: chunkPager.pageSize
      }
    )
    chunks.value = response.rows
    chunkPager.total = response.total
  } catch (error) {
    handleError(error)
  }
}

async function loadAgentBindings() {
  if (!selectedBase.value?.baseCode) {
    agentBindings.value = undefined
    bindingTargetKeys.value = []
    return
  }
  try {
    const result = await adminPortalApi.getKnowledgeBaseAgentBindings(authStore.adminToken, selectedBase.value.baseCode)
    agentBindings.value = result
    bindingTargetKeys.value = result.boundAgents.map((item) => item.agentCode)
  } catch (error) {
    handleError(error)
  }
}

async function saveAgentBindings() {
  if (!selectedBase.value?.baseCode) return
  savingBindings.value = true
  try {
    const result = await adminPortalApi.updateKnowledgeBaseAgentBindings(
      authStore.adminToken,
      selectedBase.value.baseCode,
      bindingTargetKeys.value
    )
    agentBindings.value = result
    bindingTargetKeys.value = result.boundAgents.map((item) => item.agentCode)
    await loadKnowledgeBases(selectedBase.value.baseCode)
    ElMessage.success('Agent 绑定已更新')
  } catch (error) {
    handleError(error)
  } finally {
    savingBindings.value = false
  }
}

function openChunkDialog(row: DocumentChunkRecord) {
  activeChunk.value = row
  chunkDialogVisible.value = true
}

async function handleBaseCurrentChange(row?: KnowledgeBaseRecord) {
  if (!row?.baseCode) return
  activeTab.value = 'basic'
  await loadKnowledgeBaseDetail(row.baseCode)
}

async function handleBasePageChange(pageNum: number) {
  basePager.pageNum = pageNum
  await loadKnowledgeBases()
}

async function handleBaseSizeChange(pageSize: number) {
  basePager.pageSize = pageSize
  basePager.pageNum = 1
  await loadKnowledgeBases()
}

async function handleBaseQuery() {
  basePager.pageNum = 1
  await loadKnowledgeBases()
}

async function resetBaseFilters() {
  baseFilters.keyword = ''
  baseFilters.enabled = 'all'
  basePager.pageNum = 1
  await loadKnowledgeBases()
}

async function handleDocumentPageChange(pageNum: number) {
  documentPager.pageNum = pageNum
  await loadDocuments()
}

async function handleDocumentSizeChange(pageSize: number) {
  documentPager.pageSize = pageSize
  documentPager.pageNum = 1
  await loadDocuments()
}

async function handleDocumentQuery() {
  documentPager.pageNum = 1
  await loadDocuments()
}

async function resetDocumentFilters() {
  documentFilters.keyword = ''
  documentFilters.status = ''
  documentFilters.fileType = ''
  documentPager.pageNum = 1
  await loadDocuments()
}

async function handleChunkDocumentChange() {
  chunkPager.pageNum = 1
  await loadChunks()
}

async function handleChunkPageChange(pageNum: number) {
  chunkPager.pageNum = pageNum
  await loadChunks()
}

async function handleChunkSizeChange(pageSize: number) {
  chunkPager.pageSize = pageSize
  chunkPager.pageNum = 1
  await loadChunks()
}

async function handleChunkQuery() {
  chunkPager.pageNum = 1
  await loadChunks()
}

async function resetChunkFilters() {
  chunkFilters.keyword = ''
  chunkPager.pageNum = 1
  await loadChunks()
}

function resetDetailState() {
  pageMode.value = 'empty'
  selectedBase.value = undefined
  documents.value = []
  chunks.value = []
  agentBindings.value = undefined
  bindingTargetKeys.value = []
  selectedDocumentCode.value = ''
  documentPager.total = 0
  chunkPager.total = 0
  clearUploadFiles()
  applyBaseForm()
  stopDocumentPolling()
}

function documentStatusText(status: string) {
  if (status === 'pending') return '待处理'
  if (status === 'processing') return '处理中'
  if (status === 'completed') return '已完成'
  if (status === 'failed') return '失败'
  return status || '未知'
}

function documentStatusTagType(status: string) {
  if (status === 'pending') return 'warning'
  if (status === 'processing') return 'primary'
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'danger'
  return 'info'
}

function formatDate(value?: string) {
  if (!value) return '暂无'
  return new Date(value).toLocaleString('zh-CN')
}

function formatFileSize(fileSize?: number) {
  const value = fileSize ?? 0
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(2)} MB`
}

function handleError(error: unknown) {
  if (error instanceof Error) {
    ElMessage.error(error.message)
    return
  }
  ElMessage.error('操作失败，请稍后重试')
}
</script>

<style scoped lang="scss">
.knowledge-grid {
  display: grid;
  grid-template-columns: minmax(340px, 0.92fr) minmax(0, 1.08fr);
  gap: 18px;
  height: 100%;
  min-height: 0;
}

.knowledge-list-card,
.knowledge-detail-card {
  display: flex;
  flex-direction: column;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;

  h3 {
    margin: 0 0 8px;
    font-family: var(--sc-font-title);
  }

  p {
    margin: 0;
    max-width: 620px;
  }
}

.detail-actions,
.row-actions,
.upload-actions,
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.detail-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 6px;
}

.filter-bar,
.chunk-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) 180px auto auto;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-bar--documents {
  grid-template-columns: minmax(0, 1.2fr) 160px minmax(0, 0.7fr) auto auto;
  margin-top: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.stat-card {
  border-radius: 18px;
  padding: 18px;

  strong {
    display: block;
    margin-top: 10px;
    font-family: var(--sc-font-title);
    font-size: 2rem;
  }
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.hint-list {
  display: grid;
  gap: 12px;
  margin-bottom: 16px;
}

.upload-shell {
  display: grid;
  gap: 14px;
}

.upload-panel {
  :deep(.el-upload-dragger) {
    width: 100%;
    border-radius: 20px;
    border: 1px dashed rgba(37, 99, 235, 0.28);
    background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(255, 255, 255, 0.4)),
      rgba(255, 255, 255, 0.22);
  }
}

.upload-icon {
  font-size: 40px;
  color: var(--sc-primary);
}

.document-alert {
  margin-bottom: 16px;
}

.document-foot {
  display: grid;
  gap: 12px;
}

.error-text {
  margin: 0;
  color: #b42318;
  line-height: 1.7;
}

.binding-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;

  h4 {
    margin: 0 0 8px;
    font-family: var(--sc-font-title);
    font-size: 1.1rem;
  }

  p {
    margin: 0;
  }
}

.agent-transfer {
  :deep(.el-transfer-panel) {
    width: min(100%, 360px);
    border-radius: 18px;
    background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(255, 255, 255, 0.52)),
      rgba(255, 255, 255, 0.18);
    border: 1px solid rgba(255, 255, 255, 0.58);
    box-shadow: var(--sc-shadow-soft), inset 0 1px 0 rgba(255, 255, 255, 0.68);
    backdrop-filter: blur(18px) saturate(145%);
    -webkit-backdrop-filter: blur(18px) saturate(145%);
  }

  :deep(.el-transfer-panel__body) {
    height: 380px;
  }
}

.knowledge-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
  }

  :deep(.el-tab-pane) {
    height: 100%;
    min-height: 0;
  }
}

.chunk-detail {
  margin-top: 16px;

  h4 {
    margin: 0 0 10px;
    font-family: var(--sc-font-title);
  }

  pre {
    margin: 0;
    white-space: pre-wrap;
    line-height: 1.8;
    border-radius: 18px;
    padding: 16px;
    background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.44)),
      rgba(255, 255, 255, 0.18);
    border: 1px solid rgba(255, 255, 255, 0.56);
  }
}

.empty-state {
  display: grid;
  place-items: center;
  min-height: 360px;
  text-align: center;
  padding: 24px;

  h4 {
    margin: 0 0 10px;
    font-family: var(--sc-font-title);
    font-size: 1.3rem;
  }

  p {
    margin: 0;
    max-width: 460px;
  }
}

@media (max-width: 1320px) {
  .knowledge-grid {
    grid-template-columns: 1fr;
    height: auto;
  }
}

@media (max-width: 860px) {
  .filter-bar,
  .filter-bar--documents,
  .chunk-toolbar,
  .form-row,
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .card-head,
  .binding-head {
    flex-direction: column;
    align-items: stretch;
  }

  .agent-transfer {
    :deep(.el-transfer) {
      display: grid;
      gap: 12px;
    }

    :deep(.el-transfer-panel) {
      width: 100%;
    }
  }
}
</style>
