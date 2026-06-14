# Web 内嵌实时导航实施计划（高德 Web 服务 API + Leaflet）

## 概述

用高德 Web 服务 API 的步行路线规划 + Leaflet 地图 + 浏览器定位，在网页内实现实时导航，不跳转任何 App。

## 原理

```
实时导航 = 路径规划 + GPS实时追踪 + 步骤推进 + 语音播报 + 偏航重算
```

## 需要申请的内容

1. 高德开放平台 → 应用管理 → 创建新应用 → 添加 Key
2. 选择 **"Web服务"** 类型（注意：不是"Web端(JS API)"，两者 key 不同）
3. 免费额度足够校园场景使用

## 文件清单

### 需要新建的文件

| 序号 | 文件路径 | 说明 |
|------|----------|------|
| 1 | `frontend_app/.env` | 存放 API Key |
| 2 | `frontend_app/src/composables/useSpeech.ts` | 语音播报封装 |
| 3 | `frontend_app/src/composables/useNavigation.ts` | 导航核心逻辑 |
| 4 | `frontend_app/src/components/NavigationPanel.vue` | 导航面板 UI |

### 需要修改的文件

| 序号 | 文件路径 | 说明 |
|------|----------|------|
| 5 | `frontend_app/src/views/map/MapPage.vue` | 集成导航模式 |
| 6 | `frontend_app/src/views/detail/LandmarkDetail.vue` | 补充导航点击事件 |

---

## 文件 1：`.env`

```
VITE_AMAP_KEY=你的高德Web服务APIKey
```

## 文件 2：`src/composables/useSpeech.ts`

```typescript
export function useSpeech() {
  function speak(text: string) {
    if (!('speechSynthesis' in window)) return
    window.speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'zh-CN'
    utterance.rate = 1.0
    speechSynthesis.speak(utterance)
  }
  return { speak }
}
```

## 文件 3：`src/composables/useNavigation.ts`

