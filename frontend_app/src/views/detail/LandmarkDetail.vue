<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { doCheckin } from '../../api/checkin'

interface LandmarkDetail {
  id?: number
  name: string
  category: string
  rating: number
  checkins: number
  recommendRate: number
  openTime: string
  tags: string[]
  imgs: string[]
  campusName: string
  universityName: string
  buildYear: string
  openTimeDetail: string
  floors: string
  location: string
  latitude: number
  longitude: number
  description: string
  totalFloors: number
  floorList: FloorInfo[]
  color: string
}

interface FloorInfo {
  floorNumber: number
  floorName: string
  tags: string[]
}

const props = defineProps<{
  landmark: LandmarkDetail
}>()

const emit = defineEmits<{
  back: []
  'navigate-map': [data: {
    id: string
    name: string
    lat: number
    lng: number
    category: string
    rating: number
    openTime: string
  }]
  'checkin-success': [landmarkId: string]
}>()

const minimapContainer = ref<HTMLDivElement>()

const selectedFloorNumber = ref(1)

const currentFloor = computed(() =>
    props.landmark.floorList?.find(f => f.floorNumber === selectedFloorNumber.value)
)

const renderStars = (rating: number) => {
  const full = Math.floor(rating)
  const half = rating - full >= 0.5
  const empty = 5 - full - (half ? 1 : 0)
  return { full, half, empty }
}

const selectFloor = (floorNumber: number) => {
  selectedFloorNumber.value = floorNumber
}

const checkinMsg = ref('')
const checkinMsgType = ref<'success' | 'error'>('success')

async function handleCheckin() {
  const landmarkId = String(props.landmark.id ?? '')
  if (!landmarkId) return
  try {
    const res = await doCheckin(landmarkId)
    if (res.code === '00000') {
      checkinMsg.value = '打卡成功'
      checkinMsgType.value = 'success'
      emit('checkin-success', landmarkId)
    } else {
      checkinMsg.value = res.message || '打卡失败'
      checkinMsgType.value = 'error'
    }
  } catch {
    checkinMsg.value = '网络错误，请重试'
    checkinMsgType.value = 'error'
  }
  setTimeout(() => { checkinMsg.value = '' }, 2500)
}

onMounted(() => {
  if (!minimapContainer.value) return
  const lat = props.landmark.latitude || 0
  const lng = props.landmark.longitude || 0
  if (!lat && !lng) return

  const minimap = L.map(minimapContainer.value, {
    zoomControl: false,
    attributionControl: false,
    dragging: false,
    scrollWheelZoom: false,
    doubleClickZoom: false,
    touchZoom: false,
    boxZoom: false,
    keyboard: false,
  }).setView([lat, lng], 16)

  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
  }).addTo(minimap)

  L.marker([lat, lng]).addTo(minimap)
})

function handleViewLargeMap() {
  emit('navigate-map', {
    id: String(props.landmark.id || ''),
    name: props.landmark.name,
    lat: props.landmark.latitude || 0,
    lng: props.landmark.longitude || 0,
    category: props.landmark.category,
    rating: props.landmark.rating,
    openTime: props.landmark.openTime || '',
  })
}

const currentImageIndex = ref(0)
const touchStartX = ref(0)

const selectImage = (index: number) => {
  currentImageIndex.value = index
}

const handleTouchStart = (e: TouchEvent) => {
  touchStartX.value = e.touches[0].clientX
}

const handleTouchEnd = (e: TouchEvent) => {
  const deltaX = e.changedTouches[0].clientX - touchStartX.value
  const imgs = props.landmark.imgs
  if (!imgs || imgs.length <= 1) return
  if (deltaX < -40 && currentImageIndex.value < imgs.length - 1) {
    currentImageIndex.value++
  } else if (deltaX > 40 && currentImageIndex.value > 0) {
    currentImageIndex.value--
  }
}

const tagBubbleSizes = computed(() => {
  return props.landmark.tags.map((_, i) => {
    const sizes = [56, 66, 74, 60, 68, 58]
    return sizes[i % sizes.length]
  })
})

const tagBubbleGradients = [
  'linear-gradient(135deg, #d4edda, #8ed1a8)',
  'linear-gradient(135deg, #c8e6d3, #6cba90)',
  'linear-gradient(135deg, #e8f5e9, #a8dfc0)',
  'linear-gradient(135deg, #d0f0de, #7dbf9a)',
  'linear-gradient(135deg, #b8e6cc, #5fa87a)',
  'linear-gradient(135deg, #e2f5e8, #95c9aa)',
]

