<script setup lang="ts">
import { ref, computed } from 'vue'

interface LandmarkDetail {
  name: string
  rating: number
  checkins: number
  recommendRate: number
  tags: string[]
  buildYear: string
  openTimeDetail: string
  floors: string
  location: string
  description: string
  totalFloors: number
  color: string
}

interface FloorArea {
  floor: number
  areas: string[]
}

const props = defineProps<{
  landmark: LandmarkDetail
}>()

const emit = defineEmits<{
  back: []
}>()

const selectedFloor = ref(1)

const floorAreas: FloorArea[] = [
  { floor: 1, areas: ['入口大厅', '借阅服务台', '新书展示区', '咖啡休闲区'] },
  { floor: 2, areas: ['中文图书区', '期刊阅览室', '电子阅览区'] },
  { floor: 3, areas: ['外文图书区', '学术报告厅', '研讨室'] },
  { floor: 4, areas: ['自习室', '多媒体教室', '创客空间'] },
  { floor: 5, areas: ['特藏文献室', '档案室', '研究室'] },
  { floor: 6, areas: ['行政办公区', '会议室', '数据中心'] },
]

const currentFloorAreas = computed(() =>
  floorAreas.find(f => f.floor === selectedFloor.value)?.areas || []
)

const renderStars = (rating: number) => {
  const full = Math.floor(rating)
  const half = rating - full >= 0.5
  const empty = 5 - full - (half ? 1 : 0)
  return { full, half, empty }
}

const selectFloor = (floor: number) => {
  selectedFloor.value = floor
}
</script>