```typescript
import { ref, computed } from 'vue'

const AMAP_KEY = import.meta.env.VITE_AMAP_KEY || ''

interface Destination {
  lat: number
  lng: number
  name: string
}

interface Step {
  instruction: string
  distance: number
  duration: number
  polyline: string
}

// Module-level state (singleton pattern)
const isNavigating = ref(false)
const isRouting = ref(false)
const hasArrived = ref(false)
const destination = ref<Destination | null>(null)
const routeCoords = ref<[number, number][]>([])
const steps = ref<Step[]>([])
const currentStepIndex = ref(0)
const remainingDistance = ref(0)
const remainingTime = ref(0)
const userPosition = ref<{ lat: number; lng: number; heading?: number } | null>(null)
const routeDeviation = ref(false)

let watchId: number | null = null

export function useNavigation() {
  const currentInstruction = computed(() =>
    steps.value[currentStepIndex.value]?.instruction || ''
  )
  const nextInstruction = computed(() => {
    if (currentStepIndex.value + 1 < steps.value.length) {
      return steps.value[currentStepIndex.value + 1].instruction
    }
    return null
  })

  async function startNavigation(dest: Destination) {
    stopNavigation()
    destination.value = dest
    isRouting.value = true

    const pos = await getCurrentPosition()
    const origin = { lat: pos.coords.latitude, lng: pos.coords.longitude }
    userPosition.value = origin

    await fetchRoute(origin, dest)
    currentStepIndex.value = 0

    watchId = navigator.geolocation.watchPosition(
      onPositionUpdate,
      () => {},
      { enableHighAccuracy: true, timeout: 5000, maximumAge: 10000 }
    )

    isRouting.value = false
    isNavigating.value = true
  }

  function stopNavigation() {
    if (watchId !== null) {
      navigator.geolocation.clearWatch(watchId)
      watchId = null
    }
    isNavigating.value = false
    isRouting.value = false
    hasArrived.value = false
    destination.value = null
    routeCoords.value = []
    steps.value = []
    currentStepIndex.value = 0
    remainingDistance.value = 0
    remainingTime.value = 0
    userPosition.value = null
    routeDeviation.value = false
  }

  async function fetchRoute(
    origin: { lat: number; lng: number },
    dest: Destination
  ) {
    const originStr = `${origin.lng},${origin.lat}`
    const destStr = `${dest.lng},${dest.lat}`
    const url = `https://restapi.amap.com/v3/direction/walking?key=${AMAP_KEY}&origin=${originStr}&destination=${destStr}`

    const res = await fetch(url)
    const data = await res.json()

    if (data.status !== '1' || !data.route?.paths?.length) {
      throw new Error('路线规划失败')
    }

    const path = data.route.paths[0]
    remainingDistance.value = parseInt(path.distance)
    remainingTime.value = parseInt(path.duration)

    steps.value = path.steps.map((s: any) => ({
      instruction: s.instruction,
      distance: parseInt(s.distance),
      duration: parseInt(s.duration || 0),
      polyline: s.polyline,
    }))

    const allCoords: [number, number][] = []
    path.steps.forEach((step: any) => {
      const pts = step.polyline.split(';')
      const start = allCoords.length === 0 ? 0 : 1
      for (let i = start; i < pts.length; i++) {
        const [lng, lat] = pts[i].split(',').map(Number)
        allCoords.push([lat, lng])
      }
    })
    routeCoords.value = allCoords
  }

  function onPositionUpdate(pos: GeolocationPosition) {
    const lat = pos.coords.latitude
    const lng = pos.coords.longitude
    userPosition.value = { lat, lng, heading: pos.coords.heading || undefined }

    if (routeCoords.value.length === 0) return

    // Find nearest route vertex
    let minDist = Infinity
    let nearestIdx = 0
    for (let i = 0; i < routeCoords.value.length; i++) {
      const d = distanceBetween(lat, lng, routeCoords.value[i][0], routeCoords.value[i][1])
      if (d < minDist) {
        minDist = d
        nearestIdx = i
      }
    }

    routeDeviation.value = minDist > 50

    // Determine current step
    let total = 0
    for (let i = 0; i < steps.value.length; i++) {
      const count = steps.value[i].polyline.split(';').length
      total += i === 0 ? count : count - 1
      if (nearestIdx < total) {
        currentStepIndex.value = i
        break
      }
    }

    // Update remaining
    let remDist = 0
    let remTime = 0
    for (let i = currentStepIndex.value; i < steps.value.length; i++) {
      remDist += steps.value[i].distance
      remTime += steps.value[i].duration
    }
    remainingDistance.value = remDist
    remainingTime.value = remTime

    // Check arrival
    if (destination.value) {
      const d = distanceBetween(lat, lng, destination.value.lat, destination.value.lng)
      if (d < 20) hasArrived.value = true
    }
  }

  async function replan() {
    if (!destination.value || !userPosition.value) return
    isRouting.value = true
    try {
      await fetchRoute(userPosition.value, destination.value)
      currentStepIndex.value = 0
    } catch (e) {
      console.error('重新规划失败', e)
    }
    isRouting.value = false
    routeDeviation.value = false
  }

  return {
    isNavigating, isRouting, hasArrived,
    destination, routeCoords, steps,
    currentStepIndex, remainingDistance, remainingTime,
    userPosition, routeDeviation,
    currentInstruction, nextInstruction,
    startNavigation, stopNavigation, replan,
  }
}

function getCurrentPosition(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 10000,
    })
  })
}

