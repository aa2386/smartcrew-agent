<template>
  <div class="tool-grid">
    <GlassPanel panel-class="admin-card tool-list-card">
      <div class="card-head">
        <div>
          <h3>Tool 管理</h3>
          <p class="muted">统一查看代码 Tool 与数据库元数据，维护名称、描述、风险等级和手动调试入口。</p>
        </div>
        <el-button type="primary" @click="startCreateTool">新增 Tool</el-button>
      </div>

      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="按名称、编码或描述搜索"
        />
        <el-select v-model="filters.enabled" placeholder="启用状态">
          <el-option label="全部状态" value="all" />
          <el-option label="仅启用" value="true" />
          <el-option label="仅停用" value="false" />
        </el-select>
      </div>

      <div class="table-shell">
        <el-table
          :data="filteredTools"
          stripe
          highlight-current-row
          row-key="toolCode"
          height="100%"
          @current-change="handleCurrentChange"
        >
          <el-table-column prop="toolName" label="名称" min-width="170" />
          <el-table-column prop="toolCode" label="编码" min-width="170" />
          <el-table-column label="来源" width="100">
            <template #default="{ row }">
              <el-tag :type="sourceStatusTagType(row.sourceStatus)">
                {{ sourceStatusText(row.sourceStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'">
                {{ row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="可执行" width="90">
            <template #default="{ row }">
              <el-tag :type="row.executable ? 'success' : 'danger'">
                {{ row.executable ? '是' : '否' }}
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
                创建元数据
              </el-button>
              <span v-else class="muted">查看 / 编辑</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </GlassPanel>

    <GlassPanel panel-class="admin-card tool-detail-card">
      <div class="card-head">
        <div>
          <h3>{{ panelTitle }}</h3>
          <p class="muted">{{ panelDescription }}</p>
        </div>
        <div v-if="form.toolCode" class="status-stack">
          <el-tag :type="sourceStatusTagType(form.sourceStatus)">{{ sourceStatusText(form.sourceStatus) }}</el-tag>
          <el-tag :type="form.enabled ? 'success' : 'info'">{{ form.enabled ? '启用' : '停用' }}</el-tag>
        </div>
      </div>

      <div v-if="pageMode === 'empty'" class="empty-state">
        <h4>请选择一个 Tool，或创建新的数据库元数据</h4>
        <p class="muted">数据库配置只负责 Tool 元数据治理，真正执行能力来自代码 Bean。</p>
      </div>

      <template v-else>
        <el-tabs v-model="activeTab" class="tool-tabs">
          <el-tab-pane label="基础配置" name="basic">
            <div class="tab-pane-layout">
              <div class="detail-scroll">
                <el-alert
                  class="hint-alert"
                  type="info"
                  :closable="false"
                  show-icon
                  title="数据库配置只负责元数据治理，真正执行能力来自代码 Bean。"
                />

                <el-alert
                  v-if="form.resolveError"
                  class="hint-alert"
                  type="warning"
                  :closable="false"
                  show-icon
                  :title="`当前 Tool 不可执行：${form.resolveError}`"
                />

                <el-form label-position="top">
                  <div class="form-row">
                    <el-form-item label="Tool 编码">
                      <el-input
                        v-model="form.toolCode"
                        :disabled="pageMode !== 'create'"
                        placeholder="例如：web-search"
                      />
                    </el-form-item>
                    <el-form-item label="Tool 名称">
                      <el-input v-model="form.toolName" placeholder="请输入 Tool 名称" />
                    </el-form-item>
                  </div>

                  <el-form-item label="描述">
                    <el-input
                      v-model="form.description"
                      type="textarea"
                      :rows="3"
                      placeholder="说明 Tool 负责什么，以及适合在什么场景下被调用"
                    />
                  </el-form-item>

                  <el-form-item label="风险等级">
                    <el-select v-model="form.riskLevel" class="full-width">
                      <el-option label="LOW" value="LOW" />
                      <el-option label="MEDIUM" value="MEDIUM" />
                      <el-option label="HIGH" value="HIGH" />
                    </el-select>
                  </el-form-item>

                  <div class="form-row form-row--compact">
                    <el-form-item label="启用状态">
                      <el-switch
                        v-model="form.enabled"
                        inline-prompt
                        active-text="启用"
                        inactive-text="停用"
                      />
                    </el-form-item>
                    <el-form-item label="来源状态">
                      <el-input :model-value="sourceStatusText(form.sourceStatus)" disabled />
                    </el-form-item>
                  </div>

                  <el-form-item label="Spring Bean 名称">
                    <el-input
                      v-model="form.beanName"
                      placeholder="例如：basicTools"
                    />
                  </el-form-item>

                  <el-form-item label="扩展配置 JSON">
                    <el-input
                      v-model="form.configJson"
                      type="textarea"
                      :rows="8"
                      placeholder="{&#10;  &quot;timeoutMs&quot;: 5000&#10;}"
                    />
                  </el-form-item>

                  <div class="action-row">
                    <el-button type="primary" :loading="saving" @click="submitForm">
                      {{ submitButtonText }}
                    </el-button>
                    <el-button plain @click="resetCurrentForm">
                      {{ pageMode === 'create' ? '清空表单' : '恢复当前数据' }}
                    </el-button>
                  </div>
                </el-form>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="动作预览" name="actions">
            <div class="tab-pane-layout">
              <div class="detail-scroll">
                <div class="binding-head">
                  <div>
                    <h4>当前 Tool 可调用动作</h4>
                    <p class="muted">动作定义来自代码层 `@Tool` 反射结果，数据库仅做治理补充。</p>
                  </div>
                  <el-tag :type="form.executable ? 'success' : 'danger'">
                    {{ form.executable ? '当前可执行' : '当前不可执行' }}
                  </el-tag>
                </div>

                <el-table v-if="form.actions.length > 0" :data="form.actions" stripe class="actions-table">
                  <el-table-column prop="actionName" label="动作名称" min-width="160" />
                  <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
                  <el-table-column label="参数" min-width="260">
                    <template #default="{ row }">
                      <div v-if="row.parameters.length > 0" class="param-list">
                        <span v-for="item in row.parameters" :key="`${row.actionName}-${item.name}`" class="param-chip">
                          {{ item.name }}<span v-if="item.type"> / {{ item.type }}</span>{{ item.required ? ' *' : '' }}
                        </span>
                      </div>
                      <span v-else class="muted">无参数</span>
                    </template>
                  </el-table-column>
                </el-table>

                <div v-else class="binding-empty muted">当前没有可展示的动作定义。</div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="手动执行" name="execute">
            <div class="tab-pane-layout">
              <div class="detail-scroll">
                <el-alert
                  class="hint-alert"
                  type="info"
                  :closable="false"
                  show-icon
                  title="这里用于手动验证 Tool 的执行结果。参数和上下文都按 JSON 传入。"
                />

                <el-form label-position="top">
                  <el-form-item label="执行动作">
                    <el-select
                      v-model="executionForm.actionName"
                      class="full-width"
                      clearable
                      :disabled="form.actions.length === 0"
                      placeholder="如果当前只有一个动作，可以留空让系统自动解析"
                    >
                      <el-option
                        v-for="action in form.actions"
                        :key="action.actionName"
                        :label="`${action.actionName}${action.description ? ` / ${action.description}` : ''}`"
                        :value="action.actionName"
                      />
                    </el-select>
                  </el-form-item>

                  <el-form-item label="参数 JSON">
                    <el-input
                      v-model="executionForm.argumentsJson"
                      type="textarea"
                      :rows="8"
                      placeholder="{&#10;  &quot;keyword&quot;: &quot;SmartCrew&quot;&#10;}"
                    />
                  </el-form-item>

                  <el-form-item label="上下文 JSON">
                    <el-input
                      v-model="executionForm.contextJson"
                      type="textarea"
                      :rows="6"
                      placeholder="{&#10;  &quot;userId&quot;: 1001,&#10;  &quot;source&quot;: &quot;admin-debug&quot;&#10;}"
                    />
                  </el-form-item>

                  <div class="action-row">
                    <el-button
                      type="primary"
                      :loading="executing"
                      :disabled="!form.toolCode"
                      @click="submitExecution"
                    >
                      执行 Tool
                    </el-button>
                    <el-button plain @click="resetExecutionForm">重置输入</el-button>
                  </div>
                </el-form>

                <div v-if="executionResultText" class="result-shell">
                  <div class="binding-head">
                    <div>
                      <h4>执行结果</h4>
                      <p class="muted">{{ latestExecutionLabel }}</p>
                    </div>
                    <el-tag :type="lastExecutionResult?.success ? 'success' : 'danger'">
                      {{ lastExecutionResult?.success ? '成功' : '失败' }}
                    </el-tag>
                  </div>
                  <pre>{{ executionResultText }}</pre>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { ToolExecutionResultRecord, ToolRecord } from '../../types'

type ToolPageMode = 'empty' | 'create' | 'createFromCode' | 'edit'

const authStore = useAuthStore()
const tools = ref<ToolRecord[]>([])
const selectedToolCode = ref('')
const pageMode = ref<ToolPageMode>('empty')
const activeTab = ref('basic')
const saving = ref(false)
const executing = ref(false)
const lastExecutionResult = ref<ToolExecutionResultRecord>()
const persistedSnapshot = ref<ToolRecord>()

const filters = reactive({
  keyword: '',
  enabled: 'all'
})

const form = reactive<ToolRecord>(createEmptyForm())
const executionForm = reactive({
  actionName: '',
  argumentsJson: '{}',
  contextJson: '{\n  "source": "admin-tool-debug"\n}'
})

const panelTitle = computed(() => {
  if (pageMode.value === 'create') return '新增数据库 Tool 元数据'
  if (pageMode.value === 'createFromCode') return '为代码 Tool 创建数据库元数据'
  if (pageMode.value === 'edit') return '编辑 Tool 配置'
  return 'Tool 配置'
})

const panelDescription = computed(() => {
  if (pageMode.value === 'create') {
    return '可以直接创建新的数据库 Tool 元数据，用于补充名称、描述、风险等级与开关状态。'
  }
  if (pageMode.value === 'createFromCode') {
    return '当前 Tool 已有代码实现但尚未写入数据库配置，可以在这里补齐治理元数据。'
  }
  if (pageMode.value === 'edit') {
    return '左侧展示统一来源的 Tool 列表，右侧用于维护数据库元数据、查看动作并做手动执行验证。'
  }
  return '请先从左侧选择一个 Tool。'
})

const submitButtonText = computed(() => {
  return pageMode.value === 'create' || pageMode.value === 'createFromCode' ? '创建数据库元数据' : '保存数据库配置'
})

const filteredTools = computed(() => {
  return tools.value.filter((item) => {
    const keyword = filters.keyword.trim().toLowerCase()
    const matchesKeyword = !keyword
      || item.toolName.toLowerCase().includes(keyword)
      || item.toolCode.toLowerCase().includes(keyword)
      || (item.description || '').toLowerCase().includes(keyword)
    const matchesEnabled = filters.enabled === 'all' || String(item.enabled) === filters.enabled
    return matchesKeyword && matchesEnabled
  })
})

const executionResultText = computed(() => {
  if (!lastExecutionResult.value) return ''
  return JSON.stringify(lastExecutionResult.value, null, 2)
})

const latestExecutionLabel = computed(() => {
  if (!lastExecutionResult.value) return ''
  const duration = lastExecutionResult.value.durationMs ?? 0
  return `${lastExecutionResult.value.toolCode}#${lastExecutionResult.value.actionName || '自动解析'} / ${duration} ms`
})

onMounted(async () => {
  await loadTools()
})

function createEmptyForm(): ToolRecord {
  return {
    toolCode: '',
    toolName: '',
    description: '',
    beanName: '',
    riskLevel: 'MEDIUM',
    enabled: true,
    configJson: '{}',
    sourceStatus: 'DB_ONLY',
    hasCodeBean: false,
    hasDatabaseConfig: true,
    executable: false,
    resolveError: '',
    actions: []
  }
}

function cloneTool(payload: Partial<ToolRecord> = {}): ToolRecord {
  return {
    ...createEmptyForm(),
    ...payload,
    actions: payload.actions ? payload.actions.map((item) => ({
      ...item,
      parameters: item.parameters ? item.parameters.map((parameter) => ({ ...parameter })) : []
    })) : []
  }
}

function applyForm(payload: Partial<ToolRecord> = {}) {
  Object.assign(form, cloneTool(payload))
  form.configJson = payload.configJson || '{}'
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

async function loadTools(preferredCode?: string) {
  try {
    const response = await adminPortalApi.listTools(authStore.adminToken)
    tools.value = response.rows

    if (pageMode.value === 'create' && !preferredCode) {
      return
    }

    const targetCode = preferredCode || selectedToolCode.value || filteredTools.value[0]?.toolCode || tools.value[0]?.toolCode
    if (!targetCode) {
      resetToEmpty()
      return
    }
    await loadToolDetail(targetCode)
  } catch (error) {
    handleError(error)
  }
}

async function loadToolDetail(code: string) {
  try {
    const detail = await adminPortalApi.getTool(authStore.adminToken, code)
    selectedToolCode.value = detail.toolCode
    persistedSnapshot.value = cloneTool(detail)
    applyForm(detail)
    pageMode.value = detail.hasDatabaseConfig ? 'edit' : 'createFromCode'
    activeTab.value = 'basic'
    resetExecutionForm()
  } catch (error) {
    handleError(error)
  }
}

async function handleCurrentChange(row?: ToolRecord) {
  if (!row) return
  await loadToolDetail(row.toolCode)
}

function startCreateTool() {
  selectedToolCode.value = ''
  persistedSnapshot.value = undefined
  pageMode.value = 'create'
  activeTab.value = 'basic'
  applyForm(createEmptyForm())
  resetExecutionForm()
}

function startCreateFromCode(row: ToolRecord) {
  selectedToolCode.value = row.toolCode
  persistedSnapshot.value = undefined
  pageMode.value = 'createFromCode'
  activeTab.value = 'basic'
  applyForm({
    ...row,
    sourceStatus: 'CODE_ONLY',
    hasCodeBean: true,
    hasDatabaseConfig: false,
    configJson: row.configJson || '{}'
  })
  resetExecutionForm()
}

function resetToEmpty() {
  selectedToolCode.value = ''
  persistedSnapshot.value = undefined
  pageMode.value = 'empty'
  activeTab.value = 'basic'
  applyForm(createEmptyForm())
  resetExecutionForm()
}

function resetCurrentForm() {
  if (pageMode.value === 'edit' && persistedSnapshot.value) {
    applyForm(persistedSnapshot.value)
    return
  }
  if (pageMode.value === 'createFromCode') {
    const target = tools.value.find((item) => item.toolCode === selectedToolCode.value)
    if (target) {
      startCreateFromCode(target)
      return
    }
  }
  applyForm(createEmptyForm())
}

function resetExecutionForm() {
  executionForm.actionName = form.actions.length === 1 ? form.actions[0].actionName : ''
  executionForm.argumentsJson = '{}'
  executionForm.contextJson = '{\n  "source": "admin-tool-debug"\n}'
  lastExecutionResult.value = undefined
}

async function submitForm() {
  if (!form.toolCode.trim()) {
    ElMessage.warning('请输入 Tool 编码')
    return
  }
  if (!form.toolName.trim()) {
    ElMessage.warning('请输入 Tool 名称')
    return
  }
  if (!form.description?.trim()) {
    ElMessage.warning('请输入 Tool 描述')
    return
  }
  if (!form.beanName?.trim()) {
    ElMessage.warning('请填写 Spring Bean 名称')
    return
  }

  saving.value = true
  try {
    const payload: Partial<ToolRecord> = {
      toolCode: form.toolCode.trim(),
      toolName: form.toolName.trim(),
      description: form.description?.trim() || '',
      beanName: form.beanName?.trim() || '',
      riskLevel: form.riskLevel || 'MEDIUM',
      enabled: form.enabled,
      configJson: form.configJson || '{}'
    }

    let toolCode = form.toolCode
    if (pageMode.value === 'create' || pageMode.value === 'createFromCode') {
      const created = await adminPortalApi.createTool(authStore.adminToken, payload)
      toolCode = created.toolCode
      ElMessage.success('Tool 数据库元数据创建成功')
    } else {
      const updated = await adminPortalApi.updateTool(authStore.adminToken, form.toolCode, payload)
      toolCode = updated.toolCode
      ElMessage.success('Tool 数据库配置已保存')
    }

    pageMode.value = 'edit'
    await loadTools(toolCode)
  } catch (error) {
    handleError(error)
  } finally {
    saving.value = false
  }
}

async function submitExecution() {
  if (!form.toolCode) {
    ElMessage.warning('请先选择一个 Tool')
    return
  }

  executing.value = true
  try {
    const argumentsPayload = parseJsonInput(executionForm.argumentsJson, '参数 JSON')
    const contextPayload = parseJsonInput(executionForm.contextJson, '上下文 JSON')
    const result = await adminPortalApi.executeTool(authStore.adminToken, form.toolCode, {
      actionName: executionForm.actionName || undefined,
      arguments: argumentsPayload,
      executionContext: contextPayload
    })
    lastExecutionResult.value = result
    ElMessage.success(result.success ? 'Tool 执行成功' : 'Tool 执行返回失败结果')
  } catch (error) {
    handleError(error)
  } finally {
    executing.value = false
  }
}

function parseJsonInput(value: string, label: string) {
  const text = value.trim()
  if (!text) {
    return {}
  }
  try {
    return JSON.parse(text) as Record<string, unknown>
  } catch {
    throw new Error(`${label} 不是合法的 JSON`)
  }
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
.tool-grid {
  display: grid;
  grid-template-columns: minmax(340px, 0.92fr) minmax(0, 1.08fr);
  gap: 18px;
  height: 100%;
  min-height: 0;
}

.tool-list-card,
.tool-detail-card {
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

.filter-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 12px;
  margin-bottom: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.form-row--compact {
  align-items: end;
}

.full-width {
  width: 100%;
}

.status-stack,
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tool-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  :deep(.el-tab-pane) {
    display: flex;
    flex: 1;
    min-height: 0;
    height: 100%;
    overflow: hidden;
  }
}

.tab-pane-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.detail-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 6px;
}

.hint-alert,
.actions-table {
  margin-bottom: 16px;
}

.binding-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;

  h4 {
    margin: 0 0 8px;
    font-family: var(--sc-font-title);
    font-size: 1.08rem;
  }

  p {
    margin: 0;
  }
}

.param-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.param-chip {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.55);
  color: var(--sc-text-soft);
  font-size: 13px;
}

.binding-empty {
  padding: 16px 4px;
}

.result-shell {
  margin-top: 20px;

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
    max-width: 500px;
  }
}

@media (max-width: 1320px) {
  .tool-grid {
    grid-template-columns: 1fr;
    height: auto;
  }
}

@media (max-width: 860px) {
  .filter-bar,
  .form-row {
    grid-template-columns: 1fr;
  }

  .card-head,
  .binding-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
