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

const isMobile = /android|iphone|ipad|ipod|mobile/i.test(navigator.userAgent)

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

async function handleShareTo(target: string) {
  const url = getShareUrl()
  const shareData = {
    title: props.landmark.name,
    text: `推荐你看看「${props.landmark.name}」- EchoCampus`,
    url,
  }
  if (navigator.share && isMobile) {
    try {
      await navigator.share(shareData)
    } catch (err: any) {
      if (err.name !== 'AbortError') {
        fallbackCopy(url, target)
      }
    }
  } else {
    fallbackCopy(url, target)
  }
}

async function fallbackCopy(url: string, target: string) {
  try {
    await navigator.clipboard.writeText(url)
  } catch {
    const ta = document.createElement('textarea')
    ta.value = url
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  }
  const names: Record<string, string> = { wechat: '微信', qq: 'QQ' }
  toast.value = `链接已复制，请打开${names[target] || target}粘贴分享`
  setTimeout(() => { toast.value = '' }, 3000)
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

        <p class="share-divider">—— 您将分享至 ——</p>

        <div class="share-actions">
          <button class="share-btn" @click="handleCopyLink">
            <span class="share-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71" />
                <path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71" />
              </svg>
            </span>
            <span class="share-label">复制链接</span>
          </button>
          <button class="share-btn" @click="handleShareTo('wechat')">
            <span class="share-icon wechat">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M8.5 11a1 1 0 100-2 1 1 0 000 2zm3.5 0a1 1 0 100-2 1 1 0 000 2zm-3.5 3a1 1 0 100-2 1 1 0 000 2zm3.5 0a1 1 0 100-2 1 1 0 000 2z"/>
                <path d="M17.735 2.12C13.626.447 8.914.63 5.09 2.82 1.267 5.01-.956 9.317.385 13.51c.64 1.987 1.918 3.62 3.572 4.82v3.74a.67.67 0 001.014.56l3.448-2.068a11.19 11.19 0 002.288.258c2.083 0 4.08-.571 5.814-1.62 2.952-1.786 4.817-4.83 5.11-8.27.296-3.44-1.06-6.76-3.896-9.01zM8.5 13.5a1.5 1.5 0 110-3 1.5 1.5 0 010 3zm3.5 0a1.5 1.5 0 110-3 1.5 1.5 0 010 3zm3.5-1.5a1.5 1.5 0 110-3 1.5 1.5 0 010 3zm3.5 0a1.5 1.5 0 110-3 1.5 1.5 0 010 3z"/>
              </svg>
            </span>
            <span class="share-label">微信</span>
          </button>
          <button class="share-btn" @click="handleShareTo('qq')">
            <span class="share-icon qq">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M22.914 10.724c-.162-.984-.666-1.524-1.236-1.748.168-.552.252-1.362.252-2.064 0-4.542-2.448-6.912-5.472-6.912-1.224 0-2.376.48-3.264 1.344a6.768 6.768 0 00-2.652-.552c-3.456 0-6.156 2.88-6.156 6.42 0 .738.108 1.482.3 2.154-.54.258-1.008.804-1.14 1.752-.18 1.284.63 2.34 1.854 2.532.024 0 .048.006.072.012-.024.108-.036.222-.036.342 0 1.218.528 2.31 1.434 3.18.828.798 1.956 1.356 3.27 1.596.384.066.78.102 1.182.102.342 0 .678-.024 1.008-.072-.096.33-.168.702-.168 1.104 0 1.614.948 2.652 2.112 2.652 1.164 0 2.112-1.038 2.112-2.652 0-.402-.072-.774-.168-1.104.33.048.666.072 1.008.072.402 0 .798-.036 1.182-.102 1.314-.24 2.442-.798 3.27-1.596.906-.87 1.434-1.962 1.434-3.18 0-.12-.012-.234-.036-.342.024-.006.048-.012.072-.012 1.224-.192 2.034-1.248 1.854-2.532z"/>
              </svg>
            </span>
            <span class="share-label">QQ</span>
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

.share-icon.wechat {
  background: #e8f5e9;
  color: #07c160;
}

.share-icon.qq {
  background: #e3edff;
  color: #12b7f5;
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