<template>
  <div class="landmark-detail">
    <div class="detail-header" :style="{ background: props.landmark.color }">
      <div class="header-overlay"></div>
      <div class="header-top">
        <button class="header-btn" @click="emit('back')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <div class="header-actions">
          <button class="header-btn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
            </svg>
          </button>
          <button class="header-btn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8" />
              <polyline points="16 6 12 2 8 6" />
              <line x1="12" y1="2" x2="12" y2="15" />
            </svg>
          </button>
        </div>
      </div>
      <div class="header-bottom">
        <h1 class="detail-title">{{ props.landmark.name }}</h1>
        <div class="image-dots">
          <span class="dot active"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
      </div>
    </div>

    <div class="detail-scroll">
      <div class="scroll-content">
        <div class="info-card">
          <div class="tags-row">
            <div class="tags-left">
              <span
                v-for="tag in props.landmark.tags"
                :key="tag"
                class="tag"
              >
                {{ tag }}
              </span>
            </div>
            <div class="rating-inline">
              <div class="stars">
                <template v-for="i in 5" :key="i">
                  <svg
                    v-if="i <= renderStars(props.landmark.rating).full"
                    class="star-icon filled"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                  >
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                  </svg>
                  <svg
                    v-else-if="i === renderStars(props.landmark.rating).full + 1 && renderStars(props.landmark.rating).half"
                    class="star-icon half"
                    viewBox="0 0 24 24"
                  >
                    <defs>
                      <linearGradient id="half-detail">
                        <stop offset="50%" stop-color="#f59e0b" />
                        <stop offset="50%" stop-color="#e5e7eb" />
                      </linearGradient>
                    </defs>
                    <path
                      fill="url(#half-detail)"
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
              <span class="rating-value">{{ props.landmark.rating }}</span>
            </div>
          </div>

          <div class="info-list">
            <div class="info-item">
              <div class="info-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                  <line x1="16" y1="2" x2="16" y2="6" />
                  <line x1="8" y1="2" x2="8" y2="6" />
                  <line x1="3" y1="10" x2="21" y2="10" />
                </svg>
              </div>
              <div class="info-text">
                <span class="info-label">建造时间</span>
                <span class="info-value">{{ props.landmark.buildYear }}</span>
              </div>
            </div>
            <div class="info-item">
              <div class="info-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
              </div>
              <div class="info-text">
                <span class="info-label">开放时间</span>
                <span class="info-value">{{ props.landmark.openTimeDetail }}</span>
              </div>
            </div>
            <div class="info-item">
              <div class="info-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="4" y="2" width="16" height="20" rx="2" />
                  <line x1="8" y1="6" x2="16" y2="6" />
                  <line x1="8" y1="10" x2="16" y2="10" />
                  <line x1="8" y1="14" x2="16" y2="14" />
                  <line x1="8" y1="18" x2="12" y2="18" />
                </svg>
              </div>
              <div class="info-text">
                <span class="info-label">楼层信息</span>
                <span class="info-value">{{ props.landmark.floors }}</span>
              </div>
            </div>
            <div class="info-item">
              <div class="info-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                  <circle cx="12" cy="10" r="3" />
                </svg>
              </div>
              <div class="info-text">
                <span class="info-label">校园位置</span>
                <span class="info-value">{{ props.landmark.location }}</span>
              </div>
            </div>
          </div>

          <div class="stats-row">
            <div class="stat-item">
              <svg class="stat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 00-3-3.87" />
                <path d="M16 3.13a4 4 0 010 7.75" />
              </svg>
              <div class="stat-text">
                <span class="stat-label">打卡人数</span>
                <span class="stat-value">{{ props.landmark.checkins.toLocaleString() }}</span>
              </div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <svg class="stat-icon heart" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
              </svg>
              <div class="stat-text">
                <span class="stat-label">推荐指数</span>
                <span class="stat-value highlight">{{ props.landmark.recommendRate }}%</span>
              </div>
            </div>
          </div>
        </div>

        <div class="description-card">
          <h3 class="card-title">地标简介</h3>
          <p class="description-text">{{ props.landmark.description }}</p>
        </div>

        <div class="floor-nav-card">
          <div class="card-header">
            <h3 class="card-title">室内楼层导航</h3>
            <span class="floor-count">共 {{ props.landmark.totalFloors }} 层</span>
          </div>
          <div class="floor-buttons">
            <button
              v-for="floor in props.landmark.totalFloors"
              :key="floor"
              class="floor-btn"
              :class="{ active: selectedFloor === floor }"
              @click="selectFloor(floor)"
            >
              {{ floor }}F
            </button>
          </div>
          <div class="floor-areas">
            <div class="floor-area-header">
              <span class="floor-area-label">{{ selectedFloor }}F 功能区域</span>
            </div>
            <div class="area-tags">
              <span
                v-for="area in currentFloorAreas"
                :key="area"
                class="area-tag"
              >
                {{ area }}
              </span>
            </div>
          </div>
        </div>

        <div class="location-card">
          <div class="card-header">
            <h3 class="card-title">地标位置</h3>
            <button class="view-map-btn">
              查看大地图
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </button>
          </div>
          <div class="map-placeholder">
            <div class="map-grid">
              <div class="map-dot" style="top: 30%; left: 40%"></div>
              <div class="map-dot" style="top: 50%; left: 60%"></div>
              <div class="map-dot" style="top: 70%; left: 25%"></div>
              <div class="map-dot" style="top: 45%; left: 75%"></div>
              <div class="map-dot center" style="top: 45%; left: 45%"></div>
            </div>
            <div class="map-pin">
              <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
              </svg>
              {{ props.landmark.name }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="detail-footer">
      <button class="action-btn outline">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polygon points="3 11 22 2 13 21 11 13 3 11" />
        </svg>
        导航前往
      </button>
      <button class="action-btn primary">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="20 6 9 17 4 12" />
        </svg>
        打卡签到
      </button>
    </div>
  </div>
</template>

<style scoped>
.landmark-detail {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f7f4;
}

.detail-header {
  position: relative;
  height: 220px;
  flex-shrink: 0;
  overflow: hidden;
}

.header-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.1) 0%, rgba(0, 0, 0, 0.3) 100%);
}

.header-top {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
}

.header-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
}

.header-btn:hover {
  background: rgba(0, 0, 0, 0.4);
}

.header-btn svg {
  width: 20px;
  height: 20px;
  color: #fff;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.header-bottom {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  z-index: 1;
}

