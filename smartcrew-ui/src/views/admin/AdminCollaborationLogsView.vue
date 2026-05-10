<template>
  <div class="collaboration-grid">
    <GlassPanel panel-class="admin-card log-list-card">
      <div class="card-head">
        <div>
          <h3>协作日志</h3>
          <p class="muted">查看多智能体协作中的调度、执行与记忆读写轨迹。</p>
        </div>
        <div class="summary-badge">
          <span>当前选中</span>
          <strong>{{ activeTraceId || '未选择' }}</strong>
        </div>
      </div>

      <div class="filter-row">
        <el-input v-model="filters.traceId" placeholder="Trace ID" clearable />
        <el-input v-model="filters.rootSessionId" placeholder="会话 ID" clearable />
        <el-input v-model="filters.agentCode" placeholder="Agent Code" clearable />
        <el-select v-model="filters.stepType" placeholder="步骤类型" clearable>
          <el-option v-for="item in stepTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="关键词" clearable />
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          range-separator="至"
          value-format="YYYY-MM-DDTHH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          clearable
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <div class="table-shell">
        <el-table
          :data="logs"
          stripe
          highlight-current-row
          height="100%"
          @current-change="handleCurrentChange"
        >
          <el-table-column prop="traceId" label="Trace ID" min-width="200" show-overflow-tooltip />
          <el-table-column prop="rootSessionId" label="会话 ID" min-width="180" show-overflow-tooltip />
          <el-table-column prop="agentCode" label="Agent" width="130" />
          <el-table-column prop="stepType" label="步骤类型" width="140" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column label="开始时间" min-width="180">
            <template #default="{ row }">{{ formatDate(row.startTime) }}</template>
          </el-table-column>
          <el-table-column prop="durationMs" label="耗时(ms)" width="110" />
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

    <GlassPanel panel-class="admin-card trace-card">
      <div class="card-head">
        <div>
          <h3>步骤时间线</h3>
          <p class="muted">Trace：{{ activeTraceId || '未选择协作记录' }}</p>
        </div>
        <div class="summary-badge">
          <span>步骤数</span>
          <strong>{{ steps.length }}</strong>
        </div>
      </div>

      <div v-if="steps.length" class="timeline-list">
        <article v-for="step in steps" :key="step.id || `${step.traceId}-${step.stepType}-${step.startTime}`" class="timeline-card">
          <div class="timeline-head">
            <div>
              <strong>{{ step.stepName || step.stepType }}</strong>
              <p class="muted">
                {{ step.agentCode }} · {{ step.stepType }} · {{ formatDate(step.startTime) }}
              </p>
            </div>
            <el-tag :type="statusTagType(step.status)" effect="dark">{{ step.status }}</el-tag>
          </div>

          <div class="timeline-meta">
            <span>会话 ID：{{ step.rootSessionId }}</span>
            <span v-if="step.durationMs != null">耗时：{{ step.durationMs }} ms</span>
            <span v-if="step.parentStepId != null">父步骤：{{ step.parentStepId }}</span>
          </div>

          <div class="timeline-block">
            <h4>输入摘要</h4>
            <p>{{ step.inputSnapshot || '无' }}</p>
          </div>

          <div class="timeline-block">
            <h4>决策摘要</h4>
            <p>{{ step.decisionSnapshot || '无' }}</p>
          </div>

          <div class="timeline-block">
            <h4>输出摘要</h4>
            <p>{{ step.outputSnapshot || '无' }}</p>
          </div>

          <div v-if="step.errorMessage" class="timeline-block timeline-block--error">
            <h4>错误信息</h4>
            <p>{{ step.errorMessage }}</p>
          </div>
        </article>
      </div>

      <div v-else class="empty-text muted">请选择左侧协作记录查看步骤详情。</div>
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { CollaborationLogRecord, CollaborationLogStepRecord } from '../../types'

const pageSizeOptions = [10, 30, 50, 100, 250]
const stepTypeOptions = [
  { label: '调度', value: 'DISPATCH' },
  { label: '记忆读取', value: 'MEMORY_READ' },
  { label: '决策', value: 'DECISION' },
  { label: '执行', value: 'EXECUTION' },
  { label: '工具调用', value: 'TOOL_CALL' },
  { label: '记忆回写', value: 'MEMORY_WRITE' },
  { label: '最终响应', value: 'FINAL_RESPONSE' }
]
const statusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '运行中', value: 'RUNNING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '跳过', value: 'SKIPPED' }
]

