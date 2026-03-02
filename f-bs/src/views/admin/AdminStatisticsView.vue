<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  fetchAdminDashboard,
  type AdminDashboardResponse,
  type DailyStat,
} from '../../api/admin'

const dashboard = ref<AdminDashboardResponse | null>(null)
const loading = ref(false)
const error = ref('')

const loadDashboard = async () => {
  loading.value = true
  error.value = ''
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (!userInfoStr) throw new Error('请先登录')
    const userInfo = JSON.parse(userInfoStr)
    if (userInfo.role !== 'ADMIN') throw new Error('仅管理员可以访问此页面')
    dashboard.value = await fetchAdminDashboard(userInfo.id)
  } catch (err: any) {
    error.value = err.message || '加载数据失败'
  } finally {
    loading.value = false
  }
}

const roleLabel: Record<string, string> = {
  STUDENT: '学生',
  TEACHER: '教师',
  EMPLOYER: '企业',
  ADMIN: '管理员',
}

function barHeightPercent(items: DailyStat[]): (item: DailyStat) => number {
  const max = Math.max(1, ...items.map((s) => s.count || 0))
  return (item) => Math.max((item.count || 0) / max) * 100
}

function sampleLabels<T>(arr: T[], step: number): T[] {
  if (!arr?.length) return []
  return arr.filter((_, i) => i % step === 0)
}

const userBarHeight = computed(() => {
  const list = dashboard.value?.userStatistics?.dailyNewUsersLast30Days
  return list?.length ? barHeightPercent(list) : () => 0
})

const jobBarHeight = computed(() => {
  const list = dashboard.value?.contentStatistics?.dailyJobPostingsLast30Days
  return list?.length ? barHeightPercent(list) : () => 0
})

const appBarHeight = computed(() => {
  const list = dashboard.value?.contentStatistics?.dailyApplicationsLast30Days
  return list?.length ? barHeightPercent(list) : () => 0
})

const activityMax = computed(() => {
  const o = dashboard.value?.systemStatistics?.systemActivityByType
  return o && typeof o === 'object' ? Math.max(1, ...Object.values(o)) : 1
})

onMounted(() => {
  loadDashboard()
})
</script>

