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
  fetchLandmarks()
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
          <div class="card-cover">
            <img v-if="landmark.coverImageUrl" :src="landmark.coverImageUrl" :alt="landmark.name" />
            <div v-else class="cover-placeholder">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                <rect x="3" y="3" width="18" height="18" rx="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <path d="M21 15l-5-5L5 21" />
              </svg>
            </div>
            <span class="category-tag" v-if="landmark.categoryName">{{ landmark.categoryName }}</span>
          </div>
          <div class="card-body">
            <div class="card-top-row">
              <span class="card-campus" v-if="landmark.universityName || landmark.campusName">
                {{ landmark.universityName }}{{ landmark.universityName && landmark.campusName ? ' · ' : '' }}{{ landmark.campusName }}
              </span>
            </div>
            <h3 class="card-title">{{ landmark.name }}</h3>

            <div class="rating-section">
              <div class="stars-row">
                <template v-for="i in 5" :key="i">
                  <!-- full star -->
                  <svg
                    v-if="i <= Math.floor(landmark.rating ?? 0)"
                    class="star"
                    viewBox="0 0 24 24"
                    fill="#f59e0b"
                    stroke="#f59e0b"
                    stroke-width="1.5"
                  >
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                  </svg>
                  <!-- half star -->
                  <svg
                    v-else-if="i === Math.ceil(landmark.rating ?? 0) && ((landmark.rating ?? 0) % 1) >= 0.25"
                    class="star"
                    viewBox="0 0 24 24"
                    stroke="#f59e0b"
                    stroke-width="1.5"
                  >
                    <defs>
                      <linearGradient :id="'hg-' + landmark.id + '-' + i">
                        <stop offset="50%" stop-color="#f59e0b" />
                        <stop offset="50%" stop-color="transparent" />
                      </linearGradient>
                    </defs>
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" :fill="'url(#hg-' + landmark.id + '-' + i + ')'" />
                  </svg>
                  <!-- empty star -->
                  <svg
                    v-else
                    class="star"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="#d1d5db"
                    stroke-width="1.5"
                  >
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                  </svg>
                </template>
                <span class="rating-value">{{ landmark.rating != null ? landmark.rating + ' 分' : '暂无评分' }}</span>
              </div>
            </div>

            <div class="meta-row" v-if="landmark.checkInCount != null || landmark.favoriteCount != null">
              <span class="meta-item" v-if="landmark.checkInCount != null">
                打卡 {{ landmark.checkInCount }} 次
              </span>
              <span class="meta-item" v-if="landmark.favoriteCount != null">
                收藏 {{ landmark.favoriteCount }} 次
              </span>
            </div>

            <div class="card-actions">
              <button class="action-btn edit-btn" @click.stop="handleEdit(landmark.id)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                </svg>
                编辑
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
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 124px);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.page-title {
  display: flex;
  align-items: baseline;
  gap: 14px;
}

.page-title h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  letter-spacing: -0.02em;
}

.page-count {
  font-size: 13px;
  color: #9ca3af;
  font-weight: 500;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-select {
  padding: 8px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #374151;
  background: #fff;
  cursor: pointer;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
}

.filter-select:focus {
  border-color: #059669;
}

.search-input {
  padding: 8px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  font-size: 13px;
  color: #374151;
  outline: none;
  width: 180px;
  transition: all 0.2s;
  font-family: inherit;
}

.search-input:focus {
  border-color: #059669;
}

.search-input::placeholder {
  color: #d1d5db;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: #059669;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.add-btn:hover {
  background: #047857;
}

.vectorize-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
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
  padding: 64px 0;
  color: #9ca3af;
  font-size: 14px;
}

.landmark-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.landmark-card {
  display: flex;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
  transition: all 0.25s ease;
  cursor: pointer;
}

.landmark-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08), 0 2px 4px rgba(0, 0, 0, 0.04);
  transform: translateY(-2px);
}

/* ── Cover ── */
.card-cover {
  width: 160px;
  aspect-ratio: 1 / 1;
  flex-shrink: 0;
  background: #f3f4f6;
  position: relative;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d1d5db;
}

.cover-placeholder svg {
  width: 32px;
  height: 32px;
}

.card-body {
  flex: 1;
  padding: 16px 20px;
  min-width: 0;
}

.card-top-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 10px;
}

.category-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 11px;
  font-weight: 500;
  color: #059669;
  background: #ecfdf5;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 14px 0;
}

.card-campus {
  font-size: 11px;
  color: #9ca3af;
  white-space: nowrap;
}

.rating-section {
  margin-bottom: 12px;
}


.stars-row {
  display: flex;
  align-items: center;
  gap: 2px;
}

.star {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.rating-value {
  font-size: 12px;
  font-weight: 600;
  color: #059669;
  margin-left: 6px;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 16px;
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
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  font-family: inherit;
}

.action-btn svg {
  width: 14px;
  height: 14px;
}

.edit-btn {
  color: #6b7280;
}

.edit-btn:hover {
  border-color: #059669;
  color: #059669;
  background: #ecfdf5;
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
  margin-top: auto;
  padding-top: 32px;
}

.page-btn {
  padding: 8px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.page-btn:hover:not(:disabled) {
  border-color: #059669;
  color: #059669;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}
</style>
