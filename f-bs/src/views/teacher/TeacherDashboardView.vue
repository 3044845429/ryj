<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  approveProfileUpdate,
  getTeacherByUserId,
  getTeacherDashboard,
  rejectProfileUpdate,
  type TeacherDashboardResponse,
  type TeacherRecord,
} from '@/api/teacher'

type ReviewAction = 'APPROVE' | 'REJECT'

const route = useRoute()
const router = useRouter()

const teacherRecord = ref<TeacherRecord | null>(null)
const dashboard = ref<TeacherDashboardResponse | null>(null)
const loading = ref(true)
const refreshing = ref(false)
const error = ref<string | null>(null)

const activeSection = ref<string>((route.query.section as string) || 'overview')
const submitting = ref(false)

const reviewDialog = ref({
  visible: false,
  requestId: null as number | null,
  action: 'APPROVE' as ReviewAction,
  comment: '',
})

const message = ref<{ type: 'success' | 'error'; text: string } | null>(null)
let messageTimer: number | undefined

const profile = computed(() => dashboard.value?.profile ?? null)
const overview = computed(() => dashboard.value?.overview ?? null)
const pendingApprovals = computed(() => dashboard.value?.pendingApprovals ?? [])
const guidedStudents = computed(() => dashboard.value?.guidedStudents ?? [])
const employerCollaborations = computed(() => dashboard.value?.employerCollaborations ?? [])
const guidanceNotes = computed(() => dashboard.value?.recentGuidanceNotes ?? [])
const studentActivities = computed(() => dashboard.value?.recentStudentActivities ?? [])

const openApprovalDetail = (requestId: number) => {
  router.push({ name: 'teacher-approval-detail', params: { requestId: String(requestId) } })
}

const approvalSubmitDisabled = computed(() => {
  if (submitting.value) return true
  if (reviewDialog.value.action === 'REJECT') {
    return !reviewDialog.value.comment.trim()
  }
  return false
})

const overviewStats = computed(() => {
  const data = overview.value
  if (!data) return []
  return [
    {
      key: 'students',
      label: '结对学生',
      value: data.totalGuidedStudents,
      hint: '持续跟进',
      accent: 'accent-blue',
    },
    {
      key: 'approvals',
      label: '待审核',
      value: data.pendingApprovalCount,
      hint: '档案变更',
      accent: 'accent-purple',
    },
    {
      key: 'interviews',
      label: '面试进程',
      value: data.activeInterviewCount,
      hint: '关注学生面试',
      accent: 'accent-emerald',
    },
    {
      key: 'collaboration',
      label: '企业协同',
      value: data.collaborationCount,
      hint: '活跃合作方',
      accent: 'accent-amber',
    },
  ]
})

const interviewStatusMap: Record<string, string> = {
  SCHEDULED: '面试待进行',
  COMPLETED: '面试已完成',
  CANCELLED: '面试已取消',
}

const applicationStatusMap: Record<string, string> = {
  SUBMITTED: '已投递',
  REVIEWING: '企业筛选中',
  INTERVIEW: '面试阶段',
  OFFERED: '获得录用',
  REJECTED: '未通过',
}

const formatDateTime = (value: string | null) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(
    date.getHours(),
  ).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const shorten = (value: string | null, limit = 120) => {
  if (!value) return '学生暂未填写详细说明'
  return value.length > limit ? `${value.slice(0, limit)}…` : value
}

