function resolveApiBaseUrl() {
  const devProxyTarget = import.meta.env.VITE_DEV_PROXY_TARGET?.trim()
  if (import.meta.env.DEV && devProxyTarget) {
    return ''
  }
  const configured = import.meta.env.VITE_API_BASE_URL?.trim()
  if (configured) {
    return configured.replace(/\/$/, '')
  }
  if (typeof window !== 'undefined') {
    return `${window.location.protocol}//${window.location.hostname}:8085`
  }
  return 'http://localhost:8085'
}

export const portalConfig = {
  apiBaseUrl: resolveApiBaseUrl(),
  enableWeb: import.meta.env.VITE_ENABLE_WEB !== 'false',
  enableAdmin: import.meta.env.VITE_ENABLE_ADMIN !== 'false'
}
