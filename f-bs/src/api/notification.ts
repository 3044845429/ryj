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

export interface SystemNotification {
  id: number
  userId: number | null
  category: 'SYSTEM' | 'INTERVIEW' | 'APPLICATION' | 'GUIDANCE'
  title: string
  content: string
  readFlag: boolean
  createdAt: string
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('token')
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers ?? {}),
  }
  
  // 如果有 token，添加到 headers
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

/**
 * 获取用户通知列表
 */
export async function getUserNotifications(
  userId: number,
  options?: {
    page?: number
    size?: number
    unreadOnly?: boolean
  }
): Promise<Page<SystemNotification>> {
  const params = new URLSearchParams()
  params.append('userId', userId.toString())
  if (options?.page) {
    params.append('page', options.page.toString())
  }
  if (options?.size) {
    params.append('size', options.size.toString())
  }
  if (options?.unreadOnly) {
    params.append('unreadOnly', 'true')
  }
  return request<Page<SystemNotification>>(`/api/notifications?${params.toString()}`)
}

/**
 * 获取未读通知数量
 */
export async function getUnreadNotificationCount(userId: number): Promise<number> {
  const result = await getUserNotifications(userId, {
    page: 1,
    size: 1,
    unreadOnly: true,
  })
  return result.total
}

/**
 * 标记通知为已读
 */
export async function markNotificationAsRead(notificationId: number): Promise<boolean> {
  const result = await request<boolean>(`/api/notifications/${notificationId}`, {
    method: 'PUT',
    body: JSON.stringify({
      readFlag: true,
    }),
  })
  return result
}

/**
 * 标记所有通知为已读
 */
export async function markAllNotificationsAsRead(userId: number): Promise<boolean> {
  // 获取所有未读通知
  const result = await getUserNotifications(userId, {
    page: 1,
    size: 1000,
    unreadOnly: true,
  })
  
  // 批量标记为已读
  const promises = result.records.map(notification => 
    markNotificationAsRead(notification.id)
  )
  
  await Promise.all(promises)
  return true
}

/**
 * 删除通知
 */
export async function deleteNotification(notificationId: number): Promise<boolean> {
  return request<boolean>(`/api/notifications/${notificationId}`, {
    method: 'DELETE',
  })
}

