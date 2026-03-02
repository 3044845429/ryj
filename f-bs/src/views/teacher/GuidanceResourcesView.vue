<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getPublicResources, type PublicResourceSummary } from '@/api/public'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

const loading = ref(true)
const error = ref('')
const resources = ref<PublicResourceSummary[]>([])

const formatSize = (size: number) => {
  if (!size || size <= 0) return '未知大小'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`
}

const formatDate = (value: string | null) => {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const downloadHref = (item: PublicResourceSummary) => {
  if (!item.downloadUrl) return '#'
  if (item.downloadUrl.startsWith('http')) return item.downloadUrl
  return `${API_BASE}${item.downloadUrl}`
}

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    resources.value = await getPublicResources()
  } catch (e: any) {
    error.value = e?.message || '加载资料失败，请稍后重试'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="teacher-page">
    <main class="teacher-body">
      <div class="body-inner">
        <header class="top-bar">
          <div class="top-bar__title">
            <h1>资料下载</h1>
            <p class="subtitle">集中访问简历模板、协议范本与就业指导相关文件</p>
          </div>
        </header>

        <div v-if="loading" class="card loading-card">
          <div class="spinner" />
          <p>正在加载资料...</p>
        </div>

        <div v-else-if="error" class="card error-card">
          <p>{{ error }}</p>
        </div>

        <div v-else-if="!resources.length" class="card empty-card">
          <p>暂时还没有可下载的资料。</p>
          <small>管理员或教师可通过首页的资料上传入口补充内容。</small>
        </div>

        <section v-else class="card resource-section">
          <div class="resource-header">
            <h2>全部资料</h2>
            <span class="count">共 {{ resources.length }} 个文件</span>
          </div>
          <div class="resource-grid">
            <article v-for="item in resources" :key="item.id" class="resource-card">
              <div class="resource-main">
                <div class="resource-icon">
                  <span>{{ (item.fileType || 'file').split('/').pop()?.toUpperCase().slice(0, 3) }}</span>
                </div>
                <div class="resource-meta">
                  <h3 class="resource-title" :title="item.fileName">{{ item.fileName }}</h3>
                  <p v-if="item.description" class="resource-desc">
                    {{ item.description }}
                  </p>
                  <div class="resource-sub">
                    <span>{{ formatSize(item.fileSize) }}</span>
                    <span>·</span>
                    <span>{{ formatDate(item.createdAt) }}</span>
                  </div>
                </div>
              </div>
              <a class="resource-download" :href="downloadHref(item)" target="_blank" rel="noopener">
                下载
              </a>
            </article>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.teacher-page {
  min-height: calc(100vh - 80px);
  padding: 2.5rem 4vw;
  background: radial-gradient(circle at 0 0, rgba(37, 99, 235, 0.04), transparent 50%),
    radial-gradient(circle at 100% 100%, rgba(124, 58, 237, 0.04), transparent 55%),
    #f4f5fb;
}

.teacher-body {
  max-width: 1120px;
  margin: 0 auto;
}

.body-inner {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.top-bar__title h1 {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #111827;
}

.subtitle {
  margin: 0.25rem 0 0;
  color: #6b7280;
  font-size: 0.95rem;
}

.card {
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.06);
  padding: 1.75rem 1.75rem;
}

.loading-card,
.error-card,
.empty-card {
  text-align: center;
  color: #6b7280;
}

.spinner {
  width: 32px;
  height: 32px;
  margin: 0 auto 0.75rem;
  border-radius: 999px;
  border: 3px solid rgba(37, 99, 235, 0.15);
  border-top-color: #2563eb;
  animation: spin 0.9s linear infinite;
}

.resource-section {
  padding-top: 1.5rem;
}

.resource-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.resource-header h2 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 600;
  color: #111827;
}

.count {
  font-size: 0.85rem;
  color: #9ca3af;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 1rem;
}

.resource-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.9rem 1rem;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.95));
}

.resource-main {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.resource-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.resource-meta {
  min-width: 0;
}

.resource-title {
  margin: 0 0 0.15rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.resource-desc {
  margin: 0 0 0.2rem;
  font-size: 0.82rem;
  color: #6b7280;
  max-height: 2.4em;
  overflow: hidden;
  text-overflow: ellipsis;
}

.resource-sub {
  display: flex;
  gap: 0.35rem;
  font-size: 0.8rem;
  color: #9ca3af;
}

.resource-download {
  flex-shrink: 0;
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  border: 1px solid rgba(37, 99, 235, 0.5);
  color: #2563eb;
  font-size: 0.85rem;
  text-decoration: none;
}

.resource-download:hover {
  background: rgba(37, 99, 235, 0.06);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .teacher-page {
    padding: 1.5rem 1.25rem;
  }
}
</style>

