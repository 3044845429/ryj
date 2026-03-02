<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  approveProfileUpdate,
  getTeacherByUserId,
  getTeacherProfileApprovalDetail,
  rejectProfileUpdate,
  type TeacherProfileApprovalDetail,
} from '@/api/teacher'

const route = useRoute()
const router = useRouter()

const teacherId = ref<number | null>(null)
const detail = ref<TeacherProfileApprovalDetail | null>(null)
const loading = ref(true)
const pageError = ref('')
const actionError = ref('')
const comment = ref('')
const submitting = ref(false)
const successMessage = ref('')

const requestId = computed(() => {
  const raw = Number(route.params.requestId)
  return Number.isFinite(raw) ? raw : null
})

const isPending = computed(() => detail.value?.status === 'PENDING')

const fieldKeys = ['gender', 'age', 'major', 'graduationYear', 'biography'] as const
const fieldLabels: Record<(typeof fieldKeys)[number], string> = {
  gender: '性别',
  age: '年龄',
  major: '专业',
  graduationYear: '毕业年份',
  biography: '个人简介',
}

const differences = computed(() => {
  const current = detail.value?.currentProfile || {}
  const requested = detail.value?.requestedProfile || {}
  return {
    gender: requested.gender !== current.gender,
    age: requested.age !== current.age,
    major: requested.major !== current.major,
    biography: requested.biography !== current.biography,
    graduationYear: requested.graduationYear !== current.graduationYear,
  }
})

const statusMeta = computed(() => {
  switch (detail.value?.status) {
    case 'APPROVED':
      return { text: '教师已审核通过', klass: 'status-badge success' }
    case 'REJECTED':
      return { text: '教师已驳回', klass: 'status-badge danger' }
    case 'PENDING':
    default:
      return { text: '待教师审核', klass: 'status-badge warning' }
  }
})

const formatDateTime = (value: string | null | undefined) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(
    date.getHours(),
  ).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const fieldText = (value: string | number | null | undefined) => {
  if (value === null || value === undefined || value === '') {
    return '—'
  }
  return value
}

const goBack = () => {
  router.push({ name: 'teacher-approvals' })
}

const ensureTeacher = async () => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (!userInfoStr) {
    throw new Error('请先登录后再访问该页面')
  }
  const userInfo = JSON.parse(userInfoStr)
  if (userInfo.role !== 'TEACHER') {
    throw new Error('当前账号不是教师角色，无法访问档案审核页面')
  }
  const record = await getTeacherByUserId(userInfo.id)
  teacherId.value = record.id
}

const fetchDetail = async () => {
  if (!teacherId.value) {
    throw new Error('未能识别教师身份')
  }
  if (!requestId.value) {
    throw new Error('无效的档案申请编号')
  }
  detail.value = await getTeacherProfileApprovalDetail(teacherId.value, requestId.value)
}

onMounted(async () => {
  loading.value = true
  pageError.value = ''
  try {
    await ensureTeacher()
    await fetchDetail()
  } catch (e) {
    pageError.value = e instanceof Error ? e.message : '加载档案审核信息失败'
  } finally {
    loading.value = false
  }
})

const handleApprove = async () => {
  if (!teacherId.value || !requestId.value || !isPending.value) return
  submitting.value = true
  actionError.value = ''
  successMessage.value = ''
  try {
    const payload = comment.value.trim() ? { reviewComment: comment.value.trim() } : {}
    await approveProfileUpdate(teacherId.value, requestId.value, payload)
    successMessage.value = '已通过该档案申请，学生档案信息已同步更新。'
    await fetchDetail()
    comment.value = ''
  } catch (e) {
    actionError.value = e instanceof Error ? e.message : '审核通过操作失败'
  } finally {
    submitting.value = false
  }
}

