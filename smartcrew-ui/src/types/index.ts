/**
 * 统一 API 响应信封结构。
 *
 * @template T 业务数据类型
 */
export interface ApiEnvelope<T> {
  success: boolean
  code: number
  message: string
  data: T
}

/** 当前登录用户信息。 */
export interface CurrentUser {
  userId: number
  username: string
  displayName: string
  role: string
  avatarUrl?: string
}

/** 登录接口响应数据。 */
export interface LoginResponse {
  token: string
  tokenType: string
  sessionId: string
  expireAt: string
  user: CurrentUser
}

/** 客户端会话状态，持久化到 localStorage。 */
export interface SessionState {
  token: string
  sessionId: string
  expireAt: string
  user: CurrentUser
}

/** 聊天会话摘要。 */
export interface ChatSession {
  sessionId: string
  title: string
  preview: string
  messageCount: number
  lastMessageAt: string
  source: string
}

/** 聊天消息记录。 */
export interface ChatMessage {
  id?: number
  sessionId: string
  messageSeq?: number
  role: string
  content: string
  traceId?: string
  createTime: string
}

/** 用户管理记录。 */
export interface UserRecord {
  id: number
  username: string
  displayName: string
  avatarUrl?: string
  role: string
  status: string
  lastLoginAt?: string
}

/** 用户第三方身份记录。 */
export interface UserIdentityRecord {
  id: number
  userId: number
  provider: string
  providerUserId: string
  tenantKey: string
  profileSnapshotJson?: string
}

/** 智能体记录。 */
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

/** 智能体提示词绑定记录。 */
export interface AgentPromptBindingRecord {
  id?: number
  agentCode: string
  promptTemplateId: number
  templateName: string
  category: string
  templateContent?: string
  sortOrder: number
}

/** 工具动作参数记录。 */
export interface ToolActionParameterRecord {
  name: string
  description?: string
  type?: string
  required?: boolean
}

/** 工具动作记录。 */
export interface ToolActionRecord {
  toolCode: string
  actionName: string
  description?: string
  parameters: ToolActionParameterRecord[]
}

/** 工具定义记录，包含运行时解析信息。 */
export interface ToolRecord {
  id?: number
  toolCode: string
  toolName: string
  description?: string
  beanName?: string
  riskLevel?: string
  enabled: boolean
  configJson?: string
  sourceStatus?: string
  hasCodeBean?: boolean
  hasDatabaseConfig?: boolean
  executable?: boolean
  resolveError?: string
  actions: ToolActionRecord[]
}

/** 工具执行结果记录。 */
export interface ToolExecutionResultRecord {
  toolCode: string
  actionName: string
  success: boolean
  output?: unknown
  errorMessage?: string
  durationMs?: number
}

/** 智能体工具绑定记录。 */
export interface AgentToolBindingRecord {
  agentCode: string
  boundTools: ToolRecord[]
  availableTools: ToolRecord[]
}

/** 提示词模板记录。 */
export interface PromptRecord {
  id: number
  templateName: string
  templateContent: string
  category: string
  remark?: string
}

/** 用户偏好设置记录。 */
export interface PreferenceRecord {
  id?: number
  userId: number
  prefKey: string
  prefValue: string
  prefType: string
  source: string
}

/** 通用分页表格数据结构。 */
export interface TableData<T> {
  rows: T[]
  total: number
}

/** 知识库记录。 */
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

/** 知识库文档记录。 */
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

/** 文档切片记录。 */
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

/** 知识库智能体选项（用于绑定选择）。 */
export interface KnowledgeBaseAgentOptionRecord {
  agentCode: string
  agentName: string
  agentType: string
  enabled: boolean
}

/** 知识库智能体绑定记录。 */
export interface KnowledgeBaseAgentBindingRecord {
  baseCode: string
  boundAgents: KnowledgeBaseAgentOptionRecord[]
  availableAgents: KnowledgeBaseAgentOptionRecord[]
}
