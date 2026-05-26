<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getLandmarkDetail, updateLandmark, deleteLandmark } from '../api/landmark'
import type { LandmarkCreateRequest } from '../api/landmark'
import { listCategories, type CategoryVO } from '../api/category'
import { searchUniversities, type UniversityVO } from '../api/university'
import { searchCampuses, type CampusVO } from '../api/campus'
import SearchableSelect, { type SelectOption } from '../components/SearchableSelect.vue'
import TagListInput from '../components/TagListInput.vue'
import FloorManager, { type FloorEntry } from '../components/FloorManager.vue'
import ImageManager from '../components/ImageManager.vue'

const route = useRoute()
const router = useRouter()
const id = route.params.id as string

const form = ref<LandmarkCreateRequest>({
  name: '',
  categoryId: '',
  campusId: '',
  openTime: '',
  tags: [],
  buildYear: '',
  openTimeDetail: '',
  floors: '',
  location: '',
  description: '',
  totalFloors: undefined,
  floorList: undefined,
})

const categories = ref<CategoryVO[]>([])

const selectedUniversity = ref<SelectOption | null>(null)
const universityOptions = ref<UniversityVO[]>([])
const universityLoading = ref(false)
let universityTimer: ReturnType<typeof setTimeout> | undefined

const selectedCampus = ref<SelectOption | null>(null)
const campusOptions = ref<CampusVO[]>([])
const campusLoading = ref(false)
let campusTimer: ReturnType<typeof setTimeout> | undefined

const tagList = ref<string[]>([])
const floorList = ref<FloorEntry[]>([])

const submitting = ref(false)
const deleting = ref(false)
const error = ref('')
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await listCategories()
    categories.value = res.data ?? []
  } catch {
    categories.value = []
  }

  try {
    const res = await getLandmarkDetail(id)
    const d = res.data
    form.value = {
      name: d.name,
      categoryId: d.categoryId,
      campusId: d.campusId,
      openTime: d.openTime ?? '',
      tags: d.tags ?? [],
      buildYear: d.buildYear ?? '',
      openTimeDetail: d.openTimeDetail ?? '',
      floors: d.floors ?? '',
      location: d.location ?? '',
      description: d.description ?? '',
      totalFloors: d.totalFloors ?? undefined,
      floorList: undefined,
    }
    tagList.value = d.tags ?? []
    if (d.floorList) {
      floorList.value = d.floorList.map((f) => ({
        floorNumber: f.floorNumber,
        floorName: f.floorName,
        tags: f.tags ?? [],
      }))
    }
    if (d.universityId && d.universityName) {
      selectedUniversity.value = { id: d.universityId, name: d.universityName }
    }
    if (d.campusId && d.campusName) {
      selectedCampus.value = { id: d.campusId, name: d.campusName }
    }
  } catch {
    error.value = '加载地标信息失败'
  } finally {
    loading.value = false
  }
})

function handleUniversitySearch(keyword: string) {
  clearTimeout(universityTimer)
  universityTimer = setTimeout(async () => {
    universityLoading.value = true
    try {
      const res = await searchUniversities(keyword)
      universityOptions.value = res.data ?? []
    } catch {
      universityOptions.value = []
    } finally {
      universityLoading.value = false
    }
  }, 300)
}

function handleCampusSearch(keyword: string) {
  clearTimeout(campusTimer)
  campusTimer = setTimeout(async () => {
    campusLoading.value = true
    try {
      const res = await searchCampuses(keyword, selectedUniversity.value?.id)
      campusOptions.value = res.data ?? []
    } catch {
      campusOptions.value = []
    } finally {
      campusLoading.value = false
    }
  }, 300)
}

function onUniversitySelect(uni: SelectOption | null) {
  selectedUniversity.value = uni
  selectedCampus.value = null
  campusOptions.value = []
  form.value.campusId = ''
}

function onCampusSelect(cam: SelectOption | null) {
  selectedCampus.value = cam
  form.value.campusId = cam?.id ?? ''
}

async function handleSubmit() {
  error.value = ''

  if (!form.value.name.trim()) {
    error.value = '请填写地标名称'
    return
  }
  if (!form.value.categoryId) {
    error.value = '请选择分类'
    return
  }
  if (!selectedCampus.value) {
    error.value = '请选择校区'
    return
  }

  form.value.tags = tagList.value

  if (floorList.value.length > 0) {
    form.value.floorList = floorList.value.map((f) => ({
      floorNumber: f.floorNumber,
      floorName: f.floorName,
      tags: f.tags,
    }))
    form.value.totalFloors = floorList.value.length
    form.value.floors = floorList.value.map((f) => f.floorName).join(',')
  } else {
    form.value.floorList = []
    form.value.totalFloors = 0
    form.value.floors = ''
  }

  submitting.value = true
  try {
    await updateLandmark(id, form.value)
    router.push('/landmark')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败，请重试'
  } finally {
    submitting.value = false
  }
}

