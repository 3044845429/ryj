<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getUnreadNotificationCount, getUserNotifications, markAllNotificationsAsRead } from '@/api/notification'
import type { SystemNotification } from '@/api/notification'

const router = useRouter()
const route = useRoute()
const showUserMenu = ref(false)
const userMenuRef = ref<HTMLElement | null>(null)
const showNoticePopover = ref(false)
const noticeRef = ref<HTMLElement | null>(null)
const noticeList = ref<SystemNotification[]>([])
const noticeLoading = ref(false)
const isLoggedIn = ref(false)
const currentUser = ref<{ id?: number; userId?: number; username?: string; fullName?: string; role?: string } | null>(null)
const showLoginDialog = ref(false)
const targetRoute = ref('')
const unreadCount = ref(0)
const currentRole = computed(() => currentUser.value?.role ?? null)
const isStudentRole = computed(() => currentRole.value === 'STUDENT')
const isEmployerRole = computed(() => currentRole.value === 'EMPLOYER')
const isTeacherRole = computed(() => currentRole.value === 'TEACHER')
const isAdminRole = computed(() => currentRole.value === 'ADMIN')

// 检查登录状态
const checkLoginStatus = () => {
  const userInfo = localStorage.getItem('userInfo')
  const token = localStorage.getItem('token')
  isLoggedIn.value = !!(userInfo && token)
  
  if (userInfo) {
    try {
      currentUser.value = JSON.parse(userInfo)
      loadUnreadCount()
    } catch (e) {
      currentUser.value = null
      unreadCount.value = 0
    }
  } else {
    currentUser.value = null
    unreadCount.value = 0
  }
}

// 加载未读消息数（已登录时）
async function loadUnreadCount() {
  const id = currentUser.value?.id ?? currentUser.value?.userId
  if (id == null) {
    unreadCount.value = 0
    return
  }
  try {
    unreadCount.value = await getUnreadNotificationCount(Number(id))
  } catch {
    unreadCount.value = 0
  }
}

// 从通知页返回时刷新未读数；路由变化时重新检查登录并刷新未读数（解决切换用户后角标不更新）
watch(() => route.path, (newPath, oldPath) => {
  if (oldPath === '/notifications' && newPath !== '/notifications' && isLoggedIn.value) {
    loadUnreadCount()
  }
  // 每次路由变化都重新读一次登录状态与未读数，确保切换为教师后能看到待处理提示
  checkLoginStatus()
})

// 点击消息铃铛：弹出待处理信息列表，不跳转；看完后自动清除待处理提示
async function toggleNoticePopover() {
  showNoticePopover.value = !showNoticePopover.value
  if (showNoticePopover.value && currentUser.value != null) {
    const id = currentUser.value.id ?? currentUser.value.userId
    if (id != null) {
      noticeLoading.value = true
      noticeList.value = []
      try {
        const res = await getUserNotifications(Number(id), {
          page: 1,
          size: 50,
          unreadOnly: true,
        })
        noticeList.value = res.records || []
        // 用户查看了待处理消息后，将其标记为已读，清除角标提示
        if (noticeList.value.length > 0) {
          await markAllNotificationsAsRead(Number(id))
          unreadCount.value = 0
        }
      } catch {
        noticeList.value = []
      } finally {
        noticeLoading.value = false
      }
    }
  }
}

function closeNoticePopover() {
  showNoticePopover.value = false
}

// 分类中文
function noticeCategoryLabel(cat: string) {
  const map: Record<string, string> = {
    SYSTEM: '系统',
    INTERVIEW: '面试',
    APPLICATION: '申请',
    GUIDANCE: '就业指导',
  }
  return map[cat] ?? cat
}

