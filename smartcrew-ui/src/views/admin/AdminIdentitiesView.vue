<template>
  <div class="identity-grid">
    <GlassPanel panel-class="admin-card selector-card">
      <div class="card-head">
        <div>
          <h3>平台身份映射</h3>
          <p class="muted">按系统用户查看或维护飞书、企微等第三方身份绑定。</p>
        </div>
        <el-button type="primary" @click="dialogVisible = true" :disabled="!selectedUserId">新增绑定</el-button>
      </div>

      <el-select
        v-model="selectedUserId"
        placeholder="请选择用户"
        filterable
        style="width: 100%"
        @change="handleUserChange"
      >
        <el-option
          v-for="item in users"
          :key="item.id"
          :label="`${item.displayName}（${item.username}）`"
          :value="item.id"
        />
      </el-select>
    </GlassPanel>

    <GlassPanel panel-class="admin-card">
      <div class="card-head">
        <div>
          <h3>映射明细</h3>
          <p class="muted">当前用户：{{ selectedUser?.displayName || '未选择用户' }}</p>
        </div>
      </div>

      <el-table :data="identities" stripe>
        <el-table-column prop="provider" label="平台" width="120" />
        <el-table-column prop="providerUserId" label="平台用户 ID" min-width="180" />
        <el-table-column prop="tenantKey" label="租户标识" min-width="160" />
        <el-table-column prop="profileSnapshotJson" label="快照 JSON" min-width="240" show-overflow-tooltip />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="danger" plain size="small" @click="removeIdentity(row.id)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="selectedUserId && !identities.length" class="empty-text muted">
        当前用户还没有绑定任何第三方身份。
      </div>
    </GlassPanel>

    <el-dialog v-model="dialogVisible" title="新增身份绑定" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="平台" prop="provider">
          <el-select v-model="form.provider" placeholder="请选择平台">
            <el-option label="飞书" value="FEISHU" />
            <el-option label="企业微信" value="WECOM" />
            <el-option label="Web 本地账号" value="WEB" />
          </el-select>
        </el-form-item>
        <el-form-item label="平台用户 ID" prop="providerUserId">
          <el-input v-model="form.providerUserId" placeholder="请输入平台侧用户标识" />
        </el-form-item>
        <el-form-item label="租户标识">
          <el-input v-model="form.tenantKey" placeholder="可选，用于区分不同租户" />
        </el-form-item>
        <el-form-item label="快照 JSON">
          <el-input v-model="form.profileSnapshotJson" type="textarea" :rows="4" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="bindIdentity">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { UserIdentityRecord, UserRecord } from '../../types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const users = ref<UserRecord[]>([])
const identities = ref<UserIdentityRecord[]>([])
const selectedUserId = ref<number>()
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  provider: 'FEISHU',
  providerUserId: '',
  tenantKey: '',
  profileSnapshotJson: ''
})

const rules: FormRules<typeof form> = {
  provider: [{ required: true, message: '请选择平台', trigger: 'change' }],
  providerUserId: [{ required: true, message: '请输入平台用户 ID', trigger: 'blur' }]
}

const selectedUser = computed(() => users.value.find((item) => item.id === selectedUserId.value))

onMounted(async () => {
  await loadUsers()
  syncUserFromRoute()
})

watch(
  () => route.query.userId,
  () => {
    syncUserFromRoute()
  }
)

async function loadUsers() {
  try {
    const response = await adminPortalApi.listUsers(authStore.adminToken)
    users.value = response.rows
    if (!selectedUserId.value && users.value.length > 0) {
      selectedUserId.value = users.value[0].id
      await loadIdentities()
    }
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

function syncUserFromRoute() {
  const routeUserId = Number(route.query.userId)
  if (Number.isFinite(routeUserId) && routeUserId > 0) {
    selectedUserId.value = routeUserId
    loadIdentities()
  }
}

async function handleUserChange(userId: number) {
  router.replace({ query: { userId: String(userId) } })
  await loadIdentities()
}

async function loadIdentities() {
  if (!selectedUserId.value) return
  try {
    identities.value = await adminPortalApi.listUserIdentities(authStore.adminToken, selectedUserId.value)
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function bindIdentity() {
  if (!selectedUserId.value) {
    ElMessage.warning('请先选择用户')
    return
  }
  try {
    await formRef.value?.validate()
    await adminPortalApi.bindUserIdentity(authStore.adminToken, selectedUserId.value, form)
    ElMessage.success('身份绑定成功')
    dialogVisible.value = false
    form.providerUserId = ''
    form.tenantKey = ''
    form.profileSnapshotJson = ''
    await loadIdentities()
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function removeIdentity(identityId: number) {
  if (!selectedUserId.value) return
  try {
    await ElMessageBox.confirm('确认解绑该第三方身份吗？', '解绑确认', {
      type: 'warning'
    })
    await adminPortalApi.unbindUserIdentity(authStore.adminToken, selectedUserId.value, identityId)
    ElMessage.success('解绑成功')
    await loadIdentities()
  } catch (error) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}
</script>

<style scoped lang="scss">
.identity-grid {
  display: grid;
  gap: 18px;
}

.selector-card {
  display: grid;
  gap: 16px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  h3 {
    margin: 0 0 8px;
    font-family: var(--sc-font-title);
  }

  p {
    margin: 0;
  }
}

.empty-text {
  padding-top: 18px;
}
</style>
