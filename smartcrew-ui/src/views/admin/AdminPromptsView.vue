<template>
  <div class="prompt-grid">
    <GlassPanel panel-class="admin-card">
      <div class="card-head">
        <div>
          <h3>Prompt 分类</h3>
          <p class="muted">按分类查看最新 Prompt，并可继续追加新版本内容。</p>
        </div>
        <el-button type="primary" @click="dialogVisible = true">新增 Prompt</el-button>
      </div>

      <el-table :data="latestPrompts" stripe highlight-current-row @current-change="handleCurrentChange">
        <el-table-column prop="category" label="分类" min-width="180" />
        <el-table-column prop="templateName" label="模板名称" min-width="180" />
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
      </el-table>
    </GlassPanel>

    <GlassPanel panel-class="admin-card">
      <div class="card-head">
        <div>
          <h3>分类详情</h3>
          <p class="muted">当前分类：{{ selectedCategory || '未选择' }}</p>
        </div>
      </div>

      <template v-if="selectedPrompt">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="模板名称">{{ selectedPrompt.templateName }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ selectedPrompt.category }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ selectedPrompt.remark || '暂无' }}</el-descriptions-item>
        </el-descriptions>

        <div class="prompt-content glass-panel">
          <pre>{{ selectedPrompt.templateContent }}</pre>
        </div>

        <h4 class="history-title">同分类历史记录</h4>
        <el-table :data="categoryHistory" stripe>
          <el-table-column prop="id" label="ID" width="100" />
          <el-table-column prop="templateName" label="模板名称" min-width="180" />
          <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        </el-table>
      </template>

      <div v-else class="empty-text muted">请选择左侧分类后查看 Prompt 内容。</div>
    </GlassPanel>

    <el-dialog v-model="dialogVisible" title="新增 Prompt" width="620px">
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
        <el-button type="primary" @click="createPrompt">保存 Prompt</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { adminPortalApi } from '../../api/portal'
import { useAuthStore } from '../../stores/auth'
import type { PromptRecord } from '../../types'

const authStore = useAuthStore()
const prompts = ref<PromptRecord[]>([])
const selectedCategory = ref('')
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

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

const latestPrompts = computed(() => {
  const grouped = new Map<string, PromptRecord>()
  prompts.value.forEach((item) => {
    const current = grouped.get(item.category)
    if (!current || item.id > current.id) {
      grouped.set(item.category, item)
    }
  })
  return [...grouped.values()].sort((a, b) => a.category.localeCompare(b.category))
})

const selectedPrompt = computed(() => latestPrompts.value.find((item) => item.category === selectedCategory.value))
const categoryHistory = computed(() =>
  prompts.value
    .filter((item) => item.category === selectedCategory.value)
    .sort((a, b) => b.id - a.id)
)

onMounted(loadPrompts)

async function loadPrompts() {
  try {
    const response = await adminPortalApi.listPrompts(authStore.adminToken)
    prompts.value = response.rows
    if (!selectedCategory.value && latestPrompts.value.length > 0) {
      selectedCategory.value = latestPrompts.value[0].category
    }
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

function handleCurrentChange(row?: PromptRecord) {
  if (row) {
    selectedCategory.value = row.category
  }
}

async function createPrompt() {
  try {
    await formRef.value?.validate()
    await adminPortalApi.createPrompt(authStore.adminToken, form)
    ElMessage.success('Prompt 已保存')
    dialogVisible.value = false
    form.category = ''
    form.templateName = ''
    form.templateContent = ''
    form.remark = ''
    await loadPrompts()
  } catch (error) {
    if (error instanceof Error) {
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

.history-title {
  margin: 20px 0 12px;
  font-family: var(--sc-font-title);
}

.empty-text {
  padding: 16px 4px;
}

@media (max-width: 1200px) {
  .prompt-grid {
    grid-template-columns: 1fr;
  }
}
</style>
