<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchAdminDashboard,
  type AdminDashboardResponse,
  type AdminModuleInfo,
} from '../../api/admin'

const router = useRouter()
const dashboard = ref<AdminDashboardResponse | null>(null)
const loading = ref(false)
const error = ref('')

const loadDashboard = async () => {
  loading.value = true
  error.value = ''
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (!userInfoStr) {
      throw new Error('请先登录')
    }
    const userInfo = JSON.parse(userInfoStr)
    if (userInfo.role !== 'ADMIN') {
      throw new Error('仅管理员可以访问此页面')
    }
    dashboard.value = await fetchAdminDashboard(userInfo.id)
  } catch (err: any) {
    error.value = err.message || '加载数据失败'
  } finally {
    loading.value = false
  }
}

const navigateToModule = (moduleKey: string) => {
  router.push(`/admin/${moduleKey}`)
}

onMounted(() => {
  loadDashboard()
})
</script>

<template>
  <div class="admin-overview">
    <div class="page-header">
      <h1>管理员总览</h1>
      <p class="subtitle">系统管理与数据监控中心</p>
    </div>

    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <div v-else-if="dashboard" class="dashboard-content">
      <!-- 统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon">👥</div>
          <div class="stat-content">
            <h3>{{ dashboard.header.totalUsers }}</h3>
            <p>总用户数</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">✅</div>
          <div class="stat-content">
            <h3>{{ dashboard.header.activeUsers }}</h3>
            <p>活跃用户</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🎓</div>
          <div class="stat-content">
            <h3>{{ dashboard.header.totalStudents }}</h3>
            <p>学生用户</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🏢</div>
          <div class="stat-content">
            <h3>{{ dashboard.header.totalEmployers }}</h3>
            <p>企业用户</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">👨‍🏫</div>
          <div class="stat-content">
            <h3>{{ dashboard.header.totalTeachers }}</h3>
            <p>教师用户</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">💼</div>
          <div class="stat-content">
            <h3>{{ dashboard.header.activeJobPostings }}</h3>
            <p>活跃岗位</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">📝</div>
          <div class="stat-content">
            <h3>{{ dashboard.header.totalApplications }}</h3>
            <p>求职申请</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🔔</div>
          <div class="stat-content">
            <h3>{{ dashboard.header.pendingNotifications }}</h3>
            <p>待处理通知</p>
          </div>
        </div>
      </div>

      <!-- 模块导航 -->
      <div class="modules-section">
        <h2>功能模块</h2>
        <div class="modules-grid">
          <div
            v-for="module in dashboard.modules"
            :key="module.key"
            class="module-card"
            @click="navigateToModule(module.key)"
          >
            <h3>{{ module.name }}</h3>
            <p>{{ module.description }}</p>
            <div class="capabilities">
              <span v-for="cap in module.capabilities" :key="cap" class="cap-tag">{{ cap }}</span>
            </div>
          </div>
        </div>
      </div>


      <!-- 最近用户 -->
      <div class="recent-section">
        <h2>最近注册用户</h2>
        <div class="recent-users">
          <div v-for="user in dashboard.recentUsers" :key="user.id" class="user-item">
            <div class="user-info">
              <strong>{{ user.fullName || user.username }}</strong>
              <span class="user-role">{{ user.role }}</span>
            </div>
            <div class="user-meta">
              <span>{{ user.email || '无邮箱' }}</span>
              <span class="user-status" :class="user.status.toLowerCase()">{{ user.status }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-overview {
  max-width: 1600px;
  margin: 0 auto;
  padding: 3rem 4rem;
  min-height: calc(100vh - 200px);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
  background: 
    radial-gradient(circle at 0% 0%, rgba(0, 113, 227, 0.02) 0%, transparent 50%),
    radial-gradient(circle at 100% 100%, rgba(94, 92, 230, 0.02) 0%, transparent 50%),
    linear-gradient(180deg, #fafafa 0%, #ffffff 100%);
}

.page-header {
  margin-bottom: 3.5rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  position: relative;
}

.page-header::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, #0071e3, #5e5ce6, #af52de);
  border-radius: 0 0 3px 3px;
  box-shadow: 0 2px 6px rgba(0, 113, 227, 0.25);
}

.page-header h1 {
  font-size: 2.75rem;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: #1d1d1f;
  margin: 0 0 0.875rem 0;
  line-height: 1.1;
  background: linear-gradient(135deg, #1d1d1f 0%, #424245 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  color: #86868b;
  font-size: 1.125rem;
  font-weight: 400;
  margin: 0;
  letter-spacing: -0.01em;
  line-height: 1.5;
}

.loading-state,
.error-state {
  text-align: center;
  padding: 4rem 2rem;
  color: #86868b;
  font-size: 1.125rem;
}

.error-state {
  color: #ff3b30;
}

.dashboard-content {
  animation: fadeIn 0.6s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 统计卡片 - 苹果风格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.5rem;
  margin-bottom: 4rem;
}

.stat-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: saturate(180%) blur(30px);
  -webkit-backdrop-filter: saturate(180%) blur(30px);
  border-radius: 20px;
  padding: 2rem 1.75rem;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.06),
    0 2px 8px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  gap: 1.25rem;
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
  overflow: hidden;
  cursor: default;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #0071e3, #5e5ce6, #af52de, #ff2d55);
  opacity: 0;
  transition: opacity 0.5s ease;
  border-radius: 24px 24px 0 0;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 
    0 12px 40px rgba(0, 0, 0, 0.1),
    0 4px 16px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  border-color: rgba(0, 113, 227, 0.2);
}