.detail-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
}

.image-dots {
  display: flex;
  gap: 6px;
  justify-content: center;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
}

.dot.active {
  width: 24px;
  border-radius: 4px;
  background: #fff;
}

.detail-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.detail-scroll::-webkit-scrollbar {
  display: none;
}

.scroll-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 16px;
}

.info-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
}

.tags-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.tags-left {
  display: flex;
  gap: 6px;
}

.tag {
  font-size: 13px;
  color: #2d8a6e;
  background: #e8f5e9;
  padding: 4px 12px;
  border-radius: 12px;
}

.rating-inline {
  display: flex;
  align-items: center;
  gap: 6px;
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

.info-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.info-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e8f5e9;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-icon svg {
  width: 18px;
  height: 18px;
  color: #2d8a6e;
}

.info-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 12px;
  color: #9ca3af;
}

.info-value {
  font-size: 14px;
  color: #1a1a1a;
  font-weight: 500;
}

.stats-row {
  display: flex;
  align-items: center;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.stat-icon {
  width: 20px;
  height: 20px;
  color: #9ca3af;
}

.stat-icon.heart {
  color: #ef4444;
}

.stat-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 12px;
  color: #9ca3af;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #2d8a6e;
}

.stat-value.highlight {
  color: #ef4444;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: #f0f0f0;
}

.description-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 10px;
}

.description-text {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.6;
  margin: 0;
}

.floor-nav-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.floor-count {
  font-size: 13px;
  color: #2d8a6e;
  background: #e8f5e9;
  padding: 2px 10px;
  border-radius: 10px;
}

.floor-buttons {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  scrollbar-width: none;
  margin-bottom: 16px;
}

.floor-buttons::-webkit-scrollbar {
  display: none;
}

.floor-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  background: #e8f5e9;
  color: #6b7280;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.floor-btn.active {
  background: #2d8a6e;
  color: #fff;
}

.floor-areas {
  background: #f0f7f4;
  border-radius: 12px;
  padding: 14px;
}

.floor-area-header {
  margin-bottom: 10px;
}

.floor-area-label {
  font-size: 14px;
  font-weight: 600;
  color: #2d8a6e;
}

.area-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.area-tag {
  font-size: 13px;
  color: #1a1a1a;
  background: #fff;
  padding: 6px 14px;
  border-radius: 16px;
}

.location-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
}

.view-map-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  color: #2d8a6e;
  font-size: 13px;
  cursor: pointer;
}

.view-map-btn svg {
  width: 14px;
  height: 14px;
}

.map-placeholder {
  position: relative;
  height: 160px;
  background: #e8f5e9;
  border-radius: 12px;
  overflow: hidden;
}

.map-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(45, 138, 110, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(45, 138, 110, 0.1) 1px, transparent 1px);
  background-size: 30px 30px;
}

.map-dot {
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(45, 138, 110, 0.3);
  transform: translate(-50%, -50%);
}

.map-dot.center {
  width: 20px;
  height: 20px;
  background: rgba(45, 138, 110, 0.2);
  border: 2px solid #2d8a6e;
}

.map-dot.center::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2d8a6e;
}

.map-pin {
  position: absolute;
  bottom: 10px;
  right: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  background: #2d8a6e;
  color: #fff;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

.detail-footer {
  flex-shrink: 0;
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  padding-bottom: max(12px, env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid #e8f5e9;
}

.action-btn {
  flex: 1;
  height: 48px;
  border-radius: 12px;
  border: none;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn svg {
  width: 18px;
  height: 18px;
}

.action-btn.outline {
  background: #fff;
  color: #2d8a6e;
  border: 1.5px solid #2d8a6e;
}

.action-btn.outline:hover {
  background: #e8f5e9;
}

.action-btn.primary {
  background: #2d8a6e;
  color: #fff;
  box-shadow: 0 4px 12px rgba(45, 138, 110, 0.3);
}

.action-btn.primary:hover {
  background: #247a5e;
}
</style>