<template>
  <div class="admin-statistics">
    <header class="page-header">
      <h1>数据统计</h1>
      <p class="subtitle">用户活跃度、内容发布量、系统使用统计</p>
    </header>

    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <template v-else-if="dashboard">
      <!-- 核心指标：全局一览 -->
      <section class="stats-strip">
        <div class="strip-inner">
          <div class="stat-cell">
            <span class="stat-cell-icon">👥</span>
            <div class="stat-cell-body">
              <span class="stat-cell-value">{{ dashboard.header.totalUsers }}</span>
              <span class="stat-cell-label">总用户</span>
            </div>
          </div>
          <div class="stat-cell">
            <span class="stat-cell-icon">✅</span>
            <div class="stat-cell-body">
              <span class="stat-cell-value">{{ dashboard.header.activeUsers }}</span>
              <span class="stat-cell-label">活跃用户</span>
            </div>
          </div>
          <div class="stat-cell">
            <span class="stat-cell-icon">🎓</span>
            <div class="stat-cell-body">
              <span class="stat-cell-value">{{ dashboard.header.totalStudents }}</span>
              <span class="stat-cell-label">学生</span>
            </div>
          </div>
          <div class="stat-cell">
            <span class="stat-cell-icon">🏢</span>
            <div class="stat-cell-body">
              <span class="stat-cell-value">{{ dashboard.header.totalEmployers }}</span>
              <span class="stat-cell-label">企业</span>
            </div>
          </div>
          <div class="stat-cell">
            <span class="stat-cell-icon">👨‍🏫</span>
            <div class="stat-cell-body">
              <span class="stat-cell-value">{{ dashboard.header.totalTeachers }}</span>
              <span class="stat-cell-label">教师</span>
            </div>
          </div>
          <div class="stat-cell">
            <span class="stat-cell-icon">💼</span>
            <div class="stat-cell-body">
              <span class="stat-cell-value">{{ dashboard.header.activeJobPostings }}</span>
              <span class="stat-cell-label">活跃岗位</span>
            </div>
          </div>
          <div class="stat-cell">
            <span class="stat-cell-icon">📝</span>
            <div class="stat-cell-body">
              <span class="stat-cell-value">{{ dashboard.header.totalApplications }}</span>
              <span class="stat-cell-label">求职申请</span>
            </div>
          </div>
          <div class="stat-cell">
            <span class="stat-cell-icon">🔔</span>
            <div class="stat-cell-body">
              <span class="stat-cell-value">{{ dashboard.header.pendingNotifications }}</span>
              <span class="stat-cell-label">待处理通知</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 数据详情：两列 + 一列 -->
      <div class="detail-grid">
        <!-- 用户活跃度 -->
        <article class="panel">
          <h2 class="panel-title">用户活跃度统计</h2>
          <div class="panel-body">
            <div class="metric-row">
              <span class="metric-label">按角色活跃用户</span>
              <div class="tag-row">
                <span
                  v-for="(count, role) in dashboard.userStatistics?.activeUsersByRole"
                  :key="role"
                  class="tag"
                >
                  {{ roleLabel[role] ?? role }}: {{ count }}
                </span>
              </div>
            </div>
            <div v-if="dashboard.userStatistics?.dailyNewUsersLast30Days?.length" class="chart-block">
              <h3 class="chart-title">最近30天每日新增用户</h3>
              <div class="chart-bar-wrap">
                <div
                  v-for="stat in dashboard.userStatistics.dailyNewUsersLast30Days"
                  :key="stat.date"
                  class="chart-bar"
                  :style="{ '--h': userBarHeight(stat) + '%' }"
                  :title="`${stat.date}: ${stat.count}人`"
                >
                  <span v-if="stat.count > 0" class="chart-bar-num">{{ stat.count }}</span>
                </div>
              </div>
              <div class="chart-axis">
                <span
                  v-for="stat in sampleLabels(dashboard.userStatistics.dailyNewUsersLast30Days, 5)"
                  :key="stat.date"
                >
                  {{ stat.date ? stat.date.substring(5) : '' }}
                </span>
              </div>
            </div>
          </div>
        </article>

        <!-- 内容发布量 -->
        <article class="panel">
          <h2 class="panel-title">内容发布量统计</h2>
          <div class="panel-body">
            <div class="kpi-grid">
              <div class="kpi">
                <span class="kpi-label">近7天职位发布</span>
                <span class="kpi-value">{{ dashboard.contentStatistics?.jobPostingsLast7Days ?? 0 }}</span>
              </div>
              <div class="kpi">
                <span class="kpi-label">近7天求职申请</span>
                <span class="kpi-value">{{ dashboard.contentStatistics?.applicationsLast7Days ?? 0 }}</span>
              </div>
              <div class="kpi">
                <span class="kpi-label">近7天简历创建</span>
                <span class="kpi-value">{{ dashboard.contentStatistics?.resumesLast7Days ?? 0 }}</span>
              </div>
              <div class="kpi">
                <span class="kpi-label">近7天系统通知</span>
                <span class="kpi-value">{{ dashboard.contentStatistics?.notificationsLast7Days ?? 0 }}</span>
              </div>
            </div>
            <div v-if="dashboard.contentStatistics?.dailyJobPostingsLast30Days?.length" class="chart-block">
              <h3 class="chart-title">最近30天发布趋势</h3>
              <div class="trend-legend">
                <span class="legend-dot job"></span> 职位发布
                <span class="legend-dot app"></span> 求职申请
              </div>
              <div class="chart-bar-wrap dual">
                <div class="chart-bar-group">
                  <div
                    v-for="stat in dashboard.contentStatistics.dailyJobPostingsLast30Days"
                    :key="'j-' + stat.date"
                    class="chart-bar job"
                    :style="{ '--h': jobBarHeight(stat) + '%' }"
                    :title="`${stat.date}: ${stat.count}`"
                  />
                </div>
                <div class="chart-bar-group">
                  <div
                    v-for="stat in dashboard.contentStatistics.dailyApplicationsLast30Days"
                    :key="'a-' + stat.date"
                    class="chart-bar app"
                    :style="{ '--h': appBarHeight(stat) + '%' }"
                    :title="`${stat.date}: ${stat.count}`"
                  />
                </div>
              </div>
              <div class="chart-axis">
                <span
                  v-for="stat in sampleLabels(dashboard.contentStatistics.dailyJobPostingsLast30Days, 5)"
                  :key="stat.date"
                >
                  {{ stat.date ? stat.date.substring(5) : '' }}
                </span>
              </div>
            </div>
          </div>
        </article>

        <!-- 系统使用统计：通栏 -->
        <article class="panel panel-full">
          <h2 class="panel-title">系统使用统计</h2>
          <div class="panel-body">
            <div class="kpi-grid four">
              <div class="kpi">
                <span class="kpi-label">最近30天搜索次数</span>
                <span class="kpi-value">{{ dashboard.systemStatistics?.searchesLast30Days ?? 0 }}</span>
              </div>
              <div class="kpi">
                <span class="kpi-label">最近30天就业指导</span>
                <span class="kpi-value">{{ dashboard.systemStatistics?.guidanceRecordsLast30Days ?? 0 }}</span>
              </div>
              <div class="kpi">
                <span class="kpi-label">最近30天面试安排</span>
                <span class="kpi-value">{{ dashboard.systemStatistics?.interviewsLast30Days ?? 0 }}</span>
              </div>
              <div class="kpi">
                <span class="kpi-label">最近30天资源上传</span>
                <span class="kpi-value">{{ dashboard.systemStatistics?.publicResourcesLast30Days ?? 0 }}</span>
              </div>
            </div>
            <div
              v-if="dashboard.systemStatistics?.systemActivityByType && Object.keys(dashboard.systemStatistics.systemActivityByType).length"
              class="chart-block"
            >
              <h3 class="chart-title">系统活动类型分布</h3>
              <div class="progress-list">
                <div
                  v-for="(count, type) in dashboard.systemStatistics.systemActivityByType"
                  :key="type"
                  class="progress-row"
                >
                  <span class="progress-label">{{ type }}</span>
                  <div class="progress-track">
                    <div
                      class="progress-fill"
                      :style="{ width: `${(count / activityMax) * 100}%` }"
                    />
                  </div>
                  <span class="progress-value">{{ count }}</span>
                </div>
              </div>
            </div>
          </div>
        </article>
      </div>
    </template>
  </div>
