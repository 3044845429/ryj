const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers ?? {}),
  }

  try {
    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
    })

    // 检查响应状态
    if (!response.ok) {
      // 尝试解析错误响应
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
    // 如果是网络错误或其他错误，重新抛出
    if (error instanceof Error) {
      throw error
    }
    throw new Error('网络请求失败，请检查网络连接')
  }
}

export type UserRole = 'STUDENT' | 'TEACHER' | 'EMPLOYER' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'DISABLED'

export interface AdminDashboardHeader {
  totalUsers: number
  activeUsers: number
  totalStudents: number
  totalEmployers: number
  totalTeachers: number
  totalJobPostings: number
  activeJobPostings: number
  totalApplications: number
  pendingNotifications: number
}

export interface AdminModuleInfo {
  key: string
  name: string
  description: string
  capabilities: string[]
}

export interface DailyStat {
  date: string
  count: number
}

export interface UserStatistics {
  totalUsers: number
  activeUsers: number
  disabledUsers: number
  usersByRole: Record<UserRole, number>
  newUsersLast30Days: number
  activeUsersLast7Days: number
  dailyNewUsersLast30Days: DailyStat[]
  activeUsersByRole: Record<UserRole, number>
  dailyActiveUsersLast30Days: DailyStat[]
}

export interface ContentStatistics {
  totalJobPostings: number
  activeJobPostings: number
  totalApplications: number
  totalResumes: number
  totalNotifications: number
  unreadNotifications: number
  dailyJobPostingsLast30Days: DailyStat[]
  dailyApplicationsLast30Days: DailyStat[]
  dailyResumesLast30Days: DailyStat[]
  dailyNotificationsLast30Days: DailyStat[]
  jobPostingsLast7Days: number
  applicationsLast7Days: number
  resumesLast7Days: number
  notificationsLast7Days: number
  contentByType: Record<string, number>
}

export interface SystemStatistics {
  totalInterviews: number
  scheduledInterviews: number
  completedInterviews: number
  totalGuidanceRecords: number
  totalSearchHistory: number
  searchesLast30Days: number
  dailySearchesLast30Days: DailyStat[]
  guidanceRecordsLast30Days: number
  interviewsLast30Days: number
  publicResourcesLast30Days: number
  systemActivityByType: Record<string, number>
}

export interface UserOverview {
  id: number
  username: string
  fullName: string | null
  role: UserRole
  status: UserStatus
  email: string | null
  phone: string | null
  createdAt: string | null
  lastActiveAt: string | null
}

export interface ActivityLog {
  id: number
  action: string
  description: string
  userId: string
  userName: string
  timestamp: string | null
  category: string
}

export interface AdminDashboardResponse {
  header: AdminDashboardHeader
  modules: AdminModuleInfo[]
  userStatistics: UserStatistics
  contentStatistics: ContentStatistics
  systemStatistics: SystemStatistics
  recentUsers: UserOverview[]
  recentActivities: ActivityLog[]
}

export interface AdminUserRequest {
  username?: string
  password?: string
  fullName: string
  email?: string
  phone?: string
  role: UserRole
  status?: UserStatus
}

export interface AdminNotificationRequest {
  userId?: number | null
  title: string
  content: string
  category?: 'SYSTEM' | 'INTERVIEW' | 'APPLICATION' | 'GUIDANCE'
  readFlag?: boolean
}

export type ProfileUpdateRole = 'TEACHER' | 'EMPLOYER'
export type ProfileUpdateStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface ProfileUpdateRequestRecord {
  id: number
  userId: number
  role: ProfileUpdateRole
  payload: string
  status: ProfileUpdateStatus
  createdAt: string
  reviewedAt?: string | null
  reviewerId?: number | null
  reviewComment?: string | null
}

export interface ProfileUpdateRequestPage {
  records: ProfileUpdateRequestRecord[]
  total: number
  size: number
  current: number
  pages?: number
}

export interface UserPage {
  records: User[]
  total: number
  size: number
  current: number
  pages?: number
}

