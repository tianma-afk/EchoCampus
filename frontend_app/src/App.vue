<script setup lang="ts">
import { ref } from 'vue'
import SearchPage from './views/search/SearchPage.vue'
import MapPage from './views/map/MapPage.vue'
import LandmarkRepo from './views/repo/LandmarkRepo.vue'
import LandmarkDetail from './views/detail/LandmarkDetail.vue'
import ProfilePage from './views/profile/ProfilePage.vue'
import BottomNav from './components/BottomNav.vue'

interface LandmarkData {
  name: string
  rating: number
  checkins: number
  openTime: string
  category: string
  tags: string[]
  color: string
  buildYear: string
  openTimeDetail: string
  floors: string
  location: string
  description: string
  totalFloors: number
  recommendRate: number
}

const currentTab = ref('repo')
const showDetail = ref(false)
const selectedLandmark = ref<LandmarkData | null>(null)

const handleSelectLandmark = (landmark: LandmarkData) => {
  selectedLandmark.value = landmark
  showDetail.value = true
}

const handleBack = () => {
  showDetail.value = false
  selectedLandmark.value = null
}
</script>

<template>
  <SearchPage v-if="currentTab === 'scan'" />
  <MapPage v-else-if="currentTab === 'map'" />
  <LandmarkDetail
    v-else-if="currentTab === 'repo' && showDetail && selectedLandmark"
    :landmark="selectedLandmark"
    @back="handleBack"
  />
  <LandmarkRepo v-else-if="currentTab === 'repo'" @select="handleSelectLandmark" />
  <ProfilePage v-else-if="currentTab === 'profile'" />
  <BottomNav v-if="!showDetail" v-model="currentTab" />
</template>
