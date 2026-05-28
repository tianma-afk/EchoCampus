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
    if (res.code !== 200) {
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
    if (res.code !== 200) {
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
      if (res.code !== 200) {
        campusError.value = res.message
        savingCampus.value = false
        return
      }
    } else {
      const res = await createCampus(editCampusName.value.trim(), universityId)
      if (res.code !== 200) {
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
    if (res.code !== 200) {
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
          <button class="add-btn" @click="openCreateCampus">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            新增校区
          </button>
        </div>

        <table class="data-table" v-if="!campusLoading && campuses.length > 0">
          <thead>
            <tr>
              <th>校区名称</th>
              <th class="col-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="cam in campuses" :key="cam.id">
              <td class="campus-name">{{ cam.name }}</td>
              <td class="col-actions">
                <button class="action-btn edit-btn" @click="openEditCampus(cam)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                  </svg>
                  编辑
                </button>
                <button class="action-btn delete-btn" @click="handleDeleteCampus(cam)">
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

        <div v-else-if="campusLoading" class="empty-state">加载中...</div>
        <div v-else class="empty-state">暂无校区，点击"新增校区"开始添加</div>

        <div class="pagination" v-if="campusTotal > campusPageSize">
          <button class="page-btn" :disabled="campusPage <= 1" @click="campusPage--; fetchCampuses()">上一页</button>
          <span class="page-info">{{ campusPage }} / {{ Math.ceil(campusTotal / campusPageSize) }}</span>
          <button class="page-btn" :disabled="campusPage >= Math.ceil(campusTotal / campusPageSize)" @click="campusPage++; fetchCampuses()">下一页</button>
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

.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
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
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.data-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
  font-size: 14px;
  color: #374151;
}

.campus-name {
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
