<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  createJobApplication,
  fetchJobPostingDetail,
  fetchJobPostings,
  fetchStudentResumes,
  type JobPosting,
  type JobPostingDetail,
  type JobPostingWorkType,
  type ResumeSummary,
} from '../../api/student'

const route = useRoute()

const pageSize = 6

const jobList = ref<JobPosting[]>([])
const totalJobs = ref(0)
const currentPage = ref(1)
const loadingJobs = ref(false)
const jobError = ref('')

const selectedJobId = ref<number | null>(null)
const jobDetail = ref<JobPostingDetail | null>(null)
const detailLoading = ref(false)
const detailError = ref('')

const searchForm = reactive({
  keyword: '',
  workType: '' as JobPostingWorkType | '',
})

const studentIdInput = ref('')
const studentId = ref<number | null>(null)
const resumes = ref<ResumeSummary[]>([])
const resumeLoading = ref(false)
const resumeError = ref('')
const selectedResumeId = ref<number | null>(null)

const coverLetter = ref('')
const sendingApplication = ref(false)
const applicationMessage = ref('')
const applicationError = ref('')

const workTypeOptions: { value: JobPostingWorkType | ''; label: string }[] = [
  { value: '', label: '全部类型' },
  { value: 'FULL_TIME', label: '全职' },
  { value: 'PART_TIME', label: '兼职' },
  { value: 'INTERNSHIP', label: '实习' },
  { value: 'FLEXIBLE', label: '灵活' },
  { value: 'REMOTE', label: '远程' },
]

const jobStatusLabels: Record<string, string> = {
  OPEN: '招聘中',
  CLOSED: '已结束',
  DRAFT: '草稿',
}

const totalPages = computed(() => {
  if (totalJobs.value <= 0) return 1
  return Math.max(1, Math.ceil(totalJobs.value / pageSize))
})

const selectedJob = computed(() => jobDetail.value?.posting ?? null)
const requirementList = computed(() => jobDetail.value?.requirements ?? [])
const descriptionParagraphs = computed(() => {
  const description = selectedJob.value?.description ?? ''
  if (!description) return []
  return description
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
})

const resumeSelectModel = computed({
  get: () => (selectedResumeId.value ? String(selectedResumeId.value) : ''),
  set: (value: string) => {
    selectedResumeId.value = value ? Number(value) : null
  },
})

const formatWorkType = (type: JobPostingWorkType | null | undefined) => {
  const map: Record<JobPostingWorkType, string> = {
    FULL_TIME: '全职',
    PART_TIME: '兼职',
    INTERNSHIP: '实习',
    FLEXIBLE: '灵活',
    REMOTE: '远程',
  }
  return type ? map[type] : '未指定'
}

const formatDate = (value: string | null | undefined) =>
  value ? value.replace('T', ' ').substring(0, 10) : '未注明'

let listRequestToken = 0
let detailRequestToken = 0

const loadJobDetail = async (jobId: number) => {
  detailLoading.value = true
  detailError.value = ''
  const token = ++detailRequestToken
  try {
    const data = await fetchJobPostingDetail(jobId)
    if (token !== detailRequestToken) return
    jobDetail.value = {
      posting: data.posting,
      requirements: data.requirements ?? [],
    }
  } catch (error) {
    if (token !== detailRequestToken) return
    jobDetail.value = null
    detailError.value = error instanceof Error ? error.message : '职位详情加载失败'
  } finally {
    if (token === detailRequestToken) {
      detailLoading.value = false
    }
  }
}