export interface User {
  id: number
  username: string
  fullName: string | null
  email: string | null
  phone: string | null
  role: UserRole
  status: UserStatus
  createdAt: string | null
  updatedAt: string | null
}

export async function fetchAdminDashboard(adminUserId: number): Promise<AdminDashboardResponse> {
  return request<AdminDashboardResponse>(`/api/admin/dashboard?userId=${adminUserId}`)
}

export async function fetchUsers(params: {
  adminUserId: number
  page?: number
  size?: number
  role?: UserRole
  status?: UserStatus
  keyword?: string
}): Promise<UserPage> {
  const searchParams = new URLSearchParams()
  searchParams.set('adminUserId', String(params.adminUserId))
  if (params.page) searchParams.set('page', String(params.page))
  if (params.size) searchParams.set('size', String(params.size))
  if (params.role) searchParams.set('role', params.role)
  if (params.status) searchParams.set('status', params.status)
  if (params.keyword) searchParams.set('keyword', params.keyword)
  return request<UserPage>(`/api/admin/users?${searchParams.toString()}`)
}

export async function fetchUserDetail(adminUserId: number, userId: number): Promise<User> {
  return request<User>(`/api/admin/users/${userId}?adminUserId=${adminUserId}`)
}

export async function createUser(adminUserId: number, payload: AdminUserRequest): Promise<User> {
  return request<User>(`/api/admin/users?adminUserId=${adminUserId}`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function updateUser(
  adminUserId: number,
  userId: number,
  payload: AdminUserRequest,
): Promise<User> {
  return request<User>(`/api/admin/users/${userId}?adminUserId=${adminUserId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function updateUserStatus(
  adminUserId: number,
  userId: number,
  status: UserStatus,
): Promise<boolean> {
  return request<boolean>(`/api/admin/users/${userId}/status?adminUserId=${adminUserId}&status=${status}`, {
    method: 'PUT',
  })
}

export async function updateUserRole(
  adminUserId: number,
  userId: number,
  role: UserRole,
): Promise<boolean> {
  return request<boolean>(`/api/admin/users/${userId}/role?adminUserId=${adminUserId}&role=${role}`, {
    method: 'PUT',
  })
}

export async function deleteUser(adminUserId: number, userId: number): Promise<boolean> {
  return request<boolean>(`/api/admin/users/${userId}?adminUserId=${adminUserId}`, {
    method: 'DELETE',
  })
}

export async function sendSystemNotification(
  adminUserId: number,
  payload: AdminNotificationRequest,
): Promise<boolean> {
  return request<boolean>(`/api/admin/notifications?adminUserId=${adminUserId}`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function fetchProfileUpdateRequests(params: {
  adminUserId: number
  page?: number
  size?: number
  role?: ProfileUpdateRole
  status?: ProfileUpdateStatus
}): Promise<ProfileUpdateRequestPage> {
  const searchParams = new URLSearchParams()
  searchParams.set('adminUserId', String(params.adminUserId))
  if (params.page) searchParams.set('page', String(params.page))
  if (params.size) searchParams.set('size', String(params.size))
  if (params.role) searchParams.set('role', params.role)
  if (params.status) searchParams.set('status', params.status)
  return request<ProfileUpdateRequestPage>(`/api/admin/profile-requests?${searchParams.toString()}`)
}

export async function approveProfileUpdateRequest(
  adminUserId: number,
  requestId: number,
  reviewComment?: string,
): Promise<boolean> {
  return request<boolean>(`/api/admin/profile-requests/${requestId}/approve?adminUserId=${adminUserId}`, {
    method: 'PUT',
    body: JSON.stringify({ reviewComment: reviewComment ?? '' }),
  })
}

export async function rejectProfileUpdateRequest(
  adminUserId: number,
  requestId: number,
  reviewComment?: string,
): Promise<boolean> {
  return request<boolean>(`/api/admin/profile-requests/${requestId}/reject?adminUserId=${adminUserId}`, {
    method: 'PUT',
    body: JSON.stringify({ reviewComment: reviewComment ?? '' }),
  })
}