const scrollToSection = async (section: string) => {
  activeSection.value = section
  await nextTick()
  const element = document.getElementById(`section-${section}`)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const showMessage = (type: 'success' | 'error', text: string) => {
  message.value = { type, text }
  if (messageTimer) {
    clearTimeout(messageTimer)
  }
  messageTimer = window.setTimeout(() => {
    message.value = null
    messageTimer = undefined
  }, 3600)
}

const openReviewDialog = (action: ReviewAction, requestId: number) => {
  // 通过：直接提交，无需弹窗
  if (action === 'APPROVE') {
    approveNow(requestId)
    return
  }
  // 退回：打开弹窗填写原因
  reviewDialog.value = {
    visible: true,
    requestId,
    action,
    comment: '',
  }
}

const closeReviewDialog = () => {
  reviewDialog.value.visible = false
  reviewDialog.value.comment = ''
  reviewDialog.value.requestId = null
}

// 直接通过（无弹窗）
const approveNow = async (requestId: number) => {
  if (!teacherRecord.value || !requestId) return
  if (submitting.value) return
  submitting.value = true
  try {
    await approveProfileUpdate(teacherRecord.value.id, requestId, {})
    // 即时更新本地数据，避免等待重新加载
    if (dashboard.value) {
      dashboard.value.pendingApprovals = dashboard.value.pendingApprovals.filter(
        (req) => req.requestId !== requestId,
      )
      if (dashboard.value.overview && dashboard.value.overview.pendingApprovalCount > 0) {
        dashboard.value.overview.pendingApprovalCount -= 1
      }
    }
    showMessage('success', '已通过该学生的档案调整申请')
    await loadDashboard(false)
  } catch (err: any) {
    showMessage('error', err?.message || '操作失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}

const submitReview = async () => {
  if (!teacherRecord.value || !reviewDialog.value.requestId) {
    return
  }

  submitting.value = true
  try {
    if (reviewDialog.value.action === 'APPROVE') {
      const payload = reviewDialog.value.comment.trim()
        ? { reviewComment: reviewDialog.value.comment.trim() }
        : {}
      await approveProfileUpdate(teacherRecord.value.id, reviewDialog.value.requestId, payload)
      if (dashboard.value) {
        dashboard.value.pendingApprovals = dashboard.value.pendingApprovals.filter(
          (req) => req.requestId !== reviewDialog.value.requestId,
        )
        if (dashboard.value.overview && dashboard.value.overview.pendingApprovalCount > 0) {
          dashboard.value.overview.pendingApprovalCount -= 1
        }
      }
      showMessage('success', '已通过该学生的档案调整申请')
    } else {
      await rejectProfileUpdate(teacherRecord.value.id, reviewDialog.value.requestId, {
        reviewComment: reviewDialog.value.comment.trim(),
      })
      if (dashboard.value) {
        dashboard.value.pendingApprovals = dashboard.value.pendingApprovals.filter(
          (req) => req.requestId !== reviewDialog.value.requestId,
        )
        if (dashboard.value.overview && dashboard.value.overview.pendingApprovalCount > 0) {
          dashboard.value.overview.pendingApprovalCount -= 1
        }
      }
      showMessage('success', '已退回该学生的档案申请')
    }
    closeReviewDialog()
    await loadDashboard(false)
  } catch (err: any) {
    showMessage('error', err?.message || '操作失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}

const loadDashboard = async (showGlobalLoading = true) => {
  if (!teacherRecord.value) return
  if (showGlobalLoading) {
    loading.value = true
  } else {
    refreshing.value = true
  }
  error.value = null
  try {
    dashboard.value = await getTeacherDashboard(teacherRecord.value.id)
  } catch (err: any) {
    error.value = err?.message || '教学驾驶舱数据加载失败'
  } finally {
    if (showGlobalLoading) {
      loading.value = false
    }
    refreshing.value = false
  }
}

const refreshDashboard = async () => {
  if (!teacherRecord.value || refreshing.value) return
  await loadDashboard(false)
}

const initialise = async () => {
  const stored = localStorage.getItem('userInfo')
  if (!stored) {
    error.value = '请先登录后访问教师专区'
    loading.value = false
    return
  }

  try {
    const parsed = JSON.parse(stored)
    if (!parsed?.id) {
      throw new Error('用户信息缺失')
    }
    if (parsed.role !== 'TEACHER') {
      error.value = '当前账号不具备教师角色权限'
      loading.value = false
      return
    }
    teacherRecord.value = await getTeacherByUserId(parsed.id)
    await loadDashboard(true)
    const section = route.query.section
    if (typeof section === 'string') {
      await scrollToSection(section)
    }
  } catch (err: any) {
    error.value = err?.message || '教师信息加载失败'
    loading.value = false
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query.section,
  async (section) => {
    if (typeof section === 'string') {
      await scrollToSection(section)
    }
  },
)

onMounted(() => {
  initialise()
})
</script>

<template>
  <div class="teacher-page">
    <div class="dashboard-aurora" aria-hidden="true" />

    <nav class="teacher-nav">
      <div class="nav-container">
        <div class="nav-logo">
          <span class="logo-icon">🎓</span>
          <span class="logo-text">教师工作台</span>
        </div>
        <div class="nav-links">
          <router-link to="/teacher/overview" class="nav-link" active-class="active">
            <span class="link-icon">📊</span><span>仪表板</span>
          </router-link>
          <router-link to="/teacher/guidance" class="nav-link" active-class="active">
            <span class="link-icon">📝</span><span>指导记录</span>
          </router-link>
          <router-link to="/teacher/statistics" class="nav-link" active-class="active">
            <span class="link-icon">📈</span><span>统计分析</span>
          </router-link>
          <router-link to="/teacher/approvals" class="nav-link" active-class="active">
            <span class="link-icon">✅</span><span>档案审核</span>
          </router-link>
          <router-link to="/teacher/profile" class="nav-link" active-class="active">
            <span class="link-icon">👤</span><span>教师信息</span>
          </router-link>
        </div>
      </div>
    </nav>

    <main class="teacher-body dashboard-body">
      <div class="body-inner">
      <div class="dashboard-header">
        <div class="section-tabs">
          <button
            v-for="tab in [
              { key: 'overview', label: '总览' },
              { key: 'approvals', label: '档案审核' },
              { key: 'students', label: '学生跟进' },
              { key: 'collaboration', label: '校企协同' },
              { key: 'notes', label: '指导记事' },
              { key: 'activities', label: '就业动向' },
            ]"
            :key="tab.key"
            :class="['section-tab', { active: activeSection === tab.key }]"
            type="button"
            @click="scrollToSection(tab.key)"
          >
            {{ tab.label }}
          </button>
        </div>
        <div class="header-actions">
          <button class="refresh-btn" type="button" :disabled="refreshing" @click="refreshDashboard">
            <span v-if="refreshing" class="refresh-spinner" />
            {{ refreshing ? '刷新中...' : '刷新数据' }}
          </button>
          <transition name="fade">
            <div v-if="message" class="feedback-banner" :class="message.type">
              {{ message.text }}
            </div>
          </transition>
        </div>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="spinner" />
        <p>正在为您加载教师教学驾驶舱...</p>
      </div>

      <template v-else>
        <div v-if="error" class="error-state">
          <h2>无法访问教师模块</h2>
          <p>{{ error }}</p>
        </div>

        <div v-else-if="dashboard" class="dashboard-content">
          <section id="section-overview" class="hero-panel" :class="{ 'is-active': activeSection === 'overview' }">
            <div class="hero-profile">
              <div class="avatar" aria-hidden="true">{{ profile?.avatarInitials || 'T' }}</div>
              <div class="profile-text">
                <h1>
                  {{ profile?.name || '教师用户' }}
                  <small>{{ profile?.department || '所属学院待完善' }}</small>
                </h1>
                <p>{{ profile?.biography }}</p>
                <div class="profile-meta">
                  <span v-if="profile?.focus">{{ profile.focus }}</span>
                  <span v-if="profile?.email">📧 {{ profile.email }}</span>
                  <span v-if="profile?.phone">📞 {{ profile.phone }}</span>
                </div>
              </div>
            </div>
            <div class="stats-grid">
              <div v-for="stat in overviewStats" :key="stat.key" class="stat-card" :class="stat.accent">
                <span class="stat-label">{{ stat.label }}</span>
                <strong class="stat-value">{{ stat.value }}</strong>
                <span class="stat-hint">{{ stat.hint }}</span>
              </div>
            </div>
          </section>

          <div class="content-grid">
            <section
              id="section-approvals"
              class="card card--wide"
              :class="{ 'is-active': activeSection === 'approvals' }"
            >
              <header class="card-header">
                <div>
                  <h2>档案审核队列</h2>
                  <p>集中查看学生提交的档案变更申请，快速给予反馈。</p>
                </div>
              </header>
              <div v-if="pendingApprovals.length === 0" class="empty-state">
                <div class="empty-icon">✅</div>
                <p>暂无待审核的学生档案请求，继续关注学生就业动态吧。</p>
              </div>
              <div v-else class="approval-list">
                <article v-for="request in pendingApprovals" :key="request.requestId" class="approval-item">
                  <div class="approval-titles">
                    <div class="approval-name">{{ request.studentName }}</div>
                    <div class="approval-meta">
                      <span>{{ request.major || '专业未填写' }}</span>
                      <span v-if="request.graduationYear">预计 {{ request.graduationYear }} 毕业</span>
                      <span>提交于 {{ formatDateTime(request.submittedAt) }}</span>
                    </div>
                  </div>
                  <p class="approval-brief">{{ shorten(request.biography, 140) }}</p>
                  <div class="approval-actions">
                    <button type="button" class="action-btn outline" @click="openApprovalDetail(request.requestId)">
                      查看详情
                    </button>
                    <button type="button" class="action-btn approve" @click="approveNow(request.requestId)">
                      通过申请
                    </button>
                    <button type="button" class="action-btn reject" @click="openReviewDialog('REJECT', request.requestId)">
                      退回修改
                    </button>
                  </div>
                </article>
              </div>
            </section>

            <section
              id="section-students"
              class="card"
              :class="{ 'is-active': activeSection === 'students' }"
            >
              <header class="card-header">
                <div>
                  <h2>结对学生跟进</h2>
                  <p>实时掌握学生就业准备情况与校招进度。</p>
                </div>
              </header>
              <div v-if="guidedStudents.length === 0" class="empty-state">
                <div class="empty-icon">🎓</div>
                <p>暂未分配结对学生，可联系就业办完成认领。</p>
              </div>
              <ul v-else class="student-list">
                <li v-for="student in guidedStudents" :key="student.studentId" class="student-item">
                  <div class="student-header">
                    <h3>{{ student.studentName }}</h3>
                    <span class="student-major">{{ student.major || '专业待完善' }}</span>
                  </div>
                  <div class="student-highlights">
                    <div>
                      <span class="badge">待审核 {{ student.pendingRequestCount }}</span>
                    </div>
                    <div>
                      <span class="badge badge--accent">活跃申请 {{ student.activeApplicationCount }}</span>
                    </div>
                    <div>
                      <span class="badge badge--muted">
                        {{ student.latestInterviewStatus ? interviewStatusMap[student.latestInterviewStatus] || student.latestInterviewStatus : '暂无面试' }}
                      </span>
                    </div>
                  </div>
                  <div v-if="student.employerNames.length" class="student-employers">
                    <span v-for="company in student.employerNames" :key="company" class="employer-chip">{{ company }}</span>
                  </div>
                  <footer class="student-footer">
                    <span>最近指导：{{ formatDateTime(student.latestGuidanceAt) }}</span>
                    <span v-if="student.latestGuidanceNote" class="student-note">“{{ student.latestGuidanceNote }}”</span>
                  </footer>
                </li>
              </ul>
            </section>

            <section
              id="section-collaboration"
              class="card"
              :class="{ 'is-active': activeSection === 'collaboration' }"
            >
              <header class="card-header">
                <div>
                  <h2>校企协同概览</h2>
                  <p>了解结对学生对应的企业互动情况，精准匹配资源。</p>
                </div>
              </header>
              <div v-if="employerCollaborations.length === 0" class="empty-state">
                <div class="empty-icon">🤝</div>
                <p>目前暂无合作企业记录，可鼓励学生拓展更多机会。</p>
              </div>
              <ul v-else class="collaboration-list">
                <li v-for="employer in employerCollaborations" :key="employer.employerId" class="collaboration-item">
                  <div class="collaboration-title">
                    <h3>{{ employer.companyName }}</h3>
                    <span>最近互动：{{ formatDateTime(employer.latestInteraction) }}</span>
                  </div>
                  <div class="collaboration-stats">
                    <span>关联岗位 {{ employer.jobCount }}</span>
                    <span>参与学生 {{ employer.studentCount }}</span>
                  </div>
                  <div class="collaboration-students" v-if="employer.studentNames.length">
                    <span v-for="student in employer.studentNames" :key="student">{{ student }}</span>
                  </div>
                </li>
              </ul>
            </section>

            <section
              id="section-notes"
              class="card"
              :class="{ 'is-active': activeSection === 'notes' }"
            >
              <header class="card-header">
                <div>
                  <h2>最新指导纪要</h2>
                  <p>快速回顾近期对学生的辅导要点，保持沟通连贯。</p>
                </div>
              </header>
              <div v-if="guidanceNotes.length === 0" class="empty-state">
                <div class="empty-icon">🗒️</div>
                <p>还没有记录指导内容，建议及时补充要点备忘。</p>
              </div>
              <ul v-else class="guidance-list">
                <li v-for="note in guidanceNotes" :key="note.id" class="guidance-item">
                  <div class="guidance-meta">
                    <strong>{{ note.studentName }}</strong>
                    <span>{{ formatDateTime(note.createdAt) }}</span>
                  </div>
                  <p>{{ note.note || '暂无备注内容' }}</p>
                </li>
              </ul>
            </section>

            <section
              id="section-activities"
              class="card card--wide"
              :class="{ 'is-active': activeSection === 'activities' }"
            >
              <header class="card-header">
                <div>
                  <h2>就业动向速览</h2>
                  <p>跟踪学生最新投递与录用动态，提前规划辅导策略。</p>
                </div>
              </header>
              <div v-if="studentActivities.length === 0" class="empty-state">
                <div class="empty-icon">📈</div>
                <p>暂无新的就业活动，鼓励学生积极参与校招。</p>
              </div>
              <ul v-else class="activity-timeline">
                <li v-for="activity in studentActivities" :key="activity.applicationId" class="activity-item">
                  <div class="activity-time">{{ formatDateTime(activity.appliedAt) }}</div>
                  <div class="activity-card">
                    <div class="activity-title">
                      <strong>{{ activity.studentName }}</strong>
                      <span>{{ activity.jobTitle || '岗位名称待更新' }}</span>
                    </div>
                    <div class="activity-meta">
                      <span>{{ activity.employerName || '企业待确认' }}</span>
                      <span class="activity-status">{{ applicationStatusMap[activity.status || ''] || activity.status || '状态未知' }}</span>
                    </div>
                  </div>
                </li>
              </ul>
            </section>
          </div>
        </div>
      </template>
      </div>
    </main>

    <transition name="modal">
      <div v-if="reviewDialog.visible" class="review-modal" @click.self="closeReviewDialog">
        <div class="review-modal__panel">
          <header>
            <h3>退回档案申请</h3>
            <p>请给出退回原因，便于学生根据建议完善档案内容。</p>
          </header>
          <textarea
            v-model="reviewDialog.comment"
            placeholder="必填：说明退回原因与修改建议"
          ></textarea>
          <footer>
            <button type="button" class="btn btn--ghost" @click="closeReviewDialog">取消</button>
            <button type="button" class="btn btn--primary" :disabled="approvalSubmitDisabled" @click="submitReview">
              {{ submitting ? '处理中...' : '确认提交' }}
            </button>
          </footer>
        </div>
      </div>
    </transition>
  </div>
</template>
<style src="@/assets/teacher-layout.css"></style>
<style scoped>
.dashboard-aurora {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 360px;
  background: linear-gradient(180deg, rgba(0, 113, 227, 0.06) 0%, transparent 65%);
  pointer-events: none;
  z-index: 0;
}

.dashboard-body .body-inner {
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.section-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.section-tab {
  border: none;
  border-radius: 999px;
  padding: 0.5rem 1.25rem;
  font-size: 0.95rem;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.7);
  color: #1e3a8a;
  box-shadow: 0 8px 18px rgba(59, 130, 246, 0.15);
  cursor: pointer;
  transition: all 0.25s ease;
}

.section-tab:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.9);
}

.section-tab.active {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.25);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.refresh-btn {
  position: relative;
  border: none;
  border-radius: 999px;
  padding: 0.55rem 1.3rem;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #1d4ed8, #6366f1);
  box-shadow: 0 12px 24px rgba(59, 130, 246, 0.25);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.refresh-btn:disabled {
  opacity: 0.65;
  cursor: wait;
}

.refresh-btn:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 28px rgba(79, 70, 229, 0.3);
}

.refresh-spinner {
  display: inline-block;
  width: 1rem;
  height: 1rem;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 999px;
  margin-right: 0.5rem;
  animation: spin 0.8s linear infinite;
}

.feedback-banner {
  padding: 0.55rem 1.4rem;
  border-radius: 14px;
  font-size: 0.9rem;
  font-weight: 600;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.15);
}

.feedback-banner.success {
  background: rgba(34, 197, 94, 0.15);
  color: #047857;
}

.feedback-banner.error {
  background: rgba(248, 113, 113, 0.18);
  color: #b91c1c;
}

.loading-state,
.error-state {
  margin: 4rem auto 0;
  max-width: 520px;
  text-align: center;
  background: rgba(255, 255, 255, 0.85);
  padding: 3rem 2rem;
  border-radius: 24px;
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.15);
}

.loading-state p,
.error-state p {
  margin-top: 1.5rem;
  font-size: 1rem;
  color: #475569;
}

.error-state h2 {
  margin: 0;
  font-size: 1.6rem;
  color: #1e3a8a;
}

.spinner {
  width: 3rem;
  height: 3rem;
  border: 4px solid rgba(99, 102, 241, 0.25);
  border-top-color: #4f46e5;
  border-radius: 999px;
  margin: 0 auto;
  animation: spin 0.85s linear infinite;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 2.2rem;
}

.hero-panel {
  display: grid;
  gap: 2rem;
  padding: 2.5rem;
  border-radius: 32px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.95), rgba(59, 130, 246, 0.88));
  color: #fff;
  box-shadow: 0 35px 80px rgba(37, 99, 235, 0.35);
}

.hero-panel.is-active {
  box-shadow: 0 40px 90px rgba(37, 99, 235, 0.45);
  transform: translateY(-2px);
}

.hero-profile {
  display: flex;
  gap: 1.5rem;
  align-items: center;
}

.avatar {
  display: grid;
  place-items: center;
  width: 82px;
  height: 82px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 2rem;
  font-weight: 700;
}

.profile-text h1 {
  margin: 0 0 0.5rem;
  font-size: 2rem;
  display: flex;
  flex-direction: column;
}

.profile-text h1 small {
  font-size: 0.9rem;
  font-weight: 500;
  opacity: 0.85;
}

.profile-text p {
  margin: 0;
  font-size: 1.05rem;
  line-height: 1.6;
  opacity: 0.92;
}

.profile-meta {
  margin-top: 1rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.9rem;
  opacity: 0.9;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 1.25rem;
}

.stat-card {
  padding: 1.5rem;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.stat-label {
  font-size: 0.9rem;
  opacity: 0.85;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
}

.stat-hint {
  font-size: 0.85rem;
  opacity: 0.85;
}

.stat-card.accent-blue {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.25), rgba(37, 99, 235, 0.4));
}

