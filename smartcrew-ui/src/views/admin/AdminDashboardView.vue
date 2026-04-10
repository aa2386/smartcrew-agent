<template>
  <div class="dashboard-grid">
    <section class="stats-grid">
      <button
        v-for="card in statsCards"
        :key="card.label"
        class="stat-button"
        type="button"
        @click="router.push(card.route)"
      >
        <GlassPanel panel-class="admin-card stat-card stat-card--interactive">
          <span class="muted">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <p>{{ card.description }}</p>
        </GlassPanel>
      </button>
    </section>

    <GlassPanel panel-class="admin-card overview-card">
      <div class="card-head">
        <div>
          <h3>近期会话</h3>
          <p class="muted">快速查看最近活跃的系统会话记录。</p>
        </div>
        <el-button plain @click="router.push('/admin/conversations')">查看全部</el-button>
      </div>

      <div class="table-shell">
        <el-table :data="recentSessions" stripe height="100%">
          <el-table-column prop="title" label="会话标题" min-width="180" />
          <el-table-column prop="source" label="来源" width="100" />
          <el-table-column prop="messageCount" label="消息数" width="100" />
          <el-table-column label="最近时间" min-width="160">
            <template #default="{ row }">{{ formatDate(row.lastMessageAt) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination
        :current-page="sessionPager.pageNum"
        :page-size="sessionPager.pageSize"
        :page-sizes="pageSizeOptions"
        :total="sessionPager.total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handleSessionPageChange"
        @size-change="handleSessionSizeChange"
      />
    </GlassPanel>

    <GlassPanel panel-class="admin-card quick-card">
      <div class="card-head">
        <div>
          <h3>本期建设重点</h3>
          <p class="muted">当前版本已经具备公众页与后台管理的基础框架。</p>
        </div>
      </div>
      <ul class="focus-list">
        <li>统一用户体系，兼容本地注册与第三方平台身份映射。</li>
        <li>可视化管理 Agent、Prompt、用户偏好和消息记录。</li>
        <li>前后端保持可裁剪结构，便于未来仅开放 <code>/api/v1/*</code>。</li>
      </ul>
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { ChatSession } from '../../types'

const pageSizeOptions = [10, 30, 50, 100, 250]

const router = useRouter()
const authStore = useAuthStore()
const stats = ref({
  users: 0,
  agents: 0,
  prompts: 0,
  sessions: 0
})
const recentSessions = ref<ChatSession[]>([])
const sessionPager = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const statsCards = computed(() => [
  {
    label: '平台用户',
    value: `${stats.value.users}`,
    description: '包含本地账号与第三方映射用户。',
    route: '/admin/users'
  },
  {
    label: 'Agent 数量',
    value: `${stats.value.agents}`,
    description: '支持查询、配置与启停管理。',
    route: '/admin/agents'
  },
  {
    label: 'Prompt 数量',
    value: `${stats.value.prompts}`,
    description: '统一维护模板库与分类版本。',
    route: '/admin/prompts'
  },
  {
    label: '会话记录',
    value: `${stats.value.sessions}`,
    description: '公众页与平台接入统一进入审计链路。',
    route: '/admin/conversations'
  }
])

onMounted(async () => {
  await Promise.all([loadStats(), loadRecentSessions()])
})

async function loadStats() {
  try {
    const [users, agents, prompts, sessions] = await Promise.all([
      adminPortalApi.listUsers(authStore.adminToken, { pageNum: 1, pageSize: 1 }),
      adminPortalApi.listAgents(authStore.adminToken),
      adminPortalApi.listPrompts(authStore.adminToken),
      adminPortalApi.listConversationSessions(authStore.adminToken, { pageNum: 1, pageSize: 1 })
    ])
    stats.value = {
      users: users.total,
      agents: agents.rows.length,
      prompts: prompts.total,
      sessions: sessions.total
    }
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function loadRecentSessions() {
  try {
    const response = await adminPortalApi.listConversationSessions(authStore.adminToken, {
      pageNum: sessionPager.pageNum,
      pageSize: sessionPager.pageSize
    })
    recentSessions.value = response.rows
    sessionPager.total = response.total
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function handleSessionPageChange(pageNum: number) {
  sessionPager.pageNum = pageNum
  await loadRecentSessions()
}

async function handleSessionSizeChange(pageSize: number) {
  sessionPager.pageSize = pageSize
  sessionPager.pageNum = 1
  await loadRecentSessions()
}

function formatDate(value?: string) {
  if (!value) return '暂无'
  return new Date(value).toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.dashboard-grid {
  display: grid;
  gap: 18px;
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding-right: 6px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-button {
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.stat-card--interactive {
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.stat-button:hover .stat-card--interactive,
.stat-button:focus-visible .stat-card--interactive {
  transform: translateY(-2px);
  box-shadow: 0 22px 46px rgba(118, 136, 163, 0.22);
}

.stat-button:focus-visible {
  outline: none;
}

.stat-card {
  strong {
    display: block;
    margin: 14px 0 10px;
    font-family: var(--sc-font-title);
    font-size: 2.2rem;
  }

  p {
    margin: 0;
    color: var(--sc-text-soft);
    line-height: 1.7;
  }
}

.overview-card,
.quick-card {
  display: flex;
  flex-direction: column;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;

  h3 {
    margin: 0 0 8px;
    font-family: var(--sc-font-title);
    font-size: 1.35rem;
  }

  p {
    margin: 0;
  }
}

.table-shell {
  flex: 1;
  min-height: 0;
}

.focus-list {
  margin: 0;
  padding-left: 20px;
  color: var(--sc-text-soft);
  line-height: 1.9;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .dashboard-grid {
    height: auto;
    overflow: visible;
    padding-right: 0;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
