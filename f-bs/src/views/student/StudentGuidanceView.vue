<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getTeacherProfile, getAllHomeroomTeachers, type TeacherProfileInfo, type TeacherSearchItem } from '@/api/teacher'
import { getGuidanceRecords, createGuidanceRecord, type TeacherGuidance } from '@/api/guidance'
import { useRouter } from 'vue-router'

const router = useRouter()

type GuidanceRecordWithTeacher = TeacherGuidance & {
  teacherName?: string
  teacherDepartment?: string
}

const loading = ref(true)
const currentStudentId = ref<number | null>(null)
const currentTeacherProfile = ref<TeacherProfileInfo | null>(null)
const guidanceRecords = ref<GuidanceRecordWithTeacher[]>([])
const showSelectTeacherPrompt = ref(false)
const showSelectTeacherDialog = ref(false)
const teacherLoading = ref(false)
const teacherOptions = ref<TeacherSearchItem[]>([])
const selectedTeacherId = ref<number | null>(null)
const selecting = ref(false)

const message = ref<{ type: 'success' | 'error' | 'info'; text: string } | null>(null)
let messageTimer: number | undefined

const showMessage = (type: 'success' | 'error' | 'info', text: string) => {
  message.value = { type, text }
  if (messageTimer) {
    clearTimeout(messageTimer)
  }
  messageTimer = window.setTimeout(() => {
    message.value = null
  }, 5000) // info消息显示时间稍长
}

// 加载学生信息和当前指导老师
const loadStudentInfo = async () => {
  const stored = localStorage.getItem('userInfo')
  if (!stored) {
    showMessage('error', '请先登录')
    loading.value = false
    return
  }

  try {
    const userInfo = JSON.parse(stored)
    currentStudentId.value = userInfo.id

    // 加载指导记录
    await loadGuidanceRecords()
  } catch (error: any) {
    showMessage('error', error.message || '数据加载失败')
  } finally {
    loading.value = false
  }
}

// 加载指导记录
const loadGuidanceRecords = async () => {
  if (!currentStudentId.value) return

  try {
    const result = await getGuidanceRecords({
      studentId: currentStudentId.value,
      size: 100
    })
    
    guidanceRecords.value = result.records
    
    // 如果有指导记录，获取第一个记录的教师信息
    if (result.records.length > 0) {
      const firstRecord = result.records[0]
      // 通过教师ID获取教师详细信息
      try {
        const teacherProfile = await getTeacherProfile(firstRecord.teacherId)
        currentTeacherProfile.value = teacherProfile
      } catch (error) {
        console.error('获取教师信息失败:', error)
      }
    } else {
      // 没有指导记录，说明学生还没有选择指导老师
      showSelectTeacherPrompt.value = true
    }
  } catch (error: any) {
    console.error('加载指导记录失败:', error)
    // 如果加载失败，也显示选择指导老师提示
    showSelectTeacherPrompt.value = true
  }
}

