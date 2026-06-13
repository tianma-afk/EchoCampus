<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCheckinHistory, type CheckinRecord } from '../../api/checkin'

const emit = defineEmits<{ back: [] }>()

const records = ref<CheckinRecord[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const allLoaded = ref(false)
const pageSize = 20

const cardColors = ['#4a8c7a', '#d47a4a', '#7b5ea7', '#3a7ca5', '#c0392b', '#27ae60', '#8e44ad', '#d35400']

function getCardColor(index: number) {
  return cardColors[index % cardColors.length]
}

async function loadPage() {
  if (loading.value || allLoaded.value) return
  loading.value = true
  try {
    const res = await getCheckinHistory(page.value, pageSize)
    if (res.code === '00000') {
      records.value.push(...res.data.records)
      total.value = res.data.total
      page.value++
      if (records.value.length >= total.value) {
        allLoaded.value = true
      }
    }
  } catch {
    console.error('加载打卡记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadPage)
</script>

<template>
  <div class="checkin-history-page">
    <div class="history-header">
      <button class="back-btn" @click="emit('back')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <h2 class="history-title">打卡记录</h2>
      <span class="history-total">共 {{ total }} 条</span>
    </div>

    <div class="history-scroll">
      <div v-if="records.length === 0 && !loading" class="empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
          <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
          <polyline points="22 4 12 14.01 9 11.01" />
        </svg>
        <p>暂无打卡记录</p>
      </div>

      <div v-else class="records-list">
        <div
          v-for="(record, index) in records"
          :key="record.id"
          class="record-item"
        >
          <div class="record-dot" :style="{ background: getCardColor(index) }">
            <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
              <circle cx="12" cy="10" r="3" />
            </svg>
          </div>
          <div class="record-info">
            <span class="record-name">{{ record.landmarkName }}</span>
            <span class="record-date">{{ record.createdAt?.substring(0, 10) }}</span>
          </div>
        </div>
      </div>

      <div v-if="loading" class="load-more">
        <span>加载中...</span>
      </div>
      <button
        v-else-if="!allLoaded"
        class="load-more-btn"
        @click="loadPage"
      >
        加载更多
      </button>
      <div v-else-if="records.length > 0" class="load-more done">
        <span>已显示全部 {{ total }} 条记录</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.checkin-history-page {
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
  gap: 12px;
  padding: 16px;
  padding-top: max(16px, env(safe-area-inset-top));
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-divider);
  flex-shrink: 0;
}

.back-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

.back-btn svg {
  width: 20px;
  height: 20px;
  color: var(--color-text-heading);
}

.history-title {
  flex: 1;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-heading);
  margin: 0;
}

.history-total {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.history-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: none;
}

.history-scroll::-webkit-scrollbar {
  display: none;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--color-text-muted);
}

.empty-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
  color: var(--color-primary);
  opacity: 0.5;
}

.empty-state p {
  font-size: 14px;
  margin: 0;
}

.records-list {
  padding: 8px 16px;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 12px;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
}

.record-dot {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.record-dot svg {
  width: 18px;
  height: 18px;
}

.record-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.record-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-heading);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-date {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.load-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  font-size: 13px;
  color: var(--color-text-muted);
}

.load-more.done {
  color: var(--color-text-secondary);
}

.load-more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 16px;
  border: none;
  background: none;
  font-size: 14px;
  color: var(--color-primary);
  cursor: pointer;
}

.load-more-btn:hover {
  background: var(--color-primary-light);
}
</style>
