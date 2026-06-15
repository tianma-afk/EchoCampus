<script setup lang="ts">
import { ref, watch } from 'vue'
import { getCheckinHistory, type CheckinRecord } from '../../api/checkin'
import { getFavorites, type FavoriteRecord } from '../../api/favorite'
import { getUserRatings, type RatingRecord } from '../../api/rating'
import { getRecognitions, type RecognitionRecord } from '../../api/recognition'

const props = defineProps<{ defaultTab?: number }>()
const emit = defineEmits<{ back: [] }>()

const tabs = ['打卡', '收藏', '评分', '识别']
const activeTab = ref(props.defaultTab ?? 0)

const touchStartX = ref(0)
function handleTouchStart(e: TouchEvent) {
  touchStartX.value = e.touches[0].clientX
}
function handleTouchEnd(e: TouchEvent) {
  const delta = e.changedTouches[0].clientX - touchStartX.value
  if (delta < -60 && activeTab.value < tabs.length - 1) {
    activeTab.value++
  } else if (delta > 60 && activeTab.value > 0) {
    activeTab.value--
  }
}

// ---- 打卡 ----
const checkinRecords = ref<CheckinRecord[]>([])
const checkinLoading = ref(true)
const checkinPage = ref(1)
const checkinTotal = ref(0)

async function loadCheckins(reset = false) {
  if (reset) { checkinPage.value = 1; checkinRecords.value = [] }
  checkinLoading.value = true
  try {
    const res = await getCheckinHistory(checkinPage.value, 10)
    if (res.code === '00000') {
      if (reset) checkinRecords.value = res.data.records
      else checkinRecords.value.push(...res.data.records)
      checkinTotal.value = res.data.total
    }
  } catch { /* ignore */ }
  checkinLoading.value = false
}

function loadMoreCheckins() {
  if (checkinRecords.value.length >= checkinTotal.value) return
  checkinPage.value++
  loadCheckins(false)
}

// ---- 收藏 ----
const favoriteRecords = ref<FavoriteRecord[]>([])
const favoriteLoading = ref(true)
const favoritePage = ref(1)
const favoriteTotal = ref(0)

async function loadFavorites(reset = false) {
  if (reset) { favoritePage.value = 1; favoriteRecords.value = [] }
  favoriteLoading.value = true
  try {
    const res = await getFavorites(favoritePage.value, 10)
    if (res.code === '00000') {
      if (reset) favoriteRecords.value = res.data.records
      else favoriteRecords.value.push(...res.data.records)
      favoriteTotal.value = res.data.total
    }
  } catch { /* ignore */ }
  favoriteLoading.value = false
}

function loadMoreFavorites() {
  if (favoriteRecords.value.length >= favoriteTotal.value) return
  favoritePage.value++
  loadFavorites(false)
}

// ---- 评分 ----
const ratingRecords = ref<RatingRecord[]>([])
const ratingLoading = ref(true)
const ratingPage = ref(1)
const ratingTotal = ref(0)

async function loadRatings(reset = false) {
  if (reset) { ratingPage.value = 1; ratingRecords.value = [] }
  ratingLoading.value = true
  try {
    const res = await getUserRatings(ratingPage.value, 10)
    if (res.code === '00000') {
      if (reset) ratingRecords.value = res.data.records
      else ratingRecords.value.push(...res.data.records)
      ratingTotal.value = res.data.total
    }
  } catch { /* ignore */ }
  ratingLoading.value = false
}

function loadMoreRatings() {
  if (ratingRecords.value.length >= ratingTotal.value) return
  ratingPage.value++
  loadRatings(false)
}

// ---- 识别 ----
const recognitionRecords = ref<RecognitionRecord[]>([])
const recognitionLoading = ref(true)
const recognitionPage = ref(1)
const recognitionTotal = ref(0)

async function loadRecognitions(reset = false) {
  if (reset) { recognitionPage.value = 1; recognitionRecords.value = [] }
  recognitionLoading.value = true
  try {
    const res = await getRecognitions(recognitionPage.value, 10)
    if (res.code === '00000') {
      if (reset) recognitionRecords.value = res.data.records
      else recognitionRecords.value.push(...res.data.records)
      recognitionTotal.value = res.data.total
    }
  } catch { /* ignore */ }
  recognitionLoading.value = false
}

function loadMoreRecognitions() {
  if (recognitionRecords.value.length >= recognitionTotal.value) return
  recognitionPage.value++
  loadRecognitions(false)
}

const loadedTabs = ref(new Set<number>())

function ensureTabLoaded(tab: number) {
  if (loadedTabs.value.has(tab)) return
  loadedTabs.value.add(tab)
  if (tab === 0) loadCheckins(true)
  else if (tab === 1) loadFavorites(true)
  else if (tab === 2) loadRatings(true)
  else if (tab === 3) loadRecognitions(true)
}

