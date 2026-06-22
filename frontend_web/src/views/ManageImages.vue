<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Picture, Star, ArrowLeft, Upload, Delete, PictureFilled, Check, Close } from '@element-plus/icons-vue'
import {
  listImages,
  setCover,
  setCuratedImages,
  deleteImagesBatch,
  type LandmarkImageVO,
} from '../api/image'
import UploadDialog from '../components/UploadDialog.vue'

const route = useRoute()
const router = useRouter()
const landmarkId = route.params.id as string

type Mode = 'browse' | 'delete' | 'cover' | 'curated'

const mode = ref<Mode>('browse')
const images = ref<LandmarkImageVO[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const hasMore = ref(true)
const total = ref(0)

// Selection state
const deleteSelected = ref<Set<string>>(new Set())
const coverSelected = ref<string | null>(null)
const curatedSelected = ref<string[]>([])

// Upload dialog
const showUpload = ref(false)

// Intersection observer
let observer: IntersectionObserver | null = null
const sentinel = ref<HTMLDivElement | null>(null)

// Current cover/curated from loaded data (used for pre-selection and badges)
const currentCoverId = computed(() => images.value.find((img) => img.cover)?.id ?? null)
const currentCuratedIds = computed(
  () => images.value.filter((img) => img.curated).map((img) => img.id),
)

onMounted(() => {
  loadFirstPage()
  setupObserver()
})

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
})

function setupObserver() {
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting && hasMore.value && !loading.value) {
        loadMore()
      }
    },
    { threshold: 0.1 },
  )
}

function observeSentinel() {
  if (observer && sentinel.value) {
    observer.disconnect()
    observer.observe(sentinel.value)
  }
}

