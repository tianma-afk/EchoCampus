<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import axios from 'axios'

import { API_BASE, proxyImageUrl } from '../../config'

// API 配置
const API_BASE_URL = `${API_BASE}/api/v1` // 后端地址
const LANDMARK_API = `${API_BASE_URL}/landmarks` // 地标列表接口

interface LandmarkSummary {
  id: string
  name: string
  rating: number
  checkins: number
  openTime: string
  category: string
  tags: string[]
  coverImg: string
}

interface Landmark {
  id: string
  name: string
  rating: number
  checkins: number
  openTime: string
  category: string
  tags: string[]
  imgs: string[]
  campusName: string
  universityName: string
  buildYear: string
  openTimeDetail: string
  floors: string
  location: string
  description: string
  totalFloors: number
  floorList: FloorInfo[]
  recommendRate: number
}

interface FloorInfo {
  floorNumber: number
  floorName: string
  tags: string[]
}

interface LandmarkQueryParams {
  category?: number | null
  searchQuery?: string
  sortBy?: string
}

const emit = defineEmits<{
  select: [landmark: Landmark]
}>()

const searchQuery = ref('')
const activeCategory = ref('全部')
const sortBy = ref('默认排序')
const landmarks = ref<LandmarkSummary[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const currentPage = ref(1)
const pageSize = ref(6)
const total = ref(0)
const loadingMore = ref(false)
const initialLoadDone = ref(false)

let sentinelEl: HTMLElement | null = null
let observer: IntersectionObserver | null = null

const setSentinelRef = (el: any) => {
  if (el && el !== sentinelEl) {
    sentinelEl = el
    observer?.disconnect()
    observer?.observe(el)
  }
}

const noMore = computed(() => total.value > 0 && landmarks.value.length >= total.value)

const sortByKey = computed(() => {
  const map: Record<string, string> = {
    '评分最高': 'rate',
    '打卡最多': 'hot',
    '名称A-Z': 'nameAsc',
  }
  return map[sortBy.value] || undefined
})

const categories = ['全部', '教学楼', '图书馆', '体育场馆', '生活区', '活动场馆', '景观景点']

const categoryToId = (name: string): number | null => {
  const map: Record<string, number> = {
    '教学楼': 1,
    '图书馆': 2,
    '体育场馆': 3,
    '生活区': 4,
    '活动场馆': 5,
    '景观景点': 6,
  }
  return map[name] ?? null
}

/**
 * 获取地标列表
 * @param params - 查询参数
 *   - category: 分类筛选（可选）
 *   - searchQuery: 搜索关键词（可选）
 *   - sortBy: 排序方式（可选）
 * @returns Promise<Landmark[]>
 */
const fetchLandmarks = async (params?: LandmarkQueryParams): Promise<void> => {
  loading.value = true
  error.value = null
  
  try {
    const response = await axios.get(LANDMARK_API, {
      params: {
        page: currentPage.value,
        pageSize: pageSize.value,
        category: params?.category ?? null,
        searchQuery: params?.searchQuery || undefined,
        sortBy: sortByKey.value
      }
    })
    
    if (response.data.code === "00000") {
      const data = response.data.data
      landmarks.value = data.records || []
      currentPage.value = data.current || 1
      total.value = data.total || 0
      initialLoadDone.value = true
    } else {
      error.value = response.data.message || '获取地标数据失败'
    }
  } catch (err: any) {
    console.error('获取地标列表失败:', err)
    error.value = err.response?.data?.message || '网络请求失败，请检查后端服务是否启动'
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (!initialLoadDone.value || loadingMore.value || noMore.value) return
  loadingMore.value = true
  const nextPage = currentPage.value + 1
  try {
    const response = await axios.get(LANDMARK_API, {
      params: {
        page: nextPage,
        pageSize: pageSize.value,
        category: categoryToId(activeCategory.value),
        searchQuery: searchQuery.value || undefined,
        sortBy: sortByKey.value
      }
    })
    if (response.data.code === "00000") {
      const data = response.data.data
      landmarks.value.push(...(data.records || []))
      currentPage.value = data.current || nextPage
      total.value = data.total || 0
    }
  } catch (err: any) {
    console.error('加载更多失败:', err)
  } finally {
    loadingMore.value = false
  }
}

/**
 * 根据 ID 获取地标详情（路径参数）
 */
const fetchLandmarkDetail = async (id: string): Promise<Landmark | null> => {
  loading.value = true
  try {
    const response = await axios.get(`${LANDMARK_API}/${id}`)
    if (response.data.code === "00000") {
      return response.data.data
    }
    return null
  } catch (err: any) {
    console.error('获取地标详情失败:', err)
    return null
  } finally {
    loading.value = false
  }
}

/**
 * 处理卡片点击：先请求详情接口，成功后跳转
 */
const handleCardClick = async (landmark: LandmarkSummary) => {
  const detail = await fetchLandmarkDetail(landmark.id)
  if (detail) {
    emit('select', detail)
  }
}

/**
 * 地标列表（由后端返回已筛选排序的数据，前端直接展示）
 */
const filteredLandmarks = computed(() => landmarks.value)

/**
 * 监听筛选条件变化，重新请求数据
 */
const handleFilterChange = () => {
  currentPage.value = 1
  total.value = 0
  initialLoadDone.value = false
  fetchLandmarks({
    category: categoryToId(activeCategory.value),
    searchQuery: searchQuery.value,
    sortBy: sortBy.value
  })
}

// 组件挂载时获取数据
onMounted(() => {
  fetchLandmarks()
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting) {
        loadMore()
      }
    },
    { rootMargin: '100px' }
  )
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
})

