<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchStudentProfileDetail,
  submitStudentProfileRequest,
  updateStudentProfileRequest,
  deleteStudentProfileRequest,
  type StudentProfile,
  type StudentProfileDetail,
  type StudentProfileUpdateRequest,
  type ProfileRequestStatus,
} from '../api/student'
import { searchTeachers, type TeacherSearchItem, getAllHomeroomTeachers } from '../api/teacher'

const router = useRouter()
const route = useRoute()

const studentIdInput = ref('')
const studentIdError = ref('')
const detail = ref<StudentProfileDetail | null>(null)
const loading = ref(false)
const loadError = ref('')
const formSaving = ref(false)
const formMessage = ref('')
const formError = ref('')
const studentId = ref<number | null>(null)
const currentPending = ref<StudentProfileUpdateRequest | null>(null)

const profileForm = reactive({
  gender: '',
  age: null as number | null,
  major: '',
  biography: '',
  graduationYear: null as number | null,
  homeroomTeacherId: null as number | null,
})

const homeroomTeachers = ref<TeacherSearchItem[]>([])
const teacherLoading = ref(false)

const statusLabels: Record<ProfileRequestStatus, string> = {
  PENDING: '待审核',
  APPROVED: '教师已审核通过',
  REJECTED: '教师已驳回',
}

const pendingStatus = computed(() => currentPending.value?.status ?? null)

const approvedProfile = computed<StudentProfile | null>(() => detail.value?.profile ?? null)

const history = computed(() => detail.value?.history ?? [])

const pendingTagClass = computed(() => {
  const status = pendingStatus.value
  if (status === 'APPROVED') return 'tag tag--approved'
  if (status === 'REJECTED') return 'tag tag--rejected'
  if (status === 'PENDING') return 'tag tag--pending'
  return 'tag'
})

const parseStudentId = (value: string | null | undefined) => {
  if (!value) return null
  const trimmed = value.trim()
  if (!trimmed) return null
  const numeric = Number.parseInt(trimmed, 10)
  return Number.isNaN(numeric) ? null : numeric
}

const populateFromLatest = () => {
  const source = currentPending.value ?? approvedProfile.value
  profileForm.gender = source?.gender ?? ''
  profileForm.age = source?.age ?? null
  profileForm.major = source?.major ?? ''
  profileForm.biography = source?.biography ?? ''
  profileForm.graduationYear = source?.graduationYear ?? null
}

const populateFromApproved = () => {
  const source = approvedProfile.value
  profileForm.gender = source?.gender ?? ''
  profileForm.age = source?.age ?? null
  profileForm.major = source?.major ?? ''
  profileForm.biography = source?.biography ?? ''
  profileForm.graduationYear = source?.graduationYear ?? null
}

const loadHomeroomTeachers = async () => {
    teacherLoading.value = true
    try {
      // 默认加载全部教师作为下拉候选；可支持按专业筛选
      // homeroomTeachers.value = await searchTeachers({ major: profileForm.major.trim() })
      homeroomTeachers.value = await getAllHomeroomTeachers();
    } catch(e) {
      homeroomTeachers.value = [];
    } finally {
      teacherLoading.value = false;
    }
  }

// 兼容不同后端字段，格式化老师显示名称
const formatTeacherName = (t: any) => {
  return (
    t?.name || t?.fullName || t?.teacherName || t?.realName || t?.username || t?.userName || `教师#${t?.id ?? ''}`
  )
}

const formatTeacherDept = (t: any) => {
  return t?.department || t?.departmentName || t?.college || '未知学院'
}

const loadProfile = async (id: number) => {
  loading.value = true
  loadError.value = ''
  try {
    const response = await fetchStudentProfileDetail(id)
    detail.value = response
    currentPending.value = response.pendingRequest
    populateFromLatest()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载个人档案失败'
  } finally {
    loading.value = false
  }
}