async function handleDelete() {
  if (!confirm('确定删除地标"' + form.value.name + '"吗？此操作不可撤销。')) return
  deleting.value = true
  try {
    await deleteLandmark(id)
    router.push('/landmark')
  } catch {
    error.value = '删除失败，请重试'
  } finally {
    deleting.value = false
  }
}

function handleCancel() {
  router.push('/landmark')
}

onBeforeUnmount(() => {
  clearTimeout(universityTimer)
  clearTimeout(campusTimer)
})
</script>

<template>
  <div class="edit-landmark">
    <div class="page-header">
      <div class="page-title">
        <button class="back-btn" @click="handleCancel">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <h1>编辑地标</h1>
      </div>
      <button class="danger-btn" :disabled="deleting" @click="handleDelete">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="3 6 5 6 21 6" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        </svg>
        {{ deleting ? '删除中...' : '删除地标' }}
      </button>
    </div>

    <div v-if="loading" class="state-msg">加载中...</div>
    <div v-else-if="error && !form.name" class="state-msg error">{{ error }}</div>

    <template v-else>
      <div class="image-section">
        <ImageManager :landmark-id="id" />
      </div>

      <form class="edit-form" @submit.prevent="handleSubmit">
        <div class="form-card">
          <h2 class="form-section-title">基本信息</h2>

        <div class="form-row">
          <div class="form-group required">
            <label>地标名称</label>
            <input v-model="form.name" type="text" placeholder="请输入地标名称" maxlength="200" />
          </div>
        </div>

        <div class="form-row two-col">
          <div class="form-group required">
            <label>分类</label>
            <select v-model="form.categoryId">
              <option value="" disabled>请选择分类</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                {{ cat.name }}
              </option>
            </select>
          </div>
          <div class="form-group required">
            <label>大学</label>
            <SearchableSelect
              :model-value="selectedUniversity"
              :options="universityOptions"
              :loading="universityLoading"
              placeholder="输入大学名称搜索"
              @search="handleUniversitySearch"
              @update:model-value="onUniversitySelect"
            />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group required">
            <label>校区</label>
            <SearchableSelect
              :model-value="selectedCampus"
              :options="campusOptions"
              :loading="campusLoading"
              :disabled="!selectedUniversity"
              :placeholder="selectedUniversity ? '输入校区名称搜索' : '请先选择大学'"
              @search="handleCampusSearch"
              @update:model-value="onCampusSelect"
            />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>地标描述</label>
            <textarea
              v-model="form.description"
              rows="4"
              placeholder="请输入地标描述信息"
            ></textarea>
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>标签</label>
            <TagListInput v-model="tagList" placeholder="输入标签名称后点击添加" />
          </div>
        </div>
      </div>

      <div class="form-card">
        <h2 class="form-section-title">位置信息</h2>

        <div class="form-row">
          <div class="form-group">
            <label>位置描述</label>
            <input v-model="form.location" type="text" placeholder="如：校园中心" maxlength="200" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>楼层</label>
            <FloorManager v-model="floorList" />
          </div>
        </div>
      </div>

      <div class="form-card">
        <h2 class="form-section-title">其他信息</h2>

        <div class="form-row two-col">
          <div class="form-group">
            <label>开放时间</label>
            <input v-model="form.openTime" type="text" placeholder="如：08:00-22:00" maxlength="50" />
          </div>
          <div class="form-group">
            <label>开放时间详情</label>
            <input
              v-model="form.openTimeDetail"
              type="text"
              placeholder="如：周一至周日 08:00-22:00"
              maxlength="200"
            />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>建成年份</label>
            <input v-model="form.buildYear" type="text" placeholder="如：2010" maxlength="30" />
          </div>
        </div>
      </div>

      <div v-if="error" class="form-error">{{ error }}</div>

      <div class="form-actions">
        <button type="button" class="cancel-btn" @click="handleCancel">取消</button>
        <button type="submit" class="submit-btn" :disabled="submitting">
          {{ submitting ? '保存中...' : '保存修改' }}
        </button>
      </div>
    </form>
    </template>
  </div>
</template>

<style scoped>
.edit-landmark {
  max-width: 800px;
}

.image-section {
  margin-bottom: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title h1 {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
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

.danger-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #ef4444;
  color: #ef4444;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.danger-btn:hover:not(:disabled) {
  background: #fef2f2;
}

.danger-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.danger-btn svg {
  width: 16px;
  height: 16px;
}

.state-msg {
  text-align: center;
  padding: 48px 0;
  color: #9ca3af;
  font-size: 14px;
}

.state-msg.error {
  color: #ef4444;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.form-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.form-row {
  margin-bottom: 16px;
}

.form-row:last-child {
  margin-bottom: 0;
}

.form-row.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.form-group.required label::after {
  content: ' *';
  color: #ef4444;
}

.form-group input,
.form-group textarea,
.form-group select {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
  resize: vertical;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  border-color: #10b981;
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: #d1d5db;
}

.form-group select {
  appearance: auto;
  cursor: pointer;
  background: #fff;
}

.form-error {
  padding: 12px 16px;
  background: #fef2f2;
  border-radius: 8px;
  color: #ef4444;
  font-size: 14px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn,
.submit-btn {
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
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
