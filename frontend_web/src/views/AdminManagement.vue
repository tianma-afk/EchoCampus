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
        <el-button type="success" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增管理员
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名或邮箱..."
        clearable
        style="width: 260px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button @click="handleSearch">搜索</el-button>
    </div>

    <div class="table-card">
      <el-table
        :data="admins"
        v-loading="loading"
      >
        <template #empty>
          <el-empty description="暂无管理员数据">
            <el-button type="success" @click="openCreate">新增管理员</el-button>
          </el-empty>
        </template>

        <el-table-column label="用户名" min-width="140">
          <template #default="{ row }">
            <span class="admin-name">{{ row.username }}</span>
          </template>
        </el-table-column>

        <el-table-column label="邮箱" min-width="200">
          <template #default="{ row }">
            <span class="email-cell">{{ row.email }}</span>
          </template>
        </el-table-column>

        <el-table-column label="超级管理员" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.is_super ? 'danger' : 'info'" size="small">
              {{ row.is_super ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="140">
          <template #default="{ row }">
            <span class="time-cell">{{ new Date(row.created_at).toLocaleDateString('zh-CN') }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              v-if="!row.is_super"
              size="small"
              type="danger"
              @click="handleDelete(row)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchAdmins"
        />
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.table-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
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

.table-footer {
  display: flex;
  justify-content: center;
  padding: 16px;
  border-top: 1px solid #f3f4f6;
}

/* el-table style overrides */
:deep(.el-table th.el-table__cell) {
  background: #f9fafb;
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

:deep(.el-table .el-table__cell) {
  padding: 12px 16px;
}

:deep(.el-table__body tr:hover > td.el-table__cell) {
  background-color: #f9fafb;
}

:deep(.el-table__body tr) {
  transition: background 0.15s;
}

/* el-pagination style overrides */
:deep(.el-pagination .el-pager li.is-active) {
  background-color: #059669;
}

:deep(.el-pagination .el-pager li:hover) {
  color: #10b981;
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
