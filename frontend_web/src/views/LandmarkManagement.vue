<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

interface Landmark {
  id: number
  name: string
  area: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
  completion: number
  feedbackCount: number
  feedbackTotal: number
  color: string
}

const landmarks = ref<Landmark[]>([
  {
    id: 1,
    name: '图书馆',
    area: '东校区',
    priority: 'HIGH',
    completion: 78,
    feedbackCount: 16,
    feedbackTotal: 20,
    color: '#6ee7b7'
  },
  {
    id: 2,
    name: '体育馆',
    area: '南校区',
    priority: 'MEDIUM',
    completion: 45,
    feedbackCount: 8,
    feedbackTotal: 15,
    color: '#93c5fd'
  },
  {
    id: 3,
    name: '实验楼A座',
    area: '西校区',
    priority: 'LOW',
    completion: 92,
    feedbackCount: 5,
    feedbackTotal: 8,
    color: '#fcd34d'
  },
  {
    id: 4,
    name: '学生活动中心',
    area: '东校区',
    priority: 'MEDIUM',
    completion: 55,
    feedbackCount: 7,
    feedbackTotal: 12,
    color: '#f9a8d4'
  },
  {
    id: 5,
    name: '行政楼',
    area: '北校区',
    priority: 'LOW',
    completion: 88,
    feedbackCount: 6,
    feedbackTotal: 8,
    color: '#c4b5fd'
  },
  {
    id: 6,
    name: '医学楼',
    area: '南校区',
    priority: 'HIGH',
    completion: 33,
    feedbackCount: 4,
    feedbackTotal: 18,
    color: '#5eead4'
  }
])

const priorityFilter = ref('全部')
const areaFilter = ref('全部')

const filteredLandmarks = computed(() => {
  return landmarks.value.filter(item => {
    const matchPriority = priorityFilter.value === '全部' || item.priority === priorityFilter.value
    const matchArea = areaFilter.value === '全部' || item.area === areaFilter.value
    return matchPriority && matchArea
  })
})

const priorityColor = (priority: string) => {
  switch (priority) {
    case 'HIGH':
      return { bg: '#fef2f2', text: '#ef4444' }
    case 'MEDIUM':
      return { bg: '#fffbeb', text: '#f59e0b' }
    case 'LOW':
      return { bg: '#eff6ff', text: '#3b82f6' }
    default:
      return { bg: '#f3f4f6', text: '#6b7280' }
  }
}

const completionColor = (completion: number) => {
  if (completion >= 80) return '#10b981'
  if (completion >= 50) return '#f59e0b'
  return '#ef4444'
}
</script>

<template>
  <div class="landmark-management">
    <div class="page-header">
      <div class="page-title">
        <h1>地标管理</h1>
        <span class="page-count">共 {{ filteredLandmarks.length }} 个地标</span>
      </div>
      <div class="page-actions">
        <select v-model="priorityFilter" class="filter-select">
          <option value="全部">全部</option>
          <option value="HIGH">HIGH</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="LOW">LOW</option>
        </select>
        <select v-model="areaFilter" class="filter-select">
          <option value="全部">全部</option>
          <option value="东校区">东校区</option>
          <option value="南校区">南校区</option>
          <option value="西校区">西校区</option>
          <option value="北校区">北校区</option>
        </select>
        <button class="add-btn" @click="router.push('/landmark/create')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          新增地标
        </button>
      </div>
    </div>

    <div class="landmark-grid">
      <div v-for="landmark in filteredLandmarks" :key="landmark.id" class="landmark-card">
        <div class="card-header" :style="{ backgroundColor: landmark.color + '40' }">
          <div class="card-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
              <circle cx="12" cy="10" r="3" />
            </svg>
          </div>
          <span class="priority-badge" :style="priorityColor(landmark.priority)">
            {{ landmark.priority }}
          </span>
        </div>

        <div class="card-body">
          <div class="card-title-row">
            <h3 class="card-title">{{ landmark.name }}</h3>
            <span class="card-area">{{ landmark.area }}</span>
          </div>

          <div class="progress-section">
            <div class="progress-header">
              <span class="progress-label">完成度</span>
              <span class="progress-value" :style="{ color: completionColor(landmark.completion) }">
                {{ landmark.completion }}%
              </span>
            </div>
            <div class="progress-bar">
              <div
                class="progress-fill"
                :style="{
                  width: landmark.completion + '%',
                  backgroundColor: completionColor(landmark.completion)
                }"
              ></div>
            </div>
          </div>

          <div class="feedback-info">
            <span class="feedback-dot"></span>
            <span class="feedback-text">审核反馈 {{ landmark.feedbackCount }}/{{ landmark.feedbackTotal }}</span>
          </div>

          <div class="card-actions">
            <button class="action-btn edit-btn">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
              </svg>
              编辑信息
            </button>
            <button class="action-btn delete-btn">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              </svg>
              删除
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.landmark-management {
  max-width: 1200px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-title h1 {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.page-count {
  font-size: 13px;
  color: #9ca3af;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #374151;
  background: #fff;
  cursor: pointer;
  outline: none;
}

.filter-select:focus {
  border-color: #10b981;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #059669;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.add-btn:hover {
  background: #047857;
}

.add-btn svg {
  width: 16px;
  height: 16px;
}

.landmark-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.landmark-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.2s;
}

.landmark-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.card-header {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.card-icon {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-icon svg {
  width: 24px;
  height: 24px;
  color: #6b7280;
}

.priority-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}

.card-body {
  padding: 16px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.card-area {
  font-size: 12px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
}

.progress-section {
  margin-bottom: 12px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.progress-label {
  font-size: 12px;
  color: #9ca3af;
}

.progress-value {
  font-size: 12px;
  font-weight: 600;
}

.progress-bar {
  height: 6px;
  background: #f3f4f6;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
}

.feedback-info {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 16px;
}

.feedback-dot {
  width: 6px;
  height: 6px;
  background: #10b981;
  border-radius: 50%;
}

.feedback-text {
  font-size: 12px;
  color: #6b7280;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.action-btn svg {
  width: 14px;
  height: 14px;
}

.edit-btn {
  color: #6b7280;
}

.edit-btn:hover {
  border-color: #10b981;
  color: #10b981;
}

.delete-btn {
  color: #ef4444;
}

.delete-btn:hover {
  background: #fef2f2;
  border-color: #ef4444;
}
</style>