.stat-card.accent-purple {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.3), rgba(99, 102, 241, 0.35));
}

.stat-card.accent-emerald {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.28), rgba(5, 150, 105, 0.32));
}

.stat-card.accent-amber {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.28), rgba(217, 119, 6, 0.34));
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.8rem;
}

.card {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 24px;
  padding: 1.75rem;
  box-shadow: 0 25px 50px rgba(15, 23, 42, 0.12);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.card.is-active {
  transform: translateY(-2px);
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.18);
}

.card--wide {
  grid-column: 1 / -1;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.card-header h2 {
  margin: 0 0 0.35rem;
  font-size: 1.4rem;
  color: #0f172a;
}

.card-header p {
  margin: 0;
  font-size: 0.95rem;
  color: #475569;
}

.empty-state {
  padding: 2.5rem 1.5rem;
  text-align: center;
  border: 2px dashed rgba(148, 163, 184, 0.35);
  border-radius: 20px;
  color: #475569;
  background: rgba(248, 250, 252, 0.7);
}

.empty-icon {
  font-size: 2rem;
  margin-bottom: 0.75rem;
}

.approval-list,
.student-list,
.collaboration-list,
.guidance-list,
.activity-timeline {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.approval-item {
  padding: 1.4rem;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.85);
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.approval-titles {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin-bottom: 0.75rem;
}

.approval-name {
  font-size: 1.2rem;
  font-weight: 600;
  color: #0f172a;
}

.approval-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.9rem;
  color: #475569;
}

.approval-brief {
  margin: 0 0 1rem;
  font-size: 0.95rem;
  color: #1f2937;
  line-height: 1.5;
}

.approval-actions {
  display: flex;
  gap: 0.75rem;
}

.action-btn {
  flex: 1;
  border: none;
  border-radius: 14px;
  padding: 0.65rem 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.action-btn.approve {
  background: linear-gradient(135deg, #34d399, #059669);
  color: #fff;
  box-shadow: 0 12px 24px rgba(16, 185, 129, 0.25);
}

.action-btn.outline {
  background: rgba(59, 130, 246, 0.1);
  color: #1d4ed8;
  border: 1px solid rgba(59, 130, 246, 0.25);
}

.action-btn.outline:hover {
  background: rgba(59, 130, 246, 0.18);
}

.action-btn.reject {
  background: linear-gradient(135deg, #f97316, #ef4444);
  color: #fff;
  box-shadow: 0 12px 24px rgba(239, 68, 68, 0.25);
}

.action-btn:hover {
  transform: translateY(-1px);
}

.student-item {
  padding: 1.25rem;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.8);
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.student-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.student-header h3 {
  margin: 0;
  font-size: 1.15rem;
  color: #1e293b;
}

.student-major {
  font-size: 0.9rem;
  color: #475569;
}

.student-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.12);
  color: #4338ca;
  font-size: 0.85rem;
  font-weight: 600;
}

.badge--accent {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.badge--muted {
  background: rgba(148, 163, 184, 0.18);
  color: #475569;
}

.student-employers {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.employer-chip {
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
  font-size: 0.8rem;
}

.student-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.85rem;
  color: #475569;
}

.student-note {
  font-style: italic;
  color: #1e3a8a;
}

.collaboration-item {
  padding: 1.3rem;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.85);
  border: 1px solid rgba(226, 232, 240, 0.85);
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}

.collaboration-title {
  display: flex;
  justify-content: space-between;
  font-size: 0.95rem;
  color: #1f2937;
}

.collaboration-stats {
  display: flex;
  gap: 1rem;
  font-size: 0.9rem;
  color: #475569;
}

.collaboration-students {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: #1d4ed8;
}

.guidance-item {
  padding: 1.1rem;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.guidance-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.6rem;
  color: #475569;
  font-size: 0.9rem;
}

.guidance-item p {
  margin: 0;
  font-size: 0.95rem;
  color: #1f2937;
  line-height: 1.5;
}

.activity-timeline {
  position: relative;
  padding-left: 1.5rem;
}

.activity-timeline::before {
  content: '';
  position: absolute;
  left: 0.45rem;
  top: 0.3rem;
  bottom: 0.3rem;
  width: 2px;
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.5), rgba(99, 102, 241, 0.1));
}

