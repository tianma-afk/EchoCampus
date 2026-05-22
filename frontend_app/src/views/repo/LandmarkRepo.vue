<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

// API 配置
const API_BASE_URL = 'http://localhost:8080/api' // 本地后端地址
const LANDMARK_API = `${API_BASE_URL}/landmarks` // 地标列表接口

interface Landmark {
  id: number
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
const landmarks = ref<Landmark[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

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
        category: params?.category ?? null,
        searchQuery: params?.searchQuery || undefined,
        sortBy: params?.sortBy !== '默认排序' ? params?.sortBy : undefined
      }
    })
    
    // 假设后端返回格式为: { code: 200, data: [...], message: 'success' }
    if (response.data.code === 200) {
      landmarks.value = response.data.data || []
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

/**
 * 根据 ID 获取地标详情（路径参数）
 */
const fetchLandmarkDetail = async (id: number): Promise<Landmark | null> => {
  loading.value = true
  try {
    const response = await axios.get(`${LANDMARK_API}/${id}`)
    if (response.data.code === 200) {
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
const handleCardClick = async (landmark: Landmark) => {
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
  fetchLandmarks({
    category: categoryToId(activeCategory.value),
    searchQuery: searchQuery.value,
    sortBy: sortBy.value
  })
}

// 组件挂载时获取数据
onMounted(() => {
  fetchLandmarks()
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
      <header class="repo-header">
        <h1 class="repo-title">地标库</h1>
        <div class="header-actions">
          <button class="icon-btn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <rect x="3" y="3" width="7" height="7" rx="1" />
              <rect x="14" y="3" width="7" height="7" rx="1" />
              <rect x="3" y="14" width="7" height="7" rx="1" />
              <rect x="14" y="14" width="7" height="7" rx="1" />
            </svg>
          </button>
          <button class="icon-btn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <line x1="4" y1="6" x2="20" y2="6" />
              <line x1="4" y1="12" x2="20" y2="12" />
              <line x1="4" y1="18" x2="20" y2="18" />
            </svg>
          </button>
        </div>
      </header>

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
          placeholder="搜索地标、类别..."
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
        <span class="stats-text">共 <strong>{{ filteredLandmarks.length }}</strong> 个地标</span>
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
        <div
          v-for="(landmark, index) in filteredLandmarks"
          :key="index"
          class="landmark-card"
          @click="handleCardClick(landmark)"
        >
          <div class="landmark-image" :style="{ backgroundImage: `url(${landmark.imgs?.[0]})`, backgroundSize: 'cover', backgroundPosition: 'center' }">
            <div class="image-decoration"></div>
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
          <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.landmark-repo {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f7f4;
}

.repo-header-fixed {
  flex-shrink: 0;
  padding: 16px;
  padding-bottom: 12px;
  background: #f0f7f4;
  z-index: 1;
}

.repo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.repo-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: #e8f5e9;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
}

.icon-btn:hover {
  background: #d4edda;
}

.icon-btn svg {
  width: 18px;
  height: 18px;
  color: #2d8a6e;
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
  color: #2d8a6e;
}

.search-icon {
  width: 18px;
  height: 18px;
  color: #9ca3af;
}

.search-input {
  width: 100%;
  height: 44px;
  padding: 0 16px 0 42px;
  border: none;
  border-radius: 12px;
  background: #e8f5e9;
  font-size: 14px;
  color: #1a1a1a;
  outline: none;
  box-sizing: border-box;
}

.search-input::placeholder {
  color: #9ca3af;
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
  background: #e8f5e9;
  color: #6b7280;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
}

.category-tab.active {
  background: #2d8a6e;
  color: #fff;
}

.category-tab:hover:not(.active) {
  background: #d4edda;
}

.landmark-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-text {
  font-size: 14px;
  color: #6b7280;
}

.stats-text strong {
  color: #2d8a6e;
}

.sort-btn {
  padding: 4px 12px;
  border: none;
  background: transparent;
  color: #6b7280;
  font-size: 13px;
  cursor: pointer;
}

.landmark-list-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 16px 80px;
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
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  gap: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.landmark-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.landmark-image {
  width: 80px;
  height: 80px;
  border-radius: 16px;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.image-decoration {
  position: absolute;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  top: -10px;
  right: -10px;
}

.image-decoration::after {
  content: '';
  position: absolute;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  bottom: 5px;
  left: 5px;
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
  color: #1a1a1a;
  margin: 0;
}

.landmark-category {
  font-size: 12px;
  color: #2d8a6e;
  background: #e8f5e9;
  padding: 2px 10px;
  border-radius: 10px;
  white-space: nowrap;
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
  color: #f59e0b;
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
  color: #f59e0b;
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
  color: #9ca3af;
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
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 10px;
  border-radius: 10px;
}

.arrow-icon {
  width: 16px;
  height: 16px;
  color: #d1d5db;
  flex-shrink: 0;
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #6b7280;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e8f5e9;
  border-top-color: #2d8a6e;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 错误提示 */
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.error-message {
  color: #ef4444;
  font-size: 14px;
  margin-bottom: 16px;
  text-align: center;
}

.retry-btn {
  padding: 10px 24px;
  background: #2d8a6e;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: #237a5e;
}

/* 空数据提示 */
.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #9ca3af;
  font-size: 14px;
}
</style>
