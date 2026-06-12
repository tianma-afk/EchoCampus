<script setup lang="ts">
import { ref, watch, onUnmounted, nextTick } from 'vue'
import axios from 'axios'
import FeedbackPage from './FeedbackPage.vue'

const emit = defineEmits<{
  openDetail: [landmarkId: string]
}>()

const showCamera = ref(false)
const videoElement = ref<HTMLVideoElement | null>(null)
const stream = ref<MediaStream | null>(null)
const facingMode = ref<'user' | 'environment'>('environment')
const fileInput = ref<HTMLInputElement | null>(null)

const API_BASE_URL = 'http://localhost:8080/api/v1'
const UPLOAD_API = `${API_BASE_URL}/upload`
const SEARCH_API = `${API_BASE_URL}/user/algorithm/search`
const MINIO_BASE_URL = 'http://localhost:9000'

const uploading = ref(false)
const showPreview = ref(false)
const previewImageUrl = ref('')
const resultShowImageUrl = ref('')

const isRecognizing = ref(false)
const recognizingTaskId = ref('')
const recognitionResults = ref<Array<{landmarkId: string, landmarkName: string, similarity: number, coverUrl: string}>>([])
const showResult = ref(false)
const showFeedback = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

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

const handleImageSelected = async (file: File | Blob) => {
  uploading.value = true
  showCamera.value = false
  try {
    const res = await axios.post(`${UPLOAD_API}/presigned-url`)
    const { uploadUrl, bucket, objectName } = res.data.data

    await new Promise<void>((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      xhr.open('PUT', uploadUrl)
      xhr.setRequestHeader('Content-Type', 'image/jpeg')
      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve()
        } else {
          reject(new Error('MinIO PUT failed (HTTP ' + xhr.status + ')'))
        }
      }
      xhr.onerror = () => reject(new Error('网络错误'))
      xhr.send(file)
    })

    previewImageUrl.value = `${MINIO_BASE_URL}/${bucket}/${objectName}`
    showPreview.value = true
  } catch (error) {
    console.error('上传图片失败:', error)
    alert('上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

const handleConfirm = async () => {
  showPreview.value = false
  resultShowImageUrl.value = previewImageUrl.value
  try {
    const imageUrl = previewImageUrl.value
    const res = await axios.post(SEARCH_API, { imageUrl })
    const taskId = res.data.data

    recognizingTaskId.value = taskId
    isRecognizing.value = true
    startPolling(taskId)
  } catch (error) {
    console.error('创建识别任务失败:', error)
    alert('创建识别任务失败，请重试')
    previewImageUrl.value = ''
  }
}

const startPolling = (taskId: string) => {
  let attempts = 0

  pollTimer = setInterval(async () => {
    attempts++
    try {
      const res = await axios.get(`${SEARCH_API}/${taskId}/result`)
      const code =res.data.code
      const results = res.data.data

      if (code === '00000') {
        if (results && results.length > 0) {
                clearInterval(pollTimer!)
                pollTimer = null
                recognitionResults.value = results
                isRecognizing.value = false
                showResult.value = true
              } else if (attempts >= 30) {
                clearInterval(pollTimer!)
                pollTimer = null
                isRecognizing.value = false
                alert('识别超时，请重试')
              }
      }else{
        clearInterval(pollTimer!)
        pollTimer = null
        isRecognizing.value = false
        alert(res.data.message)
      }


    } catch {
      if (attempts >= 30) {
        clearInterval(pollTimer!)
        pollTimer = null
        isRecognizing.value = false
        alert('识别超时，请重试')
      }
    }
  }, 2000)
}

const topResult = ref<{landmarkId: string, landmarkName: string, similarity: number, coverUrl: string} | null>(null)

watch(recognitionResults, (list) => {
  if (!list.length) {
    topResult.value = null
    return
  }
  const sorted = [...list].sort((a, b) => b.similarity - a.similarity)
  topResult.value = sorted[0]
}, { immediate: true })

const formatPercent = (num: number) => Math.round(num * 100)

const handleCardClick = (item: {landmarkId: string, landmarkName: string, similarity: number, coverUrl: string}) => {
  emit('openDetail', item.landmarkId)
}

const onCoverImgError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120"><rect fill="%23e8e8e8" width="120" height="120"/><text x="60" y="60" text-anchor="middle" dominant-baseline="central" font-size="40" fill="%23bbb">?</text></svg>'
}

const visibleCards = ref(new Set<number>())
let cardObserver: IntersectionObserver | null = null

const setupCardObserver = () => {
  const root = document.querySelector('.card-list')
  if (!root) return

  cardObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      const idx = Number((entry.target as HTMLElement).dataset.index)
      const next = new Set(visibleCards.value)
      if (entry.isIntersecting) {
        next.add(idx)
      } else {
        next.delete(idx)
      }
      visibleCards.value = next
    })
  }, { threshold: 0.7, root })

  document.querySelectorAll('.landmark-card').forEach((card, i) => {
    (card as HTMLElement).dataset.index = String(i)
    cardObserver!.observe(card)
  })
}