// 格式化时间
const formatDateTime = (dateStr: string | undefined) => {
  if (!dateStr) return '—'
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN', { 
    year: 'numeric',
    month: 'short', 
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 是否有指导老师
const hasTeacher = computed(() => {
  return guidanceRecords.value.length > 0 || currentTeacherProfile.value !== null
})

// 加载可选指导教师列表
const loadTeacherOptions = async () => {
  teacherLoading.value = true
  try {
    teacherOptions.value = await getAllHomeroomTeachers()
  } catch (error: any) {
    console.error('加载教师列表失败:', error)
    teacherOptions.value = []
    showMessage('error', error?.message || '加载教师列表失败，请稍后重试')
  } finally {
    teacherLoading.value = false
  }
}

// 去选择指导老师（打开选择对话框）
const goToSelectTeacher = () => {
  if (!currentStudentId.value) {
    showMessage('error', '请先登录学生账号')
    return
  }
  selectedTeacherId.value = null
  showSelectTeacherDialog.value = true
  if (teacherOptions.value.length === 0) {
    loadTeacherOptions()
  }
}

// 确认选择指导老师：为该老师创建第一条指导记录，建立结对关系
const confirmSelectTeacher = async () => {
  if (!currentStudentId.value) {
    showMessage('error', '学生信息缺失，请重新登录')
    return
  }
  if (!selectedTeacherId.value) {
    showMessage('error', '请选择一位指导老师')
    return
  }

  selecting.value = true
  try {
    await createGuidanceRecord({
      teacherId: selectedTeacherId.value,
      studentId: currentStudentId.value,
      note: '学生发起就业指导结对申请',
    })
    showMessage('success', '已成功选择指导老师')
    showSelectTeacherDialog.value = false
    showSelectTeacherPrompt.value = false
    await loadGuidanceRecords()
  } catch (error: any) {
    console.error('选择指导老师失败:', error)
    showMessage('error', error?.message || '选择指导老师失败，请稍后重试')
  } finally {
    selecting.value = false
  }
}

onMounted(() => {
  loadStudentInfo()
})
</script>

<template>
  <div class="student-guidance-view">
    <div class="guidance-container">
      <div class="guidance-header">
        <div class="header-content">
          <div class="header-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
            </svg>
          </div>
          <div class="header-text">
            <h1>就业指导</h1>
            <p>选择指导老师并查看就业指导记录</p>
          </div>
        </div>
        <button v-if="isLoggedInStudent" type="button" class="btn-outline" @click="goToSelectTeacher">
          {{ hasTeacher ? '更换指导老师' : '选择指导老师' }}
        </button>
      </div>

      <transition name="fade">
        <div v-if="message" class="message-banner" :class="message.type">
          <svg v-if="message.type === 'success'" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
            <polyline points="22 4 12 14.01 9 11.01"></polyline>
          </svg>
          <svg v-else-if="message.type === 'error'" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="16" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12.01" y2="8"></line>
          </svg>
          <span>{{ message.text }}</span>
        </div>
      </transition>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 未选择指导老师提示 -->
      <div v-else-if="showSelectTeacherPrompt && !hasTeacher" class="select-teacher-prompt">
        <div class="prompt-card">
          <div class="prompt-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
          </div>
          <h3>尚未选择指导老师</h3>
          <p>您还没有选择指导老师，请先选择一位指导老师以获取专业的就业指导服务。</p>
          <div class="prompt-actions">
            <button class="btn-primary" @click="goToSelectTeacher">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <line x1="19" y1="8" x2="19" y2="14"></line>
                <line x1="22" y1="11" x2="16" y2="11"></line>
              </svg>
              <span>选择指导老师</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 有指导老师时显示内容 -->
      <div v-else-if="hasTeacher" class="guidance-content">
        <!-- 当前指导老师信息 -->
        <div v-if="currentTeacherProfile" class="teacher-info-card">
          <div class="teacher-avatar">
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
          </div>
          <div class="teacher-details">
            <h3>{{ currentTeacherProfile.name || '未知' }}</h3>
            <div class="teacher-meta">
              <span v-if="currentTeacherProfile.department" class="meta-item">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                  <circle cx="12" cy="10" r="3"></circle>
                </svg>
                {{ currentTeacherProfile.department }}
              </span>
              <span v-if="currentTeacherProfile.major" class="meta-item">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                  <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                </svg>
                {{ currentTeacherProfile.major }}
              </span>
              <span v-if="currentTeacherProfile.email" class="meta-item">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                  <polyline points="22,6 12,13 2,6"></polyline>
                </svg>
                {{ currentTeacherProfile.email }}
              </span>
              <span v-if="currentTeacherProfile.phone" class="meta-item">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
                </svg>
                {{ currentTeacherProfile.phone }}
              </span>
            </div>
            <p v-if="currentTeacherProfile.biography" class="teacher-biography">{{ currentTeacherProfile.biography }}</p>
          </div>
        </div>

        <!-- 指导记录列表 -->
        <div class="records-section">
          <div class="section-header">
            <h2>指导记录</h2>
            <span class="record-count">{{ guidanceRecords.length }} 条记录</span>
          </div>

          <div v-if="guidanceRecords.length === 0" class="empty-records">
            <div class="empty-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
              </svg>
            </div>
            <p>暂无指导记录</p>
            <small>您的指导老师还未添加指导记录</small>
          </div>

          <div v-else class="records-list">
            <div v-for="record in guidanceRecords" :key="record.id" class="record-card">
              <div class="record-header">
                <div class="record-time">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polyline points="12 6 12 12 16 14"></polyline>
                  </svg>
                  {{ formatDateTime(record.createdAt) }}
                </div>
              </div>
              <div class="record-content">
                <p>{{ record.note }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 选择指导老师对话框 -->
      <transition name="fade">
        <div v-if="showSelectTeacherDialog" class="select-teacher-dialog">
          <div class="select-teacher-panel">
            <h2>选择指导老师</h2>
            <p class="dialog-subtitle">请选择一位教师作为您的就业指导老师，后续指导记录将由该老师填写。</p>
            <div class="dialog-body">
              <div v-if="teacherLoading" class="loading-state">
                <div class="spinner"></div>
                <p>正在加载教师列表...</p>
              </div>
              <div v-else>
                <label class="dialog-label">指导老师</label>
                <select v-model.number="selectedTeacherId" class="dialog-select">
                  <option :value="null">请选择指导老师</option>
                  <option v-for="t in teacherOptions" :key="t.id" :value="t.id">
                    {{ t.name || '未命名教师' }}（{{ t.department || '未知院系' }}）
                  </option>
                </select>
                <p class="dialog-hint">如您不确定选择哪位老师，可优先选择所属院系的老师，或先与老师沟通后再确认。</p>
              </div>
            </div>
            <div class="dialog-actions">
              <button class="btn-secondary" type="button" @click="showSelectTeacherDialog = false" :disabled="selecting">取消</button>
              <button class="btn-primary" type="button" @click="confirmSelectTeacher" :disabled="selecting">
                {{ selecting ? '提交中...' : '确认选择' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<style scoped>
.student-guidance-view {
  min-height: calc(100vh - 200px);
  padding: 3rem 4vw;
  background: 
    radial-gradient(circle at 0% 0%, rgba(0, 113, 227, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 100% 100%, rgba(94, 92, 230, 0.03) 0%, transparent 50%),
    linear-gradient(180deg, #fafafa 0%, #ffffff 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
}

.guidance-container {
  max-width: 1000px;
  margin: 0 auto;
}

.guidance-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2.5rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.header-icon {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(0, 113, 227, 0.1), rgba(94, 92, 230, 0.1));
  color: #0071e3;
  flex-shrink: 0;
}

.header-text h1 {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #1d1d1f;
}

.header-text p {
  margin: 0;
  color: #86868b;
  font-size: 1rem;
  font-weight: 400;
}

.message-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1.25rem 1.5rem;
  border-radius: 16px;
  margin-bottom: 2rem;
  font-weight: 500;
  font-size: 0.9375rem;
}

.message-banner.success {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.1), rgba(48, 209, 88, 0.1));
  color: #34c759;
  border: 1px solid rgba(52, 199, 89, 0.2);
}

.message-banner.error {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.1), rgba(255, 45, 85, 0.1));
  color: #ff3b30;
  border: 1px solid rgba(255, 59, 48, 0.2);
}