// 点击某一条待处理信息时，快速跳转到相应处理页面
function handleNoticeItemClick(item: SystemNotification) {
  const role = currentRole.value
  const title = item.title || ''
  const category = item.category

  // 教师：档案审核与就业指导，优先跳到教师端相关页面
  if (role === 'TEACHER') {
    if (category === 'APPLICATION' && title.includes('档案')) {
      router.push('/teacher/approvals')
    } else if (category === 'GUIDANCE') {
      router.push('/teacher/guidance')
    } else if (category === 'INTERVIEW' || category === 'APPLICATION') {
      // 面试 / 申请类通知，跳到教师仪表板查看整体情况
      router.push('/teacher/overview')
    } else {
      router.push('/teacher/overview')
    }
    closeNoticePopover()
    return
  }

  // 学生：求职申请、面试、就业指导
  if (role === 'STUDENT') {
    if (category === 'INTERVIEW') {
      router.push('/student/interviews')
    } else if (category === 'APPLICATION') {
      router.push('/student/applications')
    } else if (category === 'GUIDANCE') {
      router.push('/student/guidance')
    } else {
      router.push('/student/overview')
    }
    closeNoticePopover()
    return
  }

  // 企业用户：与投递 / 面试相关的提示
  if (role === 'EMPLOYER') {
    if (category === 'INTERVIEW') {
      router.push('/employer/interviews')
    } else if (category === 'APPLICATION') {
      router.push('/employer/applications')
    } else {
      router.push('/employer/overview')
    }
    closeNoticePopover()
    return
  }

  // 其他角色或未识别时，退回首页或资讯动态
  if (category === 'SYSTEM') {
    router.push('/notifications')
  } else {
    router.push('/home')
  }
  closeNoticePopover()
}

// 获取显示名称
const displayName = computed(() => {
  if (!isLoggedIn.value || !currentUser.value) {
    return '用户'
  }
  return currentUser.value.fullName || currentUser.value.username || '用户'
})

// 切换用户菜单显示/隐藏
const toggleUserMenu = () => {
  checkLoginStatus() // 每次打开菜单时检查登录状态
  showUserMenu.value = !showUserMenu.value
}

// 关闭用户菜单
const closeUserMenu = () => {
  showUserMenu.value = false
}

// 点击外部关闭菜单
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as Node | null
  if (userMenuRef.value && target && !userMenuRef.value.contains(target)) {
    closeUserMenu()
  }
  if (noticeRef.value && target && !noticeRef.value.contains(target)) {
    closeNoticePopover()
  }
}

// 登录
const login = () => {
  closeUserMenu()
  router.push('/login')
}

// 切换用户
const switchUser = () => {
  closeUserMenu()
  // 清除当前用户信息
  localStorage.removeItem('userInfo')
  localStorage.removeItem('token')
  isLoggedIn.value = false
  currentUser.value = null
  // 跳转到登录页面
  router.push('/login')
}

// 退出登录
const logout = () => {
  closeUserMenu()
  // 清除用户信息
  localStorage.removeItem('userInfo')
  localStorage.removeItem('token')
  isLoggedIn.value = false
  currentUser.value = null
  // 跳转到首页
  router.push('/home')
}

// 显示登录对话框
const openLoginDialog = (target: string = '') => {
  targetRoute.value = target
  showLoginDialog.value = true
}

// 关闭登录对话框
const closeLoginDialog = () => {
  showLoginDialog.value = false
  targetRoute.value = ''
}

// 去登录
const goToLogin = () => {
  closeLoginDialog()
  if (targetRoute.value) {
    router.push({ name: 'login', query: { redirect: targetRoute.value } })
  } else {
    router.push('/login')
  }
}

// 检查导航是否需要登录
const handleNavClick = (event: Event, path: string) => {
  // 如果已登录，直接放行，让路由守卫处理角色验证
  if (isLoggedIn.value) {
    return
  }

  // 未登录时，检查目标路径是否需要登录
  const needsAuth = path.startsWith('/student/') || 
                    path.startsWith('/employer/') || 
                    path.startsWith('/teacher/') ||
                    path.startsWith('/guidance/')

  if (needsAuth) {
    event.preventDefault()
    event.stopPropagation()
    openLoginDialog(path)
  }
}

