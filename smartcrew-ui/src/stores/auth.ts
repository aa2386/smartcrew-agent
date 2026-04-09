import { defineStore } from 'pinia'
import { adminPortalApi, webPortalApi } from '../api/portal'
import type { CurrentUser, LoginResponse, SessionState } from '../types'

const WEB_KEY = 'smartcrew-web-session'
const ADMIN_KEY = 'smartcrew-admin-session'

function toSessionState(payload: LoginResponse): SessionState {
  return {
    token: payload.token,
    sessionId: payload.sessionId,
    expireAt: payload.expireAt,
    user: payload.user
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    webSession: null as SessionState | null,
    adminSession: null as SessionState | null
  }),
  getters: {
    webToken: (state) => state.webSession?.token ?? '',
    adminToken: (state) => state.adminSession?.token ?? '',
    webUser: (state) => state.webSession?.user as CurrentUser | undefined,
    adminUser: (state) => state.adminSession?.user as CurrentUser | undefined,
    isWebLoggedIn: (state) => Boolean(state.webSession?.token),
    isAdminLoggedIn: (state) => Boolean(state.adminSession?.token)
  },
  actions: {
    hydrate() {
      const webRaw = localStorage.getItem(WEB_KEY)
      const adminRaw = localStorage.getItem(ADMIN_KEY)
      this.webSession = webRaw ? (JSON.parse(webRaw) as SessionState) : null
      this.adminSession = adminRaw ? (JSON.parse(adminRaw) as SessionState) : null
    },
    saveWebSession(payload: LoginResponse) {
      this.webSession = toSessionState(payload)
      localStorage.setItem(WEB_KEY, JSON.stringify(this.webSession))
    },
    saveAdminSession(payload: LoginResponse) {
      this.adminSession = toSessionState(payload)
      localStorage.setItem(ADMIN_KEY, JSON.stringify(this.adminSession))
    },
    clearWebSession() {
      this.webSession = null
      localStorage.removeItem(WEB_KEY)
    },
    clearAdminSession() {
      this.adminSession = null
      localStorage.removeItem(ADMIN_KEY)
    },
    async loginWeb(payload: { username: string; password: string }) {
      const response = await webPortalApi.login(payload)
      this.saveWebSession(response)
      return response
    },
    async registerWeb(payload: { username: string; password: string; displayName: string }) {
      const response = await webPortalApi.register(payload)
      this.saveWebSession(response)
      return response
    },
    async logoutWeb() {
      if (this.webSession) {
        try {
          await webPortalApi.logout(this.webSession.token)
        } finally {
          this.clearWebSession()
        }
      }
    },
    async refreshWebUser() {
      if (!this.webSession) return
      const user = await webPortalApi.me(this.webSession.token)
      this.webSession.user = user
      localStorage.setItem(WEB_KEY, JSON.stringify(this.webSession))
    },
    async loginAdmin(payload: { username: string; password: string }) {
      const response = await adminPortalApi.login(payload)
      this.saveAdminSession(response)
      return response
    },
    async logoutAdmin() {
      if (this.adminSession) {
        try {
          await adminPortalApi.logout(this.adminSession.token)
        } finally {
          this.clearAdminSession()
        }
      }
    }
  }
})
