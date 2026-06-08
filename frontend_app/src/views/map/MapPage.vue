<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, onActivated, watch, nextTick } from 'vue'
import axios from 'axios'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

interface LandmarkMarker {
  id: string
  name: string
  lat: number
  lng: number
  category: string
  rating: number
  openTime: string
}

const API_BASE_URL = 'http://localhost:8080/api/v1'

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

const mapContainer = ref<HTMLDivElement>()

function createLabelIcon(name: string, active: boolean): L.DivIcon {
  return L.divIcon({
    html: `<span class="marker-label${active ? ' active' : ''}">${name}</span>`,
    className: 'label-icon-container',
    iconSize: [150, 30] as any,
    iconAnchor: [75, 30] as any,
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
  window.open(`https://uri.amap.com/navigation?to=${lng},${lat},${name}`)
}

function initMap(lat: number, lng: number) {
  if (map) return

  map = L.map(mapContainer.value!, {
    zoomControl: false,
  }).setView([lat, lng], DEFAULT_ZOOM)

  L.control.zoom({ position: 'topright' }).addTo(map)

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

function onBlur() {
  setTimeout(() => {
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

function calcCenter(): [number, number] {
  if (landmarks.value.length === 0) return DEFAULT_CENTER
  const sumLat = landmarks.value.reduce((s, l) => s + l.lat, 0)
  const sumLng = landmarks.value.reduce((s, l) => s + l.lng, 0)
  return [sumLat / landmarks.value.length, sumLng / landmarks.value.length]
}

onMounted(async () => {
  await loadLandmarks()

  const center = calcCenter()
  initMap(center[0], center[1])
  isFirstActivation.value = false

  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        if (map) {
          L.circleMarker([pos.coords.latitude, pos.coords.longitude], {
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
})

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
  }
})

watch(filteredLandmarks, () => {
  refreshMarkers()
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

    <div class="search-bar">
      <header class="map-header">
        <h1 class="map-title">地图导览</h1>
      </header>
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
          @blur="onBlur"
        />
      </div>
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

    <div v-if="selectedLandmark" class="landmark-popup">
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
  top: 16px;
  left: 16px;
  right: 16px;
  z-index: 1000;
}

.map-header {
  margin-bottom: 16px;
}

.map-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-heading);
  margin: 0;
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

.leaflet-bottom {
  bottom: 72px !important;
}

.leaflet-top.leaflet-right {
  top: 175px;
}

.leaflet-bottom.leaflet-left {
  left: auto;
  right: 0;
}
</style>