.message-banner.info {
  background: linear-gradient(135deg, rgba(0, 113, 227, 0.1), rgba(94, 92, 230, 0.1));
  color: #0071e3;
  border: 1px solid rgba(0, 113, 227, 0.2);
}

.loading-state,
.select-teacher-prompt {
  text-align: center;
  padding: 4rem 2rem;
}

.spinner {
  width: 3rem;
  height: 3rem;
  border: 4px solid rgba(0, 113, 227, 0.1);
  border-top-color: #0071e3;
  border-radius: 50%;
  margin: 0 auto 1.5rem;
  animation: spin 0.8s linear infinite;
}

.select-teacher-prompt {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.prompt-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: saturate(180%) blur(30px);
  border-radius: 24px;
  padding: 3rem;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  max-width: 500px;
  width: 100%;
}

.prompt-icon {
  color: #c7c7cc;
  margin-bottom: 1.5rem;
}

.prompt-card h3 {
  margin: 0 0 0.75rem 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: #1d1d1f;
  letter-spacing: -0.01em;
}

.prompt-card p {
  margin: 0 0 2rem 0;
  color: #86868b;
  font-size: 0.9375rem;
  line-height: 1.6;
}

.prompt-actions {
  display: flex;
  justify-content: center;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 1.75rem;
  background: linear-gradient(135deg, #0071e3 0%, #5e5ce6 100%);
  color: white;
  border: none;
  border-radius: 14px;
  font-weight: 600;
  font-size: 0.9375rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  box-shadow: 0 4px 16px rgba(0, 113, 227, 0.25);
  letter-spacing: -0.01em;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 113, 227, 0.35);
  background: linear-gradient(135deg, #0077ed 0%, #6366f1 100%);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 选择指导老师对话框 */
.select-teacher-dialog {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(16px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 40;
}

.select-teacher-panel {
  width: min(520px, 92vw);
  background: #ffffff;
  border-radius: 24px;
  padding: 2.25rem 2.25rem 1.75rem;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.18);
}

.select-teacher-panel h2 {
  margin: 0 0 0.75rem 0;
  font-size: 1.5rem;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: #111827;
}

.dialog-subtitle {
  margin: 0 0 1.75rem 0;
  font-size: 0.9rem;
  color: #6b7280;
}

.dialog-body {
  margin-bottom: 1.75rem;
}

.dialog-label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: #374151;
}

.dialog-select {
  width: 100%;
  padding: 0.75rem 0.9rem;
  border-radius: 12px;
  border: 1px solid #d1d5db;
  background: #f9fafb;
  font-size: 0.95rem;
}

.dialog-select:focus {
  outline: none;
  border-color: #2563eb;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.dialog-hint {
  margin-top: 0.5rem;
  font-size: 0.8rem;
  color: #9ca3af;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-secondary {
  padding: 0.7rem 1.4rem;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  color: #374151;
  font-size: 0.9rem;
  cursor: pointer;
}

.btn-secondary:hover:not(:disabled) {
  background: #f3f4f6;
}

.guidance-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.teacher-info-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: saturate(180%) blur(30px);
  border-radius: 20px;
  padding: 2rem;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.teacher-avatar {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(0, 113, 227, 0.1), rgba(94, 92, 230, 0.1));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0071e3;
  flex-shrink: 0;
}

