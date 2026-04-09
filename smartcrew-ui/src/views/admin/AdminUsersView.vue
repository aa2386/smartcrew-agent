<template>
  <div class="page-grid">
    <GlassPanel panel-class="admin-card">
      <div class="card-head">
        <div>
          <h3>平台用户管理</h3>
          <p class="muted">查看用户状态、最近登录时间与角色信息。</p>
        </div>
        <el-input v-model="keyword" placeholder="按用户名、显示名称或角色筛选" clearable style="max-width: 320px" />
      </div>

      <el-table :data="filteredUsers" stripe>
        <el-table-column prop="displayName" label="显示名称" min-width="160" />
        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'ENABLED'"
              inline-prompt
              active-text="启用"
              inactive-text="禁用"
              @change="toggleStatus(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="180">
          <template #default="{ row }">{{ formatDate(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button plain size="small" @click="showUserDetail(row.id)">查看详情</el-button>
              <el-button plain size="small" @click="router.push(`/admin/identities?userId=${row.id}`)">
                查看映射
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </GlassPanel>

    <el-drawer v-model="detailVisible" title="用户详情" size="420px">
      <div v-if="currentUser" class="detail-panel">
        <div class="detail-item">
          <span>显示名称</span>
          <strong>{{ currentUser.displayName }}</strong>
        </div>
        <div class="detail-item">
          <span>用户名</span>
          <strong>{{ currentUser.username }}</strong>
        </div>
        <div class="detail-item">
          <span>角色</span>
          <strong>{{ currentUser.role }}</strong>
        </div>
        <div class="detail-item">
          <span>状态</span>
          <strong>{{ currentUser.status }}</strong>
        </div>
        <div class="detail-item">
          <span>最近登录</span>
          <strong>{{ formatDate(currentUser.lastLoginAt) }}</strong>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { UserRecord } from '../../types'

const router = useRouter()
const authStore = useAuthStore()
const keyword = ref('')
const users = ref<UserRecord[]>([])
const currentUser = ref<UserRecord>()
const detailVisible = ref(false)

const filteredUsers = computed(() => {
  if (!keyword.value.trim()) return users.value
  const search = keyword.value.trim().toLowerCase()
  return users.value.filter((item) =>
    [item.username, item.displayName, item.role, item.status].some((field) =>
      field?.toLowerCase().includes(search)
    )
  )
})

onMounted(loadUsers)

async function loadUsers() {
  try {
    const response = await adminPortalApi.listUsers(authStore.adminToken)
    users.value = response.rows
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function showUserDetail(userId: number) {
  try {
    currentUser.value = await adminPortalApi.getUser(authStore.adminToken, userId)
    detailVisible.value = true
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function toggleStatus(row: UserRecord, enabled: string | number | boolean) {
  const status = enabled ? 'ENABLED' : 'DISABLED'
  try {
    const updated = await adminPortalApi.updateUserStatus(authStore.adminToken, row.id, status)
    const index = users.value.findIndex((item) => item.id === row.id)
    if (index >= 0) {
      users.value[index] = updated
    }
    ElMessage.success('用户状态已更新')
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
    await loadUsers()
  }
}

function formatDate(value?: string) {
  if (!value) return '暂无'
  return new Date(value).toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.detail-panel {
  display: grid;
  gap: 16px;
}

.detail-item {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.52);

  span {
    color: var(--sc-text-soft);
  }
}
</style>