const renderStars = (rating: number) => {
  const full = Math.floor(rating)
  const half = rating - full >= 0.5
  const empty = 5 - full - (half ? 1 : 0)
  return { full, half, empty }
}

/**
 * 处理关键词搜索（点击搜索按钮或按回车，重置分类和排序）
 */
const handleKeywordSearch = () => {
  activeCategory.value = '全部'
  sortBy.value = '默认排序'
  handleFilterChange()
}

/**
 * 处理分类切换（重置排序）
 */
const handleCategoryChange = (category: string) => {
  activeCategory.value = category
  sortBy.value = '默认排序'
  handleFilterChange()
}

/**
 * 处理排序切换（示例：循环切换排序方式）
 */
const sortOptions = ['默认排序', '评分最高', '打卡最多', '名称A-Z']
const handleSortChange = () => {
  const currentIndex = sortOptions.indexOf(sortBy.value)
  const nextIndex = (currentIndex + 1) % sortOptions.length
  sortBy.value = sortOptions[nextIndex]
  handleFilterChange()
}
</script>

<template>
  <div class="landmark-repo">
    <div class="repo-header-fixed">
      <div class="search-bar">
        <button class="search-btn" @click="handleKeywordSearch">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
        </button>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜你想搜..."
          class="search-input"
          @keyup.enter="handleKeywordSearch"
        />
      </div>

      <div class="category-tabs">
        <button
          v-for="cat in categories"
          :key="cat"
          class="category-tab"
          :class="{ active: activeCategory === cat }"
          @click="handleCategoryChange(cat)"
        >
          {{ cat }}
        </button>
      </div>

      <div class="landmark-stats">
        <span class="stats-text">共 <strong>{{ total }}</strong> 个地标</span>
        <button class="sort-btn" @click="handleSortChange">{{ sortBy }}</button>
      </div>
    </div>

    <div class="landmark-list-scroll">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
      
      <!-- 错误提示 -->
      <div v-else-if="error" class="error-container">
        <p class="error-message">{{ error }}</p>
        <button class="retry-btn" @click="fetchLandmarks()">重试</button>
      </div>
      
      <!-- 空数据提示 -->
      <div v-else-if="filteredLandmarks.length === 0" class="empty-container">
        <p>暂无地标数据</p>
      </div>
      
      <!-- 地标列表 -->
      <div v-else class="landmark-list">
        <template v-for="(landmark, index) in filteredLandmarks" :key="index">
          <div
            class="landmark-card"
            @click="handleCardClick(landmark)"
          >
          <div class="landmark-image" :style="{ backgroundImage: `url(${proxyImageUrl(landmark.coverImg)})`, backgroundSize: 'cover', backgroundPosition: 'center' }">
          </div>
          <div class="landmark-info">
            <div class="landmark-header">
              <h3 class="landmark-name">{{ landmark.name }}</h3>
              <span class="landmark-category">{{ landmark.category }}</span>
            </div>
            <div class="landmark-rating">
              <div class="stars">
                <template v-for="i in 5" :key="i">
                  <svg
                    v-if="i <= renderStars(landmark.rating).full"
                    class="star-icon filled"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                  >
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                  </svg>
                  <svg
                    v-else-if="i === renderStars(landmark.rating).full + 1 && renderStars(landmark.rating).half"
                    class="star-icon half"
                    viewBox="0 0 24 24"
                  >
                    <defs>
                      <linearGradient :id="'half-' + index">
                        <stop offset="50%" stop-color="#f59e0b" />
                        <stop offset="50%" stop-color="#e5e7eb" />
                      </linearGradient>
                    </defs>
                    <path
                      :fill="'url(#half-' + index + ')'"
                      d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
                    />
                  </svg>
                  <svg
                    v-else
                    class="star-icon empty"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                  >
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                  </svg>
                </template>
              </div>
              <span class="rating-value">{{ landmark.rating }}</span>
            </div>
            <div class="landmark-meta">
              <span class="meta-item">
                <svg class="meta-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 00-3-3.87" />
                  <path d="M16 3.13a4 4 0 010 7.75" />
                </svg>
                {{ landmark.checkins.toLocaleString() }} 次打卡
              </span>
              <span class="meta-item">
                <svg class="meta-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
                {{ landmark.openTime }}
              </span>
            </div>
            <div class="landmark-tags">
              <span
                v-for="tag in landmark.tags"
                :key="tag"
                class="tag"
              >
                {{ tag }}
              </span>
            </div>
          </div>
        </div>
        <div v-if="index === landmarks.length - 3" :ref="setSentinelRef" class="scroll-sentinel"></div>
      </template>
    </div>
        <div v-if="loadingMore" class="loading-more">
          <div class="loading-spinner-small"></div>
          <span>加载中...</span>
        </div>
        <div v-else-if="noMore && landmarks.length > 0" class="no-more">没有更多了</div>
    </div>
  </div>
