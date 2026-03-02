<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getTeacherByUserId, getTeacherProfile, updateTeacherProfile, type TeacherProfileInfo } from '@/api/teacher'

const teacherId = ref<number | null>(null)
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const message = ref('')

const form = reactive({
  name: '',
  department: '',
  major: '',
  email: '',
  phone: '',
  focus: '',
  biography: '',
})

const loadProfile = async () => {
  loading.value = true
  error.value = ''
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (!userInfoStr) throw new Error('请先登录')
    const userInfo = JSON.parse(userInfoStr)
    if (userInfo.role !== 'TEACHER') throw new Error('当前账号不是教师角色')
    const rec = await getTeacherByUserId(userInfo.id)
    if (!rec || rec.id == null) {
      throw new Error('未找到当前教师档案，请联系管理员为该账号绑定教师信息')
    }
    teacherId.value = rec.id
    const detail: TeacherProfileInfo = await getTeacherProfile(rec.id)
    form.name = detail.name || ''
    form.department = detail.department || ''
    form.major = (detail as any).major || ''
    form.email = detail.email || ''
    form.phone = detail.phone || ''
    form.focus = detail.focus || ''
    form.biography = detail.biography || ''
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

const save = async () => {
  if (!teacherId.value) return
  saving.value = true
  message.value = ''
  error.value = ''
  try {
    await updateTeacherProfile(teacherId.value, {
      name: form.name.trim() || undefined,
      department: form.department.trim() || null,
      major: form.major.trim() || null,
      email: form.email.trim() || null,
      phone: form.phone.trim() || null,
      focus: form.focus.trim() || null,
      biography: form.biography.trim() || null,
    })
    message.value = '已提交管理员审核，请等待审核结果后生效'
  } catch (e: any) {
    error.value = e?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
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
            <h1>教师信息</h1>
            <p class="subtitle">完善基本资料与学院信息，便于学生按专业匹配班主任。</p>
          </div>
        </header>

        <div v-if="loading" class="teacher-loading">
          <div class="spinner" />
          加载中…
        </div>
        <div v-else class="teacher-card">
          <form class="profile-form" @submit.prevent="save">
            <div class="form-grid">
              <label class="field">
                <span class="teacher-label">姓名</span>
                <input v-model="form.name" class="teacher-input" placeholder="如：张晓琳" />
              </label>
              <label class="field">
                <span class="teacher-label">学院/部门</span>
                <input v-model="form.department" class="teacher-input" placeholder="如：计算机学院" />
              </label>
              <label class="field">
                <span class="teacher-label">专业</span>
                <input v-model="form.major" class="teacher-input" placeholder="如：软件工程" />
              </label>
              <label class="field">
                <span class="teacher-label">邮箱</span>
                <input v-model="form.email" class="teacher-input" type="email" placeholder="name@school.edu.cn" />
              </label>
              <label class="field">
                <span class="teacher-label">电话</span>
                <input v-model="form.phone" class="teacher-input" placeholder="如：138****8888" />
              </label>
              <label class="field">
                <span class="teacher-label">研究方向</span>
                <input v-model="form.focus" class="teacher-input" placeholder="如：分布式系统、AI教育" />
              </label>
            </div>
            <label class="field field-full">
              <span class="teacher-label">个人简介</span>
              <textarea v-model="form.biography" class="teacher-textarea" rows="5" placeholder="教学理念、科研方向、对学生的建议…" />
            </label>
            <div class="form-actions">
              <button type="submit" class="teacher-btn teacher-btn-primary" :disabled="saving">
                {{ saving ? '保存中…' : '保存' }}
              </button>
            </div>
            <p v-if="message" class="msg msg-ok">{{ message }}</p>
            <p v-if="error" class="msg msg-err">{{ error }}</p>
          </form>
        </div>
      </div>
    </main>
  </div>
</template>

<style src="@/assets/teacher-layout.css"></style>
<style scoped>
.profile-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}
.field-full {
  grid-column: 1 / -1;
}
.form-actions {
  padding-top: 8px;
}
.msg {
  margin: 0;
  font-size: 0.9375rem;
}
.msg-ok { color: #34c759; }
.msg-err { color: #bf4800; }
</style>
