<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getUniversityList,
  createUniversity,
  updateUniversity,
  deleteUniversity,
  type UniversityVO,
} from '../../api/university'

const router = useRouter()

const universities = ref<UniversityVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const showModal = ref(false)
const editingId = ref<string | null>(null)
const editName = ref('')
const saving = ref(false)
const error = ref('')

async function fetchList() {
  loading.value = true
  try {
    const res = await getUniversityList(page.value, pageSize.value)
    universities.value = res.data.records
    total.value = res.data.total
  } catch {
    universities.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  editName.value = ''
  error.value = ''
  showModal.value = true
}

function openEdit(uni: UniversityVO) {
  editingId.value = uni.id
  editName.value = uni.name
  error.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editingId.value = null
  editName.value = ''
  error.value = ''
}

async function handleSave() {
  if (!editName.value.trim()) {
    error.value = '请输入大学名称'
    return
  }
  saving.value = true
  error.value = ''
  try {
    if (editingId.value) {
      const res = await updateUniversity(editingId.value, editName.value.trim())
      if (res.code !== "00000") {
        error.value = res.message
        saving.value = false
        return
      }
    } else {
      const res = await createUniversity(editName.value.trim())
      if (res.code !== "00000") {
        error.value = res.message
        saving.value = false
        return
      }
    }
    closeModal()
    await fetchList()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    saving.value = false
  }
}

async function handleDelete(uni: UniversityVO) {
  if (!confirm(`确定要删除「${uni.name}」吗？`)) return
  try {
    const res = await deleteUniversity(uni.id)
    if (res.code !== "00000") {
      alert(res.message)
      return
    }
    await fetchList()
  } catch (e) {
    alert(e instanceof Error ? e.message : '删除失败')
  }
}

function goToDetail(id: string) {
  router.push(`/universities/${id}`)
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="university-management">
    <div class="page-header">
      <div class="page-title">
        <h1>大学管理</h1>
        <span class="page-count">共 {{ total }} 所大学</span>
      </div>
      <div class="page-actions">
        <el-button type="success" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增大学
        </el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table
        :data="universities"
        v-loading="loading"
        highlight-current-row
        @row-click="(row) => goToDetail(row.id)"
      >
        <template #empty>
          <el-empty description="暂无大学数据">
            <el-button type="success" @click="openCreate">新增大学</el-button>
          </el-empty>
        </template>

        <el-table-column label="大学名称" min-width="300">
          <template #default="{ row }">
            <span class="uni-name">{{ row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click.stop="openEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button size="small" type="danger" @click.stop="handleDelete(row)">
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
          @current-change="fetchList"
        />
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-card">
        <h2 class="modal-title">{{ editingId ? '编辑大学' : '新增大学' }}</h2>
        <div class="form-group">
          <label>大学名称</label>
          <input
            v-model="editName"
            type="text"
            placeholder="请输入大学名称"
            @keyup.enter="handleSave"
          />
        </div>
        <div v-if="error" class="form-error">{{ error }}</div>
        <div class="modal-actions">
          <button class="cancel-btn" @click="closeModal">取消</button>
          <button class="submit-btn" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.university-management {
  max-width: 800px;
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

.table-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.uni-name {
  font-weight: 500;
  color: #1f2937;
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
}

:deep(.el-table .el-table__cell) {
  padding: 12px 16px;
}

:deep(.el-table__body tr:hover > td.el-table__cell) {
  background-color: #f9fafb;
}

:deep(.el-table__body tr) {
  cursor: pointer;
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