const bubblePositions = computed(() => {
  const count = props.landmark.tags.length
  const sets: Record<number, { left: number; top: number }[]> = {
    1: [{ left: 42, top: 32 }],
    2: [{ left: 50, top: 8 },  { left: 6, top: 50 }],
    3: [{ left: 42, top: 0 },  { left: 0, top: 40 },   { left: 60, top: 54 }],
    4: [{ left: 34, top: 0 },  { left: 74, top: 12 },  { left: 0, top: 34 },   { left: 56, top: 62 }],
    5: [{ left: 22, top: 0 },  { left: 68, top: 6 },   { left: 0, top: 34 },   { left: 80, top: 40 },  { left: 38, top: 62 }],
    6: [{ left: 16, top: 0 },  { left: 62, top: 2 },   { left: 0, top: 32 },   { left: 78, top: 30 },  { left: 22, top: 62 },  { left: 60, top: 66 }],
  }
  return sets[Math.min(count, 6)] || sets[6]
})
</script>

<template>
  <div class="landmark-detail">
    <div
        class="detail-header"
        :style="{
        backgroundImage: props.landmark.imgs?.length ? `url(${props.landmark.imgs[currentImageIndex]})` : 'none',
        backgroundSize: 'cover',
        backgroundPosition: 'center'
      }"
        @touchstart="handleTouchStart"
        @touchend="handleTouchEnd"
    >
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
          <span
              v-for="(_img, i) in props.landmark.imgs"
              :key="i"
              class="dot"
              :class="{ active: i === currentImageIndex }"
              @click="selectImage(i)"
          ></span>
        </div>
      </div>
    </div>

    <div class="detail-scroll">
      <div class="scroll-content">
        <div class="info-card">
          <div class="tags-row">
            <div class="campus-info">
              <span class="campus-name">{{ props.landmark.campusName }}</span>
              <span class="university-name">{{ props.landmark.universityName }}</span>
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
            <div class="tags-bubbles">
              <span
                  v-for="(tag, i) in props.landmark.tags"
                  :key="tag"
                  class="tag-bubble"
                  :style="{
                  width: tagBubbleSizes[i] + 'px',
                  height: tagBubbleSizes[i] + 'px',
                  background: tagBubbleGradients[i % tagBubbleGradients.length],
                  left: bubblePositions[i]?.left + 'px',
                  top: bubblePositions[i]?.top + 'px'
                }"
              >
                {{ tag }}
              </span>
            </div>
          </div>

          <div class="info-list">
            <div class="info-item">
              <div class="info-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2z" />
                  <path d="M22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z" />
                </svg>
              </div>
              <div class="info-text">
                <span class="info-label">地标分类</span>
                <span class="info-value">{{ props.landmark.category }}</span>
              </div>
            </div>
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
                v-for="f in props.landmark.floorList"
                :key="f.floorNumber"
                class="floor-btn"
                :class="{ active: selectedFloorNumber === f.floorNumber }"
                @click="selectFloor(f.floorNumber)"
            >
              {{ f.floorName }}
            </button>
          </div>
          <div class="floor-areas">
            <div class="floor-area-header">
              <span class="floor-area-label">{{ currentFloor?.floorName }} 功能区域</span>
            </div>
            <div class="area-tags">
              <span
                  v-for="tag in currentFloor?.tags"
                  :key="tag"
                  class="area-tag"
              >
                {{ tag }}
              </span>
            </div>
          </div>
        </div>

        <div class="location-card">
          <div class="card-header">
            <h3 class="card-title">地标位置</h3>
            <button class="view-map-btn" @click="handleViewLargeMap">
              查看大地图
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </button>
          </div>
          <div v-if="props.landmark.latitude && props.landmark.longitude" class="map-minimap-wrapper">
            <div ref="minimapContainer" class="map-minimap"></div>
          </div>
          <div v-else class="map-placeholder">
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
      <button class="action-btn primary" @click="handleCheckin">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="20 6 9 17 4 12" />
        </svg>
        打卡签到
      </button>
    </div>

    <div v-if="checkinMsg" class="checkin-toast" :class="checkinMsgType">
      {{ checkinMsg }}
    </div>
  </div>
</template>

<style scoped>
.landmark-detail {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
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
  font-weight: 600;
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
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: 16px;
}

.tags-row {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.campus-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
}

.campus-name {
  font-size: 15px;
  color: var(--color-text-heading);
  font-weight: 600;
}

