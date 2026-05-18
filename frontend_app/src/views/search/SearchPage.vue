<script setup lang="ts">
</script>

<template>
  <div class="search-page">
    <div class="floating-ball">
      <svg class="camera-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
        <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z" />
        <circle cx="12" cy="13" r="4" />
      </svg>
    </div>
  </div>
</template>

<style scoped>
.search-page {
  position: relative;
  width: 100%;
  height: 100vh;
  background: #f0f7f4;
  overflow: hidden;
}

.floating-ball {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: #2d8a6e;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 32px rgba(45, 138, 110, 0.35);
  animation: float 3s ease-in-out infinite;
  cursor: pointer;
}

.floating-ball:hover {
  box-shadow: 0 16px 40px rgba(45, 138, 110, 0.45);
}

.camera-icon {
  width: 64px;
  height: 64px;
  color: #fff;
}

@keyframes float {
  0%, 100% {
    transform: translate(-50%, -50%) translateY(0);
  }
  50% {
    transform: translate(-50%, -50%) translateY(-15px);
  }
}
</style>
