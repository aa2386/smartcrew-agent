<template>
  <div class="public-grid">
    <GlassPanel panel-class="public-sidebar page-card">
      <div class="sidebar-top">
        <div class="user-card">
          <template v-if="authStore.isWebLoggedIn">
            <div class="avatar">{{ displayName.slice(0, 1) }}</div>
            <div>
              <strong>{{ displayName }}</strong>
              <p>{{ authStore.webUser?.username }}</p>
            </div>
          </template>
          <template v-else>
            <div class="avatar guest">未</div>
            <div>
              <strong>点击登录 / 注册</strong>
              <p>登录后保存会话与偏好</p>
            </div>
          </template>
        </div>

        <div class="sidebar-actions">
          <el-button
            v-if="authStore.isWebLoggedIn"
            type="primary"
            plain
            @click="chatStore.createDraftSession()"
          >
            新建对话
          </el-button>
          <el-button v-else type="primary" @click="authDialogVisible = true">登录 / 注册</el-button>
          <el-button v-if="authStore.isWebLoggedIn" plain @click="logout">退出</el-button>
        </div>
      </div>

      <div class="sidebar-list-header">
        <span>历史对话</span>
        <span class="muted">{{ chatStore.sessions.length }} 条</span>
      </div>

      <div class="conversation-list">
        <button
          v-for="session in chatStore.sessions"
          :key="session.sessionId"
          type="button"
          class="conversation-item"
          :class="{ active: chatStore.activeSessionId === session.sessionId }"
          @click="openSession(session.sessionId)"
        >
          <strong>{{ session.title }}</strong>
          <span>{{ session.preview || '暂无内容' }}</span>
          <small>{{ formatTime(session.lastMessageAt) }}</small>
        </button>

        <div v-if="!authStore.isWebLoggedIn" class="empty-tip muted">
          登录后可查看和管理你的历史会话。
        </div>
        <div v-else-if="!chatStore.sessions.length" class="empty-tip muted">
          还没有历史会话，发送第一条消息开始吧。
        </div>
      </div>
    </GlassPanel>

    <GlassPanel panel-class="public-main page-card">
      <header class="public-header">
        <div>
          <span class="header-pill">面向公众开放</span>
          <h1>SmartCrew 智能协作平台</h1>
          <p>
            在一个现代、轻盈、可扩展的交互界面里完成对话、配置与长期陪伴体验。
          </p>
        </div>
        <el-button v-if="portalConfig.enableAdmin" plain @click="router.push('/admin/login')">
          后台管理入口
        </el-button>
      </header>

      <div v-if="!chatStore.messages.length" class="hero-state">
        <div class="hero-copy">
          <h2>一个更适合长期协作的 AI 入口</h2>
          <p>
            它不仅能回答问题，也会逐步理解你的表达习惯、长期偏好与上下文目标，成为可运营、可管理、可扩展的平台入口。
          </p>
        </div>

        <div class="quick-grid">
          <button
            v-for="item in quickPrompts"
            :key="item.title"
            type="button"
            class="quick-card glass-panel"
            @click="fillPrompt(item.prompt)"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.description }}</span>
          </button>
        </div>
      </div>

      <div v-else class="message-list">
        <article
          v-for="(message, index) in chatStore.messages"
          :key="`${message.role}-${index}-${message.createTime}`"
          class="message-item"
          :class="message.role"
        >
          <div class="message-meta">
            <strong>{{ message.role === 'assistant' ? 'SmartCrew' : displayName }}</strong>
            <span>{{ formatTime(message.createTime) }}</span>
          </div>
          <div class="message-bubble">
            {{ message.content }}
          </div>
        </article>
      </div>

      <footer class="composer glass-panel">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="4"
          resize="none"
          placeholder="输入你的问题、任务或想法。按 Enter 发送，Shift + Enter 换行。"
          @keydown.enter.exact.prevent="sendMessage()"
        />
        <div class="composer-actions">
          <span class="muted">支持多轮对话、历史记录与后续用户偏好扩展</span>
          <el-button type="primary" :loading="chatStore.sending" @click="sendMessage()">
            发送消息
          </el-button>
        </div>
      </footer>
    </GlassPanel>

    <AuthDialog v-model="authDialogVisible" @success="handleAuthSuccess" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import GlassPanel from '../../components/common/GlassPanel.vue'
import AuthDialog from '../../components/public/AuthDialog.vue'
import { portalConfig } from '../../config/portal'
import { useAuthStore } from '../../stores/auth'
import { useChatStore } from '../../stores/chat'

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()
const authDialogVisible = ref(false)
const draft = ref('')

const quickPrompts = [
  {
    title: '平台功能梳理',
    description: '快速了解系统现有能力与接入结构',
    prompt: '请帮我梳理当前平台已有的核心功能与后续扩展方向。'
  },
  {
    title: 'Agent 规划建议',
    description: '从单 Agent 到多 Agent 的演进建议',
    prompt: '请给我一个适合当前系统的 Agent 架构演进建议。'
  },
  {
    title: 'Prompt 优化',
    description: '提升提示词配置与维护效率',
    prompt: '请帮我设计一套便于管理和版本化的 Prompt 配置方案。'
  }
]

const displayName = computed(() => authStore.webUser?.displayName || authStore.webUser?.username || '访客')