watch(showResult, (val) => {
  if (val) {
    nextTick(() => setupCardObserver())
  } else {
    cardObserver?.disconnect()
    cardObserver = null
    visibleCards.value.clear()
  }
})

onUnmounted(() => {
  stopCamera()
  cardObserver?.disconnect()
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <div class="campus-scan-page">
    <!-- 背景层 -->
    <div class="bg-container"></div>

    <!-- 主页面 -->
    <div v-if="!showCamera && !showPreview && !uploading && !isRecognizing && !showResult" class="main-page">
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

    <!-- 上传中 -->
    <div v-if="uploading" class="uploading-overlay">
      <div class="uploading-spinner">
        <div class="spinner-icon"></div>
        <p class="uploading-text">正在上传图片...</p>
      </div>
    </div>

    <!-- 等待识别中 -->
    <div v-if="isRecognizing" class="uploading-overlay">
      <div class="uploading-spinner">
        <div class="spinner-icon"></div>
        <p class="uploading-text">等待识别中...</p>
        <p class="task-id-text">TaskId: {{ recognizingTaskId }}</p>
      </div>
    </div>

    <!-- 图片预览 -->
    <div v-if="showPreview" class="preview-view">
      <div class="preview-image-wrapper">
        <img :src="previewImageUrl" class="preview-image" />
      </div>
      <footer class="preview-footer">
        <button class="confirm-btn" @click="handleConfirm">开始识别</button>
      </footer>
    </div>

    <!-- 识别结果 -->
    <div v-if="showResult" class="result-view">
      <div class="result-top-img">
        <img :src="resultShowImageUrl" alt="拍摄图片" class="user-capture-img" />
        <div class="result-img-overlay"></div>
        <button class="result-close-btn" @click="showResult = false">
          <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>

      <div class="result-text-desc">
        <h2 v-if="!topResult" class="desc-title no-match">未能识别到建筑</h2>
        <h2 v-else-if="formatPercent(topResult.similarity) < 30" class="desc-title">这有点难倒我了</h2>
        <h2 v-else class="desc-title">识别完成</h2>
        <p v-if="topResult" class="desc-sub">
          它可能是{{ topResult.landmarkName }}（相似度{{ formatPercent(topResult.similarity) }}%）
        </p>
        <p v-else class="desc-sub empty-sub">请换个角度再试一次</p>
      </div>

      <div v-if="recognitionResults.length > 0" class="result-card-scroll">
        <div class="card-list">
          <div class="landmark-card" v-for="(item, index) in recognitionResults" :key="item.coverUrl"
               :class="{ 'in-view': visibleCards.has(index), 'card-first': index === 0, 'card-last': index === recognitionResults.length - 1 }"
               @click="handleCardClick(item)">
            <div class="circle-img-box">
              <img :src="item.coverUrl" alt="建筑封面" class="circle-img" @error="onCoverImgError" />
            </div>
            <div class="card-text">
              <p class="land-name">{{ item.landmarkName }}</p>
              <p class="land-score">{{ formatPercent(item.similarity) }}%</p>
            </div>
          </div>
        </div>
      </div>

      <div class="result-bottom-btn">
        <button class="back-btn" @click="showResult = false">重新拍摄</button>
        <button v-if="topResult" class="feedback-btn" @click="showFeedback = true">纠正反馈</button>
      </div>
    </div>

    <FeedbackPage
      v-if="showFeedback && topResult"
      :landmark-id="topResult.landmarkId"
      :landmark-name="topResult.landmarkName"
      :image-url="resultShowImageUrl"
      @done="showFeedback = false"
      @back="showFeedback = false"
    />

    <!-- 拍摄界面 -->
    <div v-else-if="showCamera" class="camera-interface">
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
  background-color: var(--color-bg);
}

.bg-container {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 10% 90%, rgba(180, 210, 200, 0.25) 0%, transparent 55%),
    radial-gradient(circle at 90% 10%, rgba(180, 210, 200, 0.25) 0%, transparent 55%);
  z-index: 0;
}

