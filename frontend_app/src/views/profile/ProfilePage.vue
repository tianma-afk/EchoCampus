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
      <div class="header-bg"></div>
      <div class="header-content">
        <div class="user-info">
          <div class="avatar">张</div>
          <div class="user-details">
            <h2 class="user-name">张同学</h2>
            <p class="user-dept">计算机学院 · 2023级</p>
            <span class="admin-badge">管理员</span>
          </div>
        </div>
        <div class="stats-bar">
          <div class="stat-item">
            <div class="stat-number">12</div>
            <div class="stat-label">已打卡</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-number">4.8</div>
            <div class="stat-label">平均评分</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-number">36</div>
            <div class="stat-label">识别次数</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-number">8</div>
            <div class="stat-label">收藏地标</div>
          </div>
        </div>
      </div>
    </div>

    <div class="profile-scroll">
      <div class="scroll-content">
        <div class="admin-card">
          <div class="admin-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
          </div>
          <div class="admin-text">
            <h3 class="admin-title">管理员控制台</h3>
            <p class="admin-desc">地标管理 · 用户反馈审核</p>
          </div>
          <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </div>

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
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                </svg>
              </div>
              <span class="menu-label">隐私设置</span>
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
  background: #f0f7f4;
}

.profile-header {
  position: relative;
  flex-shrink: 0;
  background: #2d8a6e;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 20% 80%, rgba(255, 255, 255, 0.08) 0%, transparent 40%);
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
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.user-dept {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.admin-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #fff;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 10px;
  border-radius: 10px;
  width: fit-content;
}

.admin-badge::before {
  content: '';
  display: inline-block;
  width: 12px;
  height: 12px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='2'%3E%3Cpath d='M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
}

.stats-bar {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 14px;
  padding: 14px 8px;
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
  font-weight: 700;
  color: #fff;
}

.stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
}

.profile-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 16px 80px;
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

.admin-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #2d8a6e;
  border-radius: 16px;
  padding: 16px;
  cursor: pointer;
}

.admin-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.admin-icon svg {
  width: 22px;
  height: 22px;
  color: #fff;
}

.admin-text {
  flex: 1;
}

.admin-title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 2px;
}

.admin-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
}

.arrow-icon {
  width: 18px;
  height: 18px;
  color: rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
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
  color: #1a1a1a;
  margin: 0;
}

.view-all-btn {
  border: none;
  background: none;
  color: #2d8a6e;
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
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
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
  border-radius: 10px;
}

.checkin-date {
  font-size: 12px;
  color: #9ca3af;
  padding: 8px 10px;
}

.menu-card {
  background: #fff;
  border-radius: 16px;
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
  background: #e8f5e9;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-icon svg {
  width: 18px;
  height: 18px;
  color: #2d8a6e;
}

.menu-label {
  flex: 1;
  font-size: 14px;
  color: #1a1a1a;
}

.menu-count {
  font-size: 13px;
  color: #2d8a6e;
  background: #e8f5e9;
  padding: 2px 10px;
  border-radius: 10px;
}

.menu-arrow {
  width: 16px;
  height: 16px;
  color: #d1d5db;
  flex-shrink: 0;
}

.menu-divider {
  height: 1px;
  background: #f0f0f0;
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
  border-radius: 16px;
  background: #fff;
  color: #ef4444;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.logout-btn svg {
  width: 18px;
  height: 18px;
}
</style>
