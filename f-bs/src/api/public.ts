const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

export interface DynamicsItem {
  id: number
  title: string
  content: string | null
  category: string | null
  createdAt: string | null
}

export interface PublicDynamicsPage {
  records: DynamicsItem[]
  total: number
  current: number
  size: number
  pages: number
}

export interface PublicResourceSummary {
  id: number
  fileName: string
  description: string | null
  fileType: string | null
  fileSize: number
  downloadUrl: string
  createdAt: string
}

async function request<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`)
  const json: ApiResponse<T> = await response.json()
  if (!response.ok || json.code !== 200) {
    throw new Error(json?.message || '请求失败')
  }
  return json.data
}

/**
 * 获取资讯动态列表（网上发布的各类动态讯息，无需登录）
 */
export async function getPublicDynamics(params: {
  page?: number
  size?: number
  category?: string
}): Promise<PublicDynamicsPage> {
  const search = new URLSearchParams()
  if (params.page != null) search.set('page', String(params.page))
  if (params.size != null) search.set('size', String(params.size))
  if (params.category) search.set('category', params.category)
  const qs = search.toString()
  return request<PublicDynamicsPage>(`/api/public/dynamics${qs ? `?${qs}` : ''}`)
}

/**
 * 获取公开资料下载列表（所有角色可见）
 */
export async function getPublicResources(): Promise<PublicResourceSummary[]> {
  return request<PublicResourceSummary[]>('/api/public/files')
}