// 暴露给全局使用
;(window as any).checkAuthAndNavigate = (path: string) => {
  // 如果已登录，直接返回 true，让路由守卫处理角色检查
  if (isLoggedIn.value) {
    return true
  }
  
  // 未登录才显示登录对话框
  openLoginDialog(path)
  return false
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  checkLoginStatus()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="app-shell">
    <div class="app-shell__backdrop" />
    <header class="app-shell__header">
      <div class="brand">
        <span class="brand__logo">RyJ</span>
        <div class="brand__meta">
          <h1>高校就业管理系统</h1>
          <p>University Career Nexus</p>
        </div>
      </div>

      <nav class="nav">
        <RouterLink to="/home" class="nav-link">首页</RouterLink>
        
        <div class="nav-dropdown" v-if="isStudentRole">
          <span class="nav-link">学生专区 ▾</span>
          <div class="dropdown-menu">
            <RouterLink to="/student/overview" @click="(e) => handleNavClick(e, '/student/overview')">学生总览</RouterLink>
            <RouterLink to="/student/profile" @click="(e) => handleNavClick(e, '/student/profile')">个人档案</RouterLink>
            <RouterLink to="/student/resume" @click="(e) => handleNavClick(e, '/student/resume')">简历管理</RouterLink>
            <RouterLink to="/student/applications" @click="(e) => handleNavClick(e, '/student/applications')">求职申请</RouterLink>
            <RouterLink to="/student/interviews" @click="(e) => handleNavClick(e, '/student/interviews')">面试管理</RouterLink>
            <RouterLink to="/student/intention" @click="(e) => handleNavClick(e, '/student/intention')">就业意向</RouterLink>
            <RouterLink to="/student/guidance" @click="(e) => handleNavClick(e, '/student/guidance')">就业指导</RouterLink>
          </div>
        </div>
        
        <div class="nav-dropdown" v-if="isStudentRole">
          <span class="nav-link">职位招聘 ▾</span>
          <div class="dropdown-menu">
            <RouterLink to="/jobs/list" @click="(e) => handleNavClick(e, '/jobs/list')">职位</RouterLink>
            <RouterLink to="/jobs/my-applications" @click="(e) => handleNavClick(e, '/jobs/my-applications')">我的申请</RouterLink>
          </div>
        </div>
        
        <div class="nav-dropdown" v-if="isEmployerRole">
          <span class="nav-link">企业专区 ▾</span>
          <div class="dropdown-menu">
            <RouterLink to="/employer/overview" @click="(e) => handleNavClick(e, '/employer/overview')">企业总览</RouterLink>
            <RouterLink to="/employer/profile" @click="(e) => handleNavClick(e, '/employer/profile')">企业信息</RouterLink>
            <RouterLink to="/employer/jobs" @click="(e) => handleNavClick(e, '/employer/jobs')">岗位管理</RouterLink>
            <RouterLink to="/employer/applications" @click="(e) => handleNavClick(e, '/employer/applications')">应聘管理</RouterLink>
            <RouterLink to="/employer/interviews" @click="(e) => handleNavClick(e, '/employer/interviews')">面试安排</RouterLink>
            <RouterLink to="/employer/talent" @click="(e) => handleNavClick(e, '/employer/talent')">人才库</RouterLink>
          </div>
        </div>
        
        <div class="nav-dropdown" v-if="isTeacherRole">
          <span class="nav-link">教师专区 ▾</span>
          <div class="dropdown-menu">
            <RouterLink to="/teacher/overview" @click="(e) => handleNavClick(e, '/teacher/overview')">教师仪表板</RouterLink>
            <RouterLink to="/teacher/approvals" @click="(e) => handleNavClick(e, '/teacher/approvals')">审核学生档案</RouterLink>
            <RouterLink to="/teacher/guidance" @click="(e) => handleNavClick(e, '/teacher/guidance')">指导记录</RouterLink>
            <RouterLink to="/teacher/statistics" @click="(e) => handleNavClick(e, '/teacher/statistics')">统计分析</RouterLink>
            <RouterLink to="/teacher/profile" @click="(e) => handleNavClick(e, '/teacher/profile')">个人信息</RouterLink>
          </div>
        </div>
        
        <div class="nav-dropdown" v-if="isTeacherRole">
          <span class="nav-link">就业指导 ▾</span>
          <div class="dropdown-menu">
            <RouterLink to="/teacher/guidance" @click="(e) => handleNavClick(e, '/teacher/guidance')">指导记录</RouterLink>
            <RouterLink to="/guidance/resources" @click="(e) => handleNavClick(e, '/guidance/resources')">资源下载</RouterLink>
            <RouterLink to="/guidance/policies" @click="(e) => handleNavClick(e, '/guidance/policies')">就业政策</RouterLink>
          </div>
        </div>
        
        <div class="nav-dropdown" v-if="isAdminRole">
          <span class="nav-link">管理后台 ▾</span>
          <div class="dropdown-menu">
            <RouterLink to="/admin/overview" @click="(e) => handleNavClick(e, '/admin/overview')">管理员总览</RouterLink>
            <RouterLink to="/admin/users" @click="(e) => handleNavClick(e, '/admin/users')">用户管理</RouterLink>
            <RouterLink
              to="/admin/profile-approvals"
              @click="(e) => handleNavClick(e, '/admin/profile-approvals')"
            >
              资料审核
            </RouterLink>
            <RouterLink to="/admin/statistics" @click="(e) => handleNavClick(e, '/admin/statistics')">数据统计</RouterLink>
            <RouterLink to="/admin/notifications" @click="(e) => handleNavClick(e, '/admin/notifications')">通知管理</RouterLink>
          </div>
        </div>
        
        <RouterLink to="/notifications" class="nav-link">资讯动态</RouterLink>
      </nav>

      <div class="header-right">
        <!-- 信息提醒：点击弹出待处理信息列表，不跳转 -->
        <div v-if="isLoggedIn" class="header-notice-wrap" ref="noticeRef">
          <button
            type="button"
            class="header-notice"
            :title="unreadCount > 0 ? `您有 ${unreadCount} 条未读消息，请及时处理` : '消息与通知'"
            @click="toggleNoticePopover"
          >
            <span class="header-notice__icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                <path d="M13 22a2 2 0 0 1-2-2"></path>
              </svg>
              <span v-if="unreadCount > 0" class="header-notice__badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </span>
            <span v-if="unreadCount > 0" class="header-notice__hint">有新消息，请点击查看</span>
          </button>
          <transition name="dropdown">
            <div v-if="showNoticePopover" class="notice-popover">
              <div class="notice-popover__title">待处理信息</div>
              <div v-if="noticeLoading" class="notice-popover__loading">加载中…</div>
              <template v-else>
                <div v-if="noticeList.length === 0" class="notice-popover__empty">暂无待处理消息</div>
                <ul v-else class="notice-popover__list">
                  <li
                    v-for="item in noticeList"
                    :key="item.id"
                    class="notice-popover__item"
                    @click="handleNoticeItemClick(item)"
                  >
                    <span class="notice-popover__item-cat">{{ noticeCategoryLabel(item.category) }}</span>
                    <span class="notice-popover__item-title">{{ item.title }}</span>
                    <p v-if="item.content" class="notice-popover__item-content">{{ item.content }}</p>
                  </li>
                </ul>
              </template>
              <div class="notice-popover__footer">
                <router-link to="/notifications" class="notice-popover__link" @click="closeNoticePopover">前往资讯动态</router-link>
              </div>
            </div>
          </transition>
        </div>

      <div class="user-menu-container" ref="userMenuRef">
        <button class="user-menu-button" @click="toggleUserMenu">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
          <span>{{ displayName }}</span>
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'rotate': showUserMenu }">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </button>

        <transition name="dropdown">
          <div v-if="showUserMenu" class="user-menu-dropdown">
            <!-- 未登录状态 -->
            <template v-if="!isLoggedIn">
              <button class="menu-item" @click="login">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
                  <polyline points="10 17 15 12 10 7"></polyline>
                  <line x1="15" y1="12" x2="3" y2="12"></line>
                </svg>
                <span>登录</span>
              </button>
              <button class="menu-item" @click="router.push('/register')">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path>
                  <circle cx="9" cy="7" r="4"></circle>
                  <line x1="19" y1="8" x2="19" y2="14"></line>
                  <line x1="22" y1="11" x2="16" y2="11"></line>
                </svg>
                <span>注册</span>
              </button>
            </template>

            <!-- 已登录状态 -->
            <template v-else>
              <button class="menu-item" @click="switchUser">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path>
                  <circle cx="9" cy="7" r="4"></circle>
                  <path d="M22 21v-2a4 4 0 0 0-3-3.87"></path>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
                <span>切换用户</span>
              </button>
              <button class="menu-item logout" @click="logout">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                  <polyline points="16 17 21 12 16 7"></polyline>
                  <line x1="21" y1="12" x2="9" y2="12"></line>
                </svg>
                <span>退出登录</span>
              </button>
            </template>
          </div>
        </transition>
      </div>
      </div>
    </header>

    <main class="app-shell__content">
      <RouterView />
    </main>

    <footer class="app-shell__footer">
      <div class="footer__meta">
        <p>© 2026 高校就业管理系统 · 构建多角色协同的就业服务平台</p>
      </div>
      <div class="footer__links">
        <a href="#">使用指南</a>
        <a href="#">支持与反馈</a>
        <a href="#">隐私政策</a>
      </div>
    </footer>

    <!-- 登录提示对话框 -->
    <transition name="modal">
      <div v-if="showLoginDialog" class="modal-overlay" @click="closeLoginDialog">
        <div class="modal-dialog" @click.stop>
          <div class="modal-header">
            <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="modal-icon">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
              <path d="M9 12l2 2 4-4"></path>
            </svg>
            <h3>需要登录</h3>
            <button class="modal-close" @click="closeLoginDialog" aria-label="关闭">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <p>此功能需要登录后才能使用</p>
            <p class="modal-hint">登录后您将可以访问完整的功能模块</p>
          </div>
          <div class="modal-footer">
            <button class="modal-btn modal-btn-secondary" @click="closeLoginDialog">
              稍后再说
            </button>
            <button class="modal-btn modal-btn-primary" @click="goToLogin">
              立即登录
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.app-shell {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: radial-gradient(circle at 10% 20%, rgba(79, 172, 254, 0.18), transparent 40%),
    radial-gradient(circle at 90% 10%, rgba(124, 58, 237, 0.18), transparent 42%),
    #f5f7fb;
  color: #0f172a;
}