</template>

<style scoped>
.landmark-repo {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--color-bg);
}

.repo-header-fixed {
  flex-shrink: 0;
  padding: 24px 16px 12px;
  background: var(--color-bg);
  z-index: 1;
}

.search-bar {
  position: relative;
  margin-bottom: 12px;
}

.search-btn {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  z-index: 1;
}

.search-btn:hover .search-icon {
  color: var(--color-primary);
}

.search-icon {
  width: 18px;
  height: 18px;
  color: var(--color-text-secondary);
}

.search-input {
  width: 100%;
  height: 44px;
  padding: 0 16px 0 42px;
  border: none;
  border-radius: var(--radius-lg);
  background: var(--color-bg-input);
  font-size: 14px;
  color: var(--color-text-heading);
  outline: none;
  box-sizing: border-box;
}

.search-input::placeholder {
  color: var(--color-text-secondary);
}

.category-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 12px;
  margin-bottom: 12px;
  scrollbar-width: none;
}

.category-tabs::-webkit-scrollbar {
  display: none;
}

.category-tab {
  padding: 8px 18px;
  border-radius: 20px;
  border: none;
  background: var(--color-bg-input);
  color: var(--color-text);
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
}

.category-tab.active {
  background: var(--color-primary);
  color: #fff;
}

.category-tab:hover:not(.active) {
  background: var(--color-primary-light);
}

.landmark-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-text {
  font-size: 14px;
  color: var(--color-text);
}

.stats-text strong {
  color: var(--color-primary);
}

.sort-btn {
  padding: 4px 12px;
  border: none;
  background: transparent;
  color: var(--color-text);
  font-size: 13px;
  cursor: pointer;
}

.landmark-list-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 16px 84px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.landmark-list-scroll::-webkit-scrollbar {
  display: none;
}

.landmark-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.landmark-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  padding: 16px;
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.landmark-card:hover {
  box-shadow: var(--shadow-md);
}

.landmark-image {
  aspect-ratio: 1 / 1;
  border-radius: var(--radius-lg);
  overflow: hidden;
  position: relative;
  background-color: var(--color-bg-input);
}

.landmark-info {
  flex: 1;
  min-width: 0;
}

.landmark-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 6px;
}

.landmark-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-heading);
  margin: 0;
}

.landmark-category {
  font-size: 12px;
  color: var(--color-primary);
  background: var(--color-primary-light);
  padding: 2px 10px;
  border-radius: var(--radius-md);
  white-space: nowrap;
  flex-shrink: 0;
}

.landmark-rating {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.stars {
  display: flex;
  gap: 2px;
}

.star-icon {
  width: 14px;
  height: 14px;
}

.star-icon.filled {
  color: var(--color-star);
}

.star-icon.half {
  color: #e5e7eb;
}

.star-icon.empty {
  color: #e5e7eb;
}

.rating-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-star);
}

.landmark-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.meta-icon {
  width: 12px;
  height: 12px;
}

.landmark-tags {
  display: flex;
  gap: 6px;
}

.tag {
  font-size: 12px;
  color: var(--color-text);
  background: var(--color-bg-input);
  padding: 2px 10px;
  border-radius: var(--radius-md);
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--color-text);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--color-bg-input);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.error-message {
  color: var(--color-danger);
  font-size: 14px;
  margin-bottom: 16px;
  text-align: center;
}

.retry-btn {
  padding: 10px 24px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: var(--color-primary-hover);
}

.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.scroll-sentinel {
  height: 1px;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  color: var(--color-text);
  font-size: 13px;
}

.loading-spinner-small {
  width: 20px;
  height: 20px;
  border: 3px solid var(--color-bg-input);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.no-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  color: var(--color-text-secondary);
  font-size: 13px;
}
</style>
