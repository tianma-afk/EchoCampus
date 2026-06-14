<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listAdmins, createAdmin, updateAdmin, deleteAdmin } from '../api/admin'
import type { AdminVO, AdminCreateRequest, AdminUpdateRequest } from '../api/admin'

const admins = ref<AdminVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)

const showModal = ref(false)
const editingId = ref<string | null>(null)
const editUsername = ref('')
const editPassword = ref('')
const editEmail = ref('')
const saving = ref(false)
const error = ref('')

async function fetchAdmins() {
  loading.value = true
  try {
    const res = await listAdmins({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
    })
    if (res.code === '00000') {
      admins.value = res.data.records
      total.value = res.data.total
    }
  } catch {
    admins.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  editUsername.value = ''
  editPassword.value = ''
  editEmail.value = ''
  error.value = ''
  showModal.value = true
}

function openEdit(admin: AdminVO) {
  editingId.value = admin.id
  editUsername.value = admin.username
  editPassword.value = ''
  editEmail.value = admin.email
  error.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editingId.value = null
  editUsername.value = ''
  editPassword.value = ''
  editEmail.value = ''
  error.value = ''
}

async function handleSave() {
  if (!editUsername.value.trim()) {
    error.value = '请输入用户名'
    return
  }
  if (!editingId.value && !editPassword.value) {
    error.value = '请输入密码'
    return
  }
  saving.value = true
  error.value = ''
  try {
    if (editingId.value) {
      const body: AdminUpdateRequest = {
        username: editUsername.value.trim() || undefined,
        email: editEmail.value.trim() || undefined,
      }
      if (editPassword.value) {
        body.password = editPassword.value
      }
      const res = await updateAdmin(editingId.value, body)
      if (res.code !== '00000') {
        error.value = res.message
        saving.value = false
        return
      }
    } else {
      const body: AdminCreateRequest = {
        username: editUsername.value.trim(),
        password: editPassword.value,
        email: editEmail.value.trim(),
      }
      const res = await createAdmin(body)
      if (res.code !== '00000') {
        error.value = res.message
        saving.value = false
        return
      }
    }
    closeModal()
    await fetchAdmins()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(admin: AdminVO) {
  if (!confirm(`确认删除管理员「${admin.username}」吗？`)) return
  try {
    const res = await deleteAdmin(admin.id)
    if (res.code !== '00000') {
      alert(res.message)
      return
    }
    await fetchAdmins()
  } catch (e) {
    alert(e instanceof Error ? e.message : '删除失败')
  }
}

function handleSearch() {
  page.value = 1
  fetchAdmins()
}

onMounted(() => {
  fetchAdmins()
})
</script>

<template>
  <div class="admin-management">
    <div class="page-header">
      <div class="page-title">
        <h1>管理员管理</h1>
        <span class="page-count">共 {{ total }} 个管理员</span>
      </div>
      <div class="page-actions">
        <button class="add-btn" @click="openCreate">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          新增管理员
        </button>
      </div>
    </div>

    <div class="filter-bar">
      <input
        v-model="keyword"
        type="text"
        class="search-input"
        placeholder="搜索用户名或邮箱..."
        @keyup.enter="handleSearch"
      />
      <button class="search-btn" @click="handleSearch">搜索</button>
    </div>

    <div class="table-card">
      <table class="data-table" v-if="!loading && admins.length > 0">
        <thead>
          <tr>
            <th>用户名</th>
            <th>邮箱</th>
            <th>超级管理员</th>
            <th>创建时间</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="admin in admins" :key="admin.id">
            <td class="admin-name">{{ admin.username }}</td>
            <td class="email-cell">{{ admin.email }}</td>
            <td>
              <span class="super-tag" :class="admin.is_super ? 'super-yes' : 'super-no'">
                {{ admin.is_super ? '是' : '否' }}
              </span>
            </td>
            <td class="time-cell">{{ new Date(admin.created_at).toLocaleDateString('zh-CN') }}</td>
            <td class="col-actions">
              <button class="action-btn edit-btn" @click="openEdit(admin)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                </svg>
                编辑
              </button>
              <button
                v-if="!admin.is_super"
                class="action-btn delete-btn"
                @click="handleDelete(admin)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                </svg>
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else-if="loading" class="empty-state">加载中...</div>
      <div v-else class="empty-state">暂无管理员数据，点击"新增管理员"开始添加</div>

      <div class="pagination" v-if="total > pageSize">
        <button class="page-btn" :disabled="page <= 1" @click="page--; fetchAdmins()">上一页</button>
        <span class="page-info">{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
        <button class="page-btn" :disabled="page >= Math.ceil(total / pageSize)" @click="page++; fetchAdmins()">下一页</button>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-card">
        <h2 class="modal-title">{{ editingId ? '编辑管理员' : '新增管理员' }}</h2>
        <div class="form-group">
          <label>用户名</label>
          <input
            v-model="editUsername"
            type="text"
            placeholder="2-64个字符"
            @keyup.enter="handleSave"
          />
        </div>
        <div class="form-group">
          <label>{{ editingId ? '新密码（留空则不修改）' : '密码' }}</label>
          <input
            v-model="editPassword"
            type="password"
            :placeholder="editingId ? '留空则不修改密码' : '6-128个字符'"
            @keyup.enter="handleSave"
          />
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <input
            v-model="editEmail"
            type="email"
            placeholder="请输入邮箱地址"
            @keyup.enter="handleSave"
          />
        </div>
        <div v-if="error" class="form-error">{{ error }}</div>
        <div class="modal-actions">
          <button class="cancel-btn" @click="closeModal">取消</button>
          <button class="submit-btn" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-management {
  max-width: 900px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-title h1 {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.page-count {
  font-size: 13px;
  color: #9ca3af;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #059669;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.add-btn:hover {
  background: #047857;
}

.add-btn svg {
  width: 16px;
  height: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.search-input {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #374151;
  outline: none;
  width: 260px;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #10b981;
}

.search-input::placeholder {
  color: #d1d5db;
}

.search-btn {
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.search-btn:hover {
  border-color: #10b981;
  color: #10b981;
}

.table-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  text-align: left;
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.data-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
  font-size: 14px;
  color: #374151;
}

.data-table tbody tr:hover {
  background: #f9fafb;
}

.admin-name {
  font-weight: 500;
  color: #1f2937;
}

.email-cell {
  color: #6b7280;
}

.time-cell {
  font-size: 13px;
  color: #6b7280;
  white-space: nowrap;
}

.super-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.super-tag.super-yes {
  background: #fee2e2;
  color: #dc2626;
}

.super-tag.super-no {
  background: #f3f4f6;
  color: #9ca3af;
}

.col-actions {
  width: 190px;
  text-align: right;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  margin-left: 8px;
}

.action-btn svg {
  width: 14px;
  height: 14px;
}

.edit-btn {
  color: #6b7280;
}

.edit-btn:hover {
  border-color: #10b981;
  color: #10b981;
}

.delete-btn {
  color: #ef4444;
}

.delete-btn:hover {
  background: #fef2f2;
  border-color: #ef4444;
}

.empty-state {
  padding: 48px 16px;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px;
  border-top: 1px solid #f3f4f6;
}

.page-btn {
  padding: 6px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #374151;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #10b981;
  color: #10b981;
}

.page-btn:disabled {
  color: #d1d5db;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #6b7280;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  width: 420px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.form-group input {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  outline: none;
  transition: border-color 0.2s;
}

.form-group input:focus {
  border-color: #10b981;
}

.form-error {
  padding: 10px 14px;
  background: #fef2f2;
  border-radius: 8px;
  color: #ef4444;
  font-size: 13px;
  margin-bottom: 16px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cancel-btn,
.submit-btn {
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background: #fff;
  border: 1px solid #e5e7eb;
  color: #6b7280;
}

.cancel-btn:hover {
  border-color: #d1d5db;
  color: #374151;
}

.submit-btn {
  background: #059669;
  border: none;
  color: #fff;
}

.submit-btn:hover:not(:disabled) {
  background: #047857;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
