<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getDashboardStats, type DashboardStats } from '../api/dashboard'

const router = useRouter()
const auth = useAuthStore()

const dashboard = ref<DashboardStats | null>(null)

const stats = ref([
  { label: '地标总数', value: '--', icon: 'location', color: '#059669' },
  { label: '大学总数', value: '--', icon: 'school', color: '#4facfe' },
  { label: '待处理反馈', value: '--', icon: 'feedback', color: '#f59e0b' },
  { label: '任务总数', value: '--', icon: 'new', color: '#a18cd1' },
])

const quickActions = [
  { label: '新增地标', desc: '录入新的校园地标信息', path: '/landmark/create', color: '#059669' },
  { label: '大学管理', desc: '管理大学与校区数据', path: '/universities', color: '#4facfe' },
  { label: '反馈审核', desc: '处理用户提交的反馈', path: '/feedback', color: '#f59e0b' },
  { label: '任务管理', desc: '查看任务状态', path: '/tasks', color: '#a18cd1' },
]

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    if (res.code === '00000' && res.data) {
      dashboard.value = res.data
      stats.value[0]!.value = String(res.data.landmarkCount)
      stats.value[1]!.value = String(res.data.universityCount)
      stats.value[2]!.value = String(res.data.pendingFeedbackCount)
      stats.value[3]!.value = String(res.data.taskCount)
    }
  } catch {
    // keep -- placeholders
  }
})
</script>

<template>
  <div class="home-dashboard">
    <!-- Welcome Banner -->
    <div class="welcome-banner">
      <div class="banner-content">
        <h1 class="banner-greeting">你好，{{ auth.username }}</h1>
      </div>
      <div class="banner-decoration">
        <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="60" cy="60" r="56" stroke="currentColor" stroke-width="1.5" opacity="0.15" />
          <circle cx="60" cy="60" r="42" stroke="currentColor" stroke-width="1" opacity="0.2" />
          <circle cx="60" cy="60" r="28" stroke="currentColor" stroke-width="1" opacity="0.25" />
          <path d="M60 32v16l10 10" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.5" />
        </svg>
      </div>
    </div>

    <!-- Stats Row -->
    <div class="stats-row">
      <div v-for="stat in stats" :key="stat.label" class="stat-card">
        <div class="stat-icon" :style="{ backgroundColor: stat.color + '14', color: stat.color }">
          <svg v-if="stat.icon === 'location'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
            <circle cx="12" cy="10" r="3" />
          </svg>
          <svg v-else-if="stat.icon === 'school'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
            <path d="M6 12v5c0 2 3 3 6 3s6-1 6-3v-5" />
          </svg>
          <svg v-else-if="stat.icon === 'feedback'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <svg v-else-if="stat.icon === 'new'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="16" />
            <line x1="8" y1="12" x2="16" y2="12" />
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <!-- Quick Actions -->
    <h2 class="section-title">快捷操作</h2>
    <div class="actions-grid">
      <button
        v-for="action in quickActions"
        :key="action.label"
        class="action-card"
        @click="router.push(action.path)"
      >
        <div class="action-dot" :style="{ backgroundColor: action.color }"></div>
        <div class="action-info">
          <span class="action-label">{{ action.label }}</span>
          <span class="action-desc">{{ action.desc }}</span>
        </div>
        <svg class="action-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </button>
    </div>

    <!-- Category Breakdown -->
    <h2 class="section-title">地标分类概览</h2>
    <div v-if="dashboard?.categoryBreakdown?.length" class="category-grid">
      <div
        v-for="cat in dashboard.categoryBreakdown"
        :key="cat.categoryName"
        class="category-card"
      >
        <span class="category-name">{{ cat.categoryName }}</span>
        <span class="category-count">{{ cat.count }}</span>
      </div>
    </div>
    <div v-else class="activity-placeholder">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
        <circle cx="12" cy="10" r="3" />
      </svg>
      <p>暂无地标数据</p>
      <span>创建地标后，分类统计将显示在这里</span>
    </div>
  </div>
</template>

<style scoped>
.home-dashboard {
}

/* ── Welcome Banner ── */
.welcome-banner {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border-radius: 16px;
  padding: 32px 36px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
  position: relative;
  overflow: hidden;
}

.banner-greeting {
  font-size: 26px;
  font-weight: 700;
  color: #065f46;
  margin: 0 0 6px;
  letter-spacing: -0.02em;
}

.banner-subtitle {
  font-size: 14px;
  color: #059669;
  margin: 0;
  font-weight: 500;
}

.banner-decoration {
  width: 100px;
  height: 100px;
  color: #059669;
  flex-shrink: 0;
}

.banner-decoration svg {
  width: 100%;
  height: 100%;
}

/* ── Stats Row ── */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 36px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.stat-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon svg {
  width: 22px;
  height: 22px;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.1;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}

/* ── Section Title ── */
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px;
}

/* ── Quick Actions ── */
.actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 36px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: #fff;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;
  text-align: left;
  font-family: inherit;
}

.action-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.action-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.action-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.action-label {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.action-desc {
  font-size: 12px;
  color: #9ca3af;
}

.action-arrow {
  width: 18px;
  height: 18px;
  color: #d1d5db;
  flex-shrink: 0;
}

.action-card:hover .action-arrow {
  color: #6b7280;
}

/* ── Activity Placeholder (empty state) ── */
.activity-placeholder {
  background: #fff;
  border-radius: 12px;
  padding: 48px 24px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
}

.activity-placeholder svg {
  width: 40px;
  height: 40px;
  color: #d1d5db;
  margin-bottom: 12px;
}

.activity-placeholder p {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 4px;
  font-weight: 500;
}

.activity-placeholder span {
  font-size: 13px;
  color: #9ca3af;
}

/* ── Category Overview ── */
.category-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.category-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
}

.category-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.category-count {
  font-size: 20px;
  font-weight: 700;
  color: #374151;
}
</style>
