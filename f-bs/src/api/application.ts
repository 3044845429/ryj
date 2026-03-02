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

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers ?? {}),
  }

  const token = localStorage.getItem('token')
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  try {
    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
    })

    if (!response.ok) {
      try {
        const errorJson: ApiResponse<T> = await response.json()
        throw new Error(errorJson?.message || `请求失败: ${response.status} ${response.statusText}`)
      } catch (parseError) {
        throw new Error(`请求失败: ${response.status} ${response.statusText}`)
      }
    }

    const json: ApiResponse<T> = await response.json()
    if (json.code !== 200) {
      throw new Error(json?.message || '请求失败')
    }
    return json.data
  } catch (error) {
    if (error instanceof Error) {
      throw error
    }
    throw new Error('网络请求失败，请检查网络连接')
  }
}

export type ApplicationStatus = 'SUBMITTED' | 'REVIEWING' | 'INTERVIEW' | 'OFFERED' | 'REJECTED'

export interface JobApplication {
  id: number
  jobId: number
  studentId: number
  resumeId: number
  status: ApplicationStatus
  coverLetter: string | null
  appliedAt: string
}

export interface JobApplicationWithDetails extends JobApplication {
  jobTitle?: string
  jobLocation?: string
  jobSalaryRange?: string
  jobWorkType?: string
  companyName?: string
  resumeTitle?: string
}

export interface JobApplicationRequest {
  jobId: number
  studentId: number
  resumeId: number
  coverLetter?: string
  status?: ApplicationStatus
}

/**
 * 学生端「我的申请」列表（含职位名、公司名、状态），支持状态筛选
 */
export interface StudentApplicationItem {
  id: number
  jobId: number
  studentId: number
  resumeId: number
  status: ApplicationStatus
  coverLetter: string | null
  appliedAt: string
  jobTitle?: string
  companyName?: string
  location?: string
  salaryRange?: string
  workType?: string
}

export async function getMyApplications(
  studentId: number,
  options?: {
    page?: number
    size?: number
    status?: ApplicationStatus
  }
): Promise<Page<StudentApplicationItem>> {
  const params = new URLSearchParams()
  params.set('studentId', studentId.toString())
  if (options?.page) params.set('page', options.page.toString())
  if (options?.size) params.set('size', options.size.toString())
  if (options?.status) params.set('status', options.status)
  return request<Page<StudentApplicationItem>>(`/api/job-applications/my?${params.toString()}`)
}

/**
 * 获取学生的求职申请列表（原始接口，仅基础字段）
 */
export async function getStudentApplications(
  studentId: number,
  options?: {
    page?: number
    size?: number
    status?: ApplicationStatus
  }
): Promise<Page<JobApplication>> {
  const params = new URLSearchParams()
  params.append('studentId', studentId.toString())
  if (options?.page) {
    params.append('page', options.page.toString())
  }
  if (options?.size) {
    params.append('size', options.size.toString())
  }
  return request<Page<JobApplication>>(`/api/job-applications?${params.toString()}`)
}

/**
 * 获取申请详情
 */
export async function getApplicationDetail(id: number): Promise<JobApplication> {
  return request<JobApplication>(`/api/job-applications/${id}`)
}

/**
 * 更新申请（修改简历或求职信）
 */
export async function updateApplication(
  id: number,
  payload: {
    resumeId?: number
    coverLetter?: string
  }
): Promise<boolean> {
  return request<boolean>(`/api/job-applications/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

/**
 * 删除申请（撤销申请）
 */
export async function deleteApplication(id: number): Promise<boolean> {
  return request<boolean>(`/api/job-applications/${id}`, {
    method: 'DELETE',
  })
}

