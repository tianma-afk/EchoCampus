<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { useAuth } from './composables/useAuth'
import SearchPage from './views/search/SearchPage.vue'
import MapPage from './views/map/MapPage.vue'
import LandmarkRepo from './views/repo/LandmarkRepo.vue'
import LandmarkDetail from './views/detail/LandmarkDetail.vue'
import ProfilePage from './views/profile/ProfilePage.vue'
import LoginPage from './views/auth/LoginPage.vue'
import BottomNav from './components/BottomNav.vue'

const API_BASE_URL = 'http://localhost:8080/api/v1'

const { token, nickname, email, isLoggedIn, loading, tryRestoreSession, logout } = useAuth()

onMounted(() => {
  tryRestoreSession()
})

// 所有请求自动带上 token
axios.interceptors.request.use((config) => {
  if (token.value) {
    config.headers.Authorization = `Bearer ${token.value}`
  }
  return config
})

interface LandmarkData {
  id: string
  name: string
  rating: number
  checkins: number
  openTime: string
  category: string
  tags: string[]
  imgs: string[]
  campusName: string
  universityName: string
  color: string
  buildYear: string
  openTimeDetail: string
  floors: string
  location: string
  description: string
  totalFloors: number
  floorList: FloorInfo[]
  recommendRate: number
}

interface FloorInfo {
  floorNumber: number
  floorName: string
  tags: string[]
}

const currentTab = ref('scan')
const showDetail = ref(false)
const selectedLandmark = ref<LandmarkData | null>(null)

interface FocusMarker {
  id: string
  name: string
  lat: number
  lng: number
  category: string
  rating: number
  openTime: string
}

const mapFocusTarget = ref<FocusMarker | null>(null)

function handleLoginSuccess() {
  // 登录成功后自动切换回主界面
}

const handleSelectLandmark = (landmark: LandmarkData) => {
  selectedLandmark.value = landmark
  showDetail.value = true
}

const handleOpenLandmarkDetail = async (landmarkId: string) => {
  try {
    const res = await axios.get(`${API_BASE_URL}/landmarks/${landmarkId}`)
    selectedLandmark.value = res.data.data
    showDetail.value = true
  } catch (err) {
    console.error('获取地标详情失败:', err)
  }
}

const handleNavigateToMap = (data: FocusMarker) => {
  mapFocusTarget.value = data
  showDetail.value = false
  currentTab.value = 'map'
}

const handleBack = () => {
  showDetail.value = false
  selectedLandmark.value = null
}

const handleLogout = () => {
  logout()
  currentTab.value = 'scan'
  showDetail.value = false
}
</script>

<template>
  <!-- 启动中 -->
  <div v-if="loading" class="splash">
    <div class="splash-icon">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
        <path d="M6 12v5c0 1.1 2.7 2 6 2s6-.9 6-2v-5" />
      </svg>
    </div>
  </div>

  <!-- 未登录 -->
  <LoginPage v-else-if="!isLoggedIn" @login-success="handleLoginSuccess" />

  <!-- 已登录 -->
  <template v-else>
    <LandmarkDetail
      v-if="showDetail && selectedLandmark"
      :landmark="selectedLandmark"
      @back="handleBack"
      @navigate-map="handleNavigateToMap"
    />
    <div v-show="!showDetail">
      <KeepAlive>
        <SearchPage v-if="currentTab === 'scan'" @open-detail="handleOpenLandmarkDetail" />
        <MapPage v-else-if="currentTab === 'map'" :focus-landmark="mapFocusTarget" @open-detail="handleOpenLandmarkDetail" />
        <LandmarkRepo v-else-if="currentTab === 'repo'" @select="handleSelectLandmark" />
        <ProfilePage v-else-if="currentTab === 'profile'" :user-nickname="nickname" :user-email="email" @logout="handleLogout" />
      </KeepAlive>
      <BottomNav v-model="currentTab" />
    </div>
  </template>
</template>

<style>
.splash {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #e8f5e9 0%, #f0f7f4 50%, #e0f0e8 100%);
}

.splash-icon {
  width: 64px;
  height: 64px;
  background: #2d8a6e;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulse 1.5s ease-in-out infinite;
}

.splash-icon svg {
  width: 34px;
  height: 34px;
  color: #fff;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.08); opacity: 0.8; }
}
</style>
