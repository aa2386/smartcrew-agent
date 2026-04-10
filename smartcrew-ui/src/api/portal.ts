import { request } from './http'
import type {
  AgentRecord,
  AgentPromptBindingRecord,
  ChatMessage,
  ChatSession,
  CurrentUser,
  LoginResponse,
  PreferenceRecord,
  PromptRecord,
  UserIdentityRecord,
  UserRecord
} from '../types'

interface TablePayload<T> {
  rows: T[]
  total: number
}

interface PageParams {
  pageNum?: number
  pageSize?: number
}

export const webPortalApi = {
  register(payload: { username: string; password: string; displayName: string }) {
    return request<LoginResponse>('/api/web/auth/register', {
      method: 'POST',
      bodyJson: payload
    })
  },
  login(payload: { username: string; password: string }) {
    return request<LoginResponse>('/api/web/auth/login', {
      method: 'POST',
      bodyJson: payload
    })
  },
  logout(token: string) {
    return request<void>('/api/web/auth/logout', {
      method: 'POST',
      token
    })
  },
  me(token: string) {
    return request<CurrentUser>('/api/web/auth/me', {
      token
    })
  },
  createSession(token: string) {
    return request<ChatSession>('/api/web/chat/sessions', {
      method: 'POST',
      token
    })
  },
  listSessions(token: string) {
    return request<ChatSession[]>('/api/web/chat/sessions', {
      token
    })
  },
  listMessages(token: string, sessionId: string) {
    return request<ChatMessage[]>(`/api/web/chat/sessions/${sessionId}/messages`, {
      token
    })
  },
  sendMessage(token: string, sessionId: string, message: string) {
    return request<ChatMessage>(`/api/web/chat/sessions/${sessionId}/messages`, {
      method: 'POST',
      token,
      bodyJson: { message }
    })
  }
}