const loadJobs = async () => {
  loadingJobs.value = true
  jobError.value = ''
  const token = ++listRequestToken
  try {
    const data = await fetchJobPostings({
      page: currentPage.value,
      size: pageSize,
      workType: searchForm.workType,
      keyword: searchForm.keyword,
    })
    if (token !== listRequestToken) return
    // 兼容多种分页返回格式：直接 Page 或 再包一层 data
    const raw = data && typeof data === 'object' ? data as Record<string, unknown> : {}
    const list = Array.isArray(raw.records) ? raw.records : (Array.isArray((raw.data as Record<string, unknown>)?.records) ? (raw.data as Record<string, unknown>).records : [])
    const total = Number(raw.total) || Number((raw.data as Record<string, unknown>)?.total) || 0
    jobList.value = list as JobPosting[]
    totalJobs.value = total

    if (!jobList.value.length) {
      selectedJobId.value = null
      jobDetail.value = null
      detailError.value = ''
      return
    }

    if (!selectedJobId.value || !jobList.value.some((job) => job.id === selectedJobId.value)) {
      const firstJob = jobList.value[0]
      if (firstJob) {
        selectedJobId.value = firstJob.id
      }
    }

    if (selectedJobId.value) {
      await loadJobDetail(selectedJobId.value)
    }
  } catch (error) {
    if (token !== listRequestToken) return
    jobError.value = error instanceof Error ? error.message : '职位列表加载失败'
    jobList.value = []
    totalJobs.value = 0
    selectedJobId.value = null
    jobDetail.value = null
  } finally {
    if (token === listRequestToken) {
      loadingJobs.value = false
    }
  }
}

const selectJob = async (jobId: number) => {
  if (selectedJobId.value === jobId && jobDetail.value) return
  selectedJobId.value = jobId
  await loadJobDetail(jobId)
  applicationMessage.value = ''
  applicationError.value = ''
}

const goToPage = (page: number) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  currentPage.value = page
  loadJobs()
}