.university-name {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.tags-bubbles {
  position: relative;
  width: 150px;
  height: 120px;
  flex-shrink: 0;
  margin-left: auto;
}

.tag-bubble {
  position: absolute;
  border-radius: 50%;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  line-height: 1.2;
  padding: 4px;
  overflow: hidden;
  word-break: break-all;
  box-shadow: 0 2px 6px rgba(60, 160, 122, 0.15);
  transition: transform 0.2s, box-shadow 0.2s;
}

.tag-bubble:hover {
  transform: scale(1.08);
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

.info-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-divider);
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
  background: var(--color-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-icon svg {
  width: 18px;
  height: 18px;
  color: var(--color-primary);
}

.info-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.info-value {
  font-size: 14px;
  color: var(--color-text-heading);
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
  color: var(--color-text-secondary);
}

.stat-icon.heart {
  color: var(--color-danger);
}

.stat-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-primary);
}

.stat-value.highlight {
  color: var(--color-danger);
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--color-divider);
}

.description-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-heading);
  margin: 0 0 10px;
}

.description-text {
  font-size: 14px;
  color: var(--color-text);
  line-height: 1.6;
  margin: 0;
}

.floor-nav-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
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
  color: var(--color-primary);
  background: var(--color-primary-light);
  padding: 2px 10px;
  border-radius: var(--radius-md);
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
  height: 32px;
  border-radius: 16px;
  border: none;
  background: var(--color-bg-input);
  color: var(--color-text);
  font-size: 11px;
  font-weight: 600;
  padding: 0 12px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.floor-btn.active {
  background: var(--color-primary);
  color: #fff;
}

.floor-areas {
  background: var(--color-bg);
  border-radius: var(--radius-md);
  padding: 14px;
}

.floor-area-header {
  margin-bottom: 10px;
}

.floor-area-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}

.area-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.area-tag {
  font-size: 13px;
  color: var(--color-text-heading);
  background: var(--color-bg-card);
  padding: 6px 14px;
  border-radius: 16px;
}

.location-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: 16px;
}

.view-map-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  color: var(--color-primary);
  font-size: 13px;
  cursor: pointer;
}

.view-map-btn svg {
  width: 14px;
  height: 14px;
}

.map-minimap-wrapper {
  position: relative;
  height: 160px;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.map-minimap {
  width: 100%;
  height: 100%;
}

.map-minimap-wrapper :deep(.leaflet-control-attribution) {
  display: none;
}

.map-placeholder {
  position: relative;
  height: 160px;
  background: var(--color-bg-input);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.map-grid {
  position: absolute;
  inset: 0;
  background-image:
      linear-gradient(rgba(60, 160, 122, 0.1) 1px, transparent 1px),
      linear-gradient(90deg, rgba(60, 160, 122, 0.1) 1px, transparent 1px);
  background-size: 30px 30px;
}

.map-dot {
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(60, 160, 122, 0.3);
  transform: translate(-50%, -50%);
}

.map-dot.center {
  width: 20px;
  height: 20px;
  background: rgba(60, 160, 122, 0.2);
  border: 2px solid var(--color-primary);
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
  background: var(--color-primary);
}

.map-pin {
  position: absolute;
  bottom: 10px;
  right: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  background: var(--color-primary);
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
  background: var(--color-bg-card);
  border-top: 1px solid var(--color-border);
}

.action-btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-lg);
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
  background: var(--color-bg-card);
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
}

.action-btn.outline:hover {
  background: var(--color-primary-light);
}

.action-btn.primary {
  background: var(--color-primary);
  color: #fff;
  box-shadow: 0 4px 12px var(--color-primary-shadow);
}

.action-btn.primary:hover {
  background: var(--color-primary-hover);
}

.checkin-toast {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 12px 28px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  z-index: 9999;
  pointer-events: none;
  animation: toast-fade 2.5s ease-in-out forwards;
}

.checkin-toast.success {
  background: #d4edda;
  color: #155724;
  box-shadow: 0 4px 16px rgba(21, 87, 36, 0.2);
}

.checkin-toast.error {
  background: #f8d7da;
  color: #721c24;
  box-shadow: 0 4px 16px rgba(114, 28, 36, 0.2);
}

@keyframes toast-fade {
  0% { opacity: 0; transform: translate(-50%, -50%) scale(0.8); }
  15% { opacity: 1; transform: translate(-50%, -50%) scale(1); }
  80% { opacity: 1; transform: translate(-50%, -50%) scale(1); }
  100% { opacity: 0; transform: translate(-50%, -50%) scale(0.8); }
}
</style>