.teacher-details {
  flex: 1;
}

.teacher-details h3 {
  margin: 0 0 0.75rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1d1d1f;
  letter-spacing: -0.01em;
}

.teacher-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  color: #86868b;
  font-size: 0.875rem;
  font-weight: 400;
}

.teacher-biography {
  margin: 0.75rem 0 0 0;
  color: #86868b;
  font-size: 0.875rem;
  line-height: 1.6;
}

.records-section {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: saturate(180%) blur(30px);
  border-radius: 20px;
  padding: 2rem;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.section-header h2 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1d1d1f;
  letter-spacing: -0.01em;
}

.record-count {
  color: #86868b;
  font-size: 0.875rem;
  font-weight: 500;
}

.empty-records {
  text-align: center;
  padding: 3rem 2rem;
  color: #86868b;
}

.empty-records .empty-icon {
  color: #c7c7cc;
  margin-bottom: 1rem;
}

.empty-records p {
  margin: 0 0 0.5rem 0;
  font-size: 0.9375rem;
  font-weight: 500;
}

.empty-records small {
  font-size: 0.8125rem;
  color: #c7c7cc;
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.record-card {
  padding: 1.5rem;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.record-card:hover {
  background: rgba(0, 0, 0, 0.04);
  transform: translateY(-1px);
}

.record-header {
  margin-bottom: 1rem;
}

.record-time {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #86868b;
  font-size: 0.8125rem;
  font-weight: 500;
}

.record-content {
  color: #1d1d1f;
  line-height: 1.7;
  white-space: pre-wrap;
  font-size: 0.9375rem;
}

.record-content p {
  margin: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .guidance-header {
    flex-direction: column;
    gap: 1.5rem;
    align-items: flex-start;
  }
  
  .header-text h1 {
    font-size: 1.75rem;
  }
}

@media (max-width: 768px) {
  .student-guidance-view {
    padding: 1.5rem 1.25rem;
  }
  
  .teacher-info-card {
    flex-direction: column;
    text-align: center;
  }
  
  .prompt-card {
    padding: 2rem 1.5rem;
  }
}
</style>
