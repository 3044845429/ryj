<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  fetchProfileUpdateRequests,
  approveProfileUpdateRequest,
  rejectProfileUpdateRequest,
  type ProfileUpdateRequestPage,
  type ProfileUpdateRole,
  type ProfileUpdateStatus,
} from '../../api/admin'

const loading = ref(false)
const error = ref('')
const success = ref('')
const pageData = ref<ProfileUpdateRequestPage | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const filterRole = ref<ProfileUpdateRole | ''>('')
const filterStatus = ref<ProfileUpdateStatus | ''>('PENDING')

const showDetailModal = ref(false)
const detailRecord = ref<any | null>(null)
const reviewComment = ref('')
const processing = ref(false)

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

const loadRequests = async () => {
  if (!adminUserId.value) {
    error.value = '请先登录管理员账户'
    return
  }
  loading.value = true
  error.value = ''
  try {
    pageData.value = await fetchProfileUpdateRequests({
      adminUserId: adminUserId.value,
      page: currentPage.value,
      size: pageSize.value,
      role: filterRole.value || undefined,
      status: filterStatus.value || undefined,
    })
  } catch (err: any) {
    error.value = err.message || '加载资料更新申请失败'
  } finally {
    loading.value = false
  }
}

const openDetail = (record: any) => {
  detailRecord.value = {
    ...record,
    parsedPayload: safeParsePayload(record.payload),
  }
  reviewComment.value = ''
  showDetailModal.value = true
}

const safeParsePayload = (payload: string | null | undefined) => {
  if (!payload) return null
  try {
    return JSON.parse(payload)
  } catch {
    return null
  }
}

const mainInfoText = (record: any) => {
  const parsed = safeParsePayload(record.payload)
  if (!parsed) return ''
  if (record.role === 'EMPLOYER') {
    return parsed.companyName || ''
  }
  if (record.role === 'TEACHER') {
    return parsed.department || parsed.major || ''
  }
  return ''
}

const handleApprove = async (recordId?: number) => {
  if (!adminUserId.value) return
  const targetId = recordId ?? detailRecord.value?.id
  if (!targetId) return
  processing.value = true
  error.value = ''
  success.value = ''
  try {
    await approveProfileUpdateRequest(adminUserId.value, targetId, reviewComment.value || '')
    success.value = '资料更新申请已通过'
    showDetailModal.value = false
    detailRecord.value = null
    await loadRequests()
    setTimeout(() => {
      success.value = ''
    }, 2500)
  } catch (err: any) {
    error.value = err.message || '审批通过失败'
  } finally {
    processing.value = false
  }
}

const handleReject = async (recordId?: number) => {
  if (!adminUserId.value) return
  const targetId = recordId ?? detailRecord.value?.id
  if (!targetId) return
  if (!reviewComment.value) {
    if (!recordId) {
      error.value = '请先填写驳回原因'
    }
    return
  }
  processing.value = true
  error.value = ''
  success.value = ''
  try {
    await rejectProfileUpdateRequest(adminUserId.value, targetId, reviewComment.value)
    success.value = '资料更新申请已驳回'
    showDetailModal.value = false
    detailRecord.value = null
    await loadRequests()
    setTimeout(() => {
      success.value = ''
    }, 2500)
  } catch (err: any) {
    error.value = err.message || '驳回失败'
  } finally {
    processing.value = false
  }
}

onMounted(() => {
  loadRequests()
})
</script>