export const adminPortalApi = {
  login(payload: { username: string; password: string }) {
    return request<LoginResponse>('/api/admin/auth/login', {
      method: 'POST',
      bodyJson: payload
    })
  },
  logout(token: string) {
    return request<void>('/api/admin/auth/logout', {
      method: 'POST',
      token
    })
  },
  listUsers(token: string, params: { keyword?: string } & PageParams = {}) {
    const search = new URLSearchParams()
    if (params.keyword) search.set('keyword', params.keyword)
    if (params.pageNum) search.set('pageNum', String(params.pageNum))
    if (params.pageSize) search.set('pageSize', String(params.pageSize))
    const query = search.toString()
    return request<TablePayload<UserRecord>>(`/api/admin/users${query ? `?${query}` : ''}`, {
      token
    })
  },
  getUser(token: string, userId: number) {
    return request<UserRecord>(`/api/admin/users/${userId}`, {
      token
    })
  },
  updateUserStatus(token: string, userId: number, status: string) {
    return request<UserRecord>(`/api/admin/users/${userId}/status`, {
      method: 'PUT',
      token,
      bodyJson: { status }
    })
  },
  listUserIdentities(token: string, userId: number) {
    return request<UserIdentityRecord[]>(`/api/admin/users/${userId}/identities`, {
      token
    })
  },
  bindUserIdentity(token: string, userId: number, payload: Omit<UserIdentityRecord, 'id' | 'userId'>) {
    return request<UserIdentityRecord>(`/api/admin/users/${userId}/identities`, {
      method: 'POST',
      token,
      bodyJson: payload
    })
  },
  unbindUserIdentity(token: string, userId: number, identityId: number) {
    return request<void>(`/api/admin/users/${userId}/identities/${identityId}`, {
      method: 'DELETE',
      token
    })
  },
  listAgents(token: string) {
    return request<TablePayload<AgentRecord>>('/api/admin/agents', {
      token
    })
  },
  getAgent(token: string, code: string) {
    return request<AgentRecord>(`/api/admin/agents/${code}`, {
      token
    })
  },
  listAgentPromptBindings(token: string, code: string) {
    return request<AgentPromptBindingRecord[]>(`/api/admin/agents/${code}/prompt-bindings`, {
      token
    })
  },
  createAgent(token: string, payload: Partial<AgentRecord>) {
    return request<AgentRecord>('/api/admin/agents', {
      method: 'POST',
      token,
      bodyJson: payload
    })
  },
  updateAgent(token: string, code: string, payload: Partial<AgentRecord>) {
    return request<AgentRecord>(`/api/admin/agents/${code}`, {
      method: 'PUT',
      token,
      bodyJson: payload
    })
  },
  updateAgentPromptBindings(token: string, code: string, payload: { bindings: Array<{ promptTemplateId: number }> }) {
    return request<AgentPromptBindingRecord[]>(`/api/admin/agents/${code}/prompt-bindings`, {
      method: 'PUT',
      token,
      bodyJson: payload
    })
  },
  listPrompts(token: string) {
    return request<TablePayload<PromptRecord>>('/api/admin/prompts', {
      token
    })
  },
  listPromptCategories(token: string, params: PageParams) {
    const search = new URLSearchParams()
    if (params.pageNum) search.set('pageNum', String(params.pageNum))
    if (params.pageSize) search.set('pageSize', String(params.pageSize))
    const query = search.toString()
    return request<TablePayload<PromptRecord>>(`/api/admin/prompts/categories${query ? `?${query}` : ''}`, {
      token
    })
  },
  getPromptByCategory(token: string, category: string) {
    return request<PromptRecord>(`/api/admin/prompts/category/${category}`, {
      token
    })
  },
  createPrompt(
    token: string,
    payload: Pick<PromptRecord, 'category' | 'templateName' | 'templateContent' | 'remark'>
  ) {
    return request<PromptRecord>('/api/admin/prompts', {
      method: 'POST',
      token,
      bodyJson: payload
    })
  },
  updatePrompt(
    token: string,
    id: number,
    payload: Pick<PromptRecord, 'category' | 'templateName' | 'templateContent' | 'remark'>
  ) {
    return request<PromptRecord>(`/api/admin/prompts/${id}`, {
      method: 'PUT',
      token,
      bodyJson: payload
    })
  },
  deletePrompt(token: string, id: number) {
    return request<void>(`/api/admin/prompts/${id}`, {
      method: 'DELETE',
      token
    })
  },
  listPreferences(token: string, userId?: number) {
    const query = userId ? `?userId=${userId}` : ''
    return request<TablePayload<PreferenceRecord>>(`/api/admin/preferences${query}`, {
      token
    })
  },
  updatePreference(token: string, userId: number, payload: Omit<PreferenceRecord, 'id' | 'userId'>) {
    return request<PreferenceRecord>(`/api/admin/preferences/${userId}`, {
      method: 'PUT',
      token,
      bodyJson: payload
    })
  },
  deletePreference(token: string, userId: number, prefKey: string) {
    return request<void>(`/api/admin/preferences/${userId}/${prefKey}`, {
      method: 'DELETE',
      token
    })
  },
  listConversationSessions(
    token: string,
    params: { userId?: number; provider?: string; keyword?: string } & PageParams
  ) {
    const search = new URLSearchParams()
    if (params.userId) search.set('userId', String(params.userId))
    if (params.provider) search.set('provider', params.provider)
    if (params.keyword) search.set('keyword', params.keyword)
    if (params.pageNum) search.set('pageNum', String(params.pageNum))
    if (params.pageSize) search.set('pageSize', String(params.pageSize))
    const query = search.toString()
    return request<TablePayload<ChatSession>>(`/api/admin/conversations/sessions${query ? `?${query}` : ''}`, {
      token
    })
  },
  listConversationMessages(token: string, params: { userId?: number; sessionId?: string }) {
    const search = new URLSearchParams()
    if (params.userId) search.set('userId', String(params.userId))
    if (params.sessionId) search.set('sessionId', params.sessionId)
    const query = search.toString()
    return request<ChatMessage[]>(`/api/admin/conversations/messages${query ? `?${query}` : ''}`, {
      token
    })
  }
}