watch(activeTab, (val) => ensureTabLoaded(val), { immediate: true })

const cardColors = ['#4a8c7a', '#d47a4a', '#7b5ea7', '#3a7ca5', '#c0392b', '#27ae60', '#8e44ad', '#d35400']
function getColor(index: number) { return cardColors[index % cardColors.length] }

function renderStars(rating: number) {
  const full = Math.floor(rating)
  const half = rating - full >= 0.5
  return { full, half, empty: 5 - full - (half ? 1 : 0) }
}
</script>

<template>
  <div class="history-page" @touchstart="handleTouchStart" @touchend="handleTouchEnd">
    <div class="history-header">
      <button class="history-back" @click="emit('back')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <span class="history-title">我的记录</span>
      <div style="width: 40px" />
    </div>

    <div class="tab-bar">
      <button
        v-for="(tab, i) in tabs"
        :key="tab"
        class="tab-btn"
        :class="{ active: activeTab === i }"
        @click="activeTab = i"
      >{{ tab }}</button>
    </div>

    <div class="tab-content">
      <!-- 打卡 -->
      <div v-show="activeTab === 0" class="list-panel">
        <div v-if="checkinLoading" class="list-empty">加载中...</div>
        <div v-else-if="checkinRecords.length === 0" class="list-empty">暂无打卡记录</div>
        <div v-else class="card-list">
          <div v-for="(r, i) in checkinRecords" :key="r.id" class="record-card">
            <div class="card-img" :style="{ background: getColor(i) }">
              <span class="card-name">{{ r.landmarkName }}</span>
            </div>
            <div class="card-meta">
              <span class="card-date">{{ r.createdAt?.substring(0, 10) }}</span>
            </div>
          </div>
          <div v-if="checkinRecords.length < checkinTotal" class="load-more">
            <button class="load-more-btn" @click="loadMoreCheckins">加载更多</button>
          </div>
        </div>
      </div>

      <!-- 收藏 -->
      <div v-show="activeTab === 1" class="list-panel">
        <div v-if="favoriteLoading" class="list-empty">加载中...</div>
        <div v-else-if="favoriteRecords.length === 0" class="list-empty">暂无收藏记录</div>
        <div v-else class="card-list">
          <div v-for="(r, i) in favoriteRecords" :key="r.id" class="record-card wide">
            <div class="card-img" :style="{ background: r.coverImageUrl ? `url(${r.coverImageUrl}) center/cover` : getColor(i) }">
              <span class="card-name" v-if="!r.coverImageUrl">{{ r.landmarkName }}</span>
            </div>
            <div class="card-info">
              <div class="card-title-row">
                <span class="card-landmark-name">{{ r.landmarkName }}</span>
                <span class="card-category">{{ r.category }}</span>
              </div>
              <div class="card-rating-row">
                <template v-for="s in 5" :key="s">
                  <svg
                    v-if="s <= renderStars(r.landmarkRating).full"
                    class="mini-star filled" viewBox="0 0 24 24" fill="currentColor"
                  ><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></svg>
                  <svg
                    v-else-if="s === renderStars(r.landmarkRating).full + 1 && renderStars(r.landmarkRating).half"
                    class="mini-star empty" viewBox="0 0 24 24" fill="#e5e7eb"
                  ><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></svg>
                  <svg v-else class="mini-star empty" viewBox="0 0 24 24" fill="#e5e7eb"
                  ><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></svg>
                </template>
                <span class="mini-rating">{{ r.landmarkRating }}</span>
              </div>
            </div>
          </div>
          <div v-if="favoriteRecords.length < favoriteTotal" class="load-more">
            <button class="load-more-btn" @click="loadMoreFavorites">加载更多</button>
          </div>
        </div>
      </div>

      <!-- 评分 -->
      <div v-show="activeTab === 2" class="list-panel">
        <div v-if="ratingLoading" class="list-empty">加载中...</div>
        <div v-else-if="ratingRecords.length === 0" class="list-empty">暂无评分记录</div>
        <div v-else class="card-list">
          <div v-for="(r, i) in ratingRecords" :key="r.id" class="record-card wide">
            <div class="card-img" :style="{ background: r.coverImageUrl ? `url(${r.coverImageUrl}) center/cover` : getColor(i) }">
              <span class="card-name" v-if="!r.coverImageUrl">{{ r.landmarkName }}</span>
            </div>
            <div class="card-info">
              <div class="card-title-row">
                <span class="card-landmark-name">{{ r.landmarkName }}</span>
                <span class="card-category">{{ r.category }}</span>
              </div>
              <div class="card-rating-row">
                <span class="user-rating-label">我的评分：</span>
                <template v-for="s in 5" :key="s">
                  <svg
                    v-if="s <= renderStars(r.rating).full"
                    class="mini-star filled" viewBox="0 0 24 24" fill="currentColor"
                  ><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></svg>
                  <svg
                    v-else-if="s === renderStars(r.rating).full + 1 && renderStars(r.rating).half"
                    class="mini-star empty" viewBox="0 0 24 24" fill="#e5e7eb"
                  ><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></svg>
                  <svg v-else class="mini-star empty" viewBox="0 0 24 24" fill="#e5e7eb"
                  ><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></svg>
                </template>
                <span class="mini-rating">{{ r.rating }}</span>
              </div>
            </div>
          </div>
          <div v-if="ratingRecords.length < ratingTotal" class="load-more">
            <button class="load-more-btn" @click="loadMoreRatings">加载更多</button>
          </div>
        </div>
      </div>

      <!-- 识别 -->
      <div v-show="activeTab === 3" class="list-panel">
        <div v-if="recognitionLoading" class="list-empty">加载中...</div>
        <div v-else-if="recognitionRecords.length === 0" class="list-empty">暂无识别记录</div>
        <div v-else class="card-list">
          <div v-for="r in recognitionRecords" :key="r.id" class="record-card wide">
            <div class="card-img" :style="{ background: r.imageUrl ? `url(${r.imageUrl}) center/cover` : 'var(--color-bg-input)' }">
            </div>
            <div class="card-info">
              <div class="card-title-row">
                <span class="card-landmark-name">{{ r.landmarkName || '未识别出地标' }}</span>
                <span v-if="r.similarity != null" class="card-category">{{ (r.similarity * 100).toFixed(1) }}%</span>
              </div>
              <div v-if="r.coverImageUrl" class="card-rating-row">
                <span class="user-rating-label">匹配地标：</span>
              </div>
              <span class="card-date">{{ r.createdAt?.substring(0, 10) }}</span>
            </div>
          </div>
          <div v-if="recognitionRecords.length < recognitionTotal" class="load-more">
            <button class="load-more-btn" @click="loadMoreRecognitions">加载更多</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.history-page {
  position: fixed;
  inset: 0;
  z-index: 110;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  flex-shrink: 0;
}

