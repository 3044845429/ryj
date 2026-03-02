<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getPublicDynamics, type DynamicsItem, type PublicDynamicsPage } from '@/api/public'

const loading = ref(true)
const error = ref('')
const page = ref<PublicDynamicsPage | null>(null)
const currentPage = ref(1)
const pageSize = 20

const loadData = async (p = 1) => {
  loading.value = true
  error.value = ''
  try {
    page.value = await getPublicDynamics({
      page: p,
      size: pageSize,
      category: 'GUIDANCE', // 使用后端 GUIDANCE 分类承载就业指导 / 政策信息
    })
    currentPage.value = p
  } catch (e: any) {
    error.value = e?.message || '加载就业政策失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const formatDate = (value: string | null) => {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

onMounted(() => {
  loadData(1)
})
</script>

<template>
  <div class="teacher-page">
    <main class="teacher-body">
      <div class="body-inner">
        <header class="top-bar">
          <div class="top-bar__title">
            <h1>就业政策</h1>
            <p class="subtitle">集中查看学校及公共渠道发布的就业政策与指导文章</p>
          </div>
        </header>

        <div v-if="loading" class="card loading-card">
          <div class="spinner" />
          <p>正在加载就业政策...</p>
        </div>

        <div v-else-if="error" class="card error-card">
          <p>{{ error }}</p>
        </div>

        <div v-else-if="!page || !page.records.length" class="card empty-card">
          <p>暂时没有符合条件的就业政策或指导文章。</p>
          <small>可通过管理员后台或资讯动态模块继续补充内容。</small>
        </div>

        <section v-else class="card policy-section">
          <ul class="policy-list">
            <li v-for="item in page.records" :key="item.id" class="policy-item">
              <div class="policy-main">
                <h3 class="policy-title">{{ item.title }}</h3>
                <p v-if="item.content" class="policy-content">
                  {{ item.content.length > 160 ? `${item.content.slice(0, 160)}...` : item.content }}
                </p>
              </div>
              <div class="policy-meta">
                <span class="policy-date">{{ formatDate(item.createdAt) }}</span>
                <span v-if="item.category" class="policy-tag">{{ item.category }}</span>
              </div>
            </li>
          </ul>
          <div v-if="page.pages > 1" class="pagination">
            <button
              class="pager-btn"
              type="button"
              :disabled="currentPage <= 1"
              @click="loadData(currentPage - 1)"
            >
              上一页
            </button>
            <span class="pager-info">第 {{ currentPage }} / {{ page.pages }} 页</span>
            <button
              class="pager-btn"
              type="button"
              :disabled="currentPage >= page.pages"
              @click="loadData(currentPage + 1)"
            >
              下一页
            </button>
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
  max-width: 960px;
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

.policy-section {
  padding-top: 1.2rem;
}

.policy-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.policy-item {
  display: flex;
  justify-content: space-between;
  gap: 1.25rem;
  padding: 0.9rem 0;
  border-bottom: 1px solid rgba(209, 213, 219, 0.7);
}

.policy-main {
  flex: 1;
  min-width: 0;
}

.policy-title {
  margin: 0 0 0.2rem;
  font-size: 1rem;
  font-weight: 600;
  color: #111827;
}

.policy-content {
  margin: 0;
  font-size: 0.9rem;
  color: #4b5563;
}

.policy-meta {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
  font-size: 0.8rem;
  color: #9ca3af;
}

.policy-tag {
  padding: 0.15rem 0.6rem;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.06);
  color: #2563eb;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1.25rem;
  font-size: 0.85rem;
  color: #6b7280;
}

.pager-btn {
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.7);
  background: #f9fafb;
  color: #374151;
  cursor: pointer;
  font-size: 0.85rem;
}

.pager-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pager-info {
  min-width: 110px;
  text-align: center;
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

  .policy-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .policy-meta {
    flex-direction: row;
    gap: 0.5rem;
  }
}
</style>

