<script setup lang="ts">
import { ref, onUnmounted } from 'vue'

const showCamera = ref(false)
const videoElement = ref<HTMLVideoElement | null>(null)
const stream = ref<MediaStream | null>(null)
const facingMode = ref<'user' | 'environment'>('environment')
const fileInput = ref<HTMLInputElement | null>(null)

const handleCameraClick = () => {
  showCamera.value = true
  startCamera()
}

const startCamera = async () => {
  try {
    if (stream.value) {
      stream.value.getTracks().forEach(track => track.stop())
    }
    
    const mediaStream = await navigator.mediaDevices.getUserMedia({
      video: {
        facingMode: facingMode.value,
        width: { ideal: 1920 },
        height: { ideal: 1080 }
      },
      audio: false
    })
    
    stream.value = mediaStream
    
    if (videoElement.value) {
      videoElement.value.srcObject = mediaStream
    }
  } catch (error) {
    console.error('无法访问摄像头:', error)
    alert('无法访问摄像头，请确保已授予权限')
  }
}

const stopCamera = () => {
  if (stream.value) {
    stream.value.getTracks().forEach(track => track.stop())
    stream.value = null
  }
  showCamera.value = false
}

const switchCamera = () => {
  facingMode.value = facingMode.value === 'user' ? 'environment' : 'user'
  startCamera()
}

const takePhoto = () => {
  if (!videoElement.value) return
  
  const canvas = document.createElement('canvas')
  canvas.width = videoElement.value.videoWidth
  canvas.height = videoElement.value.videoHeight
  
  const ctx = canvas.getContext('2d')
  if (ctx) {
    ctx.drawImage(videoElement.value, 0, 0)
    canvas.toBlob((blob) => {
      if (blob) {
        console.log('拍摄的照片:', blob)
        handleImageSelected(blob)
      }
    }, 'image/jpeg', 0.9)
  }
  
  stopCamera()
}

const handleGalleryClick = () => {
  if (fileInput.value) {
    fileInput.value.click()
  }
}

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    console.log('选中图片：', file)
    handleImageSelected(file)
  }
}

const handleImageSelected = (file: File | Blob) => {
  console.log('处理图片:', file)
  // TODO: 这里可以添加上传到后端进行识别的逻辑
  alert('图片已选择，可以进行识别')
}

onUnmounted(() => {
  stopCamera()
})
</script>

<template>
  <div class="campus-scan-page">
    <!-- 背景层 -->
    <div class="bg-container"></div>

    <!-- 主页面 -->
    <div v-if="!showCamera" class="main-page">
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
    </div>

    <!-- 拍摄界面 -->
    <div v-else class="camera-interface">
      <!-- 3/4 摄像头预览区域 -->
      <div class="camera-preview">
        <video
          ref="videoElement"
          autoplay
          playsinline
          class="video-element"
        ></video>
        <div class="preview-overlay">
          <div class="focus-frame"></div>
        </div>
        <button class="close-btn" @click="stopCamera">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>

      <!-- 1/4 控制按钮区域 -->
      <div class="camera-controls">
        <div class="control-buttons">
          <!-- 相册按钮 -->
          <button class="control-btn gallery-control" @click="handleGalleryClick">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
              <circle cx="8.5" cy="8.5" r="1.5"></circle>
              <polyline points="21 15 16 10 5 21"></polyline>
            </svg>
            <span>相册</span>
          </button>

          <!-- 拍照按钮（带白边的绿色圆形） -->
          <button class="control-btn capture-btn" @click="takePhoto">
            <div class="capture-inner"></div>
          </button>

          <!-- 相机翻转按钮 -->
          <button class="control-btn switch-btn" @click="switchCamera">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6"></path>
              <path d="M1 20v-6h6"></path>
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10"></path>
              <path d="M20.49 15a9 9 0 0 1-14.85 3.36L1 14"></path>
            </svg>
            <span>翻转</span>
          </button>
        </div>
      </div>
    </div>

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

/* ========== 主页面样式 ========== */
.main-page {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
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

/* ========== 拍摄界面样式 ========== */
.camera-interface {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #000;
  z-index: 10;
}

/* 摄像头预览区域 (3/4) */
.camera-preview {
  flex: 3;
  position: relative;
  overflow: hidden;
  background: #000;
}

.video-element {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.focus-frame {
  width: 280px;
  height: 280px;
  border: 2px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.3);
}

.close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 20;
}

.close-btn svg {
  width: 24px;
  height: 24px;
  color: #fff;
}

/* 控制按钮区域 (1/4) */
.camera-controls {
  flex: 1;
  background: linear-gradient(to bottom, #1a1a1a, #000);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.control-buttons {
  display: flex;
  align-items: center;
  justify-content: space-around;
  width: 100%;
  max-width: 500px;
  gap: 30px;
}

.control-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: none;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.control-btn svg {
  width: 28px;
  height: 28px;
  color: #fff;
}

.control-btn span {
  font-size: 13px;
  color: #fff;
  font-weight: 500;
}

.control-btn:active {
  transform: scale(0.9);
}

/* 相册按钮 */
.gallery-control {
  opacity: 0.9;
}

.gallery-control:hover {
  opacity: 1;
}

/* 拍照按钮（带白边的绿色圆形） */
.capture-btn {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #169669;
  border: 4px solid rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(22, 150, 105, 0.5);
}

.capture-inner {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #fff;
  transition: all 0.2s ease;
}

.capture-btn:active .capture-inner {
  width: 56px;
  height: 56px;
}

/* 翻转按钮 */
.switch-btn {
  opacity: 0.9;
}

.switch-btn:hover {
  opacity: 1;
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