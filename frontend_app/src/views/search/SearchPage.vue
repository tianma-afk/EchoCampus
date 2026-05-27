<script setup lang="ts">
import { ref } from 'vue'
const fileInput = ref<HTMLInputElement | null>(null)

const handleCameraClick = () => {
  console.log('打开相机')
  if (fileInput.value) {
    fileInput.value.capture = 'environment'
    fileInput.value.click()
  }
}

const handleGalleryClick = () => {
  console.log('打开相册')
  if (fileInput.value) {
    fileInput.value.capture = ''
    fileInput.value.click()
  }
}

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) console.log('选中图片：', file)
}
</script>

<template>
  <div class="campus-scan-page">
    <!-- 背景层 -->
    <div class="bg-container"></div>

    <!-- 顶部标题区 -->
    <header class="page-header">
      <h1 class="title">映像校园</h1>
      <p class="subtitle">发现身边的建筑与风景</p>
    </header>

    <!-- 中间核心操作区 -->
    <main class="main-content">
      <div class="camera-container">
        <button class="camera-btn" @click="handleCameraClick">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
            <circle cx="12" cy="13" r="4"></circle>
          </svg>
        </button>
      </div>
    </main>

    <!-- 底部操作区 -->
    <footer class="page-footer">
      <button class="gallery-btn" @click="handleGalleryClick">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="btn-icon">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
          <circle cx="8.5" cy="8.5" r="1.5"></circle>
          <polyline points="21 15 16 10 5 21"></polyline>
        </svg>
        <span>从相册选择</span>
      </button>
    </footer>

    <!-- 隐藏文件输入框 -->
    <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display: none"
        @change="handleFileChange"
    />
  </div>
</template>

<style scoped>
/* 基础重置 */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  -webkit-tap-highlight-color: transparent;
}

.campus-scan-page {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: #f0f9f5;
}

/* 👇 完全匹配你给的背景效果 */
.bg-container {
  position: absolute;
  inset: 0;
  background:
      radial-gradient(circle at 10% 90%, rgba(170, 200, 230, 0.35) 0%, transparent 55%),
      radial-gradient(circle at 90% 10%, rgba(170, 230, 220, 0.35) 0%, transparent 55%);
  z-index: 0;
}

/* 顶部标题 */
.page-header {
  z-index: 2;
  margin-top: 120px;
  text-align: center;
  animation: slideDown 0.8s ease-out;
}

.title {
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue", sans-serif;
  font-size: 38px;
  font-weight: 600;
  color: #1a365d;
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.subtitle {
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue", sans-serif;
  font-size: 16px;
  font-weight: 400;
  color: #4a7c9b;
  letter-spacing: 0.5px;
}

/* 中间拍照区域 */
.main-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
  width: 100%;
}

.camera-container {
  position: relative;
  /* 上下浮动动画 */
  animation: float 3s ease-in-out infinite;
}

/* 拍照按钮 */
.camera-btn {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: #169669;
  border: none;
  box-shadow: 0 10px 30px rgba(22, 150, 105, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.camera-btn svg {
  width: 70px;
  height: 70px;
  color: #ffffff;
}

.camera-btn:active {
  transform: scale(0.95);
}

/* 底部相册按钮 */
.page-footer {
  z-index: 2;
  width: 100%;
  padding: 0 30px 100px;
  display: flex;
  justify-content: center;
  animation: slideUp 0.8s ease-out;
}

.gallery-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  border-radius: 50px;
  padding: 14px 40px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s ease;
}

.gallery-btn .btn-icon {
  width: 20px;
  height: 20px;
  color: #169669;
  margin-right: 10px;
}

.gallery-btn span {
  font-size: 17px;
  font-weight: 500;
  color: #169669;
}

.gallery-btn:hover {
  background: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

/* 动画定义 */
@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-15px);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>