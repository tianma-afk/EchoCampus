<script setup lang="ts">
</script>

<template>
  <div class="map-page">
    <div class="placeholder">
      <svg class="map-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
        <line x1="8" y1="2" x2="8" y2="18" />
        <line x1="16" y1="6" x2="16" y2="22" />
      </svg>
      <p>地图页面开发中</p>
    </div>
  </div>
</template>

<style scoped>
.map-page {
  width: 100%;
  height: 100vh;
  background: #f0f7f4;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.map-icon {
  width: 64px;
  height: 64px;
  color: #2d8a6e;
  opacity: 0.5;
}

.placeholder p {
  font-size: 16px;
  color: #9ca3af;
}
</style>
