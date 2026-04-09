<template>
  <el-dialog
    v-model="visible"
    :title="mode === 'login' ? '登录 SmartCrew' : '注册 SmartCrew'"
    width="460px"
    class="auth-dialog"
    destroy-on-close
  >
    <div class="auth-header">
      <div>
        <h3>{{ mode === 'login' ? '欢迎回来' : '创建你的智能工作台' }}</h3>
        <p>{{ mode === 'login' ? '登录后即可保存对话、偏好和历史记录。' : '注册后即可使用平台聊天与个性化能力。' }}</p>
      </div>
      <el-segmented
        v-model="mode"
        :options="[
          { label: '登录', value: 'login' },
          { label: '注册', value: 'register' }
        ]"
      />
    </div>

    <el-form
      v-if="mode === 'login'"
      ref="loginFormRef"
      :model="loginForm"
      :rules="loginRules"
      label-position="top"
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model="loginForm.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
    </el-form>

    <el-form
      v-else
      ref="registerFormRef"
      :model="registerForm"
      :rules="registerRules"
      label-position="top"
    >
      <el-form-item label="显示名称" prop="displayName">
        <el-input v-model="registerForm.displayName" placeholder="例如：小星" />
      </el-form-item>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="registerForm.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="auth-footer">
        <span class="muted">{{ mode === 'login' ? '还没有账号？切换到注册即可。' : '已有账号？切换到登录即可。' }}</span>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ mode === 'login' ? '立即登录' : '完成注册' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  success: []
}>()

const authStore = useAuthStore()
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const mode = ref<'login' | 'register'>('login')
const submitting = ref(false)
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  displayName: '',
  username: '',
  password: ''
})

const loginRules: FormRules<typeof loginForm> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules: FormRules<typeof registerForm> = {
  displayName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  submitting.value = true
  try {
    if (mode.value === 'login') {
      await loginFormRef.value?.validate()
      await authStore.loginWeb(loginForm)
      ElMessage.success('登录成功')
    } else {
      await registerFormRef.value?.validate()
      await authStore.registerWeb(registerForm)
      ElMessage.success('注册成功')
    }
    emit('success')
    visible.value = false
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-header {
  display: grid;
  gap: 16px;
  margin-bottom: 8px;

  h3 {
    margin: 0 0 8px;
    font-family: var(--sc-font-title);
    font-size: 1.4rem;
  }

  p {
    margin: 0;
    color: var(--sc-text-soft);
  }
}

.auth-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
</style>