const resetFilters = () => {
  const workTypeChanged = searchForm.workType !== ''
  searchForm.keyword = ''
  if (workTypeChanged) {
    searchForm.workType = ''
  } else {
    currentPage.value = 1
    loadJobs()
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadJobs()
}

const parseStudentId = (value: string): number | null => {
  const trimmed = value.trim()
  if (!trimmed) return null
  const parsed = Number(trimmed)
  if (!Number.isInteger(parsed) || parsed <= 0) return null
  return parsed
}

const loadResumes = async () => {
  if (!studentId.value) return
  resumeLoading.value = true
  resumeError.value = ''
  try {
    const data = await fetchStudentResumes(studentId.value)
    resumes.value = data ?? []
    if (!resumes.value.length) {
      selectedResumeId.value = null
    } else if (!selectedResumeId.value || !resumes.value.some((resume) => resume.id === selectedResumeId.value)) {
      const firstResume = resumes.value[0]
      selectedResumeId.value = firstResume ? firstResume.id : null
    }
  } catch (error) {
    resumeError.value = error instanceof Error ? error.message : '简历列表加载失败'
    resumes.value = []
    selectedResumeId.value = null
  } finally {
    resumeLoading.value = false
  }
}

const confirmStudent = async () => {
  applicationMessage.value = ''
  applicationError.value = ''
  const parsed = parseStudentId(studentIdInput.value)
  if (!parsed) {
    resumeError.value = '请输入正确的学生 ID'
    studentId.value = null
    resumes.value = []
    selectedResumeId.value = null
    return
  }
  studentId.value = parsed
  studentIdInput.value = String(parsed)
  resumeError.value = ''
  await loadResumes()
}

const sendApplication = async () => {
  if (!selectedJobId.value) {
    applicationError.value = '请先选择要申请的职位'
    return
  }
  if (!studentId.value) {
    applicationError.value = '请先确认学生 ID'
    return
  }
  if (!selectedResumeId.value) {
    applicationError.value = '请先选择要发送的简历'
    return
  }
  sendingApplication.value = true
  applicationError.value = ''
  applicationMessage.value = ''
  try {
    await createJobApplication({
      jobId: selectedJobId.value,
      studentId: studentId.value,
      resumeId: selectedResumeId.value,
      coverLetter: coverLetter.value || '',
    })
    applicationMessage.value = '求职申请已发送，简历将一同提交给企业'
    coverLetter.value = ''
  } catch (error) {
    applicationError.value = error instanceof Error ? error.message : '发送申请失败'
  } finally {
    sendingApplication.value = false
  }
}

watch(
  () => searchForm.workType,
  () => {
    currentPage.value = 1
    loadJobs()
  }
)

watch(
  () => selectedJobId.value,
  () => {
    detailError.value = ''
  }
)

onMounted(async () => {
  await loadJobs()
  
  // 检查URL参数，如果有jobId，自动选中该职位
  const jobIdParam = route.query.jobId
  if (jobIdParam) {
    const jobId = Number(jobIdParam)
    if (!isNaN(jobId)) {
      selectedJobId.value = jobId
    }
  }
})
</script>

<template>
  <div class="job-board">
    <header class="page-header">
      <div>
        <h1>学生求职申请</h1>
        <p>浏览企业发布的最新岗位，了解岗位详情并直接向企业发送求职申请与简历。</p>
      </div>
      <div class="student-box">
        <label class="student-box__label" for="student-id-input">关联学生 ID：</label>
        <div class="student-box__control">
          <input
            id="student-id-input"
            v-model="studentIdInput"
            type="text"
            placeholder="请输入学生 ID"
            @keyup.enter="confirmStudent"
          />
          <button type="button" class="btn-primary" @click="confirmStudent">确认</button>
        </div>
        <p v-if="studentId" class="student-box__hint">当前学生：#{{ studentId }}</p>
        <p v-if="resumeError" class="feedback feedback--error">{{ resumeError }}</p>
      </div>
    </header>

    <section class="filters">
      <div class="filter-group">
        <label for="job-keyword">关键词</label>
        <input
          id="job-keyword"
          v-model="searchForm.keyword"
          type="text"
          placeholder="岗位名称、技能标签等"
          @keyup.enter="handleSearch"
        />
      </div>
      <div class="filter-group">
        <label for="job-worktype">工作类型</label>
        <select id="job-worktype" v-model="searchForm.workType">
          <option v-for="option in workTypeOptions" :key="option.value || 'all'" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>
      <div class="filter-actions">
        <button type="button" class="btn-primary" @click="handleSearch">搜索职位</button>
        <button type="button" class="btn-secondary" @click="resetFilters">重置筛选</button>
      </div>
    </section>

    <div class="board-content">
      <aside class="job-list">
        <div v-if="loadingJobs" class="loading">正在加载职位，请稍候...</div>
        <div v-else-if="jobError" class="feedback feedback--error">
          <p>{{ jobError }}</p>
          <p class="empty-hint">请确认后端服务已启动（如 http://localhost:8080），且数据库已执行 <code>database/restore_all.sql</code>。</p>
        </div>
        <div v-else>
          <p class="job-list__summary">共找到 {{ totalJobs }} 个职位</p>
          <div v-if="jobList.length" class="job-list__items">
            <article
              v-for="job in jobList"
              :key="job.id"
              :class="['job-card', { 'job-card--active': job.id === selectedJobId }]"
              @click="selectJob(job.id)"
            >
              <h3>{{ job.title }}</h3>
              <ul class="job-card__meta">
                <li>{{ job.location || '工作地点待定' }}</li>
                <li>{{ formatWorkType(job.workType) }}</li>
                <li>{{ job.salaryRange || '薪资面议' }}</li>
              </ul>
              <p class="job-card__date">发布日期：{{ formatDate(job.publishedDate) }}</p>
            </article>
          </div>
          <div v-else class="empty">
            <p>暂未查询到符合条件的职位，可调整筛选条件后再试。</p>
            <p class="empty-hint">若从未导入过演示数据，请在 Navicat 中对数据库 <code>bb</code> 执行 <code>database/restore_all.sql</code> 以初始化岗位数据。</p>
          </div>
          <div v-if="totalPages > 1" class="pagination">
            <button type="button" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">上一页</button>
            <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
            <button type="button" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">
              下一页
            </button>
          </div>
        </div>
      </aside>

      <section class="job-detail">
        <div v-if="detailLoading" class="loading">正在加载职位详情...</div>
        <div v-else-if="detailError" class="feedback feedback--error">{{ detailError }}</div>
        <div v-else-if="selectedJob" class="job-detail__content">
          <header class="job-detail__header">
            <div>
              <h2>{{ selectedJob.title }}</h2>
              <p class="job-detail__meta">
                <span>{{ selectedJob.location || '地点待定' }}</span>
                <span>{{ formatWorkType(selectedJob.workType) }}</span>
                <span>{{ selectedJob.salaryRange || '薪资面议' }}</span>
              </p>
            </div>
            <span class="status" :class="[`status--${selectedJob.status?.toLowerCase()}`]">
              {{ jobStatusLabels[selectedJob.status || ''] || '状态未知' }}
            </span>
          </header>

          <dl class="job-detail__dates">
            <div>
              <dt>发布日期</dt>
              <dd>{{ formatDate(selectedJob.publishedDate) }}</dd>
            </div>
            <div>
              <dt>截止日期</dt>
              <dd>{{ formatDate(selectedJob.closingDate) }}</dd>
            </div>
          </dl>

          <section class="job-section">
            <h3>岗位描述</h3>
            <p v-if="!descriptionParagraphs.length" class="empty">企业暂未提供详细描述。</p>
            <p v-for="(paragraph, index) in descriptionParagraphs" :key="index">{{ paragraph }}</p>
          </section>

          <section class="job-section">
            <h3>岗位要求</h3>
            <ul v-if="requirementList.length" class="requirement-list">
              <li v-for="item in requirementList" :key="item.requirement">{{ item.requirement }}</li>
            </ul>
            <p v-else class="empty">企业暂未补充具体要求。</p>
          </section>

          <section class="apply-box">
            <h3>发送求职申请</h3>
            <div class="form-grid">
              <label>
                选择简历
                <select v-model="resumeSelectModel" :disabled="!resumes.length || resumeLoading">
                  <option value="" disabled>请选择要发送的简历</option>
                  <option v-for="resume in resumes" :key="resume.id" :value="resume.id">
                    #{{ resume.id }} - {{ resume.title }}
                  </option>
                </select>
              </label>
              <label>
                附加说明
                <textarea
                  v-model="coverLetter"
                  rows="3"
                  placeholder="可选：向企业介绍您的优势或求职意向"
                />
              </label>
            </div>
            <p v-if="resumeLoading" class="loading loading--inline">正在加载简历...</p>
            <p
              v-else-if="studentId && !resumes.length"
              class="feedback feedback--info"
            >
              当前学生暂无简历，请前往简历管理页面创建后再试。
            </p>
            <p v-if="applicationMessage" class="feedback feedback--success">{{ applicationMessage }}</p>
            <p v-if="applicationError" class="feedback feedback--error">{{ applicationError }}</p>
            <div class="apply-actions">
              <button type="button" class="btn-primary" :disabled="sendingApplication" @click="sendApplication">
                {{ sendingApplication ? '发送中...' : '发送求职申请' }}
              </button>
              <p class="apply-actions__hint">点击后将同时提交所选简历与申请说明。</p>
            </div>
          </section>
        </div>
        <div v-else class="job-detail__empty">
          <p>请选择左侧的职位查看详细信息并发送申请。</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.job-board {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1.5rem 3rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 2rem;
  flex-wrap: wrap;
  margin-bottom: 2rem;
}

.page-header h1 {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  color: #1e293b;
}

.page-header p {
  color: #64748b;
  line-height: 1.6;
}

.student-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1rem 1.25rem;
  min-width: 260px;
}

