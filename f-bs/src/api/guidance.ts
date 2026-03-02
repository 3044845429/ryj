const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

type Page<T> = {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface TeacherGuidance {
  id: number
  teacherId: number
  studentId: number
  note: string
  createdAt: string
}

export interface TeacherGuidanceRequest {
  teacherId: number
  studentId: number
  note: string
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('token')
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers ?? {}),
  }
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  const json: ApiResponse<T> = await response.json()
  if (!response.ok || json.code !== 200) {
    throw new Error(json?.message || '请求失败')
  }
  return json.data
}

/**
 * 获取指导记录列表
 */
export async function getGuidanceRecords(params: {
  page?: number
  size?: number
  teacherId?: number
  studentId?: number
}): Promise<Page<TeacherGuidance>> {
  const searchParams = new URLSearchParams()
  if (params.page) searchParams.set('page', params.page.toString())
  if (params.size) searchParams.set('size', params.size.toString())
  if (params.teacherId) searchParams.set('teacherId', params.teacherId.toString())
  if (params.studentId) searchParams.set('studentId', params.studentId.toString())
  
  const qs = searchParams.toString()
  return request<Page<TeacherGuidance>>(`/api/teacher-guidance${qs ? `?${qs}` : ''}`)
}

/**
 * 创建指导记录
 */
export async function createGuidanceRecord(payload: TeacherGuidanceRequest): Promise<TeacherGuidance> {
  return request<TeacherGuidance>('/api/teacher-guidance', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

/**
 * 更新指导记录
 */
export async function updateGuidanceRecord(id: number, payload: Partial<TeacherGuidanceRequest>): Promise<boolean> {
  return request<boolean>(`/api/teacher-guidance/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

/**
 * 删除指导记录
 */
export async function deleteGuidanceRecord(id: number): Promise<boolean> {
  return request<boolean>(`/api/teacher-guidance/${id}`, {
    method: 'DELETE',
  })
}

