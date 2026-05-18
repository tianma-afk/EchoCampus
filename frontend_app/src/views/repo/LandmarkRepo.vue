<script setup lang="ts">
import { ref } from 'vue'

interface Landmark {
  name: string
  rating: number
  checkins: number
  openTime: string
  category: string
  tags: string[]
  color: string
  buildYear: string
  openTimeDetail: string
  floors: string
  location: string
  description: string
  totalFloors: number
  recommendRate: number
}

const emit = defineEmits<{
  select: [landmark: Landmark]
}>()

const searchQuery = ref('')
const activeCategory = ref('全部')
const sortBy = ref('默认排序')

const categories = ['全部', '教学楼', '活动场馆', '运动场馆', '景观']

const landmarks: Landmark[] = [
  {
    name: '图书馆',
    rating: 4.8,
    checkins: 4821,
    openTime: '周一至周日',
    category: '教学楼',
    tags: ['阅读', '自习', '文化地标'],
    color: '#4a8c7a',
    buildYear: '1985年',
    openTimeDetail: '周一至周日 08:00 - 22:00',
    floors: '共6层（含地下1层）',
    location: '主校区·中轴线区域',
    description: '建于1985年，馆藏图书200余万册，是师生学习研究的重要场所。馆内设有自习室、研讨间、数字阅览室等多种功能区域，全年大部分时间对外开放。',
    totalFloors: 6,
    recommendRate: 96,
  },
  {
    name: '综合大礼堂',
    rating: 4.6,
    checkins: 2356,
    openTime: '活动期间开放',
    category: '活动场馆',
    tags: ['活动', '典礼'],
    color: '#d4a056',
    buildYear: '1992年',
    openTimeDetail: '活动期间开放',
    floors: '共3层',
    location: '主校区·南区',
    description: '建于1992年，可容纳2000余人，是学校举办开学典礼、毕业典礼、大型文艺演出的重要场所。',
    totalFloors: 3,
    recommendRate: 92,
  },
  {
    name: '理工实验楼',
    rating: 4.5,
    checkins: 1893,
    openTime: '周一至周五',
    category: '教学楼',
    tags: ['实验', '研究'],
    color: '#5b9bd5',
    buildYear: '2005年',
    openTimeDetail: '周一至周五 08:00 - 21:00',
    floors: '共8层',
    location: '主校区·东区',
    description: '建于2005年，配备先进实验设备，涵盖物理、化学、生物等多个学科实验室，是理工科学生实践教学的核心基地。',
    totalFloors: 8,
    recommendRate: 88,
  },
  {
    name: '体育馆',
    rating: 4.7,
    checkins: 3102,
    openTime: '周一至周日',
    category: '运动场馆',
    tags: ['运动', '健身'],
    color: '#7b5ea7',
    buildYear: '2010年',
    openTimeDetail: '周一至周日 06:00 - 22:00',
    floors: '共4层',
    location: '主校区·西区',
    description: '建于2010年，内设篮球场、羽毛球场、游泳馆、健身房等设施，是师生日常锻炼和举办体育赛事的重要场所。',
    totalFloors: 4,
    recommendRate: 94,
  },
  {
    name: '枫林广场',
    rating: 4.9,
    checkins: 6520,
    openTime: '全天开放',
    category: '景观',
    tags: ['景观', '打卡'],
    color: '#d47a4a',
    buildYear: '1980年',
    openTimeDetail: '全天开放',
    floors: '开放式广场',
    location: '主校区·中心区域',
    description: '建于1980年，种植枫树百余棵，秋季红叶满园，是校园最具代表性的景观之一，也是师生休闲散步、拍照打卡的热门地点。',
    totalFloors: 1,
    recommendRate: 98,
  },
]

const filteredLandmarks = landmarks

const renderStars = (rating: number) => {
  const full = Math.floor(rating)
  const half = rating - full >= 0.5
  const empty = 5 - full - (half ? 1 : 0)
  return { full, half, empty }
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
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索地标、类别..."
          class="search-input"
        />
      </div>

      <div class="category-tabs">
        <button
          v-for="cat in categories"
          :key="cat"
          class="category-tab"
          :class="{ active: activeCategory === cat }"
          @click="activeCategory = cat"
        >
          {{ cat }}
        </button>
      </div>

      <div class="landmark-stats">
        <span class="stats-text">共 <strong>{{ landmarks.length }}</strong> 个地标</span>
        <button class="sort-btn">{{ sortBy }}</button>
      </div>
    </div>

    <div class="landmark-list-scroll">
      <div class="landmark-list">
        <div
          v-for="(landmark, index) in filteredLandmarks"
          :key="index"
          class="landmark-card"
          @click="emit('select', landmark)"
        >
          <div class="landmark-image" :style="{ background: landmark.color }">
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

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
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
</style>