.student-box__label {
  font-weight: 600;
  color: #334155;
}

.student-box__control {
  display: flex;
  margin-top: 0.75rem;
  gap: 0.75rem;
}

.student-box__control input {
  flex: 1;
  padding: 0.5rem 0.75rem;
  border: 1px solid #cbd5f5;
  border-radius: 8px;
}

.student-box__hint {
  margin-top: 0.75rem;
  color: #0f766e;
  font-weight: 500;
}

.filters {
  display: flex;
  gap: 1.5rem;
  align-items: flex-end;
  flex-wrap: wrap;
  margin-bottom: 2rem;
  background: #ffffff;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-group label {
  font-weight: 600;
  color: #334155;
}

.filter-group input,
.filter-group select {
  min-width: 220px;
  padding: 0.6rem 0.75rem;
  border: 1px solid #cbd5f5;
  border-radius: 10px;
}

.filter-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.board-content {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(0, 2fr);
  gap: 1.5rem;
}

.job-list {
  background: #ffffff;
  border-radius: 18px;
  padding: 1.5rem;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  height: fit-content;
}

.job-list__summary {
  font-weight: 600;
  color: #475569;
  margin-bottom: 1rem;
}

.job-list__items {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.job-card {
  border: 1px solid transparent;
  border-radius: 14px;
  padding: 1rem 1.25rem;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  cursor: pointer;
  transition: all 0.25s ease;
}

.job-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(102, 126, 234, 0.2);
}

