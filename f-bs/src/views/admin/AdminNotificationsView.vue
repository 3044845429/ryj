<script setup lang="ts">
import { ref, computed } from 'vue'
import { sendSystemNotification, type AdminNotificationRequest } from '../../api/admin'

const loading = ref(false)
const error = ref('')
const success = ref('')
const formData = ref<AdminNotificationRequest>({
  userId: null,
  title: '',
  content: '',
  category: 'SYSTEM',
  readFlag: false,
})

const adminUserId = computed(() => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    try {
      const userInfo = JSON.parse(userInfoStr)
      return userInfo.id
    } catch (e) {
      return null
    }
  }
  return null
})

const handleSend = async () => {
  if (!adminUserId.value) {
    error.value = '请先登录'
    return
  }
  if (!formData.value.title || !formData.value.content) {
    error.value = '请填写标题和内容'
    return
  }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await sendSystemNotification(adminUserId.value, formData.value)
    success.value = formData.value.userId ? '通知发送成功' : '系统通知已发送给所有用户'
    formData.value = {
      userId: null,
      title: '',
      content: '',
      category: 'SYSTEM',
      readFlag: false,
    }
  } catch (err: any) {
    error.value = err.message || '发送失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="admin-notifications">
    <div class="page-header">
      <h1>系统通知管理</h1>
      <p class="subtitle">发送系统通知给指定用户或所有用户</p>
    </div>

    <div class="notification-form">
      <div class="form-header">
        <div class="form-header-icon">📢</div>
        <div class="form-header-text">
          <h2>创建系统通知</h2>
          <p>填写以下信息发送通知给用户</p>
        </div>
      </div>

      <div v-if="error" class="error-message">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
        <span>{{ error }}</span>
      </div>
      <div v-if="success" class="success-message">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
          <polyline points="22 4 12 14.01 9 11.01"></polyline>
        </svg>
        <span>{{ success }}</span>
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label>
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
            发送对象
          </label>
          <select v-model="formData.userId" class="form-select">
            <option :value="null">所有用户</option>
            <option :value="1">指定用户（需要实现用户选择器）</option>
          </select>
          <small class="form-hint">选择"所有用户"将发送给系统中的所有用户</small>
        </div>

        <div class="form-group">
          <label>
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
              <circle cx="12" cy="10" r="3"></circle>
            </svg>
            通知类别
          </label>
          <select v-model="formData.category" class="form-select">
            <option value="SYSTEM">系统通知</option>
            <option value="INTERVIEW">面试通知</option>
            <option value="APPLICATION">申请通知</option>
            <option value="GUIDANCE">指导通知</option>
          </select>
        </div>
      </div>

      <div class="form-group">
        <label>
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="4" y1="7" x2="20" y2="7"></line>
            <line x1="4" y1="12" x2="20" y2="12"></line>
            <line x1="4" y1="17" x2="20" y2="17"></line>
          </svg>
          通知标题 *
        </label>
        <input
          v-model="formData.title"
          type="text"
          placeholder="请输入通知标题，例如：系统维护通知"
          class="form-input"
          required
          maxlength="150"
        />
        <small class="form-hint">标题应简洁明了，建议不超过50字</small>
      </div>

      <div class="form-group">
        <label>
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
          通知内容 *
        </label>
        <textarea
          v-model="formData.content"
          placeholder="请输入通知的详细内容，支持多行文本..."
          class="form-textarea"
          rows="8"
          required
          maxlength="2000"
        ></textarea>
        <div class="form-footer-hint">
          <small class="form-hint">建议内容清晰完整，便于用户理解</small>
          <small class="char-count">{{ formData.content.length }}/2000</small>
        </div>
      </div>

      <div class="form-actions">
        <button class="btn-secondary" @click="() => {
          formData = {
            userId: null,
            title: '',
            content: '',
            category: 'SYSTEM',
            readFlag: false,
          }
          error = ''
          success = ''
        }" :disabled="loading">
          重置
        </button>
        <button class="btn-primary" @click="handleSend" :disabled="loading || !formData.title || !formData.content">
          <svg v-if="!loading" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="22" y1="2" x2="11" y2="13"></line>
            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
          </svg>
          <span v-if="loading" class="loading-spinner"></span>
          <span>{{ loading ? '发送中...' : '发送通知' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-notifications {
  max-width: 1000px;
  margin: 0 auto;
  padding: 3rem 4rem;
  min-height: calc(100vh - 200px);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
  background: 
    radial-gradient(circle at 0% 0%, rgba(0, 113, 227, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 100% 100%, rgba(94, 92, 230, 0.03) 0%, transparent 50%),
    linear-gradient(180deg, #fafafa 0%, #ffffff 100%);
}

.page-header {
  margin-bottom: 3rem;
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

.notification-form {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: saturate(180%) blur(30px);
  -webkit-backdrop-filter: saturate(180%) blur(30px);
  border-radius: 24px;
  padding: 3rem;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.06),
    0 2px 8px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  animation: fadeIn 0.6s ease-out;
}

.form-header {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  margin-bottom: 2.5rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.form-header-icon {
  font-size: 3rem;
  line-height: 1;
  filter: grayscale(0.1);
}

.form-header-text h2 {
  margin: 0 0 0.5rem 0;
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #1d1d1f;
}

.form-header-text p {
  margin: 0;
  color: #86868b;
  font-size: 0.9375rem;
  font-weight: 400;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.75rem;
  margin-bottom: 1.75rem;
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

.error-message {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.1), rgba(255, 45, 85, 0.1));
  color: #ff3b30;
  padding: 1.25rem 1.5rem;
  border-radius: 16px;
  margin-bottom: 2rem;
  border: 1px solid rgba(255, 59, 48, 0.2);
  font-size: 0.9375rem;
  font-weight: 500;
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.error-message svg {
  flex-shrink: 0;
}

.success-message {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.1), rgba(48, 209, 88, 0.1));
  color: #34c759;
  padding: 1.25rem 1.5rem;
  border-radius: 16px;
  margin-bottom: 2rem;
  border: 1px solid rgba(52, 199, 89, 0.2);
  font-size: 0.9375rem;
  font-weight: 500;
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.success-message svg {
  flex-shrink: 0;
}

.form-group {
  margin-bottom: 1.75rem;
}

.form-group label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  font-weight: 600;
  color: #1d1d1f;
  font-size: 0.9375rem;
  letter-spacing: -0.01em;
}

.form-group label svg {
  color: #0071e3;
  flex-shrink: 0;
}

.form-select,
.form-input,
.form-textarea {
  width: 100%;
  padding: 0.875rem 1.25rem;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  font-size: 0.9375rem;
  font-family: inherit;
  background: rgba(255, 255, 255, 0.9);
  color: #1d1d1f;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.form-select:focus,
.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: #0071e3;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.1);
}

.form-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%231d1d1f' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 1.25rem center;
  padding-right: 3rem;
  cursor: pointer;
}

.form-textarea {
  resize: vertical;
  min-height: 180px;
  line-height: 1.7;
  font-family: inherit;
}

.form-footer-hint {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.5rem;
}

.form-hint {
  display: block;
  margin-top: 0.5rem;
  color: #86868b;
  font-size: 0.8125rem;
  font-weight: 400;
}

.char-count {
  color: #86868b;
  font-size: 0.8125rem;
  font-weight: 500;
}

.form-actions {
  margin-top: 2.5rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}

.btn-secondary {
  padding: 0.875rem 2rem;
  background: rgba(0, 0, 0, 0.04);
  color: #1d1d1f;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  font-weight: 600;
  font-size: 0.9375rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  letter-spacing: -0.01em;
}

.btn-secondary:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.06);
  border-color: rgba(0, 0, 0, 0.12);
  transform: translateY(-1px);
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  padding: 0.875rem 2rem;
  background: linear-gradient(135deg, #0071e3 0%, #5e5ce6 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  font-size: 0.9375rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  box-shadow: 
    0 4px 16px rgba(0, 113, 227, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  letter-spacing: -0.01em;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 
    0 8px 24px rgba(0, 113, 227, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  background: linear-gradient(135deg, #0077ed 0%, #6366f1 100%);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(0);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1024px) {
  .admin-notifications {
    padding: 3rem 2.5rem;
    max-width: 100%;
  }
  
  .page-header h1 {
    font-size: 2.5rem;
  }
  
  .notification-form {
    padding: 2.5rem 2rem;
  }
  
  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .admin-notifications {
    padding: 2rem 1.5rem;
  }
  
  .page-header h1 {
    font-size: 2rem;
  }
  
  .subtitle {
    font-size: 1rem;
  }
  
  .notification-form {
    padding: 2rem 1.5rem;
  }
  
  .form-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .form-header-icon {
    font-size: 2.5rem;
  }
  
  .form-actions {
    flex-direction: column-reverse;
  }
  
  .btn-primary,
  .btn-secondary {
    width: 100%;
    justify-content: center;
  }
}
</style>

