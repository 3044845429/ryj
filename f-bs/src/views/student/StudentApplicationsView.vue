<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getMyApplications, type ApplicationStatus, type StudentApplicationItem } from '../../api/application'

const statusOptions: { value: ApplicationStatus | 'all'; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 'SUBMITTED', label: '已提交' },
  { value: 'REVIEWING', label: '筛选中' },
  { value: 'INTERVIEW', label: '面试' },
  { value: 'OFFERED', label: '已录用' },
  { value: 'REJECTED', label: '已拒绝' },
]

const studentId = ref<number | null>(null)
const applications = ref<StudentApplicationItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 15
const loading = ref(false)
const error = ref('')
const selectedStatus = ref<ApplicationStatus | 'all'>('all')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const filteredStatus = computed(() => (selectedStatus.value === 'all' ? undefined : selectedStatus.value))

const formatDate = (value: string | null | undefined) => {
  if (!value) return '—'
  try {
    const d = new Date(value)
    return new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).format(d)
  } catch {
    return value
  }
}

const getStatusLabel = (status: string) => {
  const opt = statusOptions.find((o) => o.value === status)
  return opt?.label ?? status
}

const getStatusClass = (status: string) => {
  const map: Record<string, string> = {
    SUBMITTED: 'status-submitted',
    REVIEWING: 'status-reviewing',
    INTERVIEW: 'status-interview',
    OFFERED: 'status-offered',
    REJECTED: 'status-rejected',
  }
  return map[status] ?? ''
}

const workTypeLabels: Record<string, string> = {
  FULL_TIME: '全职',
  PART_TIME: '兼职',
  INTERNSHIP: '实习',
  REMOTE: '远程',
}
const formatWorkType = (type: string | null | undefined) => {
  if (!type) return '—'
  return workTypeLabels[type] ?? type
}

