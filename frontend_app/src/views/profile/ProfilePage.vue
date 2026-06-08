<script setup lang="ts">
interface CheckinRecord {
  name: string
  color: string
  date: string
}

const recentCheckins: CheckinRecord[] = [
  { name: '图书馆', color: '#4a8c7a', date: '2024-06-10' },
  { name: '枫林广场', color: '#d47a4a', date: '2024-06-08' },
  { name: '体育馆', color: '#7b5ea7', date: '2024-06-05' },
]
</script>

<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="header-content">
        <div class="user-info">
          <div class="avatar">张</div>
          <div class="user-details">
            <h2 class="user-name">张同学</h2>
          </div>
        </div>
        <div class="stats-bar">
          <div class="stat-item">
            <div class="stat-number">12</div>
            <div class="stat-label">打卡</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-number">36</div>
            <div class="stat-label">识别</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-number">8</div>
            <div class="stat-label">收藏</div>
          </div>
        </div>
      </div>
    </div>

    <div class="profile-scroll">
      <div class="scroll-content">
        <div class="section">
          <div class="section-header">
            <h3 class="section-title">最近打卡</h3>
            <button class="view-all-btn">查看全部</button>
          </div>
          <div class="checkin-cards">
            <div
              v-for="record in recentCheckins"
              :key="record.name"
              class="checkin-card"
            >
              <div class="checkin-image" :style="{ background: record.color }">
                <span class="checkin-name">{{ record.name }}</span>
              </div>
              <div class="checkin-date">{{ record.date }}</div>
            </div>
          </div>
        </div>

        <div class="section">
          <h3 class="section-title">功能</h3>
          <div class="menu-card">
            <div class="menu-item">
              <div class="menu-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z" />
                  <circle cx="12" cy="13" r="4" />
                </svg>
              </div>
              <span class="menu-label">我的识别记录</span>
              <span class="menu-count">36</span>
              <svg class="menu-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </div>
            <div class="menu-divider"></div>
            <div class="menu-item">
              <div class="menu-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                  <circle cx="12" cy="10" r="3" />
                </svg>
              </div>
              <span class="menu-label">我的打卡记录</span>
              <span class="menu-count">12</span>
              <svg class="menu-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </div>
            <div class="menu-divider"></div>
            <div class="menu-item">
              <div class="menu-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
                </svg>
              </div>
              <span class="menu-label">我的收藏</span>
              <span class="menu-count">8</span>
              <svg class="menu-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </div>
          </div>
        </div>

        <div class="section">
          <h3 class="section-title">设置</h3>
          <div class="menu-card">
            <div class="menu-item">
              <div class="menu-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" />
                  <path d="M13.73 21a2 2 0 01-3.46 0" />
                </svg>
              </div>
              <span class="menu-label">消息通知</span>
              <svg class="menu-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </div>
            <div class="menu-divider"></div>
            <div class="menu-item">
              <div class="menu-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="3" />
                  <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z" />
                </svg>
              </div>
              <span class="menu-label">通用设置</span>
              <svg class="menu-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </div>
            <div class="menu-divider"></div>
            <div class="menu-item">
              <div class="menu-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3" />
                  <line x1="12" y1="17" x2="12.01" y2="17" />
                </svg>
              </div>
              <span class="menu-label">帮助与反馈</span>
              <svg class="menu-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </div>
          </div>
        </div>

        <button class="logout-btn">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
            <polyline points="16 17 21 12 16 7" />
            <line x1="21" y1="12" x2="9" y2="12" />
          </svg>
          退出登录
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--color-bg);
}

.profile-header {
  position: relative;
  flex-shrink: 0;
  background: var(--color-bg);
  overflow: hidden;
}

.header-content {
  position: relative;
  z-index: 1;
  padding: 24px 16px 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 600;
  color: var(--color-primary);
  flex-shrink: 0;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-heading);
  margin: 0;
}


.stats-bar {
  display: flex;
  align-items: center;
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: 14px 8px;
  box-shadow: var(--shadow-sm);
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-number {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-heading);
}

.stat-label {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--color-divider);
}

.profile-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 16px 84px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.profile-scroll::-webkit-scrollbar {
  display: none;
}

.scroll-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 16px;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-heading);
  margin: 0;
}

.view-all-btn {
  border: none;
  background: none;
  color: var(--color-primary);
  font-size: 13px;
  cursor: pointer;
}

.checkin-cards {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  scrollbar-width: none;
}

.checkin-cards::-webkit-scrollbar {
  display: none;
}

.checkin-card {
  flex-shrink: 0;
  width: 120px;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.checkin-image {
  height: 100px;
  display: flex;
  align-items: flex-end;
  padding: 10px;
}

.checkin-name {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: rgba(255, 255, 255, 0.25);
  padding: 3px 10px;
  border-radius: var(--radius-md);
}

.checkin-date {
  font-size: 12px;
  color: var(--color-text-secondary);
  padding: 8px 10px;
}

.menu-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
}

.menu-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-icon svg {
  width: 18px;
  height: 18px;
  color: var(--color-primary);
}

.menu-label {
  flex: 1;
  font-size: 14px;
  color: var(--color-text-heading);
}

.menu-count {
  font-size: 13px;
  color: var(--color-primary);
  background: var(--color-primary-light);
  padding: 2px 10px;
  border-radius: var(--radius-md);
}

.menu-arrow {
  width: 16px;
  height: 16px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.menu-divider {
  height: 1px;
  background: var(--color-divider);
  margin: 0 16px;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 48px;
  border: none;
  border-radius: var(--radius-xl);
  background: var(--color-bg-card);
  color: var(--color-danger);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
}

.logout-btn svg {
  width: 18px;
  height: 18px;
}
</style>
