<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { doCheckin } from '../../api/checkin'
import { toggleFavorite } from '../../api/favorite'
import { submitRating } from '../../api/rating'
import { wgs84ToGcj02 } from '../../utils/coordConvert'
import FeedbackPage from '../feedback/FeedbackPage.vue'
import ShareModal from '../../components/ShareModal.vue'
import { getComments, submitComment, likeComment, unlikeComment, type CommentItem } from '../../api/comment'

interface LandmarkDetail {
  id?: string | number
  name: string
  category: string
  rating: number
  checkins: number
  recommendRate: number
  favoriteCount?: number
  isFavorited?: boolean
  userRating?: number | null
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

const isFavorited = ref(props.landmark.isFavorited ?? false)
const favoriteCount = ref(props.landmark.favoriteCount ?? 0)

watch(() => props.landmark, (lm) => {
  isFavorited.value = lm.isFavorited ?? false
  favoriteCount.value = lm.favoriteCount ?? 0
})

async function handleToggleFavorite() {
  const landmarkId = String(props.landmark.id ?? '')
  if (!landmarkId) return
  try {
    const res = await toggleFavorite(landmarkId)
    if (res.code === '00000') {
      isFavorited.value = res.data
      favoriteCount.value += res.data ? 1 : -1
    }
  } catch { /* ignore */ }
}

const checkinMsg = ref('')
const checkinMsgType = ref<'success' | 'error' | 'info'>('success')
const checkinLocating = ref(false)
const checkinBtnDisabled = ref(false)

const showRatingPanel = ref(false)
const showFeedback = ref(false)
const showShareModal = ref(false)

// 社区评论
const comments = ref<CommentItem[]>([])
const commentText = ref('')
const replyTo = ref<{ id: string; nickname: string } | null>(null)
const loadingComments = ref(false)

const fetchComments = async () => {
  const landmarkId = String(props.landmark.id || '')
  if (!landmarkId) return
  loadingComments.value = true
  try {
    const res = await getComments(landmarkId)
    if (res.code === '00000') {
      comments.value = res.data
    }
  } catch { /* ignore */ }
  finally { loadingComments.value = false }
}

const handleSubmitComment = async () => {
  if (!commentText.value.trim()) return
  const landmarkId = String(props.landmark.id || '')
  if (!landmarkId) return
  try {
    const parentId = replyTo.value?.id
    const res = await submitComment(landmarkId, commentText.value.trim(), parentId)
    if (res.code === '00000') {
      commentText.value = ''
      replyTo.value = null
      await fetchComments()
    }
  } catch { /* ignore */ }
}

const handleToggleLike = async (comment: CommentItem) => {
  try {
    if (comment.isLiked) {
      const res = await unlikeComment(comment.id)
      if (res.code === '00000') {
        comment.isLiked = false
        comment.likeCount = Math.max(0, comment.likeCount - 1)
      }
    } else {
      const res = await likeComment(comment.id)
      if (res.code === '00000') {
        comment.isLiked = true
        comment.likeCount = comment.likeCount + 1
      }
    }
  } catch { /* ignore */ }
}

const setReplyTo = (c: CommentItem) => {
  replyTo.value = { id: c.id, nickname: c.nickname }
  commentText.value = ''
}

const cancelReply = () => {
  replyTo.value = null
  commentText.value = ''
}

const formatTime = (dateStr: string) => {
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return d.toLocaleDateString('zh-CN')
}

const ratingValue = ref(0)
const ratingHoverValue = ref(0)
const ratingSubmitting = ref(false)

function openRatingPanel() {
  ratingValue.value = 0
  ratingHoverValue.value = 0
  showRatingPanel.value = true
}

function closeRatingPanel() {
  showRatingPanel.value = false
  checkinMsg.value = '打卡成功'
  checkinMsgType.value = 'success'
  setTimeout(() => { checkinMsg.value = '' }, 2500)
}

function setRatingByPosition(event: MouseEvent | TouchEvent, starIndex: number) {
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const clientX = 'touches' in event ? event.touches[0].clientX : event.clientX
  const x = clientX - rect.left
  const half = x < rect.width / 2
  ratingValue.value = starIndex + 1 - (half ? 0.5 : 0)
}

function setRatingHover(event: MouseEvent | TouchEvent, starIndex: number) {
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const clientX = 'touches' in event ? event.touches[0].clientX : event.clientX
  const x = clientX - rect.left
  const half = x < rect.width / 2
  ratingHoverValue.value = starIndex + 1 - (half ? 0.5 : 0)
}

function clearRatingHover() {
  ratingHoverValue.value = 0
}

async function handleSubmitRating() {
  if (ratingValue.value <= 0) return
  const landmarkId = String(props.landmark.id ?? '')
  if (!landmarkId) return
  ratingSubmitting.value = true
  try {
    await submitRating(landmarkId, ratingValue.value)
  } catch { /* ignore */ }
  ratingSubmitting.value = false
  showRatingPanel.value = false
  checkinMsg.value = '打卡成功'
  checkinMsgType.value = 'success'
  setTimeout(() => { checkinMsg.value = '' }, 2500)
}

async function handleCheckin() {
  const landmarkId = String(props.landmark.id ?? '')
  if (!landmarkId || checkinBtnDisabled.value) return

  if (!navigator.geolocation) {
    checkinMsg.value = '当前设备不支持定位'
    checkinMsgType.value = 'error'
    setTimeout(() => { checkinMsg.value = '' }, 2500)
    return
  }

  checkinBtnDisabled.value = true
  checkinLocating.value = true
  checkinMsg.value = '定位中...'
  checkinMsgType.value = 'info'

  navigator.geolocation.getCurrentPosition(
    async (pos) => {
      const gcj = wgs84ToGcj02(pos.coords.latitude, pos.coords.longitude)
      const latitude = gcj.lat
      const longitude = gcj.lng
      checkinLocating.value = false
      try {
        const res = await doCheckin(landmarkId, latitude, longitude)
        if (res.code === '00000') {
          checkinMsg.value = '打卡成功'
          checkinMsgType.value = 'success'
          emit('checkin-success', landmarkId)
          openRatingPanel()
        } else {
          checkinMsg.value = res.message || '打卡失败'
          checkinMsgType.value = 'error'
          setTimeout(() => { checkinMsg.value = '' }, 2500)
        }
      } catch {
        checkinMsg.value = '网络错误，请重试'
        checkinMsgType.value = 'error'
        setTimeout(() => { checkinMsg.value = '' }, 2500)
      }
      checkinBtnDisabled.value = false
    },
    (err) => {
      checkinLocating.value = false
      checkinBtnDisabled.value = false
      const messages: Record<number, string> = {
        1: '定位失败，请开启位置权限',
        2: '定位失败，请检查GPS或网络',
        3: '定位超时，请重试',
      }
      checkinMsg.value = messages[err.code] || '定位失败'
      checkinMsgType.value = 'error'
      setTimeout(() => { checkinMsg.value = '' }, 2500)
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 }
  )
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

  fetchComments()
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
const touchStartY = ref(0)

const selectImage = (index: number) => {
  currentImageIndex.value = index
}

const handleTouchStart = (e: TouchEvent) => {
  touchStartX.value = e.touches[0].clientX
  touchStartY.value = e.touches[0].clientY
}

const handleTouchEnd = (e: TouchEvent) => {
  const deltaX = e.changedTouches[0].clientX - touchStartX.value
  const deltaY = e.changedTouches[0].clientY - touchStartY.value
  const imgs = props.landmark.imgs
  if (imgs && imgs.length > 1 && Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 20) {
    if (deltaX < 0 && currentImageIndex.value < imgs.length - 1) {
      currentImageIndex.value++
    } else if (deltaX > 0 && currentImageIndex.value > 0) {
      currentImageIndex.value--
    }
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
    <!-- Fixed top bar -->
    <div class="detail-topbar">
      <button class="topbar-btn" @click="emit('back')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <span class="topbar-title">{{ props.landmark.name }}</span>
      <div class="topbar-actions">
        <button class="topbar-btn" :class="{ 'favorited': isFavorited }" @click="handleToggleFavorite">
          <svg viewBox="0 0 24 24" :fill="isFavorited ? '#e74c3c' : 'none'" stroke="currentColor" stroke-width="2">
            <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
          </svg>
        </button>
        <button class="topbar-btn" @click="showShareModal = true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8" />
            <polyline points="16 6 12 2 8 6" />
            <line x1="12" y1="2" x2="12" y2="15" />
          </svg>
        </button>
      </div>
    </div>

    <div class="detail-scroll">
      <div class="scroll-content">
        <!-- 精选图片 -->
        <div v-if="props.landmark.imgs?.length" class="image-gallery">
          <div
            class="image-card"
            @touchstart="handleTouchStart"
            @touchend="handleTouchEnd"
          >
            <img :src="props.landmark.imgs[currentImageIndex]" :alt="props.landmark.name" />
            <span class="image-counter" v-if="props.landmark.imgs.length > 1">
              {{ currentImageIndex + 1 }}/{{ props.landmark.imgs.length }}
            </span>
          </div>
          <div class="thumbnail-strip" v-if="props.landmark.imgs.length > 1">
            <button
              v-for="(img, i) in props.landmark.imgs"
              :key="i"
              class="thumbnail"
              :class="{ active: i === currentImageIndex }"
              @click.stop="selectImage(i)"
            >
              <img :src="img" :alt="'精选 ' + (i + 1)" />
            </button>
          </div>
        </div>
        <div v-else class="image-placeholder-card">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <circle cx="8.5" cy="8.5" r="1.5" />
            <path d="M21 15l-5-5L5 21" />
          </svg>
          <span>暂无精选图片</span>
        </div>

        <div class="info-card">
          <div class="tags-row">
            <div class="campus-info">
              <span class="landmark-name">{{ props.landmark.name }}</span>
              <span class="university-name">{{ props.landmark.universityName }}·{{ props.landmark.campusName }}</span>
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
            <div class="stat-item">
              <svg class="stat-icon heart" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
              </svg>
              <div class="stat-text">
                <span class="stat-label">收藏人数</span>
                <span class="stat-value highlight">{{ favoriteCount.toLocaleString() }}</span>
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

        <div class="detail-feedback-row">
          <span class="feedback-text-link" @click="showFeedback = true">有问题？去反馈</span>
        </div>

        <!-- 社区讨论 -->
        <div class="comment-section">
          <div class="comment-header">
            <span class="comment-title">社区讨论</span>
            <span class="comment-count" v-if="comments.length">{{ comments.length }} 条评论</span>
          </div>

          <div class="comment-list" v-if="comments.length > 0">
            <div v-for="c in comments" :key="c.id" class="comment-item">
              <div class="comment-avatar">{{ c.nickname.charAt(0) }}</div>
              <div class="comment-body">
                <div class="comment-info">
                  <span class="comment-nickname">{{ c.nickname }}</span>
                  <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
                </div>
                <p class="comment-content">{{ c.content }}</p>
                <div class="comment-actions">
                  <button class="comment-action-btn" @click="handleToggleLike(c)">
                    <svg viewBox="0 0 24 24" :fill="c.isLiked ? '#e74c3c' : 'none'" :stroke="c.isLiked ? '#e74c3c' : '#999'" stroke-width="2" width="16" height="16">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                    </svg>
                    <span v-if="c.likeCount > 0" class="like-num">{{ c.likeCount }}</span>
                  </button>
                  <button class="comment-action-btn reply-btn" @click="setReplyTo(c)">回复</button>
                </div>
                <div v-if="c.replies && c.replies.length > 0" class="comment-replies">
                  <div v-for="r in c.replies" :key="r.id" class="reply-item">
                    <div class="comment-avatar reply-avatar">{{ r.nickname.charAt(0) }}</div>
                    <div class="comment-body">
                      <div class="comment-info">
                        <span class="comment-nickname">{{ r.nickname }}</span>
                        <span class="comment-time">{{ formatTime(r.createdAt) }}</span>
                      </div>
                      <p class="comment-content">{{ r.content }}</p>
                      <div class="comment-actions">
                        <button class="comment-action-btn" @click="handleToggleLike(r)">
                          <svg viewBox="0 0 24 24" :fill="r.isLiked ? '#e74c3c' : 'none'" :stroke="r.isLiked ? '#e74c3c' : '#999'" stroke-width="2" width="14" height="14">
                            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                          </svg>
                          <span v-if="r.likeCount > 0" class="like-num small">{{ r.likeCount }}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="!loadingComments" class="comment-empty">暂无评论，来说两句吧</div>
          <div v-else class="comment-empty">加载中...</div>

          <div class="comment-input-bar">
            <span v-if="replyTo" class="reply-hint">
              回复 @{{ replyTo.nickname }}
              <button class="cancel-reply" @click="cancelReply">取消</button>
            </span>
            <div class="input-row">
              <input
                v-model="commentText"
                class="comment-input"
                :placeholder="replyTo ? '写下你的回复...' : '写下你的评论...'"
                maxlength="500"
                @keyup.enter="handleSubmitComment"
              />
              <button class="send-btn" :disabled="!commentText.trim()" @click="handleSubmitComment">发送</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="detail-footer">
      <button class="action-btn outline" @click="handleViewLargeMap">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polygon points="3 11 22 2 13 21 11 13 3 11" />
        </svg>
        导航前往
      </button>
      <button class="action-btn primary" :disabled="checkinBtnDisabled" @click="handleCheckin">
        <svg v-if="!checkinLocating" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="20 6 9 17 4 12" />
        </svg>
        <svg v-else class="loading-spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" stroke-dasharray="31.4 31.4" stroke-linecap="round" />
        </svg>
        {{ checkinLocating ? '定位中...' : '打卡签到' }}
      </button>
    </div>

    <div v-if="checkinMsg" class="checkin-toast" :class="checkinMsgType">
      {{ checkinMsg }}
    </div>

    <!-- 评分半屏面板 -->
    <div v-if="showRatingPanel" class="rating-overlay" @click.self="closeRatingPanel">
      <div class="rating-panel">
        <div class="rating-panel-header">
          <span class="rating-panel-title">为 {{ props.landmark.name }} 评分</span>
          <button class="rating-close-btn" @click="closeRatingPanel">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
        <div class="rating-stars-row">
          <template v-for="i in 5" :key="i">
            <span
              class="rating-star-big"
              @click="setRatingByPosition($event, i - 1)"
              @touchstart="setRatingByPosition($event, i - 1)"
              @mousemove="setRatingHover($event, i - 1)"
              @touchmove="setRatingHover($event, i - 1)"
              @mouseleave="clearRatingHover"
            >
              <svg viewBox="0 0 24 24" width="44" height="44">
                <defs>
                  <linearGradient :id="'rating-grad-' + i">
                    <stop offset="50%" :stop-color="(ratingHoverValue || ratingValue) >= i - 0.5 ? '#f59e0b' : '#e5e7eb'" />
                    <stop offset="50%" :stop-color="(ratingHoverValue || ratingValue) >= i ? '#f59e0b' : '#e5e7eb'" />
                  </linearGradient>
                </defs>
                <path
                  :fill="(ratingHoverValue || ratingValue) >= i ? '#f59e0b' : ((ratingHoverValue || ratingValue) >= i - 0.5 ? `url(#rating-grad-${i})` : '#e5e7eb')"
                  d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
                />
              </svg>
            </span>
          </template>
        </div>
        <div class="rating-panel-value">{{ ratingValue > 0 ? ratingValue + ' 分' : '点击星星评分' }}</div>
        <button
          class="rating-submit-btn"
          :disabled="ratingValue <= 0 || ratingSubmitting"
          @click="handleSubmitRating"
        >
          {{ ratingSubmitting ? '提交中...' : (ratingValue > 0 ? '提交评分 ' + ratingValue + ' 分' : '提交评分') }}
        </button>
      </div>
    </div>
  </div>

  <FeedbackPage
    v-if="showFeedback"
    default-type="INFO_ERROR"
    :landmark-id="String(props.landmark.id || '')"
    :landmark-name="props.landmark.name"
    @done="showFeedback = false"
    @back="showFeedback = false"
  />

  <ShareModal
    v-if="showShareModal"
    :landmark="props.landmark"
    @close="showShareModal = false"
  />
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

/* ── Top Bar ── */
.detail-topbar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  flex-shrink: 0;
  background: var(--color-bg-card);
  z-index: 10;
}

.topbar-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-text);
}

.topbar-btn svg {
  width: 20px;
  height: 20px;
}

.topbar-btn.favorited svg {
  color: #e74c3c;
}

.topbar-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text-heading);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  text-align: center;
  margin: 0 12px;
}

