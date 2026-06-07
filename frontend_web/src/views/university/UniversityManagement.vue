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
        <button class="add-btn" @click="openCreate">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          新增大学
        </button>
      </div>
    </div>

    <div class="table-card">
      <table class="data-table" v-if="!loading && universities.length > 0">
        <thead>
          <tr>
            <th>大学名称</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="uni in universities" :key="uni.id" @click="goToDetail(uni.id)" class="clickable-row">
            <td class="uni-name">{{ uni.name }}</td>
            <td class="col-actions">
              <button class="action-btn edit-btn" @click.stop="openEdit(uni)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                </svg>
                编辑
              </button>
              <button class="action-btn delete-btn" @click.stop="handleDelete(uni)">
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
      <div v-else class="empty-state">暂无大学数据，点击"新增大学"开始添加</div>

      <div class="pagination" v-if="total > pageSize">
        <button class="page-btn" :disabled="page <= 1" @click="page--; fetchList()">上一页</button>
        <span class="page-info">{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
        <button class="page-btn" :disabled="page >= Math.ceil(total / pageSize)" @click="page++; fetchList()">下一页</button>
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
            ref="nameInput"
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

.clickable-row {
  cursor: pointer;
  transition: background 0.15s;
}

.clickable-row:hover {
  background: #f9fafb;
}

.uni-name {
  font-weight: 500;
  color: #1f2937;
}

.col-actions {
  width: 200px;
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
