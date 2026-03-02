<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  fetchUsers,
  createUser,
  updateUser,
  updateUserStatus,
  updateUserRole,
  deleteUser,
  type User,
  type UserPage,
  type UserRole,
  type UserStatus,
  type AdminUserRequest,
} from '../../api/admin'

const loading = ref(false)
const error = ref('')
const success = ref('')
const users = ref<UserPage | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const filterRole = ref<UserRole | ''>('')
const filterStatus = ref<UserStatus | ''>('')
const showCreateModal = ref(false)
const showEditModal = ref(false)
const editingUser = ref<User | null>(null)
const showDeleteConfirm = ref(false)
const deletingUser = ref<User | null>(null)
const formData = ref<AdminUserRequest>({
  fullName: '',
  email: '',
  phone: '',
  role: 'STUDENT',
  status: 'ACTIVE',
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

const loadUsers = async () => {
  if (!adminUserId.value) {
    error.value = '请先登录'
    return
  }
  loading.value = true
  error.value = ''
  try {
    users.value = await fetchUsers({
      adminUserId: adminUserId.value,
      page: currentPage.value,
      size: pageSize.value,
      role: filterRole.value || undefined,
      status: filterStatus.value || undefined,
      keyword: searchKeyword.value || undefined,
    })
  } catch (err: any) {
    error.value = err.message || '加载用户列表失败'
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  formData.value = {
    fullName: '',
    email: '',
    phone: '',
    role: 'STUDENT',
    status: 'ACTIVE',
  }
  showCreateModal.value = true
}

const handleEdit = (user: User) => {
  editingUser.value = user
  formData.value = {
    fullName: user.fullName || '',
    email: user.email || '',
    phone: user.phone || '',
    role: user.role,
    status: user.status,
  }
  showEditModal.value = true
}

const handleSave = async () => {
  if (!adminUserId.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    if (editingUser.value) {
      await updateUser(adminUserId.value, editingUser.value.id, formData.value)
      success.value = '用户信息已成功更新'
    } else {
      if (!formData.value.username) {
        error.value = '用户名不能为空'
        return
      }
      await createUser(adminUserId.value, formData.value)
      success.value = '用户已成功创建'
    }
    showCreateModal.value = false
    showEditModal.value = false
    editingUser.value = null
    await loadUsers()
    // 3秒后清除成功消息
    setTimeout(() => {
      success.value = ''
    }, 3000)
  } catch (err: any) {
    error.value = err.message || '保存失败'
  } finally {
    loading.value = false
  }
}

const handleDelete = (user: User) => {
  deletingUser.value = user
  showDeleteConfirm.value = true
}

const confirmDelete = async () => {
  if (!adminUserId.value || !deletingUser.value) {
    error.value = '缺少必要参数'
    return
  }
  
  loading.value = true
  error.value = ''
  success.value = ''
  
  try {
    console.log('开始删除用户:', deletingUser.value.id)
    const result = await deleteUser(adminUserId.value, deletingUser.value.id)
    console.log('删除结果:', result)
    
    // 删除成功
    const userName = deletingUser.value.fullName || deletingUser.value.username
    success.value = `用户 "${userName}" 已成功删除`
    
    // 关闭对话框
    showDeleteConfirm.value = false
    deletingUser.value = null
    
    // 刷新用户列表
    await loadUsers()
    
    // 3秒后清除成功消息
    setTimeout(() => {
      success.value = ''
    }, 3000)
  } catch (err: any) {
    console.error('删除用户失败:', err)
    // 显示错误信息，保持对话框打开
    error.value = err.message || err.toString() || '删除失败，请稍后重试'
    // 错误信息也显示在确认对话框中
  } finally {
    loading.value = false
  }
}

const cancelDelete = () => {
  showDeleteConfirm.value = false
  deletingUser.value = null
}

const handleStatusChange = async (userId: number, status: UserStatus) => {
  if (!adminUserId.value) return
  loading.value = true
  try {
    await updateUserStatus(adminUserId.value, userId, status)
    await loadUsers()
  } catch (err: any) {
    error.value = err.message || '更新状态失败'
  } finally {
    loading.value = false
  }
}

const handleRoleChange = async (userId: number, role: UserRole) => {
  if (!adminUserId.value) return
  loading.value = true
  try {
    await updateUserRole(adminUserId.value, userId, role)
    await loadUsers()
  } catch (err: any) {
    error.value = err.message || '更新角色失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="admin-users">
    <div class="page-header">
      <h1>用户管理</h1>
      <p class="subtitle">管理系统中的所有用户账户</p>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filters">
      <input
        v-model="searchKeyword"
        type="text"
        placeholder="搜索用户名、姓名或邮箱..."
        class="search-input"
        @keyup.enter="loadUsers"
      />
      <select v-model="filterRole" class="filter-select" @change="loadUsers">
        <option value="">所有角色</option>
        <option value="STUDENT">学生</option>
        <option value="TEACHER">教师</option>
        <option value="EMPLOYER">企业</option>
        <option value="ADMIN">管理员</option>
      </select>
      <select v-model="filterStatus" class="filter-select" @change="loadUsers">
        <option value="">所有状态</option>
        <option value="ACTIVE">启用</option>
        <option value="DISABLED">禁用</option>
      </select>
      <button class="btn-primary" @click="loadUsers">搜索</button>
      <button class="btn-primary" @click="handleCreate">创建用户</button>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-if="success" class="success-message">{{ success }}</div>

    <!-- 用户列表 -->
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="users" class="users-table">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>姓名</th>
            <th>邮箱</th>
            <th>角色</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users.records" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td class="name-cell">{{ user.fullName || '-' }}</td>
            <td>{{ user.email || '-' }}</td>
            <td>
              <select
                :value="user.role"
                class="role-select"
                @change="handleRoleChange(user.id, ($event.target as HTMLSelectElement).value as UserRole)"
              >
                <option value="STUDENT">学生</option>
                <option value="TEACHER">教师</option>
                <option value="EMPLOYER">企业</option>
                <option value="ADMIN">管理员</option>
              </select>
            </td>
            <td>
              <select
                :value="user.status"
                class="status-select"
                @change="handleStatusChange(user.id, ($event.target as HTMLSelectElement).value as UserStatus)"
              >
                <option value="ACTIVE">启用</option>
                <option value="DISABLED">禁用</option>
              </select>
            </td>
            <td>{{ user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '-' }}</td>
            <td>
              <div class="action-buttons">
                <button class="btn-edit" @click="handleEdit(user)">编辑</button>
                <button class="btn-delete" @click="handleDelete(user)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="pagination">
        <button
          :disabled="currentPage <= 1"
          @click="currentPage--; loadUsers()"
        >
          上一页
        </button>
        <span>第 {{ currentPage }} / {{ users.pages || 1 }} 页</span>
        <button
          :disabled="currentPage >= (users.pages || 1)"
          @click="currentPage++; loadUsers()"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 创建/编辑模态框 -->
    <div v-if="showCreateModal || showEditModal" class="modal-overlay" @click.self="showCreateModal = false; showEditModal = false">
      <div class="modal-content">
        <h2>{{ editingUser ? '编辑用户' : '创建用户' }}</h2>
        <div class="form-group">
          <label v-if="!editingUser">用户名 *</label>
          <input
            v-if="!editingUser"
            v-model="formData.username"
            type="text"
            placeholder="用户名"
            required
          />
        </div>
        <div class="form-group">
          <label v-if="!editingUser">密码 {{ editingUser ? '(留空不修改)' : '*' }}</label>
          <input
            v-model="formData.password"
            type="password"
            :placeholder="editingUser ? '留空不修改密码' : '密码'"
            :required="!editingUser"
          />
        </div>
        <div class="form-group">
          <label>姓名 *</label>
          <input v-model="formData.fullName" type="text" placeholder="姓名" required />
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="formData.email" type="email" placeholder="邮箱" />
        </div>
        <div class="form-group">
          <label>电话</label>
          <input v-model="formData.phone" type="tel" placeholder="电话" />
        </div>
        <div class="form-group">
          <label>角色 *</label>
          <select v-model="formData.role" required>
            <option value="STUDENT">学生</option>
            <option value="TEACHER">教师</option>
            <option value="EMPLOYER">企业</option>
            <option value="ADMIN">管理员</option>
          </select>
        </div>
        <div class="form-group">
          <label>状态 *</label>
          <select v-model="formData.status" required>
            <option value="ACTIVE">启用</option>
            <option value="DISABLED">禁用</option>
          </select>
        </div>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showCreateModal = false; showEditModal = false">取消</button>
          <button class="btn-primary" @click="handleSave">保存</button>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="cancelDelete">
      <div class="confirm-dialog">
        <div class="confirm-header">
          <h3>确认删除</h3>
        </div>
        <div class="confirm-body">
          <p>确定要删除用户 <strong>{{ deletingUser?.fullName || deletingUser?.username }}</strong> 吗？</p>
          <p class="confirm-warning">此操作将永久删除该用户及其所有相关数据，包括：</p>
          <ul class="confirm-list">
            <li v-if="deletingUser?.role === 'STUDENT'">学生资料、简历、求职申请、面试记录等</li>
            <li v-if="deletingUser?.role === 'TEACHER'">教师信息、指导记录等</li>
            <li v-if="deletingUser?.role === 'EMPLOYER'">企业信息、岗位发布等</li>
            <li>系统通知、搜索历史等</li>
          </ul>
          <p class="confirm-warning">此操作不可恢复！</p>
          <div v-if="error" class="confirm-error">{{ error }}</div>
        </div>
        <div class="confirm-actions">
          <button class="btn-secondary" @click="cancelDelete" :disabled="loading">取消</button>
          <button class="btn-delete-confirm" @click="confirmDelete" :disabled="loading">
            {{ loading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-users {
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

.filters {
  display: flex;
  gap: 1rem;
  margin-bottom: 2.5rem;
  flex-wrap: wrap;
  align-items: center;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(30px);
  -webkit-backdrop-filter: saturate(180%) blur(30px);
  border-radius: 18px;
  padding: 1.25rem 1.75rem;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.06),
    0 2px 8px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.search-input {
  flex: 1;
  min-width: 280px;
  padding: 0.875rem 1.25rem;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  font-size: 0.9375rem;
  background: rgba(255, 255, 255, 0.9);
  color: #1d1d1f;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  font-family: inherit;
}

.search-input:focus {
  outline: none;
  border-color: #0071e3;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.1);
}

.search-input::placeholder {
  color: #86868b;
}

.filter-select {
  padding: 0.875rem 1.25rem;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  font-size: 0.9375rem;
  background: rgba(255, 255, 255, 0.9);
  color: #1d1d1f;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  font-family: inherit;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%231d1d1f' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 1rem center;
  padding-right: 2.75rem;
}

.filter-select:focus {
  outline: none;
  border-color: #0071e3;
  background-color: #ffffff;
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.1);
}

.btn-primary {
  padding: 0.875rem 1.75rem;
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

.error-message {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.1), rgba(255, 45, 85, 0.1));
  color: #ff3b30;
  padding: 1.25rem 1.5rem;
  border-radius: 16px;
  margin-bottom: 1.5rem;
  border: 1px solid rgba(255, 59, 48, 0.2);
  font-size: 0.9375rem;
  font-weight: 500;
  backdrop-filter: blur(10px);
}

.success-message {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.1), rgba(48, 209, 88, 0.1));
  color: #34c759;
  padding: 1.25rem 1.5rem;
  border-radius: 16px;
  margin-bottom: 1.5rem;
  border: 1px solid rgba(52, 199, 89, 0.2);
  font-size: 0.9375rem;
  font-weight: 500;
  backdrop-filter: blur(10px);
}

.loading {
  text-align: center;
  padding: 5rem 3rem;
  color: #86868b;
  font-size: 1.125rem;
  font-weight: 500;
}

.users-table {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: saturate(180%) blur(30px);
  -webkit-backdrop-filter: saturate(180%) blur(30px);
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.06),
    0 2px 8px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: linear-gradient(180deg, rgba(0, 113, 227, 0.04) 0%, rgba(94, 92, 230, 0.02) 100%);
  border-bottom: 2px solid rgba(0, 0, 0, 0.06);
}

th,
td {
  padding: 1.25rem 1.5rem;
  text-align: left;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

th {
  font-weight: 600;
  color: #1d1d1f;
  font-size: 0.8125rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

tbody tr {
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

tbody tr:hover {
  background: rgba(0, 113, 227, 0.02);
}

tbody tr:last-child td {
  border-bottom: none;
}

td {
  color: #1d1d1f;
  font-size: 0.9375rem;
}

.name-cell {
  white-space: nowrap;
  writing-mode: horizontal-tb;
  text-orientation: mixed;
  direction: ltr;
}

.role-select,
.status-select {
  padding: 0.625rem 1rem;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  font-size: 0.875rem;
  background: rgba(255, 255, 255, 0.9);
  color: #1d1d1f;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  font-family: inherit;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 10 10'%3E%3Cpath fill='%231d1d1f' d='M5 8L1 4h8z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  padding-right: 2.5rem;
}

.role-select:focus,
.status-select:focus {
  outline: none;
  border-color: #0071e3;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.1);
}

.action-buttons {
  display: flex;
  gap: 0.625rem;
  align-items: center;
}

.btn-edit,
.btn-delete {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 10px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.btn-edit {
  background: linear-gradient(135deg, #0071e3, #5e5ce6);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 113, 227, 0.2);
}

.btn-edit:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 113, 227, 0.3);
}

.btn-delete {
  background: linear-gradient(135deg, #ff3b30, #ff2d55);
  color: white;
  box-shadow: 0 2px 8px rgba(255, 59, 48, 0.2);
}

.btn-delete:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.3);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1.5rem;
  padding: 2rem;
  background: rgba(0, 0, 0, 0.02);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.pagination button {
  padding: 0.875rem 1.75rem;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  color: #1d1d1f;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  letter-spacing: -0.01em;
}

.pagination button:hover:not(:disabled) {
  background: #ffffff;
  border-color: #0071e3;
  color: #0071e3;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 113, 227, 0.15);
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
}

.pagination span {
  color: #86868b;
  font-size: 0.9375rem;
  font-weight: 500;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: saturate(180%) blur(40px);
  -webkit-backdrop-filter: saturate(180%) blur(40px);
  border-radius: 24px;
  padding: 2.5rem;
  max-width: 580px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.2),
    0 8px 24px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  animation: slideUp 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(40px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-content h2 {
  margin: 0 0 2rem 0;
  font-size: 1.75rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: #1d1d1f;
  background: linear-gradient(135deg, #1d1d1f 0%, #424245 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #0f172a;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 0.875rem 1.25rem;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  font-size: 0.9375rem;
  background: rgba(255, 255, 255, 0.9);
  color: #1d1d1f;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  font-family: inherit;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #0071e3;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.1);
}

.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2.5rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.btn-secondary {
  padding: 0.875rem 1.75rem;
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

.btn-secondary:hover {
  background: rgba(0, 0, 0, 0.06);
  border-color: rgba(0, 0, 0, 0.12);
  transform: translateY(-1px);
}

/* 删除确认对话框 */
.confirm-dialog {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: saturate(180%) blur(40px);
  -webkit-backdrop-filter: saturate(180%) blur(40px);
  border-radius: 24px;
  padding: 0;
  max-width: 500px;
  width: 90%;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 
    0 24px 64px rgba(0, 0, 0, 0.2),
    0 8px 24px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  animation: slideUp 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  overflow: hidden;
}

.confirm-header {
  padding: 2rem 2rem 1.5rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.confirm-header h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #1d1d1f;
}

.confirm-body {
  padding: 2rem;
}

.confirm-body p {
  margin: 0 0 1rem 0;
  color: #1d1d1f;
  font-size: 0.9375rem;
  line-height: 1.6;
}

.confirm-body p strong {
  color: #0071e3;
  font-weight: 600;
}

.confirm-warning {
  color: #ff3b30 !important;
  font-weight: 500;
  margin-top: 1rem !important;
}

.confirm-list {
  margin: 1rem 0;
  padding-left: 1.5rem;
  color: #86868b;
  font-size: 0.875rem;
  line-height: 1.8;
}

.confirm-list li {
  margin-bottom: 0.5rem;
}

.confirm-error {
  margin-top: 1.5rem;
  padding: 1rem;
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.1), rgba(255, 45, 85, 0.1));
  color: #ff3b30;
  border-radius: 12px;
  border: 1px solid rgba(255, 59, 48, 0.2);
  font-size: 0.875rem;
  font-weight: 500;
}

.confirm-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  padding: 1.5rem 2rem;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(0, 0, 0, 0.02);
}

.btn-delete-confirm {
  padding: 0.875rem 1.75rem;
  background: linear-gradient(135deg, #ff3b30, #ff2d55);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  font-size: 0.9375rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  box-shadow: 
    0 4px 16px rgba(255, 59, 48, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  letter-spacing: -0.01em;
}

.btn-delete-confirm:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 
    0 6px 20px rgba(255, 59, 48, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  background: linear-gradient(135deg, #ff453a, #ff375f);
}

.btn-delete-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 响应式设计 */
@media (max-width: 1600px) {
  .admin-users {
    padding: 4rem 4rem;
  }
}

@media (max-width: 1400px) {
  .admin-users {
    padding: 3rem 3rem;
  }
}

@media (max-width: 1024px) {
  .admin-users {
    padding: 2.5rem 2rem;
  }
  
  .page-header h1 {
    font-size: 3rem;
  }
  
  .filters {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-input {
    min-width: 100%;
  }
  
  table {
    font-size: 0.875rem;
  }
  
  th,
  td {
    padding: 1rem 1.25rem;
  }
}

@media (max-width: 768px) {
  .admin-users {
    padding: 2rem 1.5rem;
  }
  
  .page-header h1 {
    font-size: 2.5rem;
  }
  
  .subtitle {
    font-size: 1.125rem;
  }
  
  .users-table {
    overflow-x: auto;
  }
  
  table {
    min-width: 800px;
  }
}
</style>

