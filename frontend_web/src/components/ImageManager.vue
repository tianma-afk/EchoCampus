<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  listImages,
  presignUpload,
  confirmUpload,
  setCover,
  setCuratedImages,
  deleteImage,
  type LandmarkImageVO,
} from '../api/image'

const props = defineProps<{
  landmarkId: string
}>()

const images = ref<LandmarkImageVO[]>([])
const loading = ref(true)
const uploading = ref(false)
const error = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

onMounted(() => {
  loadImages()
})

async function loadImages() {
  loading.value = true
  try {
    const res = await listImages(props.landmarkId)
    images.value = res.data ?? []
  } catch {
    images.value = []
  } finally {
    loading.value = false
  }
}

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploading.value = true
  error.value = ''

  try {
    const presignRes = await presignUpload(props.landmarkId, file.name)
    const { key, presignedUrl } = presignRes.data
    console.log('[ImageManager] presigned URL obtained, key:', key)

    const uploadRes = await fetch(presignedUrl, {
      method: 'PUT',
      body: file,
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
    })
    if (!uploadRes.ok) {
      const errorText = await uploadRes.text().catch(() => '')
      console.error('[ImageManager] MinIO PUT failed:', uploadRes.status, errorText)
      throw new Error('上传到存储失败（HTTP ' + uploadRes.status + '），请确认图片服务可用')
    }
    console.log('[ImageManager] MinIO PUT success')

    const confirmRes = await confirmUpload(props.landmarkId, key)
    console.log('[ImageManager] confirmUpload response:', confirmRes)

    await loadImages()
    console.log('[ImageManager] images reloaded, count:', images.value.length)
  } catch (e) {
    console.error('[ImageManager] upload error:', e)
    error.value = e instanceof Error ? e.message : '上传失败'
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function handleSetCover(imageId: string) {
  try {
    await setCover(props.landmarkId, imageId)
    await loadImages()
  } catch {
    error.value = '设置封面失败'
  }
}

async function handleToggleCurated(image: LandmarkImageVO) {
  try {
    const curatedIds = images.value.filter((img) => img.isCurated).map((img) => img.id)
    let newIds: string[]
    if (image.isCurated) {
      newIds = curatedIds.filter((id) => id !== image.id)
    } else {
      if (curatedIds.length >= 6) {
        error.value = '精选图片最多 6 张'
        return
      }
      newIds = [...curatedIds, image.id]
    }
    await setCuratedImages(props.landmarkId, newIds)
    await loadImages()
  } catch {
    error.value = '设置精选失败'
  }
}

async function handleDelete(imageId: string) {
  if (!confirm('确定删除这张图片吗？')) return
  try {
    await deleteImage(props.landmarkId, imageId)
    await loadImages()
  } catch {
    error.value = '删除失败'
  }
}

const curatedOrder = (img: LandmarkImageVO) => {
  if (!img.isCurated) return 0
  const list = images.value.filter((i) => i.isCurated)
  return list.findIndex((i) => i.id === img.id) + 1
}
</script>

<template>
  <div class="image-manager">
    <div class="image-header">
      <h3 class="section-title">图片管理</h3>
      <div class="header-actions">
        <button type="button" class="upload-btn" :disabled="uploading" @click="triggerUpload">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          {{ uploading ? '上传中...' : '上传图片' }}
        </button>
        <input ref="fileInput" type="file" accept="image/*" class="sr-only" @change="handleFileSelected" />
      </div>
    </div>

    <div v-if="error" class="error-msg">{{ error }}</div>

    <div v-if="loading" class="state-msg">加载图片中...</div>

    <div v-else-if="images.length === 0" class="empty-msg">暂无图片，请上传</div>

    <div v-else class="image-grid">
      <div v-for="img in images" :key="img.id" class="image-card" :class="{ cover: img.isCover }">
        <div class="image-thumb">
          <img :src="img.url" :alt="'图片 ' + img.id.slice(0, 8)" />
          <span v-if="img.isCover" class="badge cover-badge">封面</span>
          <span v-if="img.isCurated" class="badge curated-badge">精选 {{ curatedOrder(img) }}</span>
        </div>
        <div class="image-actions">
          <button
            type="button"
            class="img-btn cover-btn"
            :class="{ active: img.isCover }"
            @click="handleSetCover(img.id)"
          >
            设为封面
          </button>
          <button
            type="button"
            class="img-btn curated-btn"
            :class="{ active: img.isCurated }"
            @click="handleToggleCurated(img)"
          >
            {{ img.isCurated ? '取消精选' : '设为精选' }}
          </button>
          <button type="button" class="img-btn delete-btn" @click="handleDelete(img.id)">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.image-manager {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.image-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.upload-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #059669;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-btn:hover:not(:disabled) {
  background: #047857;
}

.upload-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.upload-btn svg {
  width: 16px;
  height: 16px;
}

.error-msg {
  padding: 8px 12px;
  background: #fef2f2;
  border-radius: 8px;
  color: #ef4444;
  font-size: 13px;
  margin-bottom: 12px;
}

.state-msg,
.empty-msg {
  text-align: center;
  padding: 32px 0;
  color: #9ca3af;
  font-size: 14px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.image-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.2s;
}

.image-card.cover {
  border-color: #10b981;
}

.image-thumb {
  position: relative;
  aspect-ratio: 4 / 3;
  background: #f9fafb;
  overflow: hidden;
}

.image-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.badge {
  position: absolute;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
}

.cover-badge {
  top: 6px;
  left: 6px;
  background: #10b981;
}

.curated-badge {
  top: 6px;
  right: 6px;
  background: #8b5cf6;
}

.image-actions {
  display: flex;
  gap: 4px;
  padding: 8px;
}

.img-btn {
  flex: 1;
  padding: 6px 4px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  font-size: 11px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}

.img-btn:hover {
  border-color: #10b981;
  color: #10b981;
}

.img-btn.active {
  background: #ecfdf5;
  border-color: #10b981;
  color: #059669;
}

.img-btn.curated-btn.active {
  background: #f5f3ff;
  border-color: #8b5cf6;
  color: #7c3aed;
}

.delete-btn {
  color: #ef4444;
}

.delete-btn:hover {
  background: #fef2f2;
  border-color: #ef4444;
  color: #ef4444;
}
</style>