const loadApplications = async () => {
  if (studentId.value == null) {
    applications.value = []
    total.value = 0
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await getMyApplications(studentId.value, {
      page: currentPage.value,
      size: pageSize,
      status: filteredStatus.value,
    })
    const raw = data && typeof data === 'object' ? (data as Record<string, unknown>) : {}
    const list = Array.isArray(raw.records) ? raw.records : []
    const totalNum = Number(raw.total) ?? 0
    applications.value = list as StudentApplicationItem[]
    total.value = totalNum
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
    applications.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const goToPage = (page: number) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadApplications()
}

onMounted(() => {
  try {
    const raw = localStorage.getItem('userInfo')
    if (raw) {
      const info = JSON.parse(raw)
      if (info?.role === 'STUDENT' && info?.id) {
        studentId.value = info.id
        loadApplications()
        return
      }
    }
  } catch (_) {}
  error.value = '请先使用学生账号登录后查看申请记录'
})

watch(selectedStatus, () => {
  currentPage.value = 1
  loadApplications()
})
</script>

<template>
  <div class="applications-page">
    <header class="top-bar">
      <div class="top-bar__left">
        <h1>我的申请</h1>
        <p>投递记录与状态一览</p>
      </div>
      <div class="top-bar__right">
        <span v-if="studentId != null && !loading && applications.length > 0" class="summary">
          共 {{ total }} 条
        </span>
        <label class="filter-wrap">
          <span class="filter-label">状态</span>
          <select v-model="selectedStatus" class="filter-select">
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </label>
      </div>
    </header>

    <main class="main-area">
      <div v-if="studentId == null && !loading" class="state-screen state-screen--empty">
        <div class="state-screen__icon">👤</div>
        <h2>请先登录</h2>
        <p>使用学生账号登录后查看申请记录</p>
        <RouterLink to="/login" class="btn btn-primary">去登录</RouterLink>
      </div>

      <div v-else-if="error && !loading" class="state-screen state-screen--error">
        <p>{{ error }}</p>
        <button type="button" class="btn btn-secondary" @click="loadApplications">重试</button>
      </div>

      <div v-else-if="loading" class="state-screen state-screen--loading">
        加载中…
      </div>

      <div v-else-if="applications.length === 0" class="state-screen state-screen--empty">
        <div class="state-screen__icon">💼</div>
        <h2>暂无申请</h2>
        <p>去职位页投递后，状态会在此更新</p>
        <RouterLink to="/student/jobs" class="btn btn-primary">浏览职位</RouterLink>
      </div>

      <template v-else>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>职位</th>
                <th>公司</th>
                <th>地点</th>
                <th>薪资</th>
                <th>类型</th>
                <th>状态</th>
                <th>申请时间</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="app in applications" :key="app.id">
                <td class="cell-job">
                  <RouterLink :to="{ path: '/student/jobs', query: { jobId: app.jobId } }" class="job-link">
                    {{ app.jobTitle || '—' }}
                  </RouterLink>
                </td>
                <td>{{ app.companyName || '—' }}</td>
                <td>{{ app.location || '—' }}</td>
                <td>{{ app.salaryRange || '—' }}</td>
                <td>{{ formatWorkType(app.workType) }}</td>
                <td>
                  <span :class="['status-badge', getStatusClass(app.status)]">
                    {{ getStatusLabel(app.status) }}
                  </span>
                </td>
                <td class="cell-date">{{ formatDate(app.appliedAt) }}</td>
                <td class="col-action">
                  <RouterLink :to="{ path: '/student/jobs', query: { jobId: app.jobId } }" class="btn btn-link">
                    查看
                  </RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <footer v-if="totalPages > 1" class="pagination-bar">
          <button
            type="button"
            class="btn btn-ghost"
            :disabled="currentPage <= 1"
            @click="goToPage(currentPage - 1)"
          >
            上一页
          </button>
          <span class="pagination-text">第 {{ currentPage }} / {{ totalPages }} 页</span>
          <button
            type="button"
            class="btn btn-ghost"
            :disabled="currentPage >= totalPages"
            @click="goToPage(currentPage + 1)"
          >
            下一页
          </button>
        </footer>
      </template>
    </main>
  </div>
</template>

<style scoped>
.applications-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f7;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.top-bar {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1.5rem;
  padding: 24px 32px 20px;
  background: #fff;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.top-bar__left h1 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 2px;
  letter-spacing: -0.02em;
}

.top-bar__left p {
  font-size: 0.9375rem;
  color: #86868b;
  margin: 0;
}

.top-bar__right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.summary {
  font-size: 0.9375rem;
  color: #86868b;
}

.filter-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filter-label {
  font-size: 0.875rem;
  color: #86868b;
  font-weight: 500;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #d2d2d7;
  border-radius: 8px;
  background: #fff;
  font-size: 0.9375rem;
  color: #1d1d1f;
  cursor: pointer;
  min-width: 100px;
}

.filter-select:focus {
  outline: none;
  border-color: #0071e3;
}

.main-area {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  margin: 20px 32px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.state-screen {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}

.state-screen--loading {
  color: #86868b;
  font-size: 1rem;
}

.state-screen--error {
  color: #bf4800;
  font-size: 1rem;
}

.state-screen--error .btn {
  margin-top: 16px;
}

.state-screen__icon {
  font-size: 2.5rem;
  margin-bottom: 12px;
}

.state-screen h2 {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 8px;
}

.state-screen p {
  font-size: 0.9375rem;
  color: #86868b;
  margin: 0 0 20px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 0.9375rem;
  font-weight: 500;
  text-decoration: none;
  border: none;
  cursor: pointer;
  transition: opacity 0.2s, background 0.2s;
}

.btn-primary {
  background: #0071e3;
  color: #fff;
}

.btn-primary:hover {
  opacity: 0.9;
}

.btn-secondary {
  background: #e8e8ed;
  color: #1d1d1f;
}

.btn-secondary:hover {
  background: #d2d2d7;
}

.btn-ghost {
  background: transparent;
  color: #0071e3;
}

.btn-ghost:hover:not(:disabled) {
  text-decoration: underline;
}

.btn-ghost:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-link {
  background: transparent;
  color: #0071e3;
  padding: 6px 12px;
}

.btn-link:hover {
  text-decoration: underline;
}

.table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9375rem;
}

.data-table th {
  text-align: left;
  padding: 14px 16px;
  font-weight: 600;
  color: #1d1d1f;
  background: #f5f5f7;
  border-bottom: 1px solid #e8e8ed;
  white-space: nowrap;
}

.data-table td {
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f2;
  color: #515154;
  vertical-align: middle;
}

.data-table tbody tr:hover {
  background: #fafafa;
}

.data-table tbody tr:last-child td {
  border-bottom: none;
}

.cell-job {
  font-weight: 500;
  color: #1d1d1f;
}

.job-link {
  color: #1d1d1f;
  text-decoration: none;
}

.job-link:hover {
  color: #0071e3;
  text-decoration: underline;
}

.cell-date {
  color: #86868b;
  white-space: nowrap;
}

.col-action {
  width: 1%;
  white-space: nowrap;
}

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 500;
  white-space: nowrap;
}

.status-submitted {
  background: #e8f4fd;
  color: #0071e3;
}

.status-reviewing {
  background: #fff4e5;
  color: #bf4800;
}

.status-interview {
  background: #e8f4fd;
  color: #0071e3;
}

.status-offered {
  background: #e8f5e9;
  color: #2e7d32;
}

.status-rejected {
  background: #ffebee;
  color: #c62828;
}

.pagination-bar {
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  border-top: 1px solid #e8e8ed;
  background: #fafafa;
}

.pagination-text {
  font-size: 0.9375rem;
  color: #86868b;
}

@media (max-width: 900px) {
  .top-bar {
    padding: 20px 20px 16px;
  }

  .main-area {
    margin: 16px 20px 20px;
  }

  .table-wrap {
    overflow-x: auto;
  }

  .data-table {
    min-width: 800px;
  }
}

@media (max-width: 600px) {
  .top-bar {
    flex-direction: column;
    align-items: stretch;
    padding: 16px;
  }

  .top-bar__right {
    justify-content: space-between;
  }

  .main-area {
    margin: 12px;
  }
}
</style>