.history-back {
  width: 40px;
  height: 40px;
  border: none;
  background: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.history-back svg {
  width: 24px;
  height: 24px;
  color: var(--color-text-heading);
}

.history-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-heading);
}

.tab-bar {
  display: flex;
  gap: 0;
  padding: 0 16px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--color-divider);
}

.tab-btn {
  flex: 1;
  height: 40px;
  border: none;
  background: none;
  font-size: 14px;
  color: var(--color-text-secondary);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.tab-btn.active {
  color: var(--color-primary);
  font-weight: 600;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  border-radius: 2px;
  background: var(--color-primary);
}

.tab-content {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.tab-content::-webkit-scrollbar { display: none; }

.list-panel {
  min-height: 100%;
  padding: 16px;
}

.list-empty {
  text-align: center;
  padding: 60px 16px;
  color: var(--color-text-muted);
  font-size: 14px;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-card {
  display: flex;
  gap: 12px;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.record-card.wide {
  flex-direction: row;
  align-items: stretch;
  height: 100px;
}

.record-card.wide .card-img {
  width: 100px;
  height: 100%;
  flex-shrink: 0;
}

.record-card:not(.wide) .card-img {
  width: 100%;
  height: 100px;
}

.card-img {
  display: flex;
  align-items: flex-end;
  padding: 10px;
  background-size: cover;
  background-position: center;
}

.card-name {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: rgba(255, 255, 255, 0.25);
  padding: 3px 10px;
  border-radius: var(--radius-md);
}

.card-meta {
  padding: 8px 10px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.card-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 12px 12px 12px 0;
  overflow: hidden;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-landmark-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-heading);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-category {
  font-size: 12px;
  color: var(--color-primary);
  background: var(--color-primary-light);
  padding: 2px 8px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.card-rating-row {
  display: flex;
  align-items: center;
  gap: 2px;
}

.mini-star {
  width: 12px;
  height: 12px;
}

.mini-star.filled {
  color: var(--color-star);
}

.mini-rating {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-star);
  margin-left: 4px;
}

.user-rating-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-right: 4px;
}

.card-date {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.record-card:not(.wide) .card-date {
  padding: 8px 10px;
  display: block;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.load-more-btn {
  border: none;
  background: none;
  color: var(--color-primary);
  font-size: 14px;
  cursor: pointer;
  padding: 8px 20px;
}
</style>
