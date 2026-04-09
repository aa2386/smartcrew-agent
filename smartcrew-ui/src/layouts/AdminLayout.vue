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
import { Connection, Cpu, DataAnalysis, MessageBox, Setting, UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const menuItems = [
  { path: '/admin/dashboard', label: '总览', icon: DataAnalysis },
  { path: '/admin/users', label: '用户管理', icon: UserFilled },
  { path: '/admin/identities', label: '平台映射', icon: Connection },
  { path: '/admin/agents', label: 'Agent 管理', icon: Cpu },
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
  min-height: 100vh;
}

.admin-sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
  border-radius: 28px;
  padding: 24px;
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
  background: rgba(14, 165, 233, 0.1);
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
  gap: 18px;
  min-width: 0;
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
  background: linear-gradient(135deg, var(--sc-primary), var(--sc-accent));
}

.admin-content {
  min-width: 0;
}

@media (max-width: 1100px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }
}
</style>
