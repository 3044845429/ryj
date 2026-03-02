<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  fetchEmployerProfile,
  saveEmployerProfile,
  type EmployerProfile,
  type EmployerProfileRequestPayload,
} from '../../api/employer'

const userInfo = ref<{ id?: number; role?: string } | null>(null)
const userId = ref<number | null>(null)

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error' | ''>('')

const form = reactive({
  companyName: '',
  contactPerson: '',
  contactEmail: '',
  contactPhone: '',
  description: '',
  website: '',
})

const resetMessage = () => {
  message.value = ''
  messageType.value = ''
}

const assignProfile = (profile: EmployerProfile | null) => {
  if (!profile) {
    form.companyName = ''
    form.contactPerson = ''
    form.contactEmail = ''
    form.contactPhone = ''
    form.description = ''
    form.website = ''
    return
  }
  form.companyName = profile.companyName || ''
  form.contactPerson = profile.contactPerson || ''
  form.contactEmail = profile.contactEmail || ''
  form.contactPhone = profile.contactPhone || ''
  form.description = profile.description || ''
  form.website = profile.website || ''
}

const loadProfile = async () => {
  if (!userId.value) return
  loading.value = true
  resetMessage()
  try {
    const profile = await fetchEmployerProfile(userId.value)
    assignProfile(profile)
    if (!profile) {
      message.value = '请完善企业资料以便后续发布岗位和管理投递'
      messageType.value = 'error'
    }
  } catch (err) {
    console.error(err)
    message.value = (err as Error).message || '加载企业资料失败'
    messageType.value = 'error'
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!userId.value) {
    message.value = '请使用企业账号登录后再试'
    messageType.value = 'error'
    return
  }
  if (!form.companyName.trim()) {
    message.value = '企业名称不能为空'
    messageType.value = 'error'
    return
  }
  saving.value = true
  resetMessage()
  const payload: EmployerProfileRequestPayload = {
    companyName: form.companyName.trim(),
    contactPerson: form.contactPerson.trim() || null,
    contactEmail: form.contactEmail.trim() || null,
    contactPhone: form.contactPhone.trim() || null,
    description: form.description.trim() || null,
    website: form.website.trim() || null,
  }
  try {
    const profile = await saveEmployerProfile(userId.value, payload)
    assignProfile(profile)
    message.value = '已提交管理员审核，审核通过后企业资料会更新'
    messageType.value = 'success'
  } catch (err) {
    console.error(err)
    message.value = (err as Error).message || '保存失败，请稍后重试'
    messageType.value = 'error'
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  const stored = localStorage.getItem('userInfo')
  if (stored) {
    try {
      userInfo.value = JSON.parse(stored)
      userId.value = userInfo.value?.id ?? null
    } catch (err) {
      console.error('解析用户信息失败', err)
      userInfo.value = null
      userId.value = null
    }
  }
  if (!userId.value || userInfo.value?.role !== 'EMPLOYER') {
    message.value = '仅企业用户可访问该页面'
    messageType.value = 'error'
    return
  }
  loadProfile()
})
</script>

<template>
  <div class="profile-page">
    <header class="page-header">
      <div>
        <h1>企业资料管理</h1>
        <p class="subtitle">完善企业信息以提升岗位展示效果与候选人信任度</p>
      </div>
      <RouterLink class="back-link" to="/employer/overview">
        <span class="icon">←</span>
        <span>返回总览</span>
      </RouterLink>
    </header>

    <section v-if="message" :class="['message', messageType]">{{ message }}</section>

    <section v-if="userInfo?.role !== 'EMPLOYER'" class="guard">请登录企业账号后再进行资料维护。</section>

    <form v-else class="profile-form" @submit.prevent="submit">
      <fieldset :disabled="loading || saving">
        <legend>企业基础信息</legend>
        <label>
          企业名称<span>*</span>
          <input v-model="form.companyName" type="text" placeholder="请输入企业对外展示名称" />
        </label>
        <label>
          企业官网
          <input v-model="form.website" type="url" placeholder="https://example.com" />
        </label>
      </fieldset>

      <fieldset :disabled="loading || saving">
        <legend>联系人信息</legend>
        <label>
          联系人
          <input v-model="form.contactPerson" type="text" placeholder="负责招聘的联系人" />
        </label>
        <label>
          联系邮箱
          <input v-model="form.contactEmail" type="email" placeholder="hr@example.com" />
        </label>
        <label>
          联系电话
          <input v-model="form.contactPhone" type="tel" placeholder="010-12345678" />
        </label>
      </fieldset>

      <fieldset :disabled="loading || saving">
        <legend>企业简介</legend>
        <textarea
          v-model="form.description"
          rows="6"
          placeholder="介绍企业业务、核心优势、企业文化等内容，帮助候选人快速了解公司"
        ></textarea>
      </fieldset>

      <div class="actions">
        <button type="submit" :disabled="saving">{{ saving ? '正在保存...' : '保存资料' }}</button>
        <button type="button" class="secondary" @click="assignProfile(null)" :disabled="saving">清空表单</button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 2rem 1.5rem 4rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.page-header h1 {
  margin: 0;
  font-size: 2.1rem;
  font-weight: 700;
  color: #1e293b;
}

.subtitle {
  margin: 0.35rem 0 0;
  color: #64748b;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border-radius: 12px;
  background: linear-gradient(135deg, #ffffff, #f8fafc);
  color: #475569;
  text-decoration: none;
  font-weight: 600;
  border: 1px solid #e2e8f0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.back-link:hover {
  transform: translateX(-4px);
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: #cbd5e1;
}

.back-link .icon {
  font-size: 1.2rem;
  transition: transform 0.3s;
}

.back-link:hover .icon {
  transform: translateX(-2px);
}

.message {
  padding: 1rem 1.2rem;
  border-radius: 12px;
  margin-bottom: 1.2rem;
  font-size: 0.95rem;
}

.message.success {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
  border: 1px solid rgba(34, 197, 94, 0.25);
}

.message.error {
  background: rgba(248, 113, 113, 0.12);
  color: #b91c1c;
  border: 1px solid rgba(248, 113, 113, 0.25);
}

.guard {
  padding: 2rem;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #cbd5f5;
  color: #475569;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

fieldset {
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 1.5rem;
  display: grid;
  gap: 1rem;
}

legend {
  font-weight: 600;
  color: #1f2937;
  padding: 0 0.6rem;
}

label {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  color: #334155;
  font-size: 0.95rem;
}

label span {
  color: #ef4444;
  margin-left: 0.3rem;
}

input,
textarea {
  padding: 0.75rem 1rem;
  border-radius: 10px;
  border: 2px solid #e2e8f0;
  font-size: 0.95rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

input:focus,
textarea:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

textarea {
  resize: vertical;
}

.actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-start;
}

button {
  padding: 0.75rem 1.5rem;
  border-radius: 10px;
  border: none;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

button[type='submit'] {
  background: linear-gradient(135deg, #2563eb, #9333ea);
  color: #fff;
  box-shadow: 0 8px 18px rgba(79, 70, 229, 0.25);
}

button.secondary {
  background: #e2e8f0;
  color: #1f2937;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  fieldset {
    padding: 1.2rem;
  }

  .actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