</template>

<style scoped>
.admin-statistics {
  --accent: #0071e3;
  --accent2: #5e5ce6;
  --accent3: #34c759;
  --surface: rgba(255, 255, 255, 0.92);
  --border: rgba(0, 0, 0, 0.06);
  --muted: #86868b;
  --text: #1d1d1f;
  max-width: 1600px;
  margin: 0 auto;
  padding: 2rem 2.5rem 4rem;
  min-height: calc(100vh - 200px);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
  background:
    radial-gradient(circle at 0% 0%, rgba(0, 113, 227, 0.04) 0%, transparent 50%),
    radial-gradient(circle at 100% 100%, rgba(94, 92, 230, 0.04) 0%, transparent 50%),
    linear-gradient(180deg, #f8f9fa 0%, #fff 100%);
}

.page-header {
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--border);
  position: relative;
}
.page-header::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 48px;
  height: 3px;
  background: linear-gradient(90deg, var(--accent), var(--accent2));
  border-radius: 0 0 3px 3px;
}
.page-header h1 {
  font-size: 2.25rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--text);
  margin: 0 0 0.5rem 0;
}
.subtitle {
  color: var(--muted);
  font-size: 1rem;
  margin: 0;
}

.loading-state,
.error-state {
  text-align: center;
  padding: 4rem 2rem;
  color: var(--muted);
  font-size: 1.125rem;
}
.error-state {
  color: #ff3b30;
}

/* 核心指标条：全局一览 */
.stats-strip {
  margin-bottom: 2rem;
}
.strip-inner {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 1rem;
  background: var(--surface);
  backdrop-filter: saturate(180%) blur(24px);
  border-radius: 16px;
  padding: 1.25rem 1.5rem;
  border: 1px solid var(--border);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.05);
}
.stat-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
}
.stat-cell-icon {
  font-size: 1.75rem;
  line-height: 1;
  opacity: 0.9;
}
.stat-cell-body {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}
.stat-cell-value {
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--text);
  line-height: 1.2;
}
.stat-cell-label {
  font-size: 0.8125rem;
  color: var(--muted);
  font-weight: 500;
}

