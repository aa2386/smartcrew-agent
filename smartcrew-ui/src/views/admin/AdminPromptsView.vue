<template>
  <div class="prompt-grid">
    <GlassPanel panel-class="admin-card prompt-list-card">
      <div class="card-head">
        <div>
          <h3>Prompt 分类</h3>
          <p class="muted">按分类查看最新 Prompt，并支持新增、编辑、删除。</p>
        </div>
        <el-button type="primary" @click="openCreateDialog">新增 Prompt</el-button>
      </div>

      <div class="table-shell">
        <el-table :data="promptCategories" stripe highlight-current-row height="100%" @current-change="handleCurrentChange">
          <el-table-column prop="category" label="分类" min-width="180" />
          <el-table-column prop="templateName" label="模板名称" min-width="180" />
          <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
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

    <GlassPanel panel-class="admin-card prompt-detail-card">
      <div class="card-head">
        <div>
          <h3>分类详情</h3>
          <p class="muted">当前分类：{{ selectedCategory || '未选择' }}</p>
        </div>
      </div>

      <div class="detail-scroll">
        <template v-if="selectedPrompt">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="模板名称">{{ selectedPrompt.templateName }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ selectedPrompt.category }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ selectedPrompt.remark || '暂无' }}</el-descriptions-item>
          </el-descriptions>

          <div class="prompt-content glass-panel">
            <pre>{{ selectedPrompt.templateContent }}</pre>
          </div>

          <div class="latest-actions">
            <el-button plain @click="openEditDialog(selectedPrompt)">编辑当前版本</el-button>
            <el-button type="danger" plain @click="removePrompt(selectedPrompt)">删除当前版本</el-button>
          </div>

          <h4 class="history-title">同分类历史记录</h4>
          <el-table :data="categoryHistory" stripe max-height="280">
            <el-table-column prop="id" label="ID" width="90" />
            <el-table-column prop="templateName" label="模板名称" min-width="160" />
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button plain size="small" @click="openEditDialog(row)">编辑</el-button>
                  <el-button type="danger" plain size="small" @click="removePrompt(row)">删除</el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else class="empty-text muted">请选择左侧分类后查看 Prompt 内容。</div>
      </div>
    </GlassPanel>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑 Prompt' : '新增 Prompt'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类" prop="category">
          <el-input v-model="form.category" placeholder="例如：initial-agent" />
        </el-form-item>
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="例如：初始智能体默认 Prompt" />
        </el-form-item>
        <el-form-item label="模板内容" prop="templateContent">
          <el-input v-model="form.templateContent" type="textarea" :rows="10" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPrompt">保存 Prompt</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { PromptRecord } from '../../types'

const pageSizeOptions = [10, 30, 50, 100, 250]

const authStore = useAuthStore()
const prompts = ref<PromptRecord[]>([])
const promptCategories = ref<PromptRecord[]>([])
const selectedCategory = ref('')
const dialogVisible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const pager = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const form = reactive({
  category: '',
  templateName: '',
  templateContent: '',
  remark: ''
})

const rules: FormRules<typeof form> = {
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateContent: [{ required: true, message: '请输入模板内容', trigger: 'blur' }]
}

const selectedPrompt = computed(() =>
  prompts.value
    .filter((item) => item.category === selectedCategory.value)
    .sort((a, b) => b.id - a.id)[0]
)
const categoryHistory = computed(() =>
  prompts.value
    .filter((item) => item.category === selectedCategory.value)
    .sort((a, b) => b.id - a.id)
)

onMounted(async () => {
  await Promise.all([loadPrompts(), loadPromptCategories()])
})

async function loadPrompts() {
  try {
    const response = await adminPortalApi.listPrompts(authStore.adminToken)
    prompts.value = response.rows
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function loadPromptCategories() {
  try {
    const response = await adminPortalApi.listPromptCategories(authStore.adminToken, {
      pageNum: pager.pageNum,
      pageSize: pager.pageSize
    })
    promptCategories.value = response.rows
    pager.total = response.total
    syncSelectedCategory()
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

function syncSelectedCategory() {
  if (!selectedCategory.value && promptCategories.value.length > 0) {
    selectedCategory.value = promptCategories.value[0].category
    return
  }
  if (selectedCategory.value && !promptCategories.value.some((item) => item.category === selectedCategory.value)) {
    selectedCategory.value = promptCategories.value[0]?.category || ''
  }
}

async function handlePageChange(pageNum: number) {
  pager.pageNum = pageNum
  await loadPromptCategories()
}

async function handleSizeChange(pageSize: number) {
  pager.pageSize = pageSize
  pager.pageNum = 1
  await loadPromptCategories()
}

function handleCurrentChange(row?: PromptRecord) {
  if (row) {
    selectedCategory.value = row.category
  }
}

function resetForm() {
  form.category = ''
  form.templateName = ''
  form.templateContent = ''
  form.remark = ''
}

function openCreateDialog() {
  editingId.value = undefined
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: PromptRecord) {
  editingId.value = row.id
  form.category = row.category
  form.templateName = row.templateName
  form.templateContent = row.templateContent
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function submitPrompt() {
  try {
    await formRef.value?.validate()
    const payload = {
      category: form.category,
      templateName: form.templateName,
      templateContent: form.templateContent,
      remark: form.remark
    }
    if (editingId.value) {
      await adminPortalApi.updatePrompt(authStore.adminToken, editingId.value, payload)
      ElMessage.success('Prompt 已更新')
    } else {
      await adminPortalApi.createPrompt(authStore.adminToken, payload)
      ElMessage.success('Prompt 已创建')
    }
    dialogVisible.value = false
    await Promise.all([loadPrompts(), loadPromptCategories()])
    selectedCategory.value = payload.category
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function removePrompt(row: PromptRecord) {
  try {
    await ElMessageBox.confirm(`确认删除 Prompt #${row.id}（${row.templateName}）吗？`, '删除确认', {
      type: 'warning'
    })
    await adminPortalApi.deletePrompt(authStore.adminToken, row.id)
    ElMessage.success('Prompt 已删除')
    await Promise.all([loadPrompts(), loadPromptCategories()])
  } catch (error) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}
</script>

<style scoped lang="scss">
.prompt-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 18px;
  height: 100%;
  min-height: 0;
}

.prompt-list-card,
.prompt-detail-card {
  display: flex;
  flex-direction: column;
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

.table-shell {
  flex: 1;
  min-height: 0;
}

.detail-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 6px;
}

.prompt-content {
  margin-top: 18px;
  border-radius: 18px;
  padding: 18px;

  pre {
    margin: 0;
    white-space: pre-wrap;
    line-height: 1.8;
    font-family: var(--sc-font-body);
  }
}

.latest-actions {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}

.history-title {
  margin: 20px 0 12px;
  font-family: var(--sc-font-title);
}

.empty-text {
  min-height: 100%;
  display: grid;
  place-items: center;
  padding: 16px 4px;
  text-align: center;
}

@media (max-width: 1200px) {
  .prompt-grid {
    grid-template-columns: 1fr;
    height: auto;
  }
}
</style>
