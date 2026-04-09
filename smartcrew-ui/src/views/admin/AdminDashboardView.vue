<template>
  <div class="dashboard-grid">
    <section class="stats-grid">
      <GlassPanel v-for="card in statsCards" :key="card.label" panel-class="admin-card stat-card">
        <span class="muted">{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <p>{{ card.description }}</p>
      </GlassPanel>
    </section>

    <GlassPanel panel-class="admin-card overview-card">
      <div class="card-head">
        <div>
          <h3>近期会话</h3>
          <p class="muted">快速查看最近活跃的系统会话记录。</p>
        </div>
        <el-button plain @click="router.push('/admin/conversations')">查看全部</el-button>
      </div>

      <el-table :data="recentSessions" stripe>
        <el-table-column prop="title" label="会话标题" min-width="180" />
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column prop="messageCount" label="消息数" width="100" />
        <el-table-column label="最近时间" min-width="160">
          <template #default="{ row }">{{ formatDate(row.lastMessageAt) }}</template>
        </el-table-column>
      </el-table>
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
        <li>可视化管理 Agent、Prompt、长期偏好和消息记录。</li>
        <li>前后端保持可裁剪结构，便于未来仅开放 `/api/v1/*`。</li>
      </ul>
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { ChatSession } from '../../types'

const router = useRouter()
const authStore = useAuthStore()
const stats = ref({
  users: 0,
  agents: 0,
  prompts: 0,
  sessions: 0
})
const recentSessions = ref<ChatSession[]>([])

const statsCards = computed(() => [
  {
    label: '平台用户',
    value: `${stats.value.users}`,
    description: '包含本地账号与第三方映射用户。'
  },
  {
    label: 'Agent 数量',
    value: `${stats.value.agents}`,
    description: '支持查询、配置与启停管理。'
  },
  {
    label: 'Prompt 数量',
    value: `${stats.value.prompts}`,
    description: '通过后台页面统一维护和追加。'
  },
  {
    label: '会话记录',
    value: `${stats.value.sessions}`,
    description: '公众页与平台接入统一进入审计链路。'
  }
])

onMounted(async () => {
  try {
    const [users, agents, prompts, sessions] = await Promise.all([
      adminPortalApi.listUsers(authStore.adminToken),
      adminPortalApi.listAgents(authStore.adminToken),
      adminPortalApi.listPrompts(authStore.adminToken),
      adminPortalApi.listConversationSessions(authStore.adminToken, {})
    ])
    stats.value = {
      users: users.rows.length,
      agents: agents.rows.length,
      prompts: prompts.rows.length,
      sessions: sessions.rows.length
    }
    recentSessions.value = sessions.rows.slice(0, 6)
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
})

function formatDate(value?: string) {
  if (!value) return '暂无'
  return new Date(value).toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.dashboard-grid {
  display: grid;
  gap: 18px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
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
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