/* 详情网格 */
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}
.panel-full {
  grid-column: 1 / -1;
}

.panel {
  background: var(--surface);
  backdrop-filter: saturate(180%) blur(24px);
  border-radius: 16px;
  border: 1px solid var(--border);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}
.panel-title {
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--text);
  margin: 0;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--border);
  position: relative;
  padding-left: 1.25rem;
}
.panel-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: linear-gradient(180deg, var(--accent), var(--accent2));
  border-radius: 2px;
}
.panel-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.metric-row {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.metric-label {
  font-size: 0.875rem;
  color: var(--muted);
  font-weight: 500;
}
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.tag {
  background: linear-gradient(135deg, rgba(0, 113, 227, 0.08), rgba(94, 92, 230, 0.08));
  color: var(--accent);
  padding: 0.5rem 1rem;
  border-radius: 10px;
  font-size: 0.8125rem;
  font-weight: 600;
  border: 1px solid rgba(0, 113, 227, 0.15);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}
.kpi-grid.four {
  grid-template-columns: repeat(4, 1fr);
}
.kpi {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  padding: 1rem 1.25rem;
  background: rgba(0, 113, 227, 0.04);
  border-radius: 12px;
  border: 1px solid rgba(0, 113, 227, 0.1);
}
.kpi-label {
  font-size: 0.8125rem;
  color: var(--muted);
  font-weight: 500;
}
.kpi-value {
  font-size: 1.75rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--accent);
}

.chart-block {
  margin-top: 0.25rem;
}
.chart-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 1rem 0;
}
.chart-bar-wrap {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 2px;
  height: 180px;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--border);
}
.chart-bar-wrap.dual {
  gap: 1rem;
}
.chart-bar-group {
  flex: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 2px;
  height: 100%;
}
.chart-bar {
  flex: 1;
  min-width: 4px;
  height: var(--h, 5%);
  min-height: 4px;
  background: linear-gradient(180deg, var(--accent), var(--accent2));
  border-radius: 4px 4px 0 0;
  position: relative;
  transition: transform 0.2s ease;
}
.chart-bar:hover {
  transform: scaleY(1.02);
}
.chart-bar.job {
  background: linear-gradient(180deg, var(--accent), var(--accent2));
}
.chart-bar.app {
  background: linear-gradient(180deg, var(--accent3), #30d158);
}
.chart-bar-num {
  position: absolute;
  top: -1.25rem;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.6875rem;
  color: var(--muted);
  font-weight: 500;
  white-space: nowrap;
}
.chart-axis {
  display: flex;
  justify-content: space-between;
  padding-top: 0.5rem;
  font-size: 0.6875rem;
  color: var(--muted);
}
.trend-legend {
  font-size: 0.8125rem;
  color: var(--muted);
  margin-bottom: 0.75rem;
  display: flex;
  gap: 1rem;
}
.legend-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  vertical-align: middle;
  margin-right: 0.25rem;
}
.legend-dot.job { background: var(--accent); }
.legend-dot.app { background: var(--accent3); }

.progress-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.progress-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.progress-label {
  min-width: 120px;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text);
}
.progress-track {
  flex: 1;
  height: 20px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 10px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent), var(--accent2));
  border-radius: 10px;
  transition: width 0.5s ease;
}
.progress-value {
  min-width: 48px;
  text-align: right;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text);
}

@media (max-width: 1400px) {
  .strip-inner {
    grid-template-columns: repeat(4, 1fr);
  }
  .kpi-grid.four {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1024px) {
  .admin-statistics {
    padding: 1.5rem 1.25rem 3rem;
  }
  .detail-grid {
    grid-template-columns: 1fr;
  }
  .strip-inner {
    grid-template-columns: repeat(2, 1fr);
  }
  .kpi-grid {
    grid-template-columns: 1fr;
  }
  .kpi-grid.four {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .strip-inner {
    grid-template-columns: 1fr;
  }
  .kpi-grid.four {
    grid-template-columns: 1fr;
  }
  .chart-bar-wrap {
    height: 140px;
  }
}
</style>