const handleReject = async () => {
  if (!teacherId.value || !requestId.value || !isPending.value) return
  if (!comment.value.trim()) {
    actionError.value = '请填写退回原因后再提交'
    return
  }
  submitting.value = true
  successMessage.value = ''
  actionError.value = ''
  try {
    await rejectProfileUpdate(teacherId.value, requestId.value, { reviewComment: comment.value.trim() })
    successMessage.value = '已退回该档案申请，学生可根据反馈重新提交。'
    await fetchDetail()
    comment.value = ''
  } catch (e) {
    actionError.value = e instanceof Error ? e.message : '退回操作失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="teacher-page">
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

    <main class="teacher-body">
      <div class="body-inner">
        <header class="teacher-top-bar">
          <div>
            <button type="button" class="teacher-btn teacher-btn-secondary back-btn" @click="goBack">返回审核列表</button>
            <h1>档案审核详情</h1>
            <p class="subtitle">查看学生提交的档案改动，与当前档案对照后给出审核结论。</p>
          </div>
          <div v-if="detail" :class="['detail-status', statusMeta.klass]">{{ statusMeta.text }}</div>
        </header>

        <div v-if="loading" class="teacher-loading">正在加载档案审核详情…</div>
        <div v-else-if="pageError" class="teacher-card">
          <p class="teacher-error">{{ pageError }}</p>
          <button type="button" class="teacher-btn teacher-btn-secondary" @click="goBack">返回列表</button>
        </div>
        <template v-else-if="detail">
          <div class="teacher-card detail-summary">
            <h2 class="card-title">{{ detail.studentName }}</h2>
            <p class="detail-meta">
              申请编号：{{ detail.requestId }} · 提交时间：{{ formatDateTime(detail.submittedAt) }}
              <template v-if="detail.reviewedAt"> · 审核时间：{{ formatDateTime(detail.reviewedAt) }}</template>
            </p>
            <div v-if="detail.reviewComment" class="detail-note">
              <strong>历史审核意见：</strong>
              <p>{{ detail.reviewComment }}</p>
            </div>
          </div>

          <div class="detail-comparison">
            <div class="teacher-card detail-panel">
              <h3 class="card-title">当前档案</h3>
              <p class="card-desc">系统中学生已生效的档案信息</p>
              <ul class="detail-fields">
                <li v-for="key in fieldKeys" :key="key" :class="{ changed: differences[key] }">
                  <span class="teacher-label">{{ fieldLabels[key] }}</span>
                  <span class="detail-value">{{ fieldText(detail.currentProfile?.[key]) }}</span>
                </li>
              </ul>
            </div>
            <div class="teacher-card detail-panel highlight">
              <h3 class="card-title">本次申请的改动</h3>
              <p class="card-desc">学生提交的档案更新内容</p>
              <ul class="detail-fields">
                <li v-for="key in fieldKeys" :key="key" :class="{ changed: differences[key] }">
                  <span class="teacher-label">{{ fieldLabels[key] }}</span>
                  <span class="detail-value">{{ fieldText(detail.requestedProfile?.[key]) }}</span>
                </li>
              </ul>
            </div>
          </div>

          <div class="teacher-card review-box">
            <label class="teacher-label" for="review-comment">审核意见</label>
            <textarea
              id="review-comment"
              v-model="comment"
              class="teacher-textarea"
              :placeholder="isPending ? '可填写给学生的建议或退回原因' : '本次审核已完成'"
              :disabled="submitting"
              rows="4"
            />
            <div v-if="successMessage" class="detail-success">{{ successMessage }}</div>
            <div v-if="actionError" class="detail-error">{{ actionError }}</div>
            <div class="review-actions">
              <button type="button" class="teacher-btn teacher-btn-approve" :disabled="submitting || !isPending" @click="handleApprove">
                {{ submitting ? '提交中…' : '通过申请' }}
              </button>
              <button type="button" class="teacher-btn teacher-btn-reject" :disabled="submitting || !isPending" @click="handleReject">
                退回修改
              </button>
            </div>
          </div>
        </template>
      </div>
    </main>
  </div>
</template>

<style src="@/assets/teacher-layout.css"></style>
<style scoped>
.back-btn { margin-bottom: 12px; }
.detail-status { padding: 8px 16px; border-radius: 8px; font-size: 0.875rem; font-weight: 500; }
.detail-status.status-badge.warning { background: rgba(255, 149, 0, 0.15); color: #c93400; }
.detail-status.status-badge.success { background: rgba(52, 199, 89, 0.15); color: #248a3d; }
.detail-status.status-badge.danger { background: rgba(255, 59, 48, 0.15); color: #d70015; }
.detail-summary .card-title { margin-bottom: 4px; }
.detail-meta { font-size: 0.875rem; color: #86868b; margin: 0 0 12px; }
.detail-note { background: rgba(0, 113, 227, 0.08); border-radius: 8px; padding: 12px; color: #1d1d1f; }
.detail-note p { margin: 4px 0 0; }
.detail-comparison { display: grid; gap: 24px; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); margin-bottom: 24px; }
.detail-panel.highlight { border-color: rgba(0, 113, 227, 0.25); }
.detail-fields { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 12px; }
.detail-fields li { padding: 12px; background: #f5f5f7; border-radius: 8px; border: 1px solid #e8e8ed; }
.detail-fields li.changed { border-color: rgba(0, 113, 227, 0.3); background: rgba(0, 113, 227, 0.06); }
.detail-value { font-size: 0.9375rem; color: #1d1d1f; }
.review-box .teacher-label { margin-bottom: 8px; }
.review-actions { display: flex; gap: 12px; margin-top: 16px; }
.teacher-btn-approve { background: #34c759; color: #fff; }
.teacher-btn-reject { background: #ff3b30; color: #fff; }
.detail-success { margin-top: 12px; padding: 10px 12px; background: rgba(52, 199, 89, 0.12); color: #248a3d; border-radius: 8px; font-size: 0.9375rem; }
.detail-error { margin-top: 12px; color: #d70015; font-size: 0.9375rem; }
</style>

