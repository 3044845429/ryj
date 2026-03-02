<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getTeacherByUserId } from '@/api/teacher'

type TeacherRecord = {
  id: number
  userId: number
  department: string | null
  email: string | null
  phone: string | null
}

type EmploymentStatistics = {
  overview: {
    totalStudents: number
    employedStudents: number
    activelyApplying: number
    notStarted: number
    employmentRate: number
    totalApplications: number
    totalInterviews: number
    offersReceived: number
  }
  industryDistribution: Array<{
    industry: string
    count: number
    percentage: number
  }>
  locationDistribution: Array<{
    location: string
    count: number
    percentage: number
  }>
  salaryDistribution: {
    below5k: number
    range5to8k: number
    range8to12k: number
    range12to20k: number
    above20k: number
    averageSalary: number
    medianSalary: number
  }
  majorEmploymentRates: Array<{
    major: string
    totalStudents: number
    employedStudents: number
    employmentRate: number
  }>
  monthlyTrends: Array<{
    month: string
    applications: number
    interviews: number
    offers: number
  }>
}

const teacherRecord = ref<TeacherRecord | null>(null)
const statistics = ref<EmploymentStatistics | null>(null)
const loading = ref(true)
const scope = ref<'guided' | 'all'>('guided')

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

const loadStatistics = async () => {
  loading.value = true
  try {
    const response = await fetch(
      `http://localhost:8080/api/teachers/${teacherRecord.value?.id}/statistics/employment?scope=${scope.value}`
    )
    const result = await response.json()
    if (result.code === 200) {
      statistics.value = result.data
    } else {
      showMessage('error', result.message || '数据加载失败')
    }
  } catch (error: any) {
    showMessage('error', error.message || '数据加载失败')
  } finally {
    loading.value = false
  }
}

const changeScopeAndLoad = async (newScope: 'guided' | 'all') => {
  scope.value = newScope
  await loadStatistics()
}

const formatPercent = (value: number) => {
  return value.toFixed(1) + '%'
}

const formatSalary = (value: number) => {
  return (value / 1000).toFixed(1) + 'k'
}

onMounted(async () => {
  const stored = localStorage.getItem('userInfo')
  if (!stored) {
    showMessage('error', '请先登录')
    loading.value = false
    return
  }

  try {
    const userInfo = JSON.parse(stored)
    teacherRecord.value = await getTeacherByUserId(userInfo.id)
    await loadStatistics()
  } catch (error: any) {
    showMessage('error', error.message || '初始化失败')
    loading.value = false
  }
})

const salaryChartData = computed(() => {
  if (!statistics.value) return []
  const dist = statistics.value.salaryDistribution
  return [
    { label: '<5k', value: dist.below5k },
    { label: '5-8k', value: dist.range5to8k },
    { label: '8-12k', value: dist.range8to12k },
    { label: '12-20k', value: dist.range12to20k },
    { label: '>20k', value: dist.above20k }
  ]
})

