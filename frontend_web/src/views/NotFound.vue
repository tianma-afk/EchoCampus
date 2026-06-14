<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <h1 class="code">404</h1>
    <p class="message">页面不存在</p>
    <el-button type="primary" @click="router.replace('/home')">返回主页</el-button>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
}

.code {
  font-size: 64px;
  font-weight: 700;
  color: #d1d5db;
  margin: 0;
}

.message {
  font-size: 16px;
  color: #9ca3af;
  margin: 0 0 16px 0;
}
</style>
