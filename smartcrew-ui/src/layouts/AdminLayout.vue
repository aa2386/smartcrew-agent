<template>
  <div class="page-shell admin-shell">
    <aside class="glass-panel admin-sidebar">
      <div class="brand-block">
        <span class="brand-badge">SmartCrew</span>
        <h1>后台管理台</h1>
        <p>统一管理用户、Agent、Prompt 与消息审计。</p>
      </div>

      <el-menu
        class="admin-menu"
        :default-active="route.path"
        router
        background-color="transparent"
        text-color="#0f172a"
        active-text-color="#0369a1"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <el-button plain @click="router.push('/')">返回公众页</el-button>
        <el-button type="danger" plain @click="logout">退出后台</el-button>
      </div>
    </aside>

    <main class="admin-main">
      <header class="glass-panel admin-topbar">
        <div>
          <span class="muted">管理入口</span>
          <h2>{{ currentTitle }}</h2>
        </div>
        <div class="topbar-user">
          <div class="user-avatar">{{ adminName.slice(0, 1) }}</div>
          <div>
            <strong>{{ adminName }}</strong>
            <p>{{ authStore.adminUser?.role || 'ADMIN' }}</p>
          </div>
        </div>
      </header>

      <section class="admin-content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CollectionTag, Connection, Cpu, DataAnalysis, MessageBox, Setting, UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const menuItems = [
  { path: '/admin/dashboard', label: '总览', icon: DataAnalysis },
  { path: '/admin/users', label: '用户管理', icon: UserFilled },
  { path: '/admin/identities', label: '平台映射', icon: Connection },
  { path: '/admin/agents', label: 'Agent 管理', icon: Cpu },
  { path: '/admin/knowledge-bases', label: '知识库管理', icon: CollectionTag },
  { path: '/admin/prompts', label: 'Prompt 配置', icon: Setting },
  { path: '/admin/conversations', label: '消息记录', icon: MessageBox }
]

const titleMap = new Map(menuItems.map((item) => [item.path, item.label]))
const currentTitle = computed(() => titleMap.get(route.path) ?? '后台管理台')
const adminName = computed(() => authStore.adminUser?.displayName || authStore.adminUser?.username || '管理员')

async function logout() {
  try {
    await authStore.logoutAdmin()
    ElMessage.success('已退出后台登录')
  } finally {
    router.push('/admin/login')
  }
}
</script>

<style scoped lang="scss">
.admin-shell {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
  height: 100%;
  min-height: 0;
}

.admin-sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
  border-radius: 28px;
  padding: 24px;
  min-height: 0;
  overflow: hidden;
}

.brand-block {
  padding: 8px 4px 0;

  h1 {
    margin: 12px 0 10px;
    font-family: var(--sc-font-title);
    font-size: 2rem;
  }

  p {
    margin: 0;
    color: var(--sc-text-soft);
    line-height: 1.7;
  }
}

.brand-badge {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  color: var(--sc-primary-deep);
  border: 1px solid rgba(255, 255, 255, 0.24);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.58), rgba(255, 255, 255, 0.18)),
    rgba(126, 190, 255, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.42);
  font-weight: 700;
}

.admin-menu {
  border-right: none;
}

.sidebar-footer {
  margin-top: auto;
  display: grid;
  gap: 12px;
}

.admin-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 18px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.admin-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 24px;
  padding: 18px 24px;

  h2 {
    margin: 4px 0 0;
    font-family: var(--sc-font-title);
    font-size: 1.7rem;
  }
}

.topbar-user {
  display: flex;
  align-items: center;
  gap: 12px;

  p {
    margin: 4px 0 0;
    color: var(--sc-text-soft);
  }
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  color: white;
  font-family: var(--sc-font-title);
  box-shadow:
    0 14px 26px rgba(21, 59, 120, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.34);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.96), rgba(255, 123, 84, 0.82));
}

.admin-content {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

@media (max-width: 1100px) {
  .admin-shell {
    grid-template-columns: 1fr;
    height: auto;
  }

  .admin-sidebar,
  .admin-main,
  .admin-content {
    overflow: visible;
  }
}
</style>
