<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import iconUrl from 'leaflet/dist/images/marker-icon.png'
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
import shadowUrl from 'leaflet/dist/images/marker-shadow.png'

const DefaultIcon = L.icon({
  iconUrl,
  iconRetinaUrl,
  shadowUrl,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
})
L.Marker.prototype.options.icon = DefaultIcon

interface LandmarkMarker {
  id: string
  name: string
  lat: number
  lng: number
  category: string
}

const emit = defineEmits<{
  'open-detail': [landmarkId: string]
}>()

const mockLandmarks: LandmarkMarker[] = [
  { id: '1', name: '图书馆', lat: 39.9928, lng: 116.3280, category: '学习' },
  { id: '2', name: '教学楼A座', lat: 39.9940, lng: 116.3295, category: '教学' },
  { id: '3', name: '学生食堂', lat: 39.9915, lng: 116.3270, category: '餐饮' },
  { id: '4', name: '体育馆', lat: 39.9935, lng: 116.3310, category: '运动' },
  { id: '5', name: '行政楼', lat: 39.9920, lng: 116.3255, category: '行政' },
]

const DEFAULT_CENTER: [number, number] = [39.9928, 116.3280]
const DEFAULT_ZOOM = 15

let map: L.Map | null = null
const markers: L.Marker[] = []

const searchQuery = ref('')
const suggestions = ref<LandmarkMarker[]>([])
const showSuggestions = ref(false)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const mapContainer = ref<HTMLDivElement>()

function createPopupContent(lm: LandmarkMarker): string {
  return `
    <div class="marker-popup">
      <p class="popup-name">${lm.name}</p>
      <p class="popup-category">${lm.category}</p>
      <button class="popup-detail-btn" data-landmark-id="${lm.id}">查看详情</button>
    </div>
  `
}

function initMap(lat: number, lng: number) {
  if (map) return

  map = L.map(mapContainer.value!, {
    zoomControl: false,
  }).setView([lat, lng], DEFAULT_ZOOM)

  L.control.zoom({ position: 'topleft' }).addTo(map)

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

  mockLandmarks.forEach(lm => {
    const marker = L.marker([lm.lat, lm.lng])
      .addTo(map!)
      .bindPopup(createPopupContent(lm), { offset: [0, -20] })

    marker.on('popupopen', () => {
      setTimeout(() => {
        const btn = document.querySelector(`[data-landmark-id="${lm.id}"]`)
        btn?.addEventListener('click', () => {
          emit('open-detail', lm.id)
        })
      }, 0)
    })

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
    suggestions.value = mockLandmarks.filter(
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
      target?.openPopup()
    })
  }
}

function onBlur() {
  setTimeout(() => {
    showSuggestions.value = false
  }, 200)
}

onMounted(() => {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        initMap(pos.coords.latitude, pos.coords.longitude)
      },
      () => {
        initMap(DEFAULT_CENTER[0], DEFAULT_CENTER[1])
      },
      { timeout: 5000, enableHighAccuracy: false }
    )
  } else {
    initMap(DEFAULT_CENTER[0], DEFAULT_CENTER[1])
  }
})

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<template>
  <div class="map-page">
    <div ref="mapContainer" class="map-container"></div>
    <div class="search-bar">
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
  max-width: 400px;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border-radius: 12px;
  padding: 0 16px;
  height: 44px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
}

.search-icon {
  width: 18px;
  height: 18px;
  color: #9ca3af;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  color: #333;
  background: transparent;
}

.search-input::placeholder {
  color: #bbb;
}

.suggestions-list {
  margin: 8px 0 0;
  padding: 8px 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
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
  background: #f0f7f4;
}

.suggestion-name {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.suggestion-category {
  font-size: 12px;
  color: #9ca3af;
}
</style>

<style>
.marker-popup {
  min-width: 100px;
  text-align: center;
}

.popup-name {
  margin: 0 0 2px;
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.popup-category {
  margin: 0 0 8px;
  font-size: 12px;
  color: #999;
}

.popup-detail-btn {
  background: #2d8a6e;
  color: #fff;
  border: none;
  padding: 4px 14px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.popup-detail-btn:hover {
  background: #227355;
}

.leaflet-top.leaflet-left {
  top: 68px;
}

.leaflet-bottom {
  bottom: 72px !important;
}

.leaflet-bottom.leaflet-left {
  left: auto;
  right: 0;
}
</style>
