<script setup lang="ts">
import { ref } from 'vue'
import { proxyImageUrl } from '../config'

interface LandmarkData {
  id?: string | number
  name: string
  tags: string[]
  imgs: string[]
}

const props = defineProps<{
  landmark: LandmarkData
}>()

const emit = defineEmits<{
  close: []
}>()

const toast = ref('')

function getShareUrl() {
  const id = props.landmark.id ?? ''
  return `${window.location.origin}${window.location.pathname}?landmarkId=${id}`
}

async function handleCopyLink() {
  const url = getShareUrl()
  try {
    await navigator.clipboard.writeText(url)
    toast.value = '链接已复制'
  } catch {
    const ta = document.createElement('textarea')
    ta.value = url
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    toast.value = '链接已复制'
  }
  setTimeout(() => { toast.value = '' }, 2000)
}

async function handleShare() {
  const url = getShareUrl()
  const shareData: ShareData = {
    title: props.landmark.name,
    text: `推荐你看看「${props.landmark.name}」- EchoCampus`,
    url,
  }
  if (navigator.share) {
    try {
      await navigator.share(shareData)
    } catch (err: any) {
      if (err.name !== 'AbortError') {
        handleCopyLink()
      }
    }
  } else {
    handleCopyLink()
  }
}
</script>

<template>
  <div class="share-overlay" @click.self="emit('close')">
    <div class="share-card">
      <button class="close-btn" @click="emit('close')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>

      <div class="card-image">
        <img
          :src="proxyImageUrl(landmark.imgs?.[0])"
          :alt="landmark.name"
        />
      </div>

      <div class="card-body">
        <h3 class="card-name">{{ landmark.name }}</h3>
        <div class="card-tags">
          <span v-for="tag in landmark.tags" :key="tag" class="card-tag">{{ tag }}</span>
        </div>

        <p class="share-divider">—— 分享到 ——</p>

        <div class="share-actions">
          <button class="share-btn" @click="handleShare">
            <span class="share-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="18" cy="5" r="3" />
                <circle cx="6" cy="12" r="3" />
                <circle cx="18" cy="19" r="3" />
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
              </svg>
            </span>
            <span class="share-label">分享到...</span>
          </button>
          <button class="share-btn" @click="handleCopyLink">
            <span class="share-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71" />
                <path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71" />
              </svg>
            </span>
            <span class="share-label">复制链接</span>
          </button>
        </div>
      </div>
    </div>

    <Transition name="toast-fade">
      <div v-if="toast" class="share-toast">{{ toast }}</div>
    </Transition>
  </div>
</template>

<style scoped>
.share-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.share-card {
  width: 70%;
  max-width: 320px;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  animation: card-enter 0.3s ease;
}

@keyframes card-enter {
  from {
    opacity: 0;
    transform: scale(0.85) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.close-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.35);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  backdrop-filter: blur(4px);
}

.close-btn svg {
  width: 16px;
  height: 16px;
}

.card-image {
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #eee;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-body {
  padding: 16px 16px 20px;
  text-align: center;
}

.card-name {
  font-size: 17px;
  font-weight: 700;
  color: #1f1f1f;
  margin-bottom: 6px;
  letter-spacing: 0.5px;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
  margin-bottom: 12px;
}

.card-tag {
  font-size: 11px;
  color: #3ca07a;
  background: #edf4f1;
  padding: 2px 10px;
  border-radius: 999px;
  line-height: 1.6;
}

.share-divider {
  font-size: 13px;
  color: #b0b0b0;
  margin-bottom: 14px;
  font-family: 'Georgia', 'Times New Roman', serif;
  font-style: italic;
  letter-spacing: 2px;
}

.share-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.share-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 10px;
  transition: background 0.2s;
}

.share-btn:hover {
  background: #f2f0eb;
}

.share-btn:active {
  background: #e8e6e0;
}

.share-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f0eb;
  color: #7a7a7a;
}

.share-icon svg {
  width: 22px;
  height: 22px;
}

.share-label {
  font-size: 11px;
  color: #7a7a7a;
}

.share-toast {
  position: fixed;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 14px;
  white-space: nowrap;
  z-index: 310;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.3s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
}
</style>
