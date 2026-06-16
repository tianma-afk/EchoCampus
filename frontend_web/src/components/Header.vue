<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const roleLabel = computed(() => {
  if (auth.role === 'SUPER_ADMIN') return '超级管理员'
  if (auth.role === 'ADMIN') return '管理员'
  return ''
})

const roleTagType = computed(() => {
  return auth.role === 'SUPER_ADMIN' ? 'danger' : 'warning'
})

const avatarChar = computed(() => {
  return auth.username ? auth.username.charAt(0) : '管'
})
</script>

<template>
  <header class="header">
    <div class="search-bar">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
      <input type="text" placeholder="搜索地标、反馈..." />
    </div>

    <div class="header-right">
      <button class="notification-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
      </button>

      <div class="user-info">
        <div class="user-text">
          <span class="user-name">{{ auth.username }}</span>
          <span class="user-email">{{ auth.email }}</span>
          <span class="user-role">
            <el-tag :type="roleTagType" size="small">{{ roleLabel }}</el-tag>
          </span>
        </div>
        <div class="user-avatar">{{ avatarChar }}</div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f3f4f6;
  padding: 8px 16px;
  border-radius: 20px;
  width: 320px;
}

.search-bar svg {
  width: 18px;
  height: 18px;
  color: #9ca3af;
}

.search-bar input {
  border: none;
  background: none;
  outline: none;
  font-size: 14px;
  color: #374151;
  width: 100%;
}

.search-bar input::placeholder {
  color: #9ca3af;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notification-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: none;
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: all 0.2s;
}

.notification-btn:hover {
  background: #f3f4f6;
}

.notification-btn svg {
  width: 20px;
  height: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-text {
  display: flex;
  flex-direction: column;
  text-align: right;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.user-email {
  font-size: 12px;
  color: #6b7280;
}

.user-role {
  font-size: 11px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #10b981, #059669);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
}
</style>
