<template>
  <div class="page-grid">
    <GlassPanel panel-class="admin-card">
      <div class="card-head">
        <div>
          <h3>长期偏好管理</h3>
          <p class="muted">支持按用户维度查看、编辑与删除长期偏好设置。</p>
        </div>
        <div class="filter-bar">
          <el-input v-model="userIdKeyword" placeholder="输入用户 ID 筛选" clearable style="width: 220px" />
          <el-button type="primary" @click="openCreateDialog">新增偏好</el-button>
        </div>
      </div>

      <el-table :data="preferences" stripe>
        <el-table-column prop="userId" label="用户 ID" width="110" />
        <el-table-column prop="prefKey" label="偏好键" min-width="180" />
        <el-table-column prop="prefValue" label="偏好值" min-width="220" show-overflow-tooltip />
        <el-table-column prop="prefType" label="类型" width="120" />
        <el-table-column prop="source" label="来源" width="120" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button plain size="small" @click="editPreference(row)">编辑</el-button>
              <el-button type="danger" plain size="small" @click="removePreference(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </GlassPanel>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑偏好' : '新增偏好'" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户 ID" prop="userId">
          <el-input-number v-model="form.userId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="偏好键" prop="prefKey">
          <el-input v-model="form.prefKey" placeholder="例如：language" :disabled="editing" />
        </el-form-item>
        <el-form-item label="偏好值" prop="prefValue">
          <el-input v-model="form.prefValue" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="偏好类型">
          <el-input v-model="form.prefType" placeholder="默认 TEXT" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" placeholder="默认 MANUAL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePreference">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { PreferenceRecord } from '../../types'

const authStore = useAuthStore()
const userIdKeyword = ref('')
const preferences = ref<PreferenceRecord[]>([])
const dialogVisible = ref(false)
const editing = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<PreferenceRecord>({
  userId: 1,
  prefKey: '',
  prefValue: '',
  prefType: 'TEXT',
  source: 'MANUAL'
})

const rules: FormRules<typeof form> = {
  userId: [{ required: true, message: '请输入用户 ID', trigger: 'change' }],
  prefKey: [{ required: true, message: '请输入偏好键', trigger: 'blur' }],
  prefValue: [{ required: true, message: '请输入偏好值', trigger: 'blur' }]
}

onMounted(loadPreferences)
watch(userIdKeyword, loadPreferences)

async function loadPreferences() {
  try {
    const userId = Number(userIdKeyword.value)
    const response = await adminPortalApi.listPreferences(
      authStore.adminToken,
      Number.isFinite(userId) && userId > 0 ? userId : undefined
    )
    preferences.value = response.rows
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

function openCreateDialog() {
  editing.value = false
  Object.assign(form, {
    userId: Number(userIdKeyword.value) || 1,
    prefKey: '',
    prefValue: '',
    prefType: 'TEXT',
    source: 'MANUAL'
  })
  dialogVisible.value = true
}

function editPreference(row: PreferenceRecord) {
  editing.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

async function savePreference() {
  try {
    await formRef.value?.validate()
    await adminPortalApi.updatePreference(authStore.adminToken, form.userId, {
      prefKey: form.prefKey,
      prefValue: form.prefValue,
      prefType: form.prefType,
      source: form.source
    })
    ElMessage.success('偏好已保存')
    dialogVisible.value = false
    await loadPreferences()
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function removePreference(row: PreferenceRecord) {
  try {
    await ElMessageBox.confirm(`确认删除用户 ${row.userId} 的偏好 ${row.prefKey} 吗？`, '删除确认', {
      type: 'warning'
    })
    await adminPortalApi.deletePreference(authStore.adminToken, row.userId, row.prefKey)
    ElMessage.success('偏好已删除')
    await loadPreferences()
  } catch (error) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}
</script>

<style scoped lang="scss">
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

.filter-bar {
  display: flex;
  gap: 12px;
}
</style>
