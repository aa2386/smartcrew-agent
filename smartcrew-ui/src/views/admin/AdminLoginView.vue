<template>
  <div class="admin-login">
    <GlassPanel panel-class="login-card">
      <span class="login-badge">SmartCrew 后台</span>
      <h1>管理员登录</h1>
      <p>通过管理员账号进入平台控制台，统一管理用户、智能体和消息记录。</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入管理员用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
      </el-form>

      <div class="login-actions">
        <el-button plain @click="router.push('/')">返回公众页</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">登录后台</el-button>
      </div>
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import GlassPanel from '../../components/common/GlassPanel.vue'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入管理员用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  submitting.value = true
  try {
    await formRef.value?.validate()
    await authStore.loginAdmin(form)
    ElMessage.success('登录成功')
    router.push('/admin/dashboard')
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
.admin-login {
  min-height: calc(100vh - 48px);
  display: grid;
  place-items: center;
}

.login-card {
  width: min(520px, 100%);
  border-radius: 32px;
  padding: 30px;

  h1 {
    margin: 14px 0 12px;
    font-family: var(--sc-font-title);
    font-size: 2.4rem;
  }

  p {
    margin: 0 0 26px;
    color: var(--sc-text-soft);
    line-height: 1.8;
  }
}

.login-badge {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  color: var(--sc-accent);
  background: rgba(249, 115, 22, 0.12);
  font-weight: 700;
}

.login-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
</style>
