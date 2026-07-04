<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, onActivated, watch, nextTick } from 'vue'
import axios from 'axios'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useNavigation } from '../../composables/useNavigation'
import { useSpeech } from '../../composables/useSpeech'
import { wgs84ToGcj02 } from '../../utils/coordConvert'
import { API_BASE } from '../../config'

interface LandmarkMarker {
  id: string
  name: string
  lat: number
  lng: number
  category: string
  rating: number
  openTime: string
  checkins?: number
}

const API_BASE_URL = `${API_BASE}/api/v1`

const emit = defineEmits<{
  'open-detail': [landmarkId: string]
}>()

interface FocusMarker {
  id: string
  name: string
  lat: number
  lng: number
  category: string
  rating: number
  openTime: string
}

const props = defineProps<{
  focusLandmark: FocusMarker | null
}>()

const landmarks = ref<LandmarkMarker[]>([])
const DEFAULT_CENTER: [number, number] = [23.0500, 113.3995]
const DEFAULT_ZOOM = 15

let map: L.Map | null = null
const markers: L.Marker[] = []

const searchQuery = ref('')
const suggestions = ref<LandmarkMarker[]>([])
const showSuggestions = ref(false)
const loading = ref(false)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const hotRankings = ref<LandmarkMarker[]>([])
const isSearchFocused = ref(false)
const showRanking = computed(() => isSearchFocused.value && !searchQuery.value.trim() && hotRankings.value.length > 0)

const categories = ['全部', '教学楼', '图书馆', '体育场馆', '生活区', '活动场馆', '景观景点']
const activeCategory = ref('全部')

const filteredLandmarks = computed(() => {
  if (activeCategory.value === '全部') return landmarks.value
  return landmarks.value.filter(l => l.category === activeCategory.value)
})

const selectedLandmark = ref<LandmarkMarker | null>(null)
let activeMarkerData: { marker: L.Marker; lm: LandmarkMarker } | null = null

const isFirstActivation = ref(true)

const starData = computed(() => {
  if (!selectedLandmark.value) return { full: 0, hasHalf: false }
  const rating = selectedLandmark.value.rating || 0
  return {
    full: Math.floor(rating),
    hasHalf: rating - Math.floor(rating) >= 0.5,
  }
})

const nav = useNavigation()
const navIsNavigating = nav.isNavigating
const navIsRouting = nav.isRouting
const navHasError = nav.navError
const navHasArrived = nav.hasArrived
const navRouteDeviation = nav.routeDeviation
const navDest = nav.destination
const navCurInstr = nav.currentInstruction
const navNextInstr = nav.nextInstruction
const navRemDist = nav.remainingDistance
const navRemTime = nav.remainingTime
const { speak } = useSpeech()

function exitNavigation() {
  nav.stopNavigation()
  clearNavLayers()
}
let routePolyline: L.Polyline | null = null
let userMarker: L.Marker | null = null
let endMarker: L.Marker | null = null

const mapContainer = ref<HTMLDivElement>()

function createLabelIcon(name: string, active: boolean): L.DivIcon {
  return L.divIcon({
    html: `<span class="marker-label${active ? ' active' : ''}">${name}</span>`,
    className: 'label-icon-container',
    iconSize: [150, 30] as any,
    iconAnchor: [75, 30] as any,
  })
}

function createUserMarkerIcon(heading?: number): L.DivIcon {
  const deg = heading ?? 0
  return L.divIcon({
    html: `<div class="user-arrow" style="transform:rotate(${deg}deg)">
      <svg viewBox="0 0 32 32" width="28" height="28">
        <circle cx="16" cy="16" r="15" fill="#3388ff" opacity="0.2"/>
        <path d="M16 4L8 24h16z" fill="#3388ff" stroke="#fff" stroke-width="1.5"/>
      </svg>
    </div>`,
    className: 'user-marker-icon',
    iconSize: [28, 28] as any,
    iconAnchor: [14, 14] as any,
  })
}

