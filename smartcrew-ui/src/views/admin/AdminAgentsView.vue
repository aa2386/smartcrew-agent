<template>
  <div class="agent-grid">
    <GlassPanel panel-class="admin-card">
      <div class="card-head">
        <div>
          <h3>Agent 管理</h3>
          <p class="muted">
            统一查看代码实现与数据库配置，并为 Agent 维护基础人格提示词和工作流模板绑定关系。
          </p>
        </div>
        <el-button type="primary" @click="startCreateAgent">新增 Agent</el-button>
      </div>

      <el-table
        :data="agents"
        stripe
        highlight-current-row
        row-key="agentCode"
        @current-change="handleCurrentChange"
      >
        <el-table-column prop="agentName" label="名称" min-width="160" />
        <el-table-column prop="agentCode" label="编码" min-width="180" />
        <el-table-column prop="agentType" label="类型" width="120" />
        <el-table-column label="来源状态" width="120">
          <template #default="{ row }">
            <el-tag :type="sourceStatusTagType(row.sourceStatus)">
              {{ sourceStatusText(row.sourceStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="运行态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.runtimeMode === 'STUB' ? 'warning' : 'primary'">
              {{ row.runtimeMode || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.sourceStatus === 'CODE_ONLY'"
              link
              type="primary"
              @click.stop="startCreateFromCode(row)"
            >
              创建数据库信息
            </el-button>
            <span v-else class="muted">查看 / 编辑</span>
          </template>
        </el-table-column>
      </el-table>
    </GlassPanel>

    <GlassPanel panel-class="admin-card">
      <div class="card-head">
        <div>
          <h3>{{ panelTitle }}</h3>
          <p class="muted">{{ panelDescription }}</p>
        </div>
        <div class="status-stack" v-if="form.agentCode">
          <el-tag :type="sourceStatusTagType(form.sourceStatus)">
            {{ sourceStatusText(form.sourceStatus) }}
          </el-tag>
          <el-tag type="primary">{{ currentRuntimeText }}</el-tag>
        </div>
      </div>

      <el-alert
        v-if="form.agentCode"
        class="source-alert"
        type="info"
        :closable="false"
        show-icon
        title="提示词分层规则"
        description="Agent 基础人格提示词用于定义角色、风格和安全边界；工作流模板按绑定顺序依次拼接，用于定义执行步骤与任务流程。"
      />

      <el-form v-if="pageMode !== 'empty'" label-position="top">
        <div class="form-row">
          <el-form-item label="Agent 编码">
            <el-input
              v-model="form.agentCode"
              :disabled="pageMode !== 'create'"
              placeholder="例如：customer-service-agent"
            />
          </el-form-item>
          <el-form-item label="Agent 名称">
            <el-input v-model="form.agentName" placeholder="请输入 Agent 名称" />
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item label="Agent 类型">
            <el-input v-model="form.agentType" placeholder="例如：BUILTIN / WORKFLOW / STUB" />
          </el-form-item>
          <el-form-item label="策略类型">
            <el-input v-model="form.strategyType" placeholder="例如：REACT" />
          </el-form-item>
        </div>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入 Agent 的定位与职责说明"
          />
        </el-form-item>

        <el-form-item label="基础人格 Prompt">
          <el-input
            v-model="form.systemPrompt"
            type="textarea"
            :rows="6"
            placeholder="用于定义 Agent 的人格、风格、安全边界等基础约束"
          />
        </el-form-item>

        <el-form-item label="扩展配置 JSON">
          <el-input
            v-model="form.configJson"
            type="textarea"
            :rows="8"
            placeholder="{&#10;  &quot;temperature&quot;: 0.7&#10;}"
          />
        </el-form-item>

        <div class="form-row">
          <el-form-item label="启用状态">
            <el-switch v-model="form.enabled" inline-prompt active-text="启用" inactive-text="停用" />
          </el-form-item>
          <el-form-item label="运行时 Bean">
            <el-input
              :model-value="form.beanClassName || '当前仅有数据库配置或尚未接入代码实现'"
              disabled
            />
          </el-form-item>
        </div>

        <div class="binding-head">
          <div>
            <h4>工作流 Prompt 模板</h4>
            <p class="muted">
              模板会按列表顺序依次拼接到基础人格 Prompt 之后。绑定的是具体模板记录，不会自动跟随同分类的新版本。
            </p>
          </div>
        </div>

        <div class="binding-toolbar">
          <el-select
            v-model="selectedPromptTemplateId"
            class="prompt-select"
            filterable
            placeholder="请选择要绑定的 Prompt 模板"
          >
            <el-option
              v-for="prompt in promptOptions"
              :key="prompt.id"
              :label="promptOptionLabel(prompt)"
              :value="prompt.id"
            />
          </el-select>
          <el-button @click="addPromptBinding">添加模板</el-button>
        </div>

        <el-table v-if="promptBindings.length > 0" :data="promptBindings" stripe class="binding-table">
          <el-table-column label="顺序" width="80">
            <template #default="{ row }">{{ row.sortOrder }}</template>
          </el-table-column>
          <el-table-column prop="templateName" label="模板名称" min-width="180" />
          <el-table-column prop="category" label="分类" min-width="140" />
          <el-table-column label="操作" width="200">
            <template #default="{ $index }">
              <div class="binding-actions">
                <el-button link type="primary" :disabled="$index === 0" @click="movePromptBinding($index, -1)">
                  上移
                </el-button>
                <el-button
                  link
                  type="primary"
                  :disabled="$index === promptBindings.length - 1"
                  @click="movePromptBinding($index, 1)"
                >
                  下移
                </el-button>
                <el-button link type="danger" @click="removePromptBinding($index)">移除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="binding-empty muted">当前未绑定工作流 Prompt 模板。</div>

        <div class="action-row">
          <el-button type="primary" :loading="saving" @click="submitForm">
            {{ submitButtonText }}
          </el-button>
          <el-button @click="resetToEmpty">清空当前表单</el-button>
        </div>
      </el-form>

      <div v-else class="empty-state">
        <h4>请选择一个 Agent 或新增数据库 Agent</h4>
        <p class="muted">
          代码实现与数据库配置是两套独立来源。你可以先新增数据库占位 Agent，也可以先从代码 Agent 创建数据库信息。
        </p>
      </div>
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { AgentPromptBindingRecord, AgentRecord, PromptRecord } from '../../types'

type AgentPageMode = 'empty' | 'create' | 'createFromCode' | 'edit'

const authStore = useAuthStore()
const agents = ref<AgentRecord[]>([])
const promptOptions = ref<PromptRecord[]>([])
const promptBindings = ref<AgentPromptBindingRecord[]>([])
const selectedPromptTemplateId = ref<number>()
const saving = ref(false)
const pageMode = ref<AgentPageMode>('empty')
const selectedAgentCode = ref('')

const form = reactive<AgentRecord>(createEmptyForm())

const panelTitle = computed(() => {
  if (pageMode.value === 'create') return '新增数据库 Agent'
  if (pageMode.value === 'createFromCode') return '为代码 Agent 创建数据库信息'
  if (pageMode.value === 'edit') return '编辑数据库配置'
  return 'Agent 配置'
})

const panelDescription = computed(() => {
  if (pageMode.value === 'create') {
    return '直接创建一个新的数据库占位 Agent，后续可以再补代码实现和工作流模板绑定。'
  }
  if (pageMode.value === 'createFromCode') {
    return '当前 Agent 已有代码实现但尚未写入数据库配置，可以在这里补齐人格层与模板绑定。'
  }
  if (pageMode.value === 'edit') {
    return '当前 Agent 已存在数据库配置，可以继续维护基础人格 Prompt 和工作流模板。'
  }
  return '左侧展示统一来源的 Agent 列表，右侧用于新增或维护数据库配置。'
})

const currentRuntimeText = computed(() => {
  if (!form.agentCode) return '未选择 Agent'
  return `${form.runtimeMode || '未知'} / ${form.beanClassName || '未暴露运行类'}`
})

const submitButtonText = computed(() => {
  if (pageMode.value === 'create' || pageMode.value === 'createFromCode') {
    return '创建数据库信息'
  }
  return '保存数据库配置'
})

onMounted(async () => {
  await Promise.all([loadPrompts(), loadAgents()])
})

function createEmptyForm(): AgentRecord {
  return {
    agentCode: '',
    agentName: '',
    agentType: 'STUB',
    description: '',
    strategyType: 'REACT',
    systemPrompt: '',
    configJson: '{}',
    enabled: true,
    runtimeMode: '',
    beanClassName: '',
    sourceStatus: 'DB_ONLY',
    hasCodeBean: false,
    hasDatabaseConfig: true
  }
}

function applyForm(payload: Partial<AgentRecord>) {
  Object.assign(form, createEmptyForm(), payload)
  form.configJson = payload.configJson || '{}'
  form.strategyType = payload.strategyType || 'REACT'
  form.enabled = payload.enabled ?? true
}

function sourceStatusText(status?: string) {
  if (status === 'CODE_ONLY') return '仅代码'
  if (status === 'DB_ONLY') return '仅数据库'
  if (status === 'LINKED') return '已关联'
  return '未知'
}

function sourceStatusTagType(status?: string) {
  if (status === 'CODE_ONLY') return 'warning'
  if (status === 'DB_ONLY') return 'info'
  return 'success'
}

function promptOptionLabel(prompt: PromptRecord) {
  return `${prompt.templateName} / ${prompt.category} / #${prompt.id}`
}

function normalizeBindingSortOrder() {
  promptBindings.value = promptBindings.value.map((item, index) => ({
    ...item,
    sortOrder: index + 1
  }))
}

async function loadPrompts() {
  try {
    const response = await adminPortalApi.listPrompts(authStore.adminToken)
    promptOptions.value = [...response.rows].sort((left, right) => {
      if (left.category === right.category) {
        return right.id - left.id
      }
      return left.category.localeCompare(right.category)
    })
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function loadAgents(preferredCode?: string) {
  try {
    const response = await adminPortalApi.listAgents(authStore.adminToken)
    agents.value = response.rows
    const targetCode = preferredCode || selectedAgentCode.value || agents.value[0]?.agentCode
    if (targetCode) {
      await loadAgentDetail(targetCode)
      return
    }
    resetToEmpty()
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function loadAgentDetail(code: string) {
  try {
    const [detail, bindings] = await Promise.all([
      adminPortalApi.getAgent(authStore.adminToken, code),
      adminPortalApi.listAgentPromptBindings(authStore.adminToken, code)
    ])
    selectedAgentCode.value = detail.agentCode
    applyForm(detail)
    promptBindings.value = bindings
    normalizeBindingSortOrder()
    pageMode.value = detail.hasDatabaseConfig ? 'edit' : 'createFromCode'
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function handleCurrentChange(row?: AgentRecord) {
  if (!row) return
  await loadAgentDetail(row.agentCode)
}

function startCreateAgent() {
  selectedAgentCode.value = ''
  pageMode.value = 'create'
  applyForm(createEmptyForm())
  promptBindings.value = []
  selectedPromptTemplateId.value = undefined
}

function startCreateFromCode(row: AgentRecord) {
  selectedAgentCode.value = row.agentCode
  pageMode.value = 'createFromCode'
  applyForm({
    ...row,
    sourceStatus: 'CODE_ONLY',
    hasCodeBean: true,
    hasDatabaseConfig: false,
    configJson: row.configJson || '{}',
    strategyType: row.strategyType || 'REACT'
  })
  promptBindings.value = []
  selectedPromptTemplateId.value = undefined
}

function resetToEmpty() {
  selectedAgentCode.value = ''
  pageMode.value = 'empty'
  applyForm(createEmptyForm())
  promptBindings.value = []
  selectedPromptTemplateId.value = undefined
}

function addPromptBinding() {
  if (!selectedPromptTemplateId.value) {
    ElMessage.warning('请先选择一个 Prompt 模板')
    return
  }
  if (promptBindings.value.some((item) => item.promptTemplateId === selectedPromptTemplateId.value)) {
    ElMessage.warning('同一个 Prompt 模板不能重复绑定')
    return
  }
  const prompt = promptOptions.value.find((item) => item.id === selectedPromptTemplateId.value)
  if (!prompt) {
    ElMessage.warning('未找到所选 Prompt 模板')
    return
  }
  promptBindings.value.push({
    agentCode: form.agentCode,
    promptTemplateId: prompt.id,
    templateName: prompt.templateName,
    category: prompt.category,
    templateContent: prompt.templateContent,
    sortOrder: promptBindings.value.length + 1
  })
  selectedPromptTemplateId.value = undefined
}

function movePromptBinding(index: number, step: number) {
  const targetIndex = index + step
  if (targetIndex < 0 || targetIndex >= promptBindings.value.length) {
    return
  }
  const copied = [...promptBindings.value]
  const current = copied[index]
  copied[index] = copied[targetIndex]
  copied[targetIndex] = current
  promptBindings.value = copied
  normalizeBindingSortOrder()
}

function removePromptBinding(index: number) {
  promptBindings.value.splice(index, 1)
  normalizeBindingSortOrder()
}

async function savePromptBindings(agentCode: string) {
  const bindings = await adminPortalApi.updateAgentPromptBindings(authStore.adminToken, agentCode, {
    bindings: promptBindings.value.map((item) => ({
      promptTemplateId: item.promptTemplateId
    }))
  })
  promptBindings.value = bindings
  normalizeBindingSortOrder()
}

async function submitForm() {
  if (!form.agentCode.trim()) {
    ElMessage.warning('请输入 Agent 编码')
    return
  }
  if (!form.agentName.trim()) {
    ElMessage.warning('请输入 Agent 名称')
    return
  }
  if (!form.agentType.trim()) {
    ElMessage.warning('请输入 Agent 类型')
    return
  }

  saving.value = true
  try {
    const payload: Partial<AgentRecord> = {
      agentCode: form.agentCode.trim(),
      agentName: form.agentName.trim(),
      agentType: form.agentType.trim(),
      description: form.description?.trim() || '',
      strategyType: form.strategyType?.trim() || 'REACT',
      systemPrompt: form.systemPrompt || '',
      configJson: form.configJson || '{}',
      enabled: form.enabled
    }

    let agentCode = form.agentCode
    if (pageMode.value === 'create' || pageMode.value === 'createFromCode') {
      const created = await adminPortalApi.createAgent(authStore.adminToken, payload)
      agentCode = created.agentCode
      await savePromptBindings(agentCode)
      ElMessage.success('Agent 数据库信息创建成功')
      pageMode.value = 'edit'
      await loadAgents(agentCode)
      return
    }

    const updated = await adminPortalApi.updateAgent(authStore.adminToken, form.agentCode, payload)
    agentCode = updated.agentCode
    await savePromptBindings(agentCode)
    ElMessage.success('Agent 数据库配置已保存')
    await loadAgents(agentCode)
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.agent-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: 18px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;

  h3 {
    margin: 0 0 8px;
    font-family: var(--sc-font-title);
  }

  p {
    margin: 0;
    max-width: 560px;
  }
}

.status-stack {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.source-alert {
  margin-bottom: 18px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.binding-head {
  margin: 12px 0 8px;

  h4 {
    margin: 0 0 8px;
    font-family: var(--sc-font-title);
    font-size: 18px;
  }

  p {
    margin: 0;
  }
}

.binding-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.prompt-select {
  flex: 1;
}

.binding-table {
  margin-bottom: 16px;
}

.binding-actions {
  display: flex;
  gap: 10px;
}

.binding-empty {
  margin-bottom: 16px;
  padding: 12px 4px;
}

.action-row {
  display: flex;
  gap: 12px;
}

.empty-state {
  display: grid;
  place-items: center;
  min-height: 320px;
  text-align: center;
  padding: 24px;

  h4 {
    margin: 0 0 12px;
    font-family: var(--sc-font-title);
    font-size: 20px;
  }

  p {
    margin: 0;
    max-width: 460px;
  }
}

@media (max-width: 1200px) {
  .agent-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
  }

  .card-head {
    flex-direction: column;
  }

  .status-stack {
    justify-content: flex-start;
  }

  .binding-toolbar {
    flex-direction: column;
  }
}
</style>