onMounted(async () => {
  if (authStore.isWebLoggedIn) {
    await loadChatData()
  }
})

watch(
  () => authStore.isWebLoggedIn,
  async (loggedIn) => {
    if (loggedIn) {
      await loadChatData()
    } else {
      chatStore.$reset()
    }
  }
)

async function loadChatData() {
  try {
    await chatStore.loadSessions(authStore.webToken)
    if (chatStore.activeSessionId) {
      await chatStore.loadMessages(authStore.webToken, chatStore.activeSessionId)
    }
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function handleAuthSuccess() {
  await loadChatData()
}

async function openSession(sessionId: string) {
  try {
    await chatStore.loadMessages(authStore.webToken, sessionId)
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

function fillPrompt(prompt: string) {
  draft.value = prompt
}

async function sendMessage() {
  const content = draft.value.trim()
  if (!content) {
    ElMessage.warning('请输入消息内容')
    return
  }
  if (!authStore.isWebLoggedIn) {
    authDialogVisible.value = true
    return
  }
  try {
    await chatStore.sendMessage(authStore.webToken, content)
    draft.value = ''
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

async function logout() {
  await authStore.logoutWeb()
  ElMessage.success('已退出登录')
}

function formatTime(value?: string) {
  if (!value) return '刚刚'
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped lang="scss">
.public-grid {
  display: grid;
  grid-template-columns: minmax(280px, 16.7vw) minmax(0, 1fr);
  gap: 20px;
  min-height: calc(100vh - 48px);
}

.public-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-top {
  display: grid;
  gap: 16px;
}

.user-card {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.52);

  p {
    margin: 6px 0 0;
    color: var(--sc-text-soft);
    font-size: 0.92rem;
  }
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  color: white;
  font-family: var(--sc-font-title);
  font-size: 1.1rem;
  background: linear-gradient(135deg, var(--sc-primary), var(--sc-accent));
}

.avatar.guest {
  background: linear-gradient(135deg, #64748b, #94a3b8);
}

.sidebar-actions {
  display: grid;
  gap: 10px;
}

.sidebar-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow: auto;
}

.conversation-item {
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 18px;
  padding: 14px;
  text-align: left;
  background: rgba(255, 255, 255, 0.48);
  transition: border-color 0.2s ease, transform 0.2s ease, background 0.2s ease;

  strong,
  span,
  small {
    display: block;
  }

  span,
  small {
    margin-top: 6px;
    color: var(--sc-text-soft);
  }
}

.conversation-item:hover,
.conversation-item.active {
  border-color: rgba(14, 165, 233, 0.4);
  background: rgba(255, 255, 255, 0.82);
  transform: translateY(-1px);
}

.empty-tip {
  padding: 14px 4px;
  line-height: 1.7;
}

.public-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 20px;
  min-height: 0;
}

.public-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;

  h1 {
    margin: 12px 0 12px;
    font-family: var(--sc-font-title);
    font-size: clamp(2rem, 3vw, 3.6rem);
    line-height: 0.98;
  }

  p {
    max-width: 760px;
    margin: 0;
    color: var(--sc-text-soft);
    line-height: 1.8;
    font-size: 1.02rem;
  }
}

.header-pill {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  color: var(--sc-primary-deep);
  background: rgba(14, 165, 233, 0.1);
  font-weight: 700;
}

.hero-state {
  display: grid;
  align-content: start;
  gap: 22px;
}

.hero-copy {
  padding: 26px;
  border-radius: 28px;
  background:
    linear-gradient(135deg, rgba(14, 165, 233, 0.12), rgba(249, 115, 22, 0.12)),
    rgba(255, 255, 255, 0.56);

  h2 {
    margin: 0 0 12px;
    font-family: var(--sc-font-title);
    font-size: 1.9rem;
  }

  p {
    margin: 0;
    line-height: 1.9;
    color: var(--sc-text-soft);
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.quick-card {
  cursor: pointer;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 22px;
  text-align: left;
  transition: transform 0.2s ease, border-color 0.2s ease;

  strong,
  span {
    display: block;
  }

  span {
    margin-top: 10px;
    color: var(--sc-text-soft);
    line-height: 1.7;
  }
}

.quick-card:hover {
  transform: translateY(-2px);
  border-color: rgba(249, 115, 22, 0.35);
}

.message-list {
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-right: 8px;
}

.message-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-item.user {
  align-items: flex-end;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--sc-text-soft);
  font-size: 0.92rem;
}

.message-bubble {
  max-width: min(75%, 860px);
  padding: 16px 18px;
  border-radius: 24px;
  white-space: pre-wrap;
  line-height: 1.8;
}

.message-item.assistant .message-bubble {
  border-top-left-radius: 10px;
  background: rgba(255, 255, 255, 0.74);
}

.message-item.user .message-bubble {
  color: white;
  border-top-right-radius: 10px;
  background: linear-gradient(135deg, var(--sc-primary), var(--sc-accent));
}

.composer {
  border-radius: 24px;
  padding: 18px;
  display: grid;
  gap: 14px;
}

.composer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

@media (max-width: 1100px) {
  .public-grid {
    grid-template-columns: 1fr;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }

  .public-header {
    flex-direction: column;
  }
}
</style>