function onMarkerClick(lm: LandmarkMarker, marker: L.Marker) {
  if (activeMarkerData) {
    activeMarkerData.marker.setIcon(createLabelIcon(activeMarkerData.lm.name, false))
  }
  marker.setIcon(createLabelIcon(lm.name, true))
  activeMarkerData = { marker, lm }
  selectedLandmark.value = lm
}

function closeCard() {
  selectedLandmark.value = null
  if (activeMarkerData) {
    activeMarkerData.marker.setIcon(createLabelIcon(activeMarkerData.lm.name, false))
    activeMarkerData = null
  }
}

function startNavigation() {
  if (!selectedLandmark.value) return
  const { lat, lng, name } = selectedLandmark.value
  nav.startNavigation({ lat, lng, name })
}

function clearNavLayers() {
  if (!map) return
  if (routePolyline) { map.removeLayer(routePolyline); routePolyline = null }
  if (userMarker) { map.removeLayer(userMarker); userMarker = null }
  if (endMarker) { map.removeLayer(endMarker); endMarker = null }
}

function onRecenter() {
  const pos = nav.userPosition.value
  if (!map || !pos) return
  map.flyTo([pos.lat, pos.lng], 17)
}

function fmtDist(m: number): string {
  if (m >= 1000) return `${(m / 1000).toFixed(1)}公里`
  return `${Math.round(m)}米`
}
function fmtTime(s: number): string {
  if (s < 60) return `${Math.round(s)}秒`
  const m = Math.round(s / 60)
  return `${m}分钟`
}

function initMap(lat: number, lng: number) {
  if (map) return

  map = L.map(mapContainer.value!, {
    zoomControl: false,
  }).setView([lat, lng], DEFAULT_ZOOM)


  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    attribution: '&copy; <a href="https://www.amap.com/">高德地图</a>',
    maxZoom: 18,
  }).addTo(map)

  refreshMarkers()
}

function refreshMarkers() {
  if (!map) return
  markers.forEach(m => m.remove())
  markers.length = 0
  activeMarkerData = null
  selectedLandmark.value = null

  filteredLandmarks.value.forEach(lm => {
    const marker = L.marker([lm.lat, lm.lng], {
      icon: createLabelIcon(lm.name, false),
    }).addTo(map!)

    marker.on('click', () => onMarkerClick(lm, marker))

    markers.push(marker)
  })
}

function handleSearchInput() {
  if (debounceTimer) clearTimeout(debounceTimer)

  const q = searchQuery.value.trim()
  if (!q) {
    suggestions.value = []
    showSuggestions.value = false
    return
  }

  debounceTimer = setTimeout(() => {
    suggestions.value = filteredLandmarks.value.filter(
      lm => lm.name.includes(q) || lm.category.includes(q)
    )
    showSuggestions.value = suggestions.value.length > 0
  }, 200)
}

function selectSuggestion(lm: LandmarkMarker) {
  searchQuery.value = lm.name
  showSuggestions.value = false

  if (map) {
    map.flyTo([lm.lat, lm.lng], 17)
    map.once('moveend', () => {
      const target = markers.find(m => {
        const pos = m.getLatLng()
        return pos.lat === lm.lat && pos.lng === lm.lng
      })
      if (target) {
        onMarkerClick(lm, target)
      }
    })
  }
}

function handleCategoryChange(cat: string) {
  activeCategory.value = cat
}

function onFocus() {
  isSearchFocused.value = true
}

function onBlur() {
  setTimeout(() => {
    isSearchFocused.value = false
    showSuggestions.value = false
  }, 200)
}

async function loadLandmarks() {
  loading.value = true
  try {
    const res = await axios.get(`${API_BASE_URL}/landmarks`, {
      params: { pageSize: 100 },
    })
    const records = res.data?.data?.records || []
    landmarks.value = records.map((item: any) => ({
      id: item.id,
      name: item.name,
      lat: item.latitude,
      lng: item.longitude,
      category: item.category,
      rating: item.rating,
      openTime: item.openTime,
    }))
  } catch (e) {
    console.error('加载地标失败:', e)
  } finally {
    loading.value = false
  }
}