.topbar-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

/* ── Image Gallery ── */
.image-gallery {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.image-card {
  position: relative;
  border-radius: var(--radius-xl);
  overflow: hidden;
  aspect-ratio: 4 / 4;
  background: var(--color-bg-input);
}

.image-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.image-counter {
  position: absolute;
  bottom: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
}

/* ── Thumbnail Strip ── */
.thumbnail-strip {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.thumbnail-strip::-webkit-scrollbar {
  display: none;
}

.thumbnail {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid transparent;
  padding: 0;
  cursor: pointer;
  flex-shrink: 0;
  transition: border-color 0.2s;
}

.thumbnail.active {
  border-color: var(--color-primary);
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* ── Image Placeholder ── */
.image-placeholder-card {
  border-radius: var(--radius-xl);
  background: var(--color-bg-input);
  aspect-ratio: 4 / 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--color-text-muted);
}

.image-placeholder-card svg {
  width: 40px;
  height: 40px;
}

.image-placeholder-card span {
  font-size: 13px;
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

.landmark-name {
  font-size: 18px;
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

.detail-feedback-row {
  display: flex;
  justify-content: center;
  padding: 8px 0 0;
}

.feedback-text-link {
  font-size: 13px;
  color: var(--color-primary);
  cursor: pointer;
}

.feedback-text-link:active {
  opacity: 0.7;
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

.checkin-toast.info {
  background: #cce5ff;
  color: #004085;
  box-shadow: 0 4px 16px rgba(0, 64, 133, 0.2);
}

.action-btn.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes toast-fade {
  0% { opacity: 0; transform: translate(-50%, -50%) scale(0.8); }
  15% { opacity: 1; transform: translate(-50%, -50%) scale(1); }
  80% { opacity: 1; transform: translate(-50%, -50%) scale(1); }
  100% { opacity: 0; transform: translate(-50%, -50%) scale(0.8); }
}

/* 评分半屏面板 */
.rating-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.rating-panel {
  width: 100%;
  max-width: 480px;
  background: var(--color-bg-card);
  border-radius: 20px 20px 0 0;
  padding: 24px 20px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  animation: rating-slide-up 0.3s ease-out;
}

@keyframes rating-slide-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.rating-panel-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rating-panel-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-heading);
}

.rating-close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: var(--color-bg-input);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.rating-close-btn svg {
  width: 16px;
  height: 16px;
  color: var(--color-text-secondary);
}

.rating-stars-row {
  display: flex;
  gap: 8px;
}

.rating-star-big {
  cursor: pointer;
  transition: transform 0.15s;
  user-select: none;
  -webkit-user-select: none;
  touch-action: manipulation;
}

.rating-star-big:active {
  transform: scale(1.15);
}

.rating-panel-value {
  font-size: 15px;
  color: var(--color-text-secondary);
}

.rating-submit-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--color-primary);
  color: #fff;
}

.rating-submit-btn:disabled {
  background: var(--color-bg-input);
  color: var(--color-text-muted);
  cursor: not-allowed;
}

/* 社区评论 */
.comment-section {
  margin-top: 16px;
  border-top: 1px solid var(--color-border);
  padding-bottom: 16px;
}

.comment-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 14px 0 10px;
}