.activity-item {
  position: relative;
  padding-left: 1.5rem;
}

.activity-item::before {
  content: '';
  position: absolute;
  left: -0.05rem;
  top: 0.5rem;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
}

.activity-time {
  font-size: 0.85rem;
  color: #6b7280;
  margin-bottom: 0.4rem;
}

.activity-card {
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 16px;
  padding: 1rem 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.activity-title {
  display: flex;
  justify-content: space-between;
  gap: 1.5rem;
  font-size: 0.95rem;
  color: #111827;
}

.activity-meta {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.85rem;
  color: #475569;
}

.activity-status {
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-weight: 600;
}

.review-modal {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(15, 23, 42, 0.35);
  backdrop-filter: blur(6px);
  z-index: 40;
}

.review-modal__panel {
  width: min(480px, 92vw);
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  padding: 2rem;
  box-shadow: 0 40px 80px rgba(15, 23, 42, 0.2);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.review-modal__panel header h3 {
  margin: 0 0 0.5rem;
  font-size: 1.4rem;
  color: #0f172a;
}

.review-modal__panel header p {
  margin: 0;
  color: #475569;
  font-size: 0.95rem;
}

.review-modal__panel textarea {
  min-height: 140px;
  border-radius: 18px;
  border: 1px solid rgba(203, 213, 225, 0.9);
  padding: 1rem 1.2rem;
  font-size: 0.95rem;
  color: #0f172a;
  resize: vertical;
  background: rgba(248, 250, 252, 0.95);
}

.review-modal__panel footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn {
  border: none;
  border-radius: 14px;
  padding: 0.65rem 1.5rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn--ghost {
  background: rgba(226, 232, 240, 0.6);
  color: #1f2937;
}

.btn--primary {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
  box-shadow: 0 16px 32px rgba(79, 70, 229, 0.25);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.btn:not(:disabled):hover {
  transform: translateY(-1px);
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .hero-panel {
    padding: 2rem;
  }

  .profile-text h1 {
    font-size: 1.6rem;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .card {
    padding: 1.5rem;
  }

  .review-modal__panel {
    padding: 1.5rem;
  }
}
</style>