async function loadHotRankings() {
  try {
    const res = await axios.get(`${API_BASE_URL}/landmarks`, {
      params: { sortBy: 'hot', pageSize: 10 },
    })
    const records = res.data?.data?.records || []
    hotRankings.value = records.map((item: any) => ({
      id: item.id,
      name: item.name,
      lat: item.latitude,
      lng: item.longitude,
      category: item.category,
      rating: item.rating,
      openTime: item.openTime,
      checkins: item.checkins || 0,
    }))
  } catch (e) {
    console.error('加载热度榜失败:', e)
  }
}

function calcCenter(): [number, number] {
  if (landmarks.value.length === 0) return DEFAULT_CENTER
  const sumLat = landmarks.value.reduce((s, l) => s + l.lat, 0)
  const sumLng = landmarks.value.reduce((s, l) => s + l.lng, 0)
  return [sumLat / landmarks.value.length, sumLng / landmarks.value.length]
}

function showUserLocation() {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        if (map) {
          const gcj = wgs84ToGcj02(pos.coords.latitude, pos.coords.longitude)
          L.circleMarker([gcj.lat, gcj.lng], {
            radius: 8,
            color: '#3388ff',
            fillColor: '#3388ff',
            fillOpacity: 0.5,
          }).addTo(map)
        }
      },
      () => {},
      { timeout: 5000 }
    )
  }
}

onMounted(async () => {
  await Promise.all([loadLandmarks(), loadHotRankings()])

  const center = calcCenter()
  initMap(center[0], center[1])
  isFirstActivation.value = false

  showUserLocation()
})

onUnmounted(() => {
  nav.stopNavigation()
  clearNavLayers()
  if (map) {
    map.remove()
    map = null
  }
})

watch(filteredLandmarks, () => {
  refreshMarkers()
})

watch(() => nav.isNavigating.value, (navigating) => {
  if (!map) return
  clearNavLayers()
  const coords = nav.routeCoords.value
  if (navigating && coords.length > 0) {
    routePolyline = L.polyline(coords, {
      color: '#3388ff',
      weight: 5,
      opacity: 0.8,
    }).addTo(map)

    if (coords.length > 0) {
      const end = coords[coords.length - 1]
      endMarker = L.marker(end, {
        icon: L.divIcon({
          html: `<div style="background:#e74c3c;color:#fff;padding:4px 8px;border-radius:6px;font-size:12px;font-weight:600;white-space:nowrap;">${nav.destination.value?.name || ''}</div>`,
          className: '',
          iconSize: [80, 28],
          iconAnchor: [40, 28],
        }),
      }).addTo(map)
    }
    map.fitBounds(routePolyline.getBounds(), { padding: [50, 50] })
  }
})

watch(() => nav.currentStepIndex.value, (idx, oldIdx) => {
  const steps = nav.steps.value
  if (idx !== oldIdx && idx >= 0 && idx < steps.length) {
    speak(steps[idx].instruction)
  }
})

watch(() => nav.hasArrived.value, (arrived) => {
  if (arrived) speak('您已到达目的地附近')
})

watch(() => nav.userPosition.value, (pos, prevPos) => {
  if (!map || !pos || !nav.isNavigating.value) return
  if (!userMarker) {
    userMarker = L.marker([pos.lat, pos.lng], {
      icon: createUserMarkerIcon(pos.heading),
      zIndexOffset: 1000,
    }).addTo(map)
    return
  }

  const latLngChanged = !prevPos || prevPos.lat !== pos.lat || prevPos.lng !== pos.lng

  if (latLngChanged) {
    userMarker.setLatLng([pos.lat, pos.lng])

    const d = map.getCenter().distanceTo(L.latLng(pos.lat, pos.lng))
    if (d > 50) {
      map.panTo([pos.lat, pos.lng])
    }
  }

  if (!prevPos || prevPos.heading !== pos.heading) {
    const el = userMarker.getElement()
    if (el) {
      const arrow = el.querySelector<HTMLElement>('.user-arrow')
      if (arrow) {
        arrow.style.transform = `rotate(${pos.heading ?? 0}deg)`
      }
    }
  }
})

