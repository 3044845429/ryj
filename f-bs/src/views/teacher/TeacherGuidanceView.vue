<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getTeacherByUserId, getTeacherDashboard } from '@/api/teacher'
import {
  getGuidanceRecords,
  createGuidanceRecord,
  deleteGuidanceRecord,
  type TeacherGuidance,
} from '@/api/guidance'

type TeacherRecord = {
  id: number
  userId: number
  department: string | null
  email: string | null
  phone: string | null
}

type GuidanceRecord = TeacherGuidance & { studentName?: string }

const teacherRecord = ref<TeacherRecord | null>(null)
const guidanceRecords = ref<GuidanceRecord[]>([])
const guidedStudents = ref<{ id: number; name: string; major: string }[]>([])
const loading = ref(true)
const submitting = ref(false)

// 表单数据
const showAddDialog = ref(false)
const formData = ref({
  studentId: null as number | null,
  manualStudentId: '' as string,
  note: ''
})

const message = ref<{ type: 'success' | 'error'; text: string } | null>(null)
let messageTimer: number | undefined

const showMessage = (type: 'success' | 'error', text: string) => {
  message.value = { type, text }
  if (messageTimer) {
    clearTimeout(messageTimer)
  }
  messageTimer = window.setTimeout(() => {
    message.value = null
  }, 3000)
}

// 用仪表盘结对学生补全每条指导记录的学生姓名，便于展示
const studentNameMap = computed(() => {
  const map: Record<number, string> = {}
  guidedStudents.value.forEach((s) => { map[s.id] = s.name })
  return map
})
const displayRecords = computed(() => {
  return guidanceRecords.value.map((r) => ({
    ...r,
    studentName: studentNameMap.value[r.studentId] || undefined,
  }))
})