.job-card--active {
  border-color: #6366f1;
  box-shadow: 0 12px 26px rgba(99, 102, 241, 0.2);
}

.job-card h3 {
  font-size: 1.1rem;
  color: #1f2937;
  margin-bottom: 0.75rem;
}

.job-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  color: #475569;
  font-size: 0.9rem;
  margin-bottom: 0.75rem;
}

.job-card__meta li::before {
  content: '•';
  margin-right: 0.35rem;
}

.job-card__date {
  font-size: 0.85rem;
  color: #64748b;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-top: 1.5rem;
}

.pagination button {
  padding: 0.5rem 1.25rem;
  border-radius: 999px;
  border: none;
  background: #e0e7ff;
  color: #4338ca;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.job-detail {
  background: #ffffff;
  border-radius: 18px;
  padding: 2rem;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  min-height: 520px;
}

.job-detail__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
  margin-bottom: 1.5rem;
}

.job-detail__header h2 {
  font-size: 1.5rem;
  color: #1e293b;
  margin-bottom: 0.5rem;
}

.job-detail__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  color: #475569;
}

.status {
  align-self: flex-start;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
  background: #e2e8f0;
  color: #475569;
  text-transform: uppercase;
}

.status--open {
  background: #dcfce7;
  color: #166534;
}

.status--closed {
  background: #fee2e2;
  color: #b91c1c;
}

.status--draft {
  background: #ede9fe;
  color: #5b21b6;
}

.job-detail__dates {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.job-detail__dates dt {
  font-weight: 600;
  color: #475569;
}

.job-detail__dates dd {
  margin-top: 0.25rem;
  color: #0f172a;
}

.job-section + .job-section {
  margin-top: 1.75rem;
}

.job-section h3 {
  font-size: 1.1rem;
  margin-bottom: 0.75rem;
  color: #1f2937;
}

.requirement-list {
  display: grid;
  gap: 0.5rem;
  padding-left: 1.25rem;
  list-style: disc;
  color: #475569;
}

.apply-box {
  margin-top: 2.5rem;
  padding-top: 1.75rem;
  border-top: 1px dashed #cbd5f5;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1.5rem;
}

.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  color: #334155;
  font-weight: 600;
}

.form-grid select,
.form-grid textarea {
  border: 1px solid #cbd5f5;
  border-radius: 10px;
  padding: 0.6rem 0.75rem;
  font-weight: 500;
}

.form-grid textarea {
  resize: vertical;
}

.apply-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1.5rem;
}

.apply-actions__hint {
  color: #64748b;
  font-size: 0.9rem;
}

.job-detail__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #64748b;
}

.loading {
  color: #6366f1;
  font-weight: 600;
}

.loading--inline {
  margin-top: 0.5rem;
}

.feedback {
  margin-top: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  font-weight: 500;
}

.feedback--success {
  background: #dcfce7;
  color: #166534;
}

.feedback--info {
  background: #dbeafe;
  color: #1d4ed8;
}

.feedback--error {
  background: #fee2e2;
  color: #991b1b;
}

.btn-primary {
  padding: 0.6rem 1.4rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 999px;
  color: #ffffff;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(102, 126, 234, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 0.55rem 1.2rem;
  border-radius: 999px;
  border: 1px solid #cbd5f5;
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;
}

.btn-secondary:hover {
  background: #e2e8f0;
}

.empty {
  color: #94a3b8;
}

.empty-hint {
  margin-top: 0.75rem;
  font-size: 0.9rem;
  color: #64748b;
}

.empty-hint code {
  background: #f1f5f9;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-size: 0.85em;
}

@media (max-width: 992px) {
  .board-content {
    grid-template-columns: 1fr;
  }

  .job-detail {
    min-height: auto;
  }
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
  }

  .filters {
    padding: 1rem;
  }

  .filter-group input,
  .filter-group select {
    min-width: 180px;
  }

  .student-box__control {
    flex-direction: column;
  }

  .student-box__control input,
  .student-box__control button {
    width: 100%;
  }
}
</style>
