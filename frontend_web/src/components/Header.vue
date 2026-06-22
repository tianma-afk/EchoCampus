<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const showDropdown = ref(false)

const roleLabel = computed(() => {
  if (auth.role === 'SUPER_ADMIN') return '超级管理员'
  if (auth.role === 'ADMIN') return '管理员'
  return ''
})

const roleTagType = computed(() => {
  return auth.role === 'SUPER_ADMIN' ? 'danger' : 'warning'
})

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
}

function closeDropdown() {
  showDropdown.value = false
}

function handleLogout() {
  showDropdown.value = false
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <header class="header">
    <div class="header-left">
      <span class="brand-name">映像校园</span>
      <span class="brand-divider"></span>
      <span class="brand-sub">控制台</span>
    </div>

    <div class="header-right">
      <div class="avatar-wrapper" @click.stop="toggleDropdown">
        <div class="user-avatar">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
        </div>

        <!-- Dropdown -->
        <Transition name="dropdown">
          <div v-if="showDropdown" class="dropdown-panel" @click.stop>
            <div class="dropdown-user">
              <div class="dropdown-user-info">
                <span class="dropdown-name">{{ auth.username }}</span>
                <span class="dropdown-email">{{ auth.email }}</span>
              </div>
            </div>
            <div class="dropdown-role">
              <el-tag :type="roleTagType" size="small">{{ roleLabel }}</el-tag>
            </div>
            <div class="dropdown-divider"></div>
            <button class="dropdown-logout" @click="handleLogout">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
              退出登录
            </button>
          </div>
        </Transition>
      </div>

      <!-- Click-outside overlay -->
      <div v-if="showDropdown" class="dropdown-overlay" @click="closeDropdown"></div>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-name {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: -0.01em;
}

.brand-divider {
  width: 1px;
  height: 16px;
  background: #d1d5db;
}

.brand-sub {
  font-size: 18px;
  font-weight: 400;
  color: #6b7280;
}

.header-right {
  position: relative;
}

/* ── Avatar ── */
.avatar-wrapper {
  position: relative;
  cursor: pointer;
  z-index: 101;
}

.user-avatar {
  width: 36px;
  height: 36px;
  background: #f3f4f6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: background 0.2s, color 0.2s;
}

.user-avatar svg {
  width: 20px;
  height: 20px;
}

.user-avatar:hover {
  background: #e5e7eb;
  color: #374151;
}

/* ── Dropdown Panel ── */
.dropdown-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 260px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1), 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 20px;
  z-index: 102;
}

.dropdown-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.dropdown-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.dropdown-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.dropdown-email {
  font-size: 12px;
  color: #9ca3af;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-role {
  margin-bottom: 12px;
}

.dropdown-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 0 -20px 12px;
}

.dropdown-logout {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: none;
  background: none;
  color: #6b7280;
  cursor: pointer;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  font-family: inherit;
}

.dropdown-logout:hover {
  background: #fef2f2;
  color: #ef4444;
}

.dropdown-logout svg {
  width: 18px;
  height: 18px;
}

/* ── Dropdown Transition ── */
.dropdown-enter-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.dropdown-leave-active {
  transition: opacity 0.1s ease, transform 0.1s ease;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-4px);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ── Click-outside overlay ── */
.dropdown-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
}
</style>