async function loadFirstPage() {
  loading.value = true
  error.value = ''
  page.value = 1
  try {
    const res = await listImages(landmarkId, 1)
    if (!res || !res.data) {
      const msg = `加载图片失败: 响应数据为空 (code=${res?.code}, message=${res?.message})`
      console.error(msg, res)
      error.value = msg
      return
    }
    images.value = res.data.records
    hasMore.value = res.data.hasMore
    total.value = res.data.total
    observeSentinel()
  } catch (e) {
    console.error('加载图片失败:', e)
    error.value = `加载图片失败: ${e instanceof Error ? e.message : String(e)}`
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (!hasMore.value || loading.value) return
  loading.value = true
  page.value++
  try {
    const res = await listImages(landmarkId, page.value)
    images.value.push(...res.data.records)
    hasMore.value = res.data.hasMore
    total.value = res.data.total
    observeSentinel()
  } catch (e) {
    console.error('加载更多失败:', e)
    page.value--
  } finally {
    loading.value = false
  }
}

// Mode actions
function enterMode(newMode: Mode) {
  if (newMode === 'browse') return
  mode.value = newMode
  // pre-populate selections
  if (newMode === 'cover') {
    coverSelected.value = currentCoverId.value
  } else if (newMode === 'curated') {
    curatedSelected.value = [...currentCuratedIds.value]
  } else if (newMode === 'delete') {
    deleteSelected.value = new Set()
  }
}

function cancelMode() {
  mode.value = 'browse'
  deleteSelected.value = new Set()
  coverSelected.value = null
  curatedSelected.value = []
}

// Delete mode
function toggleDeleteSelect(imageId: string) {
  const s = new Set(deleteSelected.value)
  if (s.has(imageId)) {
    s.delete(imageId)
  } else {
    s.add(imageId)
  }
  deleteSelected.value = s
}

// Cover mode
function selectCover(imageId: string) {
  coverSelected.value = imageId
}

// Curated mode
function toggleCuratedSelect(imageId: string) {
  const arr = [...curatedSelected.value]
  const idx = arr.indexOf(imageId)
  if (idx >= 0) {
    // remove and shift subsequent numbers up
    arr.splice(idx, 1)
  } else {
    if (arr.length >= 5) {
      alert('精选图片最多 5 张')
      return
    }
    arr.push(imageId)
  }
  curatedSelected.value = arr
}

function curatedOrder(imageId: string): number {
  const idx = curatedSelected.value.indexOf(imageId)
  return idx >= 0 ? idx + 1 : 0
}

// Confirm handlers
async function handleDeleteConfirm() {
  if (deleteSelected.value.size === 0) return

  const ids = [...deleteSelected.value]

  // Check which images are affected before showing confirm
  let affectedCover = false
  let affectedCuratedCount = 0
  if (currentCoverId.value && ids.includes(currentCoverId.value)) {
    affectedCover = true
  }
  for (const id of ids) {
    if (currentCuratedIds.value.includes(id)) {
      affectedCuratedCount++
    }
  }

  let msg = `确定删除 ${ids.length} 张图片吗？`
  if (affectedCover || affectedCuratedCount > 0) {
    const parts: string[] = []
    if (affectedCover) parts.push('1 张封面图片')
    if (affectedCuratedCount > 0) parts.push(`${affectedCuratedCount} 张精选图片`)
    msg = `将删除 ${ids.length} 张图片，其中包含${parts.join('和')}，确定删除吗？`
  }
  if (!confirm(msg)) return

  try {
    await deleteImagesBatch(landmarkId, ids)
    cancelMode()
    await loadFirstPage()
  } catch (e) {
    console.error('删除失败:', e)
    error.value = '删除失败'
  }
}

async function handleCoverConfirm() {
  if (!coverSelected.value) return
  try {
    await setCover(landmarkId, coverSelected.value)
    cancelMode()
    await loadFirstPage()
  } catch (e) {
    console.error('设置封面失败:', e)
    error.value = '设置封面失败'
  }
}

async function handleCuratedConfirm() {
  try {
    await setCuratedImages(landmarkId, curatedSelected.value)
    cancelMode()
    await loadFirstPage()
  } catch (e) {
    console.error('设置精选失败:', e)
    error.value = '设置精选失败'
  }
}

function isInMode(): boolean {
  return mode.value !== 'browse'
}

function handleBack() {
  router.push(`/landmark/${landmarkId}/edit`)
}

function onUploaded() {
  loadFirstPage()
}

// Badge helpers for cards in browse mode
function isCoverImage(img: LandmarkImageVO): boolean {
  return img.cover
}

function isCuratedImage(img: LandmarkImageVO): boolean {
  return img.curated
}

function curatedIdx(img: LandmarkImageVO): number {
  if (!img.curated) return 0
  const list = images.value.filter((i) => i.curated)
  return list.findIndex((i) => i.id === img.id) + 1
}
</script>

<template>
  <div class="manage-images">
    <!-- Top toolbar -->
    <div class="toolbar">
      <div class="toolbar-top">
        <div class="toolbar-left">
          <button class="back-btn" @click="handleBack">
            <el-icon :size="18"><ArrowLeft /></el-icon>
          </button>
          <h1>图片管理</h1>
          <span class="image-count">共 {{ total }} 张</span>
        </div>
        <div class="toolbar-actions">
          <button
            type="button"
            class="tool-btn upload-btn"
            :disabled="isInMode()"
            @click="showUpload = true"
          >
            <el-icon :size="16"><Upload /></el-icon>
            上传图片
          </button>
          <button
            type="button"
            class="tool-btn"
            :class="{ active: mode === 'delete' }"
            :disabled="isInMode() && mode !== 'delete'"
            @click="enterMode('delete')"
          >
            <el-icon :size="16"><Delete /></el-icon>
            删除图片
          </button>
          <button
            type="button"
            class="tool-btn"
            :class="{ active: mode === 'cover' }"
            :disabled="isInMode() && mode !== 'cover'"
            @click="enterMode('cover')"
          >
            <el-icon :size="16"><Picture /></el-icon>
            设为封面
          </button>
          <button
            type="button"
            class="tool-btn"
            :class="{ active: mode === 'curated' }"
            :disabled="isInMode() && mode !== 'curated'"
            @click="enterMode('curated')"
          >
            <el-icon :size="16"><Star /></el-icon>
            设为精选
          </button>
        </div>
      </div>

      <!-- Confirm / Cancel bar (shown only in operation mode) -->
      <div v-if="isInMode()" class="toolbar-action-bar">
        <span class="action-label">
          {{ mode === 'delete' ? '删除模式' : mode === 'cover' ? '封面模式' : '精选模式' }}
        </span>
        <button type="button" class="confirm-btn" @click="
          mode === 'delete' ? handleDeleteConfirm() :
          mode === 'cover' ? handleCoverConfirm() :
          handleCuratedConfirm()
        ">
          保存更改
        </button>
        <button type="button" class="cancel-action-btn" @click="cancelMode">取消</button>
      </div>
    </div>

    <div v-if="error" class="error-msg">{{ error }}</div>

    <!-- Image grid -->
    <div v-if="images.length > 0" class="image-grid">
      <div
        v-for="img in images"
        :key="img.id"
        class="image-card"
        @click="
          mode === 'delete' ? toggleDeleteSelect(img.id) :
          mode === 'cover' ? selectCover(img.id) :
          mode === 'curated' ? toggleCuratedSelect(img.id) :
          undefined
        "
      >
        <div class="card-thumb">
          <img :src="img.url" :alt="'图片 ' + img.id.slice(0, 8)" />

          <!-- Left-top corner badges -->
          <span v-if="isCoverImage(img)" class="corner-badge cover-badge" title="封面图片">
            <el-icon :size="14"><Picture /></el-icon>
          </span>
          <span
            v-else-if="isCuratedImage(img) && mode !== 'curated'"
            class="corner-badge curated-badge"
            title="精选图片"
          >
            <el-icon :size="14"><Star /></el-icon>
          </span>

          <!-- Right-top selection box (only in operation modes) -->
          <span
            v-if="isInMode()"
            class="select-box"
            :class="{
              selected:
                mode === 'delete' ? deleteSelected.has(img.id) :
                mode === 'cover' ? coverSelected === img.id :
                mode === 'curated' ? curatedSelected.includes(img.id) :
                false,
              'cover-active': mode === 'cover' && (coverSelected === img.id),
              'curated-active': mode === 'curated' && curatedSelected.includes(img.id),
              'delete-active': mode === 'delete' && deleteSelected.has(img.id),
            }"
          >
            <template v-if="mode === 'delete' && deleteSelected.has(img.id)">
              <el-icon :size="14"><Close /></el-icon>
            </template>
            <template v-else-if="mode === 'cover' && coverSelected === img.id">
              <el-icon :size="14"><Check /></el-icon>
            </template>
            <template v-else-if="mode === 'curated' && curatedSelected.includes(img.id)">
              <span class="curated-num">{{ curatedOrder(img.id) }}</span>
            </template>
          </span>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="!loading && images.length === 0 && !error" class="empty-state">
      <el-icon :size="48" color="#d1d5db"><PictureFilled /></el-icon>
      <p>暂无图片</p>
      <button type="button" class="upload-btn-empty" :disabled="isInMode()" @click="showUpload = true">
        上传第一张图片
      </button>
    </div>

    <!-- Infinite scroll sentinel -->
    <div ref="sentinel" class="scroll-sentinel">
      <div v-if="loading" class="load-more">加载中...</div>
      <div v-else-if="!hasMore && images.length > 0" class="load-more">已加载全部图片</div>
    </div>

    <!-- Upload dialog -->
    <UploadDialog
      :visible="showUpload"
      :landmark-id="landmarkId"
      @update:visible="showUpload = $event"
      @uploaded="onUploaded"
    />
  </div>
