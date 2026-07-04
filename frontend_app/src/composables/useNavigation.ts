import { ref, computed } from 'vue'
import { wgs84ToGcj02 } from '../utils/coordConvert'

const AMAP_KEY = import.meta.env.VITE_AMAP_KEY || ''
const AMAP_JSCODE = import.meta.env.VITE_AMAP_JSCODE || ''

interface Destination {
  lat: number
  lng: number
  name: string
}

interface Step {
  instruction: string
  distance: number
  duration: number
  path: [number, number][]
}

const isNavigating = ref(false)
const isRouting = ref(false)
const hasArrived = ref(false)
const navError = ref('')
const destination = ref<Destination | null>(null)
const routeCoords = ref<[number, number][]>([])
const steps = ref<Step[]>([])
const currentStepIndex = ref(0)
const remainingDistance = ref(0)
const remainingTime = ref(0)
const userPosition = ref<{ lat: number; lng: number; heading?: number } | null>(null)
const routeDeviation = ref(false)

let watchId: number | null = null
let sdkLoaded = false
let sdkLoading = false
let sdkLoadResolve: (() => void) | null = null
let lastLatLng: { lat: number; lng: number } | null = null

// 罗盘状态
let compassRaw: number | null = null
let compassStaleTimer: ReturnType<typeof setTimeout> | null = null
let smoothHeading = 0
let hasSmoothHeading = false
const COMPASS_SMOOTH = 0.3
const COMPASS_STALE_MS = 5000