.main-page {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

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

.camera-btn {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: var(--color-primary);
  border: none;
  box-shadow: 0 10px 30px var(--color-primary-shadow);
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
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: all 0.3s ease;
}

.gallery-btn .btn-icon {
  width: 20px;
  height: 20px;
  color: var(--color-primary);
  margin-right: 10px;
}

.gallery-btn span {
  font-size: 17px;
  font-weight: 500;
  color: var(--color-primary);
}

.gallery-btn:hover {
  background: #ffffff;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.camera-interface {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #000;
  z-index: 10;
}

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

.gallery-control {
  opacity: 0.9;
}

.gallery-control:hover {
  opacity: 1;
}

.capture-btn {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--color-primary);
  border: 4px solid rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px var(--color-primary-shadow);
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

.switch-btn {
  opacity: 0.9;
}

.switch-btn:hover {
  opacity: 1;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-30px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.uploading-overlay {
  position: absolute;
  inset: 0;
  z-index: 10;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}

.uploading-spinner {
  text-align: center;
}

.spinner-icon {
  width: 50px;
  height: 50px;
  margin: 0 auto 20px;
  border: 4px solid rgba(255, 255, 255, 0.2);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.uploading-text {
  font-size: 17px;
  color: #fff;
  font-weight: 500;
}

.task-id-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 12px;
  word-break: break-all;
  padding: 0 40px;
  user-select: all;
  cursor: text;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.preview-view {
  position: relative;
  z-index: 10;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #000;
  padding-bottom: env(safe-area-inset-bottom);
}

.preview-image-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: min(100%, 600px);
  margin: 0 auto;
  padding: 20px;
  background: #000;
  min-height: 0;
}

.preview-image {
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-footer {
  flex-shrink: 0;
  z-index: 2;
  width: 100%;
  background: linear-gradient(to bottom, #1a1a1a, #000);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 20px calc(80px + env(safe-area-inset-bottom)) 20px;
  min-height: 120px;
}

.confirm-btn {
  width: 200px;
  height: 50px;
  border-radius: 50px;
  background: var(--color-primary);
  border: none;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 16px var(--color-primary-shadow);
  transition: all 0.2s ease;
}

.confirm-btn:active {
  transform: scale(0.95);
}

.result-view {
  position: relative;
  z-index: 10;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-card);
  overflow: hidden;
  animation: resultFadeIn 0.4s ease-out;
}

@keyframes resultFadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.result-top-img {
  position: relative;
  width: 100%;
  height: 40vh;
  flex-shrink: 0;
  background: radial-gradient(ellipse at center, rgba(60,160,122,0.12) 0%, transparent 60%),
              #1a1a1a;
}

.user-capture-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.result-img-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 80px;
  background: linear-gradient(to top, rgba(0,0,0,0.4), transparent);
}

.result-close-btn {
  position: absolute;
  top: 16px;
  left: 16px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(0,0,0,0.35);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  backdrop-filter: blur(4px);
}

.result-text-desc {
  padding: 8px 24px 0;
  text-align: center;
}

.desc-title {
  font-size: 22px;
  color: var(--color-text-heading);
  font-weight: 500;
  margin-bottom: 6px;
  animation: slideUpText 0.5s 0.1s ease-out both;
}

@keyframes slideUpText {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.desc-title.no-match {
  color: var(--color-text-secondary);
}

.desc-sub {
  font-size: 14px;
  color: var(--color-text);
  animation: slideUpText 0.5s 0.2s ease-out both;
}

.desc-sub.empty-sub {
  color: var(--color-text-muted);
}

.result-card-scroll {
  flex: 1;
  overflow: hidden;
  padding: 10px 0 20px;
  min-height: 0;
}

.card-list {
  display: flex;
  gap: calc((100vw - 70vw) / 2 - 25px);
  padding: 0;
  overflow-x: auto;
  overflow-y: visible;
  height: 100%;
  align-items: flex-start;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
}

.card-list::-webkit-scrollbar {
  display: none;
}

.landmark-card {
  flex-shrink: 0;
  width: 70vw;
  max-width: 360px;
  text-align: center;
  scroll-snap-align: center;
  opacity: 0.5;
  transform: scale(0.85);
  transition: all 0.35s ease;
  cursor: default;
  padding-top: 5px;
}

.landmark-card.card-first {
  margin-left: calc((100vw - 70vw) / 2);
}

.landmark-card.card-last {
  margin-right: calc((100vw - 70vw) / 2);
}

.landmark-card.in-view {
  opacity: 1;
  transform: scale(1);
}

.landmark-card:active {
  transform: scale(0.95);
}

.circle-img-box {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 14px rgba(0,0,0,0.12);
  margin: 0 auto 8px;
}

.circle-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.land-name {
  font-size: 15px;
  color: var(--color-text-heading);
  font-weight: 500;
  margin-bottom: 3px;
}

.land-score {
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 600;
}

.result-bottom-btn {
  padding: 12px 30px calc(80px + env(safe-area-inset-bottom));
  flex-shrink: 0;
}

.back-btn {
  width: 100%;
  height: 52px;
  border-radius: 52px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  font-size: 18px;
  font-weight: 600;
  box-shadow: 0 5px 18px var(--color-primary-shadow);
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:active {
  transform: scale(0.97);
}

.feedback-btn {
  width: 100%;
  height: 52px;
  border-radius: 52px;
  background: #fff;
  color: var(--color-primary);
  border: 2px solid var(--color-primary);
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 12px;
}

.feedback-btn:active {
  transform: scale(0.97);
  background: #f0fdf4;
}
</style>