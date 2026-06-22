<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()

const collapsed = ref(false)

function toggleCollapse() {
  collapsed.value = !collapsed.value
}

const menuItems = computed(() => {
  const items = [
    {
      id: 'home',
      label: '主页',
      icon: 'home',
      path: '/home',
    },
    {
      id: 'university',
      label: '大学管理',
      icon: 'school',
      path: '/universities',
    },
    {
      id: 'landmark',
      label: '地标管理',
      icon: 'location',
      path: '/landmark',
    },
    {
      id: 'task',
      label: '任务管理',
      icon: 'checklist',
      path: '/tasks',
    },
    {
      id: 'feedback',
      label: '反馈审核',
      icon: 'feedback',
      path: '/feedback',
    },
  ]
  if (auth.isSuperAdmin) {
    items.push({
      id: 'admins',
      label: '管理员管理',
      icon: 'admin',
      path: '/admins',
    })
  }
  return items
})

const isActive = (path: string) => {
  return route.path.startsWith(path)
}
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }">
    <nav class="sidebar-nav">
      <router-link
        v-for="item in menuItems"
        :key="item.id"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
        :title="collapsed ? item.label : ''"
      >
        <span class="nav-icon">
          <svg v-if="item.icon === 'home'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
            <polyline points="9 22 9 12 15 12 15 22" />
          </svg>
          <svg v-else-if="item.icon === 'school'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
            <path d="M6 12v5c0 2 3 3 6 3s6-1 6-3v-5" />
          </svg>
          <svg v-else-if="item.icon === 'location'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
            <circle cx="12" cy="10" r="3" />
          </svg>
          <svg v-else-if="item.icon === 'feedback'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <el-icon v-else-if="item.icon === 'checklist'" :size="20"><List /></el-icon>
          <svg v-else-if="item.icon === 'settings'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3" />
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
          </svg>
          <svg v-else-if="item.icon === 'admin'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
        </span>
        <span v-show="!collapsed" class="nav-label">{{ item.label }}</span>
      </router-link>
    </nav>

    <div class="sidebar-footer">
      <button class="collapse-btn" @click="toggleCollapse" :title="collapsed ? '展开菜单' : '收起菜单'">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline v-if="collapsed" points="13 17 18 12 13 7" />
          <polyline v-else points="11 7 6 12 11 17" />
          <line v-if="collapsed" x1="6" y1="12" x2="18" y2="12" />
        </svg>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 240px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: width 0.25s ease;
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
}

.sidebar.collapsed .sidebar-nav {
  padding: 16px 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  color: #6b7280;
  text-decoration: none;
  margin-bottom: 2px;
  transition: all 0.2s ease;
  position: relative;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
}

.sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 10px 0;
  gap: 0;
}

.nav-item:hover {
  background: #f3f4f6;
  color: #374151;
}

.nav-item.active {
  background: #ecfdf5;
  color: #059669;
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: #059669;
  border-radius: 0 2px 2px 0;
}

.sidebar.collapsed .nav-item.active::before {
  display: none;
}

.nav-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nav-icon svg {
  width: 20px;
  height: 20px;
}

.nav-label {
  font-size: 14px;
  flex: 1;
  opacity: 1;
  transition: opacity 0.15s ease;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid #f0f0f0;
}

.collapse-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 14px;
  border: none;
  background: none;
  color: #9ca3af;
  cursor: pointer;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
  font-family: inherit;
  white-space: nowrap;
  overflow: hidden;
}

.sidebar.collapsed .collapse-btn {
  justify-content: center;
  padding: 10px 0;
}

.collapse-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.collapse-btn svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.collapse-label {
  opacity: 1;
  transition: opacity 0.15s ease;
}
</style>
