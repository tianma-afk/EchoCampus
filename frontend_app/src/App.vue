<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'
import SearchPage from './views/search/SearchPage.vue'
import MapPage from './views/map/MapPage.vue'
import LandmarkRepo from './views/repo/LandmarkRepo.vue'
import LandmarkDetail from './views/detail/LandmarkDetail.vue'
import ProfilePage from './views/profile/ProfilePage.vue'
import BottomNav from './components/BottomNav.vue'

const API_BASE_URL = 'http://localhost:8080/api/v1'

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
</script>

<template>
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
      <ProfilePage v-else-if="currentTab === 'profile'" />
    </KeepAlive>
    <BottomNav v-model="currentTab" />
  </div>
</template>
