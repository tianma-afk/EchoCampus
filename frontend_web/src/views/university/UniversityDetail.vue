<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getUniversityById,
  updateUniversity,
  deleteUniversity,
  type UniversityVO,
} from '../../api/university'
import {
  getCampusList,
  createCampus,
  updateCampus,
  deleteCampus,
  type CampusVO,
} from '../../api/campus'

const route = useRoute()
const router = useRouter()
const universityId = route.params.id as string

const university = ref<UniversityVO | null>(null)
const loading = ref(false)

const campuses = ref<CampusVO[]>([])
const campusTotal = ref(0)
const campusPage = ref(1)
const campusPageSize = ref(10)
const campusLoading = ref(false)

const editingUniName = ref(false)
const editUniName = ref('')
const savingUni = ref(false)

const showCampusModal = ref(false)
const editingCampusId = ref<string | null>(null)
const editCampusName = ref('')
const savingCampus = ref(false)
const campusError = ref('')

async function fetchUniversity() {
  loading.value = true
  try {
    const res = await getUniversityById(universityId)
    university.value = res.data
    editUniName.value = res.data.name
  } catch {
    university.value = null
  } finally {
    loading.value = false
  }
}

async function fetchCampuses() {
  campusLoading.value = true
  try {
    const res = await getCampusList(campusPage.value, campusPageSize.value, universityId)
    campuses.value = res.data.records
    campusTotal.value = res.data.total
  } catch {
    campuses.value = []
  } finally {
    campusLoading.value = false
  }
}

async function handleUpdateUniName() {
  if (!editUniName.value.trim() || editUniName.value.trim() === university.value?.name) {
    editingUniName.value = false
    return
  }
  savingUni.value = true
  try {
    const res = await updateUniversity(universityId, editUniName.value.trim())
    if (res.code !== "00000") {
      alert(res.message)
      editUniName.value = university.value?.name ?? ''
    } else {
      university.value!.name = editUniName.value.trim()
    }
  } catch (e) {
    alert(e instanceof Error ? e.message : '保存失败')
  } finally {
    savingUni.value = false
    editingUniName.value = false
  }
}

async function handleDeleteUni() {
  if (!confirm(`确定要删除「${university.value?.name}」吗？\n如果该大学下有校区，将无法删除。`)) return
  try {
    const res = await deleteUniversity(universityId)
    if (res.code !== "00000") {
      alert(res.message)
      return
    }
    router.push('/universities')
  } catch (e) {
    alert(e instanceof Error ? e.message : '删除失败')
  }
}

function openCreateCampus() {
  editingCampusId.value = null
  editCampusName.value = ''
  campusError.value = ''
  showCampusModal.value = true
}

function openEditCampus(cam: CampusVO) {
  editingCampusId.value = cam.id
  editCampusName.value = cam.name
  campusError.value = ''
  showCampusModal.value = true
}

function closeCampusModal() {
  showCampusModal.value = false
  editingCampusId.value = null
  editCampusName.value = ''
  campusError.value = ''
}

async function handleSaveCampus() {
  if (!editCampusName.value.trim()) {
    campusError.value = '请输入校区名称'
    return
  }
  savingCampus.value = true
  campusError.value = ''
  try {
    if (editingCampusId.value) {
      const res = await updateCampus(editingCampusId.value, editCampusName.value.trim())
      if (res.code !== "00000") {
        campusError.value = res.message
        savingCampus.value = false
        return
      }
    } else {
      const res = await createCampus(editCampusName.value.trim(), universityId)
      if (res.code !== "00000") {
        campusError.value = res.message
        savingCampus.value = false
        return
      }
    }
    closeCampusModal()
    await fetchCampuses()
  } catch (e) {
    campusError.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    savingCampus.value = false
  }
}

async function handleDeleteCampus(cam: CampusVO) {
  if (!confirm(`确定要删除「${cam.name}」吗？`)) return
  try {
    const res = await deleteCampus(cam.id)
    if (res.code !== "00000") {
      alert(res.message)
      return
    }
    await fetchCampuses()
  } catch (e) {
    alert(e instanceof Error ? e.message : '删除失败')
  }
}

onMounted(() => {
  fetchUniversity()
  fetchCampuses()
})
</script>

