<script setup lang="ts">
import { ref, watch } from 'vue'
import { presignUpload, confirmUpload } from '../api/image'

const props = defineProps<{
  visible: boolean
  landmarkId: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  uploaded: []
}>()

interface UploadItem {
  file: File
  status: 'pending' | 'uploading' | 'done' | 'error'
  progress: number
  errorMsg: string
}

const uploads = ref<UploadItem[]>([])
const isDragging = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

watch(
  () => props.visible,
  (v) => {
    if (v) {
      uploads.value = []
    }
  },
)

function triggerFileInput() {
  fileInput.value?.click()
}

function addFiles(files: FileList | File[]) {
  for (const file of files) {
    if (!file.type.startsWith('image/')) continue
    uploads.value.push({
      file,
      status: 'pending',
      progress: 0,
      errorMsg: '',
    })
  }
}

function handleFileInput(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files) {
    addFiles(input.files)
    input.value = ''
  }
}

function handleDragOver(e: DragEvent) {
  e.preventDefault()
  isDragging.value = true
}

function handleDragLeave() {
  isDragging.value = false
}

function handleDrop(e: DragEvent) {
  e.preventDefault()
  isDragging.value = false
  if (e.dataTransfer?.files) {
    addFiles(e.dataTransfer.files)
  }
}

async function startUpload(item: UploadItem) {
  item.status = 'uploading'
  item.progress = 0
  item.errorMsg = ''

  try {
    const presignRes = await presignUpload(props.landmarkId, item.file.name)
    const { key, presignedUrl } = presignRes.data

    await new Promise<void>((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      xhr.open('PUT', presignedUrl)
      xhr.setRequestHeader('Content-Type', item.file.type || 'application/octet-stream')

      xhr.upload.onprogress = (ev) => {
        if (ev.lengthComputable) {
          item.progress = Math.round((ev.loaded / ev.total) * 100)
        }
      }

      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve()
        } else {
          reject(new Error('MinIO PUT failed (HTTP ' + xhr.status + ')'))
        }
      }

      xhr.onerror = () => reject(new Error('网络错误'))
      xhr.send(item.file)
    })

    await confirmUpload(props.landmarkId, key)
    item.status = 'done'
    item.progress = 100
  } catch (e) {
    item.status = 'error'
    item.errorMsg = e instanceof Error ? e.message : '上传失败'
  }
}

async function uploadAll() {
  for (const item of uploads.value) {
    if (item.status === 'pending' || item.status === 'error') {
      await startUpload(item)
    }
  }
}

const hasPending = () => uploads.value.some((u) => u.status === 'pending' || u.status === 'error')
const allDone = () =>
  uploads.value.length > 0 && uploads.value.every((u) => u.status === 'done')
const uploadCount = () =>
  uploads.value.filter((u) => u.status === 'uploading').length
const doneCount = () => uploads.value.filter((u) => u.status === 'done').length

function handleClose() {
  if (doneCount() > 0) {
    emit('uploaded')
  }
  emit('update:visible', false)
}

function handleOverlayClick(e: MouseEvent) {
  if ((e.target as HTMLElement).classList.contains('dialog-overlay')) {
    handleClose()
  }
}
</script>

<template>
  <div v-if="visible" class="dialog-overlay" @click="handleOverlayClick">
    <div class="upload-dialog">
      <div class="dialog-header">
        <h3>上传图片</h3>
        <button class="close-btn" @click="handleClose">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div class="dialog-body">
        <!-- Drop zone -->
        <div
          class="drop-zone"
          :class="{ dragging: isDragging }"
          @dragover="handleDragOver"
          @dragleave="handleDragLeave"
          @drop="handleDrop"
          @click="triggerFileInput"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          <p>拖拽图片到此处或点击选择文件</p>
          <span class="hint">支持 JPG、JPEG、PNG、WebP 格式</span>
        </div>
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          multiple
          class="sr-only"
          @change="handleFileInput"
        />

        <!-- Upload list -->
        <div v-if="uploads.length > 0" class="upload-list">
          <div
            v-for="(item, idx) in uploads"
            :key="idx"
            class="upload-item"
            :class="item.status"
          >
            <div class="upload-info">
              <span class="upload-name">{{ item.file.name }}</span>
              <span class="upload-size">{{ (item.file.size / 1024).toFixed(0) }} KB</span>
            </div>
            <div class="upload-bar-wrap">
              <div
                class="upload-bar"
                :class="item.status"
                :style="{ width: item.progress + '%' }"
              ></div>
            </div>
            <span v-if="item.status === 'pending'" class="upload-tag pending">等待上传</span>
            <span v-else-if="item.status === 'uploading'" class="upload-tag uploading">
              {{ item.progress }}%
            </span>
            <span v-else-if="item.status === 'done'" class="upload-tag done">完成</span>
            <span v-else-if="item.status === 'error'" class="upload-tag error" :title="item.errorMsg">
              失败
            </span>
          </div>
        </div>
      </div>

      <div class="dialog-footer">
        <span class="footer-info">
          共 {{ uploads.length }} 个文件，
          {{ doneCount() }} 完成
        </span>
        <button
          v-if="hasPending() && uploadCount() === 0"
          type="button"
          class="upload-all-btn"
          @click="uploadAll"
        >
          开始上传
        </button>
        <button type="button" class="cancel-btn" @click="handleClose">
          {{ allDone() ? '完成' : '关闭' }}
        </button>
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

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.upload-dialog {
  background: #fff;
  border-radius: 12px;
  width: 520px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f3f4f6;
}

.dialog-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  cursor: pointer;
  color: #9ca3af;
  border-radius: 6px;
}

.close-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.close-btn svg {
  width: 18px;
  height: 18px;
}

.dialog-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.drop-zone {
  border: 2px dashed #d1d5db;
  border-radius: 10px;
  padding: 36px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.drop-zone:hover,
.drop-zone.dragging {
  border-color: #10b981;
  background: #f0fdf6;
}

.drop-zone svg {
  width: 40px;
  height: 40px;
  color: #9ca3af;
  margin-bottom: 12px;
}

.drop-zone.dragging svg {
  color: #10b981;
}

.drop-zone p {
  margin: 0 0 4px;
  color: #6b7280;
  font-size: 14px;
}

.drop-zone .hint {
  font-size: 12px;
  color: #9ca3af;
}

.upload-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.upload-item.error {
  background: #fef2f2;
}

.upload-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.upload-name {
  font-size: 13px;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.upload-size {
  font-size: 11px;
  color: #9ca3af;
}

.upload-bar-wrap {
  width: 100px;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
  flex-shrink: 0;
}

.upload-bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
}

.upload-bar.uploading {
  background: #10b981;
}

.upload-bar.done {
  background: #059669;
}

.upload-bar.error {
  background: #ef4444;
}

.upload-tag {
  font-size: 11px;
  font-weight: 500;
  flex-shrink: 0;
  width: 52px;
  text-align: right;
}

.upload-tag.pending {
  color: #9ca3af;
}

.upload-tag.uploading {
  color: #10b981;
}

.upload-tag.done {
  color: #059669;
}

.upload-tag.error {
  color: #ef4444;
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f3f4f6;
}

.footer-info {
  font-size: 13px;
  color: #9ca3af;
  margin-right: auto;
}

.upload-all-btn {
  padding: 8px 20px;
  background: #059669;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-all-btn:hover {
  background: #047857;
}

.cancel-btn {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover {
  border-color: #d1d5db;
  color: #374151;
}
</style>
