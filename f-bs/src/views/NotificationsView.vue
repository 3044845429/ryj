<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getPublicDynamics, type DynamicsItem } from '@/api/public'

const loading = ref(true)
const error = ref<string | null>(null)
const list = ref<DynamicsItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const categoryFilter = ref<string>('')

const categoryOptions = [
  { value: '', label: '全部' },
  { value: 'SYSTEM', label: '系统' },
  { value: 'INTERVIEW', label: '面试' },
  { value: 'APPLICATION', label: '申请' },
  { value: 'GUIDANCE', label: '指导' },
]

const categoryLabels: Record<string, string> = {
  SYSTEM: '系统',
  INTERVIEW: '面试',
  APPLICATION: '申请',
  GUIDANCE: '指导',
}

const categoryClass: Record<string, string> = {
  SYSTEM: 'dynamics-cat-system',
  INTERVIEW: 'dynamics-cat-interview',
  APPLICATION: 'dynamics-cat-application',
  GUIDANCE: 'dynamics-cat-guidance',
}

const hasMore = computed(() => list.value.length < total.value)

function formatDate(value: string | null | undefined) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const now = new Date()
  const today = now.toDateString() === d.toDateString()
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (yesterday.toDateString() === d.toDateString()) {
    return `昨天 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }
  if (today) {
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function load(append = false) {
  try {
    if (!append) loading.value = true
    error.value = null
    const page = append ? currentPage.value : 1
    if (!append) currentPage.value = 1
    const res = await getPublicDynamics({
      page,
      size: pageSize,
      category: categoryFilter.value || undefined,
    })
    if (append) {
      list.value = [...list.value, ...(res.records || [])]
    } else {
      list.value = res.records || []
    }
    total.value = res.total ?? 0
    if (!append && res.records?.length) currentPage.value = page
  } catch (e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
    if (!append) list.value = []
  } finally {
    loading.value = false
  }
}

function setCategory(value: string) {
  categoryFilter.value = value
  load()
}

function loadMore() {
  if (!hasMore.value || loading.value) return
  currentPage.value += 1
  load(true)
}

onMounted(() => load())
</script>

<template>
  <div class="dynamics-page">
    <div class="dynamics-page__backdrop" aria-hidden="true" />
    <main class="dynamics-page__main">
      <div class="dynamics-page__inner">
        <header class="dynamics-page__header">
          <div>
            <h1 class="dynamics-page__title">资讯动态</h1>
            <p class="dynamics-page__subtitle">浏览网上发布的各类动态讯息，了解系统公告、面试与申请等最新动态</p>
          </div>
          <div class="dynamics-page__filters">
            <button
              v-for="opt in categoryOptions"
              :key="opt.value || 'all'"
              type="button"
              class="dynamics-page__filter"
              :class="{ active: categoryFilter === opt.value }"
              @click="setCategory(opt.value)"
            >
              {{ opt.label }}
            </button>
          </div>
        </header>

        <div v-if="loading && list.length === 0" class="dynamics-page__loading">
          <div class="dynamics-page__spinner" />
          <span>加载中…</span>
        </div>

        <div v-else-if="error" class="dynamics-page__error">
          <p>{{ error }}</p>
          <button type="button" class="dynamics-page__btn-primary" @click="load">重试</button>
        </div>

        <div v-else-if="list.length === 0" class="dynamics-page__empty">
          <div class="dynamics-page__empty-icon">📋</div>
          <p>暂无动态</p>
          <p class="dynamics-page__empty-hint">当前分类下暂无发布的资讯，请稍后再来或切换其他分类</p>
        </div>

        <div v-else class="dynamics-page__list">
          <article
            v-for="item in list"
            :key="item.id"
            class="dynamics-page__card"
          >
            <div class="dynamics-page__card-head">
              <span :class="['dynamics-page__category', categoryClass[item.category || ''] || '']">
                {{ categoryLabels[item.category || ''] || item.category || '动态' }}
              </span>
              <time class="dynamics-page__time">{{ formatDate(item.createdAt) }}</time>
            </div>
            <h2 class="dynamics-page__card-title">{{ item.title }}</h2>
            <p v-if="item.content" class="dynamics-page__card-content">{{ item.content }}</p>
          </article>
          <div v-if="hasMore" class="dynamics-page__more">
            <button
              type="button"
              class="dynamics-page__btn-secondary"
              :disabled="loading"
              @click="loadMore"
            >
              {{ loading ? '加载中…' : '加载更多' }}
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.dynamics-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f7;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.dynamics-page__backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 320px;
  background: linear-gradient(180deg, rgba(0, 113, 227, 0.04) 0%, transparent 60%);
  pointer-events: none;
  z-index: 0;
}

.dynamics-page__main {
  position: relative;
  z-index: 1;
  flex: 1;
  padding: 32px 40px 48px;
  overflow: auto;
}

.dynamics-page__inner {
  max-width: 800px;
  margin: 0 auto;
}

.dynamics-page__header {
  margin-bottom: 32px;
}

.dynamics-page__title {
  font-size: 1.75rem;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 6px;
  letter-spacing: -0.025em;
}

.dynamics-page__subtitle {
  font-size: 0.9375rem;
  color: #86868b;
  margin: 0 0 20px;
}

.dynamics-page__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dynamics-page__filter {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 0.9375rem;
  font-weight: 500;
  color: #86868b;
  background: #e8e8ed;
  border: none;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.dynamics-page__filter:hover {
  background: #d2d2d7;
  color: #1d1d1f;
}

.dynamics-page__filter.active {
  background: #0071e3;
  color: #fff;
}

.dynamics-page__btn-primary {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 0.9375rem;
  font-weight: 500;
  background: #0071e3;
  color: #fff;
  border: none;
  cursor: pointer;
  transition: opacity 0.2s;
}

.dynamics-page__btn-primary:hover {
  opacity: 0.9;
}

.dynamics-page__btn-secondary {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 0.9375rem;
  font-weight: 500;
  background: #e8e8ed;
  color: #1d1d1f;
  border: none;
  cursor: pointer;
  transition: background 0.2s;
}

.dynamics-page__btn-secondary:hover {
  background: #d2d2d7;
}

.dynamics-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  color: #86868b;
  font-size: 1rem;
}

.dynamics-page__spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e8e8ed;
  border-top-color: #0071e3;
  border-radius: 50%;
  margin-bottom: 16px;
  animation: dynamics-spin 0.8s linear infinite;
}

@keyframes dynamics-spin {
  to { transform: rotate(360deg); }
}

.dynamics-page__error {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  text-align: center;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.dynamics-page__error p {
  margin: 0 0 16px;
  color: #515154;
  font-size: 0.9375rem;
}

.dynamics-page__empty {
  text-align: center;
  padding: 64px 24px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.dynamics-page__empty-icon {
  font-size: 3rem;
  margin-bottom: 16px;
  opacity: 0.7;
}

.dynamics-page__empty p {
  margin: 0;
  font-size: 1rem;
  color: #1d1d1f;
}

.dynamics-page__empty-hint {
  margin-top: 8px !important;
  font-size: 0.9375rem !important;
  color: #86868b !important;
}

.dynamics-page__list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dynamics-page__card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s;
}

.dynamics-page__card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.dynamics-page__card-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.dynamics-page__category {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.dynamics-cat-system {
  background: rgba(0, 113, 227, 0.12);
  color: #0071e3;
}

.dynamics-cat-interview {
  background: rgba(52, 199, 89, 0.12);
  color: #248a3d;
}

.dynamics-cat-application {
  background: rgba(255, 149, 0, 0.12);
  color: #c93400;
}

.dynamics-cat-guidance {
  background: rgba(88, 86, 214, 0.12);
  color: #5856d6;
}

.dynamics-page__time {
  font-size: 0.8125rem;
  color: #86868b;
  margin-left: auto;
}

.dynamics-page__card-title {
  font-size: 1.0625rem;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 8px;
  letter-spacing: -0.01em;
  line-height: 1.35;
}

.dynamics-page__card-content {
  font-size: 0.9375rem;
  color: #515154;
  line-height: 1.5;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.dynamics-page__more {
  display: flex;
  justify-content: center;
  padding: 24px 0 0;
}

.dynamics-page__more .dynamics-page__btn-secondary {
  min-width: 120px;
}

@media (max-width: 768px) {
  .dynamics-page__main {
    padding: 24px 20px 32px;
  }

  .dynamics-page__header {
    margin-bottom: 24px;
  }

  .dynamics-page__title {
    font-size: 1.5rem;
  }

  .dynamics-page__card {
    padding: 16px 20px;
  }
}
</style>
