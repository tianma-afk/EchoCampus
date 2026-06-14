<script setup lang="ts">
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
</script>

<template>
  <div class="home">
    <div class="welcome-card">
      <h1 class="welcome-title">欢迎你，{{ auth.username }}</h1>
      <p class="welcome-subtitle">映像校园管理后台</p>
    </div>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.welcome-card {
  text-align: center;
}

.welcome-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px 0;
}

.welcome-subtitle {
  font-size: 15px;
  color: #9ca3af;
  margin: 0;
}
</style>
