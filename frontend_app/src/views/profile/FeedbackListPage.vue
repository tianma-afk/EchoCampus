<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyFeedbacks, type FeedbackRecord, FEEDBACK_TYPE_MAP, FEEDBACK_STATUS_MAP } from '../../api/feedback'
import FeedbackDetailPage from './FeedbackDetailPage.vue'
import FeedbackPage from '../search/FeedbackPage.vue'

defineEmits<{ back: [] }>()

const records = ref<FeedbackRecord[]>([])
const loading = ref(true)
const page = ref(1)
const total = ref(0)

async function loadFeedbacks(nextPage: number, reset = false) {
  if (reset) { records.value = [] }
  loading.value = true
  try {
    const res = await getMyFeedbacks(nextPage, 10)
    if (res.code === '00000') {
      if (reset) records.value = res.data.records
      else records.value.push(...res.data.records)
      total.value = res.data.total
      page.value = nextPage
    }
  } catch { /* ignore */ }
  loading.value = false
}

function loadMore() {
  if (records.value.length >= total.value) return
  loadFeedbacks(page.value + 1)
}

function typeLabel(t: string) {
  return FEEDBACK_TYPE_MAP[t] || t
}

function statusBadge(s: string) {
  return FEEDBACK_STATUS_MAP[s] || { label: s, color: '#6b7280', bg: '#f3f4f6' }
}

function formatDate(d: string) {
  return d?.substring(0, 10) || ''
}

onMounted(() => loadFeedbacks(1, true))

const selectedFeedback = ref<FeedbackRecord | null>(null)
const showSuggestForm = ref(false)

function onSuggestDone() {
  showSuggestForm.value = false
  loadFeedbacks(1, true)
}
</script>

<template>
  <div class="feedback-list-page">
    <div class="list-header">
      <button class="back-btn" @click="$emit('back')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <span class="header-title">我的反馈</span>
      <div style="width: 40px" />
    </div>

    <div class="list-body">
      <!-- 建议新增地标入口 -->
      <div class="suggest-banner">
        <button class="suggest-entry-btn" @click="showSuggestForm = true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="suggest-icon">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="16" />
            <line x1="8" y1="12" x2="16" y2="12" />
          </svg>
          <span>建议新增地标</span>
        </button>
      </div>

      <div v-if="loading" class="list-empty">加载中...</div>
      <div v-else-if="records.length === 0" class="list-empty">暂无反馈记录</div>
      <div v-else class="list-scroll">
        <div
          v-for="r in records"
          :key="r.id"
          class="feedback-card"
          @click="selectedFeedback = r"
        >
          <div class="card-left">
            <span class="card-type">{{ typeLabel(r.feedbackType) }}</span>
            <span class="card-landmark">{{ r.landmarkName || '新增地标建议' }}</span>
          </div>
          <div class="card-right">
            <span
              class="status-badge"
              :style="{ color: statusBadge(r.status).color, background: statusBadge(r.status).bg }"
            >{{ statusBadge(r.status).label }}</span>
            <span class="card-date">{{ formatDate(r.createdAt) }}</span>
          </div>
        </div>
        <div v-if="records.length < total" class="load-more">
          <button class="load-more-btn" @click="loadMore">加载更多</button>
        </div>
      </div>
    </div>
  </div>

  <FeedbackDetailPage
    v-if="selectedFeedback"
    :feedback="selectedFeedback"
    @back="selectedFeedback = null"
  />

  <FeedbackPage
    v-if="showSuggestForm"
    mode="suggest"
    @done="onSuggestDone"
    @back="showSuggestForm = false"
  />
</template>

<style scoped>
.feedback-list-page {
  position: fixed;
  inset: 0;
  z-index: 120;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  flex-shrink: 0;
  background: rgba(250, 248, 245, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.back-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.back-btn svg {
  width: 24px;
  height: 24px;
  color: var(--color-text-heading);
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-heading);
}

.list-body {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.list-body::-webkit-scrollbar { display: none; }

.suggest-banner {
  padding: 12px 16px;
}

.suggest-entry-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 46px;
  border: 2px dashed #d1d5db;
  border-radius: var(--radius-lg);
  background: var(--color-bg-card);
  color: #d97706;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.suggest-entry-btn:active {
  background: #fffbeb;
  border-color: #d97706;
}

.suggest-icon {
  width: 18px;
  height: 18px;
}

.list-empty {
  text-align: center;
  padding: 60px 16px;
  color: var(--color-text-muted);
  font-size: 14px;
}

.list-scroll {
  padding: 0 16px 40px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feedback-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  box-shadow: var(--shadow-sm);
  cursor: pointer;
}

.card-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.card-type {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.card-landmark {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-heading);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.status-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--radius-md);
}

.card-date {
  font-size: 12px;
  color: var(--color-text-muted);
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
