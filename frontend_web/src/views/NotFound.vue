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
  gap: 12px;
}

.code {
  font-size: 72px;
  font-weight: 800;
  color: #e5e7eb;
  margin: 0;
  letter-spacing: -0.03em;
  line-height: 1;
}

.message {
  font-size: 16px;
  color: #6b7280;
  margin: 0 0 20px 0;
  font-weight: 500;
}
</style>