.app-shell__backdrop {
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, rgba(15, 23, 42, 0.08), transparent 60%);
  pointer-events: none;
}

.app-shell__header {
  position: sticky;
  top: 0;
  z-index: 5;
  display: grid;
  grid-template-columns: 280px 1fr auto;
  align-items: center;
  gap: 2rem;
  padding: 1.75rem 4vw;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px);
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.08);
}

.brand {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.brand__logo {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: linear-gradient(135deg, #2563eb, #8b5cf6);
  color: white;
  font-weight: 700;
  font-size: 1.2rem;
  letter-spacing: 0.08em;
}

.brand__meta h1 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 700;
  color: #0f172a;
}

.brand__meta p {
  margin: 0;
  font-size: 0.85rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #475569;
}

.nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
}

.nav-link {
  position: relative;
  color: #1e293b;
  text-decoration: none;
  font-weight: 600;
  letter-spacing: 0.03em;
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 8px;
  white-space: nowrap;
}

.nav-link:hover {
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
}

.nav > a.nav-link.router-link-active {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.1), rgba(139, 92, 246, 0.1));
  color: #2563eb;
}

/* 下拉菜单容器 */
.nav-dropdown {
  position: relative;
}

.nav-dropdown:hover .nav-link {
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
}

.nav-dropdown:hover .dropdown-menu {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

/* 下拉菜单内容 */
.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  min-width: 180px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.15);
  padding: 0.5rem;
  z-index: 100;
  border: 1px solid rgba(148, 163, 184, 0.12);
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px);
  transition: all 0.25s ease;
  margin-top: 0.5rem;
}

