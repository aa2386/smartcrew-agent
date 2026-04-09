<template>
  <div class="page-grid">
    <GlassPanel panel-class="admin-card">
      <div class="card-head">
        <div>
          <h3>平台用户管理</h3>
          <p class="muted">查看用户状态、最近登录时间、角色信息和长期偏好。</p>
        </div>
        <el-input v-model="keyword" placeholder="按用户名、显示名称或角色筛选" clearable style="max-width: 320px" />
      </div>

      <div class="table-shell">
        <el-table :data="filteredUsers" stripe height="100%">
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
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button plain size="small" @click="showUserDetail(row.id)">查看详情</el-button>
              <el-button plain size="small" @click="openPreferencePanel(row.id)">偏好设置</el-button>
              <el-button plain size="small" @click="router.push(`/admin/identities?userId=${row.id}`)">查看映射</el-button>
            </el-space>
          </template>
        </el-table-column>
        </el-table>
      </div>
    </GlassPanel>

    <el-drawer v-model="detailVisible" title="用户详情与偏好" size="560px">
      <template v-if="currentUser">
        <div class="detail-panel">
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

        <div class="preference-head">
          <div>
            <h4>长期偏好</h4>
            <p class="muted">固定管理三项偏好：语言、称呼、风格。</p>
          </div>
          <el-button type="primary" plain @click="openPreferenceDialog()">新增偏好</el-button>
        </div>

        <el-table :data="preferenceRows" stripe>
          <el-table-column prop="label" label="偏好键" min-width="160" />
          <el-table-column label="偏好值" min-width="220">
            <template #default="{ row }">{{ row.prefValue || '未设置' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-space>
                <el-button plain size="small" @click="openPreferenceDialog(row)">
                  {{ row.exists ? '编辑' : '设置' }}
                </el-button>
                <el-button
                  type="danger"
                  plain
                  size="small"
                  :disabled="!row.exists"
                  @click="removePreference(row.prefKey)"
                >
                  删除
                </el-button>
              </el-space>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>

    <el-dialog v-model="preferenceDialogVisible" :title="editingPreference ? '编辑偏好' : '新增偏好'" width="520px">
      <el-form ref="formRef" :model="preferenceForm" :rules="rules" label-position="top">
        <el-form-item label="偏好键" prop="prefKey">
          <el-select v-model="preferenceForm.prefKey" placeholder="请选择偏好键" style="width: 100%">
            <el-option v-for="item in preferenceOptions" :key="item.key" :label="item.label" :value="item.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="偏好值" prop="prefValue">
          <el-input v-model="preferenceForm.prefValue" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="preferenceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePreference">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { PreferenceRecord, UserRecord } from '../../types'

type PreferenceOption = {
  key: 'language' | 'nickname' | 'tone'
  label: string
}

const preferenceOptions: PreferenceOption[] = [
  { key: 'language', label: '用户偏好语言' },
  { key: 'nickname', label: '用户偏好称呼' },
  { key: 'tone', label: '用户偏好风格' }
]

const router = useRouter()
const authStore = useAuthStore()
const keyword = ref('')
const users = ref<UserRecord[]>([])
const currentUser = ref<UserRecord>()
const detailVisible = ref(false)
const preferences = ref<PreferenceRecord[]>([])
const preferenceDialogVisible = ref(false)
const editingPreference = ref(false)
const formRef = ref<FormInstance>()

const preferenceForm = reactive({
  prefKey: 'language' as PreferenceOption['key'],
  prefValue: '',
  prefType: 'TEXT',
  source: 'MANUAL'
})

const rules: FormRules<typeof preferenceForm> = {
  prefKey: [{ required: true, message: '请选择偏好键', trigger: 'change' }],
  prefValue: [{ required: true, message: '请输入偏好值', trigger: 'blur' }]
}

const filteredUsers = computed(() => {
  if (!keyword.value.trim()) return users.value
  const search = keyword.value.trim().toLowerCase()
  return users.value.filter((item) =>
    [item.username, item.displayName, item.role, item.status].some((field) =>
      field?.toLowerCase().includes(search)
    )
  )
})

const preferenceRows = computed(() => {
  const map = new Map(preferences.value.map((item) => [item.prefKey, item]))
  return preferenceOptions.map((option) => {
    const record = map.get(option.key)
    return {
      prefKey: option.key,
      label: option.label,
      prefValue: record?.prefValue || '',
      exists: Boolean(record)
    }
  })
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
    await loadPreferences(userId)
    detailVisible.value = true
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function openPreferencePanel(userId: number) {
  await showUserDetail(userId)
}

async function loadPreferences(userId: number) {
  const response = await adminPortalApi.listPreferences(authStore.adminToken, userId)
  preferences.value = response.rows.filter((item) =>
    preferenceOptions.some((option) => option.key === item.prefKey)
  )
}

function openPreferenceDialog(row?: { prefKey: string; prefValue: string; exists: boolean }) {
  if (!currentUser.value) return
  editingPreference.value = Boolean(row?.exists)
  if (row) {
    preferenceForm.prefKey = row.prefKey as PreferenceOption['key']
    preferenceForm.prefValue = row.prefValue || ''
  } else {
    const firstEmpty = preferenceRows.value.find((item) => !item.exists)
    preferenceForm.prefKey = (firstEmpty?.prefKey || 'language') as PreferenceOption['key']
    preferenceForm.prefValue = ''
  }
  preferenceForm.prefType = 'TEXT'
  preferenceForm.source = 'MANUAL'
  preferenceDialogVisible.value = true
}

async function savePreference() {
  if (!currentUser.value) return
  try {
    await formRef.value?.validate()
    await adminPortalApi.updatePreference(authStore.adminToken, currentUser.value.id, {
      prefKey: preferenceForm.prefKey,
      prefValue: preferenceForm.prefValue,
      prefType: preferenceForm.prefType,
      source: preferenceForm.source
    })
    ElMessage.success('偏好已保存')
    preferenceDialogVisible.value = false
    await loadPreferences(currentUser.value.id)
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function removePreference(prefKey: string) {
  if (!currentUser.value) return
  try {
    await ElMessageBox.confirm('确认删除该偏好设置吗？', '删除确认', { type: 'warning' })
    await adminPortalApi.deletePreference(authStore.adminToken, currentUser.value.id, prefKey)
    ElMessage.success('偏好已删除')
    await loadPreferences(currentUser.value.id)
  } catch (error) {
    if (error instanceof Error && error.message !== 'cancel') {
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
.page-grid {
  height: 100%;
  min-height: 0;
}

.admin-card {
  display: flex;
  flex-direction: column;
}

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
  gap: 14px;
}

.detail-item {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0.14)),
    rgba(255, 255, 255, 0.08);

  span {
    color: var(--sc-text-soft);
  }
}

.preference-head {
  margin: 18px 0 12px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;

  h4 {
    margin: 0 0 6px;
    font-family: var(--sc-font-title);
  }

  p {
    margin: 0;
  }
}
</style>
