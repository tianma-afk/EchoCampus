<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getLandmarkList, deleteLandmark, type LandmarkAdminVO } from '../api/landmark'
import { listCategories, type CategoryVO } from '../api/category'
import { vectorizeAllImages } from '../api/task'

const router = useRouter()

const landmarks = ref<LandmarkAdminVO[]>([])
const categories = ref<CategoryVO[]>([])
const categoryFilter = ref('')
const keywordFilter = ref('')
const loading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const totalCount = ref(0)
const pageSize = 9

const categoryColors: Record<string, string> = {
  教学楼: '#6ee7b7',
  图书馆: '#93c5fd',
  体育场馆: '#fcd34d',
  生活区: '#f9a8d4',
  活动场馆: '#c4b5fd',
  景观景点: '#5eead4',
}

function categoryColor(name: string) {
  return categoryColors[name] ?? '#e5e7eb'
}

async function fetchLandmarks() {
  loading.value = true
  try {
    const res = await getLandmarkList(
      currentPage.value,
      pageSize,
      categoryFilter.value || undefined,
      undefined,
      keywordFilter.value || undefined,
    )
    const page = res.data
    landmarks.value = page.records
    totalPages.value = page.pages
    totalCount.value = page.total
  } catch {
    landmarks.value = []
  } finally {
    loading.value = false
  }
}

function goPage(page: number) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

function handleEdit(id: string) {
  router.push(`/landmark/${id}/edit`)
}

const vectorizing = ref(false)

async function handleVectorize() {
  if (vectorizing.value) return
  vectorizing.value = true
  try {
    const res = await vectorizeAllImages()
    if (res.data) {
      alert('向量化任务已提交，任务ID：' + res.data)
    } else {
      alert('暂无可向量化的图片')
    }
  } catch {
    alert('向量化任务提交失败，请重试')
  } finally {
    vectorizing.value = false
  }
}

async function handleDelete(landmark: LandmarkAdminVO) {
  if (!confirm('确定删除地标"' + landmark.name + '"吗？此操作不可撤销。')) return
  try {
    await deleteLandmark(landmark.id)
    await fetchLandmarks()
  } catch {
    alert('删除失败，请重试')
  }
}

function ratingPercent(rating: number | null) {
  if (rating == null) return 0
  return Math.round((rating / 5) * 100)
}

onMounted(async () => {
  try {
    const res = await listCategories()
    categories.value = res.data ?? []
  } catch {
    categories.value = []
  }
  await fetchLandmarks()
})

watch(categoryFilter, () => {
  currentPage.value = 1
  fetchLandmarks()
})

let keywordTimer: ReturnType<typeof setTimeout> | undefined
watch(keywordFilter, () => {
  clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => {
    currentPage.value = 1
    fetchLandmarks()
  }, 300)
})
</script>

<template>
  <div class="landmark-management">
    <div class="page-header">
      <div class="page-title">
        <h1>地标管理</h1>
        <span class="page-count">共 {{ totalCount }} 个地标</span>
      </div>
      <div class="page-actions">
        <select v-model="categoryFilter" class="filter-select">
          <option value="">全部分类</option>
          <option v-for="cat in categories" :key="cat.id" :value="cat.id">
            {{ cat.name }}
          </option>
        </select>
        <input
          v-model="keywordFilter"
          type="text"
          class="search-input"
          placeholder="搜索地标名称..."
        />
        <button
          :disabled="vectorizing"
          class="vectorize-btn"
          @click="handleVectorize"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="5 3 19 12 5 21 5 3" />
          </svg>
          {{ vectorizing ? '提交中...' : '向量化所有图片' }}
        </button>
        <button class="add-btn" @click="router.push('/landmark/create')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          新增地标
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">加载中...</div>

    <div v-else-if="landmarks.length === 0" class="empty-state">
      <p>暂无地标数据</p>
    </div>

    <template v-else>
      <div class="landmark-grid">
        <div
          v-for="landmark in landmarks"
          :key="landmark.id"
          class="landmark-card"
          @click="router.push('/landmark/' + landmark.id)"
        >
          <div
            class="card-header"
            :style="{ backgroundColor: categoryColor(landmark.categoryName) + '40' }"
          >
            <div class="card-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                <circle cx="12" cy="10" r="3" />
              </svg>
            </div>
            <span class="category-badge" v-if="landmark.categoryName">
              {{ landmark.categoryName }}
            </span>
          </div>

          <div class="card-body">
            <div class="card-title-row">
              <h3 class="card-title">{{ landmark.name }}</h3>
              <span class="card-campus" v-if="landmark.universityName || landmark.campusName">
                {{ landmark.universityName }}{{ landmark.universityName && landmark.campusName ? ' · ' : '' }}{{ landmark.campusName }}
              </span>
            </div>

            <div class="progress-section">
              <div class="progress-header">
                <span class="progress-label">评分</span>
                <span class="progress-value">{{ landmark.rating ?? '-' }} / 5</span>
              </div>
              <div class="progress-bar">
                <div
                  class="progress-fill"
                  :style="{ width: ratingPercent(landmark.rating) + '%' }"
                ></div>
              </div>
            </div>

            <div class="meta-row" v-if="landmark.checkInCount != null || landmark.recommendRate != null">
              <span class="meta-item" v-if="landmark.checkInCount != null">
                打卡 {{ landmark.checkInCount }} 次
              </span>
              <span class="meta-item" v-if="landmark.recommendRate != null">
                推荐率 {{ (landmark.recommendRate * 100).toFixed(0) }}%
              </span>
            </div>

            <div class="card-actions">
              <button class="action-btn edit-btn" @click.stop="handleEdit(landmark.id)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                </svg>
                编辑信息
              </button>
              <button class="action-btn delete-btn" @click.stop="handleDelete(landmark)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                </svg>
                删除
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination" v-if="totalPages > 1">
        <button class="page-btn" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">
          上一页
        </button>
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
        <button
          class="page-btn"
          :disabled="currentPage >= totalPages"
          @click="goPage(currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.landmark-management {
  max-width: 1200px;
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

.filter-select {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #374151;
  background: #fff;
  cursor: pointer;
  outline: none;
}

.filter-select:focus {
  border-color: #10b981;
}

.search-input {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #374151;
  outline: none;
  width: 160px;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #10b981;
}

.search-input::placeholder {
  color: #d1d5db;
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

.vectorize-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.vectorize-btn:hover:not(:disabled) {
  background: #2563eb;
}

.vectorize-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.vectorize-btn svg {
  width: 16px;
  height: 16px;
}

.add-btn svg {
  width: 16px;
  height: 16px;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 48px 0;
  color: #9ca3af;
  font-size: 14px;
}

.landmark-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.landmark-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.2s;
  cursor: pointer;
}

.landmark-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.card-header {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.card-icon {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-icon svg {
  width: 24px;
  height: 24px;
  color: #6b7280;
}

.category-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.9);
  color: #374151;
}

.card-body {
  padding: 16px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.card-campus {
  font-size: 12px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
}

.progress-section {
  margin-bottom: 10px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.progress-label {
  font-size: 12px;
  color: #9ca3af;
}

.progress-value {
  font-size: 12px;
  font-weight: 600;
  color: #059669;
}

.progress-bar {
  height: 6px;
  background: #f3f4f6;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  background: #10b981;
  transition: width 0.3s;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.meta-item {
  font-size: 12px;
  color: #6b7280;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
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

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #10b981;
  color: #10b981;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #6b7280;
}
</style>