</template>

<style scoped>
.manage-images {
}

/* Toolbar */
.toolbar {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
  gap: 10px;
}

.toolbar-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-action-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 8px;
}

.action-label {
  font-size: 13px;
  font-weight: 500;
  color: #059669;
  margin-right: auto;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.toolbar-left h1 {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.image-count {
  font-size: 13px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 10px;
  border-radius: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.2s;
}

.back-btn:hover {
  border-color: #059669;
  color: #059669;
  background: #ecfdf5;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tool-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.tool-btn:hover:not(:disabled) {
  border-color: #10b981;
  color: #10b981;
}

.tool-btn.active {
  background: #ecfdf5;
  border-color: #10b981;
  color: #059669;
}

.tool-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.upload-btn {
  background: #059669;
  color: #fff;
  border-color: #059669;
}

.upload-btn:hover:not(:disabled) {
  background: #047857;
  border-color: #047857;
  color: #fff;
}

.confirm-btn {
  padding: 7px 16px;
  background: #059669;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.confirm-btn:hover {
  background: #047857;
}

.cancel-action-btn {
  padding: 7px 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
}

.cancel-action-btn:hover {
  border-color: #d1d5db;
  color: #374151;
}

/* Error */
.error-msg {
  padding: 10px 14px;
  background: #fef2f2;
  border-radius: 8px;
  color: #ef4444;
  font-size: 13px;
  margin-bottom: 16px;
}

/* Image grid */
.image-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.image-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  cursor: default;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
}

.image-card:hover {
  border-color: #059669;
  box-shadow: 0 4px 16px rgba(5, 150, 105, 0.1);
  transform: translateY(-1px);
}

.card-thumb {
  position: relative;
  aspect-ratio: 4 / 3;
  background: #f9fafb;
  overflow: hidden;
}

.card-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Left-top corner badges */
.corner-badge {
  position: absolute;
  top: 6px;
  left: 6px;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-badge {
  background: #10b981;
  color: #fff;
}

.curated-badge {
  background: #f59e0b;
  color: #fff;
}

/* Right-top selection box */
.select-box {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  border: 2px solid #d1d5db;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  z-index: 2;
}

.select-box svg {
  width: 14px;
  height: 14px;
}

.select-box.cover-active {
  border-color: #10b981;
  background: #10b981;
  color: #fff;
}

.select-box.curated-active {
  border-color: #10b981;
  background: #10b981;
  color: #fff;
}

.select-box.delete-active {
  border-color: #ef4444;
  background: #ef4444;
  color: #fff;
}

.curated-num {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

/* Empty state */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
}

.empty-state p {
  color: #9ca3af;
  font-size: 14px;
  margin: 0;
}

.upload-btn-empty {
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

.upload-btn-empty:hover:not(:disabled) {
  background: #047857;
}

.upload-btn-empty:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* Scroll sentinel */
.scroll-sentinel {
  padding: 24px 0;
  text-align: center;
}

.load-more {
  font-size: 13px;
  color: #9ca3af;
}
</style>