.dropdown-menu a {
  display: block;
  padding: 0.75rem 1rem;
  color: #475569;
  text-decoration: none;
  font-weight: 500;
  font-size: 0.9rem;
  border-radius: 8px;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.dropdown-menu a:hover {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(139, 92, 246, 0.08));
  color: #2563eb;
}

.dropdown-menu a.router-link-active {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(139, 92, 246, 0.12));
  color: #2563eb;
  font-weight: 600;
}

/* 右上角：信息提醒 + 用户菜单 */
.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.header-notice-wrap {
  position: relative;
}

.header-notice {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0 0.5rem;
  height: 44px;
  border-radius: 12px;
  color: #475569;
  text-decoration: none;
  transition: background 0.2s, color 0.2s;
  position: relative;
  border: none;
  background: none;
  cursor: pointer;
  font: inherit;
}

.header-notice:hover {
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
}

.header-notice:hover .header-notice__hint {
  color: #2563eb;
}

.header-notice__icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-notice__icon svg {
  display: block;
}

.header-notice__hint {
  font-size: 0.8rem;
  color: #64748b;
  white-space: nowrap;
  transition: color 0.2s;
}

.header-notice__badge {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #ef4444;
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

/* 消息弹层：待处理信息列表 */
.notice-popover {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  min-width: 320px;
  max-width: 400px;
  max-height: 70vh;
  background: white;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.15);
  border: 1px solid rgba(148, 163, 184, 0.12);
  z-index: 100;
  display: flex;
  flex-direction: column;
}