onActivated(() => {
  if (isFirstActivation.value) return
  const target = props.focusLandmark
  if (!target || !map) return

  handleCategoryChange(target.category)

  const lm: LandmarkMarker = {
    id: target.id,
    name: target.name,
    lat: target.lat,
    lng: target.lng,
    category: target.category,
    rating: target.rating,
    openTime: target.openTime,
  }

  nextTick(() => {
    map!.flyTo([lm.lat, lm.lng], 17)
    map!.once('moveend', () => {
      const m = markers.find(mk => {
        const p = mk.getLatLng()
        return p.lat === lm.lat && p.lng === lm.lng
      })
      if (m) {
        onMarkerClick(lm, m)
      }
    })
  })
})
</script>

<template>
  <div class="map-page">
    <div ref="mapContainer" class="map-container"></div>

    <div v-if="!navIsNavigating" class="search-bar">
      <div class="search-input-wrapper">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          v-model="searchQuery"
          class="search-input"
          type="text"
          placeholder="搜索地标..."
          @input="handleSearchInput"
          @focus="onFocus"
          @blur="onBlur"
        />
      </div>
      <ul v-if="showRanking" class="ranking-list">
        <li
          v-for="(lm, index) in hotRankings"
          :key="lm.id"
          class="ranking-item"
          @mousedown.prevent="selectSuggestion(lm)"
        >
          <span class="rank-badge" :class="'rank-' + (index + 1)">
            <template v-if="index === 0">&#129351;</template>
            <template v-else-if="index === 1">&#129352;</template>
            <template v-else-if="index === 2">&#129353;</template>
            <template v-else>{{ index + 1 }}</template>
          </span>
          <div class="ranking-info">
            <span class="ranking-name">{{ lm.name }}</span>
            <span class="ranking-category">{{ lm.category }}</span>
          </div>
          <span class="ranking-count">{{ (lm.checkins || 0).toLocaleString() }} 次打卡</span>
        </li>
      </ul>
      <ul v-if="showSuggestions && suggestions.length > 0" class="suggestions-list">
        <li
          v-for="lm in suggestions"
          :key="lm.id"
          class="suggestion-item"
          @mousedown.prevent="selectSuggestion(lm)"
        >
          <span class="suggestion-name">{{ lm.name }}</span>
          <span class="suggestion-category">{{ lm.category }}</span>
        </li>
      </ul>
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
      <div class="landmark-count-bar">
        <span>共 <strong>{{ filteredLandmarks.length }}</strong> 个地标</span>
      </div>
    </div>

    <div v-if="selectedLandmark && !navIsNavigating" class="landmark-popup">
      <div class="pop-header">
        <div class="pop-icon"></div>
        <div class="pop-title-area">
          <div class="pop-name">{{ selectedLandmark.name }}</div>
          <div class="pop-tag-time">{{ selectedLandmark.category }} · {{ selectedLandmark.openTime }}</div>
          <div class="star-box">
            <template v-for="i in 5" :key="i">
              <div
                class="star"
                :class="{
                  'star-empty': i > starData.full && !(i === starData.full + 1 && starData.hasHalf),
                }"
              ></div>
            </template>
            <span class="score-text">{{ selectedLandmark.rating }}</span>
          </div>
        </div>
        <div class="close-btn" @click="closeCard">&times;</div>
      </div>
      <div class="pop-btn-wrap">
        <button class="btn-nav" @click="startNavigation">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="3 11 22 2 13 21 11 13 3 11" />
          </svg>
          导航前往
        </button>
        <button class="btn-detail" @click="emit('open-detail', selectedLandmark.id)">
          查看详情
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 导航面板 -->
    <div v-show="navIsRouting || navIsNavigating || navHasError || navHasArrived" class="nav-panel">
      <div class="nav-top">
        <button class="nav-exit" @click="exitNavigation">&#8592; 退出</button>
        <span class="nav-dest">{{ navDest?.name || '' }}</span>
        <span class="nav-eta">{{ fmtTime(navRemTime) }}</span>
      </div>
      <div class="nav-body">
        <div class="nav-distance">{{ fmtDist(navRemDist) }}</div>

        <div v-if="navIsRouting" class="nav-loading">路线计算中...</div>

        <div v-else-if="navHasArrived" class="nav-arrived">已到达目的地</div>

        <div v-else-if="navHasError" class="nav-error">
          {{ navHasError }}
          <button class="nav-replan-btn" @click="exitNavigation">关闭</button>
        </div>

        <template v-else>
          <div class="nav-instruction">{{ navCurInstr }}</div>
          <div v-if="navNextInstr" class="nav-next">
            下一步：{{ navNextInstr }}
          </div>
          <div v-if="navRouteDeviation" class="nav-deviation">
            已偏离路线
            <button class="nav-replan-btn" @click="nav.replan()">重新规划</button>
          </div>
        </template>
      </div>
      <div class="nav-bottom">
        <button class="nav-btn" @click="onRecenter">&#x1F4CD; 我的位置</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.map-page {
  width: 100%;
  height: 100vh;
  position: relative;
  z-index: 0;
  overflow: hidden;
}

