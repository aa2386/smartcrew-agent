export interface ApiEnvelope<T> {
  success: boolean
  code: number
  message: string
  data: T
}

export interface CurrentUser {
  userId: number
  username: string
  displayName: string
  role: string
  avatarUrl?: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  sessionId: string
  expireAt: string
  user: CurrentUser
}

export interface SessionState {
  token: string
  sessionId: string
  expireAt: string
  user: CurrentUser
}

export interface ChatSession {
  sessionId: string
  title: string
  preview: string
  messageCount: number
  lastMessageAt: string
  source: string
}

export interface ChatMessage {
  id?: number
  sessionId: string
  messageSeq?: number
  role: string
  content: string
  traceId?: string
  createTime: string
}

export interface UserRecord {
  id: number
  username: string
  displayName: string
  avatarUrl?: string
  role: string
  status: string
  lastLoginAt?: string
}

export interface UserIdentityRecord {
  id: number
  userId: number
  provider: string
  providerUserId: string
  tenantKey: string
  profileSnapshotJson?: string
}

export interface AgentRecord {
  id?: number
  agentCode: string
  agentName: string
  agentType: string
  description?: string
  strategyType?: string
  systemPrompt?: string
  configJson?: string
  enabled: boolean
  runtimeMode?: string
  beanClassName?: string
  sourceStatus?: string
  hasCodeBean?: boolean
  hasDatabaseConfig?: boolean
}

export interface AgentPromptBindingRecord {
  id?: number
  agentCode: string
  promptTemplateId: number
  templateName: string
  category: string
  templateContent?: string
  sortOrder: number
}

export interface PromptRecord {
  id: number
  templateName: string
  templateContent: string
  category: string
  remark?: string
}

export interface PreferenceRecord {
  id?: number
  userId: number
  prefKey: string
  prefValue: string
  prefType: string
  source: string
}

export interface TableData<T> {
  rows: T[]
  total: number
}

export interface KnowledgeBaseRecord {
  id?: number
  baseCode: string
  baseName: string
  description?: string
  embeddingModel: string
  collectionName: string
  enabled: boolean
  documentCount: number
  chunkCount: number
  agentCount: number
  processingDocumentCount: number
  hasDocuments: boolean
  collectionNameEditable: boolean
  embeddingModelEditable: boolean
  createTime?: string
  updateTime?: string
}

export interface KnowledgeDocumentRecord {
  id?: number
  baseId?: number
  baseCode: string
  documentCode: string
  documentName: string
  filePath?: string
  fileType: string
  fileSize: number
  status: string
  chunkCount: number
  errorMessage?: string
  createTime?: string
  updateTime?: string
}

export interface DocumentChunkRecord {
  id?: number
  documentId?: number
  documentCode: string
  documentName: string
  chunkIndex: number
  content: string
  contentPreview: string
  vectorId?: string
  tokenCount?: number
  metadata?: string
  createTime?: string
  updateTime?: string
}

export interface KnowledgeBaseAgentOptionRecord {
  agentCode: string
  agentName: string
  agentType: string
  enabled: boolean
}

export interface KnowledgeBaseAgentBindingRecord {
  baseCode: string
  boundAgents: KnowledgeBaseAgentOptionRecord[]
  availableAgents: KnowledgeBaseAgentOptionRecord[]
}