const maxSalaryCount = computed(() => {
  return Math.max(...salaryChartData.value.map((d) => d.value), 1)
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
            <h1>就业统计分析</h1>
            <p class="subtitle">结对学生或全部学生的就业数据概览</p>
          </div>
          <div class="scope-toggle">
            <button
              type="button"
              :class="{ active: scope === 'guided' }"
              :disabled="loading"
              @click="changeScopeAndLoad('guided')"
            >
              结对学生
            </button>
            <button
              type="button"
              :class="{ active: scope === 'all' }"
              :disabled="loading"
              @click="changeScopeAndLoad('all')"
            >
              全部学生
            </button>
          </div>
        </header>

        <transition name="fade">
          <div v-if="message" class="stats-message" :class="message.type">{{ message.text }}</div>
        </transition>

        <div v-if="loading" class="teacher-loading">
          <div class="spinner" />
          加载统计数据中…
        </div>

        <template v-else-if="statistics">
      <!-- 总览统计 -->
      <section class="teacher-card overview-section">
        <h2 class="card-title">总览数据</h2>
        <div class="stats-grid">
          <div class="stat-card primary">
            <span class="stat-label">总学生数</span>
            <strong class="stat-value">{{ statistics.overview.totalStudents }}</strong>
          </div>
          <div class="stat-card success">
            <span class="stat-label">已就业</span>
            <strong class="stat-value">{{ statistics.overview.employedStudents }}</strong>
          </div>
          <div class="stat-card warning">
            <span class="stat-label">求职中</span>
            <strong class="stat-value">{{ statistics.overview.activelyApplying }}</strong>
          </div>
          <div class="stat-card muted">
            <span class="stat-label">未开始</span>
            <strong class="stat-value">{{ statistics.overview.notStarted }}</strong>
          </div>
          <div class="stat-card accent">
            <span class="stat-label">就业率</span>
            <strong class="stat-value">{{ formatPercent(statistics.overview.employmentRate) }}</strong>
          </div>
          <div class="stat-card info">
            <span class="stat-label">申请总数</span>
            <strong class="stat-value">{{ statistics.overview.totalApplications }}</strong>
          </div>
          <div class="stat-card secondary">
            <span class="stat-label">面试总数</span>
            <strong class="stat-value">{{ statistics.overview.totalInterviews }}</strong>
          </div>
          <div class="stat-card success">
            <span class="stat-label">Offer数</span>
            <strong class="stat-value">{{ statistics.overview.offersReceived }}</strong>
          </div>
        </div>
      </section>

      <!-- 公司分布 -->
      <section class="teacher-card chart-section">
        <h2 class="card-title">就业公司分布（Top 10）</h2>
        <div v-if="statistics.industryDistribution.length === 0" class="empty-chart">
          暂无公司分布数据
        </div>
        <div v-else class="industry-list">
          <div
            v-for="item in statistics.industryDistribution.slice(0, 10)"
            :key="item.industry"
            class="industry-item"
          >
            <span class="industry-name">{{ item.industry }}</span>
            <div class="industry-bar-wrapper">
              <div class="industry-bar" :style="{ width: formatPercent(item.percentage) }"></div>
            </div>
            <span class="industry-count">{{ item.count }}人 ({{ formatPercent(item.percentage) }})</span>
          </div>
        </div>
      </section>

      <!-- 地区分布 -->
      <section class="teacher-card chart-section">
        <h2 class="card-title">地区分布</h2>
        <div v-if="statistics.locationDistribution.length === 0" class="empty-chart">
          暂无地区分布数据
        </div>
        <div v-else class="location-grid">
          <div
            v-for="item in statistics.locationDistribution.slice(0, 12)"
            :key="item.location"
            class="location-card"
          >
            <strong>{{ item.location }}</strong>
            <span class="location-count">{{ item.count }}人</span>
            <span class="location-percent">{{ formatPercent(item.percentage) }}</span>
          </div>
        </div>
      </section>

      <!-- 薪资分布 -->
      <section class="teacher-card chart-section">
        <h2 class="card-title">薪资分布</h2>
        <div class="salary-stats">
          <div class="salary-summary">
            <div class="summary-item">
              <span>平均薪资</span>
              <strong>{{ formatSalary(statistics.salaryDistribution.averageSalary) }}</strong>
            </div>
            <div class="summary-item">
              <span>中位数</span>
              <strong>{{ formatSalary(statistics.salaryDistribution.medianSalary) }}</strong>
            </div>
          </div>
          <div class="salary-chart">
            <div v-for="item in salaryChartData" :key="item.label" class="salary-bar">
              <div
                class="bar-fill"
                :style="{ height: (item.value / maxSalaryCount) * 100 + '%' }"
              ></div>
              <span class="bar-value">{{ item.value }}</span>
              <span class="bar-label">{{ item.label }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 专业就业率 -->
      <section class="teacher-card chart-section">
        <h2 class="card-title">专业就业率</h2>
        <div v-if="statistics.majorEmploymentRates.length === 0" class="empty-chart">
          暂无专业就业率数据
        </div>
        <div v-else class="major-list">
          <div v-for="item in statistics.majorEmploymentRates" :key="item.major" class="major-item">
            <span class="major-name">{{ item.major }}</span>
            <span class="major-students">{{ item.employedStudents }}/{{ item.totalStudents }}</span>
            <div class="major-bar-wrapper">
              <div class="major-bar" :style="{ width: formatPercent(item.employmentRate) }"></div>
            </div>
            <span class="major-rate">{{ formatPercent(item.employmentRate) }}</span>
          </div>
        </div>
      </section>

      <!-- 月度趋势 -->
      <section class="teacher-card chart-section">
        <h2 class="card-title">月度趋势</h2>
        <div v-if="statistics.monthlyTrends.length === 0" class="empty-chart">
          暂无月度趋势数据
        </div>
        <div v-else class="trends-table">
          <table>
            <thead>
              <tr>
                <th>月份</th>
                <th>申请数</th>
                <th>面试数</th>
                <th>Offer数</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="trend in statistics.monthlyTrends" :key="trend.month">
                <td>{{ trend.month }}</td>
                <td>{{ trend.applications }}</td>
                <td>{{ trend.interviews }}</td>
                <td>{{ trend.offers }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
        </template>
      </div>
    </main>
  </div>
</template>

<style src="@/assets/teacher-layout.css"></style>
<style scoped>
.scope-toggle {
  display: flex;
  gap: 4px;
  background: #e8e8ed;
  padding: 4px;
  border-radius: 8px;
}
.scope-toggle button {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 0.9375rem;
  font-weight: 500;
  color: #86868b;
  background: transparent;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.scope-toggle button.active {
  background: #fff;
  color: #0071e3;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}
.scope-toggle button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.stats-message {
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 0.9375rem;
  font-weight: 500;
}
.stats-message.success { background: rgba(52, 199, 89, 0.12); color: #248a3d; }
.stats-message.error { background: rgba(255, 59, 48, 0.12); color: #d70015; }

.overview-section { margin-bottom: 24px; }
.overview-section .card-title { margin-bottom: 20px; }
.chart-section { margin-bottom: 24px; }
.chart-section .card-title { margin-bottom: 16px; font-size: 1.125rem; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1.25rem;
}

.stat-card {
  padding: 1.5rem;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.stat-card {
  padding: 20px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #f5f5f7;
  border: 1px solid #e8e8ed;
}
.stat-card.primary { background: rgba(0, 113, 227, 0.08); border-color: rgba(0, 113, 227, 0.2); }
.stat-card.success { background: rgba(52, 199, 89, 0.08); border-color: rgba(52, 199, 89, 0.2); }
.stat-card.warning { background: rgba(255, 149, 0, 0.08); border-color: rgba(255, 149, 0, 0.2); }
.stat-card.muted { background: #f5f5f7; }
.stat-card.accent { background: rgba(88, 86, 214, 0.08); border-color: rgba(88, 86, 214, 0.2); }
.stat-card.info { background: rgba(0, 122, 255, 0.08); border-color: rgba(0, 122, 255, 0.2); }
.stat-card.secondary { background: rgba(255, 45, 85, 0.08); border-color: rgba(255, 45, 85, 0.2); }
.stat-label { font-size: 0.875rem; color: #86868b; font-weight: 500; }
.stat-value { font-size: 1.75rem; color: #1d1d1f; font-weight: 600; letter-spacing: -0.02em; }

.empty-chart {
  text-align: center;
  padding: 3rem;
  color: #94a3b8;
  font-size: 1rem;
}

.industry-list,
.major-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.industry-item,
.major-item {
  display: grid;
  grid-template-columns: 150px 1fr auto;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.5);
}

.industry-name,
.major-name {
  font-weight: 600;
  color: #334155;
}

.industry-bar-wrapper,
.major-bar-wrapper {
  height: 24px;
  background: rgba(226, 232, 240, 0.6);
  border-radius: 999px;
  overflow: hidden;
}

.industry-bar,
.major-bar {
  height: 100%;
  background: #0071e3;
  border-radius: 999px;
  transition: width 0.5s ease;
}

.industry-count,
.major-rate {
  font-size: 0.95rem;
  color: #64748b;
  font-weight: 600;
  min-width: 120px;
  text-align: right;
}

.major-students {
  font-size: 0.9rem;
  color: #94a3b8;
}

.location-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 1rem;
}

.location-card {
  padding: 16px;
  border-radius: 10px;
  background: rgba(0, 113, 227, 0.06);
  border: 1px solid rgba(0, 113, 227, 0.12);
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: center;
}
.location-card strong { font-size: 1rem; color: #1d1d1f; }
.location-count { font-size: 1.25rem; font-weight: 600; color: #0071e3; }

.location-percent {
  font-size: 0.85rem;
  color: #64748b;
}

.salary-stats {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.salary-summary {
  display: flex;
  gap: 2rem;
  justify-content: center;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem 2rem;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.08), rgba(5, 150, 105, 0.12));
}

.summary-item span {
  font-size: 0.9rem;
  color: #64748b;
  font-weight: 600;
}

.summary-item strong {
  font-size: 2rem;
  color: #059669;
  font-weight: 700;
}

.salary-chart {
  display: flex;
  gap: 1.5rem;
  justify-content: space-around;
  align-items: flex-end;
  height: 240px;
  padding: 1rem 2rem;
  background: rgba(248, 250, 252, 0.5);
  border-radius: 16px;
}

.salary-bar {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  height: 100%;
  justify-content: flex-end;
}

.bar-fill {
  width: 100%;
  max-width: 56px;
  background: #0071e3;
  border-radius: 6px 6px 0 0;
  transition: height 0.5s ease;
}

.bar-value {
  font-weight: 700;
  color: #1e293b;
  font-size: 0.95rem;
}

.bar-label {
  font-size: 0.85rem;
  color: #64748b;
  font-weight: 600;
  margin-top: 0.5rem;
}

.trends-table {
  overflow-x: auto;
}

.trends-table table {
  width: 100%;
  border-collapse: collapse;
}

.trends-table th,
.trends-table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
}

.trends-table th {
  background: rgba(248, 250, 252, 0.8);
  font-weight: 600;
  color: #334155;
}

.trends-table td {
  color: #64748b;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