.notice-popover__title {
  padding: 1rem 1.25rem;
  font-weight: 700;
  font-size: 1rem;
  color: #0f172a;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.notice-popover__loading,
.notice-popover__empty {
  padding: 1.5rem 1.25rem;
  color: #64748b;
  font-size: 0.9rem;
}

.notice-popover__list {
  list-style: none;
  margin: 0;
  padding: 0.5rem 0;
  overflow: auto;
}

.notice-popover__item {
  padding: 0.75rem 1.25rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.08);
}

.notice-popover__item:last-child {
  border-bottom: none;
}

.notice-popover__item-cat {
  display: inline-block;
  font-size: 0.7rem;
  color: #64748b;
  background: rgba(100, 116, 139, 0.12);
  padding: 0.15rem 0.5rem;
  border-radius: 6px;
  margin-bottom: 0.35rem;
}

.notice-popover__item-title {
  display: block;
  font-weight: 600;
  font-size: 0.9rem;
  color: #0f172a;
}

.notice-popover__item-content {
  margin: 0.35rem 0 0;
  font-size: 0.8rem;
  color: #475569;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-word;
}

.notice-popover__footer {
  padding: 0.75rem 1.25rem;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}

.notice-popover__link {
  font-size: 0.9rem;
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}

.notice-popover__link:hover {
  text-decoration: underline;
}

/* 用户菜单容器 */
.user-menu-container {
  position: relative;
}

.user-menu-button {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #2563eb, #8b5cf6);
  color: white;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.25);
  transition: all 0.2s ease;
  max-width: 200px;
}

.user-menu-button span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

.user-menu-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.3);
}

.user-menu-button svg:last-child {
  transition: transform 0.3s ease;
}

.user-menu-button svg:last-child.rotate {
  transform: rotate(180deg);
}

/* 用户菜单下拉框 */
.user-menu-dropdown {
  position: absolute;
  top: calc(100% + 0.75rem);
  right: 0;
  min-width: 200px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.15);
  padding: 0.5rem;
  z-index: 100;
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border: none;
  background: transparent;
  color: #1e293b;
  font-weight: 500;
  font-size: 0.95rem;
  text-align: left;
  cursor: pointer;
  border-radius: 10px;
  transition: all 0.2s ease;
}

.menu-item:hover {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(139, 92, 246, 0.08));
  color: #2563eb;
}

.menu-item.logout {
  color: #dc2626;
}

.menu-item.logout:hover {
  background: rgba(220, 38, 38, 0.08);
  color: #b91c1c;
}

