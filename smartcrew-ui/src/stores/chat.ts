import { defineStore } from 'pinia'
import { webPortalApi } from '../api/portal'
import type { ChatMessage, ChatSession } from '../types'

export const useChatStore = defineStore('chat', {
  state: () => ({
    sessions: [] as ChatSession[],
    messages: [] as ChatMessage[],
    activeSessionId: '' as string,
    loadingSessions: false,
    loadingMessages: false,
    sending: false
  }),
  actions: {
    async loadSessions(token: string) {
      this.loadingSessions = true
      try {
        this.sessions = await webPortalApi.listSessions(token)
        if (!this.activeSessionId && this.sessions.length > 0) {
          this.activeSessionId = this.sessions[0].sessionId
        }
      } finally {
        this.loadingSessions = false
      }
    },
    async ensureSession(token: string) {
      if (this.activeSessionId) {
        return this.activeSessionId
      }
      const session = await webPortalApi.createSession(token)
      this.activeSessionId = session.sessionId
      this.sessions = [session, ...this.sessions]
      return session.sessionId
    },
    async loadMessages(token: string, sessionId: string) {
      this.loadingMessages = true
      try {
        this.activeSessionId = sessionId
        this.messages = await webPortalApi.listMessages(token, sessionId)
      } finally {
        this.loadingMessages = false
      }
    },
    createDraftSession() {
      this.activeSessionId = ''
      this.messages = []
    },
    async sendMessage(token: string, message: string) {
      const sessionId = await this.ensureSession(token)
      const userMessage: ChatMessage = {
        sessionId,
        role: 'user',
        content: message,
        createTime: new Date().toISOString()
      }
      this.messages = [...this.messages, userMessage]
      this.sending = true
      try {
        const assistantMessage = await webPortalApi.sendMessage(token, sessionId, message)
        this.messages = [...this.messages, assistantMessage]
        await this.loadSessions(token)
      } finally {
        this.sending = false
      }
    }
  }
})