.stat-card:hover::before {
  opacity: 1;
}

.stat-icon {
  font-size: 2.5rem;
  line-height: 1;
  filter: grayscale(0.1);
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  opacity: 0.9;
  flex-shrink: 0;
}

.stat-card:hover .stat-icon {
  transform: scale(1.1) rotate(5deg);
  opacity: 1;
  filter: grayscale(0);
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-content h3 {
  font-size: 2.5rem;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: #1d1d1f;
  margin: 0 0 0.5rem 0;
  line-height: 1;
  background: linear-gradient(135deg, #1d1d1f 0%, #424245 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-content p {
  color: #86868b;
  font-size: 0.9375rem;
  font-weight: 500;
  margin: 0;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

/* 模块导航区域 */
.modules-section {
  margin-bottom: 4rem;
}

.modules-section h2 {
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: #1d1d1f;
  margin: 0 0 2rem 0;
  position: relative;
  padding-left: 1.25rem;
}

.modules-section h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: linear-gradient(180deg, #0071e3, #5e5ce6);
  border-radius: 2px;
}

.modules-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
}

.module-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(30px);
  -webkit-backdrop-filter: saturate(180%) blur(30px);
  border-radius: 20px;
  padding: 2rem;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.06),
    0 2px 8px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
  overflow: hidden;
}

.module-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(0, 113, 227, 0.05), rgba(94, 92, 230, 0.05));
  opacity: 0;
  transition: opacity 0.5s ease;
}

.module-card:hover {
  transform: translateY(-4px);
  box-shadow: 
    0 16px 48px rgba(0, 0, 0, 0.1),
    0 8px 24px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  border-color: rgba(0, 113, 227, 0.25);
}

.module-card:hover::after {
  opacity: 1;
}

.module-card h3 {
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #1d1d1f;
  margin: 0 0 0.75rem 0;
  position: relative;
  z-index: 1;
  line-height: 1.3;
}

.module-card p {
  color: #86868b;
  font-size: 0.9375rem;
  line-height: 1.6;
  margin: 0 0 1.5rem 0;
  position: relative;
  z-index: 1;
  font-weight: 400;
}

.capabilities {
  display: flex;
  flex-wrap: wrap;
  gap: 0.625rem;
  position: relative;
  z-index: 1;
}

.cap-tag {
  background: linear-gradient(135deg, rgba(0, 113, 227, 0.08), rgba(94, 92, 230, 0.08));
  color: #0071e3;
  padding: 0.5rem 1rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: -0.01em;
  border: 1px solid rgba(0, 113, 227, 0.12);
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  backdrop-filter: blur(10px);
}

.module-card:hover .cap-tag {
  background: linear-gradient(135deg, rgba(0, 113, 227, 0.12), rgba(94, 92, 230, 0.12));
  border-color: rgba(0, 113, 227, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 113, 227, 0.15);
}

/* 最近用户区域 */
.recent-section {
  margin-bottom: 3rem;
}

.recent-section h2 {
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #1d1d1f;
  margin: 0 0 1.5rem 0;
}

.recent-users {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(30px);
  -webkit-backdrop-filter: saturate(180%) blur(30px);
  border-radius: 20px;
  padding: 0;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.06),
    0 2px 8px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  overflow: hidden;
}

.user-item {
  padding: 1.25rem 1.75rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.user-item:last-child {
  border-bottom: none;
}

.user-item:hover {
  background: rgba(0, 113, 227, 0.02);
  padding-left: 2rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.user-info strong {
  color: #1d1d1f;
  font-weight: 600;
  font-size: 1rem;
  letter-spacing: -0.01em;
}

.user-role {
  background: linear-gradient(135deg, rgba(0, 113, 227, 0.1), rgba(94, 92, 230, 0.1));
  color: #0071e3;
  padding: 0.375rem 1rem;
  border-radius: 12px;
  font-size: 0.8125rem;
  font-weight: 600;
  letter-spacing: -0.01em;
  border: 1px solid rgba(0, 113, 227, 0.15);
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  color: #86868b;
  font-size: 0.9375rem;
}

.user-status {
  padding: 0.375rem 1rem;
  border-radius: 12px;
  font-size: 0.8125rem;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.user-status.active {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.1), rgba(48, 209, 88, 0.1));
  color: #34c759;
  border: 1px solid rgba(52, 199, 89, 0.2);
}

.user-status.disabled {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.1), rgba(255, 45, 85, 0.1));
  color: #ff3b30;
  border: 1px solid rgba(255, 59, 48, 0.2);
}

/* 响应式设计 */
@media (max-width: 1600px) {
  .admin-overview {
    padding: 4rem 4rem;
  }
  
  .stats-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 1.5rem;
  }
  
  .modules-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 1.5rem;
  }
}

@media (max-width: 1400px) {
  .admin-overview {
    padding: 3rem 3rem;
  }
  
  .stats-grid {
    grid-template-columns: repeat(4, 1fr);
  }
  
  .modules-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1024px) {
  .admin-overview {
    padding: 2.5rem 2rem;
  }
  
  .page-header h1 {
    font-size: 3rem;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .modules-grid {
    grid-template-columns: 1fr;
  }
}

</style>