.menu-item svg {
  flex-shrink: 0;
}

/* 下拉动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.25s ease;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-5px) scale(0.98);
}

.app-shell__content {
  position: relative;
  z-index: 1;
  flex: 1;
  padding: 2.5rem 4vw 3.5rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.app-shell__footer {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem 4vw 2rem;
  color: #475569;
  font-size: 0.85rem;
  border-top: 1px solid rgba(148, 163, 184, 0.2);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.6), rgba(255, 255, 255, 0.85));
}

.footer__links {
  display: flex;
  gap: 1.5rem;
}

.footer__links a {
  color: #475569;
  text-decoration: none;
  transition: color 0.2s ease;
}

.footer__links a:hover {
  color: #1d4ed8;
}

@media (max-width: 1024px) {
  .app-shell__header {
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
    gap: 1.5rem;
  }

  .nav {
    flex-wrap: wrap;
    justify-content: center;
    max-width: 100%;
  }

  .nav-link {
    font-size: 0.85rem;
    padding: 0.6rem 0.8rem;
  }

  .dropdown-menu {
    min-width: 150px;
  }

  .dropdown-menu a {
    font-size: 0.85rem;
    padding: 0.6rem 0.8rem;
  }

  .app-shell__footer {
    flex-direction: column;
    gap: 1rem;
    text-align: center;
  }
}

@media (max-width: 640px) {
  .app-shell__content {
    padding: 1.5rem 1.25rem 2.5rem;
  }
}

/* 登录提示对话框样式 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 1rem;
}

.modal-dialog {
  background: white;
  border-radius: 24px;
  box-shadow: 0 25px 50px rgba(15, 23, 42, 0.3);
  max-width: 480px;
  width: 100%;
  overflow: hidden;
  animation: modalSlideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 2.5rem 2rem 1.5rem;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(139, 92, 246, 0.08));
}

.modal-icon {
  color: #2563eb;
  animation: iconBounce 0.6s ease-out;
}

@keyframes iconBounce {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

.modal-header h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: #0f172a;
}

.modal-close {
  position: absolute;
  top: 1rem;
  right: 1rem;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: rgba(15, 23, 42, 0.05);
  color: #64748b;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: rgba(15, 23, 42, 0.1);
  color: #0f172a;
}

.modal-body {
  padding: 2rem;
  text-align: center;
}

.modal-body p {
  margin: 0;
  font-size: 1.1rem;
  color: #1e293b;
  line-height: 1.6;
}

.modal-hint {
  margin-top: 0.75rem !important;
  font-size: 0.95rem !important;
  color: #64748b !important;
}

.modal-footer {
  display: flex;
  gap: 1rem;
  padding: 1.5rem 2rem 2rem;
  border-top: 1px solid rgba(148, 163, 184, 0.15);
}

.modal-btn {
  flex: 1;
  padding: 0.875rem 1.5rem;
  border: none;
  border-radius: 14px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-btn-secondary {
  background: rgba(148, 163, 184, 0.1);
  color: #475569;
}

.modal-btn-secondary:hover {
  background: rgba(148, 163, 184, 0.2);
  color: #1e293b;
}

.modal-btn-primary {
  background: linear-gradient(135deg, #2563eb, #8b5cf6);
  color: white;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.3);
}

.modal-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.4);
}

.modal-btn-primary:active {
  transform: translateY(0);
}

/* 模态框动画 */
.modal-enter-active {
  transition: all 0.3s ease-out;
}

.modal-leave-active {
  transition: all 0.25s ease-in;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-dialog {
  transform: translateY(-20px) scale(0.95);
}

.modal-leave-to .modal-dialog {
  transform: translateY(10px) scale(0.98);
}

@media (max-width: 640px) {
  .modal-dialog {
    margin: 0 1rem;
  }

  .modal-header {
    padding: 2rem 1.5rem 1.25rem;
  }

  .modal-body {
    padding: 1.5rem;
  }

  .modal-footer {
    flex-direction: column;
    padding: 1.25rem 1.5rem 1.75rem;
  }
}
</style>