function loadAmapSDK(): Promise<void> {
  if (sdkLoaded) return Promise.resolve()
  if (sdkLoading) {
    return new Promise((resolve) => { sdkLoadResolve = resolve })
  }
  sdkLoading = true

  if (AMAP_JSCODE) {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: AMAP_JSCODE }
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=AMap.Walking`
    script.onload = () => {
      ;(window as any).AMap.plugin('AMap.Walking', () => {
        sdkLoaded = true
        sdkLoading = false
        resolve()
        if (sdkLoadResolve) {
          sdkLoadResolve()
          sdkLoadResolve = null
        }
      })
    }
    script.onerror = () => {
      sdkLoading = false
      reject(new Error('高德地图SDK加载失败，请检查网络和API Key'))
    }
    document.head.appendChild(script)
  })
}

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
    navError.value = ''

    const compassOk = await requestCompassPermission()
    if (!compassOk) {
      console.warn('罗盘权限未授权，将使用GPS方向')
    }

    try {
      const pos = await getCurrentPosition()
      const gcj = wgs84ToGcj02(pos.coords.latitude, pos.coords.longitude)
      const origin = { lat: gcj.lat, lng: gcj.lng }
      userPosition.value = origin
      console.log('当前位置:', origin.lat, origin.lng, '目的地:', dest.lat, dest.lng)

      await loadAmapSDK()

      await searchRoute(origin, dest)
      currentStepIndex.value = 0

      watchId = navigator.geolocation.watchPosition(
        onPositionUpdate,
        onPositionError,
        { enableHighAccuracy: true, timeout: 3000, maximumAge: 0 }
      )

      if (compassOk) startCompass()

      isRouting.value = false
      isNavigating.value = true
    } catch (e: any) {
      isRouting.value = false
      isNavigating.value = false
      navError.value = e.message || '导航启动失败'
      console.error('导航启动失败:', e)
    }
  }

  function stopNavigation() {
    if (watchId !== null) {
      navigator.geolocation.clearWatch(watchId)
      watchId = null
    }
    stopCompass()
    gpsErrorCount = 0
    isNavigating.value = false
    isRouting.value = false
    hasArrived.value = false
    navError.value = ''
    destination.value = null
    routeCoords.value = []
    steps.value = []
    currentStepIndex.value = 0
    remainingDistance.value = 0
    remainingTime.value = 0
    userPosition.value = null
    routeDeviation.value = false
  }

  let gpsErrorCount = 0

  function onPositionError(err: GeolocationPositionError) {
    gpsErrorCount++
    console.warn('GPS error #' + gpsErrorCount + ':', err.code, err.message)
    if (gpsErrorCount > 5) {
      console.warn('GPS too many errors, lowering accuracy')
      if (watchId !== null) navigator.geolocation.clearWatch(watchId)
      watchId = navigator.geolocation.watchPosition(
        onPositionUpdate,
        onPositionError,
        { enableHighAccuracy: false, timeout: 5000, maximumAge: 3000 }
      )
    }
  }

  function onCompassUpdate(event: DeviceOrientationEvent) {
    let raw = event.alpha
    if (typeof (event as any).webkitCompassHeading === 'number') {
      raw = (event as any).webkitCompassHeading
    }
    if (raw === null || raw === undefined) return

    if (!hasSmoothHeading) {
      smoothHeading = raw
      hasSmoothHeading = true
    } else {
      let diff = raw - smoothHeading
      if (diff > 180) diff -= 360
      if (diff < -180) diff += 360
      smoothHeading += COMPASS_SMOOTH * diff
      smoothHeading = ((smoothHeading % 360) + 360) % 360
    }

    compassRaw = smoothHeading

    if (compassStaleTimer) clearTimeout(compassStaleTimer)
    compassStaleTimer = setTimeout(() => {
      compassRaw = null
      hasSmoothHeading = false
    }, COMPASS_STALE_MS)

    if (userPosition.value) {
      userPosition.value = { ...userPosition.value, heading: smoothHeading }
    }
  }

  function startCompass() {
    window.addEventListener('deviceorientationabsolute', onCompassUpdate)
    window.addEventListener('deviceorientation', onCompassUpdate)
  }

  function stopCompass() {
    window.removeEventListener('deviceorientationabsolute', onCompassUpdate)
    window.removeEventListener('deviceorientation', onCompassUpdate)
    if (compassStaleTimer) { clearTimeout(compassStaleTimer); compassStaleTimer = null }
    compassRaw = null
    hasSmoothHeading = false
  }

  async function requestCompassPermission(): Promise<boolean> {
    const api = (DeviceOrientationEvent as any)
    if (typeof api.requestPermission === 'function') {
      try {
        const result = await api.requestPermission()
        return result === 'granted'
      } catch { return false }
    }
    return true
  }

  function repairStepPath(path: any[]): [number, number][] {
    return path.map((p: any) => {
      if (Array.isArray(p) && p.length >= 2) {
        return [p[0] as number, p[1] as number]
      }
      if (typeof p === 'object' && p.lng && p.lat) {
        return [p.lng as number, p.lat as number]
      }
      return [0, 0]
    })
  }

  function searchRoute(
    origin: { lat: number; lng: number },
    dest: Destination
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      const amap = (window as any).AMap
      if (!amap) {
        reject(new Error('高德地图SDK未加载'))
        return
      }

      const walking = new amap.Walking({ map: null, policy: amap.WalkingPolicy?.LEAST_DISTANCE })
      walking.search(
        [origin.lng, origin.lat],
        [dest.lng, dest.lat],
        (status: string, result: any) => {
          console.log('AMap.Walking result:', status, JSON.stringify(result))
          if (status !== 'complete' || !result.routes || result.routes.length === 0) {
            const info = result?.info || result?.message || '未知错误'
            reject(new Error('路线规划失败：' + info))
            return
          }

          const route = result.routes[0]
          remainingDistance.value = route.distance || 0
          remainingTime.value = route.time || 0

          steps.value = (route.steps || []).map((s: any) => ({
            instruction: s.instruction || '',
            distance: s.distance || 0,
            duration: s.time || 0,
            path: repairStepPath(s.path || []),
          }))

          const allCoords: [number, number][] = []
          route.steps.forEach((step: any) => {
            const pts = repairStepPath(step.path || [])
            const start = allCoords.length === 0 ? 0 : 1
            for (let i = start; i < pts.length; i++) {
              const [lng, lat] = pts[i]
              allCoords.push([lat, lng])
            }
          })
          routeCoords.value = allCoords

          resolve()
        }
      )
    })
  }

  function onPositionUpdate(pos: GeolocationPosition) {
    const gcj = wgs84ToGcj02(pos.coords.latitude, pos.coords.longitude)
    const lat = gcj.lat
    const lng = gcj.lng

    let heading: number | undefined

    if (compassRaw !== null && !isNaN(compassRaw)) {
      heading = compassRaw
    } else if (pos.coords.heading !== null && !isNaN(pos.coords.heading)) {
      heading = pos.coords.heading
    } else if (lastLatLng) {
      heading = computeBearing(lastLatLng.lat, lastLatLng.lng, lat, lng)
    }

    lastLatLng = { lat, lng }
    userPosition.value = { lat, lng, heading }

    if (routeCoords.value.length === 0) return

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

    let total = 0
    for (let i = 0; i < steps.value.length; i++) {
      const count = steps.value[i].path.length
      total += i === 0 ? count : count - 1
      if (nearestIdx < total) {
        currentStepIndex.value = i
        break
      }
    }

    let remDist = 0
    remDist += distanceBetween(lat, lng, routeCoords.value[nearestIdx][0], routeCoords.value[nearestIdx][1])
    for (let i = nearestIdx; i < routeCoords.value.length - 1; i++) {
      remDist += distanceBetween(
        routeCoords.value[i][0], routeCoords.value[i][1],
        routeCoords.value[i + 1][0], routeCoords.value[i + 1][1]
      )
    }
    remainingDistance.value = remDist
    remainingTime.value = Math.round(remDist / 1.4)

    if (destination.value) {
      const d = distanceBetween(lat, lng, destination.value.lat, destination.value.lng)
      if (d < 20) hasArrived.value = true
    }
  }

  async function replan() {
    if (!destination.value || !userPosition.value) return
    isRouting.value = true
    try {
      await searchRoute(userPosition.value, destination.value)
      currentStepIndex.value = 0
    } catch (e: any) {
      navError.value = e.message || '重新规划失败'
    }
    isRouting.value = false
    routeDeviation.value = false
  }

  return {
    isNavigating, isRouting, hasArrived, navError,
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

function computeBearing(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const dLng = ((lng2 - lng1) * Math.PI) / 180
  const y = Math.sin(dLng) * Math.cos((lat2 * Math.PI) / 180)
  const x = Math.cos((lat1 * Math.PI) / 180) * Math.sin((lat2 * Math.PI) / 180) -
    Math.sin((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.cos(dLng)
  let bearing = (Math.atan2(y, x) * 180) / Math.PI
  return (bearing + 360) % 360
}