<template>
  <div class="university-detail">
    <div class="page-header">
      <div class="page-title">
        <button class="back-btn" @click="router.push('/universities')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <div class="breadcrumb">
          <span class="breadcrumb-link" @click="router.push('/universities')">大学管理</span>
          <span class="breadcrumb-sep">/</span>
          <span v-if="university" class="breadcrumb-current">{{ university.name }}</span>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-state">加载中...</div>

    <template v-if="university">
      <div class="info-card">
        <div class="info-row">
          <span class="info-label">大学名称</span>
          <div class="info-value-row" v-if="!editingUniName">
            <span class="info-value">{{ university.name }}</span>
            <button class="inline-btn" @click="editingUniName = true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
              </svg>
            </button>
          </div>
          <div class="info-value-row" v-else>
            <input
              v-model="editUniName"
              type="text"
              class="inline-input"
              @keyup.enter="handleUpdateUniName"
              @keyup.escape="editingUniName = false"
            />
            <button class="inline-btn save" :disabled="savingUni" @click="handleUpdateUniName">保存</button>
            <button class="inline-btn" @click="editingUniName = false">取消</button>
          </div>
        </div>
        <div class="info-actions">
          <button class="delete-uni-btn" @click="handleDeleteUni">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            </svg>
            删除大学
          </button>
        </div>
      </div>

      <!-- Campus Section -->
      <div class="section-card">
        <div class="section-header">
          <h2 class="section-title">校区列表</h2>
          <el-button type="success" size="small" @click="openCreateCampus">
            <el-icon><Plus /></el-icon>
            新增校区
          </el-button>
        </div>

        <el-table
          :data="campuses"
          v-loading="campusLoading"
        >
          <template #empty>
            <el-empty description="暂无校区">
              <el-button type="success" @click="openCreateCampus">新增校区</el-button>
            </el-empty>
          </template>

          <el-table-column label="校区名称" min-width="400">
            <template #default="{ row }">
              <span class="campus-name">{{ row.name }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openEditCampus(row)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button size="small" type="danger" @click="handleDeleteCampus(row)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer" v-if="campusTotal > campusPageSize">
          <el-pagination
            v-model:current-page="campusPage"
            :page-size="campusPageSize"
            :total="campusTotal"
            layout="prev, pager, next"
            @current-change="fetchCampuses"
          />
        </div>
      </div>
    </template>

    <!-- Campus Modal -->
    <div v-if="showCampusModal" class="modal-overlay" @click.self="closeCampusModal">
      <div class="modal-card">
        <h2 class="modal-title">{{ editingCampusId ? '编辑校区' : '新增校区' }}</h2>
        <div class="form-group">
          <label>校区名称</label>
          <input
            v-model="editCampusName"
            type="text"
            placeholder="请输入校区名称"
            @keyup.enter="handleSaveCampus"
          />
        </div>
        <div v-if="campusError" class="form-error">{{ campusError }}</div>
        <div class="modal-actions">
          <button class="cancel-btn" @click="closeCampusModal">取消</button>
          <button class="submit-btn" :disabled="savingCampus" @click="handleSaveCampus">
            {{ savingCampus ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.university-detail {
  max-width: 800px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.2s;
}

.back-btn:hover {
  border-color: #10b981;
  color: #10b981;
}

.back-btn svg {
  width: 18px;
  height: 18px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breadcrumb-link {
  color: #6b7280;
  cursor: pointer;
}

.breadcrumb-link:hover {
  color: #059669;
}

.breadcrumb-sep {
  color: #d1d5db;
}

.breadcrumb-current {
  color: #1f2937;
  font-weight: 500;
}

.loading-state {
  padding: 48px 16px;
  text-align: center;
  color: #9ca3af;
}

.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.info-label {
  font-size: 13px;
  font-weight: 500;
  color: #9ca3af;
  min-width: 64px;
}

.info-value-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.inline-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.inline-btn:hover {
  border-color: #10b981;
  color: #10b981;
}

.inline-btn.save {
  background: #059669;
  color: #fff;
  border: none;
}

.inline-btn.save:hover:not(:disabled) {
  background: #047857;
}

.inline-btn.save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.inline-input {
  padding: 6px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 15px;
  color: #1f2937;
  outline: none;
  width: 200px;
}

.inline-input:focus {
  border-color: #10b981;
}

.info-actions {
  border-top: 1px solid #f3f4f6;
  padding-top: 16px;
}

.delete-uni-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fff;
  color: #ef4444;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.delete-uni-btn:hover {
  background: #fef2f2;
}

/* Campus Section */
.section-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f3f4f6;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.campus-name {
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