const authStore = useAuthStore()
const filters = reactive({
  traceId: '',
  rootSessionId: '',
  agentCode: '',
  stepType: '',
  status: '',
  keyword: ''
})
const pager = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})
const dateRange = ref<[string, string] | []>([])
const logs = ref<CollaborationLogRecord[]>([])
const steps = ref<CollaborationLogStepRecord[]>([])
const activeTraceId = ref('')
const activeLog = ref<CollaborationLogRecord | null>(null)

onMounted(loadLogs)

/** 加载协作日志列表，并将第一行设为当前选中行以加载步骤时间线 */
async function loadLogs() {
  try {
    const response = await adminPortalApi.listCollaborationLogs(authStore.adminToken, {
      traceId: filters.traceId || undefined,
      rootSessionId: filters.rootSessionId || undefined,
      agentCode: filters.agentCode || undefined,
      stepType: filters.stepType || undefined,
      status: filters.status || undefined,
      keyword: filters.keyword || undefined,
      startTimeFrom: dateRange.value[0] || undefined,
      startTimeTo: dateRange.value[1] || undefined,
      pageNum: pager.pageNum,
      pageSize: pager.pageSize
    })
    logs.value = response.rows
    pager.total = response.total
    if (response.rows.length > 0) {
      const selectedTraceId = activeLog.value?.traceId
      const matchedRow =
        selectedTraceId ? response.rows.find((row) => row.traceId === selectedTraceId) ?? response.rows[0] : response.rows[0]
      await handleCurrentChange(matchedRow)
    } else {
      activeLog.value = null
      activeTraceId.value = ''
      steps.value = []
    }
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

/** 搜索按钮点击：重置至第一页并重新加载 */
async function handleSearch() {
  pager.pageNum = 1
  await loadLogs()
}

/** 分页页码变化时重新加载 */
async function handlePageChange(pageNum: number) {
  pager.pageNum = pageNum
  await loadLogs()
}

/** 每页条数变化时重置至第一页并重新加载 */
async function handleSizeChange(pageSize: number) {
  pager.pageSize = pageSize
  pager.pageNum = 1
  await loadLogs()
}

/** 选中某行协作日志时加载其步骤时间线详情 */
async function handleCurrentChange(row?: CollaborationLogRecord) {
  if (!row) return
  activeLog.value = row
  activeTraceId.value = row.traceId
  try {
    steps.value = await adminPortalApi.listCollaborationLogSteps(authStore.adminToken, row.traceId)
  } catch (error) {
    steps.value = []
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

/** 根据步骤状态返回对应的 Element Plus Tag 类型 */
function statusTagType(status?: string) {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'RUNNING':
      return 'warning'
    case 'SKIPPED':
      return 'info'
    default:
      return 'info'
  }
}

/** 格式化 ISO 日期为中文本地时间字符串 */
function formatDate(value?: string) {
  if (!value) return '暂无'
  return new Date(value).toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.collaboration-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(0, 0.98fr);
  gap: 18px;
  height: 100%;
  min-height: 0;
}

.log-list-card,
.trace-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.summary-badge {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  min-width: 120px;
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.48), rgba(255, 255, 255, 0.16)),
    rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.3);
  text-align: right;

  span {
    font-size: 0.78rem;
    color: var(--sc-text-soft);
  }

  strong {
    font-size: 1rem;
  }
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.table-shell {
  flex: 1;
  min-height: 0;
}

.timeline-list {
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-right: 6px;
}

.timeline-card {
  padding: 16px;
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0.14)),
    rgba(255, 255, 255, 0.08);
}

.timeline-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;

  h4 {
    margin: 0;
  }

  p {
    margin: 6px 0 0;
  }
}

.timeline-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin: 12px 0 0;
  color: var(--sc-text-soft);
  font-size: 0.92rem;
}

.timeline-block {
  margin-top: 14px;

  h4 {
    margin: 0 0 8px;
    font-size: 0.96rem;
  }

  p {
    margin: 0;
    white-space: pre-wrap;
    line-height: 1.8;
  }
}

.timeline-block--error p {
  color: #b42318;
}

.empty-text {
  flex: 1;
  display: grid;
  place-items: center;
  padding: 16px 4px;
  text-align: center;
}

@media (max-width: 1200px) {
  .collaboration-grid {
    grid-template-columns: 1fr;
    height: auto;
  }

  .filter-row {
    grid-template-columns: 1fr;
  }
}
</style>
