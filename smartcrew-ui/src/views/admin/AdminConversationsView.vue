<template>
  <div class="conversation-grid">
    <GlassPanel panel-class="admin-card">
      <div class="card-head">
        <div>
          <h3>会话检索</h3>
          <p class="muted">按用户、来源平台和关键词快速查看后台消息记录。</p>
        </div>
      </div>

      <div class="filter-row">
        <el-input v-model="filters.userId" placeholder="用户 ID" clearable />
        <el-select v-model="filters.provider" placeholder="来源平台" clearable>
          <el-option label="Web" value="WEB" />
          <el-option label="飞书" value="FEISHU" />
          <el-option label="企业微信" value="WECOM" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="关键词 / Session ID" clearable />
        <el-button type="primary" @click="loadSessions">查询</el-button>
      </div>

      <el-table :data="sessions" stripe highlight-current-row @current-change="handleCurrentChange">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="sessionId" label="Session ID" min-width="220" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column prop="messageCount" label="消息数" width="100" />
        <el-table-column label="最近时间" min-width="180">
          <template #default="{ row }">{{ formatDate(row.lastMessageAt) }}</template>
        </el-table-column>
      </el-table>
    </GlassPanel>

    <GlassPanel panel-class="admin-card">
      <div class="card-head">
        <div>
          <h3>消息详情</h3>
          <p class="muted">当前会话：{{ activeSessionId || '未选择会话' }}</p>
        </div>
      </div>

      <div v-if="messages.length" class="message-list">
        <article v-for="(item, index) in messages" :key="`${item.role}-${index}-${item.createTime}`" class="timeline-card">
          <div class="timeline-head">
            <strong>{{ item.role === 'assistant' ? '智能体回复' : '用户消息' }}</strong>
            <span>{{ formatDate(item.createTime) }}</span>
          </div>
          <p>{{ item.content }}</p>
        </article>
      </div>

      <div v-else class="empty-text muted">请选择一条会话记录后查看详细消息。</div>
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { ChatMessage, ChatSession } from '../../types'

const authStore = useAuthStore()
const filters = reactive({
  userId: '',
  provider: '',
  keyword: ''
})
const sessions = ref<ChatSession[]>([])
const messages = ref<ChatMessage[]>([])
const activeSessionId = ref('')

onMounted(loadSessions)

async function loadSessions() {
  try {
    const response = await adminPortalApi.listConversationSessions(authStore.adminToken, {
      userId: Number(filters.userId) || undefined,
      provider: filters.provider || undefined,
      keyword: filters.keyword || undefined
    })
    sessions.value = response.rows
    messages.value = []
    activeSessionId.value = ''
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function handleCurrentChange(row?: ChatSession) {
  if (!row) return
  activeSessionId.value = row.sessionId
  try {
    messages.value = await adminPortalApi.listConversationMessages(authStore.adminToken, {
      userId: Number(filters.userId) || undefined,
      sessionId: row.sessionId
    })
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

function formatDate(value?: string) {
  if (!value) return '暂无'
  return new Date(value).toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.conversation-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: 18px;
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

.filter-row {
  display: grid;
  grid-template-columns: 160px 180px minmax(0, 1fr) 100px;
  gap: 12px;
  margin-bottom: 16px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.timeline-card {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.58);

  p {
    margin: 12px 0 0;
    line-height: 1.8;
    white-space: pre-wrap;
  }
}

.timeline-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: var(--sc-text-soft);
}

.empty-text {
  padding: 16px 4px;
}

@media (max-width: 1200px) {
  .conversation-grid {
    grid-template-columns: 1fr;
  }

  .filter-row {
    grid-template-columns: 1fr;
  }
}
</style>