<template>
  <div class="admin-profile-approvals">
    <div class="page-header">
      <h1>资料审核</h1>
      <p class="subtitle">审核教师与企业用户提交的个人资料变更申请</p>
    </div>

    <div class="filters">
      <select v-model="filterRole" class="filter-select" @change="() => { currentPage = 1; loadRequests() }">
        <option value="">所有角色</option>
        <option value="TEACHER">教师</option>
        <option value="EMPLOYER">企业</option>
      </select>
      <select v-model="filterStatus" class="filter-select" @change="() => { currentPage = 1; loadRequests() }">
        <option value="">所有状态</option>
        <option value="PENDING">待审核</option>
        <option value="APPROVED">已通过</option>
        <option value="REJECTED">已驳回</option>
      </select>
      <button class="btn-primary" @click="() => { currentPage = 1; loadRequests() }">刷新</button>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-if="success" class="success-message">{{ success }}</div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="pageData && pageData.records.length" class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>角色</th>
            <th>用户ID</th>
            <th>主要信息</th>
            <th>状态</th>
            <th>提交时间</th>
            <th>审核时间</th>
            <th>审核意见</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in pageData.records" :key="record.id">
            <td>{{ record.id }}</td>
            <td>{{ record.role === 'TEACHER' ? '教师' : '企业' }}</td>
            <td>{{ record.userId }}</td>
            <td>{{ mainInfoText(record) }}</td>
            <td>
              <span
                class="status-badge"
                :class="{
                  pending: record.status === 'PENDING',
                  approved: record.status === 'APPROVED',
                  rejected: record.status === 'REJECTED',
                }"
              >
                {{ record.status === 'PENDING' ? '待审核' : record.status === 'APPROVED' ? '已通过' : '已驳回' }}
              </span>
            </td>
            <td>{{ record.createdAt || '-' }}</td>
            <td>{{ record.reviewedAt || '-' }}</td>
            <td class="review-comment-cell">
              <span v-if="record.reviewComment" :title="record.reviewComment">
                {{ record.reviewComment.length > 16 ? record.reviewComment.slice(0, 16) + '...' : record.reviewComment }}
              </span>
              <span v-else>-</span>
            </td>
            <td>
              <button class="btn-link" @click="openDetail(record)">详情</button>
              <button
                v-if="record.status === 'PENDING'"
                class="btn-link approve"
                @click="handleApprove(record.id)"
              >
                直接通过
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button :disabled="currentPage <= 1" @click="() => { currentPage--; loadRequests() }">上一页</button>
        <span>第 {{ currentPage }} / {{ pageData.pages || 1 }} 页</span>
        <button
          :disabled="pageData.pages && currentPage >= pageData.pages"
          @click="() => { currentPage++; loadRequests() }"
        >
          下一页
        </button>
      </div>
    </div>
    <div v-else-if="!loading" class="empty-state">当前没有资料更新申请</div>

    <!-- 详情弹窗 -->
    <div v-if="showDetailModal && detailRecord" class="modal-backdrop">
      <div class="modal">
        <h2>资料更新详情（{{ detailRecord.role === 'TEACHER' ? '教师' : '企业' }}）</h2>
        <p class="modal-subtitle">用户ID：{{ detailRecord.userId }}，申请编号：{{ detailRecord.id }}</p>

        <div class="payload-view">
          <h3>变更内容</h3>
          <pre v-if="detailRecord.parsedPayload">{{ JSON.stringify(detailRecord.parsedPayload, null, 2) }}</pre>
          <p v-else class="empty-payload">无法解析的内容</p>
        </div>

        <div class="review-section" v-if="detailRecord.status === 'PENDING'">
          <label class="review-label">
            审核意见（可选）：
            <textarea v-model="reviewComment" rows="3" placeholder="填写通过/驳回的原因，便于用户理解" />
          </label>
          <div class="review-actions">
            <button class="btn-primary" :disabled="processing" @click="handleApprove()">通过</button>
            <button class="btn-danger" :disabled="processing || !reviewComment" @click="handleReject()">驳回</button>
            <button class="btn-secondary" :disabled="processing" @click="() => { showDetailModal = false }">
              关闭
            </button>
          </div>
        </div>
        <div v-else class="review-readonly">
          <p>
            当前状态：
            <strong>{{
              detailRecord.status === 'APPROVED' ? '已通过' : detailRecord.status === 'REJECTED' ? '已驳回' : '未知'
            }}</strong>
          </p>
          <p>审核时间：{{ detailRecord.reviewedAt || '-' }}</p>
          <p>审核意见：{{ detailRecord.reviewComment || '-' }}</p>
          <button class="btn-secondary" @click="() => { showDetailModal = false }">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-profile-approvals {
  padding: 24px;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
}

.subtitle {
  margin-top: 4px;
  color: #666;
  font-size: 14px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.filter-select {
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #dcdcdc;
}

.btn-primary {
  padding: 6px 12px;
  border-radius: 4px;
  border: none;
  background-color: #409eff;
  color: #fff;
  cursor: pointer;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 6px 12px;
  border-radius: 4px;
  border: 1px solid #dcdcdc;
  background-color: #fff;
  color: #333;
  cursor: pointer;
}

.btn-danger {
  padding: 6px 12px;
  border-radius: 4px;
  border: none;
  background-color: #f56c6c;
  color: #fff;
  cursor: pointer;
}

.btn-link {
  background: none;
  border: none;
  color: #409eff;
  cursor: pointer;
  padding: 0 4px;
}

.btn-link.approve {
  color: #67c23a;
}

.error-message {
  margin-bottom: 12px;
  color: #f56c6c;
}

.success-message {
  margin-bottom: 12px;
  color: #67c23a;
}

.loading,
.empty-state {
  margin-top: 24px;
  color: #888;
}

.table-wrapper {
  margin-top: 8px;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  background-color: #fff;
}

th,
td {
  padding: 8px 10px;
  border-bottom: 1px solid #eee;
  text-align: left;
  font-size: 13px;
}

th {
  background-color: #f8f8f8;
  font-weight: 500;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.status-badge.pending {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.status-badge.approved {
  background-color: #f0f9eb;
  color: #67c23a;
}

.status-badge.rejected {
  background-color: #fef0f0;
  color: #f56c6c;
}

.review-comment-cell {
  max-width: 180px;
}

.pagination {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination button {
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid #dcdcdc;
  background-color: #fff;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  width: 640px;
  max-width: 95vw;
  max-height: 90vh;
  background-color: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  overflow: auto;
}

.modal h2 {
  margin-top: 0;
}

.modal-subtitle {
  margin-top: 4px;
  margin-bottom: 12px;
  color: #666;
  font-size: 13px;
}

.payload-view {
  margin-bottom: 16px;
}

.payload-view h3 {
  margin: 0 0 4px;
}

.payload-view pre {
  background-color: #f8f8f8;
  padding: 10px;
  border-radius: 4px;
  max-height: 260px;
  overflow: auto;
  font-size: 12px;
}

.empty-payload {
  color: #999;
  font-size: 13px;
}

.review-section {
  margin-top: 8px;
}

.review-label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
}

.review-label textarea {
  width: 100%;
  margin-top: 4px;
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #dcdcdc;
  resize: vertical;
}

.review-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.review-readonly p {
  margin: 4px 0;
  font-size: 13px;
}
</style>

