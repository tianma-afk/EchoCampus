<script setup lang="ts">
const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [tab: string]
}>()

const tabs = [
  { key: 'scan', label: '识别', icon: 'scan' },
  { key: 'map', label: '地图', icon: 'map' },
  { key: 'repo', label: '地标库', icon: 'repo' },
  { key: 'profile', label: '我的', icon: 'profile' },
]

const switchTab = (key: string) => {
  emit('update:modelValue', key)
}
</script>

<template>
  <nav class="bottom-nav">
    <div
      v-for="tab in tabs"
      :key="tab.key"
      class="nav-item"
      :class="{ active: props.modelValue === tab.key }"
      @click="switchTab(tab.key)"
    >
      <div class="nav-icon-wrapper">
        <svg v-if="tab.icon === 'scan'" class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M3 7V5a2 2 0 012-2h2" />
          <path d="M17 3h2a2 2 0 012 2v2" />
          <path d="M21 17v2a2 2 0 01-2 2h-2" />
          <path d="M7 21H5a2 2 0 01-2-2v-2" />
          <line x1="7" y1="12" x2="17" y2="12" />
        </svg>
        <svg v-else-if="tab.icon === 'map'" class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
          <line x1="8" y1="2" x2="8" y2="18" />
          <line x1="16" y1="6" x2="16" y2="22" />
        </svg>
        <svg v-else-if="tab.icon === 'repo'" class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <rect x="3" y="3" width="7" height="7" rx="1" />
          <rect x="14" y="3" width="7" height="7" rx="1" />
          <rect x="3" y="14" width="7" height="7" rx="1" />
          <rect x="14" y="14" width="7" height="7" rx="1" />
        </svg>
        <svg v-else-if="tab.icon === 'profile'" class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <circle cx="12" cy="8" r="4" />
          <path d="M20 21a8 8 0 10-16 0" />
        </svg>
      </div>
      <span class="nav-label">{{ tab.label }}</span>
    </div>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 64px;
  background: #fff;
  border-top: 1px solid #e8f5e9;
  z-index: 100;
  padding-bottom: env(safe-area-inset-bottom, 0);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex: 1;
  cursor: pointer;
  padding: 6px 0;
  transition: color 0.2s;
}

.nav-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  transition: background 0.2s;
}

.nav-item.active .nav-icon-wrapper {
  background: #2d8a6e;
}

.nav-icon {
  width: 20px;
  height: 20px;
  color: #9ca3af;
  transition: color 0.2s;
}

.nav-item.active .nav-icon {
  color: #fff;
}

.nav-label {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 500;
  transition: color 0.2s;
}

.nav-item.active .nav-label {
  color: #2d8a6e;
}
</style>