const handleSelectStudent = async () => {
  const parsed = parseStudentId(studentIdInput.value)
  if (!parsed) {
    studentIdError.value = '请输入有效的学生ID'
    return
  }
  studentIdError.value = ''
  studentId.value = parsed
  localStorage.setItem('currentStudentId', String(parsed))
  await loadProfile(parsed)
  router.replace({
    name: 'student-profile',
    query: { studentId: String(parsed) },
  })
}

const handleSubmit = async () => {
  if (!studentId.value) {
    formError.value = '请先选择学生ID'
    return
  }
  
  // 验证必填项
  if (!profileForm.homeroomTeacherId) {
    formError.value = '请选择提交给哪位教师审核'
    return
  }
  
  formSaving.value = true
  formError.value = ''
  formMessage.value = ''
  try {
    const payload = {
      id: studentId.value,
      gender: profileForm.gender || null,
      age: profileForm.age ?? null,
      major: profileForm.major || null,
      biography: profileForm.biography || null,
      graduationYear: profileForm.graduationYear ?? null,
      reviewerId: profileForm.homeroomTeacherId ?? undefined,
    }
    
    // 如果当前有待审核申请，则更新；如果是被退回的申请，则重新提交
    if (currentPending.value && hasPending.value) {
      await updateStudentProfileRequest(currentPending.value.id, payload)
      formMessage.value = '已更新待审核的档案申请'
    } else {
      await submitStudentProfileRequest(payload)
      formMessage.value = '已提交档案更新申请，已提交至教师端等待审核'
    }
    await loadProfile(studentId.value)
  } catch (error) {
    formError.value = error instanceof Error ? error.message : '提交档案申请失败'
  } finally {
    formSaving.value = false
  }
}

const handleWithdraw = async () => {
  if (!studentId.value || !currentPending.value) return
  if (!window.confirm('确定要撤回当前的待审核申请吗？')) {
    return
  }
  formSaving.value = true
  formError.value = ''
  formMessage.value = ''
  try {
    await deleteStudentProfileRequest(currentPending.value.id, studentId.value)
    formMessage.value = '已撤回待审核申请'
    await loadProfile(studentId.value)
  } catch (error) {
    formError.value = error instanceof Error ? error.message : '撤回申请失败'
  } finally {
    formSaving.value = false
  }
}

watch(
  () => route.query.studentId,
  (value) => {
    const parsed = typeof value === 'string' ? parseStudentId(value) : null
    if (parsed) {
      studentId.value = parsed
      studentIdInput.value = String(parsed)
      loadProfile(parsed)
    }
  },
  { immediate: true }
)

onMounted(async () => {
  // 页面加载时先加载班主任列表
  await loadHomeroomTeachers()
  
  if (!route.query.studentId) {
    // 首先尝试从登录信息中获取学生ID
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo && userInfo.role === 'STUDENT' && userInfo.id) {
          const id = userInfo.id
          studentId.value = id
          studentIdInput.value = String(id)
          localStorage.setItem('currentStudentId', String(id))
          await loadProfile(id)
          router.replace({ name: 'student-profile', query: { studentId: String(id) } })
          return
        }
      } catch (e) {
        console.error('Failed to parse userInfo:', e)
      }
    }
    
    // 如果不是学生登录，再尝试从缓存中获取
    const stored = parseStudentId(localStorage.getItem('currentStudentId'))
    if (stored) {
      studentId.value = stored
      studentIdInput.value = String(stored)
      await loadProfile(stored)
      router.replace({ name: 'student-profile', query: { studentId: String(stored) } })
    }
  }
})

const hasPending = computed(() => pendingStatus.value === 'PENDING')
const isRejected = computed(() => pendingStatus.value === 'REJECTED')
const canEdit = computed(() => !hasPending.value || isRejected.value)
</script>

