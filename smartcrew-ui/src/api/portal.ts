import { request } from './http'
import type {
  AgentRecord,
  CollaborationLogRecord,
  CollaborationLogStepRecord,
  AgentPromptBindingRecord,
  AgentToolBindingRecord,
  ChatMessage,
  ChatSession,
  CurrentUser,
  DocumentChunkRecord,
  KnowledgeBaseAgentBindingRecord,
  KnowledgeBaseRecord,
  KnowledgeDocumentRecord,
  LoginResponse,
  PreferenceRecord,
  PromptRecord,
  ToolExecutionResultRecord,
  ToolRecord,
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

/** 面向公众用户的 API 集合（注册、登录、聊天会话）。 */
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

/** 面向后台管理员的 API 集合（用户、Agent、工具、知识库、协作日志等管理接口）。 */
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
  listAgentToolBindings(token: string, code: string) {
    return request<AgentToolBindingRecord>(`/api/admin/agents/${code}/tool-bindings`, {
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
  updateAgentToolBindings(token: string, code: string, toolCodes: string[]) {
    return request<AgentToolBindingRecord>(`/api/admin/agents/${code}/tool-bindings`, {
      method: 'PUT',
      token,
      bodyJson: { toolCodes }
    })
  },
  listTools(token: string) {
    return request<TablePayload<ToolRecord>>('/api/admin/tools', {
      token
    })
  },
  getTool(token: string, code: string) {
    return request<ToolRecord>(`/api/admin/tools/${code}`, {
      token
    })
  },
  createTool(token: string, payload: Partial<ToolRecord>) {
    return request<ToolRecord>('/api/admin/tools', {
      method: 'POST',
      token,
      bodyJson: payload
    })
  },
  updateTool(token: string, code: string, payload: Partial<ToolRecord>) {
    return request<ToolRecord>(`/api/admin/tools/${code}`, {
      method: 'PUT',
      token,
      bodyJson: payload
    })
  },
  executeTool(
    token: string,
    code: string,
    payload: { actionName?: string; arguments?: Record<string, unknown>; executionContext?: Record<string, unknown> }
  ) {
    return request<ToolExecutionResultRecord>(`/api/admin/tools/${code}/execute`, {
      method: 'POST',
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
  },
  /**
   * 分页查询协作日志，支持按 Trace ID、会话 ID、Agent、步骤类型、状态、关键词和时间范围筛选。
   */
  listCollaborationLogs(
    token: string,
    params: {
      traceId?: string
      rootSessionId?: string
      agentCode?: string
      stepType?: string
      status?: string
      keyword?: string
      startTimeFrom?: string
      startTimeTo?: string
    } & PageParams
  ) {
    const search = new URLSearchParams()
    if (params.traceId) search.set('traceId', params.traceId)
    if (params.rootSessionId) search.set('rootSessionId', params.rootSessionId)
    if (params.agentCode) search.set('agentCode', params.agentCode)
    if (params.stepType) search.set('stepType', params.stepType)
    if (params.status) search.set('status', params.status)
    if (params.keyword) search.set('keyword', params.keyword)
    if (params.startTimeFrom) search.set('startTimeFrom', params.startTimeFrom)
    if (params.startTimeTo) search.set('startTimeTo', params.startTimeTo)
    if (params.pageNum) search.set('pageNum', String(params.pageNum))
    if (params.pageSize) search.set('pageSize', String(params.pageSize))
    const query = search.toString()
    return request<TablePayload<CollaborationLogRecord>>(
      `/api/admin/collaboration-logs${query ? `?${query}` : ''}`,
      {
        token
      }
    )
  },
  /**
   * 按 Trace ID 查询协作日志的步骤时间线详情。
   */
  listCollaborationLogSteps(token: string, traceId: string) {
    return request<CollaborationLogStepRecord[]>(`/api/admin/collaboration-logs/${traceId}/steps`, {
      token
    })
  },
  listKnowledgeBases(
    token: string,
    params: { keyword?: string; enabled?: boolean } & PageParams = {}
  ) {
    const search = new URLSearchParams()
    if (params.keyword) search.set('keyword', params.keyword)
    if (params.enabled !== undefined) search.set('enabled', String(params.enabled))
    if (params.pageNum) search.set('pageNum', String(params.pageNum))
    if (params.pageSize) search.set('pageSize', String(params.pageSize))
    const query = search.toString()
    return request<TablePayload<KnowledgeBaseRecord>>(`/api/admin/knowledge-bases${query ? `?${query}` : ''}`, {
      token
    })
  },
  getKnowledgeBase(token: string, baseCode: string) {
    return request<KnowledgeBaseRecord>(`/api/admin/knowledge-bases/${baseCode}`, {
      token
    })
  },
  createKnowledgeBase(
    token: string,
    payload: Pick<KnowledgeBaseRecord, 'baseCode' | 'baseName' | 'description' | 'embeddingModel' | 'collectionName' | 'enabled'>
  ) {
    return request<KnowledgeBaseRecord>('/api/admin/knowledge-bases', {
      method: 'POST',
      token,
      bodyJson: payload
    })
  },
  updateKnowledgeBase(
    token: string,
    baseCode: string,
    payload: Pick<KnowledgeBaseRecord, 'baseCode' | 'baseName' | 'description' | 'embeddingModel' | 'collectionName' | 'enabled'>
  ) {
    return request<KnowledgeBaseRecord>(`/api/admin/knowledge-bases/${baseCode}`, {
      method: 'PUT',
      token,
      bodyJson: payload
    })
  },
  deleteKnowledgeBase(token: string, baseCode: string) {
    return request<void>(`/api/admin/knowledge-bases/${baseCode}`, {
      method: 'DELETE',
      token
    })
  },
  listKnowledgeDocuments(
    token: string,
    baseCode: string,
    params: { keyword?: string; status?: string; fileType?: string } & PageParams = {}
  ) {
    const search = new URLSearchParams()
    if (params.keyword) search.set('keyword', params.keyword)
    if (params.status) search.set('status', params.status)
    if (params.fileType) search.set('fileType', params.fileType)
    if (params.pageNum) search.set('pageNum', String(params.pageNum))
    if (params.pageSize) search.set('pageSize', String(params.pageSize))
    const query = search.toString()
    return request<TablePayload<KnowledgeDocumentRecord>>(
      `/api/admin/knowledge-bases/${baseCode}/documents${query ? `?${query}` : ''}`,
      { token }
    )
  },
  uploadKnowledgeDocuments(token: string, baseCode: string, files: File[]) {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))
    return request<KnowledgeDocumentRecord[]>(`/api/admin/knowledge-bases/${baseCode}/documents`, {
      method: 'POST',
      token,
      bodyFormData: formData
    })
  },
  reprocessKnowledgeDocument(token: string, baseCode: string, documentCode: string) {
    return request<KnowledgeDocumentRecord>(
      `/api/admin/knowledge-bases/${baseCode}/documents/${documentCode}/reprocess`,
      {
        method: 'POST',
        token
      }
    )
  },
  deleteKnowledgeDocument(token: string, baseCode: string, documentCode: string) {
    return request<void>(`/api/admin/knowledge-bases/${baseCode}/documents/${documentCode}`, {
      method: 'DELETE',
      token
    })
  },
  listDocumentChunks(
    token: string,
    baseCode: string,
    documentCode: string,
    params: { keyword?: string } & PageParams = {}
  ) {
    const search = new URLSearchParams()
    if (params.keyword) search.set('keyword', params.keyword)
    if (params.pageNum) search.set('pageNum', String(params.pageNum))
    if (params.pageSize) search.set('pageSize', String(params.pageSize))
    const query = search.toString()
    return request<TablePayload<DocumentChunkRecord>>(
      `/api/admin/knowledge-bases/${baseCode}/documents/${documentCode}/chunks${query ? `?${query}` : ''}`,
      { token }
    )
  },
  getKnowledgeBaseAgentBindings(token: string, baseCode: string) {
    return request<KnowledgeBaseAgentBindingRecord>(`/api/admin/knowledge-bases/${baseCode}/agent-bindings`, {
      token
    })
  },
  updateKnowledgeBaseAgentBindings(token: string, baseCode: string, agentCodes: string[]) {
    return request<KnowledgeBaseAgentBindingRecord>(`/api/admin/knowledge-bases/${baseCode}/agent-bindings`, {
      method: 'PUT',
      token,
      bodyJson: { agentCodes }
    })
  }
}