function distanceBetween(
  lat1: number, lng1: number, lat2: number, lng2: number
): number {
  const R = 6371000
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLng = ((lng2 - lng1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}
```

## 文件 4：`src/components/NavigationPanel.vue`

```vue
<script setup lang="ts">
defineProps<{
  destinationName: string
  currentInstruction: string
  nextInstruction: string | null
  remainingDistance: number
  remainingTime: number
  isRouting: boolean
  hasArrived: boolean
  routeDeviation: boolean
}>()

const emit = defineEmits<{
  exit: []
  recenter: []
  replan: []
}>()

function fmtDist(m: number): string {
  if (m >= 1000) return `${(m / 1000).toFixed(1)}公里`
  return `${Math.round(m)}米`
}
function fmtTime(s: number): string {
  if (s < 60) return `${Math.round(s)}秒`
  const m = Math.round(s / 60)
  return `${m}分钟`
}
</script>

<template>
  <div class="nav-panel">
    <div class="nav-top">
      <button class="nav-exit" @click="emit('exit')">← 退出</button>
      <span class="nav-dest">{{ destinationName }}</span>
      <span class="nav-eta">{{ fmtTime(remainingTime) }}</span>
    </div>

    <div class="nav-body">
      <div class="nav-distance">{{ fmtDist(remainingDistance) }}</div>

      <div v-if="isRouting" class="nav-loading">路线计算中...</div>

      <div v-else-if="hasArrived" class="nav-arrived">已到达目的地</div>

      <template v-else>
        <div class="nav-instruction">{{ currentInstruction }}</div>
        <div v-if="nextInstruction" class="nav-next">
          下一步：{{ nextInstruction }}
        </div>

        <div v-if="routeDeviation" class="nav-deviation">
          已偏离路线
          <button class="nav-replan-btn" @click="emit('replan')">重新规划</button>
        </div>
      </template>
    </div>

    <div class="nav-bottom">
      <button class="nav-btn" @click="emit('recenter')">📍 我的位置</button>
    </div>
  </div>
</template>

<style scoped>
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
```

## 文件 5：修改 `src/views/map/MapPage.vue`

### 在 `<script>` 区域添加：

```typescript
// 在文件顶部的 import 区域添加：
import { watch } from 'vue'  // 如果已有则跳过
import { useNavigation } from '../../composables/useNavigation'
import { useSpeech } from '../../composables/useSpeech'

// 在 setup 函数内添加：
const nav = useNavigation()
const { speak } = useSpeech()

// 导航图层引用
let routePolyline: L.Polyline | null = null
let userMarker: L.CircleMarker | null = null
let startMarker: L.Marker | null = null
let endMarker: L.Marker | null = null

// 替换第 105-109 行的 startNavigation 函数：
function startNavigation() {
  if (!selectedLandmark.value) return
  const { lat, lng, name } = selectedLandmark.value

  // 清除地图上的地标弹出层
  closeCard()

  // 关闭选中的标记高亮
  if (activeMarkerData) {
    activeMarkerData.marker.setIcon(createLabelIcon(activeMarkerData.lm.name, false))
    activeMarkerData = null
  }

  // 启动导航
  nav.startNavigation({ lat, lng, name })
}

// 监听导航状态变化，绘制/清除路线
watch(() => nav.isNavigating.value, (navigating) => {
  if (!map) return

  // 清除旧的导航图层
  if (routePolyline) { map.removeLayer(routePolyline); routePolyline = null }
  if (userMarker) { map.removeLayer(userMarker); userMarker = null }
  if (startMarker) { map.removeLayer(startMarker); startMarker = null }
  if (endMarker) { map.removeLayer(endMarker); endMarker = null }

  if (navigating && nav.routeCoords.value.length > 0) {
    // 画路线
    routePolyline = L.polyline(nav.routeCoords.value, {
      color: '#3388ff',
      weight: 5,
      opacity: 0.8,
    }).addTo(map)

    // 起点标记
    if (nav.routeCoords.value.length > 0) {
      startMarker = L.circleMarker(nav.routeCoords.value[0], {
        radius: 6,
        color: '#27ae60',
        fillColor: '#27ae60',
        fillOpacity: 0.8,
      }).addTo(map)
    }

    // 终点标记
    if (nav.routeCoords.value.length > 0) {
      const end = nav.routeCoords.value[nav.routeCoords.value.length - 1]
      endMarker = L.marker(end, {
        icon: L.divIcon({
          html: '<div style="background:#e74c3c;color:#fff;padding:4px 8px;border-radius:6px;font-size:12px;font-weight:600;white-space:nowrap;">' + (nav.destination.value?.name || '') + '</div>',
          className: '',
          iconSize: [80, 28],
          iconAnchor: [40, 28],
        }),
      }).addTo(map)
    }

    // 缩放到路线范围
    map.fitBounds(routePolyline.getBounds(), { padding: [50, 50] })
  }
})

// 监听步骤变化，触发语音播报
watch(() => nav.currentStepIndex.value, (idx, oldIdx) => {
  if (idx !== oldIdx && idx >= 0 && idx < nav.steps.value.length) {
    speak(nav.steps.value[idx].instruction)
  }
})

// 监听到达
watch(() => nav.hasArrived.value, (arrived) => {
  if (arrived) {
    speak('您已到达目的地附近')
  }
})

// 监听用户位置更新，刷新定位标记
watch(() => nav.userPosition.value, (pos) => {
  if (!map || !pos || !nav.isNavigating.value) return
  if (!userMarker) {
    userMarker = L.circleMarker([pos.lat, pos.lng], {
      radius: 8,
      color: '#3388ff',
      fillColor: '#3388ff',
      fillOpacity: 0.6,
    }).addTo(map)
  } else {
    userMarker.setLatLng([pos.lat, pos.lng])
  }
})

// 在 onUnmounted 中添加：
onUnmounted(() => {
  nav.stopNavigation()
  // ... 原有的 map.remove() 代码
})

// 在 template 中，地图容器下方添加导航面板：
// 在 `</div> <!-- .map-page -->` 之前插入：
```

### 在 `<template>` 区域，`.map-page` div 末尾（第 418 行之前）添加：

```html
<NavigationPanel
  v-if="nav.isNavigating.value"
  :destination-name="nav.destination.value?.name || ''"
  :current-instruction="nav.currentInstruction.value"
  :next-instruction="nav.nextInstruction.value"
  :remaining-distance="nav.remainingDistance.value"
  :remaining-time="nav.remainingTime.value"
  :is-routing="nav.isRouting.value"
  :has-arrived="nav.hasArrived.value"
  :route-deviation="nav.routeDeviation.value"
  @exit="nav.stopNavigation()"
  @recenter="onRecenter"
  @replan="nav.replan()"
/>
```

### 添加 recenter 方法：

```typescript
function onRecenter() {
  if (!map || !nav.userPosition.value) return
  map.flyTo([nav.userPosition.value.lat, nav.userPosition.value.lng], 17)
}
```

### 在 `import` 区域添加 NavigationPanel 导入：

```typescript
import NavigationPanel from '../../components/NavigationPanel.vue'
```

### 修改第 255-270 行的定位代码：

原来只获取一次位置，导航模式下改为持续追踪（不需要改，因为 useNavigation 中已有 watchPosition）。

但非导航模式下的单次定位蓝色圆点保留。

### 导航模式下隐藏搜索栏和地标弹出层：

在 template 中，给搜索栏和地标弹出层添加 `v-if="!nav.isNavigating.value"`：

```html
<!-- 搜索栏区域 -->
<div v-if="!nav.isNavigating.value" class="search-bar">

<!-- 地标弹出层 -->
<div v-if="selectedLandmark && !nav.isNavigating.value" class="landmark-popup">
```

### 原有位置定位（第 255-270 行）需要提取为一个独立函数，避免与导航冲突：

```typescript
function showUserLocation() {
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
}
```

并在 `onMounted` 中 `initMap` 后调用 `showUserLocation()`。

## 文件 6：修改 `src/views/detail/LandmarkDetail.vue`

在 `LandmarkDetail.vue` 第 462-468 行的"导航前往"按钮添加 `@click` 事件：

```html
<button class="action-btn outline" @click="handleViewLargeMap">
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
    <polygon points="3 11 22 2 13 21 11 13 3 11" />
  </svg>
  导航前往
</button>
```

这样点击"导航前往"会跳转到地图页并聚焦目标，然后用户再点击地图上的"导航前往"按钮启动导航。

## 实施顺序

1. 在 `.env` 中填入高德 Web 服务 API Key
2. 创建 `useSpeech.ts`、`useNavigation.ts`、`NavigationPanel.vue`
3. 修改 `MapPage.vue`
4. 修改 `LandmarkDetail.vue`
5. 运行 `npm run dev` 测试

## 注意

- 高德 API Key 需要选择 **"Web服务"** 类型，不是 "Web端(JS API)"
- `distanceBetween` 使用 Haversine 公式，适合校园短距离
- 语音播报需要用户与页面发生一次交互后才能播放（浏览器 autoplay 策略）
- 首次定位可能需要几秒钟，手机上建议开启 GPS
- 路线偏航检测阈值设为 50 米，可根据实际调整