<template>
  <div class="profile-page">
    <header class="profile-header">
      <div>
        <h1>学生个人档案</h1>
        <p>档案更新需提交至教师端审核，审核通过后方可生效</p>
      </div>
      <div class="student-selector">
        <input v-model="studentIdInput" class="input" placeholder="输入学生ID" />
        <button class="btn btn--primary" @click="handleSelectStudent">加载档案</button>
      </div>
    </header>

    <p v-if="studentIdError" class="error">{{ studentIdError }}</p>
    <p v-if="loadError" class="error">{{ loadError }}</p>

    <section v-if="studentId" class="profile-content">
      <div v-if="loading" class="loading">正在加载档案信息...</div>

      <div v-else>
        <div class="card">
          <header class="card__header">
            <div>
              <h2>基本资料</h2>
              <p>当前展示的是最近一次审核通过的档案信息。</p>
            </div>
            <span v-if="pendingStatus" :class="pendingTagClass">
              <strong>当前申请：</strong> {{ statusLabels[pendingStatus] }}
            </span>
          </header>

          <div v-if="approvedProfile" class="profile-summary">
            <div class="profile-summary__item">
              <span class="label">性别</span>
              <span>{{ approvedProfile.gender || '未填写' }}</span>
            </div>
            <div class="profile-summary__item">
              <span class="label">年龄</span>
              <span>{{ approvedProfile.age ?? '未填写' }}</span>
            </div>
            <div class="profile-summary__item">
              <span class="label">专业</span>
              <span>{{ approvedProfile.major || '未填写' }}</span>
            </div>
            <div class="profile-summary__item">
              <span class="label">毕业年份</span>
              <span>{{ approvedProfile.graduationYear ?? '未填写' }}</span>
            </div>
            <div class="profile-summary__item profile-summary__item--full">
              <span class="label">个人简介</span>
              <span>{{ approvedProfile.biography || '未填写' }}</span>
            </div>
          </div>
          <div v-else class="empty-state">尚未有审核通过的档案信息。</div>
        </div>

        <div class="card">
          <header class="card__header">
            <div>
              <h2>提交/修改档案申请</h2>
              <p v-if="hasPending">当前有待审核申请，可在审核前进行修改或撤回。</p>
              <p v-else-if="isRejected">您的档案申请已被退回，请根据老师意见修改后重新提交。</p>
              <p v-else>填写以下信息提交新的档案更新申请。</p>
            </div>
            <div class="card__actions">
              <button class="btn" @click="populateFromLatest">同步当前申请数据</button>
              <button v-if="approvedProfile" class="btn" @click="populateFromApproved">填充已通过档案</button>
              <button v-if="hasPending" class="btn btn--danger" @click="handleWithdraw">撤回申请</button>
            </div>
          </header>

          <form class="form" @submit.prevent="handleSubmit">
            <div class="form__grid">
              <label class="form__field">
                <span>性别</span>
                <input v-model="profileForm.gender" class="input" placeholder="如：男 / 女" />
              </label>
              <label class="form__field">
                <span>年龄</span>
                <input v-model.number="profileForm.age" class="input" type="number" min="0" placeholder="如：21" />
              </label>
              <label class="form__field">
                <span>专业</span>
                <input v-model="profileForm.major" class="input" placeholder="填写主修专业" />
              </label>
              <label class="form__field">
                <span>预计毕业年份</span>
                <input v-model.number="profileForm.graduationYear" class="input" type="number" min="1900" max="2100" placeholder="如：2025" />
              </label>
              <label class="form__field">
                <span>提交给哪位教师审核</span>
                <select v-model.number="profileForm.homeroomTeacherId" class="input">
                  <option :value="null">请选择审核教师</option>
                  <option v-for="t in homeroomTeachers" :key="t.id" :value="t.id">
                    {{ formatTeacherName(t) }}（{{ formatTeacherDept(t) }}）
                  </option>
                </select>
                <small v-if="teacherLoading" style="color:#6b7280;">加载可选审核教师...</small>
              </label>
            </div>
            <label class="form__field">
              <span>个人简介</span>
              <textarea v-model="profileForm.biography" class="textarea" rows="5" placeholder="简要介绍学习背景、兴趣特长等"></textarea>
            </label>

            <div class="form__actions">
              <button type="submit" class="btn btn--primary" :disabled="formSaving">
                {{ formSaving ? '提交中...' : currentPending ? '更新待审核申请' : '提交审核' }}
              </button>
            </div>
          </form>

          <p v-if="formMessage" class="message">{{ formMessage }}</p>
          <p v-if="formError" class="error">{{ formError }}</p>
        </div>

        <div class="card">
          <header class="card__header">
            <div>
              <h2>申请记录</h2>
              <p>查看档案更新申请的历史记录与审核结果。</p>
            </div>
          </header>

          <table v-if="history.length" class="history-table">
            <thead>
              <tr>
                <th>申请时间</th>
                <th>状态</th>
                <th>审核时间</th>
                <th>审核人</th>
                <th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in history" :key="item.id">
                <td>{{ item.createdAt ? new Date(item.createdAt).toLocaleString() : '未知' }}</td>
                <td>
                  <span :class="['tag', `tag--${item.status.toLowerCase()}`]">{{ statusLabels[item.status] }}</span>
                </td>
                <td>{{ item.reviewedAt ? new Date(item.reviewedAt).toLocaleString() : '-' }}</td>
                <td>{{ item.reviewerId ?? '-' }}</td>
                <td>{{ item.reviewComment || '-' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-state">暂无申请记录。</div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.profile-header h1 {
  margin: 0;
  font-size: 28px;
}

.profile-header p {
  margin: 4px 0 0;
  color: #5f6368;
}

.student-selector {
  display: flex;
  gap: 12px;
  align-items: center;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 12px 40px rgba(15, 35, 95, 0.08);
}

.card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.card__header h2 {
  margin: 0;
  font-size: 20px;
}

.card__header p {
  margin: 6px 0 0;
  color: #6b7280;
}

.card__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.profile-summary {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.profile-summary__item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
}

.profile-summary__item--full {
  grid-column: 1 / -1;
}

.label {
  font-size: 13px;
  color: #6b7280;
}

.input,
.textarea {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  font-size: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.input:focus,
.textarea:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.btn {
  border: none;
  border-radius: 999px;
  padding: 10px 18px;
  background: #e5e7eb;
  color: #111827;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.12);
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
  box-shadow: none;
}

.btn--primary {
  background: linear-gradient(135deg, #2563eb, #4338ca);
  color: #fff;
}

.btn--danger {
  background: #f87171;
  color: #fff;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form__grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.form__field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form__actions {
  display: flex;
  justify-content: flex-end;
}

.message {
  margin-top: 16px;
  color: #16a34a;
}

.error {
  margin-top: 12px;
  color: #dc2626;
}

.loading {
  color: #2563eb;
}

.empty-state {
  padding: 24px;
  text-align: center;
  color: #6b7280;
  background: #f9fafb;
  border-radius: 12px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
  background: #e2e8f0;
  color: #1f2937;
}

.tag--pending,
.tag--pending.tag {
  background: rgba(251, 191, 36, 0.2);
  color: #b45309;
}

.tag--approved,
.tag--approved.tag {
  background: rgba(74, 222, 128, 0.2);
  color: #166534;
}

.tag--rejected,
.tag--rejected.tag {
  background: rgba(248, 113, 113, 0.2);
  color: #b91c1c;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 16px;
}

.history-table th,
.history-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.history-table tbody tr:hover {
  background: #f8fafc;
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .student-selector {
    width: 100%;
  }

  .student-selector .input {
    flex: 1;
  }

  .card__actions {
    flex-direction: column;
    align-items: flex-end;
  }
}
</style>
