<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import iconUrl from 'leaflet/dist/images/marker-icon.png'
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
import shadowUrl from 'leaflet/dist/images/marker-shadow.png'

const props = withDefaults(
  defineProps<{
    modelValue: { lat: number | null; lng: number | null }
    readonly?: boolean
    height?: string
  }>(),
  {
    readonly: false,
    height: '300px',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: { lat: number; lng: number }]
}>()

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

//华工附近
const DEFAULT_CENTER: [number, number] = [23.0510, 113.4016]
const DEFAULT_ZOOM = 15

const mapContainer = ref<HTMLDivElement>()
let map: L.Map | null = null
let marker: L.Marker | null = null

function getCenter(): [number, number] {
  const { lat, lng } = props.modelValue
  if (lat != null && lng != null) {
    return [lat, lng]
  }
  return DEFAULT_CENTER
}

function initMap() {
  if (map) return
  map = L.map(mapContainer.value!, {
    zoomControl: false,
    attributionControl: false,
  }).setView(getCenter(), DEFAULT_ZOOM)

  L.control.zoom({ position: 'topright' }).addTo(map)

  L.tileLayer(
    'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
    {
      subdomains: ['1', '2', '3', '4'],
      attribution: '&copy; <a href="https://www.amap.com/">高德地图</a>',
      maxZoom: 18,
    },
  ).addTo(map)

  if (!props.readonly) {
    map.on('click', (e: L.LeafletMouseEvent) => {
      placeMarker(e.latlng.lat, e.latlng.lng)
    })
  }

  updateMarker()
}

function updateMarker() {
  if (!map) return
  if (marker) {
    marker.remove()
    marker = null
  }
  const { lat, lng } = props.modelValue
  if (lat != null && lng != null) {
    marker = L.marker([lat, lng]).addTo(map)
  }
}

function placeMarker(lat: number, lng: number) {
  if (!map) return
  if (marker) {
    marker.setLatLng([lat, lng])
  } else {
    marker = L.marker([lat, lng]).addTo(map)
  }
  emit('update:modelValue', { lat: +lat.toFixed(7), lng: +lng.toFixed(7) })
}

watch(
  () => props.modelValue,
  () => updateMarker(),
)

onMounted(() => {
  initMap()
})

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<template>
  <div class="map-picker">
    <div ref="mapContainer" class="map-container" :style="{ height }"></div>
    <div v-if="!readonly" class="map-hint">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="hint-icon">
        <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
      </svg>
      点击地图即可标注地标位置
    </div>
    <div v-if="modelValue.lat != null && modelValue.lng != null" class="coord-display">
      {{ modelValue.lat.toFixed(7) }}, {{ modelValue.lng.toFixed(7) }}
    </div>
  </div>
</template>

<style scoped>
.map-picker {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  position: relative;
}

.map-container {
  width: 100%;
}

.map-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 12px;
  color: #9ca3af;
  background: #fafafa;
  border-top: 1px solid #f3f4f6;
}

.hint-icon {
  width: 16px;
  height: 16px;
  color: #10b981;
  flex-shrink: 0;
}

.coord-display {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-family: monospace;
  pointer-events: none;
  white-space: nowrap;
}
</style>