.map-container {
  width: 100%;
  height: 100%;
}

.search-bar {
  position: absolute;
  top: 24px;
  left: 16px;
  right: 16px;
  z-index: 1000;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-bg-input);
  border-radius: var(--radius-md);
  padding: 0 16px;
  height: 44px;
  box-shadow: var(--shadow-md);
}

.search-icon {
  width: 18px;
  height: 18px;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  color: var(--color-text-heading);
  background: transparent;
}

.search-input::placeholder {
  color: var(--color-text-muted);
}

.suggestions-list {
  margin: 8px 0 0;
  padding: 8px 0;
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  list-style: none;
  overflow: hidden;
}

.suggestion-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.suggestion-item:hover,
.suggestion-item:active {
  background: var(--color-bg);
}

.suggestion-name {
  font-size: 15px;
  color: var(--color-text-heading);
  font-weight: 500;
}

.suggestion-category {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.ranking-list {
  margin: 8px 0 0;
  padding: 8px 0;
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  list-style: none;
  overflow: hidden;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.ranking-item:hover,
.ranking-item:active {
  background: var(--color-bg);
}

.rank-badge {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.rank-badge.rank-1,
.rank-badge.rank-2,
.rank-badge.rank-3 {
  font-size: 20px;
  width: 28px;
  height: 28px;
}

.ranking-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ranking-name {
  font-size: 15px;
  color: var(--color-text-heading);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-category {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.ranking-count {
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.category-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  margin-top: 8px;
  padding-bottom: 4px;
  scrollbar-width: none;
}

.category-tabs::-webkit-scrollbar {
  display: none;
}

.category-tab {
  padding: 6px 14px;
  border-radius: 16px;
  border: none;
  background: rgba(242, 240, 235, 0.95);
  color: var(--color-text);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
}

.category-tab.active {
  background: var(--color-primary);
  color: #fff;
  box-shadow: 0 2px 8px var(--color-primary-shadow);
}

.category-tab:hover:not(.active) {
  background: var(--color-primary-light);
}

.landmark-count-bar {
  margin-top: 10px;
  font-size: 14px;
  color: var(--color-text);
}

.landmark-count-bar strong {
  color: var(--color-primary);
}

.landmark-popup {
  width: 94%;
  margin: 0 auto;
  background: var(--color-bg-card);
  border-radius: 20px;
  padding: 24px 20px;
  position: fixed;
  bottom: 84px;
  left: 0;
  right: 0;
  z-index: 2000;
  box-shadow: var(--shadow-lg);
  animation: popupSlideIn 0.25s ease-out;
}

@keyframes popupSlideIn {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.pop-header {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  position: relative;
}

.pop-icon {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-xl);
  background: #5a8f7b;
  flex-shrink: 0;
  background-image: radial-gradient(circle at 80% 20%, #7ab8a0 32%, transparent 33%),
    radial-gradient(circle at 20% 80%, #4a7d6a 32%, transparent 33%);
}

.pop-title-area {
  flex: 1;
  min-width: 0;
}

.pop-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-heading);
  margin: 0 0 6px;
}

.pop-tag-time {
  font-size: 13px;
  color: var(--color-text);
  margin-bottom: 8px;
}

.star-box {
  display: flex;
  align-items: center;
  gap: 4px;
}

.star {
  width: 18px;
  height: 18px;
  background: var(--color-star);
  clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%);
}

.star-empty {
  background: #e5e5e5;
}

.score-text {
  font-size: 16px;
  color: var(--color-star);
  font-weight: 600;
  margin-left: 6px;
}

.close-btn {
  position: absolute;
  top: -4px;
  right: -4px;
  font-size: 28px;
  color: var(--color-text-secondary);
  cursor: pointer;
  user-select: none;
  line-height: 1;
}

.pop-btn-wrap {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.btn-nav {
  flex: 1;
  padding: 12px 0;
  border: 2px solid var(--color-primary);
  border-radius: var(--radius-full);
  background: var(--color-bg-card);
  color: var(--color-primary);
  font-size: 15px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-nav:active {
  background: var(--color-primary-light);
}

.btn-detail {
  flex: 1.3;
  padding: 12px 0;
  border: none;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-detail:active {
  background: var(--color-primary-hover);
}

.nav-panel {
  position: fixed;
  bottom: 84px;
  left: 12px;
  right: 12px;
  background: var(--color-bg-card);
  border-radius: 16px;
  box-shadow: 0 -2px 16px rgba(0, 0, 0, 0.12);
  z-index: 3000;
  overflow: hidden;
}
.nav-top {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--color-primary);
  color: #fff;
}
.nav-exit {
  background: none;
  border: none;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.nav-dest {
  flex: 1;
  font-weight: 600;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nav-eta {
  font-size: 13px;
  opacity: 0.9;
}
.nav-body {
  padding: 16px;
}
.nav-distance {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-heading);
  margin-bottom: 8px;
}
.nav-loading,
.nav-arrived {
  font-size: 15px;
  color: var(--color-text-secondary);
}
.nav-arrived {
  color: var(--color-primary);
  font-weight: 600;
}
.nav-instruction {
  font-size: 16px;
  color: var(--color-text-heading);
  font-weight: 500;
  line-height: 1.4;
}
.nav-next {
  margin-top: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.nav-deviation {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #e74c3c;
  font-size: 14px;
}
.nav-error {
  font-size: 14px;
  color: #e74c3c;
  display: flex;
  align-items: center;
  gap: 10px;
}
.nav-replan-btn {
  background: var(--color-primary);
  color: #fff;
  border: none;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
}
.nav-bottom {
  display: flex;
  gap: 8px;
  padding: 0 16px 14px;
}
.nav-btn {
  flex: 1;
  padding: 10px 0;
  border: 1.5px solid var(--color-primary);
  border-radius: 24px;
  background: var(--color-bg-card);
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
</style>

<style>
.label-icon-container {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: transparent;
  border: none;
}

.marker-label {
  display: inline-block;
  background: var(--color-bg-card);
  color: var(--color-text-heading);
  font-weight: 600;
  font-size: 14px;
  padding: 4px 10px;
  border-radius: 6px;
  white-space: nowrap;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
  transition: all 0.2s;
  cursor: pointer;
}

.marker-label.active {
  background: var(--color-primary-light);
  font-size: 16px;
  padding: 6px 14px;
  box-shadow: 0 2px 8px var(--color-primary-shadow);
}

.user-marker-icon {
  background: transparent;
  border: none;
}

.user-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease;
}

.leaflet-bottom {
  bottom: 72px !important;
}

.leaflet-bottom.leaflet-left {
  left: auto;
  right: 0;
}
</style>
