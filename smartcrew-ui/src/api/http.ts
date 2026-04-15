import { portalConfig } from '../config/portal'
import type { ApiEnvelope } from '../types'

interface RequestOptions extends RequestInit {
  token?: string
  bodyJson?: unknown
  bodyFormData?: FormData
}

interface TableEnvelope<T> {
  code: number
  message: string
  total: number
  rows: T[]
}

function isApiEnvelope<T>(payload: unknown): payload is ApiEnvelope<T> {
  return Boolean(payload) && typeof payload === 'object' && 'success' in (payload as Record<string, unknown>)
}

function isTableEnvelope<T>(payload: unknown): payload is TableEnvelope<T> {
  return Boolean(payload) && typeof payload === 'object' && 'rows' in (payload as Record<string, unknown>) && 'total' in (payload as Record<string, unknown>)
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { token, bodyJson, bodyFormData, ...init } = options
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  if (bodyJson !== undefined) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const response = await fetch(`${portalConfig.apiBaseUrl}${normalizedPath}`, {
    ...init,
    headers,
    body: bodyJson !== undefined ? JSON.stringify(bodyJson) : bodyFormData ?? options.body
  })

  const rawText = await response.text()
  const payload = rawText ? (JSON.parse(rawText) as unknown) : null

  if (!response.ok) {
    const message =
      payload && typeof payload === 'object' && 'message' in payload
        ? String((payload as Record<string, unknown>).message || '请求失败，请稍后重试')
        : '请求失败，请稍后重试'
    throw new Error(message)
  }

  if (isApiEnvelope<T>(payload)) {
    if (!payload.success) {
      throw new Error(payload.message || '请求失败，请稍后重试')
    }
    return payload.data
  }

  if (isTableEnvelope<unknown>(payload)) {
    if (payload.code !== 200) {
      throw new Error(payload.message || '列表数据获取失败')
    }
    return payload as T
  }

  if (payload && typeof payload === 'object' && 'code' in payload) {
    const code = Number((payload as Record<string, unknown>).code)
    if (!Number.isNaN(code) && code !== 200) {
      throw new Error(String((payload as Record<string, unknown>).message || '请求失败，请稍后重试'))
    }
  }

  return payload as T
}