const loadData = async () => {
  const stored = localStorage.getItem('userInfo')
  if (!stored) {
    showMessage('error', '请先登录')
    loading.value = false
    return
  }

  try {
    const userInfo = JSON.parse(stored)
    teacherRecord.value = await getTeacherByUserId(userInfo.id)
    const teacherId = teacherRecord.value.id

    const [page, dashboard] = await Promise.all([
      getGuidanceRecords({ teacherId, page: 1, size: 50 }),
      getTeacherDashboard(teacherId),
    ])
    guidanceRecords.value = (page.records || []) as GuidanceRecord[]
    guidedStudents.value = (dashboard.guidedStudents || []).map((s) => ({
      id: s.studentId,
      name: s.studentName,
      major: s.major || '未知专业',
    }))
  } catch (error: any) {
    showMessage('error', error?.message || '数据加载失败')
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  formData.value = {
    studentId: null,
    manualStudentId: '',
    note: ''
  }
  showAddDialog.value = true
}

const closeAddDialog = () => {
  showAddDialog.value = false
}

const submitGuidance = async () => {
  const selectedId = formData.value.studentId
  const manualId = formData.value.manualStudentId.trim()
  const targetId = selectedId || (manualId ? Number(manualId) : null)

  if (!targetId || !Number.isFinite(targetId) || targetId <= 0) {
    showMessage('error', '请从下拉列表选择学生，或输入有效的学生ID')
    return
  }
  if (!formData.value.note.trim()) {
    showMessage('error', '请填写指导内容')
    return
  }

  submitting.value = true
  try {
    await createGuidanceRecord({
      teacherId: teacherRecord.value!.id,
      studentId: targetId,
      note: formData.value.note.trim()
    })
    showMessage('success', '指导记录添加成功')
    closeAddDialog()
    await loadData()
  } catch (error: any) {
    showMessage('error', error?.message || '添加失败')
  } finally {
    submitting.value = false
  }
}

const deleteGuidance = async (id: number) => {
  if (!confirm('确定要删除这条指导记录吗？')) {
    return
  }

  try {
    await deleteGuidanceRecord(id)
    showMessage('success', '删除成功')
    await loadData()
  } catch (error: any) {
    showMessage('error', error?.message || '删除失败')
  }
}

const formatDateTime = (dateStr: string | undefined) => {
  if (!dateStr) return '—'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  loadData()
})
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
            <h1>指导记录</h1>
            <p class="subtitle">管理结对学生指导记录</p>
          </div>
          <button type="button" class="teacher-btn teacher-btn-primary" @click="openAddDialog" :disabled="loading">
            添加指导记录
          </button>
        </header>

        <transition name="fade">
          <div v-if="message" class="guidance-message" :class="message.type">{{ message.text }}</div>
        </transition>

        <div v-if="loading" class="teacher-loading">
          <div class="spinner" />
          加载中…
        </div>
        <div v-else-if="guidanceRecords.length === 0" class="teacher-card">
          <div class="teacher-empty">暂无指导记录，点击上方按钮添加第一条记录</div>
        </div>
        <div v-else class="teacher-card">
          <div class="records-list">
            <article v-for="record in displayRecords" :key="record.id" class="record-card">
              <div class="record-header">
                <div class="record-student">
                  <strong>{{ record.studentName || `学生ID: ${record.studentId}` }}</strong>
                  <span class="record-time">{{ formatDateTime(record.createdAt) }}</span>
                </div>
                <button type="button" class="teacher-btn btn-delete" @click="deleteGuidance(record.id!)">删除</button>
              </div>
              <div class="record-content">{{ record.note }}</div>
            </article>
          </div>
        </div>
      </div>
    </main>

    <transition name="modal">
      <div v-if="showAddDialog" class="guidance-modal" @click.self="closeAddDialog">
        <div class="guidance-modal-panel">
          <h2 class="card-title">添加指导记录</h2>
          <div class="form-group">
            <label class="teacher-label">选择学生（或输入学生ID）</label>
            <select v-model="formData.studentId" class="teacher-input">
              <option :value="null">从结对学生中选择</option>
              <option v-for="student in guidedStudents" :key="student.id" :value="student.id">
                {{ student.name }} — {{ student.major }}
              </option>
            </select>
            <div class="teacher-input-hint">如学生尚未在“结对学生”中，可直接在下面输入学生ID。</div>
          </div>
          <div class="form-group">
            <label class="teacher-label">学生ID（可选）</label>
            <input
              v-model="formData.manualStudentId"
              class="teacher-input"
              type="number"
              min="1"
              placeholder="例如：学生登录账号对应的ID"
            />
          </div>
          <div class="form-group">
            <label class="teacher-label">指导内容</label>
            <textarea
              v-model="formData.note"
              class="teacher-textarea"
              placeholder="记录本次指导的要点、建议和后续跟进计划…"
              rows="5"
              required
            />
          </div>
          <div class="guidance-modal-actions">
            <button type="button" class="teacher-btn teacher-btn-secondary" @click="closeAddDialog">取消</button>
            <button type="button" class="teacher-btn teacher-btn-primary" @click="submitGuidance" :disabled="submitting">
              {{ submitting ? '提交中…' : '确认添加' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style src="@/assets/teacher-layout.css"></style>
<style scoped>
.guidance-message {
  padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; font-size: 0.9375rem; font-weight: 500;
}
.guidance-message.success { background: rgba(52, 199, 89, 0.12); color: #248a3d; }
.guidance-message.error { background: rgba(255, 59, 48, 0.12); color: #d70015; }
.records-list { display: flex; flex-direction: column; gap: 16px; }
.record-card {
  padding: 20px; background: #f5f5f7; border-radius: 10px; border: 1px solid #e8e8ed;
}
.record-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.record-student { display: flex; flex-direction: column; gap: 4px; }
.record-student strong { font-size: 1.0625rem; color: #1d1d1f; }
.record-time { font-size: 0.875rem; color: #86868b; }
.btn-delete { background: rgba(255, 59, 48, 0.12); color: #d70015; padding: 8px 16px; font-size: 0.875rem; }
.btn-delete:hover { background: rgba(255, 59, 48, 0.2); }
.record-content { font-size: 0.9375rem; color: #515154; line-height: 1.5; white-space: pre-wrap; }

.guidance-modal {
  position: fixed; inset: 0; z-index: 100;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.4); backdrop-filter: blur(8px);
}
.guidance-modal-panel {
  width: min(520px, 92vw); background: #fff; border-radius: 12px;
  padding: 24px; box-shadow: 0 24px 48px rgba(0,0,0,0.18);
}
.guidance-modal-panel .card-title { margin-bottom: 20px; }
.form-group { margin-bottom: 20px; }
.form-group .teacher-label { margin-bottom: 8px; }
.guidance-modal-actions { display: flex; justify-content: flex-end; gap: 12px; padding-top: 8px; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.modal-enter-active, .modal-leave-active { transition: opacity 0.25s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>

