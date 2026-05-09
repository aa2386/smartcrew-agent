<template>
  <div class="logs-page">
    <!-- 筛选区域 -->
    <GlassPanel panel-class="admin-card filter-card">
      <div class="card-head">
        <div>
          <h3>Agent 行为日志</h3>
          <p class="muted">按会话、trace、Agent、事件类型等维度检索多 Agent 协作中的行为时间线。</p>
        </div>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <div class="filter-grid">
        <el-input v-model="filters.traceId" placeholder="Trace ID" clearable />
        <el-input v-model="filters.sessionId" placeholder="Session ID" clearable />
        <el-input v-model="filters.userId" placeholder="用户 ID" clearable />
        <el-select v-model="filters.agentCode" placeholder="Agent 编码" clearable filterable>
          <el-option
            v-for="agent in agentOptions"
            :key="agent.agentCode"
            :label="`${agent.agentName} / ${agent.agentCode}`"
            :value="agent.agentCode"
          />
        </el-select>
        <el-select v-model="filters.eventType" placeholder="事件类型" clearable>
          <el-option
            v-for="item in eventTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="filters.eventStatus" placeholder="事件状态" clearable>
          <el-option
            v-for="item in eventStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-date-picker
          v-model="filters.timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          clearable
        />
      </div>
    </GlassPanel>

    <!-- 日志表格 -->
    <GlassPanel panel-class="admin-card table-card">
      <div class="table-shell">
        <el-table
          :data="logs"
          stripe
          highlight-current-row
          height="100%"
          @current-change="handleCurrentChange"
        >
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
          </el-table-column>
          <el-table-column prop="traceId" label="Trace ID" width="260" show-overflow-tooltip />
          <el-table-column prop="sessionId" label="Session ID" width="220" show-overflow-tooltip />
          <el-table-column prop="userId" label="用户" width="100" />
          <el-table-column prop="agentCode" label="Agent" width="150" show-overflow-tooltip />
          <el-table-column label="事件类型" width="130">
            <template #default="{ row }">{{ eventTypeLabel(row.eventType) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="eventStatusTagType(row.eventStatus)" size="small">
                {{ eventStatusLabel(row.eventStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="耗时" width="90">
            <template #default="{ row }">
              {{ row.durationMs != null ? `${row.durationMs} ms` : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="toolCode" label="工具" width="150" show-overflow-tooltip />
          <el-table-column prop="eventSummary" label="摘要" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination
        :current-page="pager.pageNum"
        :page-size="pager.pageSize"
        :page-sizes="pageSizeOptions"
        :total="pager.total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </GlassPanel>

    <!-- 日志详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="日志详情"
      direction="rtl"
      size="520px"
      class="log-drawer"
    >
      <template v-if="detailRow">
        <div class="detail-section">
          <h4>基本信息</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="ID">{{ detailRow.id }}</el-descriptions-item>
            <el-descriptions-item label="Trace ID">
              <div class="trace-id-row">
                <span>{{ detailRow.traceId }}</span>
                <el-button link type="primary" size="small" @click="loadTrace">查看完整时间线</el-button>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="Session ID">{{ detailRow.sessionId }}</el-descriptions-item>
            <el-descriptions-item label="用户 ID">{{ detailRow.userId }}</el-descriptions-item>
            <el-descriptions-item label="Agent 编码">{{ detailRow.agentCode }}</el-descriptions-item>
            <el-descriptions-item label="来源 Agent">{{ detailRow.sourceAgent || '-' }}</el-descriptions-item>
            <el-descriptions-item label="目标 Agent">{{ detailRow.targetAgent || '-' }}</el-descriptions-item>
            <el-descriptions-item label="事件类型">{{ eventTypeLabel(detailRow.eventType) }}</el-descriptions-item>
            <el-descriptions-item label="事件状态">
              <el-tag :type="eventStatusTagType(detailRow.eventStatus)" size="small">
                {{ eventStatusLabel(detailRow.eventStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="耗时">{{ detailRow.durationMs != null ? `${detailRow.durationMs} ms` : '-' }}</el-descriptions-item>
            <el-descriptions-item label="工具编码">{{ detailRow.toolCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="动作名称">{{ detailRow.actionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="时间">{{ formatDate(detailRow.createTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <h4>事件摘要</h4>
          <p class="detail-text">{{ detailRow.eventSummary || '暂无' }}</p>
        </div>

        <div v-if="detailRow.errorMessage" class="detail-section">
          <h4>错误信息</h4>
          <el-alert type="error" :closable="false" show-icon :title="detailRow.errorMessage" />
        </div>

        <div class="detail-section">
          <h4>扩展数据</h4>
          <pre class="detail-json">{{ formatMetadata(detailRow.metadataJson) }}</pre>
        </div>
      </template>

      <!-- Trace 时间线 -->
      <template v-if="traceLoading">
        <div class="loading-state">
          <p class="muted">正在加载 trace 完整时间线...</p>
        </div>
      </template>
      <template v-else-if="traceRecords.length > 0">
        <div class="detail-section">
          <h4>Trace 时间线 ({{ traceRecords.length }} 条)</h4>
          <div class="trace-timeline">
            <div
              v-for="(item, index) in traceRecords"
              :key="index"
              class="timeline-item"
            >
              <div class="timeline-dot" :class="eventStatusDotClass(item.eventStatus)" />
              <div class="timeline-body">
                <div class="timeline-head">
                  <strong>{{ eventTypeLabel(item.eventType) }}</strong>
                  <el-tag :type="eventStatusTagType(item.eventStatus)" size="small">
                    {{ eventStatusLabel(item.eventStatus) }}
                  </el-tag>
                </div>
                <p class="muted">{{ formatDate(item.createTime) }}</p>
                <p v-if="item.eventSummary">{{ item.eventSummary }}</p>
                <p v-if="item.toolCode" class="muted">工具: {{ item.toolCode }}{{ item.actionName ? ` / ${item.actionName}` : '' }}</p>
                <p v-if="item.durationMs != null" class="muted">耗时: {{ item.durationMs }} ms</p>
                <p v-if="item.errorMessage" class="error-text">{{ item.errorMessage }}</p>
              </div>
            </div>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { AgentBehaviorLogRecord } from '../../types'

// ---- 常量 ----

const pageSizeOptions = [10, 30, 50, 100, 250]

const eventTypeMap: Record<string, string> = {
  SESSION_RECEIVED: '会话接入',
  AGENT_STARTED: 'Agent 开始',
  AGENT_FINISHED: 'Agent 完成',
  DELEGATION_STARTED: '委托发起',
  DELEGATION_FINISHED: '委托返回',
  TOOL_STARTED: '工具调用开始',
  TOOL_FINISHED: '工具调用完成',
  MEMORY_READ: '记忆读取',
  MEMORY_WRITE: '记忆写入',
  TASK_CREATED: '任务创建',
  TASK_UPDATED: '任务更新',
  ERROR: '异常'
}

const eventTypeOptions = Object.entries(eventTypeMap).map(([value, label]) => ({ value, label }))

const eventStatusMap: Record<string, string> = {
  SUCCESS: '成功',
  FAILED: '失败',
  SKIPPED: '跳过',
  NEEDS_CONFIRMATION: '待确认'
}

const eventStatusOptions = Object.entries(eventStatusMap).map(([value, label]) => ({ value, label }))

function eventTypeLabel(type: string) {
  return eventTypeMap[type] || type || '未知'
}

function eventStatusLabel(status: string) {
  return eventStatusMap[status] || status || '未知'
}

function eventStatusTagType(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'SKIPPED') return 'info'
  if (status === 'NEEDS_CONFIRMATION') return 'warning'
  return 'info'
}

function eventStatusDotClass(status: string) {
  if (status === 'SUCCESS') return 'dot-success'
  if (status === 'FAILED') return 'dot-danger'
  if (status === 'SKIPPED') return 'dot-info'
  if (status === 'NEEDS_CONFIRMATION') return 'dot-warning'
  return 'dot-info'
}

// ---- 状态 ----

const authStore = useAuthStore()

const filters = reactive({
  traceId: '',
  sessionId: '',
  userId: '',
  agentCode: '',
  eventType: '',
  eventStatus: '',
  timeRange: null as [string, string] | null
})

const pager = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const logs = ref<AgentBehaviorLogRecord[]>([])
const agentOptions = ref<Array<{ agentCode: string; agentName: string }>>([])

// 详情抽屉
const drawerVisible = ref(false)
const detailRow = ref<AgentBehaviorLogRecord | null>(null)

// Trace 时间线
const traceLoading = ref(false)
const traceRecords = ref<AgentBehaviorLogRecord[]>([])

// ---- 生命周期 ----

onMounted(() => {
  loadAgents()
  loadLogs()
})

// ---- 数据加载 ----

async function loadAgents() {
  try {
    const response = await adminPortalApi.listAgents(authStore.adminToken)
    agentOptions.value = response.rows.map((item) => ({
      agentCode: item.agentCode,
      agentName: item.agentName
    }))
  } catch {
    // 非关键路径，静默处理
  }
}

async function loadLogs() {
  try {
    const params = {
      traceId: filters.traceId || undefined,
      sessionId: filters.sessionId || undefined,
      userId: filters.userId || undefined,
      agentCode: filters.agentCode || undefined,
      eventType: filters.eventType || undefined,
      eventStatus: filters.eventStatus || undefined,
      startTime: filters.timeRange?.[0] || undefined,
      endTime: filters.timeRange?.[1] || undefined,
      pageNum: pager.pageNum,
      pageSize: pager.pageSize
    }
    const response = await adminPortalApi.listAgentLogs(authStore.adminToken, params)
    logs.value = response.rows
    pager.total = response.total
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
    logs.value = []
    pager.total = 0
  }
}

async function loadTrace() {
  if (!detailRow.value?.traceId) return
  traceLoading.value = true
  traceRecords.value = []
  try {
    const result = await adminPortalApi.listAgentLogTrace(authStore.adminToken, detailRow.value.traceId)
    traceRecords.value = result.logs || []
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  } finally {
    traceLoading.value = false
  }
}

// ---- 事件处理 ----

async function handleSearch() {
  pager.pageNum = 1
  await loadLogs()
}

async function handlePageChange(pageNum: number) {
  pager.pageNum = pageNum
  await loadLogs()
}

async function handleSizeChange(pageSize: number) {
  pager.pageSize = pageSize
  pager.pageNum = 1
  await loadLogs()
}

function handleCurrentChange(row?: AgentBehaviorLogRecord) {
  if (!row) return
  detailRow.value = row
  drawerVisible.value = true
}

function openDetail(row: AgentBehaviorLogRecord) {
  detailRow.value = row
  traceRecords.value = []
  drawerVisible.value = true
}

// ---- 格式化 ----

function formatDate(value?: string) {
  if (!value) return '暂无'
  return new Date(value).toLocaleString('zh-CN')
}

function formatMetadata(jsonStr?: string) {
  if (!jsonStr) return '暂无'
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch {
    return jsonStr
  }
}
</script>

<style scoped lang="scss">
.logs-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: 100%;
  min-height: 0;
}

.filter-card {
  flex-shrink: 0;
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
  }
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.table-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.table-shell {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

// 详情抽屉
.log-drawer {
  .detail-section {
    margin-bottom: 24px;

    h4 {
      margin: 0 0 12px;
      font-family: var(--sc-font-title);
      font-size: 16px;
    }
  }

  .detail-text {
    margin: 0;
    line-height: 1.8;
    white-space: pre-wrap;
  }

  .detail-json {
    margin: 0;
    padding: 14px;
    border-radius: var(--sc-radius-sm);
    background: rgba(255, 255, 255, 0.32);
    border: 1px solid rgba(255, 255, 255, 0.48);
    font-size: 13px;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-all;
    max-height: 320px;
    overflow: auto;
  }

  .trace-id-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .loading-state {
    padding: 32px 0;
    text-align: center;
  }
}

// Trace 时间线
.trace-timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-item {
  display: flex;
  gap: 14px;
}

.timeline-dot {
  flex-shrink: 0;
  width: 12px;
  height: 12px;
  margin-top: 6px;
  border-radius: 999px;
  box-shadow: var(--sc-shadow-soft);

  &.dot-success {
    background: var(--sc-support);
  }
  &.dot-danger {
    background: #ef4444;
  }
  &.dot-info {
    background: var(--sc-primary);
  }
  &.dot-warning {
    background: var(--sc-accent);
  }
}

.timeline-body {
  flex: 1;
  min-width: 0;
  padding: 12px 16px;
  border-radius: var(--sc-radius-sm);
  background: rgba(255, 255, 255, 0.26);
  border: 1px solid rgba(255, 255, 255, 0.44);

  p {
    margin: 6px 0 0;
    line-height: 1.7;

    &.error-text {
      color: #ef4444;
    }
  }
}

.timeline-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.error-text {
  color: #ef4444;
}

@media (max-width: 1200px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .card-head {
    flex-direction: column;
  }
}
</style>