.comment-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-heading);
}

.comment-count {
  font-size: 12px;
  color: var(--color-text-muted);
}

.comment-list {
  padding-bottom: 8px;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(0,0,0,0.04);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), #6db39e);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.reply-avatar {
  width: 28px;
  height: 28px;
  font-size: 12px;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-nickname {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-heading);
}

.comment-time {
  font-size: 11px;
  color: var(--color-text-muted);
}

.comment-content {
  font-size: 14px;
  color: var(--color-text);
  line-height: 1.5;
  margin-bottom: 6px;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 16px;
}

.comment-action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  padding: 2px 0;
  font-size: 12px;
  color: #999;
  cursor: pointer;
}

.comment-action-btn:active {
  opacity: 0.6;
}

.like-num {
  font-size: 12px;
  color: #999;
}

.like-num.small {
  font-size: 11px;
}

.reply-btn {
  color: #999;
}

.comment-replies {
  margin-top: 8px;
  padding: 8px 0 8px 12px;
  background: rgba(0,0,0,0.02);
  border-radius: 8px;
  border-left: 2px solid rgba(0,0,0,0.06);
}

.reply-item {
  display: flex;
  gap: 8px;
  padding: 6px 0;
}

.reply-item:last-child {
  padding-bottom: 0;
}

.reply-item + .reply-item {
  border-top: 1px solid rgba(0,0,0,0.03);
}

.comment-empty {
  padding: 20px 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-muted);
}

.comment-input-bar {
  padding: 10px 0;
  border-top: 1px solid rgba(0,0,0,0.06);
}

.reply-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-primary);
  margin-bottom: 6px;
}

.cancel-reply {
  background: none;
  border: none;
  font-size: 12px;
  color: #999;
  cursor: pointer;
}

.input-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.comment-input {
  flex: 1;
  height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(0,0,0,0.1);
  border-radius: 19px;
  font-size: 14px;
  background: rgba(0,0,0,0.03);
  outline: none;
  transition: border-color 0.2s;
}

.comment-input:focus {
  border-color: var(--color-primary);
}

.send-btn {
  flex-shrink: 0;
  height: 38px;
  padding: 0 18px;
  border-radius: 19px;
  background: var(--color-primary);
  border: none;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.send-btn:not(:disabled):active {
  transform: scale(0.95);
}
</style>
