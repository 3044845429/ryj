<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTeacherByUserId, getTeacherDashboard, approveProfileUpdate, rejectProfileUpdate, type TeacherPendingApproval } from '@/api/teacher'

type ReviewAction = 'APPROVE' | 'REJECT'

const router = useRouter()
const teacherId = ref<number | null>(null)
const loading = ref(true)
const error = ref('')
const approvals = ref<TeacherPendingApproval[]>([])
const keyword = ref('')
const majorFilter = ref('')

const dialog = ref({ visible: false, action: 'APPROVE' as ReviewAction, requestId: null as number | null, comment: '' })
const submitting = ref(false)

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const mj = majorFilter.value.trim().toLowerCase()
  return approvals.value.filter(a => {
    const hitKw = !kw || a.studentName.toLowerCase().includes(kw)
    const hitMj = !mj || (a.major || '').toLowerCase().includes(mj)
    return hitKw && hitMj
  })
})

const loadData = async () => {
  loading.value = true
  error.value = ''
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (!userInfoStr) throw new Error('请先登录')
    const userInfo = JSON.parse(userInfoStr)
    if (userInfo.role !== 'TEACHER') throw new Error('当前账号不是教师角色')
    const rec = await getTeacherByUserId(userInfo.id)
    teacherId.value = rec.id
    const dash = await getTeacherDashboard(rec.id)
    approvals.value = dash.pendingApprovals
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

const openDetail = (requestId: number) => {
  router.push({ name: 'teacher-approval-detail', params: { requestId: String(requestId) } })
}

const openDialog = (action: ReviewAction, requestId: number) => {
  // 通过：直接提交，不弹窗
  if (action === 'APPROVE') {
    approveNow(requestId)
    return
  }
  // 退回：弹窗填写原因
  dialog.value = { visible: true, action, requestId, comment: '' }
}

const closeDialog = () => {
  dialog.value.visible = false
  dialog.value.comment = ''
}

// 直接通过（无弹窗）
const approveNow = async (requestId: number) => {
  if (!teacherId.value || !requestId) return
  if (submitting.value) return
  submitting.value = true
  try {
    await approveProfileUpdate(teacherId.value, requestId, {})
    approvals.value = approvals.value.filter((item) => item.requestId !== requestId)
    alert('✅ 审核通过，学生档案已更新')
    await loadData()
  } catch (e) {
    alert(e instanceof Error ? e.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

const submit = async () => {
  if (!teacherId.value || !dialog.value.requestId) return
  
  // 退回时必须填写原因
  if (dialog.value.action === 'REJECT' && !dialog.value.comment.trim()) {
    alert('退回申请时必须填写原因')
    return
  }
  
  submitting.value = true
  try {
    if (dialog.value.action === 'APPROVE') {
      const payload = dialog.value.comment.trim() ? { reviewComment: dialog.value.comment.trim() } : {}
      await approveProfileUpdate(teacherId.value, dialog.value.requestId, payload)
      approvals.value = approvals.value.filter((item) => item.requestId !== dialog.value.requestId)
      alert('✅ 审核通过，学生档案已更新')
    } else {
      await rejectProfileUpdate(teacherId.value, dialog.value.requestId, { reviewComment: dialog.value.comment.trim() })
      approvals.value = approvals.value.filter((item) => item.requestId !== dialog.value.requestId)
      alert('📝 已退回申请，学生将收到您的意见并可重新提交')
    }
    closeDialog()
    await loadData()
  } catch (e) {
    alert(e instanceof Error ? e.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)

const formatDateTime = (value: string | null) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
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
            <h1>档案审核</h1>
            <p class="subtitle">处理学生提交的档案变更申请</p>
          </div>
          <div class="filters">
            <input v-model="keyword" class="teacher-input filter-input" placeholder="学生姓名" />
            <input v-model="majorFilter" class="teacher-input filter-input" placeholder="专业" />
          </div>
        </header>

        <div v-if="loading" class="teacher-loading">
          <div class="spinner" />
          加载中…
        </div>
        <div v-else-if="error" class="teacher-error">{{ error }}</div>
        <div v-else class="teacher-card">
          <div v-if="filtered.length === 0" class="teacher-empty">暂无待审核申请</div>
          <div v-else class="approval-list">
            <article v-for="req in filtered" :key="req.requestId" class="approval-item">
              <div class="approval-head">
                <div class="approval-title">
                  <strong>{{ req.studentName }}</strong>
                  <span class="approval-meta">
                    {{ req.major || '专业未填写' }}
                    <template v-if="req.graduationYear"> · 预计 {{ req.graduationYear }} 毕业</template>
                    · 提交于 {{ formatDateTime(req.submittedAt) }}
                  </span>
                </div>
                <div class="approval-actions">
                  <button type="button" class="teacher-btn teacher-btn-secondary" @click="openDetail(req.requestId)">查看详情</button>
                  <button type="button" class="teacher-btn btn-approve" @click="openDialog('APPROVE', req.requestId)">通过</button>
                  <button type="button" class="teacher-btn btn-reject" @click="openDialog('REJECT', req.requestId)">退回</button>
                </div>
              </div>
              <p class="approval-brief">{{ (req.biography && req.biography.length > 180) ? (req.biography.slice(0,180) + '…') : (req.biography || '—') }}</p>
            </article>
          </div>
        </div>
      </div>
    </main>

    <div v-if="dialog.visible" class="approval-modal" @click.self="closeDialog">
      <div class="approval-modal-panel">
        <h3>退回档案申请</h3>
        <p class="approval-modal-hint">请填写退回原因，学生将根据您的意见修改后重新提交。</p>
        <textarea
          v-model="dialog.comment"
          class="teacher-textarea"
          placeholder="例如：个人简介不够详细，请补充实习经历…"
          rows="4"
        />
        <div class="approval-modal-actions">
          <button type="button" class="teacher-btn teacher-btn-secondary" @click="closeDialog">取消</button>
          <button type="button" class="teacher-btn teacher-btn-primary" :disabled="submitting || !dialog.comment.trim()" @click="submit">
            {{ submitting ? '提交中…' : '确认退回' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style src="@/assets/teacher-layout.css"></style>
<style scoped>
.filters { display: flex; gap: 12px; align-items: center; }
.filter-input { width: 180px; }
.approval-list { display: flex; flex-direction: column; gap: 16px; }
.approval-item {
  padding: 20px;
  background: #f5f5f7;
  border-radius: 10px;
  border: 1px solid #e8e8ed;
}
.approval-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 12px; }
.approval-title { display: flex; flex-direction: column; gap: 4px; }
.approval-title strong { font-size: 1.0625rem; color: #1d1d1f; }
.approval-meta { font-size: 0.875rem; color: #86868b; }
.approval-actions { display: flex; gap: 8px; flex-shrink: 0; }
.btn-approve { background: #34c759; color: #fff; }
.btn-approve:hover { opacity: 0.9; }
.btn-reject { background: #ff3b30; color: #fff; }
.btn-reject:hover { opacity: 0.9; }
.approval-brief { margin: 0; font-size: 0.9375rem; color: #515154; line-height: 1.5; }

.approval-modal {
  position: fixed; inset: 0; z-index: 100;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.4); backdrop-filter: blur(8px);
}
.approval-modal-panel {
  width: min(480px, 92vw); background: #fff; border-radius: 12px;
  padding: 24px; box-shadow: 0 24px 48px rgba(0,0,0,0.18);
  display: flex; flex-direction: column; gap: 16px;
}
.approval-modal-panel h3 { margin: 0; font-size: 1.25rem; font-weight: 600; color: #1d1d1f; }
.approval-modal-hint { margin: 0; font-size: 0.9375rem; color: #86868b; line-height: 1.5; }
.approval-modal-actions { display: flex; justify-content: flex-end; gap: 12px; padding-top: 8px; }
</style>


